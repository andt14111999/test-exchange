# frozen_string_literal: true

require 'rails_helper'

describe FiatTransaction, type: :model do
  describe 'associations' do
    it 'belongs to fiat_account' do
      expect(described_class.new).to belong_to(:fiat_account)
    end

    it 'belongs to operation as polymorphic' do
      expect(described_class.new).to belong_to(:operation).optional
    end

    it 'delegates user to fiat_account' do
      fiat_transaction = create(:fiat_transaction)
      expect(fiat_transaction.user).to eq(fiat_transaction.fiat_account.user)
    end
  end

  describe 'validations' do
    it 'validates presence of amount' do
      fiat_transaction = build(:fiat_transaction, amount: nil)
      expect(fiat_transaction).to be_invalid
      expect(fiat_transaction.errors[:amount]).to include("can't be blank")
    end

    it 'validates amount is other than 0' do
      fiat_transaction = build(:fiat_transaction, amount: 0)
      expect(fiat_transaction).to be_invalid
      expect(fiat_transaction.errors[:amount]).to include('must be other than 0')
    end

    it 'validates presence of transaction_type' do
      fiat_transaction = build(:fiat_transaction, transaction_type: nil)
      expect(fiat_transaction).to be_invalid
      expect(fiat_transaction.errors[:transaction_type]).to include("can't be blank")
    end

    it 'validates inclusion of transaction_type in TRANSACTION_TYPES' do
      fiat_transaction = build(:fiat_transaction, transaction_type: 'invalid')
      expect(fiat_transaction).to be_invalid
      expect(fiat_transaction.errors[:transaction_type]).to include('is not included in the list')
    end

    it 'validates presence of currency' do
      fiat_transaction = described_class.new(amount: 100, transaction_type: 'mint')
      expect(fiat_transaction).to be_invalid
      expect(fiat_transaction.errors[:currency]).to include("can't be blank")
    end

    it 'validates inclusion of status in STATUSES' do
      fiat_transaction = build(:fiat_transaction, status: 'invalid')
      expect(fiat_transaction).to be_invalid
      expect(fiat_transaction.errors[:status]).to include('is not included in the list')
    end

    it 'allows blank status' do
      fiat_transaction = build(:fiat_transaction, status: nil)
      fiat_transaction.valid?
      expect(fiat_transaction.errors[:status]).to be_empty
    end
  end

  describe 'scopes' do
    it 'sorts by created_at in descending order' do
      old_transaction = create(:fiat_transaction, created_at: 2.days.ago)
      new_transaction = create(:fiat_transaction, created_at: 1.day.ago)

      sorted_transactions = described_class.sorted
      expect(sorted_transactions.first).to eq(new_transaction)
      expect(sorted_transactions.last).to eq(old_transaction)
    end

    it 'filters by currency' do
      vnd_transaction = create(:fiat_transaction)
      php_account = create(:fiat_account, currency: 'PHP')
      php_transaction = create(:fiat_transaction, fiat_account: php_account, currency: 'PHP')

      vnd_transactions = described_class.of_currency('VND')
      expect(vnd_transactions).to include(vnd_transaction)
      expect(vnd_transactions).not_to include(php_transaction)
    end

    it 'filters by transaction_type' do
      mint_transaction = create(:fiat_transaction, transaction_type: 'mint')
      burn_transaction = create(:fiat_transaction, transaction_type: 'burn')

      mint_transactions = described_class.of_transaction_type('mint')
      expect(mint_transactions).to include(mint_transaction)
      expect(mint_transactions).not_to include(burn_transaction)
    end

    it 'filters deposits' do
      deposit_transaction = create(:fiat_transaction, transaction_type: 'deposit')
      withdrawal_transaction = create(:fiat_transaction, transaction_type: 'withdrawal')

      deposits = described_class.deposits
      expect(deposits).to include(deposit_transaction)
      expect(deposits).not_to include(withdrawal_transaction)
    end

    it 'filters withdrawals' do
      deposit_transaction = create(:fiat_transaction, transaction_type: 'deposit')
      withdrawal_transaction = create(:fiat_transaction, transaction_type: 'withdrawal')

      withdrawals = described_class.withdrawals
      expect(withdrawals).to include(withdrawal_transaction)
      expect(withdrawals).not_to include(deposit_transaction)
    end

    it 'filters transfers' do
      transfer_transaction = create(:fiat_transaction, transaction_type: 'transfer')
      deposit_transaction = create(:fiat_transaction, transaction_type: 'deposit')

      transfers = described_class.transfers
      expect(transfers).to include(transfer_transaction)
      expect(transfers).not_to include(deposit_transaction)
    end

    it 'filters refunds' do
      refund_transaction = create(:fiat_transaction, transaction_type: 'refund')
      deposit_transaction = create(:fiat_transaction, transaction_type: 'deposit')

      refunds = described_class.refunds
      expect(refunds).to include(refund_transaction)
      expect(refunds).not_to include(deposit_transaction)
    end

    it 'filters pending transactions' do
      pending_transaction = create(:fiat_transaction, status: 'pending')
      completed_transaction = create(:fiat_transaction, status: 'completed')

      pending = described_class.pending
      expect(pending).to include(pending_transaction)
      expect(pending).not_to include(completed_transaction)
    end

    it 'filters completed transactions' do
      pending_transaction = create(:fiat_transaction, status: 'pending')
      completed_transaction = create(:fiat_transaction, status: 'completed')

      completed = described_class.completed
      expect(completed).to include(completed_transaction)
      expect(completed).not_to include(pending_transaction)
    end

    it 'filters failed transactions' do
      failed_transaction = create(:fiat_transaction, status: 'failed')
      completed_transaction = create(:fiat_transaction, status: 'completed')

      failed = described_class.failed
      expect(failed).to include(failed_transaction)
      expect(failed).not_to include(completed_transaction)
    end

    it 'filters cancelled transactions' do
      cancelled_transaction = create(:fiat_transaction, status: 'cancelled')
      completed_transaction = create(:fiat_transaction, status: 'completed')

      cancelled = described_class.cancelled
      expect(cancelled).to include(cancelled_transaction)
      expect(cancelled).not_to include(completed_transaction)
    end
  end

  describe 'callbacks' do
    it 'sets balance snapshot before create' do
      fiat_account = create(:fiat_account, balance: 1000, frozen_balance: 200)
      fiat_transaction = build(:fiat_transaction, fiat_account: fiat_account)

      fiat_transaction.save!

      expect(fiat_transaction.snapshot_balance).to eq(1000)
      expect(fiat_transaction.snapshot_frozen_balance).to eq(200)
    end

    it 'does not override existing balance snapshots' do
      fiat_transaction = build(:fiat_transaction)
      fiat_transaction.snapshot_balance = 500
      fiat_transaction.snapshot_frozen_balance = 100

      fiat_transaction.save!

      expect(fiat_transaction.snapshot_balance).to eq(500)
      expect(fiat_transaction.snapshot_frozen_balance).to eq(100)
    end

    it 'sets default status to pending if blank' do
      fiat_transaction = build(:fiat_transaction, status: nil)
      fiat_transaction.save!
      expect(fiat_transaction.status).to eq('pending')
    end

    it 'does not override existing status' do
      fiat_transaction = build(:fiat_transaction, status: 'completed')
      fiat_transaction.save!
      expect(fiat_transaction.status).to eq('completed')
    end
  end

  describe 'status check methods' do
    it 'returns true for pending?' do
      fiat_transaction = create(:fiat_transaction, status: 'pending')
      expect(fiat_transaction.pending?).to be true
    end

    it 'returns false for pending? when not pending' do
      fiat_transaction = create(:fiat_transaction, status: 'completed')
      expect(fiat_transaction.pending?).to be false
    end

    it 'returns true for completed?' do
      fiat_transaction = create(:fiat_transaction, status: 'completed')
      expect(fiat_transaction.completed?).to be true
    end

    it 'returns false for completed? when not completed' do
      fiat_transaction = create(:fiat_transaction, status: 'pending')
      expect(fiat_transaction.completed?).to be false
    end

    it 'returns true for failed?' do
      fiat_transaction = create(:fiat_transaction, status: 'failed')
      expect(fiat_transaction.failed?).to be true
    end

    it 'returns false for failed? when not failed' do
      fiat_transaction = create(:fiat_transaction, status: 'pending')
      expect(fiat_transaction.failed?).to be false
    end

    it 'returns true for cancelled?' do
      fiat_transaction = create(:fiat_transaction, status: 'cancelled')
      expect(fiat_transaction.cancelled?).to be true
    end

    it 'returns false for cancelled? when not cancelled' do
      fiat_transaction = create(:fiat_transaction, status: 'pending')
      expect(fiat_transaction.cancelled?).to be false
    end
  end

  describe 'status change methods' do
    it 'changes status to completed with complete!' do
      fiat_transaction = create(:fiat_transaction, status: 'pending')
      fiat_transaction.complete!
      expect(fiat_transaction.status).to eq('completed')
    end

    it 'changes status to failed with fail!' do
      fiat_transaction = create(:fiat_transaction, status: 'pending')
      fiat_transaction.fail!
      expect(fiat_transaction.status).to eq('failed')
    end

    it 'changes status to cancelled with cancel!' do
      fiat_transaction = create(:fiat_transaction, status: 'pending')
      fiat_transaction.cancel!
      expect(fiat_transaction.status).to eq('cancelled')
    end
  end

  describe '.ransackable_attributes' do
    it 'returns allowed attributes for ransack' do
      expected_attributes = %w[
        id fiat_account_id amount transaction_type currency
        snapshot_balance snapshot_frozen_balance
        created_at updated_at
      ]

      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe '.ransackable_associations' do
    it 'returns allowed associations for ransack' do
      expected_associations = %w[fiat_account operation]
      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end
end
