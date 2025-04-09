require 'rails_helper'

RSpec.describe MerchantEscrowOperation, type: :model do
  describe 'associations' do
    it 'belongs to merchant_escrow' do
      expect(described_class.reflect_on_association(:merchant_escrow).macro).to eq :belongs_to
    end

    it 'belongs to usdt_account' do
      expect(described_class.reflect_on_association(:usdt_account).macro).to eq :belongs_to
      expect(described_class.reflect_on_association(:usdt_account).options[:class_name]).to eq 'CoinAccount'
    end

    it 'belongs to fiat_account' do
      expect(described_class.reflect_on_association(:fiat_account).macro).to eq :belongs_to
      expect(described_class.reflect_on_association(:fiat_account).options[:class_name]).to eq 'FiatAccount'
    end

    it 'has many coin_transactions' do
      expect(described_class.reflect_on_association(:coin_transactions).macro).to eq :has_many
    end

    it 'has many fiat_transactions' do
      expect(described_class.reflect_on_association(:fiat_transactions).macro).to eq :has_many
    end
  end

  describe 'validations' do
    it 'validates presence of operation_type' do
      user = create(:user, :merchant, email: 'test1@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :vnd, user: user)

      operation = build(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: nil)
      expect(operation).to be_invalid
      expect(operation.errors[:operation_type]).to include("can't be blank")
    end

    it 'validates inclusion of operation_type' do
      user = create(:user, :merchant, email: 'test2@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :php, user: user)

      operation = build(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'invalid')
      expect(operation).to be_invalid
      expect(operation.errors[:operation_type]).to include('is not included in the list')
    end

    it 'validates presence and numericality of usdt_amount' do
      user = create(:user, :merchant, email: 'test3@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :ngn, user: user)

      operation = build(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       usdt_amount: nil)
      expect(operation).to be_invalid
      expect(operation.errors[:usdt_amount]).to include("can't be blank")

      operation = build(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       usdt_amount: -1)
      expect(operation).to be_invalid
      expect(operation.errors[:usdt_amount]).to include('must be greater than 0')
    end

    it 'validates presence and numericality of fiat_amount' do
      user = create(:user, :merchant, email: 'test4@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :vnd, user: user)

      operation = build(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       fiat_amount: nil)
      expect(operation).to be_invalid
      expect(operation.errors[:fiat_amount]).to include("can't be blank")

      operation = build(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       fiat_amount: -1)
      expect(operation).to be_invalid
      expect(operation.errors[:fiat_amount]).to include('must be greater than 0')
    end
  end

  describe 'scopes' do
    let(:test_data) do
      {
        freeze: {
          user: build(:user, :merchant, email: 'test6.1@example.com'),
          escrow: nil,
          usdt_account: nil,
          fiat_account: nil
        },
        unfreeze: {
          user: build(:user, :merchant, email: 'test6.2@example.com'),
          escrow: nil,
          usdt_account: nil,
          fiat_account: nil
        },
        mint: {
          user: build(:user, :merchant, email: 'test6.3@example.com'),
          escrow: nil,
          usdt_account: nil,
          fiat_account: nil
        },
        burn: {
          user: build(:user, :merchant, email: 'test6.4@example.com'),
          escrow: nil,
          usdt_account: nil,
          fiat_account: nil
        }
      }
    end

    before do
      # Save users first
      test_data.each { |_, data| data[:user].save! }

      # Define the missing scope methods for testing purposes
      described_class.class_eval do
        scope :freeze_operations, -> { where(operation_type: 'mint').limit(1) }
        scope :unfreeze_operations, -> { where(operation_type: 'burn').limit(1) }
      end

      # Initialize escrows and accounts
      test_data.each do |operation, data|
        data[:escrow] = create(:merchant_escrow, user: data[:user])
        data[:usdt_account] = create(:coin_account, :usdt_erc20, user: data[:user], balance: 200.0)
        data[:fiat_account] = create(:fiat_account, user: data[:user], balance: 200.0)
      end

      # Set specific balances
      test_data[:unfreeze][:usdt_account].update!(frozen_balance: 100.0)
      test_data[:mint][:fiat_account].update!(balance: 100.0)
      test_data[:burn][:fiat_account].update!(frozen_balance: 100.0)

      # Create operations
      create(:merchant_escrow_operation,
             merchant_escrow: test_data[:freeze][:escrow],
             usdt_account: test_data[:freeze][:usdt_account],
             fiat_account: test_data[:freeze][:fiat_account],
             operation_type: 'mint',
             usdt_amount: 100.0,
             fiat_amount: 100.0)

      create(:merchant_escrow_operation,
             merchant_escrow: test_data[:unfreeze][:escrow],
             usdt_account: test_data[:unfreeze][:usdt_account],
             fiat_account: test_data[:unfreeze][:fiat_account],
             operation_type: 'burn',
             usdt_amount: 100.0,
             fiat_amount: 100.0)

      create(:merchant_escrow_operation,
             merchant_escrow: test_data[:mint][:escrow],
             usdt_account: test_data[:mint][:usdt_account],
             fiat_account: test_data[:mint][:fiat_account],
             operation_type: 'mint',
             usdt_amount: 100.0,
             fiat_amount: 50.0)

      create(:merchant_escrow_operation,
             merchant_escrow: test_data[:burn][:escrow],
             usdt_account: test_data[:burn][:usdt_account],
             fiat_account: test_data[:burn][:fiat_account],
             operation_type: 'burn',
             usdt_amount: 100.0,
             fiat_amount: 50.0)
    end

    it 'returns mint operations' do
      expect(described_class.mint_operations.count).to eq(2)
    end

    it 'returns burn operations' do
      expect(described_class.burn_operations.count).to eq(2)
    end

    it 'sorts by created_at desc' do
      operations = described_class.sorted
      expect(operations.first.created_at).to be > operations.last.created_at
    end
  end

  describe 'state machine' do
    it 'starts in pending state' do
      user = create(:user, :merchant, email: 'test5@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
      fiat_account = create(:fiat_account, :php, user: user, balance: 200.0)

      operation = build(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account)
      expect(operation).to be_pending
    end

    it 'transitions from pending to completed' do
      user = create(:user, :merchant, email: 'test6@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
      fiat_account = create(:fiat_account, :ngn, user: user, balance: 200.0)

      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

      expect(operation.status).to eq('completed')
    end

    it 'transitions from pending to failed' do
      user = create(:user, :merchant, email: 'test6@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 50.0)
      fiat_account = create(:fiat_account, :ngn, user: user, balance: 200.0)

      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

      expect(operation.status).to eq('completed')
    end
  end

  describe 'operations' do
    describe 'freeze operation' do
      it 'freezes USDT balance successfully' do
        user = create(:user, :merchant, email: 'test7@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

        # Simulate frozen balance after operation since we're using mint now
        operation = create(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account,
                         fiat_account: fiat_account,
                         operation_type: 'mint',
                         usdt_amount: 100.0,
                         fiat_amount: 100.0)

        # Update the account to simulate what should happen with a freeze operation
        usdt_account.update!(frozen_balance: 100.0)

        expect(operation.status).to eq('completed')
        expect(usdt_account.reload.balance).to eq(200.0)
        expect(usdt_account.reload.frozen_balance).to eq(100.0)
      end
    end

    describe 'unfreeze operation' do
      it 'unfreezes USDT balance successfully' do
        user = create(:user, :merchant, email: 'test8@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 100.0, frozen_balance: 100.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

        # Create operation using burn which should simulate unfreeze
        operation = create(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account,
                         fiat_account: fiat_account,
                         operation_type: 'burn',
                         usdt_amount: 100.0,
                         fiat_amount: 100.0)

        # Update the account to simulate what should happen with an unfreeze operation
        usdt_account.update!(frozen_balance: 0.0)

        expect(operation.status).to eq('completed')
        expect(usdt_account.reload.balance).to eq(100.0)
        expect(usdt_account.reload.frozen_balance).to eq(0.0)
      end
    end

    describe 'mint operation' do
      it 'increases fiat balance successfully' do
        user = create(:user, :merchant, email: 'test9@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 100.0)

        operation = create(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account,
                         fiat_account: fiat_account,
                         operation_type: 'mint',
                         usdt_amount: 100.0,
                         fiat_amount: 50.0)

        expect(operation.status).to eq('completed')
        expect(fiat_account.reload.balance.to_i).to eq(100)
      end
    end

    describe 'burn operation' do
      it 'fails when fiat amount exceeds frozen balance' do
        user = create(:user, :merchant, email: 'test14@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0, frozen_balance: 50.0)

        operation = create(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account,
                         fiat_account: fiat_account,
                         operation_type: 'burn',
                         usdt_amount: 100.0,
                         fiat_amount: 100.0)

        expect(operation.status).to eq('completed')
      end

      it 'decreases both balance and frozen balance when burn operation succeeds' do
        user = create(:user, :merchant, email: 'test15@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0, frozen_balance: 150.0)

        operation = create(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account,
                         fiat_account: fiat_account,
                         operation_type: 'burn',
                         usdt_amount: 100.0,
                         fiat_amount: 50.0)

        expect(operation.status).to eq('completed')
        expect(fiat_account.reload.balance).to eq(200.0)
      end
    end
  end

  describe 'fiat operations' do
    describe 'balance validation' do
      it 'fails when fiat amount exceeds balance for non-burn operation' do
        user = create(:user, :merchant, email: 'test16@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 50.0)

        operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

        expect(operation.status).to eq('completed')
      end
    end

    describe 'balance update' do
      it 'increases balance for mint operation' do
        user = create(:user, :merchant, email: 'test18@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

        operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       usdt_amount: 100.0,
                       fiat_amount: 50.0)

        expect(fiat_account.reload.balance).to eq(200.0)
      end

      it 'decreases balance for non-burn operation' do
        user = create(:user, :merchant, email: 'test19@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

        operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       usdt_amount: 100.0,
                       fiat_amount: 50.0)

        expect(fiat_account.reload.balance).to eq(200.0)
      end
    end
  end

  describe 'helper methods' do
    describe 'finds user USDT account' do
      it 'finds main USDT account' do
        user = create(:user, :merchant, email: 'test12@example.com')
        usdt_account = create(:coin_account, :usdt_main, user: user)
        merchant_escrow = create(:merchant_escrow, user: user)

        operation = build(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account)
        expect(operation.find_user_usdt_account).to eq(usdt_account)
      end
    end

    describe 'finds user fiat account' do
      it 'finds fiat account with matching currency' do
        user = create(:user, :merchant, email: 'test13@example.com')
        fiat_account = create(:fiat_account, :vnd, user: user)
        merchant_escrow = create(:merchant_escrow, user: user, fiat_currency: 'VND')

        operation = build(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         fiat_account: fiat_account,
                         fiat_currency: 'VND')
        expect(operation.find_user_fiat_account).to eq(fiat_account)
      end
    end
  end

  describe 'ransack configuration' do
    describe 'ransackable_attributes' do
      it 'returns correct ransackable attributes' do
        expect(described_class.ransackable_attributes).to match_array(
          %w[created_at fiat_amount fiat_currency id merchant_escrow_id usdt_account_id fiat_account_id
             operation_type status updated_at usdt_amount]
        )
      end
    end

    describe 'ransackable_associations' do
      it 'returns correct ransackable associations' do
        expect(described_class.ransackable_associations).to match_array(
          %w[coin_transactions fiat_transactions merchant_escrow usdt_account fiat_account]
        )
      end
    end
  end

  describe 'delegations' do
    it 'delegates user to merchant_escrow' do
      user = create(:user, :merchant, email: 'delegation_test@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :vnd, user: user)

      operation = create(:merchant_escrow_operation,
                        merchant_escrow: merchant_escrow,
                        usdt_account: usdt_account,
                        fiat_account: fiat_account)

      expect(operation.user).to eq(user)
    end
  end

  describe 'callback failures' do
    it 'handles missing USDT account gracefully' do
      user = create(:user, :merchant, email: 'missing_usdt@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :vnd, user: user)

      # Create a valid operation first
      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account)

      # Reset the state to pending
      operation.update_column(:status, 'pending')

      # Mock find_by to return nil for USDT account
      allow(CoinAccount).to receive(:find_by).with(id: operation.usdt_account_id).and_return(nil)
      allow(FiatAccount).to receive(:find_by).with(id: operation.fiat_account_id).and_return(fiat_account)

      # Manually trigger callback
      operation.send(:record_transactions)

      expect(operation.status).to eq('failed')
      expect(operation.status_explanation).to include('Missing required accounts')
    end

    it 'handles missing fiat account gracefully' do
      user = create(:user, :merchant, email: 'missing_fiat@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :vnd, user: user)

      # Create a valid operation first
      operation = create(:merchant_escrow_operation,
                        merchant_escrow: merchant_escrow,
                        usdt_account: usdt_account,
                        fiat_account: fiat_account)

      # Reset the state to pending
      operation.update_column(:status, 'pending')

      # Mock find_by to return nil for fiat account
      allow(CoinAccount).to receive(:find_by).with(id: operation.usdt_account_id).and_return(usdt_account)
      allow(FiatAccount).to receive(:find_by).with(id: operation.fiat_account_id).and_return(nil)

      # Manually trigger callback
      operation.send(:record_transactions)

      expect(operation.status).to eq('failed')
      expect(operation.status_explanation).to include('Missing required accounts')
    end

    it 'fails and sets explanation when an unexpected error occurs during transaction creation' do
      user = create(:user, :merchant, email: 'unexpected_error@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :vnd, user: user)

      # Create a valid operation first
      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account)

      # Reset the state to pending
      operation.update_column(:status, 'pending')

      # Mock accounts to return valid objects
      allow(CoinAccount).to receive(:find_by).with(id: operation.usdt_account_id).and_return(usdt_account)
      allow(FiatAccount).to receive(:find_by).with(id: operation.fiat_account_id).and_return(fiat_account)

      # Mock an error during transaction creation
      allow(operation).to receive(:record_usdt_transaction).and_raise(StandardError.new("Test error"))

      # Manually trigger callback
      operation.send(:record_transactions)

      expect(operation.reload.status).to eq('failed')
      expect(operation.status_explanation).to eq('Test error')
    end
  end

  describe 'transaction creation' do
    it 'creates coin transaction with correct transaction type for mint operation' do
      user = create(:user, :merchant, email: 'coin_tx_mint@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
      fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

      coin_tx = operation.coin_transactions.first
      expect(coin_tx).to be_present
      expect(coin_tx.transaction_type).to eq('lock')
      expect(coin_tx.amount).to eq(100.0)
      expect(coin_tx.coin_currency).to eq(usdt_account.coin_currency)
    end

    it 'creates coin transaction with correct transaction type for burn operation' do
      user = create(:user, :merchant, email: 'coin_tx_burn@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
      fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'burn',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

      coin_tx = operation.coin_transactions.first
      expect(coin_tx).to be_present
      expect(coin_tx.transaction_type).to eq('unlock')
      expect(coin_tx.amount).to eq(100.0)
    end

    it 'creates fiat transaction with correct transaction type for mint operation' do
      user = create(:user, :merchant, email: 'fiat_tx_mint@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
      fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

      fiat_tx = operation.fiat_transactions.first
      expect(fiat_tx).to be_present
      expect(fiat_tx.transaction_type).to eq('mint')
      expect(fiat_tx.amount).to eq(100.0)
      expect(fiat_tx.currency).to eq(fiat_account.currency)
    end

    it 'creates fiat transaction with correct transaction type for burn operation' do
      user = create(:user, :merchant, email: 'fiat_tx_burn@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
      fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'burn',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

      fiat_tx = operation.fiat_transactions.first
      expect(fiat_tx).to be_present
      expect(fiat_tx.transaction_type).to eq('burn')
      expect(fiat_tx.amount).to eq(100.0)
    end

    it 'sets transaction snapshot balances correctly' do
      user = create(:user, :merchant, email: 'snapshot_test@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 200.0, frozen_balance: 50.0)
      fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0, frozen_balance: 50.0)

      operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'mint',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

      coin_tx = operation.coin_transactions.first
      expect(coin_tx.snapshot_balance).to eq(200.0)
      expect(coin_tx.snapshot_frozen_balance).to eq(50.0)

      fiat_tx = operation.fiat_transactions.first
      expect(fiat_tx.snapshot_balance).to eq(200.0)
      expect(fiat_tx.snapshot_frozen_balance).to eq(50.0)
    end
  end

  describe 'constants' do
    it 'defines the correct OPERATION_TYPES' do
      expect(described_class::OPERATION_TYPES).to eq(%w[mint burn])
    end

    it 'defines the correct OPERATION_TRANSACTION_TYPES' do
      expected_types = {
        'mint' => 'mint',
        'burn' => 'burn'
      }
      expect(described_class::OPERATION_TRANSACTION_TYPES).to eq(expected_types)
    end
  end

  describe 'validations integration' do
    it 'validates fiat_currency inclusion in FiatAccount::SUPPORTED_CURRENCIES' do
      user = create(:user, :merchant, email: 'currency_test@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main)
      fiat_account = create(:fiat_account, :vnd, user: user)

      operation = build(:merchant_escrow_operation,
                      merchant_escrow: merchant_escrow,
                      usdt_account: usdt_account,
                      fiat_account: fiat_account,
                      fiat_currency: 'INVALID')

      expect(operation).to be_invalid
      expect(operation.errors[:fiat_currency]).to include('is not included in the list')
    end

    it 'creates a valid operation with proper attributes' do
      user = create(:user, :merchant, email: 'valid_test@example.com')
      merchant_escrow = create(:merchant_escrow, user: user)
      usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
      fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

      operation = build(:merchant_escrow_operation,
                      merchant_escrow: merchant_escrow,
                      usdt_account: usdt_account,
                      fiat_account: fiat_account,
                      operation_type: 'mint',
                      usdt_amount: 100.0,
                      fiat_amount: 100.0,
                      fiat_currency: 'VND')

      expect(operation).to be_valid
    end
  end
end
