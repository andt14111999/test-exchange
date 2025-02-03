# frozen_string_literal: true

class AdminUserPasswordResetJob
  include Sidekiq::Worker

  sidekiq_options queue: :default, retry: 3

  def perform(admin_user_id)
    admin_user = AdminUser.find_by(id: admin_user_id)
    return unless admin_user && !admin_user.admin?

    admin_user.send_reset_password_instructions
  end
end
