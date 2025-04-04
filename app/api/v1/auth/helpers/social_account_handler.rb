# frozen_string_literal: true

module V1
  module Auth
    module Helpers
      module SocialAccountHandler
        def find_or_create_social_account(auth)
          social_account = SocialAccount.find_by(
            provider: auth.provider,
            provider_user_id: auth.uid
          )

          return validate_existing_account(social_account) if social_account

          create_new_account(auth)
        end

        def validate_existing_account(social_account)
          social_account
        end

        def create_new_account(auth)
          user = ::User.find_or_initialize_by(email: auth.info.email)

          if user.new_record?
            create_new_user(user, auth)
            create_social_account(user, auth)
          end
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
      end
    end
  end
end
