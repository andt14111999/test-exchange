require 'rails_helper'

RSpec.describe CoinDeposit, type: :model do
  describe 'validations' do
    it 'validates presence of tx_hash' do
      deposit = build(:coin_deposit, tx_hash: nil)
      expect(deposit).to be_invalid
      expect(deposit.errors[:tx_hash]).to include("can't be blank")
    end

    it 'validates presence of coin_amount' do
      deposit = build(:coin_deposit, coin_amount: nil)
      expect(deposit).to be_invalid
      expect(deposit.errors[:coin_amount]).to include("can't be blank")
    end

    it 'validates coin_amount is greater than 0' do
      deposit = build(:coin_deposit, coin_amount: 0)
      expect(deposit).to be_invalid
      expect(deposit.errors[:coin_amount]).to include('must be greater than 0')
    end

    it 'validates presence of coin_account' do
      deposit = build(:coin_deposit, coin_account: nil, coin_currency: 'btc')
      expect(deposit).to be_invalid
      expect(deposit.errors[:coin_account]).to include("can't be blank")
    end

    it 'validates out_index is less than or equal to 0 for ETH' do
      deposit = build(:coin_deposit, :eth, out_index: 1)
      expect(deposit).to be_invalid
      expect(deposit.errors[:out_index]).to include('must be less than or equal to 0')
    end

    it 'validates uniqueness of tx_hash with scope' do
      existing_deposit = create(:coin_deposit)
      deposit = build(:coin_deposit,
        tx_hash: existing_deposit.tx_hash,
        out_index: existing_deposit.out_index,
        coin_currency: existing_deposit.coin_currency,
        coin_account: existing_deposit.coin_account
      )
      expect(deposit).to be_invalid
      expect(deposit.errors[:tx_hash]).to include('has already been taken')
    end
  end

  describe 'associations' do
    it 'belongs to coin_account' do
      association = described_class.reflect_on_association(:coin_account)
      expect(association.macro).to eq :belongs_to
      expect(association.options[:optional]).to be true
    end

    it 'belongs to user' do
      association = described_class.reflect_on_association(:user)
      expect(association.macro).to eq :belongs_to
      expect(association.options[:optional]).to be true
    end

    it 'has one coin_deposit_operation' do
      association = described_class.reflect_on_association(:coin_deposit_operation)
      expect(association.macro).to eq :has_one
      expect(association.options[:autosave]).to be false
      expect(association.options[:dependent]).to eq :destroy
    end
  end

  describe 'callbacks' do
    it 'sets user from coin_account on create' do
      coin_account = create(:coin_account, coin_currency: 'btc', layer: 'bitcoin')
      deposit = create(:coin_deposit, coin_account: coin_account, user: nil)
      expect(deposit.user).to eq(coin_account.user)
    end

    it 'calculates coin_fee on create' do
      deposit = create(:coin_deposit)
      expect(deposit.coin_fee).to eq(0)
    end

    it 'verifies deposit after create' do
      deposit = create(:coin_deposit)
      expect(deposit).to be_verified
      expect(deposit.verified_at).not_to be_nil
    end
  end

  describe 'state machine' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin') }
    let(:main_account) { create(:coin_account, user: user, coin_currency: 'btc', layer: 'all', account_type: 'main') }

    before do
      main_account # Create main account before each test
    end

    it 'has correct initial state' do
      deposit = build(:coin_deposit, user: user, coin_account: coin_account)
      expect(deposit).to be_pending
    end

    it 'transitions from pending to forged' do
      deposit = build(:coin_deposit, user: user, coin_account: coin_account)
      deposit.save(validate: false)
      deposit.update_column(:status, 'pending') # Force status to pending
      expect { deposit.forge! }.to change { deposit.status }.from('pending').to('forged')
    end

    it 'transitions from pending to verified' do
      deposit = build(:coin_deposit, user: user, coin_account: coin_account)
      deposit.save(validate: false)
      deposit.update_column(:status, 'pending') # Force status to pending
      expect { deposit.verify! }.to change { deposit.status }.from('pending').to('verified')
    end

    it 'transitions from pending to rejected' do
      deposit = build(:coin_deposit, user: user, coin_account: coin_account)
      deposit.save(validate: false)
      deposit.update_column(:status, 'pending') # Force status to pending
      expect { deposit.reject! }.to change { deposit.status }.from('pending').to('rejected')
    end

    it 'transitions from locked to verified' do
      deposit = create(:coin_deposit, :locked, user: user, coin_account: coin_account)
      expect { deposit.release! }.to change { deposit.status }.from('locked').to('verified')
    end

    it 'sets verified_at when transitioning to verified' do
      deposit = build(:coin_deposit, user: user, coin_account: coin_account)
      deposit.save(validate: false)
      deposit.update_column(:status, 'pending') # Force status to pending
      deposit.update_column(:verified_at, nil) # Force verified_at to nil
      expect { deposit.verify! }.to change { deposit.verified_at }.from(nil)
    end
  end

  describe 'ransackable attributes and associations' do
    it 'returns correct ransackable attributes' do
      expected_attributes = %w[
        id user_id coin_account_id coin_currency coin_amount coin_fee
        tx_hash out_index confirmations_count required_confirmations_count
        status locked_reason last_seen_ip verified_at created_at updated_at
      ]
      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end

    it 'returns correct ransackable associations' do
      expected_associations = %w[user coin_account coin_deposit_operation]
      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end

  describe 'delegations' do
    it 'delegates address to coin_account' do
      coin_account = create(:coin_account, coin_currency: 'btc', layer: 'bitcoin')
      deposit = create(:coin_deposit, coin_account: coin_account)
      expect(deposit.address).to eq(coin_account.address)
    end
  end

  describe 'scopes' do
    it 'orders by id in descending order' do
      deposit1 = create(:coin_deposit)
      deposit2 = create(:coin_deposit)
      expect(described_class.sorted).to eq([ deposit2, deposit1 ])
    end
  end

  describe 'private methods' do
    describe '#main_coin_account' do
      it 'returns main coin account for the currency' do
        user = create(:user)
        main_account = create(:coin_account, user: user, coin_currency: 'btc', layer: 'all', account_type: 'main')
        deposit = create(:coin_deposit, user: user, coin_account: main_account, coin_currency: 'btc')
        expect(deposit.send(:main_coin_account)).to eq(main_account)
      end
    end

    describe '#deposit_client' do
      it 'returns a new instance of CoinDepositService' do
        deposit = create(:coin_deposit)
        expect(deposit.send(:deposit_client)).to be_a(KafkaService::Services::Coin::CoinDepositService)
      end
    end
  end
end
