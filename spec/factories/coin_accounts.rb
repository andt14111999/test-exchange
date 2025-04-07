# frozen_string_literal: true

FactoryBot.define do
  factory :coin_account do
    user
    coin_currency { 'btc' }
    layer { 'bitcoin' }
    balance { 10.0 }
    frozen_balance { 0.0 }
    account_type { 'deposit' }
    address { '0x1234567890abcdef' }

    trait :main do
      account_type { 'main' }
      layer { 'all' }
    end

    trait :deposit do
      account_type { 'deposit' }
      layer { 'bitcoin' }
    end

    trait :with_balance do
      balance { 100.0 }
      frozen_balance { 0.0 }
    end

    trait :with_frozen_balance do
      balance { 100.0 }
      frozen_balance { 30.0 }
    end

    trait :eth do
      coin_currency { 'eth' }
      layer { 'erc20' }
    end

    trait :bnb do
      coin_currency { 'bnb' }
      layer { 'bep20' }
    end

    trait :usdt_erc20 do
      coin_currency { 'usdt' }
      layer { 'erc20' }
      account_type { 'deposit' }
    end

    trait :usdt_trc20 do
      coin_currency { 'usdt' }
      layer { 'trc20' }
      account_type { 'deposit' }
    end

    trait :usdt_bep20 do
      coin_currency { 'usdt' }
      layer { 'bep20' }
      account_type { 'deposit' }
    end

    trait :usdt_main do
      coin_currency { 'usdt' }
      layer { 'all' }
      account_type { 'main' }
    end

    trait :with_deposits do
      after(:create) do |account|
        create_list(:coin_deposit, 2, coin_account: account)
      end
    end

    after(:build) do |account|
      # Ensure unique layer for each user and coin_currency
      if CoinAccount.exists?(user_id: account.user_id, coin_currency: account.coin_currency, layer: account.layer)
        # Find a supported layer that hasn't been used yet
        supported_layers = CoinAccount::SUPPORTED_NETWORKS[account.coin_currency] || []
        unused_layer = supported_layers.find { |l| !CoinAccount.exists?(user_id: account.user_id, coin_currency: account.coin_currency, layer: l) }
        account.layer = unused_layer if unused_layer
      end
    end
  end
end
