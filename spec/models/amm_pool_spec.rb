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
end
