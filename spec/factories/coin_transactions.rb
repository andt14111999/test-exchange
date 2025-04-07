FactoryBot.define do
  factory :coin_transaction do
    amount { 100 }
    coin_currency { 'usdt' }
    transaction_type { 'transfer' }
    association :coin_account, factory: [ :coin_account, :main ]
    operation { nil }

    trait :with_withdrawal_operation do
      transaction_type { 'withdrawal' }
      association :operation, factory: :coin_withdrawal_operation
    end
  end
end
