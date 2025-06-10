# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::MerchantEscrows', type: :feature do
  context 'when admin is signed in' do
    before do
      admin = create(:admin_user, :superadmin)
      login_as admin, scope: :admin_user
    end

    it 'displays index page with escrow information' do
      user = create(:user, :merchant, email: 'merchant@example.com', display_name: 'Merchant User')
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account = create(:fiat_account, :vnd, user: user)
      escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'pending'
      )

      visit admin_merchant_escrows_path

      expect(page).to have_content('Merchant User')
      expect(page).to have_content('100.00000000')
      expect(page).to have_content('1000.00')
      expect(page).to have_content('VND')
      expect(page).to have_content('Pending')
    end

    it 'filters escrows by fiat currency' do
      user = create(:user, :merchant)
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account_vnd = create(:fiat_account, :vnd, user: user)
      fiat_account_php = create(:fiat_account, :php, user: user)

      escrow_vnd = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account_vnd,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'pending'
      )
      escrow_php = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account_php,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'PHP',
        status: 'pending'
      )

      visit admin_merchant_escrows_path

      # Verify both escrows are visible before filtering
      expect(page).to have_content(escrow_vnd.id)
      expect(page).to have_content(escrow_php.id)

      # Apply filter
      within '.filter_form' do
        find('select[name="q[fiat_currency_eq]"]').find(:option, 'VND').select_option
        click_button 'Filter'
      end

      # Wait for the page to update and verify filter is applied
      expect(page).to have_content('Current filters:')
      expect(page).to have_content('Fiat currency equals VND')

      # Wait for the filter to be applied and page to reload
      expect(page).to have_content('Displaying 1 Merchant Escrow')

      # Verify only VND escrow is visible in the table
      within 'table' do
        expect(page).to have_selector('tr', text: escrow_vnd.id.to_s)
        expect(page).to have_selector('tr', text: '100.00000000')
        expect(page).to have_selector('tr', text: '1000.00')
        expect(page).to have_selector('tr', text: 'VND')
        expect(page).not_to have_selector("tr[data-id='#{escrow_php.id}']")
        expect(page).not_to have_selector('tr', text: 'PHP')
      end
    end

    it 'filters escrows by status' do
      user = create(:user, :merchant)
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account = create(:fiat_account, :vnd, user: user)

      pending_escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'pending'
      )
      active_escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'active'
      )

      visit admin_merchant_escrows_path

      # Verify both escrows are visible before filtering
      expect(page).to have_content(pending_escrow.id)
      expect(page).to have_content(active_escrow.id)

      # Apply filter
      within '.filter_form' do
        find('select[name="q[status_eq]"]').find(:option, 'pending').select_option
        click_button 'Filter'
      end

      # Wait for the page to update and verify filter is applied
      expect(page).to have_content('Current filters:')
      expect(page).to have_content('Status equals pending')

      # Wait for the filter to be applied and page to reload
      expect(page).to have_content('Displaying 1 Merchant Escrow')

      # Verify only pending escrow is visible in the table
      within 'table' do
        expect(page).to have_selector('tr', text: pending_escrow.id.to_s)
        expect(page).to have_selector('tr', text: 'Pending')
        expect(page).not_to have_selector("tr[data-id='#{active_escrow.id}']")
        expect(page).not_to have_selector('tr', text: 'Active')
      end
    end

    it 'displays escrow details on show page' do
      user = create(:user, :merchant, email: 'merchant@example.com', display_name: 'Merchant User')
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account = create(:fiat_account, :vnd, user: user)
      escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'pending'
      )

      visit admin_merchant_escrow_path(escrow)

      # Main attributes
      expect(page).to have_content(escrow.id)
      expect(page).to have_content('Merchant User')
      expect(page).to have_content(usdt_account.id)
      expect(page).to have_content(fiat_account.id)
      expect(page).to have_content('100.00000000')
      expect(page).to have_content('1000.00')
      expect(page).to have_content('VND')
      expect(page).to have_content('Pending')
    end

    it 'displays escrow operations panel' do
      user = create(:user, :merchant)
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account = create(:fiat_account, :vnd, user: user)
      escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'pending'
      )
      operation = create(:merchant_escrow_operation,
        merchant_escrow: escrow,
        operation_type: 'mint',
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: FiatAccount::SUPPORTED_CURRENCIES.keys.first,
        status: 'completed'
      )

      visit admin_merchant_escrow_path(escrow)

      # Check Escrow Operations panel
      within find('.panel', text: 'Escrow Operations') do
        expect(page).to have_content('Escrow Operations')
        expect(page).to have_content(operation.id)
        expect(page).to have_content('mint')
        expect(page).to have_content('100.00000000')
        expect(page).to have_content('1000.00')
        expect(page).to have_content('Completed')
        expect(page).to have_link('View', href: admin_merchant_escrow_operation_path(operation))
      end
    end

    it 'displays state actions sidebar for pending escrow' do
      user = create(:user, :merchant)
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account = create(:fiat_account, :vnd, user: user)
      escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'pending'
      )

      visit admin_merchant_escrow_path(escrow)

      # Check State Actions sidebar
      within '#sidebar' do
        expect(page).to have_content('State Actions')
        expect(page).to have_button('Cancel Escrow')
      end
    end

    it 'does not display state actions sidebar for active escrow' do
      user = create(:user, :merchant)
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account = create(:fiat_account, :vnd, user: user)
      escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'active'
      )

      visit admin_merchant_escrow_path(escrow)

      # Check State Actions sidebar
      within '#sidebar' do
        expect(page).to have_content('State Actions')
        expect(page).not_to have_button('Cancel Escrow')
      end
    end

    it 'cancels pending escrow' do
      user = create(:user, :merchant)
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account = create(:fiat_account, :vnd, user: user)
      escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'pending'
      )

      visit admin_merchant_escrow_path(escrow)

      # Click Cancel Escrow button
      within '#sidebar' do
        page.driver.submit :put, cancel_admin_merchant_escrow_path(escrow), {}
      end

      # Wait for the page to reload
      sleep 1

      # Check redirect and flash message
      expect(page).to have_current_path(admin_merchant_escrow_path(escrow))
      expect(page).to have_content('Escrow was successfully cancelled')

      # Check escrow status is updated
      escrow.reload
      expect(escrow.status).to eq('cancelled')
    end

    it 'shows error when escrow cannot be cancelled' do
      user = create(:user, :merchant)
      usdt_account = create(:coin_account, :usdt_trc20, user: user)
      fiat_account = create(:fiat_account, :vnd, user: user)
      escrow = create(:merchant_escrow,
        user: user,
        usdt_account: usdt_account,
        fiat_account: fiat_account,
        usdt_amount: 100.0,
        fiat_amount: 1000.0,
        fiat_currency: 'VND',
        status: 'pending'
      )

      # Stub the cancel! method to return false
      allow_any_instance_of(MerchantEscrow).to receive(:cancel!).and_return(false)

      visit admin_merchant_escrow_path(escrow)

      # Click Cancel Escrow button
      within '#sidebar' do
        page.driver.submit :put, cancel_admin_merchant_escrow_path(escrow), {}
      end

      # Wait for the page to reload
      sleep 1

      # Check redirect and flash message
      expect(page).to have_current_path(admin_merchant_escrow_path(escrow))
      expect(page).to have_content('Could not cancel escrow')

      # Check escrow status is not updated
      escrow.reload
      expect(escrow.status).to eq('pending')
    end
  end
end
