# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinWithdrawal, type: :model do
  describe 'associations' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }
    let(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: 'erc20') }

    before do
      coin_account
    end

    it { expect(withdrawal).to belong_to(:user) }
    it { expect(withdrawal).to have_one(:coin_withdrawal_operation).dependent(:destroy) }
  end

  describe 'validations' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }
    let(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: 'erc20') }

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
    let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }
    let(:withdrawal) { build(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: nil) }

    before do
      coin_account
    end

    describe '#validate_coin_amount' do
      it 'is valid when amount + fee is less than available balance' do
        withdrawal.coin_layer = 'erc20'
        expect(withdrawal).to be_valid
      end

      it 'is invalid when amount + fee exceeds available balance' do
        withdrawal.coin_layer = 'erc20'
        withdrawal.coin_amount = 100.0
        expect(withdrawal).to be_invalid
        expect(withdrawal.errors[:coin_amount]).to include('exceeds available balance')
      end

      it 'is invalid when coin_amount is nil' do
        withdrawal.coin_layer = 'erc20'
        withdrawal.coin_amount = nil
        expect(withdrawal).to be_invalid
        expect(withdrawal.errors[:coin_amount]).to include("can't be blank")
      end
    end

    describe '#validate_coin_address_and_layer' do
      it 'is valid with both address and layer' do
        withdrawal.coin_layer = 'erc20'
        expect(withdrawal).to be_valid
      end

      it 'is invalid without address' do
        withdrawal.coin_layer = 'erc20'
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

    describe '#validate_receiver_internal' do
      let(:user) { create(:user) }
      let(:receiver) { create(:user) }
      let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }

      before do
        coin_account
      end

      context 'with receiver_email' do
        it 'is valid with an existing email' do
          withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_email: receiver.email)
          expect(withdrawal).to be_valid
        end

        it 'is invalid with a non-existent email' do
          withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_email: 'nonexistent@example.com')
          expect(withdrawal).to be_invalid
          expect(withdrawal.errors[:receiver_email]).to include('not found')
        end

        it 'prevents transferring to self via email' do
          withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_email: user.email)
          expect(withdrawal).to be_invalid
          expect(withdrawal.errors[:receiver_email]).to include('cannot transfer to self')
        end
      end

      context 'with receiver_username' do
        it 'is valid with an existing username' do
          receiver.update!(username: 'validuser')
          withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_username: receiver.username)
          expect(withdrawal).to be_valid
        end

        it 'is invalid with a non-existent username' do
          withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_username: 'nonexistent_username')
          expect(withdrawal).to be_invalid
          expect(withdrawal.errors[:receiver_username]).to include('not found')
        end

        it 'prevents transferring to self via username' do
          user.update!(username: 'myusername')
          withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_username: user.username)
          expect(withdrawal).to be_invalid
          expect(withdrawal.errors[:receiver_username].first).to match(/cannot_transfer_to_self/)
        end
      end
    end
  end

  describe 'callbacks' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :usdt_main, layer: 'all', user: user, balance: 100.0) }
    let(:withdrawal) { build(:coin_withdrawal, user:, coin_currency: 'usdt', coin_layer: 'erc20') }

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
        expect(withdrawal.coin_fee).to eq(Setting.usdt_erc20_withdrawal_fee)
      end
    end

    describe 'after_create callbacks' do
      it 'creates withdrawal operation' do
        withdrawal.coin_layer = 'erc20'
        expect { withdrawal.save }.to change(CoinWithdrawalOperation, :count).by(1)
        expect(withdrawal.coin_withdrawal_operation).to have_attributes(
          coin_amount: withdrawal.coin_amount,
          coin_fee: withdrawal.coin_fee,
          coin_currency: withdrawal.coin_currency
        )
      end
    end

    describe 'after_update callback for status changes' do
      let(:user) { create(:user) }
      let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }
      let(:withdrawal) { create(:coin_withdrawal, user:, coin_currency: 'usdt', coin_layer: 'erc20') }

      before do
        coin_account
      end

      it 'calls send_event_withdrawal_to_kafka when status changes to completed or cancelled' do
        # Use object_id equality to match the exact instance
        expect_any_instance_of(KafkaService::Services::Coin::CoinWithdrawalService).to receive(:create)

        # Change status to trigger callback
        withdrawal.update!(status: 'completed')
      end

      it 'does not call send_event_withdrawal_to_kafka when other attributes change' do
        withdrawal.process!
        expect_any_instance_of(KafkaService::Services::Coin::CoinWithdrawalService).not_to receive(:create)

        # Update without changing status
        withdrawal.update!(coin_amount: withdrawal.coin_amount)
      end
    end

    describe '#send_event_withdrawal_to_kafka' do
      let(:user) { create(:user) }
      let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }

      before do
        coin_account
      end

      it 'sends an event to Kafka with right parameters' do
        withdrawal_service = instance_double(KafkaService::Services::Coin::CoinWithdrawalService)
        account_key = "#{user.id}-coin-#{coin_account.id}"

        allow(KafkaService::Services::Coin::CoinWithdrawalService).to receive(:new).and_return(withdrawal_service)
        allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key)
          .with(user_id: user.id, account_id: coin_account.id)
          .and_return(account_key)

        expect(withdrawal_service).to receive(:create).with(
          identifier: 10000,
          status: 'verified',
          user_id: user.id,
          coin: 'usdt',
          account_key: account_key,
          amount: 10,
          fee: Setting.usdt_erc20_withdrawal_fee
        )
        withdrawal = create(:coin_withdrawal, id: 10000, user:, coin_currency: 'usdt', coin_amount: 10, coin_layer: 'erc20')

        expect(withdrawal_service).to receive(:update_status).with(
          identifier: withdrawal.id,
          operation_type: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_RELEASING
        )
        withdrawal.process!
        withdrawal.complete!
        expect(withdrawal).to be_completed
      end

      it 'sends an event to Kafka with recipient_account_key for internal transfers' do
        withdrawal_service = instance_double(KafkaService::Services::Coin::CoinWithdrawalService)
        account_key = "#{user.id}-coin-#{coin_account.id}"
        receiver = create(:user)

        # Create receiver's coin account
        receiver_coin_account = create(:coin_account, :usdt_main, user: receiver)
        receiver_account_key = "#{receiver.id}-coin-#{receiver_coin_account.id}"

        allow(KafkaService::Services::Coin::CoinWithdrawalService).to receive(:new).and_return(withdrawal_service)
        allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key)
          .with(user_id: user.id, account_id: coin_account.id)
          .and_return(account_key)
        allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key)
          .with(user_id: receiver.id, account_id: receiver_coin_account.id)
          .and_return(receiver_account_key)

        # Create an internal transfer withdrawal
        withdrawal = build(:coin_withdrawal,
                          id: 10001,
                          user:,
                          coin_currency: 'usdt',
                          coin_amount: 10,
                          coin_fee: 0,
                          receiver_email: receiver.email)

        # Stub create_operations to prevent after_create callbacks from executing auto_process!
        allow(withdrawal).to receive(:create_operations)

        # Setup expectation for the create method with recipient_account_key
        expect(withdrawal_service).to receive(:create).with(
          identifier: 10001,
          status: 'verified',
          user_id: user.id,
          coin: 'usdt',
          account_key: account_key,
          amount: 10,
          fee: 0,
          recipient_account_key: receiver_account_key
        )

        withdrawal.save!
      end

      it 'sends an event to Kafka with recipient_account_key for username-based internal transfers' do
        withdrawal_service = instance_double(KafkaService::Services::Coin::CoinWithdrawalService)
        account_key = "#{user.id}-coin-#{coin_account.id}"
        receiver = create(:user)
        receiver.update!(username: 'validusername')

        # Create receiver's coin account
        receiver_coin_account = create(:coin_account, :usdt_main, user: receiver)
        receiver_account_key = "#{receiver.id}-coin-#{receiver_coin_account.id}"

        allow(KafkaService::Services::Coin::CoinWithdrawalService).to receive(:new).and_return(withdrawal_service)
        allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key)
          .with(user_id: user.id, account_id: coin_account.id)
          .and_return(account_key)
        allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key)
          .with(user_id: receiver.id, account_id: receiver_coin_account.id)
          .and_return(receiver_account_key)

        # Create an internal transfer withdrawal using username
        withdrawal = build(:coin_withdrawal,
                          id: 10002,
                          user:,
                          coin_currency: 'usdt',
                          coin_amount: 10,
                          coin_fee: 0,
                          receiver_username: receiver.username)

        # Stub create_operations to prevent after_create callbacks from executing auto_process!
        allow(withdrawal).to receive(:create_operations)

        # Setup expectation for the create method with recipient_account_key
        expect(withdrawal_service).to receive(:create).with(
          identifier: 10002,
          status: 'verified',
          user_id: user.id,
          coin: 'usdt',
          account_key: account_key,
          amount: 10,
          fee: 0,
          recipient_account_key: receiver_account_key
        )

        withdrawal.save!
      end

      it 'handles errors gracefully' do
        withdrawal_service = instance_double(KafkaService::Services::Coin::CoinWithdrawalService)

        allow(KafkaService::Services::Coin::CoinWithdrawalService).to receive(:new).and_return(withdrawal_service)
        allow(withdrawal_service).to receive(:create).and_raise(StandardError, 'Kafka error')

        expect(Rails.logger).to receive(:error).with(/Failed to send withdrawal event to Kafka/)
        withdrawal.update!(status: 'completed')
      end
    end
  end

  describe 'state transitions' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }
    let(:withdrawal) { build(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: 'erc20', coin_amount: 1.0) }

    before do
      coin_account
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

    it 'transitions from processing to cancelled' do
      withdrawal.coin_withdrawal_operation.start_relaying!
      expect(withdrawal.reload.status).to eq('processing')
      withdrawal.cancel!
      expect(withdrawal.reload.status).to eq('cancelled')
    end
  end

  describe '#record_tx_hash_arrived_at' do
    let(:user) { create(:user) }
    let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }
    let(:withdrawal) { create(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: 'erc20', tx_hash_arrived_at: nil) }

    before do
      coin_account
      withdrawal.reload
    end

    it 'updates tx_hash_arrived_at timestamp' do
      time = Time.current
      allow(Time).to receive(:current).and_return(time)
      expect { withdrawal.record_tx_hash_arrived_at }.to change { withdrawal.reload.tx_hash_arrived_at&.change(nsec: withdrawal.reload.tx_hash_arrived_at&.nsec.to_i / 1000 * 1000) }.from(nil).to(time.change(nsec: time.nsec / 1000 * 1000))
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
        %w[user coin_withdrawal_operation coin_transaction coin_internal_transfer_operation]
      )
    end
  end

  describe '#internal_transfer?' do
    let(:user) { create(:user) }
    let(:receiver) { create(:user) }
    let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }

    before do
      coin_account
    end

    it 'returns true when receiver_email is present' do
      withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_email: receiver.email)
      expect(withdrawal.internal_transfer?).to be true
    end

    it 'returns true when receiver_username is present' do
      receiver.update!(username: 'testusername')
      withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_username: receiver.username)
      expect(withdrawal.internal_transfer?).to be true
    end

    it 'returns false when none of the receiver identifiers are present' do
      withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: 'erc20', coin_address: '0x123')
      expect(withdrawal.internal_transfer?).to be false
    end
  end
end
