# frozen_string_literal: true

module V1
  class ApiKeys < Grape::API
    helpers V1::Helpers::AuthHelper

    namespace :api_keys do
      desc 'Create a new API key'
      params do
        requires :name, type: String, desc: 'Name of the API key'
      end
      post do
        authenticate_user!

        api_key = current_user.api_keys.create!(
          name: params[:name]
        )

        {
          status: 'success',
          data: {
            id: api_key.id,
            name: api_key.name,
            access_key: api_key.access_key,
            secret_key: api_key.secret_key,
            created_at: api_key.created_at
          }
        }
      end

      desc 'List all API keys'
      get do
        authenticate_user!

        api_keys = current_user.api_keys.order(created_at: :desc)

        {
          status: 'success',
          data: api_keys.map do |api_key|
            {
              id: api_key.id,
              name: api_key.name,
              access_key: api_key.access_key,
              last_used_at: api_key.last_used_at,
              created_at: api_key.created_at
            }
          end
        }
      end

      desc 'Revoke an API key'
      params do
        requires :id, type: Integer, desc: 'ID of the API key'
      end
      delete ':id' do
        authenticate_user!

        api_key = current_user.api_keys.find(params[:id])
        api_key.update!(revoked_at: Time.current)

        {
          status: 'success',
          message: 'API key revoked successfully'
        }
      end
    end
  end
end
