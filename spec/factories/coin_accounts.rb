# frozen_string_literal: true

FactoryBot.define do
  factory :coin_account do
    user
    coin_currency { 'eth' }
    layer { 'erc20' }
    balance { 0 }
    frozen_balance { 0 }
    account_type { 'deposit' }

    after(:build) do |account|
      if account.user.present? && account.coin_currency.present?
        existing_account = CoinAccount.find_by(
          user_id: account.user_id,
          coin_currency: account.coin_currency,
          layer: account.layer,
          account_type: account.account_type
        )

        account.layer = "#{account.layer}_#{Time.current.to_i}" if existing_account.present?
      end
    end

    trait :main do
      layer { 'all' }
      account_type { 'main' }
    end

    trait :deposit do
      layer { 'erc20' }
      account_type { 'deposit' }
    end

    trait :with_balance do
      balance { 100 }
    end

    trait :eth do
      coin_currency { 'eth' }
      layer { 'erc20' }
    end

    trait :eth_main do
      coin_currency { 'eth' }
      layer { 'all' }
      account_type { 'main' }
    end

    trait :bnb do
      coin_currency { 'bnb' }
      layer { 'bep20' }
    end

    trait :bnb_main do
      coin_currency { 'bnb' }
      layer { 'all' }
      account_type { 'main' }
    end

    trait :btc do
      coin_currency { 'btc' }
      layer { 'bitcoin' }
    end

    trait :btc_main do
      coin_currency { 'btc' }
      layer { 'all' }
      account_type { 'main' }
    end

    trait :btc_deposit do
      coin_currency { 'btc' }
      layer { 'bitcoin' }
      account_type { 'deposit' }
    end

    trait :usdt do
      coin_currency { 'usdt' }
      layer { 'erc20' }
    end

    trait :usdt_main do
      coin_currency { 'usdt' }
      layer { 'all' }
      account_type { 'main' }
    end

    trait :usdt_erc20 do
      coin_currency { 'usdt' }
      layer { 'erc20' }
      account_type { 'deposit' }
    end

    trait :usdt_bep20 do
      coin_currency { 'usdt' }
      layer { 'bep20' }
      account_type { 'deposit' }
    end

    trait :usdt_trc20 do
      coin_currency { 'usdt' }
      layer { 'trc20' }
      account_type { 'deposit' }
    end

    trait :with_deposits do
      after(:create) do |account|
        create_list(:coin_deposit, 2, coin_account: account)
      end
    end
  end
end
