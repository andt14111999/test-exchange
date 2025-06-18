# frozen_string_literal: true

FactoryBot.define do
  factory :coin_withdrawal do
    transient do
      account_balance { 100.0 }
    end

    user
    coin_currency { 'usdt' }
    coin_layer { 'erc20' }
    coin_address { '0xde709f2102306220921060314715629080e2fb77' }
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

    trait :internal do
      coin_address { nil }
      coin_layer { nil }
      coin_fee { 0.0 }

      transient do
        receiver_email { nil }
        receiver_username { nil }
        receiver_phone_number { nil }
      end

      after(:build) do |withdrawal, evaluator|
        if evaluator.receiver_email.present?
          withdrawal.receiver_email = evaluator.receiver_email
        elsif evaluator.receiver_username.present?
          withdrawal.receiver_username = evaluator.receiver_username
        elsif evaluator.receiver_phone_number.present?
          withdrawal.receiver_phone_number = evaluator.receiver_phone_number
        else
          # Default to email if no specific identifier is provided
          receiver = create(:user)
          withdrawal.receiver_email = receiver.email
        end
      end
    end

    trait :internal_email do
      internal
      transient do
        receiver_email { create(:user).email }
      end
    end

    trait :internal_username do
      internal
      transient do
        receiver_username { create(:user).username }
      end
    end

    trait :internal_phone do
      internal
      transient do
        receiver_phone_number { create(:user, phone_number: '1234567890').phone_number }
      end
    end
  end
end
