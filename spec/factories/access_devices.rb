# frozen_string_literal: true

FactoryBot.define do
  factory :access_device do
    user
    device_uuid_hash { Digest::SHA256.hexdigest(SecureRandom.uuid) }
    first_device { false }
    trusted { false }
    details do
      {
        device_type: 'web',
        browser: 'Chrome',
        os: 'macOS',
        ip: '192.168.1.1',
        country: 'Vietnam',
        city: 'Ho Chi Minh City'
      }
    end

    # Helper method to set device_uuid directly
    transient do
      device_uuid { nil }
    end

    after(:build) do |access_device, evaluator|
      if evaluator.device_uuid
        access_device.device_uuid = evaluator.device_uuid
      end
    end

    trait :trusted do
      trusted { true }
    end

    trait :first_device do
      first_device { true }
    end

    trait :aged_trusted do
      trusted { true }
      created_at { 73.hours.ago }
    end

    trait :mobile do
      details do
        {
          device_type: 'ios',
          browser: 'Mobile Safari',
          os: 'iOS',
          ip: '192.168.1.2',
          country: 'Vietnam',
          city: 'Ho Chi Minh City'
        }
      end
    end
  end
end
