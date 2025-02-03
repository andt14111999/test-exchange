# frozen_string_literal: true

# spec/factories/admin_users.rb
FactoryBot.define do
  factory :admin_user do
    email { Faker::Internet.unique.email }
    password { 'password123' }
    password_confirmation { 'password123' }
    fullname { 'Test User' }
    roles { 'developer' }
    authenticator_enabled { false }
    authenticator_key { nil }

    trait :developer do
      roles { 'developer' }
    end

    trait :implementor do
      roles { 'implementor' }
    end

    trait :admin do
      roles { 'admin' }
    end

    trait :explorer do
      roles { 'explorer' }
    end
  end
end
