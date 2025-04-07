require 'rails_helper'

RSpec.describe CoinTransaction, type: :model do
  describe 'validations' do
    it 'validates presence of amount' do
      transaction = build(:coin_transaction, amount: nil)
      expect(transaction).to be_invalid
      expect(transaction.errors[:amount]).to include("can't be blank")
    end

    it 'validates amount is not zero' do
      transaction = build(:coin_transaction, amount: 0)
      expect(transaction).to be_invalid
      expect(transaction.errors[:amount]).to include('must be other than 0')
    end

    it 'validates presence of coin_currency' do
      transaction = build(:coin_transaction, coin_currency: nil)
      expect(transaction).to be_invalid
      expect(transaction.errors[:coin_currency]).to include("can't be blank")
    end

    it 'validates coin_currency is included in supported networks' do
      transaction = build(:coin_transaction, coin_currency: 'invalid_currency')
      expect(transaction).to be_invalid
      expect(transaction.errors[:coin_currency]).to include('is not included in the list')
    end

    it 'validates presence of transaction_type' do
      transaction = build(:coin_transaction, transaction_type: nil)
      expect(transaction).to be_invalid
      expect(transaction.errors[:transaction_type]).to include("can't be blank")
    end

    it 'validates transaction_type is included in allowed types' do
      transaction = build(:coin_transaction, transaction_type: 'invalid_type')
      expect(transaction).to be_invalid
      expect(transaction.errors[:transaction_type]).to include('is not included in the list')
    end
  end

  describe 'associations' do
    it 'belongs to coin_account' do
      association = described_class.reflect_on_association(:coin_account)
      expect(association.macro).to eq :belongs_to
    end

    it 'belongs to operation' do
      association = described_class.reflect_on_association(:operation)
      expect(association.macro).to eq :belongs_to
      expect(association.options[:polymorphic]).to be true
      expect(association.options[:optional]).to be true
    end
  end

  describe 'scopes' do
    it 'orders by created_at in descending order' do
      create(:coin_transaction, created_at: 2.days.ago, coin_currency: 'usdt')
      create(:coin_transaction, created_at: 1.day.ago, coin_currency: 'usdt')
      create(:coin_transaction, created_at: Time.current, coin_currency: 'usdt')

      expect(described_class.sorted.pluck(:created_at)).to eq(
        described_class.order(created_at: :desc).pluck(:created_at)
      )
    end
  end

  describe 'callbacks' do
    it 'sets balance snapshot before create if not set' do
      coin_account = create(:coin_account, :main, balance: 100, frozen_balance: 50)
      transaction = build(:coin_transaction, coin_account: coin_account)

      expect { transaction.save }.to change { transaction.snapshot_balance }.from(nil).to(100)
      expect(transaction.snapshot_frozen_balance).to eq(50)
    end

    it 'does not set balance snapshot if already set' do
      coin_account = create(:coin_account, :main, balance: 100, frozen_balance: 50)
      transaction = build(
        :coin_transaction,
        coin_account: coin_account,
        snapshot_balance: 200,
        snapshot_frozen_balance: 100
      )

      expect { transaction.save }.not_to change { transaction.snapshot_balance }
      expect(transaction.snapshot_balance).to eq(200)
      expect(transaction.snapshot_frozen_balance).to eq(100)
    end
  end

  describe 'ransackable attributes' do
    it 'returns correct ransackable attributes' do
      expected_attributes = %w[
        amount coin_account_id coin_currency created_at id operation_id operation_type
        transaction_type updated_at snapshot_balance snapshot_frozen_balance
      ]
      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe 'ransackable associations' do
    it 'returns correct ransackable associations' do
      expect(described_class.ransackable_associations).to match_array(%w[coin_account operation])
    end
  end
end
