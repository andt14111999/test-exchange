# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CategorizedAccount, type: :model do
  let(:test_account_class) do
    Class.new(ApplicationRecord) do
      include CategorizedAccount
      self.table_name = 'test_accounts'
    end
  end

  before do
    ActiveRecord::Base.connection.create_table :test_accounts, force: true do |t|
      t.string :account_type
      t.string :address
      t.timestamps
    end
    stub_const('TestAccount', test_account_class)
  end

  after do
    ActiveRecord::Base.connection.drop_table :test_accounts
  end

  describe 'class methods' do
    describe '.safe_take_or_create' do
      it 'returns existing record if present' do
        existing_account = TestAccount.create!(account_type: 'main')
        allow(TestAccount).to receive(:take).and_return(existing_account)

        result = TestAccount.safe_take_or_create

        expect(result).to eq(existing_account)
      end

      it 'creates new record if none exists' do
        new_account = TestAccount.new(account_type: 'main')
        allow(TestAccount).to receive_messages(
          take: nil,
          first_or_create: new_account
        )
        allow(RedisMutex).to receive(:with_lock).and_yield

        result = TestAccount.safe_take_or_create

        expect(result).to eq(new_account)
      end

      it 'handles race condition with RecordNotUnique error from RedisMutex' do
        allow(TestAccount).to receive(:take).and_return(nil)
        allow(RedisMutex).to receive(:with_lock).and_raise(ActiveRecord::RecordNotUnique)
        existing_account = TestAccount.create!(account_type: 'main')
        allow(TestAccount).to receive(:take).and_return(existing_account)

        result = TestAccount.safe_take_or_create

        expect(result).to eq(existing_account)
      end

      it 'handles race condition with RecordNotUnique error from first_or_create' do
        # First attempt: take returns nil
        allow(TestAccount).to receive(:take).and_return(nil)
        allow(RedisMutex).to receive(:with_lock).and_yield

        # Create a record that will be returned by the second take call
        existing_account = TestAccount.create!(account_type: 'main')

        # Store original method
        original_method = TestAccount.method(:first_or_create)

        # Override first_or_create to raise RecordNotUnique
        TestAccount.define_singleton_method(:first_or_create) do |*args|
          raise ActiveRecord::RecordNotUnique
        end

        # Second attempt: take returns the existing account
        allow(TestAccount).to receive(:take).and_return(existing_account)

        result = TestAccount.safe_take_or_create
        expect(result).to eq(existing_account)

        # Restore original method
        TestAccount.define_singleton_method(:first_or_create, original_method)
      end

      it 'handles race condition with RecordNotUnique error from both RedisMutex and first_or_create' do
        allow(TestAccount).to receive(:take).and_return(nil)
        allow(RedisMutex).to receive(:with_lock).and_raise(ActiveRecord::RecordNotUnique)
        allow(TestAccount).to receive(:first_or_create).and_raise(ActiveRecord::RecordNotUnique)
        existing_account = TestAccount.create!(account_type: 'main')
        allow(TestAccount).to receive(:take).and_return(existing_account)

        result = TestAccount.safe_take_or_create

        expect(result).to eq(existing_account)
      end
    end

    describe 'account type scopes' do
      it 'returns intermediate accounts' do
        intermediate = TestAccount.create!(account_type: 'intermediate')
        TestAccount.create!(account_type: 'main')

        expect(TestAccount.intermediate_all).to contain_exactly(intermediate)
      end

      it 'returns main accounts' do
        main = TestAccount.create!(account_type: 'main')
        TestAccount.create!(account_type: 'deposit')

        expect(TestAccount.main_all).to contain_exactly(main)
      end

      it 'returns deposit accounts' do
        deposit = TestAccount.create!(account_type: 'deposit')
        TestAccount.create!(account_type: 'main')

        expect(TestAccount.deposit_all).to contain_exactly(deposit)
      end
    end

    describe '.main' do
      it 'returns or creates main account' do
        allow(TestAccount).to receive(:main_all).and_return(TestAccount.where(account_type: 'main'))
        main_account = TestAccount.create!(account_type: 'main')

        expect(TestAccount.main).to eq(main_account)
      end
    end

    describe '.deposit' do
      it 'returns last deposit account if exists' do
        deposit_account = TestAccount.create!(account_type: 'deposit')
        TestAccount.create!(account_type: 'main')

        expect(TestAccount.deposit).to eq(deposit_account)
      end

      it 'returns last main account if no deposit account exists' do
        main_account = TestAccount.create!(account_type: 'main')

        expect(TestAccount.deposit).to eq(main_account)
      end

      it 'creates new deposit account if no deposit or main account exists' do
        new_account = TestAccount.new(account_type: 'deposit')
        allow(TestAccount).to receive(:safe_take_or_create).and_return(new_account)

        expect(TestAccount.deposit).to eq(new_account)
      end
    end

    describe '.deposit_account_existed?' do
      it 'returns true if deposit account exists' do
        TestAccount.create!(account_type: 'deposit')

        expect(TestAccount.deposit_account_existed?).to be true
      end

      it 'returns true if main account exists' do
        TestAccount.create!(account_type: 'main')

        expect(TestAccount.deposit_account_existed?).to be true
      end

      it 'returns false if neither deposit nor main account exists' do
        TestAccount.create!(account_type: 'intermediate')

        expect(TestAccount.deposit_account_existed?).to be false
      end
    end

    describe '.deposit_address_existed?' do
      it 'returns true if deposit account has address' do
        TestAccount.create!(account_type: 'deposit', address: 'deposit_address')

        expect(TestAccount.deposit_address_existed?).to be true
      end

      it 'returns true if main account has address' do
        TestAccount.create!(account_type: 'main', address: 'main_address')

        expect(TestAccount.deposit_address_existed?).to be true
      end

      it 'returns false if no accounts have addresses' do
        TestAccount.create!(account_type: 'deposit', address: nil)
        TestAccount.create!(account_type: 'main', address: nil)

        expect(TestAccount.deposit_address_existed?).to be false
      end
    end

    describe '.intermediate' do
      it 'returns or creates intermediate account' do
        allow(TestAccount).to receive(:intermediate_all).and_return(TestAccount.where(account_type: 'intermediate'))
        intermediate_account = TestAccount.create!(account_type: 'intermediate')

        expect(TestAccount.intermediate).to eq(intermediate_account)
      end
    end
  end

  describe 'instance methods' do
    describe '#main?' do
      it 'returns true for main account' do
        account = TestAccount.new(account_type: 'main')

        expect(account.main?).to be true
      end

      it 'returns false for non-main account' do
        account = TestAccount.new(account_type: 'deposit')

        expect(account.main?).to be false
      end
    end

    describe '#payable?' do
      it 'returns true for payable account' do
        account = TestAccount.new(account_type: 'payable')

        expect(account.payable?).to be true
      end

      it 'returns false for non-payable account' do
        account = TestAccount.new(account_type: 'main')

        expect(account.payable?).to be false
      end
    end

    describe '#intermediate?' do
      it 'returns true for intermediate account' do
        account = TestAccount.new(account_type: 'intermediate')

        expect(account.intermediate?).to be true
      end

      it 'returns false for non-intermediate account' do
        account = TestAccount.new(account_type: 'main')

        expect(account.intermediate?).to be false
      end
    end

    describe '#can_hold_balance?' do
      it 'returns true for main account' do
        account = TestAccount.new(account_type: 'main')

        expect(account.can_hold_balance?).to be true
      end

      it 'returns true for intermediate account' do
        account = TestAccount.new(account_type: 'intermediate')

        expect(account.can_hold_balance?).to be true
      end

      it 'returns false for other account types' do
        account = TestAccount.new(account_type: 'deposit')

        expect(account.can_hold_balance?).to be false
      end
    end
  end
end
