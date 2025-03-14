# frozen_string_literal: true

module V1
  module Auth
    module Helpers
      module AuthHelper
        def fetch_facebook_auth
          graph = Koala::Facebook::API.new(params[:access_token])
          profile = graph.get_object('me', fields: 'id,name,email,picture')

          AuthHash.new(
            provider: 'facebook',
            uid: profile['id'],
            info: {
              email: profile['email'],
              name: profile['name'],
              image: profile.dig('picture', 'data', 'url')
            },
            credentials: {
              token: params[:access_token],
              expires_at: 60.days.from_now.to_i
            },
            extra: profile
          )
        rescue Koala::Facebook::AuthenticationError
          error!('Invalid Facebook access token', 422)
        end

        def fetch_google_auth
          validator = GoogleIDToken::Validator.new
          payload = validator.check(params[:id_token], ENV.fetch('GOOGLE_CLIENT_ID', nil))

          AuthHash.new(
            provider: 'google',
            uid: payload['sub'],
            info: {
              email: payload['email'],
              name: payload['name'],
              image: payload['picture']
            },
            credentials: {
              token: params[:id_token],
              expires_at: Time.zone.at(payload['exp'])
            },
            extra: payload
          )
        rescue GoogleIDToken::ValidationError => e
          error!("Invalid Google ID token: #{e.message}", 422)
        end

        def fetch_apple_auth
          token_data = AppleAuth.verify_identity_token(params[:identity_token])

          AuthHash.new(
            provider: 'apple',
            uid: token_data.sub,
            info: {
              email: token_data.email,
              name: params[:user]&.dig(:name),
              image: nil
            },
            credentials: {
              token: params[:identity_token],
              expires_at: token_data.exp
            },
            extra: token_data.to_h
          )
        rescue JWT::DecodeError
          error!('Invalid Apple identity token', 422)
        end

        def find_or_create_social_account(auth)
          social_account = SocialAccount.find_by(
            provider: auth.provider,
            provider_user_id: auth.uid
          )

          return validate_existing_account(social_account) if social_account

          create_new_account(auth)
        end

        private

        def validate_existing_account(social_account)
          user = social_account.user

          if params[:account_type] == 'merchant' && user.role != 'merchant'
            error!('You are not registered as a merchant', 422)
          elsif params[:account_type] == 'user' && user.role == 'merchant'
            error!('This account is registered as a merchant', 422)
          end

          social_account
        end

        def create_new_account(auth)
          user = ::User.find_or_initialize_by(email: auth.info.email)

          if user.new_record?
            create_new_user(user, auth)
          else
            validate_existing_user(user)
          end

          create_social_account(user, auth)
        end

        def create_new_user(user, auth)
          user.assign_attributes(
            display_name: auth.info.name,
            avatar_url: auth.info.image,
            role: params[:account_type] == 'merchant' ? 'merchant' : 'user',
            status: 'active',
            kyc_level: 0
          )
          user.save!
        end

        def validate_existing_user(user)
          if params[:account_type] == 'merchant' && user.role != 'merchant'
            error!('This email is already registered as a regular user', 422)
          elsif params[:account_type] == 'user' && user.role == 'merchant'
            error!('This email is already registered as a merchant', 422)
          end
        end

        def create_social_account(user, auth)
          SocialAccount.create!(
            user: user,
            provider: auth.provider,
            provider_user_id: auth.uid,
            email: auth.info.email,
            name: auth.info.name,
            access_token: auth.credentials.token,
            token_expires_at: auth.credentials.expires_at,
            avatar_url: auth.info.image,
            profile_data: auth.extra.raw_info
          )
        end

        def generate_jwt_token(user)
          secret = Rails.application.secret_key_base
          payload = {
            user_id: user.id,
            email: user.email,
            exp: 24.hours.from_now.to_i
          }
          JWT.encode(payload, secret, 'HS256')
        end
      end
    end
  end
end
