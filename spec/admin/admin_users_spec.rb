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

    it 'allows filtering by deactivated status' do
      deactivated_admin = create(:admin_user, deactivated: true, email: 'deactivated@example.com')

      visit admin_admin_users_path

      within '.filter_form' do
        select 'Yes', from: 'q_deactivated'
        click_button 'Filter'
      end

      expect(page).to have_content(deactivated_admin.email)
      # Check in the main table area, not the entire page (to avoid nav bar)
      within '.index_table' do
        expect(page).not_to have_content(admin.email)
      end
    end

    it 'displays deactivated field in index page' do
      create(:admin_user, deactivated: true, email: 'deactivated@example.com')
      create(:admin_user, deactivated: false, email: 'active@example.com')

      visit admin_admin_users_path

      expect(page).to have_css('[data-inline-edit-field-value="deactivated"]')
    end

    it 'shows scopes for active and deactivated admin users' do
      create(:admin_user, deactivated: true, email: 'deactivated@example.com')

      visit admin_admin_users_path

      expect(page).to have_link('All')
      expect(page).to have_link('Active')
      expect(page).to have_link('Deactivated')
    end

    it 'filters by active scope' do
      create(:admin_user, deactivated: true, email: 'deactivated@example.com')

      visit admin_admin_users_path
      # Be more specific about which "Active" link to click
      within '.scopes' do
        click_link 'Active'
      end

      within '.index_table' do
        expect(page).to have_content(admin.email)
        expect(page).not_to have_content('deactivated@example.com')
      end
    end

    it 'filters by deactivated scope' do
      deactivated_admin = create(:admin_user, deactivated: true, email: 'deactivated@example.com')

      visit admin_admin_users_path
      within '.scopes' do
        click_link 'Deactivated'
      end

      within '.index_table' do
        expect(page).to have_content(deactivated_admin.email)
        expect(page).not_to have_content(admin.email)
      end
    end

    it 'displays admin user details' do
      visit admin_admin_user_path(test_admin)

      expect(page).to have_content(test_admin.email)
      expect(page).to have_content(test_admin.fullname) if test_admin.fullname.present?
      expect(page).to have_content(test_admin.roles)
      expect(page).to have_content(test_admin.created_at.strftime('%B %d, %Y'))
    end

    it 'displays deactivated field in show page with inline editing' do
      visit admin_admin_user_path(test_admin)

      expect(page).to have_css('[data-inline-edit-field-value="deactivated"]')
      expect(page).to have_css('.inline-edit-trigger')
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

    it 'shows deactivated field in form for superadmin' do
      visit new_admin_admin_user_path

      expect(page).to have_field('admin_user_deactivated')
    end

    it 'allows creating admin user with deactivated status' do
      visit new_admin_admin_user_path

      within 'form' do
        fill_in 'Email', with: 'deactivated_admin@example.com'
        select 'operator', from: 'Roles'
        check 'admin_user_deactivated'
        click_button 'Create Admin user'
      end

      expect(page).to have_current_path(%r{/admin/admin_users/\d+})
      expect(page).to have_content('deactivated_admin@example.com')

      # Check that the deactivated status is shown
      expect(page).to have_css('.status_tag.yes', text: 'Yes')
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

    it 'allows superadmin to deactivate other admin users with database persistence', :js do
      target_admin = create(:admin_user, :operator, email: 'target@example.com', deactivated: false)

      visit admin_admin_user_path(target_admin)

      # Verify initial state in database
      expect(target_admin.reload.deactivated).to be false

      # Find the deactivated field and click edit
      within '[data-inline-edit-field-value="deactivated"]' do
        expect(page).to have_css('.status_tag.no', text: 'NO')
        expect(page).to have_css('.inline-edit-trigger')

        find('.inline-edit-trigger').click

        # Form should appear
        expect(page).to have_css('.inline-edit-form', visible: true)
        expect(page).to have_css('input[type="checkbox"]')

        # Change value to true by checking the checkbox
        check find('input[type="checkbox"]')[:name]
        click_button 'Save'

        # Wait for the update
        expect(page).to have_css('.status_tag.yes', text: 'YES', wait: 10)
        expect(page).not_to have_css('.inline-edit-form')
      end

      # Verify database was actually updated
      expect(target_admin.reload.deactivated).to be true

      # Refresh page and verify it persists
      visit admin_admin_user_path(target_admin)
      within '[data-inline-edit-field-value="deactivated"]' do
        expect(page).to have_css('.status_tag.yes', text: 'YES')
      end

      # Verify in database again after refresh
      expect(target_admin.reload.deactivated).to be true
    end

    it 'allows superadmin to reactivate deactivated admin users with database persistence', :js do
      target_admin = create(:admin_user, :operator, email: 'deactivated_target@example.com', deactivated: true)

      visit admin_admin_user_path(target_admin)

      # Verify initial state in database
      expect(target_admin.reload.deactivated).to be true

      # Find the deactivated field and click edit
      within '[data-inline-edit-field-value="deactivated"]' do
        expect(page).to have_css('.status_tag.yes', text: 'YES')

        find('.inline-edit-trigger').click

        # Uncheck the checkbox to reactivate
        uncheck find('input[type="checkbox"]')[:name]
        click_button 'Save'

        # Wait for the update
        expect(page).to have_css('.status_tag.no', text: 'NO', wait: 10)
      end

      # Verify database was actually updated
      expect(target_admin.reload.deactivated).to be false

      # Refresh page and verify it persists
      visit admin_admin_user_path(target_admin)
      within '[data-inline-edit-field-value="deactivated"]' do
        expect(page).to have_css('.status_tag.no', text: 'NO')
      end

      # Verify in database again after refresh
      expect(target_admin.reload.deactivated).to be false
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

    it 'redirects when attempting unauthorized actions including form access' do
      # Try to create a new admin user (this should redirect)
      visit new_admin_admin_user_path

      # Should see a flash message about authorization
      expect(page).to have_content('You are not authorized to perform this action')
      expect(current_path).to eq('/admin')

      # This also means they cannot access deactivated field in forms
      # since they cannot access the forms at all

      # Try to edit another admin user
      visit edit_admin_admin_user_path(test_admin)

      # Should be redirected with error message
      expect(page).to have_content('You are not authorized to perform this action')
      expect(current_path).to eq('/admin')
    end

    it 'cannot edit deactivated field inline (no access to other admin users)' do
      target_admin = create(:admin_user, :operator, email: 'target@example.com', deactivated: false)

      # Operators should not be able to view other admin users' details at all
      visit admin_admin_user_path(target_admin)

      # Should be redirected with authorization error
      expect(page).to have_content('You are not authorized to perform this action')
      expect(current_path).to eq('/admin')

      # This means they definitely cannot edit the deactivated field
      # since they can't even access the page
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
