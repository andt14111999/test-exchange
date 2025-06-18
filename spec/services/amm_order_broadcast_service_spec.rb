# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AmmOrderBroadcastService, type: :service do
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
    it 'broadcasts amm order data' do
      user = create(:user)
      service = described_class.new(user)

      expect(service).to receive(:broadcast_amm_orders).and_return(true)
      service.call
    end
  end

  describe '#broadcast_amm_orders' do
    let(:user) { create(:user) }
    let(:amm_pool) { create(:amm_pool) }
    let(:amm_order) { create(:amm_order, user: user, amm_pool: amm_pool) }
    let(:expected_data) do
      {
        status: 'success',
        data: {
          amm_orders: [
            {
              id: amm_order.id,
              identifier: amm_order.identifier,
              status: amm_order.status,
              amount_specified: amm_order.amount_specified,
              amount_estimated: amm_order.amount_estimated,
              amount_actual: amm_order.amount_actual,
              amount_received: amm_order.amount_received,
              zero_for_one: amm_order.zero_for_one,
              slippage: amm_order.slippage,
              error_message: amm_order.error_message,
              created_at: amm_order.created_at,
              updated_at: amm_order.updated_at,
              amm_pool: {
                id: amm_pool.id,
                pair: amm_pool.pair,
                token0: amm_pool.token0,
                token1: amm_pool.token1
              }
            }
          ]
        }
      }
    end

    context 'when broadcast is successful' do
      it 'returns true' do
        amm_order # Create the order
        service = described_class.new(user)

        expect(AmmOrderChannel).to receive(:broadcast_to_user)
          .with(user, expected_data)
          .and_return(true)

        expect(service.send(:broadcast_amm_orders)).to be true
      end
    end

    context 'when broadcast fails' do
      it 'returns false' do
        amm_order # Create the order
        service = described_class.new(user)

        expect(AmmOrderChannel).to receive(:broadcast_to_user)
          .with(user, expected_data)
          .and_raise(StandardError)

        expect(service.send(:broadcast_amm_orders)).to be false
      end
    end

    context 'when user has no amm orders' do
      it 'broadcasts empty amm orders array' do
        service = described_class.new(user)
        expected_empty_data = {
          status: 'success',
          data: {
            amm_orders: []
          }
        }

        expect(AmmOrderChannel).to receive(:broadcast_to_user)
          .with(user, expected_empty_data)
          .and_return(true)

        expect(service.send(:broadcast_amm_orders)).to be true
      end
    end
  end

  describe '#amm_order_data' do
    it 'returns formatted amm order data' do
      user = create(:user)
      service = described_class.new(user)

      expect(service).to receive(:user_amm_orders_data).and_return([ { order_data: 'test' } ])

      result = service.send(:amm_order_data)

      expect(result).to eq(
        amm_orders: [ { order_data: 'test' } ]
      )
    end
  end

  describe '#user_amm_orders_data' do
    it 'returns formatted amm order data for user' do
      user = create(:user)
      amm_pool = create(:amm_pool)
      amm_order = create(:amm_order, user: user, amm_pool: amm_pool)
      service = described_class.new(user)

      result = service.send(:user_amm_orders_data)

      expect(result.length).to eq(1)
      expect(result.first).to include(
        id: amm_order.id,
        identifier: amm_order.identifier,
        status: amm_order.status,
        amount_specified: amm_order.amount_specified,
        amount_estimated: amm_order.amount_estimated,
        amm_pool: {
          id: amm_pool.id,
          pair: amm_pool.pair,
          token0: amm_pool.token0,
          token1: amm_pool.token1
        }
      )
    end

    it 'returns empty array when user has no amm orders' do
      user = create(:user)
      service = described_class.new(user)

      result = service.send(:user_amm_orders_data)

      expect(result).to eq([])
    end
  end
end
