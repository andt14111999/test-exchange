# frozen_string_literal: true

FactoryBot.define do
  factory :kafka_event do
    event_id { SecureRandom.uuid }
    topic_name { 'test_topic' }
    status { 'pending' }
    payload { { 'data' => 'test' } }
    created_at { Time.current }
    updated_at { Time.current }

    trait :processed do
      status { 'processed' }
      processed_at { Time.current }
    end

    trait :failed do
      status { 'failed' }
      payload { { 'errorMessage' => 'Test error' } }
    end
  end
end
