# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AdminUserPasswordResetJob do
  it 'sends reset password instructions to non-admin user' do
    admin_user = create(:admin_user, roles: 'operator')
    expect_any_instance_of(AdminUser).to receive(:send_reset_password_instructions)

    described_class.new.perform(admin_user.id)
  end

  it 'does not send reset password instructions to admin user' do
    admin_user = create(:admin_user, roles: 'super_admin')
    expect_any_instance_of(AdminUser).not_to receive(:send_reset_password_instructions)

    described_class.new.perform(admin_user.id)
  end

  it 'handles non-existent user gracefully' do
    expect do
      described_class.new.perform(999)
    end.not_to raise_error
  end
end
