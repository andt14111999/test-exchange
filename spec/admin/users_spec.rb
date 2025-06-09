# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::Users', type: :request do
  let(:admin_user) { create(:admin_user, :super_admin) }

  before do
    sign_in admin_user, scope: :admin_user
  end

  describe 'GET /admin/users' do
    it 'displays the users index page' do
      create(:user, email: 'test@example.com', role: 'user')
      create(:user, email: 'merchant@example.com', role: 'merchant')

      get admin_users_path

      expect(response).to have_http_status(:success)
      expect(response.body).to include('test@example.com')
      expect(response.body).to include('merchant@example.com')
    end
  end

  describe 'GET /admin/users/:id' do
    it 'displays the user details page' do
      user = create(:user, email: 'test@example.com', display_name: 'Test User')

      get admin_user_path(user)

      expect(response).to have_http_status(:success)
      expect(response.body).to include('test@example.com')
      expect(response.body).to include('Test User')
    end

    it 'displays social accounts with provider status tag' do
      user = create(:user)
      social_account = create(:social_account, user: user, provider: 'google')

      get admin_user_path(user)

      expect(response).to have_http_status(:success)
      expect(response.body).to include('status_tag google">Google</span>')
    end

    it 'displays social account avatar when present' do
      user = create(:user)
      social_account = create(:social_account,
        user: user,
        avatar_url: 'https://example.com/avatar.jpg'
      )

      get admin_user_path(user)

      expect(response).to have_http_status(:success)
      expect(response.body).to include('width: 50px; height: 50px; border-radius: 50%;')
      expect(response.body).to include('https://example.com/avatar.jpg')
    end

    it 'displays social account actions' do
      user = create(:user)
      social_account = create(:social_account, user: user)

      get admin_user_path(user)

      expect(response).to have_http_status(:success)
      expect(response.body).to include(admin_social_account_path(social_account))
      expect(response.body).to include(edit_admin_social_account_path(social_account))
      expect(response.body).to include('View')
      expect(response.body).to include('Edit')
      expect(response.body).to include('|')
    end
  end

  describe 'GET /admin/users/new' do
    it 'displays the new user form' do
      get new_admin_user_path

      expect(response).to have_http_status(:success)
      expect(response.body).to include('New User')
    end
  end

  describe 'POST /admin/users' do
    it 'creates a new user' do
      user_params = {
        user: {
          email: 'new@example.com',
          username: 'newuser123',
          display_name: 'New User',
          role: 'user',
          status: 'active',
          kyc_level: 0,
          phone_verified: false,
          document_verified: false
        }
      }

      expect {
        post admin_users_path, params: user_params
      }.to change(User, :count).by(1)

      expect(response).to redirect_to(admin_user_path(User.last))
      follow_redirect!
      expect(response.body).to include('User was successfully created')
      expect(User.last.username).to eq('newuser123')
    end
  end

  describe 'GET /admin/users/:id/edit' do
    it 'displays the edit user form' do
      user = create(:user)

      get edit_admin_user_path(user)

      expect(response).to have_http_status(:success)
      expect(response.body).to include('Edit User')
    end
  end

  describe 'PUT /admin/users/:id' do
    it 'updates the user' do
      user = create(:user, email: 'old@example.com')
      new_email = 'new@example.com'

      put admin_user_path(user), params: { user: { email: new_email } }

      expect(response).to redirect_to(admin_user_path(user))
      follow_redirect!
      expect(response.body).to include('User was successfully updated')
      expect(user.reload.email).to eq(new_email)
    end

    it 'sets username when it was not set' do
      user = create(:user, username: nil)

      put admin_user_path(user), params: { user: { username: 'newusername' } }

      expect(response).to redirect_to(admin_user_path(user))
      follow_redirect!
      expect(response.body).to include('User was successfully updated')
      expect(user.reload.username).to eq('newusername')
    end

    it 'cannot change username when it is already set' do
      user = create(:user, username: 'existingusername')

      put admin_user_path(user), params: { user: { username: 'newusername' } }

      expect(user.reload.username).to eq('existingusername')
    end
  end

  describe 'DELETE /admin/users/:id' do
    it 'deletes the user' do
      user = create(:user)

      expect {
        delete admin_user_path(user)
      }.to change(User, :count).by(-1)

      expect(response).to redirect_to(admin_users_path)
      follow_redirect!
      expect(response.body).to include('User was successfully destroyed')
    end
  end

  describe 'Batch Actions' do
    let!(:users) { create_list(:user, 3, phone_verified: false, document_verified: false, status: 'active') }

    describe 'verify_phone' do
      it 'verifies phone for selected users' do
        post batch_action_admin_users_path, params: {
          batch_action: 'verify_phone',
          collection_selection: users.map(&:id)
        }

        expect(response).to redirect_to(admin_users_path)
        follow_redirect!
        expect(response.body).to include('Phone verification status updated successfully')
        users.each { |user| expect(user.reload.phone_verified).to be true }
      end
    end

    describe 'verify_document' do
      it 'verifies document for selected users' do
        post batch_action_admin_users_path, params: {
          batch_action: 'verify_document',
          collection_selection: users.map(&:id)
        }

        expect(response).to redirect_to(admin_users_path)
        follow_redirect!
        expect(response.body).to include('Document verification status updated successfully')
        users.each { |user| expect(user.reload.document_verified).to be true }
      end
    end

    describe 'suspend' do
      it 'suspends selected users' do
        post batch_action_admin_users_path, params: {
          batch_action: 'suspend',
          collection_selection: users.map(&:id)
        }

        expect(response).to redirect_to(admin_users_path)
        follow_redirect!
        expect(response.body).to include('Selected users have been suspended')
        users.each { |user| expect(user.reload.status).to eq('suspended') }
      end
    end

    describe 'activate' do
      before do
        users.each { |user| user.update(status: 'suspended') }
      end

      it 'activates selected users' do
        post batch_action_admin_users_path, params: {
          batch_action: 'activate',
          collection_selection: users.map(&:id)
        }

        expect(response).to redirect_to(admin_users_path)
        follow_redirect!
        expect(response.body).to include('Selected users have been activated')
        users.each { |user| expect(user.reload.status).to eq('active') }
      end
    end

    describe 'ban' do
      it 'bans selected users' do
        post batch_action_admin_users_path, params: {
          batch_action: 'ban',
          collection_selection: users.map(&:id)
        }

        expect(response).to redirect_to(admin_users_path)
        follow_redirect!
        expect(response.body).to include('Selected users have been banned')
        users.each { |user| expect(user.reload.status).to eq('banned') }
      end
    end
  end

  describe 'Member Actions' do
    let(:user) { create(:user, status: 'active') }

    describe 'activate' do
      before do
        user.update(status: 'suspended')
      end

      it 'activates the user' do
        put activate_admin_user_path(user)

        expect(response).to redirect_to(admin_user_path(user))
        follow_redirect!
        expect(response.body).to include('User has been activated')
        expect(user.reload.status).to eq('active')
      end
    end

    describe 'suspend' do
      it 'suspends the user' do
        put suspend_admin_user_path(user)

        expect(response).to redirect_to(admin_user_path(user))
        follow_redirect!
        expect(response.body).to include('User has been suspended')
        expect(user.reload.status).to eq('suspended')
      end
    end

    describe 'ban' do
      it 'bans the user' do
        put ban_admin_user_path(user)

        expect(response).to redirect_to(admin_user_path(user))
        follow_redirect!
        expect(response.body).to include('User has been banned')
        expect(user.reload.status).to eq('banned')
      end
    end
  end
end
