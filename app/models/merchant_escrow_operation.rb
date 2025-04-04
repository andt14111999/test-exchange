# frozen_string_literal: true

class MerchantEscrowOperation < ApplicationRecord
  include AASM

  # Constants
  OPERATION_TYPES = %w[freeze unfreeze mint burn].freeze
  OPERATION_TRANSACTION_TYPES = {
    'freeze' => 'lock',
    'unfreeze' => 'unlock',
    'mint' => 'mint',
    'burn' => 'burn'
  }.freeze

  # Associations
  belongs_to :merchant_escrow, inverse_of: :merchant_escrow_operations
  belongs_to :usdt_account, class_name: 'CoinAccount', foreign_key: 'usdt_account_id'
  belongs_to :fiat_account, class_name: 'FiatAccount', foreign_key: 'fiat_account_id'
  has_many :coin_transactions, as: :operation, dependent: :destroy
  has_many :fiat_transactions, as: :operation, dependent: :destroy

  # Delegations
  delegate :user, to: :merchant_escrow

  # Validations
  validates :operation_type, presence: true, inclusion: { in: OPERATION_TYPES }
  validates :usdt_amount, presence: true, numericality: { greater_than: 0 }
  validates :fiat_amount, presence: true, numericality: { greater_than: 0 }
  validates :fiat_currency, presence: true
  validates :status, presence: true, inclusion: { in: %w[pending completed failed] }
  validates :merchant_escrow_id, presence: true

  # Callbacks
  after_create :process_operation

  # Scopes
  scope :sorted, -> { order(created_at: :desc) }
  scope :freeze_operations, -> { where(operation_type: 'freeze') }
  scope :unfreeze_operations, -> { where(operation_type: 'unfreeze') }
  scope :mint_operations, -> { where(operation_type: 'mint') }
  scope :burn_operations, -> { where(operation_type: 'burn') }

  # State Machine
  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :completed
    state :failed

    event :complete do
      transitions from: :pending, to: :completed
    end

    event :fail do
      transitions from: :pending, to: :failed
    end
  end

  # Ransack Configuration
  def self.ransackable_attributes(_auth_object = nil)
    %w[created_at fiat_amount fiat_currency id merchant_escrow_id usdt_account_id fiat_account_id
       operation_type status updated_at usdt_amount]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[coin_transactions fiat_transactions merchant_escrow usdt_account fiat_account]
  end

  # Helper methods to find related accounts
  def find_user_usdt_account
    user.coin_accounts.of_coin('usdt').main
  end

  def find_user_fiat_account
    user.fiat_accounts.of_currency(fiat_currency).first
  end

  private

  # Operation Processing
  def process_operation
    begin
      ActiveRecord::Base.transaction do
        execute_operation
        complete!
      end
    rescue StandardError => e
      handle_operation_error(e)
    end
  end

  def execute_operation
    case operation_type
    when 'freeze'
      process_usdt_operation(:freeze_balance)
    when 'unfreeze'
      process_usdt_operation(:unfreeze_balance)
    when 'mint'
      process_fiat_operation(:increase_balance)
    when 'burn'
      process_fiat_operation(:decrease_balance)
    end
  end

  def handle_operation_error(error)
    fail!
    update!(status_explanation: error.message)
    Rails.logger.error "Failed to process merchant escrow operation: #{error.message}"
    raise error
  end

  # USDT Operations
  def process_usdt_operation(action)
    account = find_user_usdt_account
    account.with_lock do
      validate_usdt_balance(account, action)
      update_usdt_balance(account, action)
      account.save!
      record_usdt_transaction(account)
    end
  end

  def validate_usdt_balance(account, action)
    case action
    when :freeze_balance
      raise 'Insufficient balance' if usdt_amount > account.available_balance
    when :unfreeze_balance
      raise 'Insufficient frozen balance' if usdt_amount > account.frozen_balance
    end
  end

  def update_usdt_balance(account, action)
    case action
    when :freeze_balance
      account.frozen_balance += usdt_amount
    when :unfreeze_balance
      account.frozen_balance -= usdt_amount
    end
  end

  def record_usdt_transaction(account)
    create_coin_transaction(
      account,
      usdt_amount,
      OPERATION_TRANSACTION_TYPES[operation_type],
      account.balance,
      account.frozen_balance
    )
  end

  # Fiat Operations
  def process_fiat_operation(action)
    account = find_user_fiat_account
    account.with_lock do
      validate_fiat_balance(account, action)
      update_fiat_balance(account, action)
      account.save!
      record_fiat_transaction(account)
    end
  end

  def validate_fiat_balance(account, action)
    if action == :decrease_balance && fiat_amount > account.balance
      raise 'Insufficient balance'
    end
  end

  def update_fiat_balance(account, action)
    case action
    when :increase_balance
      account.balance += fiat_amount
    when :decrease_balance
      account.balance -= fiat_amount
    end
  end

  def record_fiat_transaction(account)
    create_fiat_transaction(
      account,
      fiat_amount,
      OPERATION_TRANSACTION_TYPES[operation_type],
      account.balance,
      account.frozen_balance
    )
  end

  # Transaction Creation
  def create_coin_transaction(account, amount, transaction_type, snapshot_balance, snapshot_frozen_balance)
    coin_transactions.create!(
      coin_account: account,
      amount: amount,
      transaction_type: transaction_type,
      coin_currency: account.coin_currency,
      snapshot_balance: snapshot_balance,
      snapshot_frozen_balance: snapshot_frozen_balance
    )
  end

  def create_fiat_transaction(account, amount, transaction_type, snapshot_balance, snapshot_frozen_balance)
    fiat_transactions.create!(
      fiat_account: account,
      amount: amount,
      transaction_type: transaction_type,
      currency: account.currency,
      snapshot_balance: snapshot_balance,
      snapshot_frozen_balance: snapshot_frozen_balance
    )
  end
end
