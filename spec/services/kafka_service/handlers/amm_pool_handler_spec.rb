require 'rails_helper'

describe KafkaService::Handlers::AmmPoolHandler do
  describe '#handle' do
    let(:handler) { described_class.new }
    let(:amm_pool) { create(:amm_pool) }

    context 'when payload is nil' do
      it 'returns nil when payload is nil' do
        expect(handler.handle(nil)).to be_nil
      end
    end

    context 'when operation type is AMM_POOL_UPDATE' do
      it 'processes the amm pool update' do
        payload = {
          'operationType' => KafkaService::Config::OperationTypes::AMM_POOL_UPDATE,
          'pair' => amm_pool.pair,
          'actionId' => amm_pool.id
        }

        expect(handler).to receive(:process_amm_pool_update).with(payload)
        handler.handle(payload)
      end
    end

    context 'when operation type is AMM_POOL_CREATE' do
      it 'processes the amm pool update' do
        payload = {
          'operationType' => KafkaService::Config::OperationTypes::AMM_POOL_CREATE,
          'pair' => amm_pool.pair,
          'actionId' => amm_pool.id
        }

        expect(handler).to receive(:process_amm_pool_update).with(payload)
        handler.handle(payload)
      end
    end

    context 'when operation type is not supported' do
      it 'does not process the update' do
        payload = {
          'operationType' => 'unknown_operation',
          'pair' => amm_pool.pair,
          'actionId' => amm_pool.id
        }

        expect(handler).not_to receive(:process_amm_pool_update)
        handler.handle(payload)
      end
    end
  end

  describe '#process_amm_pool_update' do
    let(:handler) { described_class.new }
    let(:amm_pool) { create(:amm_pool) }

    context 'when record is not found' do
      it 'logs the error' do
        payload = {
          'operationType' => KafkaService::Config::OperationTypes::AMM_POOL_UPDATE,
          'pair' => 'UNKNOWN/PAIR',
          'actionId' => 999999
        }

        allow(Rails.logger).to receive(:error)
        handler.send(:process_amm_pool_update, payload)
      end
    end

    context 'when standard error occurs' do
      it 'logs the error' do
        payload = {
          'operationType' => KafkaService::Config::OperationTypes::AMM_POOL_UPDATE,
          'pair' => amm_pool.pair,
          'actionId' => amm_pool.id
        }

        allow(handler).to receive(:handle_update_response).and_raise(StandardError, 'Test error')
        expect(Rails.logger).to receive(:error).with(/Error processing deposit/)
        expect(Rails.logger).to receive(:error) # For backtrace

        handler.send(:process_amm_pool_update, payload)
      end
    end
  end

  describe '#handle_update_response' do
    let(:handler) { described_class.new }
    let(:amm_pool) { create(:amm_pool, updated_at: Time.current) }

    context 'when isSuccess is true' do
      it 'updates the amm pool with params from response' do
        current_time = Time.current
        payload = {
          'actionId' => amm_pool.id,
          'isSuccess' => 'true',
          'object' => {
            'feePercentage' => 0.005,
            'currentTick' => 100,
            'currentPrice' => 1.5,
            'isActive' => true,
            'updatedAt' => (current_time.to_f * 1000 + 5000).to_i # Add 5 seconds to make it newer
          }
        }

        handler.send(:handle_update_response, payload)
        amm_pool.reload

        expect(amm_pool.fee_percentage).to eq(0.005)
        expect(amm_pool.current_tick).to eq(100)
        expect(amm_pool.price).to eq(1.5)
        expect(amm_pool).to be_active
      end

      it 'does not update if message is older than the last update' do
        current_time = Time.current
        amm_pool.update(updated_at: current_time)

        payload = {
          'actionId' => amm_pool.id,
          'isSuccess' => 'true',
          'object' => {
            'feePercentage' => 0.005,
            'currentTick' => 100,
            'currentPrice' => 1.5,
            'isActive' => true,
            'updatedAt' => (current_time.to_f * 1000 - 5000).to_i # Make it older by 5 seconds
          }
        }

        allow(Rails.logger).to receive(:error)

        handler.send(:handle_update_response, payload)
        amm_pool.reload
        expect(amm_pool.fee_percentage).not_to eq(0.005)
      end
    end

    context 'when isSuccess is false' do
      it 'updates only the status explanation' do
        payload = {
          'actionId' => amm_pool.id,
          'isSuccess' => 'false',
          'errorMessage' => 'Invalid pool parameters'
        }

        handler.send(:handle_update_response, payload)
        amm_pool.reload

        expect(amm_pool.status_explanation).to eq('Exchange Engine: Invalid pool parameters')
      end
    end

    context 'when error occurs' do
      it 'logs the error' do
        payload = {
          'actionId' => amm_pool.id,
          'isSuccess' => 'true',
          'object' => {}
        }

        allow(AmmPool).to receive(:find).and_raise(StandardError, 'Test error')
        expect(Rails.logger).to receive(:error).with(/Error handling update response/)

        handler.send(:handle_update_response, payload)
      end
    end
  end

  describe '#extract_params_from_response' do
    let(:handler) { described_class.new }

    it 'extracts params correctly from object' do
      time_now = Time.current
      object = {
        'feePercentage' => 0.005,
        'feeProtocolPercentage' => 0.01,
        'currentTick' => 100,
        'currentSqrtPrice' => 1.2,
        'currentPrice' => 1.5,
        'currentLiquidity' => 100000,
        'feeGrowthGlobal0' => 0.001,
        'feeGrowthGlobal1' => 0.002,
        'protocolFees0' => 10,
        'protocolFees1' => 20,
        'volumeToken0' => 1000,
        'volumeToken1' => 2000,
        'volumeUsd' => 3000,
        'totalValueLockedToken0' => 5000,
        'totalValueLockedToken1' => 6000,
        'statusExplanation' => 'All good',
        'updatedAt' => (time_now.to_f * 1000).to_i,
        'isActive' => true
      }

      params = handler.send(:extract_params_from_response, object)

      expect(params[:fee_percentage]).to eq(0.005)
      expect(params[:fee_protocol_percentage]).to eq(0.01)
      expect(params[:current_tick]).to eq(100)
      expect(params[:sqrt_price]).to eq(1.2)
      expect(params[:price]).to eq(1.5)
      expect(params[:liquidity]).to eq(100000)
      expect(params[:fee_growth_global0]).to eq(0.001)
      expect(params[:fee_growth_global1]).to eq(0.002)
      expect(params[:protocol_fees0]).to eq(10)
      expect(params[:protocol_fees1]).to eq(20)
      expect(params[:volume_token0]).to eq(1000)
      expect(params[:volume_token1]).to eq(2000)
      expect(params[:volume_usd]).to eq(3000)
      expect(params[:total_value_locked_token0]).to eq(5000)
      expect(params[:total_value_locked_token1]).to eq(6000)
      expect(params[:status_explanation]).to eq('All good')
      expect(params[:updated_at]).to be_within(1.second).of(time_now)
      expect(params[:status]).to eq('active')
    end

    it 'sets status to inactive when isActive is false' do
      object = {
        'updatedAt' => (Time.current.to_f * 1000).to_i,
        'isActive' => false
      }

      params = handler.send(:extract_params_from_response, object)
      expect(params[:status]).to eq('inactive')
    end

    it 'compacts the results to remove nil values' do
      object = {
        'feePercentage' => 0.005,
        'updatedAt' => (Time.current.to_f * 1000).to_i,
        'isActive' => true
      }

      params = handler.send(:extract_params_from_response, object)
      expect(params.keys).to contain_exactly(:fee_percentage, :updated_at, :status)
    end
  end
end
