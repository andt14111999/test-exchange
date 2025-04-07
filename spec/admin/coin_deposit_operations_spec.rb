# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::CoinDepositOperations', type: :system do
  include ActionView::Helpers::NumberHelper
  include Admin::BlockchainHelper

  let(:admin_user) { create(:admin_user, :admin) }

  before do
    driven_by(:rack_test)
    login_as(admin_user, scope: :admin_user)
  end

  describe 'index page' do
    let!(:coin_deposit_operation) { create(:coin_deposit_operation) }

    before do
      visit admin_coin_deposit_operations_path
    end

    it 'displays a list of coin deposit operations' do
      expect(page).to have_content(coin_deposit_operation.id)
      expect(page).to have_content(coin_deposit_operation.coin_account.id)
      expect(page).to have_content(coin_deposit_operation.coin_currency)
    end

    it 'displays formatted coin amount' do
      expect(page).to have_content(number_with_precision(coin_deposit_operation.coin_amount, precision: 8))
    end

    it 'displays formatted coin fee' do
      expect(page).to have_content(number_with_precision(coin_deposit_operation.coin_fee, precision: 8))
    end

    it 'displays formatted amount after fee' do
      expect(page).to have_content(number_with_precision(coin_deposit_operation.coin_amount - coin_deposit_operation.coin_fee - coin_deposit_operation.platform_fee, precision: 8))
    end

    it 'displays transaction hash with link' do
      if coin_deposit_operation.tx_hash.present?
        expect(page).to have_link(
          coin_deposit_operation.tx_hash.truncate(20),
          href: "#{blockchain_explorer_url(coin_deposit_operation.coin_currency)}/tx/#{coin_deposit_operation.tx_hash}"
        )
      end
    end

    it 'displays status tag' do
      expect(page).to have_selector('.status_tag', text: coin_deposit_operation.status)
    end
  end

  describe 'show page' do
    let(:coin_deposit_operation) { create(:coin_deposit_operation) }
    let!(:coin_transaction) { create(:coin_transaction, operation: coin_deposit_operation) }

    before do
      visit admin_coin_deposit_operation_path(coin_deposit_operation)
    end

    it 'displays operation details' do
      expect(page).to have_content(coin_deposit_operation.id)
      expect(page).to have_content(coin_deposit_operation.coin_account.id)
      expect(page).to have_content(coin_deposit_operation.coin_deposit.id)
      expect(page).to have_content(coin_deposit_operation.coin_currency)
    end

    it 'displays formatted amounts' do
      expect(page).to have_content(number_with_precision(coin_deposit_operation.coin_amount, precision: 8))
      expect(page).to have_content(number_with_precision(coin_deposit_operation.coin_fee, precision: 8))
      expect(page).to have_content(number_with_precision(coin_deposit_operation.coin_amount - coin_deposit_operation.coin_fee - coin_deposit_operation.platform_fee, precision: 8))
    end

    it 'displays transaction hash with link' do
      if coin_deposit_operation.tx_hash.present?
        expect(page).to have_link(
          coin_deposit_operation.tx_hash,
          href: "#{blockchain_explorer_url(coin_deposit_operation.coin_currency)}/tx/#{coin_deposit_operation.tx_hash}"
        )
      end
    end

    it 'displays status tag' do
      expect(page).to have_selector('.status_tag', text: coin_deposit_operation.status)
    end

    it 'displays related transactions' do
      within 'div.panel:has(h3:contains("Related Transactions"))' do
        expect(page).to have_content('Related Transactions')
        expect(page).to have_link(coin_transaction.id.to_s)
        expect(page).to have_content(number_with_precision(coin_transaction.amount, precision: 8))
        expect(page).to have_content(number_with_precision(coin_transaction.snapshot_balance, precision: 8))
        expect(page).to have_content(number_with_precision(coin_transaction.snapshot_frozen_balance, precision: 8))
      end
    end
  end
end
