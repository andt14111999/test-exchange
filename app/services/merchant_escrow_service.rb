# frozen_string_literal: true

class MerchantEscrowService
  def self.create_escrow(user:, usdt_amount:, fiat_amount:, fiat_currency:)
    RedisMutex.with_lock("merchant_escrow:#{user.id}", block: 10, expire: 20) do
      ActiveRecord::Base.transaction do
        escrow = MerchantEscrow.create!(
          user: user,
          usdt_amount: usdt_amount,
          fiat_amount: fiat_amount,
          fiat_currency: fiat_currency
        )

        escrow.process!
        escrow.complete!
        escrow
      end
    end
  rescue ActiveRecord::RecordInvalid, AASM::InvalidTransition => e
    Rails.logger.error("Failed to create merchant escrow: #{e.message}")
    raise
  end
end
