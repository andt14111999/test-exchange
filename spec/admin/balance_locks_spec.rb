# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::BalanceLocks', type: :feature do
  context 'when admin is signed in' do
    before do
      admin = create(:admin_user, :superadmin)
      login_as admin, scope: :admin_user
    end

    it 'displays index page with balance lock information' do
      user = create(:user)
      _balance_lock = create(:balance_lock,
        user: user,
        locked_balances: { 'btc' => '0.5', 'usdt' => '100.0' },
        status: 'locked',
        performer: 'Admin',
        reason: 'Suspicious activity',
        locked_at: 1.day.ago
      )

      visit admin_balance_locks_path

      expect(page).to have_link(user.email)
      expect(page).to have_content('btc: 0.5')
      expect(page).to have_content('usdt: 100.0')
      expect(page).to have_content('locked')
      expect(page).to have_content('Admin')
      expect(page).to have_content('Suspicious activity')
      expect(page).to have_link('Release')
    end

    it 'filters balance locks by status' do
      user = create(:user)
      locked_balance = create(:balance_lock, user: user, status: 'locked')
      released_balance = create(:balance_lock, user: user, status: 'released')

      visit admin_balance_locks_path

      # Verify both balance locks are visible before filtering
      expect(page).to have_content(locked_balance.id)
      expect(page).to have_content(released_balance.id)

      # Apply filter
      within '.filter_form' do
        find('select[name="q[status_eq]"]').find(:option, 'locked').select_option
        click_button 'Filter'
      end

      # Wait for the page to update and verify filter is applied
      expect(page).to have_content('Current filters:')
      expect(page).to have_content('Status equals locked')

      # Verify only locked balance is visible in the table
      expect(page).to have_selector("tr", text: locked_balance.id.to_s)
    end

    it 'displays balance lock details on show page' do
      user = create(:user)
      balance_lock = create(:balance_lock,
        user: user,
        locked_balances: { 'btc' => '0.5', 'usdt' => '100.0' },
        status: 'locked',
        performer: 'Admin',
        reason: 'Suspicious activity',
        locked_at: 1.day.ago
      )

      # Create related operations
      operation = create(:balance_lock_operation,
        balance_lock: balance_lock,
        operation_type: 'lock',
        status: 'completed'
      )

      visit admin_balance_lock_path(balance_lock)

      # Main attributes
      expect(page).to have_content(balance_lock.id)
      expect(page).to have_link(user.email)
      expect(page).to have_content('btc')
      expect(page).to have_content('0.5')
      expect(page).to have_content('usdt')
      expect(page).to have_content('100.0')
      expect(page).to have_content(/locked/i)
      expect(page).to have_content('Admin')
      expect(page).to have_content('Suspicious activity')

      # Related operations
      expect(page).to have_content('Balance Lock Operations')
      expect(page).to have_link(operation.id.to_s)
      expect(page).to have_content(/lock/i)
      expect(page).to have_content(/completed/i)
    end

    it 'releases a locked balance' do
      user = create(:user)
      balance_lock = create(:balance_lock, user: user, status: 'locked')

      visit admin_balance_lock_path(balance_lock)

      # Click the release button
      click_link 'Release'

      # Verify the balance lock was released
      expect(page).to have_content('Balance lock has been released successfully')

      # Reload the balance_lock to check its status
      balance_lock.reload
      expect(balance_lock.status).to eq('releasing')
    end
  end
end
