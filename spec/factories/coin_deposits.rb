FactoryBot.define do
  factory :coin_deposit do
    association :coin_account, factory: :coin_account, coin_currency: 'btc', layer: 'bitcoin'
    coin_currency { coin_account.coin_currency }
    tx_hash { SecureRandom.hex(32) }
    out_index { 0 }
    coin_amount { 1.0 }
    coin_fee { 0 }
    confirmations_count { 1 }
    required_confirmations_count { 3 }
    status { 'pending' }

    trait :eth do
      association :coin_account, factory: :coin_account, coin_currency: 'eth', layer: 'erc20'
      coin_currency { 'eth' }
    end

    trait :verified do
      status { 'verified' }
      verified_at { Time.current }
    end

    trait :locked do
      status { 'locked' }
    end

    trait :rejected do
      status { 'rejected' }
    end

    trait :forged do
      status { 'forged' }
    end
  end
end
