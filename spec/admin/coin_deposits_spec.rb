# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::CoinDeposits', type: :request do
  let(:admin) { create(:admin_user, roles: 'admin') }
  let(:current_time) { Time.current }

  before do
    sign_in admin, scope: :admin_user
    allow(Time).to receive(:current).and_return(current_time)
    allow_any_instance_of(ActiveAdmin::ResourceController).to receive(:authorized?).and_return(true)
  end

  context 'index page' do
    it 'filters deposits by status' do
      deposit = create(:coin_deposit, :verified)
      get admin_coin_deposits_path(q: { status_eq: 'verified' })

      expect(response.body).to include(deposit.id.to_s)
      expect(response.body).to include('Verified')
    end

    it 'displays deposit details' do
      deposit = create(:coin_deposit)
      get admin_coin_deposits_path

      expect(response.body).to include(deposit.id.to_s)
      expect(response.body).to include('Pending')
    end
  end

  context 'show page' do
    it 'displays deposit details' do
      deposit = create(:coin_deposit)
      get admin_coin_deposits_path

      expect(response.body).to include(deposit.id.to_s)
      expect(response.body).to include('Pending')
    end

    it 'displays last seen IP with link when IP is present' do
      deposit = create(:coin_deposit, last_seen_ip: '1.2.3.4')
      get admin_coin_deposit_path(deposit)

      expect(response.body).to include('1.2.3.4')
      expect(response.body).to include('https://ipinfo.io/1.2.3.4')
      expect(response.body).to include('target="_blank"')
      expect(response.body).to include('rel="noopener"')
    end

    it 'displays empty when last seen IP is not present' do
      deposit = create(:coin_deposit, last_seen_ip: nil)
      get admin_coin_deposit_path(deposit)

      expect(response.body).to include('<span class="empty">Empty</span>')
    end

    it 'displays coin deposit operation details' do
      deposit = create(:coin_deposit)
      operation = create(:coin_deposit_operation,
        coin_deposit: deposit,
        status: 'completed',
        coin_amount: 1.23456789,
        coin_fee: 0.0001,
        platform_fee: 0.0002
      )
      get admin_coin_deposit_path(deposit)

      expect(response.body).to include(operation.id.to_s)
      expect(response.body).to include('completed')
      expect(response.body).to include('1.23456789')
      expect(response.body).to include('0.00010000')
      expect(response.body).to include('0.00020000')
      expect(response.body).to include(operation.created_at.strftime('%B %d, %Y %H:%M'))
    end

    it 'displays release button for locked deposit' do
      deposit = create(:coin_deposit, :locked)
      allow(deposit).to receive(:may_release?).and_return(true)
      get admin_coin_deposit_path(deposit)

      expect(response.body).to include('Release')
      expect(response.body).to include(release_admin_coin_deposit_path(deposit))
      expect(response.body).to include('<input type="hidden" name="_method" value="put"')
      expect(response.body).to include('class="button"')
      expect(response.body).to include('data-confirm="Are you sure?"')
    end

    it 'does not display release button for non-locked deposit' do
      deposit = create(:coin_deposit)
      allow(deposit).to receive(:may_release?).and_return(false)
      get admin_coin_deposit_path(deposit)

      expect(response.body).not_to include('Release')
      expect(response.body).not_to include(release_admin_coin_deposit_path(deposit))
    end

    it 'verifies a deposit' do
      deposit = create(:coin_deposit, status: 'pending')

      put verify_admin_coin_deposit_path(deposit)

      expect(response).to have_http_status(:found)
      expect(response).to redirect_to(admin_coin_deposit_path(deposit))

      follow_redirect!
      expect(response.body).to include('Deposit was verified successfully')
    end

    it 'rejects a deposit' do
      deposit = create(:coin_deposit, status: 'pending')

      put reject_admin_coin_deposit_path(deposit)

      expect(response).to have_http_status(:found)
      expect(response).to redirect_to(admin_coin_deposit_path(deposit))

      follow_redirect!
      expect(response.body).to include('Deposit was rejected successfully')
    end

    it 'releases a deposit' do
      deposit = create(:coin_deposit, status: 'locked')

      put release_admin_coin_deposit_path(deposit)

      expect(response).to have_http_status(:found)
      expect(response).to redirect_to(admin_coin_deposit_path(deposit))

      follow_redirect!
      expect(response.body).to include('Deposit was released successfully')
    end
  end

  context 'batch actions' do
    it 'verifies multiple deposits' do
      deposits = create_list(:coin_deposit, 2, status: 'pending')

      post batch_action_admin_coin_deposits_path, params: {
        batch_action: 'verify',
        collection_selection: deposits.map(&:id)
      }

      expect(response).to have_http_status(:found)
      expect(response).to redirect_to(admin_coin_deposits_path)

      follow_redirect!
      expect(response.body).to include('Selected deposits were verified')
    end

    it 'rejects multiple deposits' do
      deposits = create_list(:coin_deposit, 2, status: 'pending')

      post batch_action_admin_coin_deposits_path, params: {
        batch_action: 'reject',
        collection_selection: deposits.map(&:id)
      }

      expect(response).to have_http_status(:found)
      expect(response).to redirect_to(admin_coin_deposits_path)

      follow_redirect!
      expect(response.body).to include('Selected deposits were rejected')
    end
  end
end
