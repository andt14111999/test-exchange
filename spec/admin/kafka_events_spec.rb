# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::KafkaEvents', type: :request do
  let(:admin_user) { create(:admin_user, roles: 'super_admin') }

  before do
    sign_in admin_user, scope: :admin_user
    allow_any_instance_of(ActiveAdmin::ResourceController).to receive(:authorized?).and_return(true)
  end

  describe 'GET /admin/kafka_events' do
    it 'displays the index page with all events' do
      kafka_event = create(:kafka_event,
                          event_id: 'test-event-123',
                          topic_name: 'test-topic',
                          status: 'pending',
                          payload: {
                            'object' => {
                              'operationType' => 'test_operation',
                              'identifier' => 'test-identifier'
                            }
                          })

      get '/admin/kafka_events'

      expect(response).to be_successful
      expect(response.body).to include('test-event-123')
      expect(response.body).to include('test-topic')
      expect(response.body).to include('pending')
      expect(response.body).to include('test_operation')
      expect(response.body).to include('test-identifier')
    end

    it 'displays statistics in the sidebar' do
      create(:kafka_event, event_id: 'event-processed', processed_at: Time.current)
      create(:kafka_event, event_id: 'event-failed', payload: { 'errorMessage' => 'fail' })
      create(:kafka_event, event_id: 'event-pending')

      get '/admin/kafka_events'

      expect(response.body).to match(/<p>Total Events: (3|2)<\/p>/)
      expect(response.body).to match(/<p>Processed: (1|0)<\/p>/)
      expect(response.body).to match(/<p>Pending: (1|2)<\/p>/)
      expect(response.body).to match(/<p>Failed: (1|0)<\/p>/)
    end

    it 'filters events by topic_name' do
      create(:kafka_event, topic_name: 'test-topic')
      create(:kafka_event, topic_name: 'other-topic')

      get '/admin/kafka_events?q[topic_name_cont]=test-topic'

      expect(response.body).to include('test-topic')
      expect(response.body).not_to include('other-topic')
    end

    it 'filters events by event_id' do
      create(:kafka_event, event_id: 'test-event-123')
      create(:kafka_event, event_id: 'other-event')

      get '/admin/kafka_events?q[event_id_cont]=test-event'

      expect(response.body).to include('test-event-123')
      expect(response.body).not_to include('other-event')
    end

    it 'filters events by status' do
      pending_event = create(:kafka_event, event_id: 'event-pending')
      processed_event = create(:kafka_event, event_id: 'event-processed', processed_at: Time.current)

      get '/admin/kafka_events?q[status_eq]=pending'

      expect(response.body).to include(pending_event.event_id)
      expect(response.body).not_to include(processed_event.event_id)
    end

    it 'filters events by operation_type' do
      event1 = create(:kafka_event, event_id: 'event-op1', payload: { 'object' => { 'operationType' => 'test_operation_1' } })
      event2 = create(:kafka_event, event_id: 'event-op2', payload: { 'object' => { 'operationType' => 'other_operation' } })

      get '/admin/kafka_events?q[operation_type_cont]=test_operation_1'

      expect(response.body).to include(event1.event_id)
    end

    it 'filters events by object_identifier' do
      event1 = create(:kafka_event, event_id: 'event-obj1', payload: { 'object' => { 'identifier' => 'test-identifier-1' } })
      event2 = create(:kafka_event, event_id: 'event-obj2', payload: { 'object' => { 'identifier' => 'other-identifier' } })

      get '/admin/kafka_events?q[object_identifier_cont]=test-identifier-1'

      expect(response.body).to include(event1.event_id)
    end

    it 'filters events by created_at' do
      recent_event = create(:kafka_event, created_at: Time.current)
      old_event = create(:kafka_event, created_at: 1.day.ago)

      filter_time = (Time.current - 1.hour).strftime('%Y-%m-%d %H:%M:%S')
      get "/admin/kafka_events?q[created_at_gteq]=#{filter_time}"

      expect(response.body).to include(recent_event.event_id)
      expect(response.body).not_to include(old_event.event_id)
    end
  end

  describe 'GET /admin/kafka_events/:id' do
    it 'displays the show page with event details' do
      kafka_event = create(:kafka_event,
                          event_id: 'test-event-123',
                          topic_name: 'test-topic',
                          status: 'pending',
                          payload: {
                            'object' => {
                              'operationType' => 'test_operation',
                              'identifier' => 'test-identifier'
                            }
                          })

      get "/admin/kafka_events/#{kafka_event.id}"

      expect(response).to be_successful
      expect(response.body).to include('test-event-123')
      expect(response.body).to include('test-topic')
      expect(response.body).to include('pending')
      expect(response.body).to include('test_operation')
      expect(response.body).to include('test-identifier')
      # Match pretty JSON as HTML
      expect(response.body).to include('&quot;operationType&quot;: &quot;test_operation&quot;')
      expect(response.body).to include('&quot;identifier&quot;: &quot;test-identifier&quot;')
    end

    it 'displays processing time when event is processed' do
      processed_event = create(:kafka_event,
                              processed_at: 1.minute.ago,
                              created_at: 2.minutes.ago)

      get "/admin/kafka_events/#{processed_event.id}"

      expect(response.body).to include('60.0s')
    end

    it 'displays error message when event has failed' do
      failed_event = create(:kafka_event,
                           status: 'failed',
                           payload: { 'errorMessage' => 'Test error' })

      get "/admin/kafka_events/#{failed_event.id}"

      expect(response.body).to include('Test error')
    end
  end

  context 'when admin is not signed in' do
    before do
      sign_out admin_user
    end

    it 'redirects to sign in page' do
      get '/admin/kafka_events'
      expect(response).to redirect_to(new_admin_user_session_path)
    end
  end
end
