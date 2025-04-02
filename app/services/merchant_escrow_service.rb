# frozen_string_literal: true

class MerchantEscrowService
  def self.create_escrow(user:, usdt_amount:, fiat_amount:, fiat_currency:)
    escrow = MerchantEscrow.new(
      user: user,
      usdt_amount: usdt_amount,
      fiat_amount: fiat_amount,
      fiat_currency: fiat_currency
    )

    escrow.save!
    escrow
  end
end
