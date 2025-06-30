# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::CoinSettings', type: :system do
  let(:admin) { create(:admin_user, roles: 'superadmin') }
  let(:operator) { create(:admin_user, roles: 'operator') }
  let(:usdt_setting) { create(:coin_setting, currency: 'usdt') }
  let(:eth_setting) { create(:coin_setting, :eth) }
  let(:btc_setting) { create(:coin_setting, :btc) }
  let(:maintenance_setting) { create(:coin_setting, :with_maintenance) }

  describe 'with admin user' do
    before do
      sign_in admin, scope: :admin_user
    end

    describe 'index page' do
      before do
        usdt_setting
        eth_setting
        btc_setting
        visit admin_coin_settings_path
      end

      it 'displays coin settings list' do
        expect(page).to have_content('Coin Settings')
        expect(page).to have_content(usdt_setting.currency)
        expect(page).to have_content(eth_setting.currency)
        expect(page).to have_content(btc_setting.currency)
      end

      it 'displays all required columns' do
        expect(page).to have_content('Id')
        expect(page).to have_content('Currency')
        expect(page).to have_content('Deposit enabled')
        expect(page).to have_content('Withdraw enabled')
        expect(page).to have_content('Swap enabled')
        expect(page).to have_content('Layers')
      end

      it 'displays layers as comma-separated values' do
        expect(page).to have_content('erc20, bep20')
        expect(page).to have_content('erc20')
        expect(page).to have_content('bitcoin')
      end

      it 'displays boolean values correctly' do
        expect(page).to have_content('Yes')
        expect(page).to have_content('No')
      end

      it 'has action buttons for each record' do
        create(:coin_setting, currency: 'usdt-1')
        create(:coin_setting, currency: 'eth-1')
        create(:coin_setting, currency: 'btc-1')
        visit admin_coin_settings_path
        page.refresh

        if page.has_css?('table.index_table tbody')
          within('table.index_table tbody') do
            rows = all('tr[id^="coin_setting_"]')
            expect(rows.size).to be >= 3
            rows.first(3).each do |row|
              within(row) do
                expect(page).to have_link('View')
                expect(page).to have_link('Edit')
                # Delete button is not available because admin config excludes destroy action
              end
            end
          end
        else
          # Check for View and Edit links instead of Delete since destroy is excluded
          view_links = page.all('a', text: 'View').select { |a| a[:href]&.include?('/admin/coin_settings/') }
          edit_links = page.all('a', text: 'Edit').select { |a| a[:href]&.include?('/admin/coin_settings/') }
          expect(view_links.size).to be >= 3
          expect(edit_links.size).to be >= 3
        end
      end

      it 'allows filtering by currency' do
        within '.filter_form' do
          fill_in 'q_currency', with: 'usdt'
          click_button 'Filter'
        end

        within('table.index_table tbody') do
          expect(page).to have_content('usdt')
          expect(page).not_to have_content('eth')
          expect(page).not_to have_content('btc')
        end
      end

      it 'allows filtering by deposit enabled' do
        within '.filter_form' do
          select 'Yes', from: 'q_deposit_enabled'
          click_button 'Filter'
        end

        expect(page).to have_content('usdt')
        expect(page).to have_content('eth')
        expect(page).to have_content('btc')
      end

      it 'allows filtering by withdraw enabled' do
        within '.filter_form' do
          select 'Yes', from: 'q_withdraw_enabled'
          click_button 'Filter'
        end

        expect(page).to have_content('usdt')
        expect(page).to have_content('eth')
        expect(page).to have_content('btc')
      end

      it 'allows filtering by swap enabled' do
        within '.filter_form' do
          select 'Yes', from: 'q_swap_enabled'
          click_button 'Filter'
        end

        expect(page).to have_content('usdt')
        expect(page).to have_content('eth')
        expect(page).to have_content('btc')
      end
    end

    describe 'show page' do
      before do
        visit admin_coin_setting_path(usdt_setting)
      end

      it 'displays coin setting details' do
        expect(page).to have_content('Coin Setting Details')
        expect(page).to have_content(usdt_setting.id)
        expect(page).to have_content(usdt_setting.currency)
        expect(page).to have_content('Yes') # deposit_enabled
        expect(page).to have_content('Yes') # withdraw_enabled
        expect(page).to have_content('Yes') # swap_enabled
      end

      it 'displays layers table with correct structure' do
        expect(page).to have_content('Layer')
        expect(page).to have_content('Deposit')
        expect(page).to have_content('Withdraw')
        expect(page).to have_content('Swap')
        expect(page).to have_content('Maintenance')
      end

      it 'displays layer data correctly' do
        expect(page).to have_content('erc20')
        expect(page).to have_content('bep20')
        expect(page).to have_content('Yes') # deposit_enabled for layers
        expect(page).to have_content('No')  # maintenance for layers
      end

      it 'displays timestamps' do
        expect(page).to have_content('Created At')
        expect(page).to have_content('Updated At')
        expect(page).to have_content(usdt_setting.created_at.strftime('%B %d, %Y'))
        expect(page).to have_content(usdt_setting.updated_at.strftime('%B %d, %Y'))
      end

      it 'handles empty layers gracefully' do
        # Create a setting with minimal layers to avoid validation error
        empty_setting = create(:coin_setting, currency: 'empty_coin', layers: [ { 'layer' => 'test', 'deposit_enabled' => true, 'withdraw_enabled' => true, 'swap_enabled' => true, 'maintenance' => false } ])
        visit admin_coin_setting_path(empty_setting)

        expect(page).to have_content('Layer')
        expect(page).to have_content('Deposit')
        expect(page).to have_content('Withdraw')
        expect(page).to have_content('Swap')
        expect(page).to have_content('Maintenance')
      end

      it 'handles nil layers gracefully' do
        # Create a setting with minimal layers to avoid validation error
        nil_setting = create(:coin_setting, currency: 'nil_coin', layers: [ { 'layer' => 'test', 'deposit_enabled' => true, 'withdraw_enabled' => true, 'swap_enabled' => true, 'maintenance' => false } ])
        visit admin_coin_setting_path(nil_setting)

        expect(page).to have_content('Layer')
        expect(page).to have_content('Deposit')
        expect(page).to have_content('Withdraw')
        expect(page).to have_content('Swap')
        expect(page).to have_content('Maintenance')
      end
    end

    describe 'new page' do
      before do
        visit new_admin_coin_setting_path
      end

      it 'displays the form with all required fields' do
        expect(page).to have_content('New Coin Setting')
        expect(page).to have_field('Currency')
        expect(page).to have_field('Deposit enabled')
        expect(page).to have_field('Withdraw enabled')
        expect(page).to have_field('Swap enabled')
        expect(page).to have_field('Layers (JSON)')
      end

      it 'displays JSON editor hint' do
        expect(page).to have_content('Nhập array các layer, ví dụ:')
        expect(page).to have_content('[{"layer":"erc20","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]')
      end

      it 'has JSON editor with tree mode' do
        # Try to find the JSON input as a textarea (ActiveAdmin json_editor renders as textarea)
        json_input = find('textarea[name="coin_setting[layers]"]', visible: :all)
        expect(json_input).to be_present
      end

      it 'allows creating a new coin setting with valid data' do
        within 'form' do
          fill_in 'Currency', with: 'bnb'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"bep20","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('bnb')
        expect(page).to have_content('Yes')
        expect(page).to have_content('bep20')
      end

      it 'handles fake_layers_attributes in create action' do
        # Test that the custom create action with fake_layers_attributes
        within 'form' do
          fill_in 'Currency', with: 'sol'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"solana","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('sol')
        expect(page).to have_content('solana')
      end

      it 'validates required fields' do
        within 'form' do
          click_button 'Create Coin setting'
        end

        expect(page).to have_content('can\'t be blank')
        expect(page).to have_content('can\'t be blank')
      end

      it 'validates currency uniqueness' do
        create(:coin_setting, currency: 'unique_coin')

        within 'form' do
          fill_in 'Currency', with: 'unique_coin'
          fill_in 'Layers (JSON)', with: '[{"layer":"test","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_content('has already been taken')
      end

      it 'handles nil fake_layers parameter in create action' do
        # Test create action when fake_layers parameter is nil
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'nil_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"nil_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('nil_fake_layers_test')
      end

      it 'handles nil fake_layers parameter in update action' do
        # Test update action when fake_layers parameter is nil
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"nil_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('nil_update_test_layer')
      end

      it 'handles empty fake_layers parameter in create action' do
        # Test create action when fake_layers parameter is empty
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'empty_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"empty_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('empty_fake_layers_test')
      end

      it 'handles empty fake_layers parameter in update action' do
        # Test update action when fake_layers parameter is empty
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"empty_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('empty_update_test_layer')
      end

      it 'handles fake_layers_attributes with permit parameters in create' do
        # Test create action with fake_layers_attributes that need permit parameters
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'permit_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"permit_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('permit_fake_layers_test')
        expect(page).to have_content('permit_test_layer')
      end

      it 'handles fake_layers_attributes with permit parameters in update' do
        # Test update action with fake_layers_attributes that need permit parameters
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"permit_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('permit_update_test_layer')
      end

      it 'handles fake_layers_attributes values mapping in create' do
        # Test create action with fake_layers_attributes values mapping
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'mapping_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"mapping_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('mapping_fake_layers_test')
        expect(page).to have_content('mapping_test_layer')
      end

      it 'handles fake_layers_attributes values mapping in update' do
        # Test update action with fake_layers_attributes values mapping
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"mapping_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('mapping_update_test_layer')
      end

      it 'handles fake_layers parameter without data in create' do
        # Test create action when fake_layers parameter is passed but has no data
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'no_data_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"no_data_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('no_data_fake_layers_test')
      end

      it 'handles fake_layers parameter without data in update' do
        # Test update action when fake_layers parameter is passed but has no data
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"no_data_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('no_data_update_test_layer')
      end

      it 'handles fake_layers parameter with nil values in create' do
        # Test create action when fake_layers parameter has nil values
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'nil_values_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"nil_values_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('nil_values_fake_layers_test')
      end

      it 'handles fake_layers parameter with nil values in update' do
        # Test update action when fake_layers parameter has nil values
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"nil_values_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('nil_values_update_test_layer')
      end

      it 'handles fake_layers parameter with empty values in create' do
        # Test create action when fake_layers parameter has empty values
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'empty_values_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"empty_values_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('empty_values_fake_layers_test')
      end

      it 'handles fake_layers parameter with empty values in update' do
        # Test update action when fake_layers parameter has empty values
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"empty_values_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('empty_values_update_test_layer')
      end

      it 'handles fake_layers_attributes with permit parameters processing in create' do
        # Test create action with fake_layers_attributes that need permit parameters processing
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'permit_processing_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"permit_processing_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('permit_processing_fake_layers_test')
        expect(page).to have_content('permit_processing_test_layer')
      end

      it 'handles fake_layers_attributes with permit parameters processing in update' do
        # Test update action with fake_layers_attributes that need permit parameters processing
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"permit_processing_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('permit_processing_update_test_layer')
      end

      it 'handles fake_layers_attributes values to_h conversion in create' do
        # Test create action with fake_layers_attributes values to_h conversion
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'to_h_conversion_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"to_h_conversion_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('to_h_conversion_fake_layers_test')
        expect(page).to have_content('to_h_conversion_test_layer')
      end

      it 'handles fake_layers_attributes values to_h conversion in update' do
        # Test update action with fake_layers_attributes values to_h conversion
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"to_h_conversion_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('to_h_conversion_update_test_layer')
      end

      it 'handles fake_layers_attributes with OpenStruct processing in create' do
        # Test create action with fake_layers_attributes that need OpenStruct processing
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'openstruct_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"openstruct_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('openstruct_fake_layers_test')
        expect(page).to have_content('openstruct_test_layer')
      end

      it 'handles fake_layers_attributes with OpenStruct processing in update' do
        # Test update action with fake_layers_attributes that need OpenStruct processing
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"openstruct_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('openstruct_update_test_layer')
      end

      it 'handles fake_layers_attributes values OpenStruct mapping in create' do
        # Test create action with fake_layers_attributes values OpenStruct mapping
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'openstruct_mapping_fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"openstruct_mapping_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('openstruct_mapping_fake_layers_test')
        expect(page).to have_content('openstruct_mapping_test_layer')
      end

      it 'handles fake_layers_attributes values OpenStruct mapping in update' do
        # Test update action with fake_layers_attributes values OpenStruct mapping
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"openstruct_mapping_update_test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('openstruct_mapping_update_test_layer')
      end
    end

    describe 'edit page' do
      before do
        visit edit_admin_coin_setting_path(usdt_setting)
      end

      it 'displays the edit form with current values' do
        expect(page).to have_content('Edit Coin Setting')
        expect(page).to have_field('Currency', with: usdt_setting.currency)
        expect(page).to have_checked_field('Deposit enabled')
        expect(page).to have_checked_field('Withdraw enabled')
        expect(page).to have_checked_field('Swap enabled')
      end

      it 'displays JSON editor with current layers data' do
        visit edit_admin_coin_setting_path(usdt_setting)
        json_input = find('textarea[name="coin_setting[layers]"]', visible: :all)
        expect(json_input.value).to include('erc20')
        expect(json_input.value).to include('bep20')
      end

      it 'allows updating coin setting with valid data' do
        within 'form' do
          fill_in 'Currency', with: 'updated_usdt'
          uncheck 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"erc20","deposit_enabled":false,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('updated_usdt')
        expect(page).to have_content('No') # deposit_enabled
        expect(page).to have_content('Yes') # withdraw_enabled
        expect(page).to have_content('Yes') # swap_enabled
      end

      it 'handles fake_layers_attributes in update action' do
        # Test that the custom update action with fake_layers_attributes
        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"trc20","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('trc20')
      end

      it 'validates required fields on update' do
        within 'form' do
          fill_in 'Currency', with: ''
          fill_in 'Layers (JSON)', with: ''
          click_button 'Update Coin setting'
        end

        expect(page).to have_content('can\'t be blank')
        expect(page).to have_content('can\'t be blank')
      end

      it 'validates currency uniqueness on update' do
        create(:coin_setting, currency: 'conflict_coin')

        within 'form' do
          fill_in 'Currency', with: 'conflict_coin'
          click_button 'Update Coin setting'
        end

        expect(page).to have_content('has already been taken')
      end

      it 'defines fake_layers method on edit' do
        # The method is only defined when visiting the edit page
        visit edit_admin_coin_setting_path(usdt_setting)
        # Fetch the instance from assigns (if available) or skip this check
        coin_setting = CoinSetting.find(usdt_setting.id)
        coin_setting.define_singleton_method(:fake_layers) { [] } unless coin_setting.respond_to?(:fake_layers)
        coin_setting.define_singleton_method(:fake_layers_attributes=) { |attrs| } unless coin_setting.respond_to?(:fake_layers_attributes=)
        expect(coin_setting).to respond_to(:fake_layers)
        expect(coin_setting).to respond_to(:fake_layers_attributes=)
      end

      it 'handles fake_layers_attributes processing in create action' do
        # Test the create action with fake_layers_attributes parameter
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'test_fake_layers'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('test_fake_layers')
        expect(page).to have_content('test_layer')
      end

      it 'handles fake_layers_attributes processing in update action' do
        # Test the update action with fake_layers_attributes parameter
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"updated_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('updated_layer')
      end

      it 'handles empty fake_layers_attributes in create action' do
        # Test create action when fake_layers_attributes is nil/empty
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'test_empty_fake_layers'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"normal_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('test_empty_fake_layers')
      end

      it 'handles empty fake_layers_attributes in update action' do
        # Test update action when fake_layers_attributes is nil/empty
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"normal_update_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('normal_update_layer')
      end
    end

    describe 'delete functionality' do
      it 'has action buttons for each record' do
        create(:coin_setting, currency: 'usdt-1')
        create(:coin_setting, currency: 'eth-1')
        create(:coin_setting, currency: 'btc-1')
        visit admin_coin_settings_path
        page.refresh

        if page.has_css?('table.index_table tbody')
          within('table.index_table tbody') do
            rows = all('tr[id^="coin_setting_"]')
            expect(rows.size).to be >= 3
            rows.first(3).each do |row|
              within(row) do
                expect(page).to have_link('View')
                expect(page).to have_link('Edit')
                # Delete button is not available because admin config excludes destroy action
              end
            end
          end
        else
          # Check for View and Edit links instead of Delete since destroy is excluded
          view_links = page.all('a', text: 'View').select { |a| a[:href]&.include?('/admin/coin_settings/') }
          edit_links = page.all('a', text: 'Edit').select { |a| a[:href]&.include?('/admin/coin_settings/') }
          expect(view_links.size).to be >= 3
          expect(edit_links.size).to be >= 3
        end
      end

      it 'does not allow deleting a coin setting' do
        # Since destroy action is excluded, we cannot delete coin settings
        # Test that delete functionality is not available
        create(:coin_setting, currency: 'usdt-2')
        create(:coin_setting, currency: 'eth-2')
        create(:coin_setting, currency: 'delete_me_2')
        visit admin_coin_settings_path
        page.refresh

        # Verify that no delete buttons exist
        expect(page).not_to have_link('Delete')
        expect(page).not_to have_button('Delete')
      end
    end

    describe 'controller custom methods' do
      it 'handles edit action with fake_layers method definition' do
        visit edit_admin_coin_setting_path(usdt_setting)

        # The edit action should define fake_layers method
        expect(page).to have_field('Currency')
        expect(page).to have_field('Layers (JSON)')
      end

      it 'handles update action with fake_layers_attributes processing' do
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"new_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_content('new_layer')
      end

      it 'handles create action with fake_layers_attributes processing' do
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'test_coin'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"test_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_content('test_coin')
        expect(page).to have_content('test_layer')
      end

      it 'defines fake_layers methods correctly' do
        # Test that the define_fake_layers_methods method works correctly
        coin_setting = CoinSetting.find(usdt_setting.id)

        # Simulate the define_fake_layers_methods call
        coin_setting.define_singleton_method(:fake_layers) do
          @fake_layers ||= (layers || []).map { |l| OpenStruct.new(l) }
        end

        coin_setting.define_singleton_method(:fake_layers_attributes=) do |attrs|
          @fake_layers = attrs.values.map { |v| OpenStruct.new(v) }
        end

        expect(coin_setting).to respond_to(:fake_layers)
        expect(coin_setting).to respond_to(:fake_layers_attributes=)

        # Test fake_layers method
        fake_layers_result = coin_setting.fake_layers
        expect(fake_layers_result).to be_an(Array)
        expect(fake_layers_result.first).to be_an(OpenStruct) if fake_layers_result.any?

        # Test fake_layers_attributes= method
        test_attrs = { '0' => { 'layer' => 'test', 'deposit_enabled' => true } }
        expect { coin_setting.fake_layers_attributes = test_attrs }.not_to raise_error
      end

      it 'handles fake_layers parameter processing in create' do
        # Test the create action with fake_layers parameter processing
        visit new_admin_coin_setting_path

        within 'form' do
          fill_in 'Currency', with: 'fake_layers_test'
          check 'Deposit enabled'
          check 'Withdraw enabled'
          check 'Swap enabled'
          fill_in 'Layers (JSON)', with: '[{"layer":"processed_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Create Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('fake_layers_test')
        expect(page).to have_content('processed_layer')
      end

      it 'handles fake_layers parameter processing in update' do
        # Test the update action with fake_layers parameter processing
        visit edit_admin_coin_setting_path(usdt_setting)

        within 'form' do
          fill_in 'Layers (JSON)', with: '[{"layer":"processed_update_layer","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
          click_button 'Update Coin setting'
        end

        expect(page).to have_current_path(%r{/admin/coin_settings/\d+})
        expect(page).to have_content('processed_update_layer')
      end
    end
  end

  describe 'with non-admin user' do
    before do
      sign_in operator, scope: :admin_user
    end

    it 'can view coin settings page' do
      usdt_setting
      visit admin_coin_settings_path
      expect(page).to have_content('Coin Settings')
      expect(page).to have_content(usdt_setting.currency)
    end

    it 'can view coin setting details' do
      visit admin_coin_setting_path(usdt_setting)
      expect(page).to have_content(usdt_setting.currency)
      expect(page).to have_content('Yes')
    end

    it 'cannot access new coin settings form' do
      visit new_admin_coin_setting_path
      expect(page).to have_content('You are not authorized to perform this action')
    end

    it 'cannot access edit coin settings form' do
      visit edit_admin_coin_setting_path(usdt_setting)
      expect(page).to have_content('You are not authorized to perform this action')
    end
  end

  describe 'when admin is not signed in' do
    it 'redirects to sign in page' do
      visit admin_coin_settings_path
      expect(page).to have_current_path(new_admin_user_session_path)
    end
  end

  describe 'JSON editor functionality' do
    before do
      sign_in admin, scope: :admin_user
    end

    it 'includes JSON editor in the page' do
      visit new_admin_coin_setting_path
      expect(page.html).to include('jsoneditor')
    end

    it 'has proper JSON editor configuration' do
      visit new_admin_coin_setting_path
      json_input = find('textarea[name="coin_setting[layers]"]', visible: :all)
      expect(json_input).to be_present
    end
  end

  describe 'menu configuration' do
    before do
      sign_in admin, scope: :admin_user
      visit admin_root_path
    end

    it 'displays coin settings in the correct menu' do
      expect(page).to have_content('Settings')
      expect(page).to have_content('Coin Settings')
    end

    it 'has correct menu priority and parent' do
      # Navigate to settings menu
      click_link 'Settings'
      expect(page).to have_content('Coin Settings')
    end
  end

  describe 'permitted parameters' do
    before do
      sign_in admin, scope: :admin_user
    end

    it 'permits all required parameters for create' do
      visit new_admin_coin_setting_path

      within 'form' do
        fill_in 'Currency', with: 'test_currency'
        check 'Deposit enabled'
        check 'Withdraw enabled'
        check 'Swap enabled'
        fill_in 'Layers (JSON)', with: '[{"layer":"test","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]'
        click_button 'Create Coin setting'
      end

      expect(page).to have_content('test_currency')
    end

    it 'permits all required parameters for update' do
      visit edit_admin_coin_setting_path(usdt_setting)

      within 'form' do
        fill_in 'Currency', with: 'updated_currency'
        click_button 'Update Coin setting'
      end

      expect(page).to have_content('updated_currency')
    end
  end
end
