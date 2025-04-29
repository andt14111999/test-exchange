# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_deposit do
    association :user
    association :fiat_account

    currency { fiat_account.currency }
    country_code { 'vn' }
    fiat_amount { 10_000_000 }
    original_fiat_amount { fiat_amount }
    deposit_fee { fiat_amount * 0.01 }
    memo { "Transfer #{SecureRandom.hex(6).upcase}" }
    fiat_deposit_details do
      {
        'bank_account' => 'Test Bank Account',
        'account_number' => '1234567890',
        'account_name' => 'Exchange Bank Account',
        'bank_name' => 'Test Bank'
      }
    end
    status { 'awaiting' }

    trait :with_explorer_ref do
      explorer_ref { SecureRandom.hex(12) }
    end

    trait :with_ownership_proof do
      ownership_proof_url { 'https://example.com/proof.jpg' }
      sender_name { 'John Doe' }
      sender_account_number { '9876543210' }
    end

    trait :awaiting do
      status { 'awaiting' }
    end

    trait :pending do
      status { 'pending' }
    end

    trait :ready do
      status { 'ready' }
    end

    trait :informed do
      status { 'informed' }
      money_sent_at { Time.current }
    end

    trait :verifying do
      status { 'verifying' }
      money_sent_at { Time.current }
    end

    trait :processed do
      status { 'processed' }
      processed_at { Time.current }
      money_sent_at { Time.current - 1.hour }
    end

    trait :cancelled do
      status { 'cancelled' }
      cancelled_at { Time.current }
      cancel_reason { 'User cancelled the deposit' }
    end

    trait :illegal do
      status { 'illegal' }
      cancel_reason { 'Suspicious transaction' }
    end

    trait :refunding do
      status { 'refunding' }
    end

    trait :refunded do
      status { 'refunded' }
    end

    trait :for_trade do
      association :payable, factory: :trade
    end

    trait :vietnam do
      country_code { 'vn' }
      currency { 'VND' }
    end

    trait :philippines do
      country_code { 'ph' }
      currency { 'PHP' }
    end

    trait :nigeria do
      country_code { 'ng' }
      currency { 'NGN' }
    end
  end
end
