# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::CoinWithdrawalOperations', type: :system do
  let(:admin) { create(:admin_user, roles: 'superadmin') }
  let(:coin_withdrawal) { create(:coin_withdrawal) }
  let(:coin_withdrawal_operation) do
    create(:coin_withdrawal_operation,
           coin_withdrawal: coin_withdrawal,
           coin_currency: 'btc',
           coin_amount: 1.5,
           coin_fee: 0.0001,
           status: 'pending')
  end

  before do
    sign_in admin, scope: :admin_user
    coin_withdrawal_operation
  end

  describe '#index' do
    it 'displays withdrawal operations list' do
      visit admin_coin_withdrawal_operations_path

      expect(page).to have_content(coin_withdrawal_operation.id)
      expect(page).to have_content(coin_withdrawal_operation.coin_withdrawal.id)
      expect(page).to have_content('usdt')
      expect(page).to have_content('1.0')
      expect(page).to have_content('0.0')
      expect(page).to have_content('Pending')
    end

    it 'allows filtering by status' do
      visit admin_coin_withdrawal_operations_path

      within '.filter_form' do
        select 'pending', from: 'Status'
        find('input[type="submit"]').click
      end

      expect(page).to have_content(coin_withdrawal_operation.id)
    end
  end

  describe '#show' do
    it 'displays withdrawal operation details' do
      visit admin_coin_withdrawal_operation_path(coin_withdrawal_operation)

      expect(page).to have_content(coin_withdrawal_operation.id)
      expect(page).to have_content(coin_withdrawal_operation.coin_withdrawal.id)
      expect(page).to have_content('usdt')
      expect(page).to have_content('1.5')
      expect(page).to have_content('0.0001')
      expect(page).to have_content('Pending')
    end

    it 'displays transactions panel' do
      transaction = create(:coin_transaction,
                         operation: coin_withdrawal_operation,
                         amount: 1.5,
                         coin_currency: 'btc',
                         transaction_type: 'transfer')

      visit admin_coin_withdrawal_operation_path(coin_withdrawal_operation)

      within '#transactions' do
        expect(page).to have_content(transaction.id)
        expect(page).to have_content('1.5')
        expect(page).to have_content('btc')
        expect(page).to have_content(/transfer/i)
      end
    end

    context 'when operation is pending' do
      it 'shows relay button' do
        visit admin_coin_withdrawal_operation_path(coin_withdrawal_operation)

        within '#state_actions' do
          expect(page).to have_button('Start Relaying')
        end
      end

      it 'relays the operation' do
        operation = coin_withdrawal_operation
        allow(CoinWithdrawalOperation).to receive(:find).with(operation.id.to_s).and_return(operation)
        allow(operation).to receive(:relay_later)

        visit admin_coin_withdrawal_operation_path(operation)

        within '#state_actions' do
          click_button 'Start Relaying'
        end

        expect(operation).to have_received(:relay_later)
        expect(page).to have_content('Started relaying withdrawal operation')
      end
    end

    context 'when operation is not pending' do
      it 'does not show relay button' do
        operation = coin_withdrawal_operation
        operation.status = 'processed'
        operation.save!
        operation.reload

        visit admin_coin_withdrawal_operation_path(operation)

        within '#state_actions' do
          expect(page).to have_no_button('Start Relaying', visible: true)
        end
      end
    end
  end
end
