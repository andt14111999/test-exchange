FactoryBot.define do
  factory :coin_deposit do
    coin_account
    coin_currency { coin_account.coin_currency }
    tx_hash { SecureRandom.hex(32) }
    out_index { 0 }
    coin_amount { 1.0 }
    confirmations_count { 1 }
    required_confirmations_count { 3 }
    status { 'pending' }

    trait :confirmed do
      confirmations_count { 3 }
      status { 'confirmed' }
    end

    trait :failed do
      status { 'failed' }
    end
  end
end
