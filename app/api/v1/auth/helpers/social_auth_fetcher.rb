# frozen_string_literal: true

module V1
  module Auth
    module Helpers
      module SocialAuthFetcher
        def fetch_facebook_auth
          graph = Koala::Facebook::API.new(params[:access_token])
          profile = graph.get_object('me', fields: 'id,name,email,picture')

          AuthHash.new(
            provider: 'facebook',
            uid: profile['id'],
            info: facebook_info(profile),
            credentials: facebook_credentials,
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
            info: google_info(payload),
            credentials: google_credentials(payload),
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
            info: apple_info(token_data),
            credentials: apple_credentials(token_data),
            extra: token_data.to_h
          )
        rescue JWT::DecodeError
          error!('Invalid Apple identity token', 422)
        end

        private

        def facebook_info(profile)
          {
            email: profile['email'],
            name: profile['name'],
            image: profile.dig('picture', 'data', 'url')
          }
        end

        def facebook_credentials
          {
            token: params[:access_token],
            expires_at: 60.days.from_now.to_i
          }
        end

        def google_info(payload)
          {
            email: payload['email'],
            name: payload['name'],
            image: payload['picture']
          }
        end

        def google_credentials(payload)
          {
            token: params[:id_token],
            expires_at: Time.zone.at(payload['exp'])
          }
        end

        def apple_info(token_data)
          {
            email: token_data.email,
            name: params[:user]&.dig(:name),
            image: nil
          }
        end

        def apple_credentials(token_data)
          {
            token: params[:identity_token],
            expires_at: token_data.exp
          }
        end
      end
    end
  end
end
