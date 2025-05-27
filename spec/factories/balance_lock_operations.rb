# frozen_string_literal: true

FactoryBot.define do
  factory :balance_lock_operation do
    balance_lock
    operation_type { 'lock' }
    status { 'pending' }

    trait :lock do
      operation_type { 'lock' }
    end

    trait :release do
      operation_type { 'release' }
    end

    trait :pending do
      status { 'pending' }
    end

    trait :processing do
      status { 'processing' }
    end

    trait :completed do
      status { 'completed' }
    end

    trait :failed do
      status { 'failed' }
      status_explanation { 'Insufficient balance' }
    end
  end
end
