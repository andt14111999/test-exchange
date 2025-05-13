# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Tick, type: :model do
  describe 'associations' do
    it { is_expected.to belong_to(:amm_pool) }
  end

  describe 'validations' do
    it { is_expected.to validate_presence_of(:pool_pair) }
    it { is_expected.to validate_presence_of(:tick_index) }
    it { is_expected.to validate_presence_of(:tick_key) }

    describe 'uniqueness of tick_key' do
      subject { build(:tick) }

      it { is_expected.to validate_uniqueness_of(:tick_key) }
    end
  end

  describe 'scopes' do
    let!(:active_tick) { create(:tick, :active) }
    let!(:inactive_tick) { create(:tick, :inactive) }

    it 'returns active ticks' do
      expect(described_class.active).to include(active_tick)
      expect(described_class.active).not_to include(inactive_tick)
    end

    it 'returns inactive ticks' do
      expect(described_class.inactive).to include(inactive_tick)
      expect(described_class.inactive).not_to include(active_tick)
    end
  end

  describe 'callbacks' do
    describe '#generate_tick_key' do
      it 'generates tick_key before validation on create' do
        tick = build(:tick, pool_pair: 'USDT/VND', tick_index: 100, tick_key: nil)
        tick.valid?
        expect(tick.tick_key).to eq('USDT/VND-100')
      end

      it 'does not override existing tick_key' do
        tick = build(:tick, pool_pair: 'USDT/VND', tick_index: 100, tick_key: 'CUSTOM-KEY')
        tick.valid?
        expect(tick.tick_key).to eq('CUSTOM-KEY')
      end
    end

    describe '#update_status_based_on_liquidity' do
      it 'activates tick when liquidity_net is not zero' do
        tick = build(:tick, liquidity_net: 50, status: 'inactive')
        expect(tick).to receive(:activate!).and_call_original
        tick.save
        expect(tick.status).to eq('active')
      end

      it 'deactivates tick when liquidity_net is zero' do
        tick = build(:tick, liquidity_net: 0, status: 'active')
        expect(tick).to receive(:deactivate!).and_call_original
        tick.save
        expect(tick.status).to eq('inactive')
      end
    end
  end

  describe '#send_tick_query' do
    let(:tick) { create(:tick) }
    let(:tick_service) { instance_double(KafkaService::Services::Tick::TickService) }

    before do
      allow(KafkaService::Services::Tick::TickService).to receive(:new).and_return(tick_service)
      allow(tick_service).to receive(:query)
    end

    it 'sends a query to the tick service' do
      expect(tick_service).to receive(:query).with(
        pool_pair: tick.pool_pair,
        payload: hash_including(
          operationType: KafkaService::Config::OperationTypes::TICK_QUERY,
          poolPair: tick.pool_pair
        )
      )

      tick.send_tick_query
    end
  end
end
