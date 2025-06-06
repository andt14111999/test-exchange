# frozen_string_literal: true

require 'rails_helper'

RSpec.describe BalanceLock, type: :model do
  describe 'associations' do
    it { is_expected.to belong_to(:user) }
    it { is_expected.to have_many(:balance_lock_operations).dependent(:destroy) }
    it { is_expected.to have_many(:coin_transactions).dependent(:destroy) }
  end

  describe 'validations' do
    it { is_expected.to validate_presence_of(:status) }

    context 'when locked' do
      it 'validates presence of locked_at' do
        balance_lock = build(:balance_lock, :locked, locked_at: nil)
        expect(balance_lock).to be_invalid
        expect(balance_lock.errors[:locked_at]).to include("can't be blank")
      end
    end

    context 'when not locked' do
      it 'does not validate presence of locked_balances' do
        balance_lock = build(:balance_lock, :pending, locked_balances: nil)
        expect(balance_lock).to be_valid
      end

      it 'does not validate presence of locked_at' do
        balance_lock = build(:balance_lock, :pending, locked_at: nil)
        expect(balance_lock).to be_valid
      end
    end
  end

  describe 'scopes' do
    context 'locked scope' do
      it 'returns only locked balance locks' do
        locked_balance_lock = create(:balance_lock, :locked)
        create(:balance_lock, :pending)
        create(:balance_lock, :released)

        expect(described_class.locked).to contain_exactly(locked_balance_lock)
      end
    end

    context 'released scope' do
      it 'returns only released balance locks' do
        released_balance_lock = create(:balance_lock, :released)
        create(:balance_lock, :pending)
        create(:balance_lock, :locked)

        expect(described_class.released).to contain_exactly(released_balance_lock)
      end
    end
  end

  describe 'callbacks' do
    it 'sends balance lock event to Kafka after creation with coin and fiat account keys' do
      user = create(:user)
      coin_account = create(:coin_account, user: user)
      fiat_account = create(:fiat_account, user: user)

      kafka_service = instance_double(KafkaService::Services::Coin::BalanceLockService)
      allow(KafkaService::Services::Coin::BalanceLockService).to receive(:new).and_return(kafka_service)

      coin_account_key = KafkaService::Services::AccountKeyBuilderService.build_coin_account_key(
        user_id: user.id,
        account_id: coin_account.id
      )

      fiat_account_key = KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
        user_id: user.id,
        account_id: fiat_account.id
      )

      expect(kafka_service).to receive(:create).with(
        account_keys: [ coin_account_key, fiat_account_key ],
        identifier: kind_of(String)
      )

      create(:balance_lock, user: user)
    end

    it 'sends balance lock event to Kafka with only coin account keys when user has no fiat accounts' do
      user = create(:user)
      coin_account = create(:coin_account, user: user)

      kafka_service = instance_double(KafkaService::Services::Coin::BalanceLockService)
      allow(KafkaService::Services::Coin::BalanceLockService).to receive(:new).and_return(kafka_service)

      coin_account_key = KafkaService::Services::AccountKeyBuilderService.build_coin_account_key(
        user_id: user.id,
        account_id: coin_account.id
      )

      expect(kafka_service).to receive(:create).with(
        account_keys: [ coin_account_key ],
        identifier: kind_of(String)
      )

      create(:balance_lock, user: user)
    end

    it 'sends balance lock event to Kafka with only fiat account keys when user has no coin accounts' do
      user = create(:user)
      fiat_account = create(:fiat_account, user: user)

      kafka_service = instance_double(KafkaService::Services::Coin::BalanceLockService)
      allow(KafkaService::Services::Coin::BalanceLockService).to receive(:new).and_return(kafka_service)

      fiat_account_key = KafkaService::Services::AccountKeyBuilderService.build_fiat_account_key(
        user_id: user.id,
        account_id: fiat_account.id
      )

      expect(kafka_service).to receive(:create).with(
        account_keys: [ fiat_account_key ],
        identifier: kind_of(String)
      )

      create(:balance_lock, user: user)
    end
  end

  describe 'AASM state transitions' do
    context 'mark_as_locked event' do
      it 'transitions from pending to locked' do
        balance_lock = create(:balance_lock, :pending)

        expect {
          balance_lock.mark_as_locked!
        }.to change { balance_lock.status }.from('pending').to('locked')
      end

      it 'sets locked_at timestamp if not already set' do
        balance_lock = create(:balance_lock, :pending, locked_at: nil)

        expect {
          balance_lock.mark_as_locked!
        }.to change { balance_lock.locked_at }.from(nil).to(kind_of(Time))
      end

      it 'creates a lock operation' do
        balance_lock = create(:balance_lock, :pending)

        expect {
          balance_lock.mark_as_locked!
        }.to change { balance_lock.balance_lock_operations.count }.by(1)

        operation = balance_lock.balance_lock_operations.last
        expect(operation.operation_type).to eq('lock')
        expect(operation.status).to eq('completed')
      end
    end

    context 'start_releasing event' do
      it 'transitions from locked to releasing' do
        balance_lock = create(:balance_lock, :locked)

        expect {
          balance_lock.start_releasing!
        }.to change { balance_lock.status }.from('locked').to('releasing')
      end

      it 'sends balance unlock event to Kafka' do
        balance_lock = create(:balance_lock, :locked, engine_lock_id: 'engine-lock-123')

        kafka_service = instance_double(KafkaService::Services::Coin::BalanceLockService)
        allow(KafkaService::Services::Coin::BalanceLockService).to receive(:new).and_return(kafka_service)

        expect(kafka_service).to receive(:unlock).with(
          lock_id: balance_lock.engine_lock_id.to_s,
          identifier: balance_lock.id.to_s
        )

        balance_lock.start_releasing!
      end
    end

    context 'release event' do
      it 'transitions from releasing to released' do
        balance_lock = create(:balance_lock, :releasing)

        expect {
          balance_lock.release!
        }.to change { balance_lock.status }.from('releasing').to('released')
      end

      it 'sets unlocked_at timestamp if not already set' do
        balance_lock = create(:balance_lock, :releasing, unlocked_at: nil)

        expect {
          balance_lock.release!
        }.to change { balance_lock.unlocked_at }.from(nil).to(kind_of(Time))
      end

      it 'creates a release operation' do
        balance_lock = create(:balance_lock, :releasing)

        expect {
          balance_lock.release!
        }.to change { balance_lock.balance_lock_operations.count }.by(1)

        operation = balance_lock.balance_lock_operations.last
        expect(operation.operation_type).to eq('release')
        expect(operation.status).to eq('completed')
      end
    end
  end

  describe '#total_locked_amount_for_coin' do
    it 'returns the locked amount for a specific coin' do
      balance_lock = create(:balance_lock, locked_balances: { 'usdt' => '100.0', 'btc' => '0.001' })

      expect(balance_lock.total_locked_amount_for_coin('usdt')).to eq(100.0)
      expect(balance_lock.total_locked_amount_for_coin('btc')).to eq(0.001)
    end

    it 'returns 0 for coins that are not locked' do
      balance_lock = create(:balance_lock, locked_balances: { 'usdt' => '100.0' })

      expect(balance_lock.total_locked_amount_for_coin('eth')).to eq(0)
    end

    it 'handles string keys' do
      balance_lock = create(:balance_lock, locked_balances: { 'usdt' => '100.0' })

      expect(balance_lock.total_locked_amount_for_coin(:usdt)).to eq(100.0)
    end
  end

  describe '#locked_coins' do
    it 'returns an array of locked coin currencies' do
      balance_lock = create(:balance_lock, locked_balances: { 'usdt' => '100.0', 'btc' => '0.001' })

      expect(balance_lock.locked_coins).to contain_exactly('usdt', 'btc')
    end

    it 'returns an empty array when no coins are locked' do
      balance_lock = create(:balance_lock, locked_balances: {})

      expect(balance_lock.locked_coins).to be_empty
    end
  end

  describe '.ransackable_attributes' do
    it 'returns the expected attributes' do
      expected_attributes = %w[
        id user_id locked_balances status reason
        locked_at unlocked_at created_at updated_at
      ]

      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe '.ransackable_associations' do
    it 'returns the expected associations' do
      expected_associations = %w[user balance_lock_operations coin_transactions]

      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end
end
