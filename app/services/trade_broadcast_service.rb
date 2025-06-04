# frozen_string_literal: true

class TradeBroadcastService
  def self.call(trade)
    new(trade).call
  end

  def initialize(trade)
    @trade = trade
  end

  def call
    success = broadcast_trade

    if !success && @trade.respond_to?(:delivered)
      @trade.update(delivered: false)
    end

    success
  end

  private

  def broadcast_trade
    begin
      TradeChannel.broadcast_to_trade(@trade, trade_data)
      @trade.update(delivered: true) if @trade.respond_to?(:delivered)
      true
    rescue => e
      Rails.logger.error "Error broadcasting trade: #{e.message}"
      false
    end
  end

  def trade_data
    {
      status: 'success',
      data: {
        id: @trade.id,
        ref: @trade.ref,
        status: @trade.status,
        coin_currency: @trade.coin_currency,
        fiat_currency: @trade.fiat_currency,
        coin_amount: @trade.coin_amount,
        fiat_amount: @trade.fiat_amount,
        price: @trade.price,
        payment_method: @trade.payment_method,
        taker_side: @trade.taker_side,
        buyer_id: @trade.buyer_id,
        seller_id: @trade.seller_id,
        created_at: @trade.created_at,
        updated_at: @trade.updated_at,
        paid_at: @trade.paid_at,
        released_at: @trade.released_at,
        cancelled_at: @trade.cancelled_at,
        disputed_at: @trade.disputed_at,
        expired_at: @trade.expired_at
      }
    }
  end
end
