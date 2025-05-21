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
      expect(position.errors[:tick_lower_index]).to include("must be less than 50")
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

      # Tạo coin account với balance thấp cho usdt
      create(:coin_account, :main, user: user, coin_currency: 'usdt', balance: 50, frozen_balance: 0)
      # Tạo fiat account với balance đủ cho vnd
      create(:fiat_account, user: user, currency: 'VND', balance: 200, frozen_balance: 0)

      position = build(:amm_position,
        user: user,
        amm_pool: pool,
        amount0_initial: 100,
        amount1_initial: 100
      )

      # Chỉ validate trong context :create vì validation chỉ chạy trong context đó
      expect(position.valid?(:create)).to be false
      expect(position.errors[:amount0_initial]).to include("exceeds available balance in USDT account")
    end

    it 'validates sufficient account balance for amount1_initial' do
      user = create(:user)
      pool = create(:amm_pool, token0: 'USDT', token1: 'VND')

      # Tạo coin account với balance đủ cho usdt
      create(:coin_account, :main, user: user, coin_currency: 'usdt', balance: 200, frozen_balance: 0)
      # Tạo fiat account với balance thấp cho vnd
      create(:fiat_account, user: user, currency: 'VND', balance: 50, frozen_balance: 0)

      position = build(:amm_position,
        user: user,
        amm_pool: pool,
        amount0_initial: 100,
        amount1_initial: 100
      )

      # Chỉ validate trong context :create vì validation chỉ chạy trong context đó
      expect(position.valid?(:create)).to be false
      expect(position.errors[:amount1_initial]).to include("exceeds available balance in VND account")
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
      allow_any_instance_of(described_class).to receive(:send_event_create_amm_position)
    end

    it 'has a pending scope' do
      create_list(:amm_position, 2, status: 'pending')
      create(:amm_position, status: 'open')
      expect(described_class.pending.count).to eq(2)
    end

    it 'has an open scope' do
      create_list(:amm_position, 3, status: 'open')
      create(:amm_position, status: 'pending')
      expect(described_class.open.count).to eq(3)
    end

    it 'has a closed scope' do
      create_list(:amm_position, 1, status: 'closed')
      create(:amm_position, status: 'open')
      expect(described_class.closed.count).to eq(1)
    end

    it 'has an error scope' do
      create_list(:amm_position, 2, status: 'error')
      create(:amm_position, status: 'open')
      expect(described_class.error.count).to eq(2)
    end
  end

  describe 'account_key methods' do
    let(:user) { create(:user) }
    let(:pool) { create(:amm_pool, token0: 'USDT', token1: 'VND') }
    let(:position) { build(:amm_position, user: user, amm_pool: pool) }
    let(:usdt_account) { instance_double(CoinAccount, id: 1, account_key: '1') }
    let(:vnd_account) { instance_double(FiatAccount, id: 2, account_key: '2') }

    before do
      allow(user).to receive(:main_account).with('usdt').and_return(usdt_account)
      allow(user).to receive(:main_account).with('vnd').and_return(vnd_account)
      allow(user).to receive(:main_account).with('USDT').and_return(usdt_account)
      allow(user).to receive(:main_account).with('VND').and_return(vnd_account)
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
      allow(KafkaService::Services::AmmPosition::AmmPositionService).to receive(:new).and_return(service)
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
      allow(KafkaService::Services::AmmPosition::AmmPositionService).to receive(:new).and_return(service)
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

  describe '#calculate_est_fee' do
    let(:user) { create(:user) }
    let(:amm_pool) { create(:amm_pool, price: 25000, fee_percentage: 0.003) }

    context 'when position is not open' do
      let(:position) { create(:amm_position, user: user, amm_pool: amm_pool, status: 'pending') }

      it 'returns nil without updating fields' do
        expect(position.calculate_est_fee).to be_nil
        expect(position.estimate_fee_token0).to eq(0)
        expect(position.estimate_fee_token1).to eq(0)
        expect(position.apr).to eq(0)
      end
    end

    context 'when position is open' do
      let(:created_time) { 7.days.ago }
      let(:position) do
        create(:amm_position,
          user: user,
          amm_pool: amm_pool,
          status: 'open',
          liquidity: 1000,
          amount0: 100,
          amount1: 2500000,
          tokens_owed0: 0.5,
          tokens_owed1: 12500,
          fee_collected0: 0.3,
          fee_collected1: 7500,
          fee_growth_inside0_last: 0.1,
          fee_growth_inside1_last: 0.2,
          created_at: created_time
        )
      end

      before do
        # Freeze time to make calculations predictable
        allow(Time).to receive(:now).and_return(created_time + 7.days)

        # Mock the pool fee growth values
        allow(amm_pool).to receive(:fee_growth_global0).and_return(0.3)
        allow(amm_pool).to receive(:fee_growth_global1).and_return(0.5)
      end

      it 'calculates estimated fees and APR' do
        position.calculate_est_fee

        # Verify the calculations
        expect(position.estimate_fee_token0).to be > 0
        expect(position.estimate_fee_token1).to be > 0
        expect(position.apr).to be > 0

        # Verify the fee calculation based on Uniswap V3 formula
        # fee_earned0 = liquidity * (fee_growth_inside0 - fee_growth_inside0_last)
        # = 1000 * (0.3 - 0.1) = 200
        # Plus tokens_owed0 (0.5) and fee_collected0 (0.3) = 200.8
        # Daily rate = 200.8 / 7 ≈ 28.69
        expect(position.estimate_fee_token0).to be_within(0.1).of(28.69)

        # fee_earned1 = liquidity * (fee_growth_inside1 - fee_growth_inside1_last)
        # = 1000 * (0.5 - 0.2) = 300
        # Plus tokens_owed1 (12500) and fee_collected1 (7500) = 20300
        # Daily rate = 20300 / 7 ≈ 2900
        expect(position.estimate_fee_token1).to be_within(1).of(2900)

        # Verify APR calculation is reasonable
        # We expect it to be based on the fees earned and TVL
        # With our test data, APR will be capped at 1000%
        expect(position.apr).to be > 0
        expect(position.apr).to be <= 1000 # APR should be capped at 1000%
      end

      context 'when position has zero TVL' do
        let(:position) do
          create(:amm_position,
            user: user,
            amm_pool: amm_pool,
            status: 'open',
            amount0: 0,
            amount1: 0,
            created_at: created_time
          )
        end

        it 'returns nil without updating fields' do
          expect(position.calculate_est_fee).to be_nil
          expect(position.estimate_fee_token0).to eq(0)
          expect(position.estimate_fee_token1).to eq(0)
          expect(position.apr).to eq(0)
        end
      end

      context 'when position was just created' do
        let(:position) do
          create(:amm_position,
            user: user,
            amm_pool: amm_pool,
            status: 'open',
            amount0: 100,
            amount1: 2500000,
            created_at: Time.now
          )
        end

        it 'returns nil without updating fields' do
          expect(position.calculate_est_fee).to be_nil
          expect(position.estimate_fee_token0).to eq(0)
          expect(position.estimate_fee_token1).to eq(0)
          expect(position.apr).to eq(0)
        end
      end
    end
  end

  describe '.generate_account_keys' do
    let(:user) { create(:user) }
    let(:pool) { create(:amm_pool, token0: 'USDT', token1: 'VND') }
    let(:usdt_account) { instance_double(CoinAccount, id: 1) }
    let(:vnd_account) { instance_double(FiatAccount, id: 2) }

    before do
      allow(user).to receive(:main_account).with('usdt').and_return(usdt_account)
      allow(user).to receive(:main_account).with('vnd').and_return(vnd_account)
    end

    it 'returns array of account keys' do
      result = described_class.generate_account_keys(user, pool)
      expect(result).to eq([ '1', '2' ])
    end

    it 'returns nil if any account is missing' do
      allow(user).to receive(:main_account).with('usdt').and_return(nil)

      result = described_class.generate_account_keys(user, pool)
      expect(result).to be_nil
    end
  end

  describe '.generate_identifier' do
    it 'generates identifier with given parameters' do
      user_id = 123
      pool_pair = 'USDT/VND'
      timestamp = 1650000000

      identifier = described_class.generate_identifier(user_id, pool_pair, timestamp)
      expect(identifier).to eq('amm_position_123_usdt/vnd_1650000000')
    end

    it 'uses current timestamp by default' do
      user_id = 123
      pool_pair = 'USDT/VND'
      current_time = Time.at(1650000000)

      allow(Time).to receive(:now).and_return(current_time)

      identifier = described_class.generate_identifier(user_id, pool_pair)
      expect(identifier).to eq('amm_position_123_usdt/vnd_1650000000')
    end

    it 'downcases the pool pair' do
      identifier = described_class.generate_identifier(123, 'USDT/VND', 1650000000)
      expect(identifier).to eq('amm_position_123_usdt/vnd_1650000000')
    end
  end

  describe 'status constants' do
    it 'defines the correct status constants' do
      expect(AmmPosition::STATUS_PENDING).to eq('pending')
      expect(AmmPosition::STATUS_OPEN).to eq('open')
      expect(AmmPosition::STATUS_CLOSED).to eq('closed')
      expect(AmmPosition::STATUS_ERROR).to eq('error')
    end
  end

  describe 'AASM state transitions' do
    let(:position) { create(:amm_position, status: 'pending') }

    describe '#open_position' do
      it 'transitions from pending to open' do
        expect(position.status).to eq('pending')

        position.open_position

        expect(position.status).to eq('open')
      end

      it 'raises error when transitioning from non-pending state' do
        position.update(status: 'closed')

        expect { position.open_position }.to raise_error(AASM::InvalidTransition)
      end
    end

    describe '#close' do
      it 'transitions from open to closed' do
        position.update(status: 'open')

        position.close

        expect(position.status).to eq('closed')
      end

      it 'raises error when transitioning from non-open state' do
        expect { position.close }.to raise_error(AASM::InvalidTransition)
      end
    end

    describe '#fail' do
      it 'transitions from pending to error' do
        expect(position.status).to eq('pending')

        position.fail('Test error message')

        expect(position.status).to eq('error')
      end

      it 'transitions from open to error' do
        position.update(status: 'open')

        position.fail('Test error message')

        expect(position.status).to eq('error')
      end

      it 'sets error_message during transition' do
        position.fail('Test error message')

        expect(position.error_message).to eq('Test error message')
      end

      it 'raises error when transitioning from closed state' do
        position.update(status: 'closed')

        expect { position.fail('Test error') }.to raise_error(AASM::InvalidTransition)
      end
    end
  end

  describe 'after_create callback' do
    it 'calls send_event_create_amm_position after creation' do
      position = build(:amm_position)

      expect(position).to receive(:send_event_create_amm_position)

      position.save
    end
  end

  describe '#send_event_create_amm_position' do
    let(:user) { create(:user) }
    let(:pool) { create(:amm_pool, token0: 'USDT', token1: 'VND', pair: 'USDT/VND') }
    let(:position) do
      create(:amm_position,
        user: user,
        amm_pool: pool,
        identifier: 'test_position_123',
        tick_lower_index: -100,
        tick_upper_index: 100,
        amount0_initial: 50,
        amount1_initial: 100,
        slippage: 0.5
      )
    end
    let(:service) { instance_double(KafkaService::Services::AmmPosition::AmmPositionService) }

    before do
      allow(position).to receive_messages(
        account_key0: '7216-coin-',
        account_key1: 'account_key_1',
        pool_pair: 'USDT/VND'
      )
      allow(KafkaService::Services::AmmPosition::AmmPositionService).to receive(:new).and_return(service)
      allow(service).to receive(:create)
      allow(SecureRandom).to receive(:uuid).and_return('test-uuid')
    end

    it 'returns early if position is not pending' do
      position.update(status: 'open')

      expect(service).not_to receive(:create)

      position.send(:send_event_create_amm_position)
    end

    it 'calls KafkaService with correct parameters when pending' do
      position.update(status: 'pending')

      expected_payload = {
        eventId: 'amm-position-test-uuid',
        operationType: 'amm_position_create',
        actionType: 'AmmPosition',
        actionId: position.id,
        identifier: 'test_position_123',
        poolPair: 'USDT/VND',
        ownerAccountKey0: '7216-coin-',
        ownerAccountKey1: 'account_key_1',
        tickLowerIndex: -100,
        tickUpperIndex: 100,
        amount0Initial: 50.0,
        amount1Initial: 100.0,
        slippage: 0.5e0
      }

      expect(service).to receive(:create).with(
        identifier: 'test_position_123',
        payload: expected_payload
      )

      position.send(:send_event_create_amm_position)
    end

    it 'logs error and calls fail when exception occurs' do
      position.update(status: 'pending')

      error = StandardError.new('Test error message')
      allow(service).to receive(:create).and_raise(error)

      expect(Rails.logger).to receive(:error).with('Failed to notify exchange engine about AmmPosition creation: Test error message')
      expect(position).to receive(:fail).with('Test error message')

      position.send(:send_event_create_amm_position)
    end

    it 'creates compact payload without nil values' do
      position.update(status: 'pending')
      allow(position).to receive(:account_key0).and_return(nil)

      expected_payload = hash_including(
        eventId: 'amm-position-test-uuid',
        operationType: 'amm_position_create'
      )

      expect(service).to receive(:create).with(
        identifier: 'test_position_123',
        payload: expected_payload
      )

      position.send(:send_event_create_amm_position)
    end
  end
end
