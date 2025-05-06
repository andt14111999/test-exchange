# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_withdrawal do
    association :user
    currency { 'VND' }
    country_code { 'us' }
    fiat_amount { 100.0 }
    fee { 1.0 }
    amount_after_transfer_fee { 99.0 }
    bank_name { 'Test Bank' }
    bank_account_name { 'John Doe' }
    bank_account_number { '1234567890' }
    bank_branch { 'Main Branch' }
    status { 'pending' }
    verification_status { 'unverified' }
    retry_count { 0 }
    verification_attempts { 0 }

    # Create fiat_account after defining currency
    fiat_account { association :fiat_account, user: user, balance: 1000.0, currency: currency }

    # Main status traits
    trait :pending do
      status { 'pending' }
    end

    trait :processing do
      status { 'processing' }
    end

    trait :processed do
      status { 'processed' }
      processed_at { Time.zone.now }
    end

    trait :cancelled do
      status { 'cancelled' }
      cancelled_at { Time.zone.now }
      cancel_reason { 'Cancelled by user' }
    end

    trait :bank_pending do
      status { 'bank_pending' }
    end

    trait :bank_sent do
      status { 'bank_sent' }
    end

    trait :bank_rejected do
      status { 'bank_rejected' }
      error_message { 'Rejected by bank' }
    end

    # Verification status traits
    trait :unverified do
      verification_status { 'unverified' }
    end

    trait :verifying do
      verification_status { 'verifying' }
    end

    trait :verified do
      verification_status { 'verified' }
    end

    trait :verification_failed do
      verification_status { 'failed' }
      error_message { 'Verification failed' }
    end

    # Other traits
    trait :with_errors do
      error_message { 'Some error occurred' }
    end

    trait :for_trade do
      association :withdrawable, factory: :trade
      withdrawable_type { 'Trade' }
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
