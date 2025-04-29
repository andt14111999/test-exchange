# frozen_string_literal: true

FactoryBot.define do
  factory :message do
    association :trade
    association :user
    body { Faker::Lorem.paragraph }
    is_receipt_proof { false }
    is_system { false }

    trait :receipt_proof do
      is_receipt_proof { true }
      body { 'I have completed the payment. Please find the receipt attached.' }
    end

    trait :system_message do
      is_system { true }
      body { 'System notification: The trade has been marked as paid.' }
    end
  end
end
