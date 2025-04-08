require 'rails_helper'

describe AmmPool, type: :model do
  describe 'validations' do
    it 'validates presence of pair' do
      pool = build(:amm_pool, pair: nil)
      expect(pool).to be_invalid
      expect(pool.errors[:pair]).to include("can't be blank")
    end

    it 'validates presence of token0' do
      pool = build(:amm_pool, token0: nil)
      expect(pool).to be_invalid
      expect(pool.errors[:token0]).to include("can't be blank")
    end

    it 'validates presence of token1' do
      pool = build(:amm_pool, token1: nil)
      expect(pool).to be_invalid
      expect(pool.errors[:token1]).to include("can't be blank")
    end

    it 'validates presence of tick_spacing' do
      pool = build(:amm_pool, tick_spacing: nil)
      expect(pool).to be_invalid
      expect(pool.errors[:tick_spacing]).to include("can't be blank")
    end

    it 'validates presence of fee_percentage' do
      pool = build(:amm_pool, fee_percentage: nil)
      expect(pool).to be_invalid
      expect(pool.errors[:fee_percentage]).to include("can't be blank")
    end

    it 'validates uniqueness of token pair' do
      create(:amm_pool, token0: 'USDT', token1: 'VND')
      pool = build(:amm_pool, token0: 'USDT', token1: 'VND')
      expect(pool).to be_invalid
      expect(pool.errors[:token0]).to include("the pool of token0 and token1 already exists")
    end

    it 'validates init_price is positive when present' do
      pool = build(:amm_pool, init_price: -1)
      expect(pool).to be_invalid
      expect(pool.errors[:init_price]).to include('must be greater than 0')

      pool = build(:amm_pool, init_price: 0)
      expect(pool).to be_invalid
      expect(pool.errors[:init_price]).to include('must be greater than 0')

      pool = build(:amm_pool, init_price: 1)
      expect(pool).to be_valid
    end

    it 'allows init_price to be nil' do
      pool = build(:amm_pool, init_price: nil)
      expect(pool).to be_valid
    end
  end

  describe 'state machine' do
    it 'initializes with pending status' do
      pool = build(:amm_pool)
      expect(pool.status).to eq('pending')
      expect(pool).to be_pending
    end

    it 'can transition from pending to active' do
      allow_any_instance_of(described_class).to receive(:send_event_create_amm_pool)

      pool = create(:amm_pool, status: 'pending')
      pool.activate!
      expect(pool.status).to eq('active')
    end

    it 'can transition from active to inactive' do
      allow_any_instance_of(described_class).to receive(:send_event_create_amm_pool)

      pool = create(:amm_pool, status: 'active')
      pool.deactivate!
      expect(pool.status).to eq('inactive')
    end

    it 'can transition to failed state' do
      allow_any_instance_of(described_class).to receive(:send_event_create_amm_pool)

      pool = create(:amm_pool, status: 'pending')
      pool.fail!
      expect(pool.status).to eq('failed')
    end
  end

  describe 'after_create callback' do
    let(:service) { instance_double(KafkaService::Services::AmmPool::AmmPoolService) }

    before do
      allow(KafkaService::Services::AmmPool::AmmPoolService).to receive(:new).and_return(service)
      allow(service).to receive(:create)
    end

    it 'sends create event after creation' do
      pool = build(:amm_pool)

      expect(service).to receive(:create) do |args|
        expect(args[:pair]).to eq(pool.pair)
        expect(args[:payload][:operationType]).to eq(KafkaService::Config::OperationTypes::AMM_POOL_CREATE)
        expect(args[:payload][:actionType]).to eq('AmmPool')
        expect(args[:payload][:token0]).to eq(pool.token0.upcase)
        expect(args[:payload][:token1]).to eq(pool.token1.upcase)
      end

      pool.save!
    end

    it 'transitions to failed state when Kafka service raises an error' do
      error_message = 'Kafka connection error'
      allow(service).to receive(:create).and_raise(StandardError.new(error_message))
      allow(Rails.logger).to receive(:error)

      pool = build(:amm_pool)
      pool.save!

      expect(pool).to be_failed
      expect(Rails.logger).to have_received(:error).with("Failed to notify exchange engine: #{error_message}")
    end

    it 'sets status explanation when transitioning to failed state' do
      error_message = 'Kafka connection error'
      allow(service).to receive(:create).and_raise(StandardError.new(error_message))

      pool = build(:amm_pool)
      pool.save!
      pool.reload

      expect(pool.status_explanation).to eq("Failed to notify exchange engine: #{error_message}")
    end
  end

  describe '#send_event_update_amm_pool' do
    let(:service) { instance_double(KafkaService::Services::AmmPool::AmmPoolService) }
    let(:amm_pool) do
      allow_any_instance_of(described_class).to receive(:send_event_create_amm_pool)
      create(:amm_pool, fee_percentage: 0.003, fee_protocol_percentage: 0.05, status: 'pending')
    end

    before do
      allow(KafkaService::Services::AmmPool::AmmPoolService).to receive(:new).and_return(service)
      allow(service).to receive(:update)
    end

    it 'sends update event with valid parameters' do
      params = { fee_percentage: 0.005, fee_protocol_percentage: 0.1, status: 'active' }

      expect(service).to receive(:update) do |args|
        expect(args[:pair]).to eq(amm_pool.pair)
        expect(args[:payload][:operationType]).to eq(KafkaService::Config::OperationTypes::AMM_POOL_UPDATE)
        expect(args[:payload][:actionType]).to eq('AmmPool')
        expect(args[:payload][:actionId]).to eq(amm_pool.id)
        expect(args[:payload][:feePercentage]).to eq(0.005)
        expect(args[:payload][:feeProtocolPercentage]).to eq(0.1)
        expect(args[:payload][:isActive]).to be(true)
      end

      amm_pool.send_event_update_amm_pool(params)
    end

    it 'sends update event with init_price parameter' do
      params = { init_price: 1.5 }

      expect(service).to receive(:update) do |args|
        expect(args[:pair]).to eq(amm_pool.pair)
        expect(args[:payload][:operationType]).to eq(KafkaService::Config::OperationTypes::AMM_POOL_UPDATE)
        expect(args[:payload][:actionType]).to eq('AmmPool')
        expect(args[:payload][:actionId]).to eq(amm_pool.id)
        expect(args[:payload][:initPrice]).to eq(1.5)
      end

      amm_pool.send_event_update_amm_pool(params)
    end

    it 'includes init_price in create event when present' do
      allow_any_instance_of(described_class).to receive(:send_event_create_amm_pool).and_call_original

      pool = build(:amm_pool, init_price: 1.5)

      expect(KafkaService::Services::AmmPool::AmmPoolService).to receive(:new).and_return(service)
      expect(service).to receive(:create) do |args|
        expect(args[:payload][:initPrice]).to eq(1.5)
      end

      pool.save!
    end

    it 'raises error when updating init_price on a pool with liquidity' do
      amm_pool.update!(total_value_locked_token0: 10, total_value_locked_token1: 10)

      expect {
        amm_pool.send_event_update_amm_pool({ init_price: 1.5 })
      }.to raise_error('Cannot modify initPrice on pool with liquidity')
    end

    it 'raises error when updating init_price on an active pool' do
      amm_pool.update!(status: 'active')

      expect {
        amm_pool.send_event_update_amm_pool({ init_price: 1.5 })
      }.to raise_error('Cannot modify initPrice on active pool')
    end

    it 'raises error when updating with non-positive init_price' do
      expect {
        amm_pool.send_event_update_amm_pool({ init_price: 0 })
      }.to raise_error('Initial price must be positive')

      expect {
        amm_pool.send_event_update_amm_pool({ init_price: -1 })
      }.to raise_error('Initial price must be positive')
    end

    it 'raises error when no valid changes are found' do
      params = { unknown_field: 'value' }

      expect {
        amm_pool.send_event_update_amm_pool(params)
      }.to raise_error('No valid changes found in params')
    end

    it 'raises error when params contains same values as current record' do
      params = { fee_percentage: amm_pool.fee_percentage }

      expect {
        amm_pool.send_event_update_amm_pool(params)
      }.to raise_error('No valid changes found in params')
    end

    it 'includes only changed values in the payload' do
      params = { fee_percentage: 0.005, status: 'active' }

      expect(service).to receive(:update) do |args|
        expect(args[:payload][:feePercentage]).to eq(0.005)
        expect(args[:payload][:isActive]).to be(true)
        expect(args[:payload]).not_to have_key(:feeProtocolPercentage)
      end

      amm_pool.send_event_update_amm_pool(params)
    end
  end

  describe '#validate_update_params' do
    let(:amm_pool) do
      allow_any_instance_of(described_class).to receive(:send_event_create_amm_pool)
      create(:amm_pool, fee_percentage: 0.003, fee_protocol_percentage: 0.05, status: 'pending')
    end

    it 'returns false if params contains invalid keys' do
      result = amm_pool.send(:validate_update_params, { invalid_key: 'value' })
      expect(result).to be false
    end

    it 'returns false if no changes are detected' do
      result = amm_pool.send(:validate_update_params, { fee_percentage: 0.003 })
      expect(result).to be false
    end

    it 'returns true when valid changes are detected' do
      result = amm_pool.send(:validate_update_params, { fee_percentage: 0.005 })
      expect(result).to be true
    end

    it 'handles string and symbol keys correctly' do
      # With string keys
      result = amm_pool.send(:validate_update_params, { 'fee_percentage' => 0.005 })
      expect(result).to be true

      # With symbol keys
      result = amm_pool.send(:validate_update_params, { fee_percentage: 0.005 })
      expect(result).to be true
    end
  end

  describe '#apr' do
    it 'returns 0 when pool has no liquidity' do
      pool = create(:amm_pool,
                   total_value_locked_token0: 0,
                   total_value_locked_token1: 0,
                   fee_growth_global0: 10)
      expect(pool.apr).to eq(0)
    end

    it 'calculates APR correctly' do
      pool = create(:amm_pool,
                   total_value_locked_token0: 1000,
                   total_value_locked_token1: 25000000,
                   price: 25000,
                   fee_growth_global0: 10)

      # APR = (total_fees / total_value_locked) * 100
      # total_fees = 10
      # total_value_locked = 1000 + (25000000 / 25000) = 2000
      # APR = (10 / 2000) * 100 = 0.5
      expect(pool.apr).to eq(0.5)
    end
  end

  describe '#tvl_in_token0' do
    it 'calculates TVL in token0 (USDT) correctly' do
      pool = create(:amm_pool,
                   total_value_locked_token0: 1000,
                   total_value_locked_token1: 25000000,
                   price: 25000)

      # TVL = total_value_locked_token0 + (total_value_locked_token1 / price)
      # TVL = 1000 + (25000000 / 25000) = 2000
      expect(pool.tvl_in_token0).to eq(2000.0)
    end
  end

  describe '#tvl_in_token1' do
    it 'calculates TVL in token1 (VND) correctly' do
      pool = create(:amm_pool,
                   total_value_locked_token0: 1000,
                   total_value_locked_token1: 25000000,
                   price: 25000)

      # TVL = (total_value_locked_token0 * price) + total_value_locked_token1
      # TVL = (1000 * 25000) + 25000000 = 50000000
      expect(pool.tvl_in_token1).to eq(50000000.0)
    end
  end
end
