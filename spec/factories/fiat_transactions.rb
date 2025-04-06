# frozen_string_literal: true

FactoryBot.define do
  factory :fiat_transaction do
    amount { 100.0 }
    transaction_type { 'mint' }
    created_at { Time.zone.now }
    updated_at { Time.zone.now }

    after(:build) do |fiat_transaction|
      user = create(:user)
      fiat_account = create(:fiat_account, user: user, currency: 'VND')
      fiat_transaction.fiat_account = fiat_account
      fiat_transaction.currency = fiat_account.currency
    end
  end
end
