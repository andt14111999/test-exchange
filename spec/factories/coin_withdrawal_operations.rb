FactoryBot.define do
  factory :coin_withdrawal_operation do
    coin_withdrawal
    coin_currency { 'btc' }
    coin_amount { 1.0 }
    coin_fee { 0.1 }
    status { 'pending' }
    withdrawal_status { nil }
    tx_hash { nil }

    after(:build) do |operation|
      operation.status = 'pending'
    end
  end
end
