# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::CoinAccounts', type: :system do
  include ActionView::Helpers::NumberHelper

  describe 'index page' do
    it 'displays a list of coin accounts' do
      admin_user = create(:admin_user, :admin)
      user = create(:user, email: 'test@example.com')
      coin_account = create(:coin_account, :btc_main, user: user, balance: 1.5, frozen_balance: 0.5)

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_accounts_path
      expect(page).to have_content(coin_account.id)
      expect(page).to have_content('User')
      expect(page).to have_content('btc')
      expect(page).to have_content('all')
      expect(page).to have_content('main')
      expect(page).to have_content(number_with_precision(coin_account.balance, precision: 8))
      expect(page).to have_content(number_with_precision(coin_account.frozen_balance, precision: 8))
      expect(page).to have_content(coin_account.address) if coin_account.address.present?
      expect(page).to have_content(coin_account.created_at.strftime('%B %d, %Y %H:%M'))
    end

    it 'has working filters' do
      admin_user = create(:admin_user, :admin)
      user = create(:user, email: 'test@example.com')
      coin_account = create(:coin_account, :btc_main, user: user, balance: 1.5, frozen_balance: 0.5)

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_accounts_path

      click_link 'Filters'
      select 'btc', from: 'q_coin_currency'
      click_button 'Filter'
      expect(page).to have_content(coin_account.id)

      select 'main', from: 'q_account_type'
      click_button 'Filter'
      expect(page).to have_content(coin_account.id)
    end
  end

  describe 'show page' do
    it 'displays account details and balances' do
      admin_user = create(:admin_user, :admin)
      user = create(:user, email: 'test@example.com')
      coin_account = create(:coin_account, :btc_main, user: user, balance: 1.5, frozen_balance: 0.5)

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_account_path(coin_account)
      expect(page).to have_content(coin_account.id)
      expect(page).to have_content('User')
      expect(page).to have_content('btc')
      expect(page).to have_content('all')
      expect(page).to have_content('main')
      expect(page).to have_content(number_with_precision(coin_account.balance, precision: 8))
      expect(page).to have_content(number_with_precision(coin_account.frozen_balance, precision: 8))
      expect(page).to have_content(coin_account.address) if coin_account.address.present?
      expect(page).to have_content(coin_account.created_at.strftime('%B %d, %Y %H:%M'))
      expect(page).to have_content(coin_account.updated_at.strftime('%B %d, %Y %H:%M'))
    end

    it 'displays total balances across all layers' do
      admin_user = create(:admin_user, :admin)
      user = create(:user, email: 'test@example.com')
      coin_account = create(:coin_account, :btc_main, user: user, balance: 1.5, frozen_balance: 0.5)
      deposit_account = create(:coin_account, :btc_deposit, user: user, balance: 0.5)

      login_as(admin_user, scope: :admin_user)
      visit admin_coin_account_path(coin_account)

      within 'div.panel:has(h3:contains("Total Balances Across All Layers"))' do
        expect(page).to have_content(number_with_precision(coin_account.frozen_balance, precision: 8))
        expect(page).to have_content(number_with_precision(coin_account.balance - coin_account.frozen_balance, precision: 8))
      end
    end
  end
end
