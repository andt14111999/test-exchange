# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::CoinTransactions', type: :feature do
  context 'when admin is signed in' do
    before do
      admin = create(:admin_user, :admin)
      login_as admin, scope: :admin_user
    end

    it 'displays index page with transaction information' do
      user = create(:user)
      coin_account = create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin')
      transaction = create(:coin_transaction,
        coin_account: coin_account,
        coin_currency: 'btc',
        amount: 0.001,
        transaction_type: 'transfer',
        snapshot_balance: 0.01,
        snapshot_frozen_balance: 0.005
      )

      visit admin_coin_transactions_path

      expect(page).to have_link("Coin account ##{coin_account.id}")
      expect(page).to have_link(user.email)
      expect(page).to have_content('btc')
      expect(page).to have_content('transfer')
      expect(page).to have_content('0.00100000')
      expect(page).to have_content('0.01000000')
      expect(page).to have_content('0.00500000')
    end

    it 'filters transactions by coin currency' do
      user = create(:user)
      coin_account_btc = create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin')
      coin_account_eth = create(:coin_account, user: user, coin_currency: 'eth', layer: 'erc20')

      transaction_btc = create(:coin_transaction,
        coin_account: coin_account_btc,
        coin_currency: 'btc',
        amount: 0.001,
        transaction_type: 'transfer'
      )
      transaction_eth = create(:coin_transaction,
        coin_account: coin_account_eth,
        coin_currency: 'eth',
        amount: 0.1,
        transaction_type: 'transfer'
      )

      visit admin_coin_transactions_path

      # Verify both transactions are visible before filtering
      expect(page).to have_content(transaction_btc.id)
      expect(page).to have_content(transaction_eth.id)

      # Apply filter
      within '.filter_form' do
        find('select[name="q[coin_currency_eq]"]').find(:option, 'btc').select_option
        click_button 'Filter'
      end

      # Wait for the page to update and verify filter is applied
      expect(page).to have_content('Current filters:')
      expect(page).to have_content('Coin currency equals btc')

      # Wait for the filter to be applied and page to reload
      expect(page).to have_content('Displaying 1 Coin Transaction')

      # Verify only BTC transaction is visible in the table
      within 'table' do
        expect(page).to have_selector('tr', text: transaction_btc.id.to_s)
        expect(page).to have_selector('tr', text: '0.00100000')
        expect(page).to have_selector('tr', text: 'btc')
        expect(page).not_to have_selector("tr[data-id='#{transaction_eth.id}']")
        expect(page).not_to have_selector('tr', text: '0.10000000')
      end
    end

    it 'filters transactions by transaction type' do
      user = create(:user)
      coin_account = create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin')

      transfer_tx = create(:coin_transaction,
        coin_account: coin_account,
        coin_currency: 'btc',
        amount: 0.001,
        transaction_type: 'transfer'
      )
      lock_tx = create(:coin_transaction,
        coin_account: coin_account,
        coin_currency: 'btc',
        amount: 0.1,
        transaction_type: 'lock'
      )

      visit admin_coin_transactions_path

      # Verify both transactions are visible before filtering
      expect(page).to have_content(transfer_tx.id)
      expect(page).to have_content(lock_tx.id)

      # Apply filter
      within '.filter_form' do
        find('select[name="q[transaction_type_eq]"]').find(:option, 'transfer').select_option
        click_button 'Filter'
      end

      # Wait for the page to update and verify filter is applied
      expect(page).to have_content('Current filters:')
      expect(page).to have_content('Transaction type equals transfer')

      # Wait for the filter to be applied and page to reload
      expect(page).to have_content('Displaying 1 Coin Transaction')

      # Verify only transfer transaction is visible in the table
      within 'table' do
        expect(page).to have_selector('tr', text: transfer_tx.id.to_s)
        expect(page).to have_selector('tr', text: '0.00100000')
        expect(page).not_to have_selector("tr[data-id='#{lock_tx.id}']")
        expect(page).not_to have_selector('tr', text: '0.10000000')
      end
    end

    it 'displays transaction details on show page' do
      user = create(:user)
      coin_account = create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin')
      transaction = create(:coin_transaction,
        coin_account: coin_account,
        coin_currency: 'btc',
        amount: 0.001,
        transaction_type: 'transfer',
        snapshot_balance: 0.01,
        snapshot_frozen_balance: 0.005
      )

      visit admin_coin_transaction_path(transaction)

      # Main attributes
      expect(page).to have_content(transaction.id)
      expect(page).to have_link("Coin account ##{coin_account.id}")
      expect(page).to have_link(user.email)
      expect(page).to have_content('btc')
      expect(page).to have_content('transfer')
      expect(page).to have_content('0.00100000')
      expect(page).to have_content('0.01000000')
      expect(page).to have_content('0.00500000')

      # Balance changes sidebar
      within '#sidebar' do
        expect(page).to have_content('0.01000000') # Previous Balance
        expect(page).to have_content('Increase')
        expect(page).to have_content('0.00100000') # Change amount
        expect(page).to have_content('0.01100000') # New Balance
      end
    end
  end
end
