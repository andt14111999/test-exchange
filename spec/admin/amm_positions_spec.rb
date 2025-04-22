# frozen_string_literal: true

require 'rails_helper'

describe 'AmmPosition Admin', type: :feature do
  let(:admin) { create(:admin_user) }
  let!(:position) { create(:amm_position, status: 'open') }

  before do
    allow_any_instance_of(AmmPosition).to receive(:send_event_create_amm_position)
    login_as(admin, scope: :admin_user)
  end

  describe 'index page' do
    it 'displays positions' do
      visit admin_amm_positions_path
      
      expect(page).to have_content(position.identifier)
      expect(page).to have_content(position.pool_pair)
      expect(page).to have_content(position.owner_account_key0)
    end

    it 'filters positions by status' do
      pending_position = create(:amm_position, status: 'pending')
      
      visit admin_amm_positions_path
      click_link 'Pending'
      
      expect(page).to have_content(pending_position.identifier)
      expect(page).not_to have_content(position.identifier)
    end
  end

  describe 'show page' do
    it 'displays position details' do
      visit admin_amm_position_path(position)
      
      expect(page).to have_content(position.identifier)
      expect(page).to have_content(position.pool_pair)
      expect(page).to have_content(position.owner_account_key0)
      expect(page).to have_content(position.owner_account_key1)
      expect(page).to have_content(position.status)
    end

    it 'shows collect fee button for open positions' do
      visit admin_amm_position_path(position)
      
      expect(page).to have_button('Collect Fee')
    end

    it 'shows close position button for open positions' do
      visit admin_amm_position_path(position)
      
      expect(page).to have_button('Close Position')
    end

    it 'does not show action buttons for non-open positions' do
      position.update!(status: 'pending')
      visit admin_amm_position_path(position)
      
      expect(page).not_to have_button('Collect Fee')
      expect(page).not_to have_button('Close Position')
    end
  end

  describe 'actions' do
    let(:service) { instance_double(KafkaService::Services::AmmPosition::AmmPositionService) }

    before do
      allow(KafkaService::Services::AmmPosition::AmmPositionService).to receive(:new).and_return(service)
      allow(service).to receive(:collect_fee)
      allow(service).to receive(:close)
    end

    it 'collects fee for a position' do
      expect(service).to receive(:collect_fee)
      
      visit admin_amm_position_path(position)
      click_button 'Collect Fee'
      
      expect(page).to have_content('Fee collection initiated')
    end

    it 'closes a position' do
      expect(service).to receive(:close)
      
      visit admin_amm_position_path(position)
      click_button 'Close Position'
      
      expect(page).to have_content('Position closing initiated')
    end
  end
end
