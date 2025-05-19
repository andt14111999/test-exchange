require 'rails_helper'

RSpec.describe 'Admin::AmmPools', type: :feature do
  context 'when admin is signed in' do
    before do
      admin = create(:admin_user, :admin)
      login_as admin, scope: :admin_user
    end

    it 'displays index page with pool information' do
      amm_pool = create(:amm_pool)
      visit admin_amm_pools_path

      expect(page).to have_content(amm_pool.pair)
      expect(page).to have_content(amm_pool.token0)
      expect(page).to have_content(amm_pool.token1)
    end

    it 'filters pools by status' do
      active_pool = create(:amm_pool, status: 'active')
      inactive_pool = create(:amm_pool, status: 'inactive')

      visit admin_amm_pools_path
      within '.scopes' do
        click_link 'Active'
      end

      expect(page).to have_content(active_pool.pair)
      expect(page).not_to have_content(inactive_pool.pair)
    end

    it 'filters pools by pair' do
      pool1 = create(:amm_pool, pair: 'USDT/VND')
      pool2 = create(:amm_pool, pair: 'BTC/USDT')

      visit admin_amm_pools_path
      within '.filter_form' do
        fill_in 'q[pair_cont]', with: 'USDT/VND'
        click_button 'Filter'
      end

      expect(page).to have_content('USDT/VND')
      expect(page).not_to have_content('BTC/USDT')
    end

    it 'displays pool details on show page' do
      amm_pool = create(:amm_pool)
      visit admin_amm_pool_path(amm_pool)

      expect(page).to have_content(amm_pool.pair)
      expect(page).to have_content(amm_pool.token0)
      expect(page).to have_content(amm_pool.token1)
      expect(page).to have_content(amm_pool.tick_spacing)
      expect(page).to have_content('0.300%')
      expect(page).to have_content('5.000%')
    end

    it 'displays new pool form with tickrate explanation' do
      visit new_admin_amm_pool_path

      expect(page).to have_content('Giải thích về Tick Rate và Pricing trong AMM Pool')
      expect(page).to have_field('Pair')
      expect(page).to have_field('Token0')
      expect(page).to have_field('Token1')
      expect(page).to have_field('Tick spacing')
      expect(page).to have_field('Init price')
      expect(page).to have_field('Fee Ratio')
      expect(page).to have_field('Fee Protocol Ratio')
    end

    it 'creates new pool with valid params' do
      visit new_admin_amm_pool_path

      within 'form.amm_pool' do
        fill_in 'Pair', with: 'USDT/VND'
        fill_in 'Token0', with: 'USDT'
        fill_in 'Token1', with: 'VND'
        fill_in 'Tick spacing', with: '60'
        fill_in 'Init price', with: '24000'
        fill_in 'Fee Ratio', with: '0.003'
        fill_in 'Fee Protocol Ratio', with: '0.05'
        find('input[type="submit"]').click
      end

      expect(page).to have_content('Amm pool was successfully created')
      expect(page).to have_content('USDT/VND')
      expect(page).to have_content('USDT')
      expect(page).to have_content('VND')
      expect(page).to have_content('60')
      expect(page).to have_content('24000')
      expect(page).to have_content('0.300%')
      expect(page).to have_content('5.000%')
    end

    it 'shows error when creating pool with invalid params' do
      visit new_admin_amm_pool_path

      within 'form.amm_pool' do
        fill_in 'Pair', with: ''
        fill_in 'Token0', with: ''
        fill_in 'Token1', with: ''
        find('input[type="submit"]').click
      end

      expect(page).to have_content("can't be blank")
    end

    it 'updates pool with valid params' do
      amm_pool = create(:amm_pool, status: 'pending')
      visit edit_admin_amm_pool_path(amm_pool)

      allow_any_instance_of(AmmPool).to receive(:send_event_update_amm_pool) do |pool, params|
        pool.update!(params)
        true
      end

      within 'form.amm_pool' do
        select 'active', from: 'Status'
        fill_in 'Fee Ratio', with: '0.002'
        fill_in 'Fee Protocol Ratio', with: '0.03'
        find('input[type="submit"]').click
      end

      expect(page).to have_content('AMM Pool was successfully updated')

      amm_pool.reload
      expect(amm_pool.fee_percentage).to eq(0.002)
      expect(amm_pool.fee_protocol_percentage).to eq(0.03)
    end

    it 'shows error message when update fails' do
      amm_pool = create(:amm_pool, status: 'pending')
      visit edit_admin_amm_pool_path(amm_pool)

      allow_any_instance_of(AmmPool).to receive(:send_event_update_amm_pool)
        .and_raise(StandardError.new('Update failed'))

      within 'form.amm_pool' do
        select 'active', from: 'Status'
        find('input[type="submit"]').click
      end

      expect(page).to have_content('Update failed')
    end

    it 'shows init_price field for pool without liquidity' do
      amm_pool = create(:amm_pool,
        total_value_locked_token0: 0,
        total_value_locked_token1: 0,
        status: 'pending'
      )

      visit edit_admin_amm_pool_path(amm_pool)
      within 'form.amm_pool' do
        expect(page).to have_field('Init price')
      end
    end

    it 'hides init_price field for pool with liquidity' do
      amm_pool = create(:amm_pool,
        total_value_locked_token0: 100,
        total_value_locked_token1: 100,
        status: 'active'
      )

      visit edit_admin_amm_pool_path(amm_pool)
      within 'form.amm_pool' do
        expect(page).not_to have_field('Init price')
      end
    end
  end

  context 'when admin is not signed in' do
    it 'redirects to sign in page' do
      visit admin_amm_pools_path
      expect(page).to have_current_path(new_admin_user_session_path)
    end
  end
end
