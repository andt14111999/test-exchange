# frozen_string_literal: true

require 'rails_helper'

describe AmmPosition, type: :model do
  describe 'validations' do
    it 'validates presence of identifier' do
      position = build(:amm_position, identifier: nil)
      expect(position).to be_invalid
      expect(position.errors[:identifier]).to include("can't be blank")
    end

    it 'validates presence of status' do
      position = build(:amm_position, status: nil)
      expect(position).to be_invalid
      expect(position.errors[:status]).to include("can't be blank")
    end

    it 'validates status is one of the allowed values' do
      position = build(:amm_position, status: 'invalid_status')
      expect(position).to be_invalid
      expect(position.errors[:status]).to include("must be one of: pending, open, closed, error")
    end

    it 'validates liquidity is greater than or equal to 0' do
      position = build(:amm_position, liquidity: -1)
      expect(position).to be_invalid
      expect(position.errors[:liquidity]).to include("must be greater than or equal to 0")
    end

    it 'validates slippage is greater than or equal to 0' do
      position = build(:amm_position, slippage: -1)
      expect(position).to be_invalid
      expect(position.errors[:slippage]).to include("must be greater than or equal to 0")
    end

    it 'validates tick_lower_index is less than tick_upper_index' do
      pool = create(:amm_pool, tick_spacing: 10)
      position = build(:amm_position, amm_pool: pool, tick_lower_index: 100, tick_upper_index: 50)
      expect(position).to be_invalid
      expect(position.errors[:tick_lower_index]).to include("must be less than tick_upper_index")
    end

    it 'validates tick_lower_index is a multiple of tick_spacing' do
      pool = create(:amm_pool, tick_spacing: 10)
      position = build(:amm_position, amm_pool: pool, tick_lower_index: 25, tick_upper_index: 50)
      expect(position).to be_invalid
      expect(position.errors[:tick_lower_index]).to include("must be a multiple of the pool's tick spacing (10)")
    end

    it 'validates tick_upper_index is a multiple of tick_spacing' do
      pool = create(:amm_pool, tick_spacing: 10)
      position = build(:amm_position, amm_pool: pool, tick_lower_index: 20, tick_upper_index: 55)
      expect(position).to be_invalid
      expect(position.errors[:tick_upper_index]).to include("must be a multiple of the pool's tick spacing (10)")
    end

    it 'is valid when both tick indices are multiples of tick_spacing and lower < upper' do
      pool = create(:amm_pool, tick_spacing: 10)
      position = build(:amm_position, amm_pool: pool, tick_lower_index: 20, tick_upper_index: 50)
      expect(position).to be_valid
    end

    it 'validates sufficient account balance for amount0_initial' do
      user = create(:user)
      pool = create(:amm_pool, token0: 'USDT', token1: 'VND')
      account0 = instance_double('CoinAccount', id: 1, available: 50)
      account1 = instance_double('FiatAccount', id: 2, available: 200)
      
      allow(user).to receive(:main_account).with('usdt').and_return(account0)
      allow(user).to receive(:main_account).with('vnd').and_return(account1)
      
      position = build(:amm_position, 
        user: user, 
        amm_pool: pool, 
        amount0_initial: 100, 
        amount1_initial: 100
      )
      
      expect(position).to be_invalid
      expect(position.errors[:amount0_initial]).to include("exceeds available balance in usdt account")
    end

    it 'validates sufficient account balance for amount1_initial' do
      user = create(:user)
      pool = create(:amm_pool, token0: 'USDT', token1: 'VND')
      account0 = instance_double('CoinAccount', id: 1, available: 200)
      account1 = instance_double('FiatAccount', id: 2, available: 50)
      
      allow(user).to receive(:main_account).with('usdt').and_return(account0)
      allow(user).to receive(:main_account).with('vnd').and_return(account1)
      
      position = build(:amm_position, 
        user: user, 
        amm_pool: pool, 
        amount0_initial: 100, 
        amount1_initial: 100
      )
      
      expect(position).to be_invalid
      expect(position.errors[:amount1_initial]).to include("exceeds available balance in vnd account")
    end
  end

  describe 'default values' do
    it 'initializes with pending status' do
      position = build(:amm_position)
      expect(position.status).to eq('pending')
    end

    it 'initializes with default slippage' do
      position = build(:amm_position)
      expect(position.slippage).to eq(1.0) # 100%
    end

    it 'initializes with zero amounts' do
      position = build(:amm_position)
      expect(position.amount0).to eq(0)
      expect(position.amount1).to eq(0)
      expect(position.amount0_initial).to eq(0)
      expect(position.amount1_initial).to eq(0)
      expect(position.tokens_owed0).to eq(0)
      expect(position.tokens_owed1).to eq(0)
      expect(position.fee_collected0).to eq(0)
      expect(position.fee_collected1).to eq(0)
    end
  end

  describe 'associations' do
    it 'belongs to a user' do
      association = described_class.reflect_on_association(:user)
      expect(association.macro).to eq(:belongs_to)
    end

    it 'belongs to an amm_pool' do
      association = described_class.reflect_on_association(:amm_pool)
      expect(association.macro).to eq(:belongs_to)
    end
  end

  describe 'delegations' do
    it 'delegates pair to amm_pool' do
      pool = create(:amm_pool, pair: 'USDT/VND')
      position = build(:amm_position, amm_pool: pool)
      expect(position.pair).to eq('USDT/VND')
    end

    it 'delegates token0 to amm_pool' do
      pool = create(:amm_pool, token0: 'USDT')
      position = build(:amm_position, amm_pool: pool)
      expect(position.token0).to eq('USDT')
    end

    it 'delegates token1 to amm_pool' do
      pool = create(:amm_pool, token1: 'VND')
      position = build(:amm_position, amm_pool: pool)
      expect(position.token1).to eq('VND')
    end

    it 'delegates tick_spacing to amm_pool' do
      pool = create(:amm_pool, tick_spacing: 10)
      position = build(:amm_position, amm_pool: pool)
      expect(position.tick_spacing).to eq(10)
    end
  end

  describe 'scopes' do
    before do
      allow_any_instance_of(AmmPosition).to receive(:send_event_create_amm_position)
    end

    it 'has a pending scope' do
      create_list(:amm_position, 2, status: 'pending')
      create(:amm_position, status: 'open')
      expect(AmmPosition.pending.count).to eq(2)
    end

    it 'has an open scope' do
      create_list(:amm_position, 3, status: 'open')
      create(:amm_position, status: 'pending')
      expect(AmmPosition.open.count).to eq(3)
    end

    it 'has a closed scope' do
      create_list(:amm_position, 1, status: 'closed')
      create(:amm_position, status: 'open')
      expect(AmmPosition.closed.count).to eq(1)
    end

    it 'has an error scope' do
      create_list(:amm_position, 2, status: 'error')
      create(:amm_position, status: 'open')
      expect(AmmPosition.error.count).to eq(2)
    end
  end

  describe 'account_key methods' do
    let(:user) { create(:user) }
    let(:pool) { create(:amm_pool, token0: 'USDT', token1: 'VND') }
    let(:usdt_account) { instance_double('CoinAccount', id: 1) }
    let(:vnd_account) { instance_double('FiatAccount', id: 2) }
    let(:position) { build(:amm_position, user: user, amm_pool: pool) }

    before do
      allow(user).to receive(:main_account).with('usdt').and_return(usdt_account)
      allow(user).to receive(:main_account).with('vnd').and_return(vnd_account)
    end

    it 'returns account_key0 from user main account' do
      expect(position.account_key0).to eq('1')
    end

    it 'returns account_key1 from user main account' do
      expect(position.account_key1).to eq('2')
    end
  end

  describe '#generate_identifier' do
    let(:user) { create(:user, id: 123) }
    let(:pool) { create(:amm_pool, pair: 'USDT/VND') }
    let(:position) { build(:amm_position, user: user, amm_pool: pool) }

    it 'generates an identifier based on user_id, pool_pair, and timestamp' do
      allow(Time).to receive(:now).and_return(Time.at(1650000000))
      position.generate_identifier
      expect(position.identifier).to eq("amm_position_123_usdt/vnd_1650000000")
    end
  end

  describe '#collect_fee' do
    let(:position) { create(:amm_position, status: 'open') }
    let(:service) { instance_double(KafkaService::Services::AmmPosition::AmmPositionService) }

    before do
      allow(KafkaService::Services::AmmPosition::AmmPositionService).to receive(:new).with(position).and_return(service)
      allow(service).to receive(:collect_fee)
    end

    it 'calls the service to collect fee' do
      expect(service).to receive(:collect_fee)
      position.collect_fee
    end

    it 'raises an error if position is not open' do
      position.update(status: 'pending')
      expect { position.collect_fee }.to raise_error(StandardError, 'Cannot collect fee for a position that is not open')
    end
  end

  describe '#close_position' do
    let(:position) { create(:amm_position, status: 'open') }
    let(:service) { instance_double(KafkaService::Services::AmmPosition::AmmPositionService) }

    before do
      allow(KafkaService::Services::AmmPosition::AmmPositionService).to receive(:new).with(position).and_return(service)
      allow(service).to receive(:close)
    end

    it 'calls the service to close the position' do
      expect(service).to receive(:close)
      position.close_position
    end

    it 'raises an error if position is not open' do
      position.update(status: 'pending')
      expect { position.close_position }.to raise_error(StandardError, 'Cannot close a position that is not open')
    end
  end
end
