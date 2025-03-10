# frozen_string_literal: true

module V1
  module Auth
    class Api < Grape::API
      helpers V1::Auth::Helpers::AuthHelper

      resource :auth do
        desc 'Facebook authentication'
        params do
          requires :access_token, type: String, desc: 'Facebook access token'
          optional :account_type, type: String, values: %w[user merchant], default: 'user'
        end
        post :facebook do
          auth = fetch_facebook_auth
          social_account = find_or_create_social_account(auth)
          user = social_account.user
          token = generate_jwt_token(user)
          present({ token: token, user: user }, with: V1::Auth::Entity)
        rescue StandardError => e
          error!(e.message, 422)
        end

        desc 'Google authentication'
        params do
          requires :id_token, type: String, desc: 'Google ID token'
          optional :account_type, type: String, values: %w[user merchant], default: 'user'
        end
        post :google do
          auth = fetch_google_auth
          social_account = find_or_create_social_account(auth)
          user = social_account.user
          token = generate_jwt_token(user)
          present({ token: token, user: user }, with: V1::Auth::Entity)
        rescue StandardError => e
          error!(e.message, 422)
        end

        desc 'Apple authentication'
        params do
          requires :identity_token, type: String, desc: 'Apple identity token'
          optional :account_type, type: String, values: %w[user merchant], default: 'user'
          optional :user, type: Hash do
            optional :name, type: String
          end
        end
        post :apple do
          auth = fetch_apple_auth
          social_account = find_or_create_social_account(auth)
          user = social_account.user
          token = generate_jwt_token(user)
          present({ token: token, user: user }, with: V1::Auth::Entity)
        rescue StandardError => e
          error!(e.message, 422)
        end
      end
    end
  end
end
