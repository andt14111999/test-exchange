require 'rails_helper'

RSpec.describe 'Admin Settings', type: :feature do
  context 'when admin is signed in' do
    before do
      admin = create(:admin_user, :superadmin)
      login_as admin, scope: :admin_user
      visit admin_settings_path
    end

    it 'displays the settings table' do
      expect(page).to have_content('Settings')
      expect(page).to have_content('Name')
      expect(page).to have_content('Type')
      expect(page).to have_content('Current Value')
      expect(page).to have_content('Input')
    end

    it 'displays all setting fields in table' do
      # Exchange rates
      expect(page).to have_content('Usdt to vnd rate')
      expect(page).to have_content('Usdt to php rate')
      expect(page).to have_content('Usdt to ngn rate')

      # Withdrawal fees
      expect(page).to have_content('Usdt erc20 withdrawal fee')
      expect(page).to have_content('Usdt bep20 withdrawal fee')

      # Trading fees
      expect(page).to have_content('Vnd trading fee ratio')
      expect(page).to have_content('Php trading fee ratio')
      expect(page).to have_content('Vnd fixed trading fee')
    end

    it 'shows current values and types correctly' do
      # Check that values are displayed
      expect(page).to have_content('25000.0') # Default VND rate
      expect(page).to have_content('57.0')    # Default PHP rate
      expect(page).to have_content('450.0')   # Default NGN rate

      # Check that types are shown as Float
      expect(page).to have_content('Float')
    end

    it 'shows percentage display for ratio fields' do
      # Trading fee ratios should show as percentages
      expect(page).to have_content('0.1%') # 0.001 * 100 = 0.1%
    end

    it 'has input fields for all settings' do
      # Check for input fields with correct selectors
      expect(page).to have_css('input.setting-input')
      expect(page).to have_css('input[id*="setting_"]')
      expect(page).to have_css('input[data-key]')
    end

    it 'has proper input attributes for validation' do
      # Check exchange rate inputs (no min/max)
      vnd_input = find('input[data-key="usdt_to_vnd_rate"]')
      expect(vnd_input[:type]).to eq('number')
      expect(vnd_input[:step]).to eq('any')
      expect(vnd_input[:min]).to be_nil
      expect(vnd_input[:max]).to be_nil

      # Check trading fee ratio inputs (with bounds)
      ratio_input = find('input[data-key="vnd_trading_fee_ratio"]')
      expect(ratio_input[:type]).to eq('number')
      expect(ratio_input[:min]).to eq('0.001')
      expect(ratio_input[:max]).to eq('1')
      expect(ratio_input[:step]).to eq('0.0001')

      # Check withdrawal fee inputs (with bounds)
      fee_input = find('input[data-key="usdt_erc20_withdrawal_fee"]')
      expect(fee_input[:type]).to eq('number')
      expect(fee_input[:min]).to eq('0')
      expect(fee_input[:max]).to eq('100')
    end

    it 'includes JavaScript for AJAX functionality' do
      # Verify that the JavaScript code is present in the page
      expect(page.html).to include('window.updateSetting')
      expect(page.html).to include('showNotification')
      expect(page.html).to include('/admin/settings/update')
    end
  end

  context 'when admin is not signed in' do
    it 'redirects to sign in page' do
      visit admin_settings_path
      expect(page).to have_current_path(new_admin_user_session_path)
    end
  end
end
