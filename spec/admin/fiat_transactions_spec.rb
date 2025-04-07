require 'rails_helper'

RSpec.describe 'Admin::FiatTransactions', type: :feature do
  context 'when admin is signed in' do
    before do
      admin = create(:admin_user, :admin)
      login_as admin, scope: :admin_user
    end

    it 'displays index page with transaction information' do
      user = create(:user)
      fiat_account = create(:fiat_account, user: user)
      transaction = create(:fiat_transaction,
        fiat_account: fiat_account,
        currency: 'VND',
        amount: 100_000,
        transaction_type: 'mint',
        snapshot_balance: 500_000,
        snapshot_frozen_balance: 0
      )

      visit admin_fiat_transactions_path

      expect(page).to have_content(transaction.fiat_account.id)
      expect(page).to have_content('VND')
      expect(page).to have_content('100000.00')
      expect(page).to have_content('Mint')
      expect(page).to have_content('500000.00')
      expect(page).to have_content('0.00')
    end

    it 'filters transactions by currency' do
      user = create(:user)
      fiat_account_vnd = create(:fiat_account, user: user, currency: 'VND')
      fiat_account_php = create(:fiat_account, user: user, currency: 'PHP')

      transaction_vnd = create(:fiat_transaction, fiat_account: fiat_account_vnd, currency: 'VND')
      transaction_php = create(:fiat_transaction, fiat_account: fiat_account_php, currency: 'PHP')

      visit admin_fiat_transactions_path

      within '.filter_form' do
        select 'VND', from: 'q[currency_eq]'
        click_button 'Filter'
      end

      expect(page).to have_content(transaction_vnd.id)
      expect(page).not_to have_content(transaction_php.id)
    end

    it 'filters transactions by transaction type' do
      user = create(:user)
      fiat_account = create(:fiat_account, user: user)

      mint_tx = create(:fiat_transaction, fiat_account: fiat_account, transaction_type: 'mint')
      burn_tx = create(:fiat_transaction, fiat_account: fiat_account, transaction_type: 'burn')

      visit admin_fiat_transactions_path

      within '.filter_form' do
        select 'mint', from: 'q[transaction_type_eq]'
        click_button 'Filter'
      end

      expect(page).to have_content(mint_tx.id)
      expect(page).not_to have_content(burn_tx.id)
    end

    it 'displays transaction details on show page' do
      user = create(:user)
      fiat_account = create(:fiat_account, user: user)
      transaction = create(:fiat_transaction,
        fiat_account: fiat_account,
        currency: 'VND',
        amount: 100_000,
        transaction_type: 'mint',
        snapshot_balance: 500_000,
        snapshot_frozen_balance: 0
      )

      visit admin_fiat_transaction_path(transaction)

      # Main attributes
      expect(page).to have_content(transaction.id)
      expect(page).to have_content(transaction.fiat_account.id)
      expect(page).to have_content('VND')
      expect(page).to have_content('100000.00')
      expect(page).to have_content('Mint')
      expect(page).to have_content('500000.00')
      expect(page).to have_content('0.00')

      # Related information
      expect(page).to have_content(user.email)
      expect(page).to have_link(user.email, href: admin_user_path(user))

      # Balance changes sidebar
      within '#sidebar' do
        expect(page).to have_content('500000.00') # Previous Balance
        expect(page).to have_content('Increase')
        expect(page).to have_content('100000.00') # Change amount
        expect(page).to have_content('600000.00') # New Balance
      end
    end

    it 'shows correct transaction type status tag colors' do
      user = create(:user)
      fiat_account = create(:fiat_account, user: user)

      mint_tx = create(:fiat_transaction,
        fiat_account: fiat_account,
        transaction_type: 'mint'
      )

      burn_tx = create(:fiat_transaction,
        fiat_account: fiat_account,
        transaction_type: 'burn'
      )

      visit admin_fiat_transactions_path

      within "tr#fiat_transaction_#{mint_tx.id}" do
        expect(page).to have_css('.status_tag.green')
      end

      within "tr#fiat_transaction_#{burn_tx.id}" do
        expect(page).to have_css('.status_tag.red')
      end
    end
  end

  context 'when admin is not signed in' do
    it 'redirects to sign in page' do
      visit admin_fiat_transactions_path
      expect(page).to have_current_path(new_admin_user_session_path)
    end
  end
end
