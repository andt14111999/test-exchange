# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::BalanceLockOperations', type: :feature do
  context 'when admin is signed in' do
    before do
      admin = create(:admin_user, :superadmin)
      login_as admin, scope: :admin_user
    end

    it 'displays index page with balance lock operation information' do
      user = create(:user)
      balance_lock = create(:balance_lock,
        user: user,
        locked_balances: { 'btc' => '0.5', 'usdt' => '100.0' },
        status: 'locked'
      )

      _operation = create(:balance_lock_operation,
        balance_lock: balance_lock,
        operation_type: 'lock',
        status: 'completed'
      )

      visit admin_balance_lock_operations_path

      expect(page).to have_link("Lock ##{balance_lock.id}")
      expect(page).to have_link(user.email)
      expect(page).to have_content('btc: 0.5')
      expect(page).to have_content('usdt: 100.0')
      expect(page).to have_content('lock')
      expect(page).to have_content('completed')
    end

    it 'filters operations by status' do
      user = create(:user)
      balance_lock = create(:balance_lock, user: user)

      # Create with unique status values to ensure filter works
      completed_operation = create(:balance_lock_operation,
        balance_lock: balance_lock,
        operation_type: 'lock',
        status: 'completed'
      )

      failed_operation = create(:balance_lock_operation,
        balance_lock: balance_lock,
        operation_type: 'release',
        status: 'failed'
      )

      visit admin_balance_lock_operations_path

      # Verify both operations are visible before filtering
      expect(page).to have_content(completed_operation.id)
      expect(page).to have_content(failed_operation.id)

      # Apply filter for completed status
      within '.filter_form' do
        find('select[name="q[status_eq]"]').find(:option, 'completed').select_option
        click_button 'Filter'
      end

      # Wait for the page to update and verify filter is applied
      expect(page).to have_content('Current filters:')
      expect(page).to have_content(/status equals completed/i)

      # Check we can find the completed operation in the results
      expect(page).to have_content("Displaying")
      expect(page).to have_link(completed_operation.id.to_s)

      # Check the failed operation ID is not in the results
      # We need to check for the ID in the table since "failed" may appear in filter options
      page_ids = page.all('tbody tr').map { |row| row.find('td.col-id').text.strip rescue nil }.compact
      expect(page_ids).to include(completed_operation.id.to_s)
      expect(page_ids).not_to include(failed_operation.id.to_s)
    end

    it 'filters operations by operation type' do
      user = create(:user)
      balance_lock = create(:balance_lock, user: user)

      lock_operation = create(:balance_lock_operation,
        balance_lock: balance_lock,
        operation_type: 'lock',
        status: 'completed'
      )

      release_operation = create(:balance_lock_operation,
        balance_lock: balance_lock,
        operation_type: 'release',
        status: 'completed'
      )

      visit admin_balance_lock_operations_path

      # Verify both operations are visible before filtering
      expect(page).to have_content(lock_operation.id)
      expect(page).to have_content(release_operation.id)

      # Apply filter
      within '.filter_form' do
        find('select[name="q[operation_type_eq]"]').find(:option, 'lock').select_option
        click_button 'Filter'
      end

      # Wait for the page to update and verify filter is applied
      expect(page).to have_content('Current filters:')
      expect(page).to have_content(/operation type equals lock/i)

      # Check we can find the lock operation in the results
      expect(page).to have_content("Displaying")
      expect(page).to have_link(lock_operation.id.to_s)

      # Check the release operation ID is not in the results
      page_ids = page.all('tbody tr').map { |row| row.find('td.col-id').text.strip rescue nil }.compact
      expect(page_ids).to include(lock_operation.id.to_s)
      expect(page_ids).not_to include(release_operation.id.to_s)
    end

    it 'displays operation details on show page' do
      user = create(:user)
      balance_lock = create(:balance_lock,
        user: user,
        locked_balances: { 'btc' => '0.5', 'usdt' => '100.0' },
        status: 'locked'
      )

      operation = create(:balance_lock_operation,
        balance_lock: balance_lock,
        operation_type: 'lock',
        status: 'completed',
        status_explanation: 'Successfully locked funds'
      )

      # Create related transaction
      coin_account = create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin')
      transaction = create(:coin_transaction,
        operation: operation,
        coin_account: coin_account,
        coin_currency: 'btc',
        amount: -0.5,
        transaction_type: 'lock'
      )

      visit admin_balance_lock_operation_path(operation)

      # Main attributes
      expect(page).to have_content(operation.id)
      expect(page).to have_link("Lock ##{balance_lock.id}")
      expect(page).to have_link(user.email)
      expect(page).to have_content('btc')
      expect(page).to have_content('0.5')
      expect(page).to have_content('usdt')
      expect(page).to have_content('100.0')
      expect(page).to have_content(/lock/i)
      expect(page).to have_content(/completed/i)
      expect(page).to have_content('Successfully locked funds')

      # Related transactions
      expect(page).to have_content('Related Coin Transactions')
      transaction_table = find('div.panel', text: 'Related Coin Transactions').find('table')
      within transaction_table do
        expect(page).to have_link(transaction.id.to_s)
        expect(page).to have_content('btc')
        expect(page).to have_content('-0.5')
        expect(page).to have_content(/lock/i)
      end
    end
  end
end
