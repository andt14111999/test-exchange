# frozen_string_literal: true

FactoryBot.define do
  factory :coin_withdrawal do
    transient do
      account_balance { 100.0 }
    end

    user
    coin_currency { 'btc' }
    coin_layer { 'bitcoin' }
    coin_address { '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa' }
    coin_amount { 1.0 }
    coin_fee { 0.1 }
    status { 'pending' }
    tx_hash { nil }

    after(:build) do |withdrawal, evaluator|
      unless withdrawal.user.coin_accounts.exists?(coin_currency: withdrawal.coin_currency)
        create(:coin_account, :btc_main,
          user: withdrawal.user,
          balance: evaluator.account_balance
        )
      end
    end
  end
end
