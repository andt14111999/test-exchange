# frozen_string_literal: true

FactoryBot.define do
  factory :payment_method do
    sequence(:name) { |n| "payment_method_#{n}" }
    display_name { "Payment Method #{name.capitalize}" }
    description { "Description for #{display_name}" }
    country_code { 'vn' }
    enabled { true }
    fields_required do
      {
        'account_number' => { 'type' => 'string', 'required' => true },
        'account_name' => { 'type' => 'string', 'required' => true }
      }
    end
    icon_url { 'https://example.com/payment_icon.png' }

    trait :bank_transfer do
      name { 'bank_transfer' }
      display_name { 'Bank Transfer' }
      description { 'Standard bank transfer' }
      fields_required do
        {
          'bank_name' => { 'type' => 'string', 'required' => true },
          'account_number' => { 'type' => 'string', 'required' => true },
          'account_name' => { 'type' => 'string', 'required' => true },
          'branch' => { 'type' => 'string', 'required' => false }
        }
      end
    end

    trait :mobile_money do
      name { 'mobile_money' }
      display_name { 'Mobile Money' }
      description { 'Transfer using mobile money services' }
      fields_required do
        {
          'phone_number' => { 'type' => 'string', 'required' => true },
          'provider' => { 'type' => 'string', 'required' => true }
        }
      end
    end

    trait :cash do
      name { 'cash' }
      display_name { 'Cash Deposit' }
      description { 'Physical cash deposit at bank branches' }
      fields_required do
        {
          'bank_name' => { 'type' => 'string', 'required' => true },
          'deposit_branch' => { 'type' => 'string', 'required' => false }
        }
      end
    end

    trait :disabled do
      enabled { false }
    end

    trait :vietnam do
      country_code { 'vn' }
    end

    trait :philippines do
      country_code { 'ph' }
    end

    trait :nigeria do
      country_code { 'ng' }
    end
  end
end
