# frozen_string_literal: true

require 'rails_helper'

RSpec.describe FiatAccount, type: :model do
  describe 'associations' do
    it { is_expected.to belong_to(:user) }
    it { is_expected.to have_many(:fiat_transactions).dependent(:destroy) }
  end

  describe 'validations' do
    subject { build(:fiat_account) }

    it { is_expected.to validate_presence_of(:currency) }
    it { is_expected.to validate_presence_of(:balance) }
    it { is_expected.to validate_presence_of(:frozen_balance) }
    it { is_expected.to validate_presence_of(:user_id) }

    it { is_expected.to validate_numericality_of(:balance).is_greater_than_or_equal_to(0) }
    it { is_expected.to validate_numericality_of(:frozen_balance).is_greater_than_or_equal_to(0) }

    it { is_expected.to validate_inclusion_of(:currency).in_array(FiatAccount::SUPPORTED_CURRENCIES.keys) }

    it 'validates frozen_balance cannot be greater than balance' do
      fiat_account = build(:fiat_account, balance: 100, frozen_balance: 150)
      expect(fiat_account).to be_invalid
      expect(fiat_account.errors[:frozen_balance]).to include('cannot be greater than balance')
    end
  end

  describe 'scopes' do
    describe '.of_currency' do
      it 'returns accounts with specified currency' do
        vnd_account = create(:fiat_account, currency: 'VND')
        create(:fiat_account, currency: 'PHP')

        expect(described_class.of_currency('VND')).to contain_exactly(vnd_account)
      end
    end
  end

  describe 'class methods' do
    describe '.ransackable_attributes' do
      it 'returns allowed attributes for ransack' do
        expected_attributes = %w[
          id user_id currency balance frozen_balance
          created_at updated_at
        ]

        expect(described_class.ransackable_attributes).to match_array(expected_attributes)
      end
    end

    describe '.ransackable_associations' do
      it 'returns allowed associations for ransack' do
        expected_associations = %w[user fiat_transactions]

        expect(described_class.ransackable_associations).to match_array(expected_associations)
      end
    end
  end

  describe 'instance methods' do
    describe '#available_balance' do
      it 'returns balance minus frozen_balance' do
        fiat_account = build(:fiat_account, balance: 100, frozen_balance: 30)
        expect(fiat_account.available_balance).to eq(70)
      end
    end

    describe '#mint_amount!' do
      it 'increases balance and creates mint transaction' do
        fiat_account = create(:fiat_account, balance: 100)

        expect do
          fiat_account.mint_amount!(50)
        end.to change(fiat_account, :balance).by(50)
          .and change(FiatTransaction, :count).by(1)

        transaction = fiat_account.fiat_transactions.last
        expect(transaction.amount).to eq(50)
        expect(transaction.transaction_type).to eq('mint')
        expect(transaction.currency).to eq(fiat_account.currency)
      end

      it 'does nothing when amount is zero or negative' do
        fiat_account = create(:fiat_account, balance: 100)

        expect do
          fiat_account.mint_amount!(0)
        end.not_to change { [ fiat_account.balance, FiatTransaction.count ] }

        expect do
          fiat_account.mint_amount!(-10)
        end.not_to change { [ fiat_account.balance, FiatTransaction.count ] }
      end
    end

    describe '#burn_amount!' do
      it 'decreases balance and creates burn transaction' do
        fiat_account = create(:fiat_account, balance: 100)

        expect do
          fiat_account.burn_amount!(30)
        end.to change(fiat_account, :balance).by(-30)
          .and change(FiatTransaction, :count).by(1)

        transaction = fiat_account.fiat_transactions.last
        expect(transaction.amount).to eq(30)
        expect(transaction.transaction_type).to eq('burn')
        expect(transaction.currency).to eq(fiat_account.currency)
      end

      it 'raises error when amount is greater than balance' do
        fiat_account = create(:fiat_account, balance: 100)

        expect do
          fiat_account.burn_amount!(150)
        end.to raise_error('Insufficient balance')

        expect(fiat_account.reload.balance).to eq(100)
        expect(FiatTransaction.count).to eq(0)
      end

      it 'does nothing when amount is zero or negative' do
        fiat_account = create(:fiat_account, balance: 100)

        expect do
          fiat_account.burn_amount!(0)
        end.not_to change { [ fiat_account.balance, FiatTransaction.count ] }

        expect do
          fiat_account.burn_amount!(-10)
        end.not_to change { [ fiat_account.balance, FiatTransaction.count ] }
      end
    end

    describe '#lock_amount!' do
      it 'increases frozen_balance' do
        fiat_account = create(:fiat_account, balance: 100, frozen_balance: 0)
        expect { fiat_account.lock_amount!(50) }.to change { fiat_account.frozen_balance }.by(50)
      end

      it 'raises error when amount is greater than available_balance' do
        fiat_account = create(:fiat_account, balance: 100, frozen_balance: 0)
        expect { fiat_account.lock_amount!(101) }.to raise_error('Insufficient balance')
      end

      it 'does nothing when amount is less than or equal to 0' do
        fiat_account = create(:fiat_account, balance: 100, frozen_balance: 0)
        expect { fiat_account.lock_amount!(0) }.not_to change { fiat_account.frozen_balance }
        expect { fiat_account.lock_amount!(-10) }.not_to change { fiat_account.frozen_balance }
      end
    end

    describe '#unlock_amount!' do
      it 'decreases frozen_balance' do
        fiat_account = create(:fiat_account, balance: 100, frozen_balance: 50)
        expect { fiat_account.unlock_amount!(30) }.to change { fiat_account.frozen_balance }.by(-30)
      end

      it 'raises error when amount is greater than frozen_balance' do
        fiat_account = create(:fiat_account, balance: 100, frozen_balance: 50)
        expect { fiat_account.unlock_amount!(51) }.to raise_error('Insufficient frozen balance')
      end

      it 'does nothing when amount is less than or equal to 0' do
        fiat_account = create(:fiat_account, balance: 100, frozen_balance: 50)
        expect { fiat_account.unlock_amount!(0) }.not_to change { fiat_account.frozen_balance }
        expect { fiat_account.unlock_amount!(-10) }.not_to change { fiat_account.frozen_balance }
      end
    end

    describe '#account_key' do
      it 'returns the account key using KafkaService' do
        user = create(:user, id: 123)
        fiat_account = create(:fiat_account, id: 456, user: user)

        expect(KafkaService::Services::AccountKeyBuilderService).to receive(:build_fiat_account_key).with(
          user_id: 123,
          account_id: 456
        ).and_return('123_456')

        expect(fiat_account.account_key).to eq('123_456')
      end
    end
  end
end
