# frozen_string_literal: true

FactoryBot.define do
  factory :notification do
    user
    title { 'Test Notification' }
    content { 'This is a test notification' }
    notification_type { 'info' }
    read { false }
  end
end
