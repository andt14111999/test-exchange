# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::Setup2FA', type: :system do
  let(:admin) { create(:admin_user, roles: 'super_admin') }

  before do
    sign_in admin, scope: :admin_user
  end

  describe 'setup 2FA page' do
    it 'shows setup page for user without 2FA enabled' do
      # Ensure 2FA is not enabled
      admin.update!(authenticator_enabled: false, authenticator_key: nil)

      visit admin_setup_2fa_path

      expect(page).to have_content('Setup Two-Factor Authentication')
      expect(page).to have_css('.qr-code svg') # QR code should be displayed as SVG
      expect(page).to have_field('code')
      expect(page).to have_button('Verify and Enable 2FA')
    end

    it 'shows a different page when 2FA is already enabled' do
      # Ensure 2FA is enabled
      admin.update!(authenticator_enabled: true, authenticator_key: 'TESTKEY4567890123')

      visit admin_setup_2fa_path

      expect(page).to have_content('Two-Factor Authentication is Enabled')
      expect(page).to have_field('code')
      expect(page).to have_button('Disable 2FA')
      expect(page).not_to have_css('.secret-key-container') # Secret key container should not be shown
    end
  end

  describe 'enabling 2FA' do
    before do
      admin.update!(authenticator_enabled: false, authenticator_key: nil)
      allow_any_instance_of(AdminUser).to receive(:verify_otp).and_return(true)
    end

    it 'enables 2FA with valid code' do
      visit admin_setup_2fa_path

      fill_in 'code', with: '123456'
      click_button 'Verify and Enable 2FA'

      # Should redirect to admin user page
      expect(page).to have_current_path(admin_admin_user_path(admin))
      expect(page).to have_content('2FA has been successfully enabled')

      # Admin user should have 2FA enabled now
      expect(admin.reload.authenticator_enabled).to be true
    end

    it 'shows error with invalid code' do
      allow_any_instance_of(AdminUser).to receive(:verify_otp).and_return(false)

      visit admin_setup_2fa_path

      fill_in 'code', with: 'invalid'
      click_button 'Verify and Enable 2FA'

      # Should redirect back with error
      expect(page).to have_content('Invalid verification code')
    end

    it 'redirects when 2FA is already enabled' do
      admin.update!(authenticator_enabled: true)

      visit admin_setup_2fa_path

      # Try to submit the form directly
      page.driver.post(admin_setup_2fa_verify_path, { code: '123456' })
      visit current_path

      expect(page).to have_content('2FA is already enabled')
    end
  end

  describe 'disabling 2FA' do
    before do
      admin.update!(authenticator_enabled: true, authenticator_key: 'TESTKEY4567890123')
      allow_any_instance_of(AdminUser).to receive(:verify_otp).and_return(true)
    end

    it 'disables 2FA with valid code' do
      visit admin_setup_2fa_path

      fill_in 'code', with: '123456'
      click_button 'Disable 2FA'

      # Should redirect to admin user page
      expect(page).to have_current_path(admin_admin_user_path(admin))
      expect(page).to have_content('2FA has been successfully disabled')

      # Admin user should have 2FA disabled now
      expect(admin.reload.authenticator_enabled).to be false
      expect(admin.authenticator_key).to be_nil
    end

    it 'shows error with invalid code' do
      allow_any_instance_of(AdminUser).to receive(:verify_otp).and_return(false)

      visit admin_setup_2fa_path

      fill_in 'code', with: 'invalid'
      click_button 'Disable 2FA'

      # Should redirect back with error
      expect(page).to have_content('Invalid verification code')
    end

    it 'redirects when 2FA is not enabled' do
      admin.update!(authenticator_enabled: false, authenticator_key: nil)

      visit admin_setup_2fa_path

      # Try to submit the form directly
      page.driver.post(admin_setup_2fa_disable_path, { code: '123456' })
      visit current_path

      expect(page).to have_content('2FA is not enabled')
    end
  end
end
