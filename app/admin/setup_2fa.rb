# frozen_string_literal: true

ActiveAdmin.register_page 'Setup 2FA' do
  menu false

  page_action :verify, method: :post do
    if current_admin_user.authenticator_enabled?
      redirect_back fallback_location: admin_root_path, alert: '2FA is already enabled'
    elsif current_admin_user.verify_otp(params[:code])
      current_admin_user.update!(authenticator_enabled: true)
      redirect_to admin_admin_user_path(current_admin_user), notice: '2FA has been successfully enabled'
    else
      redirect_back fallback_location: admin_root_path, alert: 'Invalid verification code'
    end
  end

  page_action :disable, method: :post do
    if current_admin_user.authenticator_enabled?
      if current_admin_user.verify_otp(params[:code])
        current_admin_user.disable_authenticator!
        current_admin_user.save!
        redirect_to admin_admin_user_path(current_admin_user), notice: '2FA has been successfully disabled'
      else
        redirect_back fallback_location: admin_root_path, alert: 'Invalid verification code'
      end
    else
      redirect_back fallback_location: admin_root_path, alert: '2FA is not enabled'
    end
  end

  content do
    if current_admin_user.authenticator_enabled?
      render partial: 'admin/setup_2fa/enabled'
    else
      current_admin_user.assign_authenticator_key
      current_admin_user.save!

      render partial: 'admin/setup_2fa/setup', locals: {
        admin_user: current_admin_user,
        secret_key: current_admin_user.authenticator_key,
        provisioning_uri: current_admin_user.generate_provisioning_uri
      }
    end
  end
end
