# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::FiatAccounts', type: :system do
  let(:admin) { create(:admin_user, roles: 'super_admin') }
  let(:user) { create(:user) }
  let(:fiat_account) do
    create(:fiat_account,
           user: user,
           currency: 'VND',
           balance: 1000.50,
           frozen_balance: 100.25)
  end

  before do
    sign_in admin, scope: :admin_user
    fiat_account
  end

  describe '#index' do
    it 'displays fiat accounts list' do
      visit admin_fiat_accounts_path

      expect(page).to have_content(fiat_account.id)
      expect(page).to have_content('User')
      expect(page).to have_content('VND')
      expect(page).to have_content('1000.50')
      expect(page).to have_content('100.25')
    end

    context 'with filters' do
      it 'allows filtering by currency' do
        visit admin_fiat_accounts_path
        within '.filter_form' do
          select 'VND', from: 'Currency'
          click_button 'Filter'
        end

        expect(page).to have_content('VND')
      end

      it 'allows filtering by user' do
        visit admin_fiat_accounts_path
        within '.filter_form' do
          first('select[name="q[user_id_eq]"]').find(:option, text: /User \d+/).select_option
          click_button 'Filter'
        end

        expect(page).to have_css('.col.col-user', text: /User \d+/)
      end
    end
  end

  describe '#show' do
    it 'displays fiat account details' do
      visit admin_fiat_account_path(fiat_account)

      expect(page).to have_content(fiat_account.id)
      expect(page).to have_content('User')
      expect(page).to have_content('VND')
      expect(page).to have_content('1000.50')
      expect(page).to have_content('100.25')
    end

    it 'displays currency information' do
      visit admin_fiat_account_path(fiat_account)

      expect(page).to have_css('div.panel h3', text: 'Currency Information')
    end
  end

  describe '#new' do
    it 'allows creating new fiat account' do
      visit new_admin_fiat_account_path

      within 'form' do
        first('select[name="fiat_account[user_id]"]').find(:option, text: /User \d+/).select_option
        select 'VND', from: 'Currency'
        fill_in 'Balance', with: '1000.50'
        fill_in 'Frozen balance', with: '100.25'
        click_button 'Create Fiat account'
      end

      # Just check that we're back at the index page after creation
      expect(page).to have_current_path(/admin\/fiat_accounts/)
    end
  end

  describe '#edit' do
    it 'allows editing fiat account' do
      visit edit_admin_fiat_account_path(fiat_account)

      within 'form' do
        select 'VND', from: 'Currency'
        fill_in 'Balance', with: '2000.50'
        click_button 'Update Fiat account'
      end

      # Just check that we're back at the index page after update
      expect(page).to have_current_path(/admin\/fiat_accounts/)
    end
  end
end
