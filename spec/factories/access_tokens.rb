# frozen_string_literal: true

FactoryBot.define do
  factory :access_token do
    resource_owner_id { create(:user).id }
    token { SecureRandom.hex(32) }
    expires_in { 7200 }
    created_at { Time.current }
  end
end
