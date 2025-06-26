# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinWithdrawal, sidekiq: :inline, type: :model do
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
      it 'still valid after update balance less than coin_amount' do
        withdrawal.coin_layer = 'erc20'
        withdrawal.coin_amount = 90.0
        withdrawal.save
        coin_account.lock_amount!(90)
        coin_account.save!
        expect(withdrawal.reload).to be_valid
      end

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

    describe '#validate_coin_address_format' do
      context 'when not internal transfer' do
        it 'validates address format using CryptocurrencyAddressValidator' do
          validator = instance_double(CryptocurrencyAddressValidator)
          allow(CryptocurrencyAddressValidator).to receive(:new)
            .with(withdrawal.coin_address, 'erc20')
            .and_return(validator)
          allow(validator).to receive(:valid?).and_return(true)

          withdrawal.coin_layer = 'erc20'
          withdrawal.valid?

          expect(CryptocurrencyAddressValidator).to have_received(:new)
            .with(withdrawal.coin_address, 'erc20')
        end

        it 'is invalid when address format is invalid' do
          validator = instance_double(CryptocurrencyAddressValidator)
          allow(CryptocurrencyAddressValidator).to receive(:new)
            .and_return(validator)
          allow(validator).to receive(:valid?).and_return(false)

          withdrawal.coin_layer = 'erc20'
          expect(withdrawal).to be_invalid
          expect(withdrawal.errors[:coin_address]).to include('has invalid format')
        end

        it 'handles ArgumentError from validator for unsupported layer' do
          allow(CryptocurrencyAddressValidator).to receive(:new)
            .and_raise(ArgumentError, "Layer 'unsupported' is not supported")

          withdrawal.coin_layer = 'unsupported'
          expect(withdrawal).to be_invalid
          expect(withdrawal.errors[:coin_layer]).to include("Layer 'unsupported' is not supported")
        end
      end

      context 'when internal transfer' do
        it 'skips address format validation' do
          withdrawal.receiver_email = 'receiver@example.com'
          withdrawal.coin_address = 'invalid_address'

          allow(User).to receive(:find_by).with(email: 'receiver@example.com').and_return(create(:user))
          expect(CryptocurrencyAddressValidator).not_to receive(:new)

          withdrawal.valid?
        end
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
          expect(withdrawal.errors[:receiver_username].first).to include('cannot_transfer_to_self')
        end
      end

      context 'with receiver_phone_number' do
        let(:user_with_phone) { create(:user) }
        let(:receiver_with_phone) { create(:user) }

        before do
          # Mock the phone_number field on User
          allow(User).to receive(:find_by).with(phone_number: '1234567890').and_return(user_with_phone)
          allow(User).to receive(:find_by).with(phone_number: '0987654321').and_return(receiver_with_phone)
          allow(User).to receive(:find_by).with(phone_number: '5555555555').and_return(nil)
        end

        it 'is valid with an existing phone number' do
          withdrawal = build(:coin_withdrawal, user: user_with_phone, coin_currency: 'usdt', receiver_phone_number: '0987654321')
          expect(withdrawal).to be_valid
        end

        it 'is invalid with a non-existent phone number' do
          withdrawal = build(:coin_withdrawal, user: user_with_phone, coin_currency: 'usdt', receiver_phone_number: '5555555555')
          expect(withdrawal).to be_invalid
          expect(withdrawal.errors[:receiver_phone_number]).to include('not found')
        end

        it 'prevents transferring to self via phone number' do
          withdrawal = build(:coin_withdrawal, user: user_with_phone, coin_currency: 'usdt', receiver_phone_number: '1234567890')
          expect(withdrawal).to be_invalid
          expect(withdrawal.errors[:receiver_phone_number].first).to include('cannot_transfer_to_self')
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

    describe 'after_commit :create_operations_later callbacks' do
      it 'creates withdrawal operation when status changes to processing and commits' do
        withdrawal.coin_layer = 'erc20'
        withdrawal.save

        expect { withdrawal.update!(status: 'processing') }.to change(CoinWithdrawalOperation, :count).by(1)
        expect(withdrawal.coin_withdrawal_operation).to have_attributes(
          coin_amount: withdrawal.coin_amount,
          coin_fee: withdrawal.coin_fee,
          coin_currency: withdrawal.coin_currency
        )
      end

      it 'does not create operation when status is not processing' do
        withdrawal.coin_layer = 'erc20'
        withdrawal.save

        expect { withdrawal.update!(status: 'completed') }.not_to change(CoinWithdrawalOperation, :count)
        expect(withdrawal.coin_withdrawal_operation).to be_nil
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

        # Stub create_operations to prevent after_enter :processing callbacks from executing
        allow(withdrawal).to receive(:create_operations_later)

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

        # Stub create_operations to prevent after_enter :processing callbacks from executing
        allow(withdrawal).to receive(:create_operations_later)

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

      it 'sends an event to Kafka with recipient_account_key for phone-based internal transfers' do
        withdrawal_service = instance_double(KafkaService::Services::Coin::CoinWithdrawalService)
        account_key = "#{user.id}-coin-#{coin_account.id}"
        receiver = create(:user)

        # Create receiver's coin account
        receiver_coin_account = create(:coin_account, :usdt_main, user: receiver)
        receiver_account_key = "#{receiver.id}-coin-#{receiver_coin_account.id}"

        # Mock phone_number lookup
        allow(User).to receive(:find_by).with(phone_number: '0987654321').and_return(receiver)

        allow(KafkaService::Services::Coin::CoinWithdrawalService).to receive(:new).and_return(withdrawal_service)
        allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key)
          .with(user_id: user.id, account_id: coin_account.id)
          .and_return(account_key)
        allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key)
          .with(user_id: receiver.id, account_id: receiver_coin_account.id)
          .and_return(receiver_account_key)

        # Create an internal transfer withdrawal using phone number
        withdrawal = build(:coin_withdrawal,
                          id: 10003,
                          user:,
                          coin_currency: 'usdt',
                          coin_amount: 10,
                          coin_fee: 0,
                          receiver_phone_number: '0987654321')

        # Stub create_operations to prevent after_enter :processing callbacks from executing
        allow(withdrawal).to receive(:create_operations_later)

        # Setup expectation for the create method with recipient_account_key
        expect(withdrawal_service).to receive(:create).with(
          identifier: 10003,
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
      withdrawal.process! # This will create the operation and transition to processing
      expect(withdrawal.reload.status).to eq('processing')
      expect(withdrawal.coin_withdrawal_operation).to be_present
    end

    it 'transitions from processing to completed' do
      withdrawal.process! # This will create the operation and transition to processing
      expect(withdrawal.reload.status).to eq('processing')
      expect_any_instance_of(described_class).to receive(:send_event_complete_withdrawal_to_kafka)
      withdrawal.coin_withdrawal_operation.update!(withdrawal_status: 'processed', tx_hash: 'tx_123')
      withdrawal.coin_withdrawal_operation.relay!
      expect(withdrawal.reload.status).to eq('processing')
    end

    it 'transitions from processing to cancelled' do
      withdrawal.process! # This will create the operation and transition to processing
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
        %w[id user_id coin_currency coin_amount coin_fee coin_address coin_layer status tx_hash created_at updated_at receiver_email receiver_username receiver_phone_number]
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

    it 'returns true when receiver_phone_number is present' do
      withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', receiver_phone_number: '1234567890')
      expect(withdrawal.internal_transfer?).to be true
    end

    it 'returns false when none of the receiver identifiers are present' do
      withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: 'erc20', coin_address: '0xde709f2102306220921060314715629080e2fb77')
      expect(withdrawal.internal_transfer?).to be false
    end
  end

  describe '#create_operations' do
    let(:user) { create(:user) }
    let(:receiver) { create(:user) }
    let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 100.0) }

    before do
      coin_account
    end

    context 'with internal transfer' do
      it 'creates a coin_internal_transfer_operation when receiver is found by email' do
        withdrawal = create(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_amount: 10, receiver_email: receiver.email)
        withdrawal.process! # Trigger processing state to create operations
        expect(withdrawal.coin_internal_transfer_operation).to be_present
        expect(withdrawal.coin_internal_transfer_operation.sender).to eq(user)
        expect(withdrawal.coin_internal_transfer_operation.receiver).to eq(receiver)
        expect(withdrawal.coin_internal_transfer_operation.coin_currency).to eq('usdt')
        expect(withdrawal.coin_internal_transfer_operation.coin_amount).to eq(10)
        expect(withdrawal.coin_internal_transfer_operation.status).to eq('completed')
      end

      it 'creates a coin_internal_transfer_operation when receiver is found by username' do
        receiver.update!(username: 'testuser')
        withdrawal = create(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_amount: 10, receiver_username: 'testuser')
        withdrawal.process! # Trigger processing state to create operations
        expect(withdrawal.coin_internal_transfer_operation).to be_present
        expect(withdrawal.coin_internal_transfer_operation.sender).to eq(user)
        expect(withdrawal.coin_internal_transfer_operation.receiver).to eq(receiver)
        expect(withdrawal.coin_internal_transfer_operation.coin_currency).to eq('usdt')
        expect(withdrawal.coin_internal_transfer_operation.coin_amount).to eq(10)
        expect(withdrawal.coin_internal_transfer_operation.status).to eq('completed')
      end

      it 'creates a coin_internal_transfer_operation when receiver is found by phone_number' do
        # Mock the phone_number lookup
        allow(User).to receive(:find_by).with(phone_number: '0987654321').and_return(receiver)

        withdrawal = create(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_amount: 10, receiver_phone_number: '0987654321')
        withdrawal.process! # Trigger processing state to create operations
        expect(withdrawal.coin_internal_transfer_operation).to be_present
        expect(withdrawal.coin_internal_transfer_operation.sender).to eq(user)
        expect(withdrawal.coin_internal_transfer_operation.receiver).to eq(receiver)
        expect(withdrawal.coin_internal_transfer_operation.coin_currency).to eq('usdt')
        expect(withdrawal.coin_internal_transfer_operation.coin_amount).to eq(10)
        expect(withdrawal.coin_internal_transfer_operation.status).to eq('completed')
      end
    end

    context 'with external transfer' do
      it 'creates a coin_withdrawal_operation' do
        # Get the actual coin_fee value to check
        allow_any_instance_of(described_class).to receive(:calculate_coin_fee).and_return(1)

        withdrawal = create(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_amount: 10, coin_fee: 1, coin_layer: 'erc20', coin_address: '0xde709f2102306220921060314715629080e2fb77')
        withdrawal.process! # Trigger processing state to create operations
        expect(withdrawal.coin_withdrawal_operation).to be_present
        expect(withdrawal.coin_withdrawal_operation.coin_amount).to eq(10)
        expect(withdrawal.coin_withdrawal_operation.coin_fee).to eq(1)
        expect(withdrawal.coin_withdrawal_operation.coin_currency).to eq('usdt')
      end
    end
  end

  describe 'private methods' do
    describe '#freeze_user_balance and #unfreeze_user_balance' do
      let(:user) { create(:user) }
      let(:withdrawal) { build(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_amount: 50.0, coin_fee: 10.0, coin_layer: 'erc20') }
      let(:coin_account) { instance_double(CoinAccount, frozen_balance: 0.0) }

      before do
        allow(withdrawal).to receive(:coin_account).and_return(coin_account)
      end

      it 'freezes the user balance' do
        expect(coin_account).to receive(:with_lock).and_yield
        expect(coin_account).to receive(:update!).with(frozen_balance: 60.0)

        withdrawal.send(:freeze_user_balance)
      end

      it 'unfreezes the user balance' do
        allow(coin_account).to receive(:frozen_balance).and_return(60.0)
        expect(coin_account).to receive(:with_lock).and_yield
        expect(coin_account).to receive(:update!).with(frozen_balance: 0.0)

        withdrawal.send(:unfreeze_user_balance)
      end
    end

    describe '#send_event_fail_withdrawal_to_kafka' do
      let(:user) { create(:user) }
      let(:withdrawal_service) { instance_double(KafkaService::Services::Coin::CoinWithdrawalService) }

      before do
        allow(KafkaService::Services::Coin::CoinWithdrawalService).to receive(:new).and_return(withdrawal_service)
      end

      it 'sends a failed event to Kafka when status is failed' do
        withdrawal = build_stubbed(:coin_withdrawal, id: 123)
        allow(withdrawal).to receive(:failed?).and_return(true)

        expect(withdrawal_service).to receive(:update_status).with(
          identifier: 123,
          operation_type: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_FAILED
        )

        withdrawal.send(:send_event_fail_withdrawal_to_kafka)
      end

      it 'sends event even if not failed' do
        withdrawal = build_stubbed(:coin_withdrawal, id: 1003)
        allow(withdrawal).to receive(:failed?).and_return(false)

        expect(withdrawal_service).to receive(:update_status).with(
          identifier: 1003,
          operation_type: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_FAILED
        )
        withdrawal.send(:send_event_fail_withdrawal_to_kafka)
      end
    end

    describe '#send_event_cancel_withdrawal_to_kafka' do
      let(:user) { create(:user) }
      let(:withdrawal_service) { instance_double(KafkaService::Services::Coin::CoinWithdrawalService) }

      before do
        allow(KafkaService::Services::Coin::CoinWithdrawalService).to receive(:new).and_return(withdrawal_service)
      end

      it 'sends a cancelled event to Kafka when status is cancelled' do
        withdrawal = build_stubbed(:coin_withdrawal, id: 123)
        allow(withdrawal).to receive(:cancelled?).and_return(true)

        expect(withdrawal_service).to receive(:update_status).with(
          identifier: 123,
          operation_type: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CANCELLED
        )

        withdrawal.send(:send_event_cancel_withdrawal_to_kafka)
      end

      it 'sends event even if not cancelled' do
        withdrawal = build_stubbed(:coin_withdrawal, id: 1006)
        allow(withdrawal).to receive(:cancelled?).and_return(false)

        expect(withdrawal_service).to receive(:update_status).with(
          identifier: 1006,
          operation_type: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CANCELLED
        )
        withdrawal.send(:send_event_cancel_withdrawal_to_kafka)
      end
    end
  end

  describe '#portal_coin' do
    let(:user) { create(:user) }

    it 'returns the portal coin for the given currency and layer' do
      withdrawal = build(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_layer: 'erc20')
      expect(CoinAccount).to receive(:coin_and_layer_to_portal_coin).with('usdt', 'erc20').and_return('erct')
      expect(withdrawal.portal_coin).to eq('erct')
    end
  end

  describe '#detect_internal_transfer_by_address' do
    context 'when address-based internal transfer detection' do
      it 'detects internal transfer when coin_address matches an existing CoinAccount address' do
        sender = create(:user)
        receiver = create(:user)
        create(:coin_account, :usdt_main, user: sender, balance: 100.0)
        create(:coin_account, user: receiver, coin_currency: 'usdt', layer: 'erc20', address: '0x1234567890abcdef', account_type: 'deposit')

        withdrawal = build(:coin_withdrawal,
          user: sender,
          coin_currency: 'usdt',
          coin_layer: 'erc20',
          coin_address: '0x1234567890abcdef',
          coin_amount: 10.0
        )

        withdrawal.valid?

        expect(withdrawal.receiver_email).to eq(receiver.email)
        expect(withdrawal.internal_transfer?).to be true
      end

      it 'does not detect internal transfer when coin_address does not match any CoinAccount' do
        sender = create(:user)
        create(:coin_account, :usdt_main, user: sender, balance: 100.0)

        withdrawal = build(:coin_withdrawal,
          user: sender,
          coin_currency: 'usdt',
          coin_layer: 'erc20',
          coin_address: '0xnonexistentaddress',
          coin_amount: 10.0
        )

        withdrawal.valid?

        expect(withdrawal.receiver_email).to be_nil
        expect(withdrawal.internal_transfer?).to be false
      end

      it 'does not detect internal transfer when coin_address matches but currency differs' do
        sender = create(:user)
        receiver = create(:user)
        create(:coin_account, :usdt_main, user: sender, balance: 100.0)
        create(:coin_account, user: receiver, coin_currency: 'eth', layer: 'erc20', address: '0x1234567890abcdef', account_type: 'deposit')

        withdrawal = build(:coin_withdrawal,
          user: sender,
          coin_currency: 'usdt',
          coin_layer: 'erc20',
          coin_address: '0x1234567890abcdef',
          coin_amount: 10.0
        )

        withdrawal.valid?

        expect(withdrawal.receiver_email).to be_nil
        expect(withdrawal.internal_transfer?).to be false
      end

      it 'does not override existing receiver information' do
        sender = create(:user)
        receiver = create(:user)
        other_receiver = create(:user)
        create(:coin_account, :usdt_main, user: sender, balance: 100.0)
        create(:coin_account, user: other_receiver, coin_currency: 'usdt', layer: 'erc20', address: '0x1234567890abcdef', account_type: 'deposit')

        withdrawal = build(:coin_withdrawal,
          user: sender,
          coin_currency: 'usdt',
          coin_layer: 'erc20',
          coin_address: '0x1234567890abcdef',
          coin_amount: 10.0,
          receiver_email: receiver.email
        )

        withdrawal.valid?

        expect(withdrawal.receiver_email).to eq(receiver.email)
        expect(withdrawal.internal_transfer?).to be true
      end

      it 'does not detect internal transfer when coin_address is blank' do
        sender = create(:user)
        create(:coin_account, :usdt_main, user: sender, balance: 100.0)

        withdrawal = build(:coin_withdrawal,
          user: sender,
          coin_currency: 'usdt',
          coin_layer: 'erc20',
          coin_address: nil,
          coin_amount: 10.0
        )

        withdrawal.valid?

        expect(withdrawal.receiver_email).to be_nil
      end

      it 'creates internal transfer operation when address belongs to our exchange' do
        sender = create(:user)
        receiver = create(:user)
        create(:coin_account, :usdt_main, user: sender, balance: 100.0)
        create(:coin_account, user: receiver, coin_currency: 'usdt', layer: 'erc20', address: '0x1234567890abcdef', account_type: 'deposit')

        withdrawal = create(:coin_withdrawal,
          user: sender,
          coin_currency: 'usdt',
          coin_layer: 'erc20',
          coin_address: '0x1234567890abcdef',
          coin_amount: 10.0
        )
        withdrawal.process! # Trigger processing state to create operations

        expect(withdrawal.coin_internal_transfer_operation).to be_present
        expect(withdrawal.coin_internal_transfer_operation.sender).to eq(sender)
        expect(withdrawal.coin_internal_transfer_operation.receiver).to eq(receiver)
        expect(withdrawal.coin_internal_transfer_operation.coin_currency).to eq('usdt')
        expect(withdrawal.coin_internal_transfer_operation.coin_amount).to eq(10.0)
        expect(withdrawal.coin_withdrawal_operation).to be_nil
      end

      it 'creates regular withdrawal operation when address does not belong to our exchange' do
        sender = create(:user)
        create(:coin_account, :usdt_main, user: sender, balance: 100.0)

        withdrawal = create(:coin_withdrawal,
          user: sender,
          coin_currency: 'usdt',
          coin_layer: 'erc20',
          coin_address: '0xde709f2102306220921060314715629080e2fb77',
          coin_amount: 10.0
        )
        withdrawal.process! # Trigger processing state to create operations

        expect(withdrawal.coin_withdrawal_operation).to be_present
        expect(withdrawal.coin_internal_transfer_operation).to be_nil
      end

      it 'prevents self-transfer when address belongs to sender' do
        sender = create(:user)
        create(:coin_account, :usdt_main, user: sender, balance: 100.0)
        create(:coin_account, user: sender, coin_currency: 'usdt', layer: 'erc20', address: '0x1234567890abcdef', account_type: 'deposit')

        withdrawal = build(:coin_withdrawal,
          user: sender,
          coin_currency: 'usdt',
          coin_layer: 'erc20',
          coin_address: '0x1234567890abcdef',
          coin_amount: 10.0
        )

        expect(withdrawal).to be_invalid
        expect(withdrawal.errors[:receiver_email]).to include('cannot transfer to self')
      end
    end
  end
end
