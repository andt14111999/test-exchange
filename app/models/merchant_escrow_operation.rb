# frozen_string_literal: true

class MerchantEscrowOperation < ApplicationRecord
  include AASM

  # Constants
  OPERATION_TYPES = %w[mint burn].freeze
  OPERATION_TRANSACTION_TYPES = {
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
  validates :fiat_currency, presence: true, inclusion: { in: FiatAccount::SUPPORTED_CURRENCIES.keys }
  validates :status, presence: true, inclusion: { in: %w[pending completed failed] }
  validates :merchant_escrow_id, presence: true

  # Callbacks
  after_create :record_transactions

  # Scopes
  scope :sorted, -> { order(created_at: :desc) }
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
    usdt_account
  end

  def find_user_fiat_account
    fiat_account
  end

  private

  # Record transactions for accounting purposes
  def record_transactions
    begin
      ActiveRecord::Base.transaction do
        usdt_account = CoinAccount.find_by(id: usdt_account_id)
        fiat_account = FiatAccount.find_by(id: fiat_account_id)

        unless usdt_account && fiat_account
          missing = []
          missing << 'USDT account' unless usdt_account
          missing << 'Fiat account' unless fiat_account
          raise StandardError, "Missing required accounts: #{missing.join(', ')}"
        end

        record_usdt_transaction(usdt_account)
        record_fiat_transaction(fiat_account)
        complete!
      end
    rescue StandardError => e
      fail!
      self.status_explanation = e.message
      save!
    end
  end

  def record_usdt_transaction(account)
    coin_transactions.create!(
      coin_account: account,
      amount: usdt_amount,
      transaction_type: operation_type == 'mint' ? 'lock' : 'unlock',
      coin_currency: account.coin_currency,
      snapshot_balance: account.balance,
      snapshot_frozen_balance: account.frozen_balance
    )
  end

  def record_fiat_transaction(account)
    fiat_transactions.create!(
      fiat_account: account,
      amount: fiat_amount,
      transaction_type: operation_type == 'mint' ? 'mint' : 'burn',
      currency: account.currency,
      snapshot_balance: account.balance,
      snapshot_frozen_balance: account.frozen_balance
    )
  end
end
