# frozen_string_literal: true

class TradeExpiryJob < ApplicationJob
  queue_as :default

  def perform
    # Process unpaid trades that have expired
    process_expired_trades

    # Process trades with expired disputes
    process_expired_disputes
  end

  private

  def process_expired_trades
    Trade.unpaid.where('expired_at < ?', Time.zone.now).find_each do |trade|
      service = TradeService.new(trade)

      if service.auto_cancel_trade!
        trade.add_system_message!("Trade #{trade.ref} was automatically cancelled due to payment timeout")
        Rails.logger.info("Trade #{trade.ref} automatically cancelled due to expiry")
      else
        Rails.logger.error("Failed to auto-cancel expired trade #{trade.ref}")
      end
    end
  end

  def process_expired_disputes
    Trade.disputed.find_each do |trade|
      next unless trade.dispute_expired?

      trade.perform_dispute_timeout_check!
    end
  end
end
