# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_withdrawal do
    association :user
    association :fiat_account

    currency { fiat_account.currency }
    country_code { 'vn' }
    fiat_amount { 10_000_000 }
    fee { fiat_amount * 0.01 }
    amount_after_transfer_fee { fiat_amount - fee }
    bank_name { 'Vietcombank' }
    bank_account_name { 'John Doe' }
    bank_account_number { '1234567890' }
    bank_branch { 'Ho Chi Minh City' }
    status { 'pending' }
    retry_count { 0 }

    trait :pending do
      status { 'pending' }
    end

    trait :processing do
      status { 'processing' }
    end

    trait :processed do
      status { 'processed' }
      processed_at { Time.current }
    end

    trait :cancelled do
      status { 'cancelled' }
      cancelled_at { Time.current }
      cancel_reason { 'User cancelled the withdrawal' }
    end

    trait :bank_pending do
      status { 'bank_pending' }
    end

    trait :bank_sent do
      status { 'bank_sent' }
    end

    trait :bank_rejected do
      status { 'bank_rejected' }
      error_message { 'Invalid bank account information' }
      retry_count { 3 }
    end

    trait :with_error do
      error_message { 'Technical issue during processing' }
      retry_count { 2 }
    end

    trait :for_trade do
      association :withdrawable, factory: :trade
    end

    trait :vietnam do
      country_code { 'vn' }
      currency { 'VND' }
      bank_name { [ 'Vietcombank', 'BIDV', 'Vietinbank', 'Techcombank' ].sample }
    end

    trait :philippines do
      country_code { 'ph' }
      currency { 'PHP' }
      bank_name { [ 'BDO', 'BPI', 'Metrobank', 'UnionBank' ].sample }
    end

    trait :nigeria do
      country_code { 'ng' }
      currency { 'NGN' }
      bank_name { [ 'GTBank', 'Access Bank', 'First Bank', 'UBA' ].sample }
    end
  end
end
