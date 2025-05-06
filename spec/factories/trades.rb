# frozen_string_literal: true

FactoryBot.define do
  factory :trade do
    association :buyer, factory: :user
    association :seller, factory: :user
    association :offer

    ref { "T#{Time.zone.today.strftime('%Y%m%d')}#{SecureRandom.hex(4).upcase}" }
    coin_currency { offer.coin_currency }
    fiat_currency { offer.currency }
    coin_amount { 0.01 }
    fiat_amount { coin_amount * price }
    price { offer.price }
    fee_ratio { 0.005 }
    coin_trading_fee { coin_amount * fee_ratio }
    payment_method { offer.payment_method.name }
    payment_details { offer.payment_details }
    taker_side { %w[buy sell].sample }
    status { 'awaiting' }

    # Ensure consistent data
    before(:create) do |trade|
      if trade.offer.offer_type == 'buy'
        trade.buyer = trade.offer.user
      else
        trade.seller = trade.offer.user
      end
    end

    trait :awaiting do
      status { 'awaiting' }
    end

    trait :unpaid do
      status { 'unpaid' }
    end

    trait :paid do
      status { 'paid' }
      paid_at { Time.current }
    end

    trait :disputed do
      status { 'disputed' }
      disputed_at { Time.current }
      dispute_reason { 'Payment not received' }
    end

    trait :released do
      status { 'released' }
      released_at { Time.current }
    end

    trait :cancelled do
      status { 'cancelled' }
      cancelled_at { Time.current }
    end

    trait :cancelled_automatically do
      status { 'cancelled_automatically' }
      cancelled_at { Time.current }
    end

    trait :with_payment_proof do
      has_payment_proof { true }
      payment_receipt_details do
        {
          'bank_name' => 'Test Bank',
          'transaction_id' => SecureRandom.hex(8),
          'amount' => fiat_amount.to_s,
          'date' => Time.current.to_s
        }
      end
    end

    trait :with_legit_proof do
      has_payment_proof { true }
      payment_proof_status { 'legit' }
    end

    trait :with_fake_proof do
      has_payment_proof { true }
      payment_proof_status { 'fake' }
    end

    trait :with_messages do
      after(:create) do |trade|
        create_list(:message, 3, trade: trade, user: trade.buyer)
        create_list(:message, 2, trade: trade, user: trade.seller)
      end
    end

    trait :with_fiat_deposit do
      after(:create) do |trade|
        create(:fiat_deposit, user: trade.seller, payable: trade)
      end
    end

    trait :with_fiat_withdrawal do
      after(:create) do |trade|
        create(:fiat_withdrawal, user: trade.buyer, withdrawable: trade)
      end
    end
  end
end
