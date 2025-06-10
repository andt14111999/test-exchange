# frozen_string_literal: true

require 'rails_helper'

describe 'Tick Admin', type: :feature do
  let(:admin) { create(:admin_user, :superadmin) }
  let(:amm_pool) { create(:amm_pool) }
  let!(:tick) { create(:tick, amm_pool: amm_pool, status: 'active') }
  let!(:inactive_tick) { create(:tick, :inactive, amm_pool: amm_pool) }

  before do
    login_as(admin, scope: :admin_user)
  end

  describe 'index page' do
    it 'displays ticks' do
      visit admin_ticks_path

      expect(page).to have_content(tick.tick_key)
      expect(page).to have_content(tick.pool_pair)
      expect(page).to have_content(tick.tick_index)
    end

    it 'filters ticks by status' do
      visit admin_ticks_path

      # Đảm bảo rằng cả hai tick đều được tạo với tick_key khác nhau
      expect(tick.tick_key).not_to eq(inactive_tick.tick_key)

      # Lọc theo trạng thái inactive - sử dụng tên hiển thị đúng (viết hoa chữ cái đầu)
      within '#filters_sidebar_section' do
        select 'Inactive', from: 'Status'
        click_button 'Filter'
      end

      # Kiểm tra xem chỉ hiển thị tick inactive
      within 'table.index_table' do
        expect(page).to have_content(inactive_tick.tick_key)
        expect(page).not_to have_content(tick.tick_key)
      end
    end

    it 'filters ticks by amm_pool' do
      another_pool = create(:amm_pool)
      another_tick = create(:tick, amm_pool: another_pool)

      visit admin_ticks_path

      # Đảm bảo rằng các tick thuộc các pool khác nhau
      expect(tick.amm_pool_id).not_to eq(another_tick.amm_pool_id)

      # Lọc theo amm_pool
      within '#filters_sidebar_section' do
        select amm_pool.pair, from: 'Amm pool'
        click_button 'Filter'
      end

      # Chỉ hiển thị tick của amm_pool đã chọn
      within 'table.index_table' do
        expect(page).to have_content(tick.tick_key)
        expect(page).not_to have_content(another_tick.tick_key)
      end
    end
  end

  describe 'show page' do
    it 'displays tick details' do
      visit admin_tick_path(tick)

      expect(page).to have_content(tick.tick_key)
      expect(page).to have_content(tick.pool_pair)
      expect(page).to have_content(tick.tick_index)
      expect(page).to have_content(tick.liquidity_gross)
      expect(page).to have_content(tick.liquidity_net)
      expect(page).to have_content(tick.status.capitalize)
    end

    it 'displays fee growth information' do
      visit admin_tick_path(tick)

      expect(page).to have_content("Fee Growth Outside0")
      expect(page).to have_content(tick.fee_growth_outside0)
      expect(page).to have_content("Fee Growth Outside1")
      expect(page).to have_content(tick.fee_growth_outside1)
    end

    it 'displays initialization information' do
      visit admin_tick_path(tick)

      expect(page).to have_content("Initialized")
      expect(page).to have_content("Tick Initialized Timestamp")
      expect(page).to have_content(tick.tick_initialized_timestamp)
    end

    it 'displays timestamp information' do
      visit admin_tick_path(tick)

      expect(page).to have_content("Created At Timestamp")
      expect(page).to have_content(tick.created_at_timestamp)
      expect(page).to have_content("Updated At Timestamp")
      expect(page).to have_content(tick.updated_at_timestamp)
    end
  end
end
