# frozen_string_literal: true

module V1
  module Helpers
    module TwoFactorHelper
      def verify_2fa_if_required!
        return true unless require_2fa_for_action?

        unless params[:two_factor_code].present?
          error!({
            status: 'error',
            message: '2FA code is required for this action',
            requires_2fa: true,
            device_trusted: device_trusted?
          }, 400)
        end

        unless current_user.verify_otp(params[:two_factor_code])
          error!({
            status: 'error',
            message: 'Invalid 2FA code'
          }, 400)
        end

        # Record this 2FA verification
        create_or_find_access_device

        true
      end
    end
  end
end
