# frozen_string_literal: true

# spec/factories/admin_users.rb
FactoryBot.define do
  factory :admin_user do
    email { Faker::Internet.unique.email }
    password { 'password123' }
    password_confirmation { 'password123' }
    fullname { 'Test User' }
    roles { 'superadmin' }
    authenticator_enabled { false }
    authenticator_key { nil }

    trait :operator do
      roles { 'operator' }
    end

    trait :superadmin do
      roles { 'superadmin' }
    end
  end
end
