# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'AdminUser', type: :feature do
  describe 'index page' do
    context 'when user is admin' do
      it 'display the list all users when user is admin' do
        admin = create(:admin_user, :admin, fullname: 'Admin Name')
        create(:admin_user, :developer, fullname: 'Developer Name')
        create(:admin_user, :implementor, fullname: 'Implementor Name')
        create(:admin_user, :explorer, fullname: 'Explorer Name')
        sign_in(admin, scope: :admin_user)
        visit '/admin/admin_users'
        expect(page).to have_content('Admin Name')
        expect(page).to have_content('Developer Name')
        expect(page).to have_content('Implementor Name')
        expect(page).to have_content('Explorer Name')
        expect(page).to have_content('New Admin User')
      end
    end

    context 'when user is not admin' do
      it 'display only the current user' do
        user = create(:admin_user, :developer, fullname: 'Developer Name')
        create(:admin_user, :admin, fullname: 'Admin Name')
        sign_in(user, scope: :admin_user)
        visit '/admin/admin_users'
        expect(page).to have_content('Developer Name')
        expect(page).not_to have_content('Admin Name')
        expect(page).not_to have_content('Implementor Name')
        expect(page).not_to have_content('Explorer Name')
      end
    end
  end
end
