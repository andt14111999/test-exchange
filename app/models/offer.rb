# frozen_string_literal: true

class Offer < ApplicationRecord
  belongs_to :user
  belongs_to :payment_method, optional: true
  has_many :trades, dependent: :nullify

  OFFER_TYPES = %w[buy sell].freeze
  STATUSES = %w[active disabled deleted scheduled].freeze

  validates :user_id, presence: true
  validates :offer_type, presence: true, inclusion: { in: OFFER_TYPES }
  validates :coin_currency, presence: true
  validates :currency, presence: true
  validates :price, presence: true, numericality: { greater_than: 0 }
  validates :min_amount, presence: true, numericality: { greater_than: 0 }
  validates :max_amount, presence: true, numericality: { greater_than: 0 }
  validates :total_amount, presence: true, numericality: { greater_than: 0 }
  validates :payment_time, presence: true, numericality: { greater_than: 0, only_integer: true }
  validates :country_code, presence: true
  validate :min_amount_less_than_max_amount
  validate :max_amount_less_than_total_amount
  validate :validate_schedule_times, if: -> { schedule_start_time.present? || schedule_end_time.present? }

  scope :active, -> { where(disabled: false, deleted: false) }
  scope :disabled, -> { where(disabled: true) }
  scope :deleted, -> { where(deleted: true) }
  scope :scheduled, -> { where('schedule_start_time IS NOT NULL OR schedule_end_time IS NOT NULL') }
  scope :currently_active, -> {
    active.where('(schedule_start_time IS NULL AND schedule_end_time IS NULL) OR
                 (schedule_start_time <= ? AND (schedule_end_time IS NULL OR schedule_end_time >= ?))',
                 Time.zone.now, Time.zone.now)
  }
  scope :buy_offers, -> { where(offer_type: 'buy') }
  scope :sell_offers, -> { where(offer_type: 'sell') }
  scope :online, -> { where(online: true) }
  scope :offline, -> { where(online: false) }
  scope :automatic, -> { where(automatic: true) }
  scope :manual, -> { where(automatic: false) }
  scope :of_currency, ->(currency) { where(currency: currency) }
  scope :of_coin_currency, ->(coin_currency) { where(coin_currency: coin_currency) }
  scope :of_country, ->(country_code) { where(country_code: country_code) }

  # Order scopes
  scope :price_asc, -> { order(price: :asc) }
  scope :price_desc, -> { order(price: :desc) }
  scope :newest_first, -> { order(created_at: :desc) }
  scope :oldest_first, -> { order(created_at: :asc) }

  before_save :ensure_bank_names_array
  before_save :update_price_from_margin, if: -> { margin.present? && margin_changed? }
  after_create :send_offer_create_to_kafka
  after_update :send_offer_update_to_kafka, if: -> { saved_change_to_disabled? || saved_change_to_deleted? || saved_change_to_price? || saved_change_to_total_amount? }

  attr_accessor :temp_start_time, :temp_end_time

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id offer_type coin_currency currency
      price min_amount max_amount total_amount
      payment_method_id payment_time payment_details
      country_code disabled deleted automatic online
      terms_of_trade disable_reason margin fixed_coin_price
      bank_names schedule_start_time schedule_end_time
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user payment_method trades]
  end

  # Convenience methods for offer type
  def buy?
    offer_type == 'buy'
  end

  def sell?
    offer_type == 'sell'
  end

  # Offer status methods
  def active?
    !disabled? && !deleted?
  end

  def scheduled?
    schedule_start_time.present? || schedule_end_time.present?
  end

  def currently_active?
    return false if disabled? || deleted?
    return true unless scheduled?

    current_time = Time.zone.now
    is_after_start = schedule_start_time.nil? || current_time >= schedule_start_time
    is_before_end = schedule_end_time.nil? || current_time <= schedule_end_time

    is_after_start && is_before_end
  end

  # Offer status management
  def disable!(reason = nil)
    self.disable_reason = reason || disable_reason || 'Disabled by user'
    if update(disabled: true)
      begin
          send_offer_disable_to_kafka
          true
      rescue StandardError => e
        errors.add(:base, e.message)
        false
      end
    end
  end

  def enable!
    self.disable_reason = nil
    if update(disabled: false)
      begin
        send_offer_enable_to_kafka
        true
      rescue StandardError => e
        errors.add(:base, e.message)
        false
      end
    end
  end

  def delete!
    return false if trades.in_progress.exists?
    send_offer_delete_to_kafka
  end

  def set_online!
    update!(online: true)
  end

  def set_offline!
    update!(online: false)
  end

  def schedule!(start_time = nil, end_time = nil)
    self.temp_start_time = start_time
    self.temp_end_time = end_time
    set_schedule_times
  end

  # Dynamic pricing
  def update_price_from_market!(market_price = nil)
    return unless margin.present?

    market_price ||= fetch_market_price
    return unless market_price.present?

    new_price = if buy?
                  market_price * (1 - margin)
    else
                  market_price * (1 + margin)
    end

    update!(price: new_price)
  end

  # Available amount management
  def available_amount
    total_amount - in_progress_amount
  end

  def in_progress_amount
    trades.in_progress.sum(:coin_amount)
  end

  def has_available_amount?(amount)
    amount <= available_amount
  end

  # Kafka event methods
  def send_offer_create_to_kafka
    offer_service.create(offer: self)
  rescue StandardError => e
    Rails.logger.error("Error sending offer create to Kafka: #{e.message}")
  end

  def send_offer_update_to_kafka
    offer_service.update(offer: self)
  rescue StandardError => e
    Rails.logger.error("Error sending offer update to Kafka: #{e.message}")
  end

  def send_offer_disable_to_kafka
    offer_service.disable(offer: self)
  rescue StandardError => e
    Rails.logger.error("Error sending offer disable to Kafka: #{e.message}")
  end

  def send_offer_enable_to_kafka
    offer_service.enable(offer: self)
  rescue StandardError => e
    Rails.logger.error("Error sending offer enable to Kafka: #{e.message}")
  end

  def send_offer_delete_to_kafka
    offer_service.delete(offer: self)
  rescue StandardError => e
    Rails.logger.error("Error sending offer delete to Kafka: #{e.message}")
  end

  private

  def offer_service
    @offer_service ||= KafkaService::Services::Offer::OfferService.new
  end

  def record_disable_reason(reason = nil)
    self.disable_reason = reason || disable_reason || 'Disabled by user'
  end

  def set_schedule_times
    update!(
      schedule_start_time: temp_start_time || schedule_start_time,
      schedule_end_time: temp_end_time || schedule_end_time
    )
  end

  def min_amount_less_than_max_amount
    return if min_amount.nil? || max_amount.nil?
    return if min_amount <= max_amount

    errors.add(:min_amount, 'must be less than or equal to max amount')
  end

  def max_amount_less_than_total_amount
    return if max_amount.nil? || total_amount.nil?
    return if max_amount <= total_amount

    errors.add(:max_amount, 'must be less than or equal to total amount')
  end

  def validate_schedule_times
    return unless schedule_start_time.present? && schedule_end_time.present?
    return if schedule_start_time < schedule_end_time

    errors.add(:schedule_start_time, 'must be earlier than end time')
  end

  def ensure_bank_names_array
    self.bank_names = [] if bank_names.nil?
  end

  def update_price_from_margin
    update_price_from_market!
  end

  def fetch_market_price
    # This should be implemented to fetch the current market price
    # from an exchange API or other source
    # For now, we'll return nil
    nil
  end
end
