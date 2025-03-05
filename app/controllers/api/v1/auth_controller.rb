# frozen_string_literal: true

module Api
  module V1
    class AuthController < Api::V1::BaseController
      respond_to :json

      def facebook
        validate_params!(:access_token) do
          auth = fetch_facebook_auth
          handle_social_auth(auth)
        end
      end

      def google
        validate_params!(:id_token, :account_type) do
          auth = fetch_google_auth
          handle_social_auth(auth)
        end
      end

      def apple
        validate_params!(:identity_token) do
          auth = fetch_apple_auth
          handle_social_auth(auth)
        end
      end

      private

      def validate_params!(*required_params)
        missing_params = required_params.select { |param| params[param].blank? }

        if missing_params.any?
          render json: { error: "#{missing_params.join(', ')} are required" }, status: :unprocessable_entity
          return
        end

        yield if block_given?
      rescue StandardError => e
        handle_error(e)
      end

      def handle_social_auth(auth)
        social_account = find_or_create_social_account(auth)
        user = social_account.user
        token = generate_jwt_token(user)

        render json: {
          token: token,
          user: user.as_json(
            only: %i[id email display_name avatar_url role status kyc_level],
            methods: %i[phone_verified document_verified]
          )
        }
      end

      def find_or_create_social_account(auth)
        social_account = SocialAccount.find_by(
          provider: auth.provider,
          provider_user_id: auth.uid
        )

        return validate_existing_account(social_account) if social_account

        create_new_account(auth)
      end

      def validate_existing_account(social_account)
        user = social_account.user

        if params[:account_type] == 'merchant' && user.role != 'merchant'
          raise StandardError, 'You are not registered as a merchant'
        elsif params[:account_type] == 'user' && user.role == 'merchant'
          raise StandardError, 'This account is registered as a merchant'
        end

        social_account
      end

      def create_new_account(auth)
        user = User.find_or_initialize_by(email: auth.info.email)

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
          raise StandardError, 'This email is already registered as a regular user'
        elsif params[:account_type] == 'user' && user.role == 'merchant'
          raise StandardError, 'This email is already registered as a merchant'
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
        raise StandardError, 'Invalid Facebook access token'
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
        raise StandardError, "Invalid Google ID token: #{e.message}"
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
        raise StandardError, 'Invalid Apple identity token'
      end

      def handle_error(error)
        Rails.logger.error "Error in auth: #{error.class} - #{error.message}"
        Rails.logger.error error.backtrace.join("\n")
        render json: { error: error.message }, status: :unprocessable_entity
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
