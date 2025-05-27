# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::CoinWithdrawals::Entity do
  let(:user) { create(:user) }
  let(:receiver) { create(:user) }
  let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }

  before do
    coin_account
  end

  describe 'exposures' do
    context 'with external withdrawal' do
      let(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: 'erc20') }
      let(:entity) { described_class.new(withdrawal) }
      let(:json) { entity.as_json }

      it 'exposes basic fields' do
        expect(json[:id]).to eq(withdrawal.id)
        expect(json[:coin_currency]).to eq(withdrawal.coin_currency)
        expect(json[:coin_amount]).to eq(withdrawal.coin_amount.to_s)
        expect(json[:coin_fee]).to eq(withdrawal.coin_fee.to_s)
        expect(json[:status]).to eq(withdrawal.status)
      end

      it 'exposes external withdrawal fields' do
        expect(json[:coin_address]).to eq(withdrawal.coin_address)
        expect(json[:coin_layer]).to eq(withdrawal.coin_layer)
      end

      it 'does not expose internal transfer fields' do
        expect(json).not_to have_key(:receiver_email)
        expect(json).not_to have_key(:receiver_username)
        expect(json).not_to have_key(:internal_transfer_status)
      end

      it 'exposes is_internal_transfer as false' do
        expect(json[:is_internal_transfer]).to be false
      end
    end

    context 'with email internal transfer' do
      let(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_email: receiver.email) }
      let(:entity) { described_class.new(withdrawal) }
      let(:json) { entity.as_json }

      it 'exposes basic fields' do
        expect(json[:id]).to eq(withdrawal.id)
        expect(json[:coin_currency]).to eq(withdrawal.coin_currency)
        expect(json[:coin_amount]).to eq(withdrawal.coin_amount.to_s)
        expect(json[:coin_fee]).to eq(withdrawal.coin_fee.to_s)
        expect(json[:status]).to eq(withdrawal.status)
      end

      it 'exposes receiver_email for email-based internal transfer' do
        expect(json[:receiver_email]).to eq(receiver.email)
      end

      it 'does not expose external withdrawal fields' do
        expect(json).not_to have_key(:coin_address)
        expect(json).not_to have_key(:coin_layer)
        expect(json).not_to have_key(:tx_hash)
      end

      it 'does not expose other internal transfer identifiers' do
        expect(json).not_to have_key(:receiver_username)
      end

      it 'exposes is_internal_transfer as true' do
        expect(json[:is_internal_transfer]).to be true
      end

      it 'exposes internal_transfer_status' do
        expect(json).to have_key(:internal_transfer_status)
      end
    end

    context 'with username internal transfer' do
      let(:withdrawal) do
        receiver.update!(username: 'testusername')
        create(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_username: receiver.username)
      end
      let(:entity) { described_class.new(withdrawal) }
      let(:json) { entity.as_json }

      it 'exposes basic fields' do
        expect(json[:id]).to eq(withdrawal.id)
        expect(json[:coin_currency]).to eq(withdrawal.coin_currency)
        expect(json[:coin_amount]).to eq(withdrawal.coin_amount.to_s)
        expect(json[:coin_fee]).to eq(withdrawal.coin_fee.to_s)
        expect(json[:status]).to eq(withdrawal.status)
      end

      it 'exposes receiver_username for username-based internal transfer' do
        expect(json[:receiver_username]).to eq(receiver.username)
      end

      it 'does not expose external withdrawal fields' do
        expect(json).not_to have_key(:coin_address)
        expect(json).not_to have_key(:coin_layer)
        expect(json).not_to have_key(:tx_hash)
      end

      it 'does not expose other internal transfer identifiers' do
        expect(json).not_to have_key(:receiver_email)
      end

      it 'exposes is_internal_transfer as true' do
        expect(json[:is_internal_transfer]).to be true
      end

      it 'exposes internal_transfer_status' do
        expect(json).to have_key(:internal_transfer_status)
      end
    end
  end
end
