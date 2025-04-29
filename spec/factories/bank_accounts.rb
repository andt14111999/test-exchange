# frozen_string_literal: true

FactoryBot.define do
  factory :bank_account do
    association :user
    bank_name { 'Vietcombank' }
    account_name { Faker::Name.name }
    account_number { Faker::Bank.account_number(digits: 10) }
    branch { Faker::Address.city }
    country_code { 'vn' }
    verified { false }
    is_primary { false }

    trait :verified do
      verified { true }
    end

    trait :primary do
      is_primary { true }
    end

    trait :vietnam do
      country_code { 'vn' }
      bank_name { [ 'Vietcombank', 'BIDV', 'Vietinbank', 'Techcombank' ].sample }
    end

    trait :philippines do
      country_code { 'ph' }
      bank_name { [ 'BDO', 'BPI', 'Metrobank', 'UnionBank' ].sample }
    end

    trait :nigeria do
      country_code { 'ng' }
      bank_name { [ 'GTBank', 'Access Bank', 'First Bank', 'UBA' ].sample }
    end
  end
end
