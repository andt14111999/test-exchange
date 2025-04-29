# frozen_string_literal: true

class TradeService
  PENDING_STATUSES = %w[awaiting unpicked picked unpaid paid disputed].freeze
  CLOSED_STATUSES = %w[released cancelled cancelled_automatically aborted aborted_fiat].freeze
  SCORABLE_STATUSES = %w[released cancelled disputed].freeze

  def initialize(trade)
    @trade = trade
  end

  class << self
    def create_trade_from_offer(offer, taker, amount, taker_side)
      Trade.transaction do
        trade = Trade.new.tap do |t|
          t.offer = offer
          t.set_offer_data(offer)
          t.set_price(offer.price)
          t.set_taker_side(taker, taker_side)
          t.calculate_amounts(amount)
          t.calculate_fees
        end

        if trade.save
          handle_fiat_token_operations(trade)
          trade.try_start!
          return trade
        else
          raise ActiveRecord::Rollback
        end

        nil
      end
    end

    def handle_fiat_token_operations(trade)
      if trade.buyer_is_taker? && trade.offer.sell?
        # Taker buys crypto with fiat - create fiat_token_deposit
        create_fiat_token_deposit(trade)
      elsif trade.seller_is_taker? && trade.offer.buy?
        # Taker sells crypto for fiat - create fiat_token_withdrawal
        create_fiat_token_withdrawal(trade)
      end
    end

    def create_fiat_token_deposit(trade)
      deposit = FiatDeposit.create!(
        user: trade.buyer,
        fiat_account: trade.buyer.fiat_accounts.find_by(currency: trade.fiat_currency.upcase),
        currency: trade.fiat_currency,
        country_code: trade.offer.country_code,
        fiat_amount: trade.fiat_amount,
        payable: trade,
        status: 'awaiting'
      )

      trade.update!(fiat_token_deposit: deposit)
    end

    def create_fiat_token_withdrawal(trade)
      # Similar implementation for withdrawal would go here
    end
  end

  def pick_trade!
    return false unless @trade.may_mark_as_picked?

    @trade.mark_as_picked!
  end

  def unpick_trade!
    return false unless @trade.may_mark_as_unpicked?

    @trade.mark_as_unpicked!
  end

  def pay_trade!
    return false unless @trade.may_mark_as_paid?

    @trade.mark_as_paid!
  end

  def release_trade!
    return false unless @trade.may_mark_as_released?

    send_event_and_return_trade('complete')
  end

  def dispute_trade!(reason)
    return false unless @trade.may_mark_as_disputed?

    @trade.dispute_reason_param = reason
    @trade.mark_as_disputed!
  end

  def cancel_dispute_trade!
    return false unless @trade.disputed?

    @trade.mark_as_paid!
  end

  def cancel_trade!(reason = nil)
    return false unless @trade.may_cancel?

    send_event_and_return_trade('cancel')
  end

  def auto_cancel_trade!
    return false unless @trade.may_cancel_automatically?

    send_event_and_return_trade('cancel')
  end

  private

  def send_event_and_return_trade(event_type)
    service = KafkaService::Services::Trade::TradeService.new

    case event_type
    when 'complete'
      service.complete(trade: @trade)
    when 'cancel'
      service.cancel(trade: @trade)
    end

    @trade
  end

  def handle_trade_released
    if @trade.fiat_token_deposit.present?
      mark_fiat_token_deposit_as_processed
    elsif @trade.fiat_token_withdrawal.present?
      mark_fiat_token_withdrawal_as_processed
    end

    # Additional logic for handling released trade could go here
  end

  def handle_trade_cancelled(reason = nil)
    if @trade.fiat_token_deposit.present?
      mark_fiat_token_deposit_as_cancelled(reason)
    elsif @trade.fiat_token_withdrawal.present?
      mark_fiat_token_withdrawal_as_cancelled(reason)
    end

    # Additional logic for handling cancelled trade could go here
  end

  def mark_fiat_token_deposit_as_processed
    return unless @trade.fiat_token_deposit&.may_process?

    @trade.fiat_token_deposit.process!
  end

  def mark_fiat_token_deposit_as_cancelled(reason = nil)
    return unless @trade.fiat_token_deposit&.may_cancel?

    @trade.fiat_token_deposit.cancel_reason_param = reason if reason.present?
    @trade.fiat_token_deposit.cancel!
  end

  def mark_fiat_token_withdrawal_as_processed
    # Implementation for processing withdrawal
  end

  def mark_fiat_token_withdrawal_as_cancelled(reason = nil)
    # Implementation for cancelling withdrawal
  end
end
