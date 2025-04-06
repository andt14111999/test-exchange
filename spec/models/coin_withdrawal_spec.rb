require 'rails_helper'

RSpec.describe CoinWithdrawal, type: :model do
  describe 'associations' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :main, user: user, coin_currency: 'btc') }
    let(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: 'btc', coin_layer: 'bitcoin') }

    before do
      coin_account
    end

    it { expect(withdrawal).to belong_to(:user) }
    it { expect(withdrawal).to have_one(:coin_withdrawal_operation).dependent(:destroy) }
  end

  describe 'validations' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :main, user: user, coin_currency: 'btc') }
    let(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: 'btc', coin_layer: 'bitcoin') }

    before do
      coin_account
    end

    it { expect(withdrawal).to validate_presence_of(:coin_currency) }
    it { expect(withdrawal).to validate_presence_of(:coin_amount) }
    it { expect(withdrawal).to validate_presence_of(:coin_address) }
    it { expect(withdrawal).to validate_presence_of(:status) }
    it { expect(withdrawal).to validate_numericality_of(:coin_amount).is_greater_than(0) }
  end

  describe 'custom validations' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :main, user: user, coin_currency: 'btc', balance: 10.0) }
    let(:withdrawal) { build(:coin_withdrawal, user: user, coin_currency: 'btc', coin_layer: nil) }

    before do
      coin_account
    end

    describe '#validate_coin_amount' do
      it 'is valid when amount + fee is less than available balance' do
        withdrawal.coin_layer = 'bitcoin'
        expect(withdrawal).to be_valid
      end

      it 'is invalid when amount + fee exceeds available balance' do
        withdrawal.coin_layer = 'bitcoin'
        withdrawal.coin_amount = 15.0
        expect(withdrawal).to be_invalid
        expect(withdrawal.errors[:coin_amount]).to include('exceeds available balance')
      end

      it 'is invalid when coin_amount is nil' do
        withdrawal.coin_layer = 'bitcoin'
        withdrawal.coin_amount = nil
        expect(withdrawal).to be_invalid
        expect(withdrawal.errors[:coin_amount]).to include("can't be blank")
      end
    end

    describe '#validate_coin_address_and_layer' do
      it 'is valid with both address and layer' do
        withdrawal.coin_layer = 'bitcoin'
        expect(withdrawal).to be_valid
      end

      it 'is invalid without address' do
        withdrawal.coin_layer = 'bitcoin'
        withdrawal.coin_address = nil
        expect(withdrawal).to be_invalid
        expect(withdrawal.errors[:coin_address]).to include("can't be blank")
      end

      it 'is invalid without layer' do
        withdrawal.coin_layer = ''
        withdrawal.valid?
        expect(withdrawal.errors[:coin_layer]).to include("can't be blank")
      end
    end
  end

  describe 'callbacks' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :main, user: user, coin_currency: 'btc', balance: 10.0) }
    let(:withdrawal) { build(:coin_withdrawal, user: user, coin_currency: 'btc', coin_layer: 'bitcoin') }

    before do
      coin_account
    end

    describe 'before_validation callbacks' do
      it 'assigns coin_layer from coin_account if not set' do
        withdrawal.coin_layer = nil
        withdrawal.valid?
        expect(withdrawal.coin_layer).to eq('all')
      end

      it 'calculates coin_fee before create' do
        withdrawal.coin_fee = nil
        withdrawal.valid?
        expect(withdrawal.coin_fee).to eq(0)
      end
    end

    describe 'after_create callbacks' do
      it 'creates withdrawal operation' do
        withdrawal.coin_layer = 'bitcoin'
        expect { withdrawal.save }.to change(CoinWithdrawalOperation, :count).by(1)
        expect(withdrawal.coin_withdrawal_operation).to have_attributes(
          coin_amount: withdrawal.coin_amount,
          coin_fee: withdrawal.coin_fee,
          coin_currency: withdrawal.coin_currency
        )
      end

      it 'freezes user balance' do
        withdrawal.coin_layer = 'bitcoin'
        expect { withdrawal.save }.to change { coin_account.reload.frozen_balance }.by(withdrawal.coin_amount)
      end
    end
  end

  describe 'state transitions' do
    let(:user) { create(:user) }
    let(:withdrawal) { build(:coin_withdrawal, user: user, coin_currency: 'btc', coin_layer: 'bitcoin', coin_amount: 1.0) }
    let(:coin_account) { withdrawal.coin_account }

    before do
      allow_any_instance_of(CoinWithdrawalOperation).to receive(:relay_later)
      withdrawal.save!
    end

    it 'starts with pending status' do
      expect(withdrawal.status).to eq('pending')
    end

    it 'transitions to processing when withdrawal operation starts relaying' do
      withdrawal.coin_withdrawal_operation.start_relaying!
      expect(withdrawal.reload.status).to eq('processing')
    end

    it 'transitions from processing to completed' do
      withdrawal.coin_withdrawal_operation.start_relaying!
      expect(withdrawal.reload.status).to eq('processing')
      withdrawal.coin_withdrawal_operation.update!(withdrawal_status: 'processed', tx_hash: 'tx_123')
      withdrawal.coin_withdrawal_operation.relay!
      expect(withdrawal.reload.status).to eq('completed')
    end

    it 'transitions from processing to cancelled and unfreezes balance' do
      withdrawal.coin_withdrawal_operation.start_relaying!
      expect(withdrawal.reload.status).to eq('processing')
      expect(coin_account.reload.frozen_balance).to eq(withdrawal.coin_amount + withdrawal.coin_fee)
      expect { withdrawal.cancel! }.to change { coin_account.reload.frozen_balance }.by(-(withdrawal.coin_amount + withdrawal.coin_fee))
      expect(withdrawal.reload.status).to eq('cancelled')
    end
  end

  describe '#record_tx_hash_arrived_at' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :main, user: user, coin_currency: 'btc') }
    let(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: 'btc', coin_layer: 'bitcoin', tx_hash_arrived_at: nil) }

    before do
      coin_account
      withdrawal.update_column(:tx_hash_arrived_at, nil)
    end

    it 'updates tx_hash_arrived_at timestamp' do
      time = Time.current
      allow(Time).to receive(:current).and_return(time)
      expect { withdrawal.record_tx_hash_arrived_at }.to change { withdrawal.reload.tx_hash_arrived_at }.from(nil).to(time)
    end
  end

  describe '.ransackable_attributes' do
    it 'returns allowed attributes for ransack' do
      expect(described_class.ransackable_attributes).to match_array(
        %w[id user_id coin_currency coin_amount coin_fee coin_address coin_layer status tx_hash created_at updated_at]
      )
    end
  end

  describe '.ransackable_associations' do
    it 'returns allowed associations for ransack' do
      expect(described_class.ransackable_associations).to match_array(
        %w[user coin_withdrawal_operation coin_transaction]
      )
    end
  end
end
