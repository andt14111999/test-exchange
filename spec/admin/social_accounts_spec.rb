# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::SocialAccounts', type: :system do
  let(:admin_user) { create(:admin_user, :admin) }

  before do
    driven_by(:rack_test)
    login_as(admin_user, scope: :admin_user)
  end

  describe 'index page' do
    it 'displays a list of social accounts' do
      social_account = create(:social_account)
      visit admin_social_accounts_path
      expect(page).to have_content(social_account.email)
    end

    it 'displays no expiry status in index page' do
      social_account = create(:social_account, token_expires_at: nil)
      visit admin_social_accounts_path
      expect(page).to have_selector('.status_tag.warning', text: 'No Expiry')
    end

    it 'displays expired status in index page' do
      social_account = create(:social_account, token_expires_at: 1.day.ago)
      visit admin_social_accounts_path
      expect(page).to have_selector('.status_tag.error', text: 'Expired')
    end

    it 'filters social accounts by provider' do
      create(:social_account, provider: 'google')
      create(:social_account, provider: 'facebook')
      visit admin_social_accounts_path
      select 'google', from: 'q_provider'
      click_button 'Filter'
      expect(page).to have_selector('.status_tag', text: 'Google')
      expect(page).not_to have_selector('.status_tag', text: 'Facebook')
    end

    it 'filters social accounts by user' do
      user = create(:user)
      social_account = create(:social_account, user: user)
      visit admin_social_accounts_path
      select user.email, from: 'q_user_id'
      click_button 'Filter'
      expect(page).to have_content(social_account.email)
    end
  end

  describe 'show page' do
    let(:social_account) { create(:social_account, profile_data: { 'data' => 'test' }) }

    before do
      visit admin_social_account_path(social_account)
    end

    it 'displays social account details' do
      expect(page).to have_content(social_account.email)
      expect(page).to have_content(social_account.name)
    end

    it 'displays token information' do
      click_link 'Token Information'
      expect(page).to have_content('Valid (Expires')
    end

    it 'displays valid status for valid token' do
      social_account.update!(token_expires_at: 1.hour.from_now)
      visit admin_social_account_path(social_account)
      click_link 'Token Information'
      expect(page).to have_content('Valid')
    end

    it 'displays no expiry for nil token_expires_at' do
      social_account.update!(token_expires_at: nil)
      visit admin_social_account_path(social_account)
      click_link 'Token Information'
      expect(page).to have_selector('.status_tag.warning', text: 'No Expiry')
    end

    it 'displays expired status for expired token' do
      social_account.update!(token_expires_at: 1.day.ago)
      visit admin_social_account_path(social_account)
      click_link 'Token Information'
      expect(page).to have_selector('.status_tag.error', text: 'Expired')
    end

    it 'displays profile data' do
      click_link 'Profile Data'
      within find('.panel', text: 'Raw Profile Data') do
        expect(page).to have_content('test')
      end
    end

    it 'saves profile data correctly' do
      expect(social_account.profile_data).to eq({ 'data' => 'test' })
    end

    it 'refreshes token' do
      click_link 'Refresh Token'
      expect(page).to have_content('Token refresh has been initiated')
    end
  end

  describe 'create new social account' do
    it 'creates a new social account' do
      user = create(:user)
      visit new_admin_social_account_path
      select user.email, from: 'social_account[user_id]'
      select 'google', from: 'social_account[provider]'
      fill_in 'social_account[provider_user_id]', with: 'user_id'
      fill_in 'social_account[email]', with: 'test@example.com'
      fill_in 'social_account[name]', with: 'Test User'
      click_button 'Create Social account'
      expect(page).to have_content('Social account was successfully created')
    end
  end

  describe 'edit social account' do
    let(:social_account) { create(:social_account) }

    it 'updates a social account' do
      visit edit_admin_social_account_path(social_account)
      fill_in 'social_account[name]', with: 'Updated Name'
      click_button 'Update Social account'
      expect(page).to have_content('Social account was successfully updated')
    end
  end

  describe 'delete social account' do
    let(:social_account) { create(:social_account) }

    it 'deletes a social account' do
      visit admin_social_account_path(social_account)
      click_link 'Delete Social Account'
      expect(page).to have_content('Social account was successfully destroyed')
    end
  end

  describe 'batch actions' do
    it 'initiates token refresh for selected accounts' do
      social_account = create(:social_account)
      visit admin_social_accounts_path
      find("input[type='checkbox'][value='#{social_account.id}']").check
      page.driver.post(batch_action_admin_social_accounts_path, { batch_action: 'refresh_tokens', collection_selection: [ social_account.id ] })
      visit admin_social_accounts_path
      expect(page).to have_content('Token refresh has been initiated for selected accounts')
    end
  end
end
