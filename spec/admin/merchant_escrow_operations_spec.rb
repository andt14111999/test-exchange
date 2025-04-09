# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::MerchantEscrowOperations', type: :system do
  include ActionView::Helpers::NumberHelper

  describe 'index page' do
    it 'displays a list of merchant escrow operations' do
      admin_user = create(:admin_user, :admin)
      merchant_escrow = create(:merchant_escrow)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account)
      operation = create(:merchant_escrow_operation,
                        merchant_escrow: merchant_escrow,
                        usdt_account: usdt_account,
                        fiat_account: fiat_account,
                        usdt_amount: 100.5,
                        fiat_amount: 2000.75,
                        fiat_currency: 'VND',
                        operation_type: 'mint',
                        status: 'failed')

      login_as(admin_user, scope: :admin_user)
      visit admin_merchant_escrow_operations_path

      expect(page).to have_content(operation.id)
      expect(page).to have_content(merchant_escrow.id)
      expect(page).to have_content('mint')
      expect(page).to have_content(number_with_precision(operation.usdt_amount, precision: 8))
      expect(page).to have_content(number_with_precision(operation.fiat_amount, precision: 2))
      expect(page).to have_content('VND')
      expect(page).to have_content('Failed')
      expect(page).to have_content(operation.created_at.strftime('%B %d, %Y %H:%M'))
    end

    it 'has working filters' do
      admin_user = create(:admin_user, :admin)
      merchant_escrow = create(:merchant_escrow)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account)

      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       status: 'failed')

      login_as(admin_user, scope: :admin_user)
      visit admin_merchant_escrow_operations_path

      within '.sidebar_section' do
        # Just filter by status to avoid dealing with the operation_type filter
        select 'failed', from: 'q_status'
        click_button 'Filter'
      end

      expect(page).to have_current_path(/#{admin_merchant_escrow_operations_path}.*status/)
      expect(page).to have_content(operation.id)
      expect(page).to have_content('mint')
      expect(page).to have_content('Failed')
    end
  end

  describe 'show page' do
    it 'displays operation details' do
      admin_user = create(:admin_user, :admin)
      merchant_escrow = create(:merchant_escrow)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account)
      operation = create(:merchant_escrow_operation,
                        merchant_escrow: merchant_escrow,
                        usdt_account: usdt_account,
                        fiat_account: fiat_account,
                        usdt_amount: 100.5,
                        fiat_amount: 2000.75,
                        fiat_currency: 'VND',
                        operation_type: 'mint',
                        status: 'failed',
                        status_explanation: 'Insufficient balance')

      login_as(admin_user, scope: :admin_user)
      visit admin_merchant_escrow_operation_path(operation)

      expect(page).to have_content(operation.id)
      expect(page).to have_content(merchant_escrow.id)
      expect(page).to have_content(usdt_account.id)
      expect(page).to have_content(fiat_account.id)
      expect(page).to have_content('mint')
      expect(page).to have_content(number_with_precision(operation.usdt_amount, precision: 8))
      expect(page).to have_content(number_with_precision(operation.fiat_amount, precision: 2))
      expect(page).to have_content('VND')
      expect(page).to have_content('Failed')
      expect(page).to have_content('Insufficient balance')
      expect(page).to have_content(operation.created_at.strftime('%B %d, %Y %H:%M'))
      expect(page).to have_content(operation.updated_at.strftime('%B %d, %Y %H:%M'))
    end

    it 'displays coin transactions' do
      admin_user = create(:admin_user, :admin)
      operation = create(:merchant_escrow_operation)
      coin_transaction = create(:coin_transaction,
                              coin_account: operation.usdt_account,
                              amount: 100.0,
                              snapshot_balance: 100.0,
                              snapshot_frozen_balance: 100.0,
                              operation_type: 'merchant_escrow',
                              operation_id: operation.id)

      operation.coin_transactions << coin_transaction

      login_as(admin_user, scope: :admin_user)
      visit admin_merchant_escrow_operation_path(operation)

      within 'div.panel:has(h3:contains("Coin Transactions"))' do
        expect(page).to have_content(coin_transaction.id)
        expect(page).to have_content(number_with_precision(coin_transaction.amount, precision: 8))
        expect(page).to have_content(number_with_precision(coin_transaction.snapshot_balance, precision: 8))
        expect(page).to have_content(number_with_precision(coin_transaction.snapshot_frozen_balance, precision: 8))
        expect(page).to have_content(coin_transaction.created_at.strftime('%B %d, %Y %H:%M'))
      end
    end

    it 'displays fiat transactions' do
      admin_user = create(:admin_user, :admin)
      operation = create(:merchant_escrow_operation)
      fiat_transaction = create(:fiat_transaction,
                              fiat_account: operation.fiat_account,
                              amount: 2000.75,
                              snapshot_balance: 10000.0,
                              snapshot_frozen_balance: 5000.0,
                              operation_type: 'merchant_escrow',
                              operation_id: operation.id)

      operation.fiat_transactions << fiat_transaction

      login_as(admin_user, scope: :admin_user)
      visit admin_merchant_escrow_operation_path(operation)

      within 'div.panel:has(h3:contains("Fiat Transactions"))' do
        expect(page).to have_content(fiat_transaction.id)
        expect(page).to have_content(number_with_precision(fiat_transaction.amount, precision: 2))
        expect(page).to have_content(number_with_precision(fiat_transaction.snapshot_balance, precision: 2))
        expect(page).to have_content(number_with_precision(fiat_transaction.snapshot_frozen_balance, precision: 2))
        expect(page).to have_content(fiat_transaction.created_at.strftime('%B %d, %Y %H:%M'))
      end
    end
  end
end
