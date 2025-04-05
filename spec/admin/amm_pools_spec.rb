require 'rails_helper'

describe 'Admin AmmPools', type: :feature do
  let(:admin_user) { create(:admin_user, role: 'implementor') }

  before do
    login_as(admin_user, scope: :admin_user)
    allow_any_instance_of(KafkaService::Services::AmmPool::AmmPoolService).to receive(:create)
    allow_any_instance_of(KafkaService::Services::AmmPool::AmmPoolService).to receive(:update)
  end

  it 'creates an amm pool and shows price range based on tick selection', :js do
    visit new_admin_amm_pool_path

    fill_in 'Pair', with: 'USDT/VND'
    fill_in 'Token 0', with: 'USDT'
    fill_in 'Token 1', with: 'VND'
    select '10', from: 'Tick spacing'

    # We should see the price range information update
    expect(page).to have_content('Price range:')

    # Test tick_spacing changes update the price range display
    select '60', from: 'Tick spacing'
    # Wait for JavaScript to update the price range
    expect(page).to have_content('Price range:')

    # Test USDT/VND demo button
    click_link 'Demo: USDT/VND (1 USDT = 26,000 VND)'

    # Verify form is filled with correct values
    expect(page).to have_field('Pair', with: 'USDT/VND')
    expect(page).to have_field('Token 0', with: 'USDT')
    expect(page).to have_field('Token 1', with: 'VND')
    expect(page).to have_select('Tick spacing', selected: '60')

    select 'pending', from: 'Status'

    click_button 'Create Amm pool'

    expect(page).to have_content('Amm pool was successfully created')
    expect(page).to have_content('USDT/VND')
  end

  it 'displays and updates amm pools' do
    pool = create(:amm_pool, status: 'active')

    visit admin_amm_pools_path
    expect(page).to have_content(pool.pair)

    click_link 'Edit'

    # When active, can only update fee_percentage, fee_protocol_percentage and status
    expect(page).not_to have_field('Pair')
    expect(page).not_to have_field('Token 0')
    expect(page).not_to have_field('Token 1')

    select '0.05% (Stable)', from: 'Fee percentage'
    click_button 'Update Amm pool'

    expect(page).to have_content('Amm pool was successfully updated')
    expect(page).to have_content('0.050%')
  end

  it 'allows editing all fields for pending pools' do
    pool = create(:amm_pool, status: 'pending')

    visit edit_admin_amm_pool_path(pool)

    # All fields should be editable for pending pools
    expect(page).to have_field('Pair')
    expect(page).to have_field('Token 0')
    expect(page).to have_field('Token 1')

    fill_in 'Current tick', with: '200'
    click_button 'Update Amm pool'

    expect(page).to have_content('Amm pool was successfully updated')
  end
end
