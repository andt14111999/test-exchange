# frozen_string_literal: true

FactoryBot.define do
  factory :site_country do
    sequence(:country_code) { |n| "country_#{n}" }
    name { "Country #{country_code.capitalize}" }
    currency { 'USD' }
    timezone { 'UTC' }
    enabled { true }
    min_trade_fiat { 10 }
    max_trade_fiat { 10000 }
    max_total_amount_of_offer_for_fiat_token { 100000 }
    supported_banks do
      {
        'Default Bank' => {
          'description' => 'Default bank for testing',
          'code' => 'DFLT'
        }
      }
    end
    supported_payment_methods { [ 'bank_transfer' ] }

    trait :vietnam do
      country_code { 'vn' }
      name { 'Vietnam' }
      currency { 'VND' }
      timezone { 'Asia/Ho_Chi_Minh' }
      min_trade_fiat { 100_000 }
      max_trade_fiat { 100_000_000 }
      max_total_amount_of_offer_for_fiat_token { 500_000_000 }
      supported_banks do
        {
          'Vietcombank' => { 'code' => 'VCB', 'description' => 'Vietcombank' },
          'BIDV' => { 'code' => 'BIDV', 'description' => 'BIDV' },
          'Techcombank' => { 'code' => 'TCB', 'description' => 'Techcombank' },
          'Vietinbank' => { 'code' => 'VTB', 'description' => 'Vietinbank' }
        }
      end
      supported_payment_methods { %w[bank_transfer cash] }
    end

    trait :philippines do
      country_code { 'ph' }
      name { 'Philippines' }
      currency { 'PHP' }
      timezone { 'Asia/Manila' }
      min_trade_fiat { 500 }
      max_trade_fiat { 500_000 }
      max_total_amount_of_offer_for_fiat_token { 2_000_000 }
      supported_banks do
        {
          'BDO' => { 'code' => 'BDO', 'description' => 'Banco de Oro' },
          'BPI' => { 'code' => 'BPI', 'description' => 'Bank of the Philippine Islands' },
          'Metrobank' => { 'code' => 'MTB', 'description' => 'Metropolitan Bank and Trust Company' },
          'UnionBank' => { 'code' => 'UBP', 'description' => 'Union Bank of the Philippines' }
        }
      end
      supported_payment_methods { %w[bank_transfer mobile_money] }
    end

    trait :nigeria do
      country_code { 'ng' }
      name { 'Nigeria' }
      currency { 'NGN' }
      timezone { 'Africa/Lagos' }
      min_trade_fiat { 5_000 }
      max_trade_fiat { 5_000_000 }
      max_total_amount_of_offer_for_fiat_token { 20_000_000 }
      supported_banks do
        {
          'GTBank' => { 'code' => 'GTB', 'description' => 'Guaranty Trust Bank' },
          'Access Bank' => { 'code' => 'ACC', 'description' => 'Access Bank' },
          'First Bank' => { 'code' => 'FBN', 'description' => 'First Bank of Nigeria' },
          'UBA' => { 'code' => 'UBA', 'description' => 'United Bank for Africa' }
        }
      end
      supported_payment_methods { %w[bank_transfer mobile_money] }
    end

    trait :disabled do
      enabled { false }
    end
  end
end
