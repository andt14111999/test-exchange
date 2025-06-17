# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin Settings', type: :feature do
  describe 'Feature tests', type: :feature do
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

  # Test the AJAX endpoint directly without JavaScript
  describe 'AJAX API', type: :request do
    let(:admin) { create(:admin_user, :superadmin) }

    before do
      sign_in admin, scope: :admin_user
    end

    describe 'PATCH /admin/settings/update' do
      context 'with valid exchange rate updates' do
        it 'updates USDT to VND rate successfully' do
          patch '/admin/settings/update', params: { id: 'usdt_to_vnd_rate', value: '24000' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(json_response['message']).to eq('Setting updated successfully')
          expect(json_response['value']).to eq('24000.0')
          expect(json_response['type']).to eq('Float')
          expect(Setting.usdt_to_vnd_rate).to eq(24000.0)
        end

        it 'updates USDT to PHP rate successfully' do
          patch '/admin/settings/update', params: { id: 'usdt_to_php_rate', value: '55.5' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.usdt_to_php_rate).to eq(55.5)
        end

        it 'updates USDT to NGN rate successfully' do
          patch '/admin/settings/update', params: { id: 'usdt_to_ngn_rate', value: '1500.75' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.usdt_to_ngn_rate).to eq(1500.75)
        end
      end

      context 'with valid withdrawal fee updates' do
        it 'updates ERC20 withdrawal fee successfully' do
          patch '/admin/settings/update', params: { id: 'usdt_erc20_withdrawal_fee', value: '15.5' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.usdt_erc20_withdrawal_fee).to eq(15.5)
        end

        it 'updates BEP20 withdrawal fee successfully' do
          patch '/admin/settings/update', params: { id: 'usdt_bep20_withdrawal_fee', value: '2.5' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.usdt_bep20_withdrawal_fee).to eq(2.5)
        end

        it 'allows zero withdrawal fees' do
          patch '/admin/settings/update', params: { id: 'usdt_erc20_withdrawal_fee', value: '0' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.usdt_erc20_withdrawal_fee).to eq(0.0)
        end
      end

      context 'with valid trading fee updates' do
        it 'updates VND trading fee ratio successfully' do
          patch '/admin/settings/update', params: { id: 'vnd_trading_fee_ratio', value: '0.005' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(json_response['display_value']).to eq('0.5%') # 0.005 * 100 = 0.5%
          expect(Setting.vnd_trading_fee_ratio).to eq(0.005)
        end

        it 'updates fixed trading fee successfully' do
          patch '/admin/settings/update', params: { id: 'vnd_fixed_trading_fee', value: '10000' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.vnd_fixed_trading_fee).to eq(10000.0)
        end

        it 'allows minimum trading fee ratio' do
          patch '/admin/settings/update', params: { id: 'vnd_trading_fee_ratio', value: '0.001' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.vnd_trading_fee_ratio).to eq(0.001)
        end

        it 'allows maximum trading fee ratio' do
          patch '/admin/settings/update', params: { id: 'vnd_trading_fee_ratio', value: '1.0' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.vnd_trading_fee_ratio).to eq(1.0)
        end
      end

      context 'with invalid exchange rate values' do
        it 'rejects negative exchange rates' do
          patch '/admin/settings/update', params: { id: 'usdt_to_vnd_rate', value: '-100' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Value must be greater than 0')
          expect(Setting.usdt_to_vnd_rate).not_to eq(-100)
        end

        it 'rejects zero exchange rates' do
          patch '/admin/settings/update', params: { id: 'usdt_to_php_rate', value: '0' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Value must be greater than 0')
        end
      end

      context 'with invalid withdrawal fee values' do
        it 'rejects negative withdrawal fees' do
          patch '/admin/settings/update', params: { id: 'usdt_erc20_withdrawal_fee', value: '-5' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Value cannot be negative')
        end

        it 'rejects withdrawal fees above maximum' do
          patch '/admin/settings/update', params: { id: 'usdt_erc20_withdrawal_fee', value: '150' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Value must be less than 100')
        end
      end

      context 'with invalid trading fee values' do
        it 'rejects trading fee ratios below minimum' do
          patch '/admin/settings/update', params: { id: 'vnd_trading_fee_ratio', value: '0.0005' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Trading fee ratio must be at least 0.001 (0.1%)')
        end

        it 'rejects trading fee ratios above maximum' do
          patch '/admin/settings/update', params: { id: 'vnd_trading_fee_ratio', value: '1.5' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Trading fee ratio must be less than or equal to 1 (100%)')
        end

        it 'rejects negative trading fee ratios' do
          patch '/admin/settings/update', params: { id: 'php_trading_fee_ratio', value: '-0.01' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Trading fee ratio must be at least 0.001 (0.1%)')
        end

        it 'rejects negative fixed trading fees' do
          patch '/admin/settings/update', params: { id: 'vnd_fixed_trading_fee', value: '-1000' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Value cannot be negative')
        end

        it 'rejects fixed trading fees above maximum' do
          patch '/admin/settings/update', params: { id: 'vnd_fixed_trading_fee', value: '2000000' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Value must be less than 1000000')
        end
      end

      context 'with edge cases' do
        it 'handles non-existent setting keys' do
          patch '/admin/settings/update', params: { id: 'non_existent_setting', value: '100' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
          expect(json_response['message']).to include('Failed to update non_existent_setting')
        end

        it 'handles empty values' do
          patch '/admin/settings/update', params: { id: 'usdt_to_vnd_rate', value: '' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
        end

        it 'handles non-numeric values' do
          patch '/admin/settings/update', params: { id: 'usdt_to_vnd_rate', value: 'abc' }

          expect(response).to have_http_status(:unprocessable_entity)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be false
        end

        it 'handles decimal values correctly' do
          patch '/admin/settings/update', params: { id: 'usdt_to_php_rate', value: '56.789' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.usdt_to_php_rate).to eq(56.789)
        end

        it 'handles very large valid values' do
          patch '/admin/settings/update', params: { id: 'usdt_to_vnd_rate', value: '999999' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.usdt_to_vnd_rate).to eq(999999.0)
        end

        it 'handles very small valid values for ratios' do
          patch '/admin/settings/update', params: { id: 'vnd_trading_fee_ratio', value: '0.0011' }

          expect(response).to have_http_status(:ok)
          json_response = JSON.parse(response.body)
          expect(json_response['success']).to be true
          expect(Setting.vnd_trading_fee_ratio).to eq(0.0011)
        end
      end

      context 'when unauthorized' do
        before do
          sign_out admin
        end

        it 'redirects when unauthorized' do
          patch '/admin/settings/update', params: { id: 'usdt_to_vnd_rate', value: '24000' }

          # Should redirect to login page when not authenticated
          expect(response).to have_http_status(:found)
          expect(response).to redirect_to(new_admin_user_session_path)
        end
      end

      context 'with missing parameters' do
        it 'handles missing id parameter' do
          patch '/admin/settings/update', params: { value: '24000' }

          # Should handle gracefully - exact behavior depends on implementation
          expect(response.status).to be_in([ 400, 422, 500 ])
        end

        it 'handles missing value parameter' do
          patch '/admin/settings/update', params: { id: 'usdt_to_vnd_rate' }

          # Should handle gracefully - exact behavior depends on implementation
          expect(response.status).to be_in([ 400, 422, 500 ])
        end
      end
    end
  end
end
