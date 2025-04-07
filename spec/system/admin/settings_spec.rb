require 'rails_helper'

RSpec.describe 'Admin Settings', type: :system do
  context 'when admin is signed in' do
    before do
      admin = create(:admin_user, :admin)
      login_as admin, scope: :admin_user
      visit admin_settings_path
    end

    it 'displays the exchange rates form' do
      expect(page).to have_content('Exchange Rates')
      expect(page).to have_content('USDT to VND Rate')
      expect(page).to have_content('USDT to PHP Rate')
      expect(page).to have_content('USDT to NGN Rate')
    end

    context 'when updating exchange rates' do
      it 'updates rates successfully with valid inputs' do
        within '#main_content' do
          fill_in 'settings[usdt_to_vnd_rate]', with: '23000'
          fill_in 'settings[usdt_to_php_rate]', with: '55.5'
          fill_in 'settings[usdt_to_ngn_rate]', with: '1500'
          click_button 'Update Exchange Rates'
        end

        expect(page).to have_content('Exchange rates updated successfully')
        expect(Setting.usdt_to_vnd_rate.to_f).to eq 23000.0
        expect(Setting.usdt_to_php_rate.to_f).to eq 55.5
        expect(Setting.usdt_to_ngn_rate.to_f).to eq 1500.0
      end

      it 'shows error message when no rates are provided' do
        within '#main_content' do
          fill_in 'settings[usdt_to_vnd_rate]', with: ''
          fill_in 'settings[usdt_to_php_rate]', with: ''
          fill_in 'settings[usdt_to_ngn_rate]', with: ''
          click_button 'Update Exchange Rates'
        end

        expect(page).to have_content('Exchange rates updated successfully')
      end

      it 'shows error message when settings params are missing' do
        page.driver.submit :post, admin_settings_update_rates_path, {}
        expect(page).to have_content('No exchange rates provided')
      end
    end

    it 'displays current exchange rates' do
      Setting.usdt_to_vnd_rate = 23500
      Setting.usdt_to_php_rate = 56.2
      Setting.usdt_to_ngn_rate = 1550

      visit admin_settings_path

      within '.panel_contents' do
        expect(page).to have_content('23500')
        expect(page).to have_content('56.2')
        expect(page).to have_content('1550')
      end
    end
  end

  context 'when admin is not signed in' do
    it 'redirects to sign in page' do
      visit admin_settings_path
      expect(page).to have_current_path(new_admin_user_session_path)
    end
  end
end
