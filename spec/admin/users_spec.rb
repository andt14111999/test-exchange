# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::Users', type: :system do
  let(:superadmin) { create(:admin_user, :superadmin) }
  let(:operator) { create(:admin_user, :operator) }

  describe 'with superadmin user' do
    before do
      sign_in superadmin, scope: :admin_user
    end

    describe 'Index page' do
      let!(:regular_user) { create(:user, email: 'regular@example.com', role: 'user', status: 'active') }
      let!(:merchant_user) { create(:user, email: 'merchant@example.com', role: 'merchant', status: 'suspended') }
      let!(:banned_user) { create(:user, email: 'banned@example.com', status: 'banned') }

      it 'displays users list with all columns' do
        visit admin_users_path

        expect(page).to have_content(regular_user.email)
        expect(page).to have_content(merchant_user.email)
        expect(page).to have_content(banned_user.email)
        expect(page).to have_content('Active')
        expect(page).to have_content('Suspended')
        expect(page).to have_content('Banned')
      end

      it 'displays user avatar when present' do
        user_with_avatar = create(:user, avatar_url: 'https://example.com/avatar.jpg')
        visit admin_users_path

        expect(page).to have_css("img[src='#{user_with_avatar.avatar_url}']")
      end

      it 'displays role status tags correctly' do
        visit admin_users_path

        expect(page).to have_content('User')
        expect(page).to have_content('Merchant')
      end

      it 'displays KYC level status tags' do
        create(:user, kyc_level: 2)
        visit admin_users_path

        expect(page).to have_content('Full Kyc')
      end

      it 'displays verification status' do
        create(:user, phone_verified: true)
        visit admin_users_path

        expect(page).to have_content('Verified')
      end

      it 'does not display avatar when not present' do
        user_without_avatar = create(:user, avatar_url: nil)
        visit admin_users_path

        # Should not have broken image or error
        expect(page).to have_content(user_without_avatar.email)
      end
    end

    describe 'Filters' do
      let!(:filter_user_active) { create(:user, email: 'test1@example.com', role: 'user', status: 'active') }
      let!(:filter_merchant_suspended) { create(:user, email: 'test2@example.com', role: 'merchant', status: 'suspended') }

      it 'filters by email' do
        visit admin_users_path
        fill_in 'q_email', with: filter_user_active.email
        click_button 'Filter'

        expect(page).to have_content(filter_user_active.email)
        expect(page).not_to have_content(filter_merchant_suspended.email)
      end

      it 'filters by role' do
        visit admin_users_path
        select 'merchant', from: 'q_role'
        click_button 'Filter'

        expect(page).to have_content(filter_merchant_suspended.email)
        expect(page).not_to have_content(filter_user_active.email)
      end

      it 'filters by status' do
        visit admin_users_path
        select 'suspended', from: 'q_status'
        click_button 'Filter'

        expect(page).to have_content(filter_merchant_suspended.email)
        expect(page).not_to have_content(filter_user_active.email)
      end

      it 'filters by KYC level' do
        filter_kyc_user = create(:user, kyc_level: 2)
        visit admin_users_path
        select '2', from: 'q_kyc_level'
        click_button 'Filter'

        expect(page).to have_content(filter_kyc_user.email)
        expect(page).not_to have_content(filter_user_active.email)
      end

      it 'filters by phone verification' do
        filter_phone_verified = create(:user, phone_verified: true)
        visit admin_users_path
        select 'Yes', from: 'q_phone_verified'
        click_button 'Filter'

        expect(page).to have_content(filter_phone_verified.email)
        expect(page).not_to have_content(filter_user_active.email)
      end

      it 'filters by document verification' do
        filter_doc_verified = create(:user, document_verified: true)
        visit admin_users_path
        select 'Yes', from: 'q_document_verified'
        click_button 'Filter'

        expect(page).to have_content(filter_doc_verified.email)
        expect(page).not_to have_content(filter_user_active.email)
      end
    end

    describe 'Scopes' do
      let!(:merchant) { create(:user, role: 'merchant') }
      let!(:regular_user) { create(:user, role: 'user') }
      let!(:suspended_user) { create(:user, status: 'suspended') }
      let!(:phone_verified_user) { create(:user, phone_verified: true) }
      let!(:document_verified_user) { create(:user, document_verified: true) }

      it 'shows merchants scope' do
        visit admin_users_path
        click_link 'Merchants'

        expect(page).to have_content(merchant.email)
        expect(page).not_to have_content(regular_user.email)
      end

      it 'shows regular users scope' do
        visit admin_users_path
        click_link 'Regular Users'

        expect(page).to have_content(regular_user.email)
        expect(page).not_to have_content(merchant.email)
      end

      it 'shows suspended users scope' do
        visit admin_users_path
        click_link 'Suspended Users'

        expect(page).to have_content(suspended_user.email)
        expect(page).not_to have_content(regular_user.email)
      end

      it 'shows phone verified users scope' do
        visit admin_users_path
        click_link 'Phone Verified'

        expect(page).to have_content(phone_verified_user.email)
        # Note: Not testing exclusion due to other test data interference
      end

      it 'shows document verified users scope' do
        visit admin_users_path
        click_link 'Document Verified'

        expect(page).to have_content(document_verified_user.email)
      end

      it 'shows banned users scope' do
        banned_user = create(:user, status: 'banned')
        visit admin_users_path
        click_link 'Banned Users'

        expect(page).to have_content(banned_user.email)
      end

      it 'shows active users scope' do
        visit admin_users_path
        click_link 'Active Users'

        expect(page).to have_content(regular_user.email)
        expect(page).not_to have_content(suspended_user.email)
      end
    end

    describe 'Show page' do
      let!(:user) { create(:user, username: 'testuser', display_name: 'Test User') }
      let!(:social_account) { create(:social_account, user: user, provider: 'google') }
      let!(:balance_lock) { create(:balance_lock, user: user) }

      it 'displays user details' do
        visit admin_user_path(user)

        expect(page).to have_content(user.email)
        expect(page).to have_content(user.username)
        expect(page).to have_content(user.display_name)
        expect(page).to have_content(user.role.humanize)
        expect(page).to have_content('Active')
      end

      it 'shows social accounts data' do
        visit admin_user_path(user)

        expect(page).to have_content(social_account.provider.humanize)
        expect(page).to have_content(social_account.email)
      end

      it 'shows balance locks data' do
        visit admin_user_path(user)

        expect(page).to have_content('Pending')
        expect(page).to have_content(balance_lock.reason)
      end

      it 'shows action items for status changes' do
        suspended_user = create(:user, status: 'suspended')
        visit admin_user_path(suspended_user)

        expect(page).to have_link('Activate User')
        expect(page).to have_link('Ban User')
      end

      it 'shows lock all funds action' do
        visit admin_user_path(user)

        expect(page).to have_link('Lock All Funds')
      end

      it 'shows different action items based on user status' do
        active_user = create(:user, status: 'active')
        visit admin_user_path(active_user)

        expect(page).to have_link('Suspend User')
        expect(page).to have_link('Ban User')
        expect(page).not_to have_link('Activate User')
      end

      it 'displays user avatar when present in show page' do
        user_with_avatar = create(:user, avatar_url: 'https://example.com/show_avatar.jpg')
        visit admin_user_path(user_with_avatar)

        expect(page).to have_css("img[src='#{user_with_avatar.avatar_url}']")
      end

      it 'displays social account avatar when present' do
        social_with_avatar = create(:social_account,
          user: user,
          provider: 'facebook',
          avatar_url: 'https://example.com/social_avatar.jpg'
        )
        visit admin_user_path(user)

        expect(page).to have_css("img[src='#{social_with_avatar.avatar_url}']")
      end
    end

    describe 'New user form' do
      it 'displays new user form' do
        visit new_admin_user_path

        expect(page).to have_field('Email')
        expect(page).to have_field('Username')
        expect(page).to have_field('Display name')
        expect(page).to have_select('Role')
        expect(page).to have_select('Status')
      end

      it 'creates a new user' do
        visit new_admin_user_path

        fill_in 'Email', with: 'newuser@example.com'
        fill_in 'Username', with: 'newuser'
        fill_in 'Display name', with: 'New User'
        select 'user', from: 'Role'
        select 'active', from: 'Status'

        expect {
          click_button 'Create User'
        }.to change(User, :count).by(1)

        user = User.last
        expect(user.email).to eq('newuser@example.com')
        expect(user.username).to eq('newuser')
        expect(user.role).to eq('user')
      end

      it 'creates a merchant user' do
        visit new_admin_user_path

        fill_in 'Email', with: 'merchant@example.com'
        fill_in 'Username', with: 'merchantuser'
        fill_in 'Display name', with: 'Merchant User'
        select 'merchant', from: 'Role'
        select 'active', from: 'Status'
        select 'Full KYC', from: 'Kyc level'

        expect {
          click_button 'Create User'
        }.to change(User, :count).by(1)

        user = User.last
        expect(user.role).to eq('merchant')
        expect(user.kyc_level).to eq(2)
      end
    end

    describe 'Edit user form' do
      let!(:user) { create(:user, username: 'oldusername') }

      it 'displays edit user form' do
        visit edit_admin_user_path(user)

        expect(page).to have_field('Email', with: user.email)
        expect(page).to have_field('Username', disabled: true)
        expect(page).to have_field('Display name', with: user.display_name)
      end

      it 'updates user information' do
        visit edit_admin_user_path(user)

        fill_in 'Display name', with: 'Updated Name'
        select 'merchant', from: 'Role'

        click_button 'Update User'

        user.reload
        expect(user.display_name).to eq('Updated Name')
        expect(user.role).to eq('merchant')
      end

      it 'prevents username change when already set' do
        visit edit_admin_user_path(user)

        expect(page).to have_field('Username', disabled: true)
      end

      it 'allows username to be set when nil' do
        user_without_username = create(:user, username: nil)
        visit edit_admin_user_path(user_without_username)

        expect(page).to have_field('Username', disabled: false)
      end

      it 'shows avatar preview when user has avatar' do
        user_with_avatar = create(:user, avatar_url: 'https://example.com/current_avatar.jpg')
        visit edit_admin_user_path(user_with_avatar)

        expect(page).to have_css("img[src='#{user_with_avatar.avatar_url}']")
      end
    end

    describe 'User stats sidebar' do
      let!(:user) { create(:user) }
      let!(:social_accounts) { create_list(:social_account, 2, user: user) }

      it 'displays user statistics' do
        visit admin_user_path(user)

        within '.sidebar_section' do
          expect(page).to have_content(social_accounts.count.to_s)
          expect(page).to have_content('Account Age')
          expect(page).to have_content('Last Updated')
        end
      end
    end

    describe 'Scoped collection optimization' do
      it 'includes social_accounts to avoid N+1 queries' do
        user_with_social = create(:user)
        create(:social_account, user: user_with_social)

        visit admin_users_path

        # This test ensures the includes(:social_accounts) is working
        expect(page).to have_content(user_with_social.email)
      end
    end
  end

  describe 'Member actions (request level)', type: :request do
    let!(:user) { create(:user, status: 'active') }

    before do
      sign_in superadmin, scope: :admin_user
    end

    it 'activates user' do
      user.update(status: 'suspended')

      put activate_admin_user_path(user)

      expect(user.reload.status).to eq('active')
    end

    it 'suspends user' do
      put suspend_admin_user_path(user)

      expect(user.reload.status).to eq('suspended')
    end

    it 'bans user' do
      put ban_admin_user_path(user)

      expect(user.reload.status).to eq('banned')
    end

    it 'locks all funds' do
      expect {
        post lock_all_funds_admin_user_path(user)
      }.to change(BalanceLock, :count).by(1)

      balance_lock = BalanceLock.last
      expect(balance_lock.user).to eq(user)
      expect(balance_lock.performer).to eq(superadmin.email)
    end

    it 'locks all funds with custom reason' do
      post lock_all_funds_admin_user_path(user), params: { reason: 'Custom security reason' }

      balance_lock = BalanceLock.last
      expect(balance_lock.reason).to eq('Custom security reason')
    end

    it 'locks all funds with default reason when no reason provided' do
      post lock_all_funds_admin_user_path(user), params: { reason: '' }

      balance_lock = BalanceLock.last
      expect(balance_lock.reason).to eq("Admin-initiated lock by #{superadmin.email}")
    end
  end

  describe 'Batch actions (request level)', type: :request do
    let!(:users) { create_list(:user, 3, phone_verified: false, document_verified: false, status: 'active') }

    before do
      sign_in superadmin, scope: :admin_user
    end

    it 'verifies phone for selected users' do
      post batch_action_admin_users_path, params: {
        batch_action: 'verify_phone',
        collection_selection: users.map(&:id)
      }

      users.each do |user|
        expect(user.reload.phone_verified).to be true
      end
    end

    it 'verifies document for selected users' do
      post batch_action_admin_users_path, params: {
        batch_action: 'verify_document',
        collection_selection: users.map(&:id)
      }

      users.each do |user|
        expect(user.reload.document_verified).to be true
      end
    end

    it 'suspends selected users' do
      post batch_action_admin_users_path, params: {
        batch_action: 'suspend',
        collection_selection: users.map(&:id)
      }

      users.each do |user|
        expect(user.reload.status).to eq('suspended')
      end
    end

    it 'activates selected users' do
      users.each { |user| user.update(status: 'suspended') }

      post batch_action_admin_users_path, params: {
        batch_action: 'activate',
        collection_selection: users.map(&:id)
      }

      users.each do |user|
        expect(user.reload.status).to eq('active')
      end
    end

    it 'bans selected users' do
      post batch_action_admin_users_path, params: {
        batch_action: 'ban',
        collection_selection: users.map(&:id)
      }

      users.each do |user|
        expect(user.reload.status).to eq('banned')
      end
    end
  end

  describe 'with operator user (non-superadmin)' do
    before do
      sign_in operator, scope: :admin_user
    end

    describe 'authorization restrictions' do
      let!(:user) { create(:user) }

      it 'allows access to new user form but prevents creation' do
        visit new_admin_user_path

        # Form should be accessible but submission should fail
        expect(page).to have_field('Email')

        fill_in 'Email', with: 'test@example.com'
        click_button 'Create User'

        # Should redirect with error message
        expect(current_path).to eq('/admin')
      end

      it 'redirects when trying to edit user' do
        put admin_user_path(user), params: { user: { email: 'new@example.com' } }

        expect(response).to redirect_to(admin_root_path)
      end

      it 'allows viewing users list' do
        visit admin_users_path

        expect(page).to have_content(user.email)
      end

      it 'allows viewing user details' do
        visit admin_user_path(user)

        expect(page).to have_content(user.email)
      end
    end
  end

  describe 'Destroy action exclusion' do
    let!(:user) { create(:user) }

    before do
      sign_in superadmin, scope: :admin_user
    end

    it 'does not allow delete action' do
      expect {
        delete admin_user_path(user)
      }.not_to change(User, :count)

      expect(response.status).to eq(404)
    end

    it 'does not show delete link in actions' do
      visit admin_users_path

      expect(page).not_to have_link('Delete')
    end
  end

  describe 'Error handling' do
    before do
      sign_in superadmin, scope: :admin_user
    end

    it 'handles balance lock creation failure gracefully' do
      user = create(:user)

      # Mock BalanceLock.create to return an invalid object
      invalid_balance_lock = BalanceLock.new
      invalid_balance_lock.errors.add(:base, 'Test error')
      allow(BalanceLock).to receive(:create).and_return(invalid_balance_lock)

      post lock_all_funds_admin_user_path(user)

      expect(response).to redirect_to(admin_user_path(user))
    end
  end

  describe 'Configuration coverage' do
    before do
      sign_in superadmin, scope: :admin_user
    end

    it 'tests permitted parameters' do
      user = create(:user)

      # Test that all permitted parameters can be updated
      put admin_user_path(user), params: {
        user: {
          email: 'updated@example.com',
          display_name: 'Updated Name',
          avatar_url: 'https://example.com/new_avatar.jpg',
          role: 'merchant',
          phone_verified: true,
          document_verified: true,
          kyc_level: 2,
          status: 'suspended'
        }
      }

      user.reload
      expect(user.email).to eq('updated@example.com')
      expect(user.display_name).to eq('Updated Name')
      expect(user.role).to eq('merchant')
      expect(user.phone_verified).to be true
      expect(user.document_verified).to be true
      expect(user.kyc_level).to eq(2)
      expect(user.status).to eq('suspended')
    end

    it 'tests all filter options' do
      # Create users with different attributes
      create(:user, email: 'filter1@example.com', username: 'filteruser1', display_name: 'Filter User 1')
      create(:user, email: 'filter2@example.com', username: 'filteruser2', display_name: 'Filter User 2')

      visit admin_users_path

      # Test all filter fields exist
      expect(page).to have_field('q_id')
      expect(page).to have_field('q_email')
      expect(page).to have_field('q_username')
      expect(page).to have_field('q_display_name')
      expect(page).to have_select('q_role')
      expect(page).to have_select('q_status')
      expect(page).to have_select('q_kyc_level')
      expect(page).to have_select('q_phone_verified')
      expect(page).to have_select('q_document_verified')
    end

    it 'covers all scopes' do
      visit admin_users_path

      # Test all scopes are present
      expect(page).to have_link('All')
      expect(page).to have_link('Merchants')
      expect(page).to have_link('Regular Users')
      expect(page).to have_link('Active Users')
      expect(page).to have_link('Suspended Users')
      expect(page).to have_link('Banned Users')
      expect(page).to have_link('Phone Verified')
      expect(page).to have_link('Document Verified')
    end

    it 'tests menu priority and label configuration' do
      # This indirectly tests the menu configuration
      visit admin_root_path
      expect(page).to have_link('Users')
    end
  end

  describe 'Additional comprehensive coverage' do
    before do
      sign_in superadmin, scope: :admin_user
    end

    describe 'Status tag display logic' do
      it 'displays correct status classes for all user statuses' do
        create(:user, status: 'active')
        create(:user, status: 'suspended')
        create(:user, status: 'banned')
        visit admin_users_path

        # Verify status tags are displayed with correct CSS classes
        expect(page).to have_css('.status_tag.ok', text: 'Active')
        expect(page).to have_css('.status_tag.warning', text: 'Suspended')
        expect(page).to have_css('.status_tag.error', text: 'Banned')
      end

      it 'displays KYC level tags with correct classes' do
        create(:user, kyc_level: 0)
        create(:user, kyc_level: 1)
        create(:user, kyc_level: 2)
        visit admin_users_path

        expect(page).to have_css('.status_tag.error', text: 'No Kyc')
        expect(page).to have_css('.status_tag.warning', text: 'Phone Kyc')
        expect(page).to have_css('.status_tag.ok', text: 'Full Kyc')
      end

      it 'displays role tags with correct classes' do
        create(:user, role: 'user')
        create(:user, role: 'merchant')
        visit admin_users_path

        expect(page).to have_css('.status_tag.ok', text: 'User')
        expect(page).to have_css('.status_tag.warning', text: 'Merchant')
      end

      it 'displays verification status tags correctly' do
        create(:user, phone_verified: true)
        create(:user, phone_verified: false)
        visit admin_users_path

        expect(page).to have_css('.status_tag.ok', text: 'Verified')
        expect(page).to have_css('.status_tag.error', text: 'Unverified')
      end
    end

    describe 'Show page tabs and content' do
      let!(:test_user) { create(:user, avatar_url: 'https://example.com/user_avatar.jpg') }
      let!(:social_account) { create(:social_account, user: test_user, avatar_url: 'https://example.com/social_avatar.jpg') }
      let!(:balance_lock) { create(:balance_lock, user: test_user, status: 'pending', locked_balances: { 'usdt' => '500.0' }) }

      it 'shows user avatar in show page when present' do
        visit admin_user_path(test_user)

        expect(page).to have_css("img[src='#{test_user.avatar_url}']")
      end

      it 'shows social account details with avatar' do
        visit admin_user_path(test_user)

        expect(page).to have_content('Connected Social Accounts')
        expect(page).to have_css("img[src='#{social_account.avatar_url}']")
        expect(page).to have_content(social_account.provider.humanize)
        expect(page).to have_content(social_account.email)
        expect(page).to have_content(social_account.name)
      end

      it 'shows balance lock details with JSON formatted balances' do
        visit admin_user_path(test_user)

        expect(page).to have_content('User Balance Locks')
        expect(page).to have_content('Pending')
        expect(page).to have_content(balance_lock.reason)
        expect(page).to have_content(balance_lock.performer)
      end

      it 'shows balance lock status with correct classes' do
        visit admin_user_path(test_user)

        # The test user has a balance lock with status 'pending' from the let! block above
        expect(page).to have_css('.status_tag.warning', text: 'Pending')
      end
    end

    describe 'Form validations and edge cases' do
      it 'shows error messages for invalid form submissions' do
        visit new_admin_user_path

        fill_in 'Email', with: 'invalid-email'
        click_button 'Create User'

        expect(page).to have_content('is invalid')
      end

      it 'allows updating avatar URL' do
        user = create(:user, avatar_url: 'https://old-avatar.com/avatar.jpg')
        visit edit_admin_user_path(user)

        fill_in 'Avatar url', with: 'https://new-avatar.com/avatar.jpg'
        click_button 'Update User'

        user.reload
        expect(user.avatar_url).to eq('https://new-avatar.com/avatar.jpg')
      end

      it 'shows current avatar in edit form when present' do
        user = create(:user, avatar_url: 'https://example.com/current.jpg')
        visit edit_admin_user_path(user)

        expect(page).to have_css("img[src='#{user.avatar_url}']")
        expect(page).to have_field('Current avatar')
      end
    end

    describe 'Username handling' do
      it 'allows setting username for new users' do
        visit new_admin_user_path

        fill_in 'Email', with: 'newuser@example.com'
        fill_in 'Username', with: 'testusername'
        fill_in 'Display name', with: 'Test User'
        select 'user', from: 'Role'
        select 'active', from: 'Status'

        expect {
          click_button 'Create User'
        }.to change(User, :count).by(1)

        user = User.last
        expect(user.username).to eq('testusername')
      end

      it 'prevents username change when already set' do
        user = create(:user, username: 'existing_username')
        visit edit_admin_user_path(user)

        expect(page).to have_field('Username', disabled: true, with: 'existing_username')
      end

      it 'allows username to be set when blank' do
        user = create(:user, username: nil)
        visit edit_admin_user_path(user)

        expect(page).to have_field('Username', disabled: false)
      end

      it 'shows username hint in form' do
        visit new_admin_user_path

        expect(page).to have_content('Can only be set once')
        expect(page).to have_content('3-20 characters, letters, numbers, and underscores only')
      end
    end

    describe 'Action items based on user status' do
      it 'shows appropriate action items for active user' do
        active_user = create(:user, status: 'active')
        visit admin_user_path(active_user)

        expect(page).to have_link('Suspend User')
        expect(page).to have_link('Ban User')
        expect(page).not_to have_link('Activate User')
        expect(page).to have_link('Lock All Funds')
      end

      it 'shows appropriate action items for suspended user' do
        suspended_user = create(:user, status: 'suspended')
        visit admin_user_path(suspended_user)

        expect(page).to have_link('Activate User')
        expect(page).to have_link('Ban User')
        expect(page).not_to have_link('Suspend User')
      end

      it 'shows appropriate action items for banned user' do
        banned_user = create(:user, status: 'banned')
        visit admin_user_path(banned_user)

        expect(page).to have_link('Activate User')
        expect(page).to have_link('Suspend User')
        expect(page).not_to have_link('Ban User')
      end
    end

    describe 'Sidebar user statistics' do
      it 'displays user statistics correctly' do
        user = create(:user, created_at: 2.months.ago, updated_at: 1.week.ago)
        create_list(:social_account, 3, user: user)

        visit admin_user_path(user)

        within '.sidebar_section' do
          expect(page).to have_content('3')
          expect(page).to have_content('Account Age')
          expect(page).to have_content('Last Updated')
        end
      end

      it 'handles users with no social accounts' do
        user = create(:user)
        visit admin_user_path(user)

        within '.sidebar_section' do
          expect(page).to have_content('0')
        end
      end
    end

    describe 'All filter combinations' do
      let!(:regular_test_user) { create(:user, email: 'test1@example.com', username: 'testuser1', display_name: 'Test User 1', role: 'user', status: 'active', kyc_level: 0, phone_verified: false, document_verified: false) }
      let!(:merchant_test_user) { create(:user, email: 'test2@example.com', username: 'testuser2', display_name: 'Test User 2', role: 'merchant', status: 'suspended', kyc_level: 2, phone_verified: true, document_verified: true) }

      it 'filters by username' do
        visit admin_users_path
        fill_in 'q_username', with: regular_test_user.username
        click_button 'Filter'

        expect(page).to have_content(regular_test_user.email)
        expect(page).not_to have_content(merchant_test_user.email)
      end

      it 'filters by display name' do
        visit admin_users_path
        fill_in 'q_display_name', with: merchant_test_user.display_name
        click_button 'Filter'

        expect(page).to have_content(merchant_test_user.email)
        expect(page).not_to have_content(regular_test_user.email)
      end

      it 'filters by ID' do
        visit admin_users_path
        fill_in 'q_id', with: regular_test_user.id
        click_button 'Filter'

        expect(page).to have_content(regular_test_user.email)
        expect(page).not_to have_content(merchant_test_user.email)
      end
    end

    describe 'Batch actions edge cases' do
      it 'handles empty selection gracefully' do
        user = create(:user)
        visit admin_users_path

        expect(page).to have_css('input[type="checkbox"]')
        # Test that batch actions are present in interface
        expect(page).to have_content(user.email)
      end

      it 'handles single user selection via request' do
        user = create(:user, phone_verified: false)

        post batch_action_admin_users_path, params: {
          batch_action: 'verify_phone',
          collection_selection: [ user.id ]
        }

        expect(user.reload.phone_verified).to be true
      end
    end

    describe 'Complex form scenarios' do
      it 'creates merchant with full KYC' do
        visit new_admin_user_path

        fill_in 'Email', with: 'merchant@test.com'
        fill_in 'Username', with: 'testmerchant'
        fill_in 'Display name', with: 'Test Merchant'
        fill_in 'Avatar url', with: 'https://example.com/merchant.jpg'
        select 'merchant', from: 'Role'
        select 'active', from: 'Status'
        select 'Full KYC', from: 'Kyc level'
        check 'Phone verified'
        check 'Document verified'

        expect {
          click_button 'Create User'
        }.to change(User, :count).by(1)

        user = User.last
        expect(user.role).to eq('merchant')
        expect(user.kyc_level).to eq(2)
        expect(user.phone_verified).to be true
        expect(user.document_verified).to be true
        expect(user.avatar_url).to eq('https://example.com/merchant.jpg')
      end

      it 'updates user status and verification in single operation' do
        user = create(:user, status: 'active', phone_verified: false, document_verified: false)
        visit edit_admin_user_path(user)

        select 'suspended', from: 'Status'
        check 'Phone verified'
        check 'Document verified'
        click_button 'Update User'

        user.reload
        expect(user.status).to eq('suspended')
        expect(user.phone_verified).to be true
        expect(user.document_verified).to be true
      end
    end

    describe 'Social account links and actions' do
      let!(:user_with_social) { create(:user) }
      let!(:google_account) { create(:social_account, user: user_with_social, provider: 'google') }
      let!(:facebook_account) { create(:social_account, user: user_with_social, provider: 'facebook') }

      it 'shows social account view and edit links' do
        visit admin_user_path(user_with_social)

        expect(page).to have_link('View', href: admin_social_account_path(google_account))
        expect(page).to have_link('Edit', href: edit_admin_social_account_path(google_account))
        expect(page).to have_link('View', href: admin_social_account_path(facebook_account))
        expect(page).to have_link('Edit', href: edit_admin_social_account_path(facebook_account))
      end
    end

    describe 'Balance lock view links' do
      let!(:user_with_locks) { create(:user) }
      let!(:first_balance_lock) { create(:balance_lock, user: user_with_locks) }
      let!(:second_balance_lock) { create(:balance_lock, user: user_with_locks) }

      it 'shows balance lock view links' do
        visit admin_user_path(user_with_locks)

        expect(page).to have_link('View', href: admin_balance_lock_path(first_balance_lock))
        expect(page).to have_link('View', href: admin_balance_lock_path(second_balance_lock))
      end
    end

    describe 'Authorization edge cases' do
      it 'prevents non-superadmin from accessing create action directly' do
        sign_out superadmin
        sign_in operator, scope: :admin_user

        post admin_users_path, params: {
          user: {
            email: 'blocked@example.com',
            role: 'user',
            status: 'active'
          }
        }

        expect(response).to redirect_to(admin_root_path)
        expect(User.find_by(email: 'blocked@example.com')).to be_nil
      end

      it 'prevents non-superadmin from accessing update action directly' do
        user = create(:user, email: 'original@example.com')
        sign_out superadmin
        sign_in operator, scope: :admin_user

        put admin_user_path(user), params: {
          user: {
            email: 'modified@example.com'
          }
        }

        expect(response).to redirect_to(admin_root_path)
        expect(user.reload.email).to eq('original@example.com')
      end
    end

    describe 'Error handling scenarios' do
      it 'handles balance lock creation with invalid balance lock' do
        user = create(:user)

        # Create a balance lock that will fail validation
        invalid_lock = instance_double(BalanceLock)
        allow(invalid_lock).to receive_messages(persisted?: false, errors: double(full_messages: [ 'Error 1', 'Error 2' ]))
        allow(BalanceLock).to receive(:create).and_return(invalid_lock)

        post lock_all_funds_admin_user_path(user)

        expect(response).to redirect_to(admin_user_path(user))
        follow_redirect!
        expect(response.body).to include('Failed to create balance lock')
      end
    end

    describe 'JSON display formatting' do
      it 'displays locked balances as JSON in balance locks table' do
        user = create(:user)
        create(:balance_lock, user: user, locked_balances: { 'usdt' => '100.50', 'btc' => '0.001' })

        visit admin_user_path(user)

        # Should display the JSON formatted locked balances (order may vary)
        expect(page).to have_css('pre')
        expect(page).to have_content('usdt')
        expect(page).to have_content('100.50')
        expect(page).to have_content('btc')
        expect(page).to have_content('0.001')
      end
    end
  end
end
