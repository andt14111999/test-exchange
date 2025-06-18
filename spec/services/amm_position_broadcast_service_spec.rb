# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AmmPositionBroadcastService, type: :service do
  describe '.call' do
    it 'initializes and calls the service' do
      user = create(:user)
      service = instance_double(described_class)

      expect(described_class).to receive(:new).with(user).and_return(service)
      expect(service).to receive(:call)

      described_class.call(user)
    end
  end

  describe '#call' do
    it 'broadcasts amm position data' do
      user = create(:user)
      service = described_class.new(user)

      expect(service).to receive(:broadcast_amm_positions).and_return(true)
      service.call
    end
  end

  describe '#broadcast_amm_positions' do
    let(:user) { create(:user) }
    let(:amm_pool) { create(:amm_pool) }
    let(:amm_position) { create(:amm_position, user: user, amm_pool: amm_pool) }
    let(:expected_data) do
      {
        status: 'success',
        data: {
          amm_positions: [
            {
              id: amm_position.id,
              identifier: amm_position.identifier,
              status: amm_position.status,
              liquidity: amm_position.liquidity,
              amount0: amm_position.amount0,
              amount1: amm_position.amount1,
              amount0_initial: amm_position.amount0_initial,
              amount1_initial: amm_position.amount1_initial,
              tick_lower_index: amm_position.tick_lower_index,
              tick_upper_index: amm_position.tick_upper_index,
              fee_collected0: amm_position.fee_collected0,
              fee_collected1: amm_position.fee_collected1,
              tokens_owed0: amm_position.tokens_owed0,
              tokens_owed1: amm_position.tokens_owed1,
              estimate_fee_token0: amm_position.estimate_fee_token0,
              estimate_fee_token1: amm_position.estimate_fee_token1,
              apr: amm_position.apr,
              created_at: amm_position.created_at,
              updated_at: amm_position.updated_at,
              amm_pool: {
                id: amm_pool.id,
                pair: amm_pool.pair,
                token0: amm_pool.token0,
                token1: amm_pool.token1,
                price: amm_pool.price
              }
            }
          ]
        }
      }
    end

    context 'when broadcast is successful' do
      it 'returns true' do
        amm_position # Create the position
        service = described_class.new(user)

        expect(AmmPositionChannel).to receive(:broadcast_to_user)
          .with(user, expected_data)
          .and_return(true)

        expect(service.send(:broadcast_amm_positions)).to be true
      end
    end

    context 'when broadcast fails' do
      it 'returns false' do
        amm_position # Create the position
        service = described_class.new(user)

        expect(AmmPositionChannel).to receive(:broadcast_to_user)
          .with(user, expected_data)
          .and_raise(StandardError)

        expect(service.send(:broadcast_amm_positions)).to be false
      end
    end

    context 'when user has no amm positions' do
      it 'broadcasts empty amm positions array' do
        service = described_class.new(user)
        expected_empty_data = {
          status: 'success',
          data: {
            amm_positions: []
          }
        }

        expect(AmmPositionChannel).to receive(:broadcast_to_user)
          .with(user, expected_empty_data)
          .and_return(true)

        expect(service.send(:broadcast_amm_positions)).to be true
      end
    end
  end

  describe '#amm_position_data' do
    it 'returns formatted amm position data' do
      user = create(:user)
      service = described_class.new(user)

      expect(service).to receive(:user_amm_positions_data).and_return([ { position_data: 'test' } ])

      result = service.send(:amm_position_data)

      expect(result).to eq(
        amm_positions: [ { position_data: 'test' } ]
      )
    end
  end

  describe '#user_amm_positions_data' do
    it 'returns formatted amm position data for user' do
      user = create(:user)
      amm_pool = create(:amm_pool)
      amm_position = create(:amm_position, user: user, amm_pool: amm_pool)
      service = described_class.new(user)

      result = service.send(:user_amm_positions_data)

      expect(result.length).to eq(1)
      expect(result.first).to include(
        id: amm_position.id,
        identifier: amm_position.identifier,
        status: amm_position.status,
        liquidity: amm_position.liquidity,
        amount0: amm_position.amount0,
        amount1: amm_position.amount1,
        amm_pool: {
          id: amm_pool.id,
          pair: amm_pool.pair,
          token0: amm_pool.token0,
          token1: amm_pool.token1,
          price: amm_pool.price
        }
      )
    end

    it 'returns empty array when user has no amm positions' do
      user = create(:user)
      service = described_class.new(user)

      result = service.send(:user_amm_positions_data)

      expect(result).to eq([])
    end
  end
end
