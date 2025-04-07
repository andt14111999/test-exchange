FactoryBot.define do
  factory :merchant_escrow_operation do
    merchant_escrow
    operation_type { 'freeze' }
    usdt_amount { 100.0 }
    fiat_amount { 100.0 }
    fiat_currency { FiatAccount::SUPPORTED_CURRENCIES.keys.first }
    status { 'pending' }

    before(:create) do |operation|
      if operation.usdt_account.nil?
        operation.usdt_account = create(:coin_account, :usdt_main, user: operation.merchant_escrow.user, balance: 200.0)
      end

      if operation.fiat_account.nil?
        operation.fiat_account = operation.merchant_escrow.fiat_account
      end
    end
  end
end
