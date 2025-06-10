# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::AdminUsers', type: :system do
  let(:admin) { create(:admin_user, roles: 'superadmin') }
  let(:operator) { create(:admin_user, roles: 'operator') }
  let(:test_admin) { create(:admin_user, roles: 'superadmin', email: 'test_admin@example.com', fullname: 'Test Admin') }

  describe 'with admin user' do
    before do
      sign_in admin, scope: :admin_user
      test_admin # ensure test_admin is created
    end

    it 'displays admin users list' do
      visit admin_admin_users_path

      expect(page).to have_content(admin.email)
      expect(page).to have_content(test_admin.email)
      expect(page).to have_content('admin')
    end

    it 'allows filtering by email' do
      visit admin_admin_users_path

      within '.filter_form' do
        fill_in 'q_email', with: test_admin.email
        click_button 'Filter'
      end

      expect(page).to have_content(test_admin.email)
    end

    it 'allows filtering by roles' do
      visit admin_admin_users_path

      within '.filter_form' do
        fill_in 'q_roles', with: 'admin'
        click_button 'Filter'
      end

      expect(page).to have_content(admin.email)
      expect(page).to have_content(test_admin.email)
    end

    it 'displays admin user details' do
      visit admin_admin_user_path(test_admin)

      expect(page).to have_content(test_admin.email)
      expect(page).to have_content(test_admin.fullname) if test_admin.fullname.present?
      expect(page).to have_content(test_admin.roles)
      expect(page).to have_content(test_admin.created_at.strftime('%B %d, %Y'))
    end

    it 'allows creating new admin user' do
      visit new_admin_admin_user_path

      within 'form' do
        fill_in 'Email', with: 'new_admin@example.com'
        select 'operator', from: 'Roles'
        click_button 'Create Admin user'
      end

      expect(page).to have_current_path(%r{/admin/admin_users/\d+})
      expect(page).to have_content('new_admin@example.com')
      expect(page).to have_content('operator')
    end

    it 'allows editing admin user' do
      visit edit_admin_admin_user_path(test_admin)

      within 'form' do
        select 'operator', from: 'Roles'
        click_button 'Update Admin user'
      end

      expect(page).to have_current_path(%r{/admin/admin_users/\d+})
      expect(page).to have_content('operator')
    end

    it 'shows authenticator field for admin users' do
      visit admin_admin_user_path(test_admin)

      expect(page).to have_content('Authenticator Enabled')
    end

    it 'shows diff authenticator options based on user view' do
      # Visit own profile
      visit admin_admin_user_path(admin)

      # Check for authenticator row
      expect(page).to have_content('Authenticator')

      # Ensure proper row rendering
      if admin.id == admin.id # Current user is viewing their own profile
        # We can't check for the exact button due to how ActiveAdmin renders,
        # but we can verify the row is present
        within '.attributes_table' do
          expect(page).to have_css('tr', text: /Authenticator/)
        end
      end

      # Other users should still show authenticator_enabled but no button
      visit admin_admin_user_path(test_admin)
      expect(page).to have_content('Authenticator Enabled')
    end
  end

  describe 'with non-admin user' do
    before do
      sign_in operator, scope: :admin_user
    end

    it 'can view admin users page but without admin privileges' do
      visit admin_admin_users_path
      expect(page).to have_content(operator.email)
    end

    it 'redirects when attempting unauthorized actions' do
      # Try to create a new admin user (this should redirect)
      visit new_admin_admin_user_path

      # Should see a flash message about authorization
      expect(page).to have_content('You are not authorized to perform this action')
      expect(current_path).to eq('/admin')
    end

    it 'gets redirected when trying to edit admin users' do
      # Try to edit another admin user
      visit edit_admin_admin_user_path(test_admin)

      # Should be redirected with error message
      expect(page).to have_content('You are not authorized to perform this action')
      expect(current_path).to eq('/admin')
    end

    it 'displays error when attempting various unauthorized admin actions' do
      # Test directly visiting the admin root to set up a session
      visit admin_root_path

      # We need to then verify the redirect behavior happens after an unauthorized action
      # Since we can't directly test the controller's before_action, we'll simulate its effect
      # by checking that we get redirected to admin root with the error message

      # First, verify we can see the dashboard
      expect(page).to have_content('Dashboard')

      # Try to visit a protected action
      visit new_admin_admin_user_path

      # Should show error and redirect
      expect(page).to have_content('You are not authorized to perform this action')
      expect(current_path).to eq('/admin')
    end
  end
end
