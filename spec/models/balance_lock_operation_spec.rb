# frozen_string_literal: true

require 'rails_helper'

RSpec.describe BalanceLockOperation, type: :model do
  describe 'associations' do
    it { is_expected.to belong_to(:balance_lock) }
    it { is_expected.to have_many(:coin_transactions).dependent(:destroy) }
  end

  describe 'validations' do
    it { is_expected.to validate_presence_of(:operation_type) }
    it { is_expected.to validate_presence_of(:status) }
    it { is_expected.to validate_inclusion_of(:operation_type).in_array(%w[lock release]) }
  end

  describe 'delegations' do
    it 'delegates locked_balances to balance_lock' do
      balance_lock = create(:balance_lock, locked_balances: { 'usdt' => '100.0' })
      operation = create(:balance_lock_operation, balance_lock: balance_lock)

      expect(operation.locked_balances).to eq(balance_lock.locked_balances)
    end
  end

  describe 'callbacks' do
    it 'auto processes after creation' do
      # Skip actual processing to avoid coin account creation issues
      allow_any_instance_of(described_class).to receive(:auto_process!)

      operation = build(:balance_lock_operation)
      expect(operation).to receive(:auto_process!)

      operation.save!
    end
  end

  describe 'AASM states' do
    before do
      # Skip actual processing to avoid coin account creation issues
      allow_any_instance_of(described_class).to receive(:auto_process!)
    end

    context 'initial state' do
      it 'starts with pending status' do
        operation = build(:balance_lock_operation)
        expect(operation.status).to eq('pending')
        expect(operation).to be_pending
      end
    end

    context 'transitions' do
      it 'can transition from pending to processing' do
        operation = create(:balance_lock_operation)

        expect {
          operation.process!
        }.to change { operation.status }.from('pending').to('processing')
      end

      it 'can transition from processing to completed' do
        operation = create(:balance_lock_operation, status: 'processing')

        expect {
          operation.complete!
        }.to change { operation.status }.from('processing').to('completed')
      end

      it 'can fail from pending' do
        operation = create(:balance_lock_operation)

        expect {
          operation.fail!
        }.to change { operation.status }.from('pending').to('failed')
      end

      it 'can fail from processing' do
        operation = create(:balance_lock_operation, status: 'processing')

        expect {
          operation.fail!
        }.to change { operation.status }.from('processing').to('failed')
      end
    end
  end

  describe '#user' do
    before do
      # Skip actual processing to avoid coin account creation issues
      allow_any_instance_of(described_class).to receive(:auto_process!)
    end

    it 'returns the user from the associated balance lock' do
      user = create(:user)
      balance_lock = create(:balance_lock, user: user)
      operation = create(:balance_lock_operation, balance_lock: balance_lock)

      expect(operation.user).to eq(user)
    end
  end

  describe '#auto_process!' do
    context 'successful processing' do
      it 'processes the operation and completes it' do
        # Set up mocks to avoid actual database operations
        operation = create(:balance_lock_operation)
        allow(operation).to receive(:process!)
        allow(operation).to receive(:lock_user_balances)
        allow(operation).to receive(:complete!)

        # Replace the implementation to avoid DB operations
        expect(operation).to receive(:process!)
        expect(operation).to receive(:lock_user_balances)
        expect(operation).to receive(:complete!)

        operation.auto_process!
      end

      context 'with lock operation type' do
        it 'calls lock_user_balances' do
          operation = build(:balance_lock_operation, operation_type: 'lock')

          # Mock the transaction to avoid DB operations
          allow(ActiveRecord::Base).to receive(:transaction).and_yield

          expect(operation).to receive(:process!)
          expect(operation).to receive(:lock_user_balances)
          expect(operation).to receive(:complete!)

          operation.auto_process!
        end
      end

      context 'with release operation type' do
        it 'calls unlock_user_balances' do
          operation = build(:balance_lock_operation, operation_type: 'release')

          # Mock the transaction to avoid DB operations
          allow(ActiveRecord::Base).to receive(:transaction).and_yield

          expect(operation).to receive(:process!)
          expect(operation).to receive(:unlock_user_balances)
          expect(operation).to receive(:complete!)

          operation.auto_process!
        end
      end
    end

    context 'failed processing' do
      it 'handles errors and updates status' do
        operation = build(:balance_lock_operation)

        # Force an error during processing
        error_message = 'Processing failed'
        allow(operation).to receive(:process!).and_raise(StandardError.new(error_message))

        expect(Rails.logger).to receive(:error).with(/Failed to process balance lock operation: #{error_message}/)

        # Allow update to be called for status update
        allow(operation).to receive(:update).and_return(true)

        expect {
          operation.auto_process!
        }.not_to raise_error

        # Verify update was called with correct parameters
        expect(operation).to have_received(:update).with(
          status: 'failed',
          status_explanation: error_message
        )
      end
    end
  end

  describe 'private methods' do
    describe '#lock_user_balances' do
      it 'creates coin transactions for each locked balance' do
        # Create the necessary accounts
        user = create(:user)
        usdt_account = create(:coin_account, :usdt_main, user: user, balance: 200.0, frozen_balance: 0)
        btc_account = create(:coin_account, :btc_main, user: user, balance: 1.0, frozen_balance: 0)

        # Create a balance lock with the right balances
        balance_lock = create(:balance_lock, user: user, locked_balances: { 'usdt' => '100.0', 'btc' => '0.5' })

        # Skip auto process
        allow_any_instance_of(described_class).to receive(:auto_process!)

        # Create the operation
        operation = create(:balance_lock_operation, balance_lock: balance_lock, operation_type: 'lock')

        # Better way to stub the method chain with proper arguments
        coin_accounts = double('coin_accounts')
        usdt_scope = double('usdt_scope')
        btc_scope = double('btc_scope')

        allow(operation.user).to receive(:coin_accounts).and_return(coin_accounts)
        allow(coin_accounts).to receive(:of_coin).with('usdt').and_return(usdt_scope)
        allow(coin_accounts).to receive(:of_coin).with('btc').and_return(btc_scope)
        allow(usdt_scope).to receive(:main).and_return(usdt_account)
        allow(btc_scope).to receive(:main).and_return(btc_account)

        # Create stubs for the transaction creation
        expect(operation.coin_transactions).to receive(:create!).with(
          hash_including(
            amount: -100.0,
            coin_currency: 'usdt',
            coin_account: usdt_account,
            transaction_type: 'lock'
          )
        )

        expect(operation.coin_transactions).to receive(:create!).with(
          hash_including(
            amount: -0.5,
            coin_currency: 'btc',
            coin_account: btc_account,
            transaction_type: 'lock'
          )
        )

        # Call the private method
        operation.send(:lock_user_balances)
      end
    end

    describe '#unlock_user_balances' do
      it 'creates coin transactions for each locked balance' do
        # Create the necessary accounts
        user = create(:user)
        usdt_account = create(:coin_account, :usdt_main, user: user, balance: 200.0, frozen_balance: 100.0)
        btc_account = create(:coin_account, :btc_main, user: user, balance: 1.0, frozen_balance: 0.5)

        # Create a balance lock with the right balances
        balance_lock = create(:balance_lock, user: user, locked_balances: { 'usdt' => '100.0', 'btc' => '0.5' })

        # Skip auto process
        allow_any_instance_of(described_class).to receive(:auto_process!)

        # Create the operation
        operation = create(:balance_lock_operation, balance_lock: balance_lock, operation_type: 'release')

        # Better way to stub the method chain with proper arguments
        coin_accounts = double('coin_accounts')
        usdt_scope = double('usdt_scope')
        btc_scope = double('btc_scope')

        allow(operation.user).to receive(:coin_accounts).and_return(coin_accounts)
        allow(coin_accounts).to receive(:of_coin).with('usdt').and_return(usdt_scope)
        allow(coin_accounts).to receive(:of_coin).with('btc').and_return(btc_scope)
        allow(usdt_scope).to receive(:main).and_return(usdt_account)
        allow(btc_scope).to receive(:main).and_return(btc_account)

        # Create stubs for the transaction creation
        expect(operation.coin_transactions).to receive(:create!).with(
          hash_including(
            amount: 100.0,
            coin_currency: 'usdt',
            coin_account: usdt_account,
            transaction_type: 'unlock'
          )
        )

        expect(operation.coin_transactions).to receive(:create!).with(
          hash_including(
            amount: 0.5,
            coin_currency: 'btc',
            coin_account: btc_account,
            transaction_type: 'unlock'
          )
        )

        # Call the private method
        operation.send(:unlock_user_balances)
      end
    end
  end

  describe '.ransackable_attributes' do
    it 'returns the expected attributes' do
      expected_attributes = %w[
        id balance_lock_id operation_type
        status status_explanation created_at updated_at
      ]

      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe '.ransackable_associations' do
    it 'returns the expected associations' do
      expected_associations = %w[balance_lock coin_transactions]

      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end
end
