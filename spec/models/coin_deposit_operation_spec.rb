# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinDepositOperation, type: :model do
  describe 'validations' do
    it 'validates presence of out_index' do
      operation = build(:coin_deposit_operation, out_index: nil)
      expect(operation).to be_invalid
      expect(operation.errors[:out_index]).to include("can't be blank")
    end

    it 'validates presence of coin_amount' do
      operation = build(:coin_deposit_operation, coin_amount: nil)
      expect(operation).to be_invalid
      expect(operation.errors[:coin_amount]).to include("can't be blank")
    end

    it 'validates coin_amount is greater than 0' do
      operation = build(:coin_deposit_operation, coin_amount: 0)
      expect(operation).to be_invalid
      expect(operation.errors[:coin_amount]).to include('must be greater than 0')
    end

    it 'validates presence of coin_fee' do
      operation = build(:coin_deposit_operation, coin_fee: nil)
      expect(operation).to be_invalid
      expect(operation.errors[:coin_fee]).to include("can't be blank")
    end

    it 'validates coin_fee is greater than or equal to 0' do
      operation = build(:coin_deposit_operation, coin_fee: -1)
      expect(operation).to be_invalid
      expect(operation.errors[:coin_fee]).to include('must be greater than or equal to 0')
    end

    it 'validates presence of coin_account' do
      operation = build(:coin_deposit_operation, coin_account: nil)
      expect(operation).to be_invalid
      expect(operation.errors[:coin_account]).to include("can't be blank")
    end

    it 'validates presence of tx_hash' do
      operation = build(:coin_deposit_operation, tx_hash: nil)
      expect(operation).to be_invalid
      expect(operation.errors[:tx_hash]).to include("can't be blank")
    end
  end

  describe 'associations' do
    it 'has many coin_transactions' do
      operation = build(:coin_deposit_operation)
      expect(operation.coin_transactions).to be_empty
    end

    it 'belongs to coin_account' do
      operation = create(:coin_deposit_operation)
      expect(operation.coin_account).to be_present
    end

    it 'belongs to coin_deposit' do
      operation = create(:coin_deposit_operation)
      expect(operation.coin_deposit).to be_present
    end

    it 'delegates user to coin_account' do
      operation = create(:coin_deposit_operation)
      expect(operation.user).to eq(operation.coin_account.user)
    end
  end

  describe 'ransackable' do
    it 'returns correct ransackable attributes' do
      expect(described_class.ransackable_attributes).to match_array(
        %w[
          id coin_account_id coin_deposit_id coin_currency
          coin_amount coin_fee tx_hash out_index status
          created_at updated_at
        ]
      )
    end

    it 'returns correct ransackable associations' do
      expect(described_class.ransackable_associations).to match_array(
        %w[coin_account coin_deposit coin_transactions]
      )
    end
  end

  describe '#coin_amount_after_fee' do
    it 'calculates amount after fee' do
      operation = create(:coin_deposit_operation, coin_amount: 1.5, coin_fee: 0.1, platform_fee: 0.05)
      expect(operation.coin_amount_after_fee).to eq(1.35)
    end

    it 'rounds to 8 decimal places' do
      operation = create(:coin_deposit_operation, coin_amount: 1.123456789, coin_fee: 0.1, platform_fee: 0.05)
      expect(operation.coin_amount_after_fee).to eq(0.97345679)
    end
  end

  describe '#main_coin_account' do
    it 'returns main coin account for user' do
      user = create(:user)
      main_account = create(:coin_account, :btc_main, user: user)
      deposit_account = create(:coin_account, :btc, user: user)
      operation = create(:coin_deposit_operation, coin_account: deposit_account)
      expect(operation.main_coin_account).to eq(main_account)
    end
  end

  describe 'callbacks' do
    describe '#create_coin_transactions' do
      it 'creates coin transaction with correct amount' do
        operation = create(:coin_deposit_operation, coin_amount: 1.5, coin_fee: 0.1, platform_fee: 0.05)
        expect(operation.coin_transactions.count).to eq(2)
        expect(operation.coin_transactions.first.amount).to eq(1.35)
      end

      it 'creates platform fee transaction when platform_fee is positive' do
        operation = create(:coin_deposit_operation, coin_amount: 1.5, coin_fee: 0.1, platform_fee: 0.05)
        expect(operation.coin_transactions.count).to eq(2)
        expect(operation.coin_transactions.second.amount).to eq(0.05)
      end

      it 'does not create platform fee transaction when platform_fee is zero' do
        operation = create(:coin_deposit_operation, coin_amount: 1.5, coin_fee: 0.1, platform_fee: 0)
        expect(operation.coin_transactions.count).to eq(1)
      end

      it 'raises error when transaction creation fails' do
        operation = build(:coin_deposit_operation)
        allow(operation.coin_transactions).to receive(:create!).and_raise(StandardError)
        expect { operation.save! }.to raise_error(StandardError)
      end
    end
  end
end
