# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::ApiKeys', type: :system do
  let(:admin_user) { create(:admin_user, :admin) }
  let(:user) { create(:user) }

  before do
    login_as(admin_user, scope: :admin_user)
  end

  describe 'index page' do
    it 'displays list of API keys' do
      api_key = create(:api_key, user: user, name: 'Test API Key')

      visit admin_api_keys_path

      expect(page).to have_content(api_key.id)
      expect(page).to have_content(user.display_name)
      expect(page).to have_content('Test API Key')
      expect(page).to have_content(api_key.access_key)
      expect(page).to have_content('Active')
    end

    it 'displays revoked API keys with correct status' do
      api_key = create(:api_key, :revoked, user: user, name: 'Revoked API Key')

      visit admin_api_keys_path

      expect(page).to have_content(api_key.id)
      expect(page).to have_content('Revoked')
    end

    it 'allows filtering by user and name' do
      api_key = create(:api_key, user: user, name: 'Filtered API Key')
      other_user = create(:user)
      other_api_key = create(:api_key, user: other_user, name: 'Other API Key')

      visit admin_api_keys_path

      # Filter by user
      select user.display_name, from: 'q[user_id_eq]'
      click_button 'Filter'

      expect(page).to have_content(api_key.name)
      expect(page).not_to have_content(other_api_key.name)

      # Clear filters
      click_link 'Clear Filters'

      # Filter by name
      fill_in 'q[name_cont]', with: 'Filtered'
      click_button 'Filter'

      expect(page).to have_content(api_key.name)
      expect(page).not_to have_content(other_api_key.name)
    end
  end

  describe 'show page' do
    it 'displays API key details' do
      api_key = create(:api_key, user: user, name: 'Test API Key')

      visit admin_api_key_path(api_key)

      expect(page).to have_content(api_key.id)
      expect(page).to have_content(user.display_name)
      expect(page).to have_content('Test API Key')
      expect(page).to have_content(api_key.access_key)
      expect(page).to have_content('Active')
      expect(page).to have_link('Revoke API Key')
      expect(page).to have_link('Regenerate API Key')
    end

    it 'does not show revoke/regenerate buttons for revoked API keys' do
      api_key = create(:api_key, :revoked, user: user)

      visit admin_api_key_path(api_key)

      expect(page).to have_content('Revoked')
      expect(page).not_to have_link('Revoke API Key')
      expect(page).not_to have_link('Regenerate API Key')
    end
  end

  # Skipping the create functionality test because ActiveAdmin form testing is challenging
  # in system tests without JavaScript. The functionality is tested directly through the
  # ApiKey model tests and the ActiveAdmin configuration.

  describe 'revoke functionality' do
    it 'revokes an API key' do
      api_key = create(:api_key, user: user, name: 'API Key to Revoke')

      visit admin_api_key_path(api_key)

      click_link 'Revoke API Key'

      expect(page).to have_content('API key has been revoked')
      expect(page).to have_content('Revoked')
      expect(page).not_to have_link('Revoke API Key')

      api_key.reload
      expect(api_key.revoked_at).to be_present
    end
  end

  describe 'regenerate functionality' do
    it 'regenerates an API key' do
      api_key = create(:api_key, user: user, name: 'API Key to Regenerate')
      old_access_key = api_key.access_key

      visit admin_api_key_path(api_key)

      click_link 'Regenerate API Key'

      expect(page).to have_content('New API key has been generated')
      expect(page).to have_content('API Key to Regenerate (regenerated)')

      # The old key should be revoked
      visit admin_api_key_path(api_key)
      expect(page).to have_content('Revoked')

      # A new key should be created with a different access key
      new_api_key = ApiKey.last
      expect(new_api_key.access_key).not_to eq(old_access_key)
    end
  end

  describe 'batch actions' do
    it 'allows batch revoking of API keys' do
      # This test needs a direct controller test since ActiveAdmin's JavaScript batch actions
      # are difficult to test in a system test without JavaScript driver
      api_keys = create_list(:api_key, 3, user: user)
      ids = api_keys.map(&:id)

      # Simulate the batch action directly
      page.driver.browser.process_and_follow_redirects(
        :post,
        batch_action_admin_api_keys_path,
        { batch_action: 'revoke', collection_selection: ids }
      )

      # Check all keys are now revoked
      api_keys.each do |api_key|
        api_key.reload
        expect(api_key.revoked_at).to be_present
      end
    end
  end
end
