# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'AdminUser', type: :feature do
  describe 'index page' do
    it 'user enabled 2fa' do
      user = create(:admin_user, :operator, fullname: 'Operator Name')
      sign_in(user, scope: :admin_user)

      expect(user).not_to be_authenticator_enabled
      expect(user.authenticator_key).to be_nil

      visit "/admin/setup_2fa"
      expect(page.current_path).to eq('/admin/setup_2fa')
      expect(page).to have_content('Setup Two-Factor Authentication')
      expect(page).to have_content('Step 1: Get Your Secret Key')
      expect(page).to have_content('Step 2: Set Up Your Authentication App')
      expect(page).to have_content('1. Open your authentication app (Google Authenticator, Authy, etc.)')
      expect(page).to have_content('Tap "+" or "Add" and scan the QR code above')
      expect(page).to have_content('Or manually enter the secret key by copying it')
      expect(page).to have_content('Step 3: Verify Setup')
      expect(page).to have_button('Verify and Enable 2FA')

      secret_key = user.authenticator_key
      code = ROTP::TOTP.new(secret_key).now
      fill_in 'otp-code', with: code
      click_button 'Verify and Enable 2FA'
      expect(page.current_path).to eq("/admin/admin_users/#{user.id}")
      expect(user.reload).to be_authenticator_enabled
      expect(user.authenticator_key).to eq(secret_key)
    end

    it 'user disabled 2fa' do
      user = create(:admin_user, :operator, fullname: 'Operator Name')
      sign_in(user, scope: :admin_user)

      user.assign_authenticator_key
      user.authenticator_enabled = true
      user.save!

      expect(user).to be_authenticator_enabled
      expect(user.authenticator_key).to be_present

      visit "/admin/setup_2fa"
      expect(page.current_path).to eq('/admin/setup_2fa')
      expect(page).to have_content('Two-Factor Authentication is Enabled')
      expect(page).to have_content('This account is already protected with two-factor authentication.')
      expect(page).to have_button('Disable 2FA')
      click_button 'Disable 2FA'

      code = ROTP::TOTP.new(user.authenticator_key).now
      fill_in 'otp-code', with: code
      click_button 'Disable 2FA'
      expect(page.current_path).to eq("/admin/admin_users/#{user.id}")
      expect(user.reload).not_to be_authenticator_enabled
      expect(user.authenticator_key).to be_nil
    end
  end
end
