# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AmmPoolBroadcastService do
  describe '.call' do
    it 'broadcasts amm pool data to the amm_pool_channel' do
      amm_pool = create(:amm_pool)

      expect(AmmPoolChannel).to receive(:broadcast_to).with(
        AmmPoolChannel.channel_name,
        {
          status: 'success',
          data: {
            id: amm_pool.id,
            pair: amm_pool.pair,
            token0: amm_pool.token0,
            token1: amm_pool.token1,
            tick_spacing: amm_pool.tick_spacing,
            fee_percentage: amm_pool.fee_percentage,
            current_tick: amm_pool.current_tick,
            sqrt_price: amm_pool.sqrt_price,
            price: amm_pool.price,
            apr: amm_pool.apr,
            tvl_in_token0: amm_pool.tvl_in_token0,
            tvl_in_token1: amm_pool.tvl_in_token1,
            created_at: amm_pool.created_at.to_i,
            updated_at: amm_pool.updated_at.to_i
          }
        }
      )

      described_class.call(amm_pool)
    end

    it 'returns true when broadcast is successful' do
      amm_pool = create(:amm_pool)

      allow(AmmPoolChannel).to receive(:broadcast_to).and_return(true)

      expect(described_class.call(amm_pool)).to be true
    end

    it 'returns false and logs error when broadcast fails' do
      amm_pool = create(:amm_pool)
      error_message = 'Broadcast failed'

      allow(AmmPoolChannel).to receive(:broadcast_to).and_raise(StandardError.new(error_message))
      allow(Rails.logger).to receive(:error)

      expect(described_class.call(amm_pool)).to be false
      expect(Rails.logger).to have_received(:error).with("Failed to broadcast amm pool: #{error_message}")
    end
  end
end
