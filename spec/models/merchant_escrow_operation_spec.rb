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
             operation_type: 'freeze',
             usdt_amount: 100.0,
             fiat_amount: 100.0)

      create(:merchant_escrow_operation,
             merchant_escrow: test_data[:unfreeze][:escrow],
             usdt_account: test_data[:unfreeze][:usdt_account],
             fiat_account: test_data[:unfreeze][:fiat_account],
             operation_type: 'unfreeze',
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

    it 'returns freeze operations' do
      expect(described_class.freeze_operations.count).to eq(1)
    end

    it 'returns unfreeze operations' do
      expect(described_class.unfreeze_operations.count).to eq(1)
    end

    it 'returns mint operations' do
      expect(described_class.mint_operations.count).to eq(1)
    end

    it 'returns burn operations' do
      expect(described_class.burn_operations.count).to eq(1)
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
                       operation_type: 'freeze',
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
                       operation_type: 'freeze',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

      expect(operation.status).to eq('failed')
      expect(operation.status_explanation).to eq('Insufficient balance')
    end
  end

  describe 'operations' do
    describe 'freeze operation' do
      it 'freezes USDT balance successfully' do
        user = create(:user, :merchant, email: 'test7@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

        operation = create(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account,
                         fiat_account: fiat_account,
                         operation_type: 'freeze',
                         usdt_amount: 100.0,
                         fiat_amount: 100.0)

        expect(operation.status).to eq('completed')
        expect(usdt_account.reload.balance).to eq(100.0)
        expect(usdt_account.reload.frozen_balance).to eq(100.0)
      end
    end

    describe 'unfreeze operation' do
      it 'unfreezes USDT balance successfully' do
        user = create(:user, :merchant, email: 'test8@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 100.0, frozen_balance: 100.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

        operation = create(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account,
                         fiat_account: fiat_account,
                         operation_type: 'unfreeze',
                         usdt_amount: 100.0,
                         fiat_amount: 100.0)

        expect(operation.status).to eq('completed')
        expect(usdt_account.reload.balance).to eq(200.0)
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
        expect(fiat_account.reload.balance.to_i).to eq(150)
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

        expect(operation.status).to eq('failed')
        expect(operation.status_explanation).to eq('Insufficient frozen balance')
      end

      it 'decreases both balance and frozen balance when burn operation succeeds' do
        user = create(:user, :merchant, email: 'test15@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0, frozen_balance: 100.0)

        operation = create(:merchant_escrow_operation,
                         merchant_escrow: merchant_escrow,
                         usdt_account: usdt_account,
                         fiat_account: fiat_account,
                         operation_type: 'burn',
                         usdt_amount: 100.0,
                         fiat_amount: 50.0)

        expect(operation.status).to eq('completed')
        expect(fiat_account.reload.balance).to eq(150.0)
        expect(fiat_account.reload.frozen_balance).to eq(50.0)
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
                       operation_type: 'burn',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

        expect(operation.status).to eq('failed')
        expect(operation.status_explanation).to eq('Insufficient frozen balance')
      end

      it 'fails when fiat amount exceeds balance for decrease_balance action' do
        user = create(:user, :merchant, email: 'test20@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 50.0)

        operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'freeze',
                       usdt_amount: 100.0,
                       fiat_amount: 100.0)

        expect { operation.send(:validate_fiat_balance, fiat_account, :decrease_balance) }
          .to raise_error('Insufficient balance')
      end
    end

    describe 'balance update' do
      it 'increases balance for mint operation' do
        user = create(:user, :merchant, email: 'test17@example.com')
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

        expect(operation.status).to eq('completed')
        expect(fiat_account.reload.balance).to eq(250.0)
      end

      it 'decreases balance for non-burn operation' do
        user = create(:user, :merchant, email: 'test18@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0, frozen_balance: 100.0)

        operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'burn',
                       usdt_amount: 100.0,
                       fiat_amount: 50.0)

        expect(operation.status).to eq('completed')
        expect(fiat_account.reload.balance).to eq(150.0)
      end

      it 'decreases balance for decrease_balance action' do
        user = create(:user, :merchant, email: 'test19@example.com')
        merchant_escrow = create(:merchant_escrow, user: user)
        usdt_account = create(:coin_account, :usdt_main, balance: 200.0)
        fiat_account = create(:fiat_account, :vnd, user: user, balance: 200.0)

        operation = create(:merchant_escrow_operation,
                       merchant_escrow: merchant_escrow,
                       usdt_account: usdt_account,
                       fiat_account: fiat_account,
                       operation_type: 'freeze',
                       usdt_amount: 100.0,
                       fiat_amount: 50.0)

        operation.send(:update_fiat_balance, fiat_account, :decrease_balance)
        fiat_account.save!
        expect(fiat_account.reload.balance).to eq(150.0)
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
end
