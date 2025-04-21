# frozen_string_literal: true

FactoryBot.define do
  factory :coin_withdrawal do
    transient do
      account_balance { 100.0 }
    end

    user
    coin_currency { 'usdt' }
    coin_layer { 'erc20' }
    coin_address { '0x0000000000000000000000000000000000000000' }
    coin_amount { 1.0 }
    coin_fee { 0.1 }
    status { 'pending' }
    tx_hash { nil }

    after(:build) do |withdrawal, evaluator|
      unless withdrawal.user.coin_accounts.exists?(coin_currency: withdrawal.coin_currency)
        create(:coin_account, :usdt_main,
          user: withdrawal.user,
          balance: evaluator.account_balance
        )
      end
    end
  end
end
