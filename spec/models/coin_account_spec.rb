require 'rails_helper'

RSpec.describe CoinAccount, type: :model do
  describe 'associations' do
    it 'belongs to user' do
      association = described_class.reflect_on_association(:user)
      expect(association.macro).to eq :belongs_to
    end

    it 'has many coin transactions' do
      association = described_class.reflect_on_association(:coin_transactions)
      expect(association.macro).to eq :has_many
      expect(association.options[:dependent]).to eq :destroy
    end
  end

  describe 'validations' do
    it 'validates presence of coin_currency' do
      account = build(:coin_account, :main)
      account.coin_currency = nil
      expect(account).to be_invalid
      expect(account.errors[:coin_currency]).to include("can't be blank")
    end

    it 'validates inclusion of coin_currency in SUPPORTED_NETWORKS keys' do
      account = build(:coin_account, :main)
      account.coin_currency = 'invalid_coin'
      expect(account).to be_invalid
      expect(account.errors[:coin_currency]).to include('is not included in the list')
    end

    it 'validates presence of layer' do
      account = build(:coin_account, :main)
      account.layer = nil
      expect(account).to be_invalid
      expect(account.errors[:layer]).to include("can't be blank")
    end

    it 'validates presence of balance' do
      account = build(:coin_account, :main)
      account.balance = nil
      expect(account).to be_invalid
      expect(account.errors[:balance]).to include("can't be blank")
    end

    it 'validates balance is greater than or equal to 0' do
      account = build(:coin_account, :main)
      account.balance = -1
      expect(account).to be_invalid
      expect(account.errors[:balance]).to include('must be greater than or equal to 0')
    end

    it 'validates presence of frozen_balance' do
      account = build(:coin_account, :main)
      account.frozen_balance = nil
      expect(account).to be_invalid
      expect(account.errors[:frozen_balance]).to include("can't be blank")
    end

    it 'validates frozen_balance is greater than or equal to 0' do
      account = build(:coin_account, :main)
      account.frozen_balance = -1
      expect(account).to be_invalid
      expect(account.errors[:frozen_balance]).to include('must be greater than or equal to 0')
    end

    it 'validates uniqueness of layer scoped to user_id, coin_currency, and account_type' do
      user = create(:user)
      existing_account = described_class.create!(
        user: user,
        coin_currency: 'usdt',
        layer: 'erc20',
        account_type: 'deposit',
        balance: 0,
        frozen_balance: 0
      )

      account = described_class.new(
        user: user,
        coin_currency: 'usdt',
        layer: 'erc20',
        account_type: 'deposit',
        balance: 0,
        frozen_balance: 0
      )

      expect(account).to be_invalid
      expect(account.errors[:layer]).to include('has already been taken')
    end

    it 'validates presence of account_type' do
      account = build(:coin_account, :main)
      account.account_type = nil
      expect(account).to be_invalid
      expect(account.errors[:account_type]).to include("can't be blank")
    end

    it 'validates inclusion of account_type in ACCOUNT_TYPES' do
      account = build(:coin_account, :main)
      account.account_type = 'invalid_type'
      expect(account).to be_invalid
      expect(account.errors[:account_type]).to include('is not included in the list')
    end

    it 'validates layer inclusion for main account' do
      account = build(:coin_account, :main)
      account.layer = 'erc20'
      expect(account).to be_invalid
      expect(account.errors[:layer]).to include('is not included in the list')
    end

    it 'validates layer inclusion for deposit account' do
      account = build(:coin_account)
      account.layer = 'invalid_layer'
      expect(account).to be_invalid
      expect(account.errors[:layer]).to include('is not included in the list')
    end

    it 'validates frozen_balance cannot be greater than balance' do
      account = build(:coin_account, :main)
      account.balance = 100
      account.frozen_balance = 101
      expect(account).to be_invalid
      expect(account.errors[:frozen_balance]).to include('cannot be greater than balance')
    end

    it 'validates layer for deposit account with unsupported layer' do
      account = build(:coin_account, coin_currency: 'usdt', layer: 'invalid_layer')
      expect(account).to be_invalid
      expect(account.errors[:layer]).to include(
        'is not supported for usdt. Supported layers are: erc20, bep20, trc20'
      )
    end
  end

  describe 'scopes' do
    it 'filters by coin currency' do
      user = create(:user)
      usdt_account = create(:coin_account, :main, user: user, coin_currency: 'usdt')
      eth_account = create(:coin_account, :main, user: user, coin_currency: 'eth')

      expect(described_class.of_coin('usdt')).to include(usdt_account)
      expect(described_class.of_coin('usdt')).not_to include(eth_account)
    end
  end

  describe 'instance methods' do
    describe '#main?' do
      it 'returns true when account_type is main' do
        account = build(:coin_account, :main)
        expect(account.main?).to be true
      end

      it 'returns false when account_type is not main' do
        account = build(:coin_account, :deposit)
        expect(account.main?).to be false
      end
    end

    describe '#deposit?' do
      it 'returns true when account_type is deposit' do
        account = build(:coin_account, :deposit)
        expect(account.deposit?).to be true
      end

      it 'returns false when account_type is not deposit' do
        account = build(:coin_account, :main)
        expect(account.deposit?).to be false
      end
    end

    describe '#available_balance' do
      it 'returns the difference between balance and frozen_balance' do
        account = build(:coin_account, :main, balance: 100, frozen_balance: 30)
        expect(account.available_balance).to eq(70)
      end
    end

    describe '#lock_amount!' do
      it 'locks the specified amount' do
        account = create(:coin_account, :main, balance: 100, frozen_balance: 0)
        account.lock_amount!(30)
        expect(account.frozen_balance).to eq(30)
        expect(account.balance).to eq(100)
      end

      it 'raises error when amount is greater than available balance' do
        account = create(:coin_account, :main, balance: 100, frozen_balance: 80)
        expect { account.lock_amount!(30) }.to raise_error('Insufficient balance')
      end

      it 'does nothing when amount is less than or equal to 0' do
        account = create(:coin_account, :main, balance: 100, frozen_balance: 0)
        account.lock_amount!(0)
        expect(account.frozen_balance).to eq(0)
      end
    end

    describe '#unlock_amount!' do
      it 'unlocks the specified amount' do
        account = create(:coin_account, :main, balance: 100, frozen_balance: 50)
        account.unlock_amount!(30)
        expect(account.frozen_balance).to eq(20)
      end

      it 'raises error when amount is greater than frozen_balance' do
        account = create(:coin_account, :main, balance: 100, frozen_balance: 50)
        expect { account.unlock_amount!(51) }.to raise_error('Insufficient frozen balance')
      end

      it 'does nothing when amount is less than or equal to 0' do
        account = create(:coin_account, :main, balance: 100, frozen_balance: 50)
        expect { account.unlock_amount!(0) }.not_to change(account, :frozen_balance)
        expect { account.unlock_amount!(-10) }.not_to change(account, :frozen_balance)
      end
    end

    describe '#handle_deposit' do
      it 'creates a new deposit record' do
        account = create(:coin_account, :main)
        deposit_params = {
          out_index: 0,
          amount: 10,
          tx_hash: '0x123',
          confirmations_count: 2,
          required_confirmations_count: 3,
          coin: 'usdt'
        }

        result = account.handle_deposit(deposit_params)
        expect(result[1]).to be true
        expect(result[0]).to be_a(CoinDeposit)
      end

      it 'updates an existing deposit record' do
        account = create(:coin_account, :main)
        deposit = create(:coin_deposit, coin_account: account, coin_currency: account.coin_currency)
        deposit_params = {
          out_index: deposit.out_index,
          amount: deposit.coin_amount,
          tx_hash: deposit.tx_hash,
          confirmations_count: 2,
          required_confirmations_count: 3,
          coin: account.coin_currency
        }

        result = account.handle_deposit(deposit_params)
        expect(result[1]).to be true
        expect(result[0]).to eq(deposit)
      end

      it 'returns error messages when deposit is invalid' do
        account = create(:coin_account, :main)
        deposit_params = {
          out_index: 0,
          amount: -1, # Invalid amount
          tx_hash: '0x123',
          confirmations_count: 2,
          required_confirmations_count: 3,
          coin: 'usdt'
        }

        result = account.handle_deposit(deposit_params)
        expect(result[1]).to be false
        expect(result[0]).to be_a(String)
        expect(result[0]).to include('Coin amount must be greater than 0')
      end
    end

    describe '#account_key' do
      it 'returns the account key using KafkaService' do
        user = create(:user, id: 123)
        account = create(:coin_account, :main, id: 456, user: user)

        expect(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key).with(
          user_id: 123,
          account_id: 456
        ).and_return('123_456')

        expect(account.account_key).to eq('123_456')
      end
    end
  end

  describe 'class methods' do
    describe '.portal_coin_to_coin_currency' do
      it 'returns the correct coin currency for portal coin' do
        expect(described_class.portal_coin_to_coin_currency('erct')).to eq('usdt')
        expect(described_class.portal_coin_to_coin_currency('unknown')).to eq('unknown')
      end
    end

    describe '.portal_coin_to_layer' do
      it 'returns the correct layer for portal coin' do
        expect(described_class.portal_coin_to_layer('erct')).to eq('erc20')
        expect(described_class.portal_coin_to_layer('unknown')).to be_nil
      end
    end

    describe '.coin_and_layer_to_portal_coin' do
      it 'returns the correct portal coin for coin and layer' do
        expect(described_class.coin_and_layer_to_portal_coin('usdt', 'erc20')).to eq('erct')
        expect(described_class.coin_and_layer_to_portal_coin('usdt', 'unknown')).to eq('usdt')
      end
    end

    describe '.supported_networks_for' do
      it 'returns supported networks for a coin currency' do
        expect(described_class.supported_networks_for('usdt')).to eq(%w[erc20 bep20 trc20])
        expect(described_class.supported_networks_for('unknown')).to eq([])
      end
    end

    describe '.ransackable_attributes' do
      it 'returns allowed attributes for ransack' do
        expected_attributes = %w[
          id user_id coin_currency layer account_type
          balance frozen_balance
          address created_at updated_at
        ]
        expect(described_class.ransackable_attributes).to match_array(expected_attributes)
      end
    end

    describe '.ransackable_associations' do
      it 'returns allowed associations for ransack' do
        expected_associations = %w[user coin_transactions]
        expect(described_class.ransackable_associations).to match_array(expected_associations)
      end
    end
  end

  describe 'callbacks' do
    describe 'after_save' do
      it 'creates balance decrease notification' do
        account = create(:coin_account, :main, balance: 100)

        expect {
          account.update!(balance: 80)
        }.to change { account.user.notifications.count }.by(1)

        notification = account.user.notifications.last
        expect(notification.title).to eq('Balance Updated')
        expect(notification.content).to eq("Your #{account.coin_currency.upcase} balance has decreased by 20.0")
        expect(notification.notification_type).to eq('balance_decrease')
      end
    end
  end

  describe 'balance change notifications' do
    let(:account) { create(:coin_account, balance: 100, frozen_balance: 50) }

    it 'broadcasts balance update when balance changes' do
      expect(BalanceBroadcastService).to receive(:call).with(account.user)
      account.update!(balance: 150)
    end

    it 'broadcasts balance update when frozen_balance changes' do
      expect(BalanceBroadcastService).to receive(:call).with(account.user)
      account.update!(frozen_balance: 60)
    end

    it 'creates notification when balance increases' do
      expect {
        account.update!(balance: 150)
      }.to change(Notification, :count).by(1)

      notification = Notification.last
      expect(notification.user).to eq(account.user)
      expect(notification.notification_type).to eq('balance_increase')
      expect(notification.content).to include('has increased by 50')
    end

    it 'creates notification when balance decreases' do
      expect {
        account.update!(balance: 50)
      }.to change(Notification, :count).by(1)

      notification = Notification.last
      expect(notification.user).to eq(account.user)
      expect(notification.notification_type).to eq('balance_decrease')
      expect(notification.content).to include('has decreased by 50')
    end

    it 'does not create notification when frozen_balance changes' do
      expect {
        account.update!(frozen_balance: 60)
      }.not_to change(Notification, :count)
    end
  end

  describe 'transaction operations' do
    # ... existing code ...
  end
end
