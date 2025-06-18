# frozen_string_literal: true

class CoinWithdrawal < ApplicationRecord
  include AASM

  acts_as_paranoid

  belongs_to :user
  has_one :coin_withdrawal_operation, dependent: :destroy
  has_one :coin_transaction, as: :reference, dependent: :nullify
  has_one :coin_internal_transfer_operation, dependent: :destroy

  validates :coin_currency, presence: true
  validates :coin_amount, presence: true, numericality: { greater_than: 0 }
  validates :coin_fee, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :coin_address, presence: true, unless: :internal_transfer?
  validates :coin_layer, presence: true, unless: :internal_transfer?
  validates :status, presence: true

  validate :validate_coin_amount
  validate :validate_coin_address_and_layer
  validate :validate_coin_address_format, unless: :internal_transfer?
  validate :validate_receiver_internal, if: :internal_transfer?

  before_validation :detect_internal_transfer_by_address
  before_validation :assign_coin_layer
  before_validation :calculate_coin_fee, on: :create
  after_create :send_event_withdrawal_to_kafka
  after_create :create_operations_later

  scope :sorted, -> { order(created_at: :desc) }

  aasm column: 'status', whiny_transitions: false do
    state :pending, initial: true
    state :processing
    state :completed, after_enter: :send_event_complete_withdrawal_to_kafka
    state :failed, after_enter: :send_event_fail_withdrawal_to_kafka
    state :cancelled, after_enter: :send_event_cancel_withdrawal_to_kafka

    event :process do
      transitions from: [ :pending ], to: :processing
    end

    event :complete do
      transitions from: [ :processing ], to: :completed
    end

    event :fail do
      transitions from: [ :processing ], to: :failed
    end

    event :cancel do
      transitions from: [ :pending, :processing ], to: :cancelled
    end
  end

  def coin_account
    @coin_account ||= user.coin_accounts.of_coin(coin_currency).main
  end

  def record_tx_hash_arrived_at
    update(tx_hash_arrived_at: Time.current)
  end

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id coin_currency coin_amount receiver_email receiver_username receiver_phone_number
      coin_fee coin_address coin_layer status
      tx_hash created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user coin_withdrawal_operation coin_transaction coin_internal_transfer_operation]
  end

  def portal_coin
    CoinAccount.coin_and_layer_to_portal_coin(coin_currency, coin_layer)
  end

  def internal_transfer?
    receiver_email.present? || receiver_phone_number.present? || receiver_username.present?
  end

  def send_event_complete_withdrawal_to_kafka
    return unless completed?

    KafkaService::Services::Coin::CoinWithdrawalService.new.update_status(
      identifier: id,
      operation_type: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_RELEASING
    )
  end

  private

  def validate_coin_amount
    return errors.add(:coin_amount, :blank) if coin_amount.nil?

    return unless coin_amount + coin_fee > coin_account.available_balance

    errors.add(:coin_amount, :exceed_available_balance)
  end

  def validate_coin_address_and_layer
    return if internal_transfer?
    return errors.add(:coin_address, :blank) if coin_address.blank?

    errors.add(:coin_layer, :blank) if coin_layer.blank?
  end

  def validate_coin_address_format
    return if coin_address.blank? || internal_transfer?

    validator = CryptocurrencyAddressValidator.new(coin_address, coin_layer)
    unless validator.valid?
      errors.add(:coin_address, :invalid_format)
    end
  rescue ArgumentError => e
    errors.add(:coin_layer, e.message)
  end

  def validate_receiver_internal
    receiver = nil

    if receiver_email.present?
      receiver = User.find_by(email: receiver_email)
      if receiver.nil?
        errors.add(:receiver_email, :not_found)
        return
      end
    elsif receiver_username.present?
      receiver = User.find_by(username: receiver_username)
      if receiver.nil?
        errors.add(:receiver_username, :not_found)
        return
      end
    elsif receiver_phone_number.present?
      receiver = User.find_by(phone_number: receiver_phone_number)
      if receiver.nil?
        errors.add(:receiver_phone_number, :not_found)
        return
      end
    end

    if receiver.id == user.id
      if receiver_email.present?
        errors.add(:receiver_email, :cannot_transfer_to_self)
      elsif receiver_username.present?
        errors.add(:receiver_username, :cannot_transfer_to_self)
      elsif receiver_phone_number.present?
        errors.add(:receiver_phone_number, :cannot_transfer_to_self)
      end
    end
  end

  def detect_internal_transfer_by_address
    return if coin_address.blank?
    return if receiver_email.present? || receiver_phone_number.present? || receiver_username.present?

    target_account = CoinAccount.find_by(address: coin_address, coin_currency: coin_currency)
    return if target_account.blank?

    # Set the receiver information to the target account's user email
    self.receiver_email = target_account.user.email
  end

  def assign_coin_layer
    self.coin_layer ||= coin_account&.layer unless internal_transfer?
  end

  def calculate_coin_fee
    return self.coin_fee = 0.0 if internal_transfer?

    fee = Setting.send("#{coin_currency}_#{coin_layer}_withdrawal_fee") if Setting.respond_to?("#{coin_currency}_#{coin_layer}_withdrawal_fee")
    self.coin_fee = fee || 0.0
  end

  def freeze_user_balance
    coin_account.with_lock do
      coin_account.update!(
        frozen_balance: coin_account.frozen_balance + coin_amount + coin_fee
      )
    end
  end

  def unfreeze_user_balance
    coin_account.with_lock do
      coin_account.update!(
        frozen_balance: coin_account.frozen_balance - (coin_amount + coin_fee)
      )
    end
  end

  def create_operations_later
    SidekiqMethod.enqueue_to('critical', self, :create_operations)
  end

  def create_operations
    if internal_transfer?
      receiver = nil
      if receiver_email.present?
        receiver = User.find_by(email: receiver_email)
      elsif receiver_username.present?
        receiver = User.find_by(username: receiver_username)
      elsif receiver_phone_number.present?
        receiver = User.find_by(phone_number: receiver_phone_number)
      end

      return unless receiver

      create_coin_internal_transfer_operation!(
        sender: user,
        receiver: receiver,
        coin_currency: coin_currency,
        coin_amount: coin_amount,
        status: 'pending'
      )
    else
      create_coin_withdrawal_operation!(
        coin_amount: coin_amount,
        coin_fee: coin_fee,
        coin_currency: coin_currency
      )
    end
  end

  def send_event_withdrawal_to_kafka
    account_key = KafkaService::Services::AccountKeyBuilderService.build_coin_account_key(
      user_id: user_id,
      account_id: main_coin_account.id
    )

    params = {
      identifier: id,
      status: pending? || processing? ? 'verified' : status,
      user_id: user_id,
      coin: coin_currency,
      account_key: account_key,
      amount: coin_amount,
      fee: coin_fee
    }

    # Add recipient_account_key for internal transfers
    recipient = nil
    if internal_transfer?
      if receiver_email.present?
        recipient = User.find_by(email: receiver_email)
      elsif receiver_username.present?
        recipient = User.find_by(username: receiver_username)
      elsif receiver_phone_number.present?
        recipient = User.find_by(phone_number: receiver_phone_number)
      end

      if recipient.present?
        recipient_account = recipient.coin_accounts.of_coin(coin_currency).main
        if recipient_account.present?
          recipient_account_key = KafkaService::Services::AccountKeyBuilderService.build_coin_account_key(
            user_id: recipient.id,
            account_id: recipient_account.id
          )
          params[:recipient_account_key] = recipient_account_key
        end
      end
    end

    KafkaService::Services::Coin::CoinWithdrawalService.new.create(**params)
  rescue StandardError => e
    Rails.logger.error("Failed to send withdrawal event to Kafka: #{e.message}")
  end

  def send_event_fail_withdrawal_to_kafka
    return unless failed?

    KafkaService::Services::Coin::CoinWithdrawalService.new.update_status(
      identifier: id,
      operation_type: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_FAILED
    )
  end

  def send_event_cancel_withdrawal_to_kafka
    return unless cancelled?

    KafkaService::Services::Coin::CoinWithdrawalService.new.update_status(
      identifier: id,
      operation_type: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CANCELLED
    )
  end

  def main_coin_account
    @main_coin_account ||= user.coin_accounts.of_coin(coin_currency).main
  end
end
