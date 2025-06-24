# frozen_string_literal: true

FactoryBot.define do
  factory :access_device do
    user
    device_uuid_hash { Digest::MD5.hexdigest(SecureRandom.uuid) }
    first_device { false }
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

    trait :trusted do
      first_device { true }
    end

    trait :aged_trusted do
      created_at { 3.days.ago }
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
