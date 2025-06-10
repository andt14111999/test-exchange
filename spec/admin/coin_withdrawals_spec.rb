# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::CoinWithdrawals', type: :system do
  describe 'index page' do
    it 'displays list of coin withdrawals' do
      admin_user = create(:admin_user, :superadmin)
      user = create(:user, email: 'user1@example.com')
      create(:coin_account, :btc_main, user:, balance: 100.0)
      withdrawal = create(:coin_withdrawal,
        user:,
        coin_currency: 'btc',
        coin_amount: 1.0,
        coin_fee: 0.0,
        coin_address: '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
        coin_layer: 'btc',
        status: 'pending')

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_withdrawals_path

      expect(page).to have_content(withdrawal.id)
      expect(page).to have_content(user.display_name)
      expect(page).to have_content(withdrawal.coin_account.id)
      expect(page).to have_content('Btc')
      expect(page).to have_content('1.0')
      expect(page).to have_content('0.0')
      expect(page).to have_content('1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa')
      expect(page).to have_content('Pending')
    end

    it 'filters withdrawals by coin_currency and status' do
      admin_user = create(:admin_user, :superadmin)
      user = create(:user)
      create(:coin_account, :btc_main, user:, balance: 100.0)
      withdrawal = create(:coin_withdrawal,
        user:,
        coin_currency: 'btc',
        status: 'pending')

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_withdrawals_path

      fill_in 'Coin currency', with: 'btc'
      select 'pending', from: 'Status'
      click_button 'Filter'

      expect(current_path).to eq(admin_coin_withdrawals_path)
      expect(page).to have_content(withdrawal.id)
    end
  end

  describe 'show page' do
    it 'displays withdrawal details' do
      admin_user = create(:admin_user, :superadmin)
      user = create(:user)
      create(:coin_account, :btc_main, user:, balance: 100.0)
      withdrawal = create(:coin_withdrawal,
        user:,
        coin_currency: 'btc',
        coin_amount: 1.0,
        coin_fee: 0.0,
        coin_address: '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
        coin_layer: 'btc',
        status: 'pending')

      operation = create(:coin_withdrawal_operation,
        coin_withdrawal: withdrawal,
        status: 'pending',
        withdrawal_status: 'pending',
        tx_hash: nil,
        scheduled_at: Time.zone.now,
        withdrawal_data: { amount: 1.0 })

      _transaction = create(:coin_transaction,
        operation:,
        amount: -1.0,
        coin_currency: 'btc',
        transaction_type: 'transfer')

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_withdrawal_path(withdrawal)

      # Withdrawal details
      expect(page).to have_content(withdrawal.id)
      expect(page).to have_content(user.display_name)
      expect(page).to have_content(withdrawal.coin_account.id)
      expect(page).to have_content('Btc')
      expect(page).to have_content('1.0')
      expect(page).to have_content('0.0')
      expect(page).to have_content('1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa')
      expect(page).to have_content('Pending')

      # Operation details
      within('div#withdrawal-operation') do
        expect(page).to have_content('Status')
        expect(page).to have_content('Pending')
        expect(page).to have_content('Empty')
      end

      # Transaction details
      within('div#transaction') do
        expect(page).to have_content('Amount')
        expect(page).to have_content('-1.0')
        expect(page).to have_content('Btc')
        expect(page).to have_content('Transfer')
      end

      # Cancel button for pending withdrawal
      expect(page).to have_button('Cancel Withdrawal')
    end

    it 'displays "No withdrawal operation found" when there is no operation' do
      admin_user = create(:admin_user, :superadmin)
      user = create(:user)
      create(:coin_account, :btc_main, user:, balance: 100.0)

      withdrawal = nil
      CoinWithdrawal.skip_callback(:create, :after, :create_operations)
      withdrawal = create(:coin_withdrawal,
        user:,
        coin_currency: 'btc',
        coin_amount: 1.0,
        coin_fee: 0.0,
        coin_address: '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
        coin_layer: 'btc',
        status: 'pending')
      CoinWithdrawal.set_callback(:create, :after, :create_operations)

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_withdrawal_path(withdrawal)

      within('div#withdrawal-operation') do
        expect(page).to have_content('No withdrawal operation found')
      end
    end
  end

  describe 'cancel action' do
    it 'successfully cancels a pending withdrawal' do
      admin_user = create(:admin_user, :superadmin)
      user = create(:user)
      create(:coin_account, :btc_main, user:, balance: 100.0)
      withdrawal = create(:coin_withdrawal,
        user:,
        coin_currency: 'btc',
        coin_amount: 1.0,
        coin_fee: 0.0,
        coin_address: '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
        coin_layer: 'btc',
        status: 'pending')

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_withdrawal_path(withdrawal)

      page.driver.submit :put, cancel_admin_coin_withdrawal_path(withdrawal), {}

      expect(page).to have_content('Withdrawal was successfully cancelled')
      withdrawal.reload
      expect(withdrawal.status).to eq('cancelled')
    end

    it 'shows error when cancellation fails' do
      admin_user = create(:admin_user, :superadmin)
      user = create(:user)
      create(:coin_account, :btc_main, user:, balance: 100.0)
      withdrawal = create(:coin_withdrawal,
        user:,
        coin_currency: 'btc',
        coin_amount: 1.0,
        coin_fee: 0.0,
        coin_address: '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
        coin_layer: 'btc',
        status: 'completed')

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_withdrawal_path(withdrawal)

      expect(page).not_to have_button('Cancel Withdrawal')
      expect(withdrawal.reload.status).to eq('completed')
    end

    it 'shows error when cancel! raises exception' do
      admin_user = create(:admin_user, :superadmin)
      user = create(:user)
      create(:coin_account, :btc_main, user:, balance: 100.0)
      withdrawal = create(:coin_withdrawal,
        user:,
        coin_currency: 'btc',
        coin_amount: 1.0,
        coin_fee: 0.0,
        coin_address: '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
        coin_layer: 'btc',
        status: 'pending')

      allow(withdrawal).to receive(:cancel!).and_raise(StandardError.new('Some error'))
      allow(CoinWithdrawal).to receive(:find).with(withdrawal.id.to_s).and_return(withdrawal)

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_withdrawal_path(withdrawal)

      page.driver.submit :put, cancel_admin_coin_withdrawal_path(withdrawal), {}

      expect(page).to have_content('Could not cancel withdrawal')
      expect(withdrawal.reload.status).to eq('pending')
    end
  end
end
