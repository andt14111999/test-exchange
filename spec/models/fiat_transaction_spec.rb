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

    it 'validates amount is greater than 0' do
      fiat_transaction = build(:fiat_transaction, amount: 0)
      expect(fiat_transaction).to be_invalid
      expect(fiat_transaction.errors[:amount]).to include('must be greater than 0')
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
