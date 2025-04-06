# frozen_string_literal: true

module ActiveAdmin
  module BaseAdminHelper
    def api_link_to(title, url, options = {})
      options[:class] = "#{options[:class]} api-link".strip
      options[:data] ||= {}
      options[:data][:type] = 'json'

      link_to title, url, options
    end

    def verify_2fa_and_process(code, success_message)
      if current_admin_user.authenticator_enabled?
        if current_admin_user.verify_otp(code)
          yield
          flash[:notice] = success_message
          :ok
        else
          flash[:error] = '2Fa code is incorrect.'
          :unprocessable_entity
        end
      else
        flash[:error] = '2Fa is not enabled.'
        :unprocessable_entity
      end
    end

    def open_2fa_button(context)
      api_link_to "#{context} 2FA", admin_setup_2fa_path, class: 'button'
    end
  end
end
