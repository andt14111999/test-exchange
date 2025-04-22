# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinWithdrawalOperation, type: :model do
  describe 'associations' do
    it { is_expected.to have_many(:coin_transactions).dependent(:destroy) }
    it { is_expected.to belong_to(:coin_withdrawal).required(true) }

    it 'belongs to coin_withdrawal' do
      user = create(:user)
      withdrawal = create(:coin_withdrawal, user: user)
      operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
      expect(operation.coin_withdrawal).to eq(withdrawal)
    end
  end

  describe 'delegations' do
    it { is_expected.to delegate_method(:user).to(:coin_withdrawal) }
    it { is_expected.to delegate_method(:record_tx_hash_arrived_at).to(:coin_withdrawal) }
    it { is_expected.to delegate_method(:coin_address).to(:coin_withdrawal) }
  end

  describe 'callbacks' do
    describe 'after_initialize' do
      it 'sets coin_amount and coin_fee from coin_withdrawal for new records' do
        user = create(:user)
        withdrawal = create(:coin_withdrawal, user: user)
        operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        expect(operation.coin_amount).to eq(1.0)
        expect(operation.coin_fee).to eq(0.1)
      end
    end

    describe 'before_validation' do
      it 'sets coin_currency from coin_withdrawal on create' do
        user = create(:user)
        withdrawal = create(:coin_withdrawal, user: user, coin_currency: 'usdt')
        operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        operation.valid?
        expect(operation.coin_currency).to eq('usdt')
      end
    end

    describe 'after_create' do
      it 'creates coin transaction' do
        user = create(:user)
        withdrawal = create(:coin_withdrawal, user: user)
        operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        expect { operation.save! }.to change(CoinTransaction, :count).by(1)
        transaction = CoinTransaction.last
        expect(transaction.amount).to eq(-1.1) # coin_amount + coin_fee
      end
    end

    describe 'before_update' do
      it 'records tx_hash_arrived_at when tx_hash changes' do
        user = create(:user)
        withdrawal = create(:coin_withdrawal, user: user)
        operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        operation.update!(tx_hash: 'new_hash')
        expect(withdrawal.reload.tx_hash_arrived_at).to be_present
      end
    end
  end

  describe 'validations' do
    let(:user) { create(:user) }
    let(:account) { create(:coin_account, :main, user: user, balance: 10.0) }
    let(:withdrawal) { create(:coin_withdrawal, user: user) }

    it 'validates coin_amount is positive' do
      operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal, coin_amount: -1)
      expect(operation).to be_invalid
      expect(operation.errors[:coin_amount]).to include('must be greater than 0')
    end

    it 'validates coin_fee is not negative' do
      operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal, coin_fee: -1)
      expect(operation).to be_invalid
      expect(operation.errors[:coin_fee]).to include('must be greater than or equal to 0')
    end
  end

  describe 'state machine' do
    let(:user) { create(:user) }
    let(:withdrawal) { create(:coin_withdrawal, user: user) }
    let(:operation) { build(:coin_withdrawal_operation, coin_withdrawal: withdrawal) }

    describe 'transitions' do
      it 'starts in pending state' do
        operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        expect(operation.status).to eq('pending')
      end

      it 'transitions from pending to relaying' do
        operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        operation.start_relaying!
        expect(operation.status).to eq('relaying')
      end

      it 'transitions from relaying to relay_crashed' do
        operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        operation.start_relaying!
        operation.crash!
        expect(operation.status).to eq('relay_crashed')
      end

      it 'transitions from relaying to relay_failed' do
        operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        operation.start_relaying!
        operation.fail!
        expect(operation.status).to eq('relay_failed')
      end

      it 'transitions from relaying to processed' do
        operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        operation.start_relaying!
        operation.withdrawal_status = 'processed'
        operation.tx_hash = 'tx_123'
        operation.relay!
        expect(operation.status).to eq('processed')
      end

      it 'sets status_explanation when withdrawal fails' do
        operation.status = 'relaying'
        allow(operation).to receive(:process_withdrawal!)
        operation.withdrawal_data = { 'status' => 'failed' }
        expect_any_instance_of(PostbackService).to receive(:post).and_return(
          OpenStruct.new(code: 422, body: { 'payment_id' => [ "can't be blank" ] }.as_json)
        )
        operation.relay_now
        expect(operation.status_explanation).to eq("{\"payment_id\"=>[\"can't be blank\"]}")
        expect(operation.status).to eq('relay_failed')
      end
    end

    describe 'after_enter callbacks' do
      it 'marks withdrawal as processing when starting to relay' do
        user = create(:user)
        withdrawal = create(:coin_withdrawal, user: user)
        operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
        operation.start_relaying!
        expect(withdrawal.reload.status).to eq('processing')
      end
    end
  end

  describe '#relay_later' do
    it 'starts relaying and calls relay_now' do
      user = create(:user)
      withdrawal = create(:coin_withdrawal, user: user)
      operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
      operation.status = 'pending'
      allow(operation).to receive(:relay_now)
      operation.relay_later
      expect(operation.status).to eq('relaying')
      expect(operation).to have_received(:relay_now)
    end
  end

  describe '#relay_now' do
    let(:user) { create(:user) }
    let(:account) { create(:coin_account, :main, user: user, balance: 10.0) }
    let(:withdrawal) { create(:coin_withdrawal, user: user) }
    let(:operation) { build(:coin_withdrawal_operation, coin_withdrawal: withdrawal) }

    before do
      operation.status = 'relaying'
    end

    it 'processes withdrawal and transitions to processed when successful' do
      expect_any_instance_of(PostbackService).to receive(:post).and_return(
        OpenStruct.new(code: 200, body: { 'status' => 'processed', 'tx_hash' => 'tx_123' })
      )
      operation.relay_now
      expect(operation.status).to eq('processed')
      expect(operation.withdrawal_status).to eq('processed')
      expect(operation.tx_hash).to be_present
    end

    it 'transitions to relay_failed when withdrawal fails' do
      expect_any_instance_of(PostbackService).to receive(:post).and_return(
        OpenStruct.new(code: 422, body: { 'payment_id' => [ "can't be blank" ] }.as_json)
      )
      operation.relay_now
      expect(operation.status).to eq('relay_failed')
    end
  end

  describe '#required_coin_amount' do
    it 'returns sum of coin_amount and coin_fee' do
      user = create(:user)
      withdrawal = create(:coin_withdrawal, user: user)
      operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
      expect(operation.required_coin_amount).to eq(1.1)
    end
  end

  describe '#withdrawal_status_processed?' do
    let(:user) { create(:user) }
    let(:account) { create(:coin_account, :main, user: user, balance: 10.0) }
    let(:withdrawal) { create(:coin_withdrawal, user: user) }

    it 'returns true when withdrawal_status is processed' do
      operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal, withdrawal_status: 'processed')
      expect(operation.withdrawal_status_processed?).to be true
    end

    it 'returns false when withdrawal_status is not processed' do
      operation = build(:coin_withdrawal_operation, coin_withdrawal: withdrawal, withdrawal_status: 'pending')
      expect(operation.withdrawal_status_processed?).to be false
    end
  end

  describe '#should_mark_withdrawal_release_succeed?' do
    let(:user) { create(:user) }
    let(:account) { create(:coin_account, :main, user: user, balance: 10.0) }
    let(:withdrawal) { create(:coin_withdrawal, user: user) }
    let(:operation) { create(:coin_withdrawal_operation, coin_withdrawal: withdrawal) }

    it 'returns true when withdrawal is processed and tx_hash changes' do
      operation.update!(withdrawal_status: 'processed', tx_hash: 'new_hash')
      expect(operation.send(:should_mark_withdrawal_release_succeed?)).to be true
    end

    it 'returns false when withdrawal is not processed' do
      operation.update!(withdrawal_status: 'pending', tx_hash: 'new_hash')
      expect(operation.send(:should_mark_withdrawal_release_succeed?)).to be false
    end

    it 'returns false when tx_hash is not present' do
      operation.update!(withdrawal_status: 'processed', tx_hash: nil)
      expect(operation.send(:should_mark_withdrawal_release_succeed?)).to be false
    end
  end

  describe '#mark_withdrawal_release_succeed' do
    let(:user) { create(:user) }
    let(:withdrawal) { create(:coin_withdrawal, user: user) }
    let(:operation) { create(:coin_withdrawal_operation, coin_withdrawal: withdrawal) }

    it 'does not raise error when withdrawal can be completed' do
      operation.start_relaying!
      operation.withdrawal_status = 'processed'
      operation.tx_hash = 'tx_123'
      expect { operation.relay! }.not_to raise_error
      expect(withdrawal.reload.status).to eq('completed')
      expect(withdrawal.tx_hash).to eq('tx_123')
    end

    it 'logs error when complete! raises an error' do
      operation.start_relaying!
      operation.withdrawal_status = 'processed'
      operation.tx_hash = 'tx_123'
      allow(withdrawal).to receive(:complete!).and_raise(StandardError, 'test error')
      allow(Rails.logger).to receive(:error)
      operation.send(:mark_withdrawal_release_succeed)
      expect(Rails.logger).to have_received(:error).with(/CoinWithdrawalOperation#\d+ mark_withdrawal_release_succeed error: test error/)
    end
  end

  describe '#mark_withdrawal_release_processed' do
    let(:user) { create(:user) }
    let(:withdrawal) { create(:coin_withdrawal, user: user) }
    let(:operation) { create(:coin_withdrawal_operation, coin_withdrawal: withdrawal) }

    it 'does not raise error when withdrawal cannot be processed' do
      operation.start_relaying!
      allow(withdrawal).to receive(:may_process?).and_return(false)
      expect { operation.send(:mark_withdrawal_release_processed) }.not_to raise_error
    end

    it 'logs error when process! raises an error' do
      operation.start_relaying!
      allow(withdrawal).to receive(:may_process?).and_return(true)
      allow(withdrawal).to receive(:process!).and_raise(StandardError, 'test error')
      allow(Rails.logger).to receive(:error)
      operation.send(:mark_withdrawal_release_processed)
      expect(Rails.logger).to have_received(:error).with("CoinWithdrawalOperation##{operation.id} mark_withdrawal_release_processed error: test error")
    end
  end

  describe 'ransackable attributes and associations' do
    it 'returns correct ransackable attributes' do
      expect(described_class.ransackable_attributes(nil)).to include(
        'id', 'coin_withdrawal_id', 'coin_amount', 'coin_fee', 'coin_currency',
        'status', 'status_explanation', 'withdrawal_status', 'tx_hash',
        'tx_hash_arrived_at', 'scheduled_at', 'withdrawal_data', 'created_at', 'updated_at'
      )
    end

    it 'returns correct ransackable associations' do
      expect(described_class.ransackable_associations(nil)).to include('coin_withdrawal', 'coin_transactions')
    end
  end
end
