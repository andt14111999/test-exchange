# frozen_string_literal: true

require 'rails_helper'

describe 'AmmPosition Admin', type: :feature do
  let(:admin) { create(:admin_user, :super_admin) }
  let!(:position) { create(:amm_position, status: 'open') }

  before do
    allow_any_instance_of(AmmPosition).to receive(:send_event_create_amm_position)
    login_as(admin, scope: :admin_user)
  end

  describe 'index page' do
    it 'displays positions' do
      visit admin_amm_positions_path

      expect(page).to have_content(position.identifier)
      expect(page).to have_content(position.amm_pool.pair)
    end

    it 'filters positions by status' do
      pending_position = create(:amm_position, status: 'pending')

      visit admin_amm_positions_path
      within '.scopes' do
        click_link 'Pending'
      end

      expect(page).to have_content(pending_position.identifier)
      expect(page).not_to have_content(position.identifier)
    end
  end

  describe 'show page' do
    it 'displays position details' do
      visit admin_amm_position_path(position)

      expect(page).to have_content(position.identifier)
      expect(page).to have_content(position.amm_pool.pair)
      expect(page).to have_content(position.account_key0 || 'Empty')
      expect(page).to have_content(position.account_key1 || 'Empty')
      expect(page).to have_content(position.status.capitalize)
    end

    it 'displays position liquidity' do
      visit admin_amm_position_path(position)

      expect(page).to have_content("Liquidity")
      expect(page).to have_content(number_with_delimiter(position.liquidity.to_f.round(6)))
    end

    it 'displays position amounts' do
      visit admin_amm_position_path(position)

      expect(page).to have_content("Amount0")
      expect(page).to have_content("Amount1")
      expect(page).to have_content(number_with_delimiter(position.amount0.to_f.round(6)))
      expect(page).to have_content(number_with_delimiter(position.amount1.to_f.round(6)))
    end
  end

  private

  def number_with_delimiter(number)
    ActiveSupport::NumberHelper.number_to_delimited(number)
  end
end
