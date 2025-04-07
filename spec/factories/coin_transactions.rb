FactoryBot.define do
  factory :coin_transaction do
    amount { 100 }
    coin_currency { 'usdt' }
    transaction_type { 'transfer' }
    association :coin_account, factory: [ :coin_account, :main ]
    operation { nil }
  end
end
