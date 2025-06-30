# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::Users snowfox_employee field', type: :request do
  describe 'GET /admin/users/:id' do
    it 'displays the snowfox_employee field with inline edit functionality' do
      admin_user = create(:admin_user, :superadmin)
      sign_in admin_user, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      get admin_user_path(user)

      expect(response).to have_http_status(:success)

      # Check that the snowfox_employee field is displayed
      expect(response.body).to include('Snowfox Employee')

      # Check for inline edit container
      expect(response.body).to include('inline-edit-container')
      expect(response.body).to include('data-controller="inline-edit"')

      # Check for the field value
      expect(response.body).to include('data-inline-edit-field-value="snowfox_employee"')

      # Check for the display value (should be "No" since it's false)
      expect(response.body).to include('<span class="status_tag no">No</span>')

      # Check for edit trigger
      expect(response.body).to include('inline-edit-trigger')
      expect(response.body).to include('✏️')
    end

    it 'displays Yes for snowfox employees' do
      admin_user = create(:admin_user, :superadmin)
      sign_in admin_user, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: true)

      get admin_user_path(user)

      expect(response).to have_http_status(:success)
      expect(response.body).to include('<span class="status_tag yes">Yes</span>')
    end
  end

  describe 'PUT /admin/users/:id with inline edit' do
    it 'updates the snowfox_employee field via inline edit' do
      admin_user = create(:admin_user, :superadmin)
      sign_in admin_user, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      put admin_user_path(user),
          params: { user: { snowfox_employee: true }, inline_edit: true },
          headers: { 'Accept' => 'application/json' }

      expect(response).to have_http_status(:success)
      expect(response.content_type).to match(/json/)

      json_response = JSON.parse(response.body)
      expect(json_response['snowfox_employee']).to be true

      # Verify the database was updated
      expect(user.reload.snowfox_employee).to be true
    end

    it 'returns only the updated field in JSON response' do
      admin_user = create(:admin_user, :superadmin)
      sign_in admin_user, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      put admin_user_path(user),
          params: { user: { snowfox_employee: true }, inline_edit: true },
          headers: { 'Accept' => 'application/json' }

      json_response = JSON.parse(response.body)
      expect(json_response.keys).to eq([ 'snowfox_employee' ])
    end

    it 'handles validation errors properly' do
      admin_user = create(:admin_user, :superadmin)
      sign_in admin_user, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      # Simulate a validation error by trying to update with invalid email
      put admin_user_path(user),
          params: { user: { email: 'invalid-email' }, inline_edit: true },
          headers: { 'Accept' => 'application/json' }

      expect(response).to have_http_status(:unprocessable_entity)

      json_response = JSON.parse(response.body)
      expect(json_response['errors']).to include('Email is invalid')
    end
  end

  describe 'Normal update (not inline edit)' do
    it 'follows the standard update flow' do
      admin_user = create(:admin_user, :superadmin)
      sign_in admin_user, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      put admin_user_path(user),
          params: { user: { display_name: 'Updated Name' } }

      expect(response).to redirect_to(admin_user_path(user))
      follow_redirect!

      expect(response.body).to include('User was successfully updated')
      expect(user.reload.display_name).to eq('Updated Name')
    end
  end

  describe 'Permission restrictions for snowfox_employee field' do
    it 'allows superadmin to update snowfox_employee via inline edit' do
      admin_user = create(:admin_user, :superadmin)
      sign_in admin_user, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      put admin_user_path(user),
          params: { user: { snowfox_employee: true }, inline_edit: true },
          headers: { 'Accept' => 'application/json' }

      expect(response).to have_http_status(:success)
      expect(user.reload.snowfox_employee).to be true
    end

    it 'prevents operator from updating snowfox_employee via inline edit' do
      operator = create(:admin_user, :operator)
      sign_in operator, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      put admin_user_path(user),
          params: { user: { snowfox_employee: true }, inline_edit: true },
          headers: { 'Accept' => 'application/json' }

      # ActiveAdmin returns 401 for CanCan authorization failures in JSON requests
      expect(response).to have_http_status(:unauthorized)
      # The response body might be different for unauthorized requests
      expect(user.reload.snowfox_employee).to be false
    end

    it 'prevents operator from updating snowfox_employee via normal update' do
      operator = create(:admin_user, :operator)
      sign_in operator, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      put admin_user_path(user),
          params: { user: { snowfox_employee: true } }

      # ActiveAdmin redirects to admin root for unauthorized access
      expect(response).to redirect_to(admin_root_path)
      follow_redirect!
      expect(response.body).to match(/not authorized|You are not authorized/)
      expect(user.reload.snowfox_employee).to be false
    end

    it 'prevents operator from updating any user fields' do
      operator = create(:admin_user, :operator)
      sign_in operator, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      put admin_user_path(user),
          params: { user: { display_name: 'Updated by Operator' }, inline_edit: true },
          headers: { 'Accept' => 'application/json' }

      # ActiveAdmin returns 401 for CanCan authorization failures in JSON requests
      expect(response).to have_http_status(:unauthorized)
      expect(user.reload.display_name).to eq('Test User')
    end

    it 'allows operator to read user details' do
      operator = create(:admin_user, :operator)
      sign_in operator, scope: :admin_user
      user = create(:user, email: 'test@example.com', display_name: 'Test User', snowfox_employee: false)

      get admin_user_path(user)

      expect(response).to have_http_status(:success)
      expect(response.body).to include('test@example.com')
      expect(response.body).to include('Test User')
    end
  end
end
