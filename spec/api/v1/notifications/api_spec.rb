# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Notifications::Api, type: :request do
  describe 'GET /api/v1/notifications' do
    let(:user) { create(:user) }
    let(:token) { JsonWebToken.encode(user_id: user.id) }

    context 'when user is authenticated' do
      it 'returns paginated notifications' do
        notification = create(:notification, user: user)
        get '/api/v1/notifications',
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response).to eq(
          'status' => 'success',
          'data' => {
            'notifications' => [
              {
                'id' => notification.id,
                'title' => notification.title,
                'content' => notification.content,
                'type' => notification.notification_type,
                'read' => notification.read,
                'created_at' => notification.created_at.as_json
              }
            ],
            'pagination' => {
              'current_page' => 1,
              'total_pages' => 1,
              'total_count' => 1,
              'per_page' => 20
            }
          }
        )
      end

      it 'respects pagination parameters' do
        create_list(:notification, 2, user: user)
        get '/api/v1/notifications',
          params: { page: 1, per_page: 1 },
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response['data']['notifications'].size).to eq(1)
        expect(json_response['data']['pagination']['per_page']).to eq(1)
      end

      it 'returns empty list when no notifications exist' do
        get '/api/v1/notifications',
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response['data']['notifications']).to be_empty
        expect(json_response['data']['pagination']['total_count']).to eq(0)
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        get '/api/v1/notifications'

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end

  describe 'PUT /api/v1/notifications/:id/read' do
    let(:user) { create(:user) }
    let(:token) { JsonWebToken.encode(user_id: user.id) }

    context 'when user is authenticated' do
      it 'marks notification as read' do
        notification = create(:notification, user: user, read: false)
        put "/api/v1/notifications/#{notification.id}/read",
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response).to eq(
          'status' => 'success',
          'data' => {
            'id' => notification.id,
            'title' => notification.title,
            'content' => notification.content,
            'type' => notification.notification_type,
            'read' => true,
            'created_at' => notification.created_at.as_json
          }
        )
        expect(notification.reload.read).to be true
      end

      it 'returns 404 when notification not found' do
        put '/api/v1/notifications/999/read',
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:not_found)
        expect(json_response).to eq(
          'status' => 'error',
          'message' => 'Notification not found'
        )
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        put '/api/v1/notifications/1/read'

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end

  describe 'PUT /api/v1/notifications/mark_all_read' do
    let(:user) { create(:user) }
    let(:token) { JsonWebToken.encode(user_id: user.id) }

    context 'when user is authenticated' do
      it 'marks all notifications as read' do
        create_list(:notification, 2, user: user, read: false)
        put '/api/v1/notifications/mark_all_read',
          headers: { 'Authorization' => "Bearer #{token}" }

        expect(response).to have_http_status(:ok)
        expect(json_response).to eq(
          'status' => 'success',
          'message' => 'All notifications marked as read'
        )
        expect(user.notifications.where(read: false).count).to eq(0)
      end
    end

    context 'when user is not authenticated' do
      it 'returns unauthorized error' do
        put '/api/v1/notifications/mark_all_read'

        expect(response).to have_http_status(:unauthorized)
      end
    end
  end
end
