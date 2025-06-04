# frozen_string_literal: true

class MerchantEscrowBroadcastService
  def self.call(merchant_escrow)
    new(merchant_escrow).call
  end

  def initialize(merchant_escrow)
    @merchant_escrow = merchant_escrow
  end

  def call
    success = broadcast_merchant_escrow

    if !success && @merchant_escrow.respond_to?(:delivered)
      @merchant_escrow.update(delivered: false)
    end

    success
  end

  private

  def broadcast_merchant_escrow
    begin
      MerchantEscrowChannel.broadcast_to_merchant_escrow(@merchant_escrow, merchant_escrow_data)
      @merchant_escrow.update(delivered: true) if @merchant_escrow.respond_to?(:delivered)
      true
    rescue => e
      Rails.logger.error "Error broadcasting merchant escrow: #{e.message}"
      false
    end
  end

  def merchant_escrow_data
    {
      status: 'success',
      data: {
        id: @merchant_escrow.id,
        user_id: @merchant_escrow.user_id,
        usdt_account_id: @merchant_escrow.usdt_account_id,
        fiat_account_id: @merchant_escrow.fiat_account_id,
        usdt_amount: @merchant_escrow.usdt_amount,
        fiat_amount: @merchant_escrow.fiat_amount,
        fiat_currency: @merchant_escrow.fiat_currency,
        exchange_rate: @merchant_escrow.exchange_rate,
        status: @merchant_escrow.status,
        created_at: @merchant_escrow.created_at,
        updated_at: @merchant_escrow.updated_at
      }
    }
  end
end
