# frozen_string_literal: true

class MerchantEscrow < ApplicationRecord
  include AASM

  acts_as_paranoid

  # Associations
  belongs_to :user
  belongs_to :usdt_account, class_name: 'CoinAccount', foreign_key: 'usdt_account_id'
  belongs_to :fiat_account, class_name: 'FiatAccount', foreign_key: 'fiat_account_id'
  has_many :merchant_escrow_operations, dependent: :destroy

  # Validations
  validates :usdt_amount, presence: true, numericality: { greater_than: 0 }
  validates :fiat_amount, presence: true, numericality: { greater_than: 0 }
  validates :fiat_currency, presence: true
  validates :exchange_rate, numericality: { greater_than: 0 }, allow_nil: true
  validate :validate_user_is_merchant

  # Scopes
  scope :sorted, -> { order(created_at: :desc) }
  scope :active, -> { where(status: 'active') }
  scope :cancelled, -> { where(status: 'cancelled') }
  scope :pending, -> { where(status: 'pending') }
  scope :transaction_error, -> { where(status: 'transaction_error') }

  # Callbacks
  after_save :broadcast_status_change, if: :saved_change_to_status?

  # State Machine
  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :active
    state :cancelled
    state :transaction_error

    event :activate do
      transitions from: :pending, to: :active
    end

    event :cancel do
      transitions from: [ :pending, :active ], to: :cancelled
    end

    event :transaction_fail do
      before do |error_msg|
        self.error_message = error_msg
      end
      transitions from: [ :pending, :active ], to: :transaction_error
    end
  end

  # Ransack Configuration
  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id usdt_account_id fiat_account_id usdt_amount fiat_amount
      fiat_currency exchange_rate status created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user usdt_account fiat_account merchant_escrow_operations]
  end

  # Public Methods
  def can_cancel?
    pending? || active?
  end

  def activate!
    update!(status: 'active')
  end

  # Helper methods to find related accounts
  def find_user_usdt_account
    user.coin_accounts.of_coin('usdt').main
  end

  def find_user_fiat_account
    user.fiat_accounts.of_currency(fiat_currency).first
  end

  private

  def validate_user_is_merchant
    errors.add(:user, :not_merchant) unless user&.merchant?
  end

  def broadcast_status_change
    MerchantEscrowBroadcastService.call(self)
  end

  def send_kafka_event_create
    merchant_escrow_client.create(
      merchant_escrow: self,
      usdt_account_key: KafkaService::Services::AccountKeyBuilderService.build_coin_account_key(
        user_id: user_id,
        account_id: usdt_account_id
      ),
      fiat_account_key: KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
        user_id: user_id,
        account_id: fiat_account_id
      )
    )
  end

  def send_kafka_event_cancel
    merchant_escrow_client.cancel(
      merchant_escrow: self,
      usdt_account_key: KafkaService::Services::AccountKeyBuilderService.build_coin_account_key(
        user_id: user_id,
        account_id: usdt_account_id
      ),
      fiat_account_key: KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
        user_id: user_id,
        account_id: fiat_account_id
      )
    )
  end

  def merchant_escrow_client
    @merchant_escrow_client ||= KafkaService::Services::Merchant::MerchantEscrowService.new
  end
end
