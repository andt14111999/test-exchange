# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinInternalTransferOperation, type: :model do
  describe 'associations' do
    it { is_expected.to belong_to(:coin_withdrawal) }
    it { is_expected.to belong_to(:sender).class_name('User') }
    it { is_expected.to belong_to(:receiver).class_name('User') }
    it { is_expected.to have_many(:coin_transactions) }
  end

  describe 'validations' do
    it { is_expected.to validate_presence_of(:coin_currency) }
    it { is_expected.to validate_presence_of(:coin_amount) }
    it { is_expected.to validate_presence_of(:status) }
    it { is_expected.to validate_numericality_of(:coin_amount).is_greater_than(0) }
  end

  describe 'callbacks' do
    it 'auto-processes after creation' do
      user = create(:user)
      withdrawal = create(:coin_withdrawal, user: user)

      # Create a new transfer and expect auto_process! to be called
      transfer = build(:coin_internal_transfer_operation,
        coin_withdrawal: withdrawal,
        sender: user,
        receiver: user,
        coin_currency: 'usdt',
        coin_amount: 10.0
      )

      # To avoid actual transaction creation in the test
      allow(transfer).to receive(:create_coin_transactions).and_return(true)

      # Expect auto_process! to be called during creation
      expect(transfer).to receive(:auto_process!)

      # Create the transfer
      transfer.save!
    end
  end

  describe 'AASM states' do
    subject(:internal_transfer) do
      described_class.new(
        coin_withdrawal: withdrawal,
        sender: user,
        receiver: user,
        coin_currency: 'usdt',
        coin_amount: 10.0,
        status: 'pending'
      )
    end

    let(:user) { create(:user) }
    let(:withdrawal) { create(:coin_withdrawal, user: user) }


    before do
      # Skip callbacks to avoid validation issues during testing
      allow_any_instance_of(described_class).to receive(:create_coin_transactions).and_return(true)
      allow_any_instance_of(described_class).to receive(:set_default_values).and_return(true)
      allow_any_instance_of(described_class).to receive(:set_coin_currency).and_return(true)
      allow_any_instance_of(described_class).to receive(:auto_process!).and_return(true)
    end

    it 'has a default state of pending' do
      expect(internal_transfer.status).to eq 'pending'
      expect(internal_transfer).to be_pending
    end

    it 'can transition from pending to processing' do
      expect(internal_transfer).to be_pending
      expect(internal_transfer.may_process?).to be true

      internal_transfer.process!
      expect(internal_transfer).to be_processing
    end

    it 'can transition from processing to completed' do
      internal_transfer.status = 'processing'
      expect(internal_transfer).to be_processing
      expect(internal_transfer.may_complete?).to be true

      internal_transfer.complete!
      expect(internal_transfer).to be_completed
    end

    it 'can transition from pending to rejected' do
      expect(internal_transfer).to be_pending
      expect(internal_transfer.may_reject?).to be true

      internal_transfer.reject!
      expect(internal_transfer).to be_rejected
    end

    it 'can transition from processing to rejected' do
      internal_transfer.status = 'processing'
      expect(internal_transfer).to be_processing
      expect(internal_transfer.may_reject?).to be true

      internal_transfer.reject!
      expect(internal_transfer).to be_rejected
    end

    it 'can transition from pending to canceled' do
      expect(internal_transfer).to be_pending
      expect(internal_transfer.may_cancel?).to be true

      internal_transfer.cancel!
      expect(internal_transfer).to be_canceled
    end
  end

  describe '#auto_process!' do
    let(:sender) { create(:user) }
    let(:receiver) { create(:user) }
    let(:withdrawal) { create(:coin_withdrawal, user: sender) }

    let(:internal_transfer) do
      create(:coin_internal_transfer_operation,
        coin_withdrawal: withdrawal,
        sender: sender,
        receiver: receiver,
        coin_currency: 'usdt',
        coin_amount: 10.0,
        status: 'pending'
      )
    end

    before do
      # Skip actual coin transactions for this test
      allow(internal_transfer).to receive_messages(create_coin_transactions: true, auto_process!: true)

      # Mock withdrawal state transitions
      allow(withdrawal).to receive_messages(may_process?: true, process!: true, may_complete?: true, complete!: true)
    end

    it 'successfully processes the internal transfer' do
      # Remove auto_process! stub for this test to test the actual method
      allow(internal_transfer).to receive(:auto_process!).and_call_original

      result = internal_transfer.auto_process!

      expect(result).to be true
      expect(internal_transfer.reload.status).to eq 'completed'
    end

    it 'handles errors gracefully' do
      # Remove auto_process! stub for this test to test the actual method
      allow(internal_transfer).to receive(:auto_process!).and_call_original

      # Simulate an error during processing
      allow(internal_transfer).to receive(:process!).and_raise(StandardError.new('Test error'))

      result = internal_transfer.auto_process!

      expect(result).to be false
      expect(internal_transfer.reload.status).to eq 'rejected'
      expect(internal_transfer.status_explanation).to eq 'Test error'
    end
  end
end
