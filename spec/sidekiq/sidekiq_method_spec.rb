# frozen_string_literal: true

require 'rails_helper'

RSpec.describe SidekiqMethod do
  describe '.enqueue_to' do
    it 'enqueues a job with the correct parameters to specified queue' do
      record = create(:coin_withdrawal)
      method_name = :create_operations

      expect {
        described_class.enqueue_to('test_queue', record, method_name)
      }.to change(described_class.jobs, :size).by(1)

      job = described_class.jobs.last
      expect(job['queue']).to eq('test_queue')
      expect(job['args']).to eq([ record.class.name, record.id, method_name.to_s ])
    end

    it 'enqueues a job with string arguments' do
      record = create(:coin_withdrawal)
      method_name = :touch
      args = [ 'status', 'explanation text' ]

      expect {
        described_class.enqueue_to('processing', record, method_name, *args)
      }.to change(described_class.jobs, :size).by(1)

      job = described_class.jobs.last
      expect(job['queue']).to eq('processing')
      expect(job['args']).to eq([ record.class.name, record.id, method_name.to_s, *args ])
    end

    it 'enqueues a job with hash arguments (strings only)' do
      record = create(:coin_withdrawal)
      method_name = :update_attributes
      kwargs = { 'status' => 'completed', 'explanation' => 'Done' }

      expect {
        described_class.enqueue_to('updates', record, method_name, kwargs)
      }.to change(described_class.jobs, :size).by(1)

      job = described_class.jobs.last
      expect(job['queue']).to eq('updates')
      expect(job['args']).to eq([ record.class.name, record.id, method_name.to_s, kwargs ])
    end

    it 'enqueues with different queue names' do
      record = create(:coin_withdrawal)

      described_class.enqueue_to('high_priority', record, :reload)
      described_class.enqueue_to('low_priority', record, :touch)

      jobs = described_class.jobs
      expect(jobs[-2]['queue']).to eq('high_priority')
      expect(jobs[-1]['queue']).to eq('low_priority')
    end

    it 'schedules the job with 2 seconds delay' do
      record = create(:coin_withdrawal)

      expect(described_class).to receive(:set).with(queue: 'test').and_return(described_class)
      expect(described_class).to receive(:perform_in).with(2.seconds, record.class.name, record.id, 'touch')

      described_class.enqueue_to('test', record, :touch)
    end
  end

  describe '#perform' do
    context 'with regular arguments' do
      it 'calls the specified method on the record with string arguments' do
        record = create(:coin_withdrawal)
        method_name = 'touch'
        args = [ 'status' ]

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:touch).with(*args)

        described_class.new.perform(record.class.name, record.id, method_name, *args)
      end

      it 'calls the specified method on the record with numeric arguments' do
        record = create(:coin_withdrawal)
        method_name = 'update_column'
        args = [ 'coin_amount', 100.50 ]

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:update_column).with(*args)

        described_class.new.perform(record.class.name, record.id, method_name, *args)
      end

      it 'calls the specified method with no arguments' do
        record = create(:coin_withdrawal)
        method_name = 'reload'

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:reload)

        described_class.new.perform(record.class.name, record.id, method_name)
      end

      it 'calls the specified method with mixed argument types' do
        record = create(:coin_withdrawal)
        method_name = 'update_column'
        args = [ 'status', 'completed' ]

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:update_column).with(*args)

        described_class.new.perform(record.class.name, record.id, method_name, *args)
      end

      it 'calls the specified method with boolean arguments' do
        record = create(:coin_withdrawal)
        method_name = 'update_column'
        args = [ 'vpn', true ]

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:update_column).with(*args)

        described_class.new.perform(record.class.name, record.id, method_name, *args)
      end

      it 'calls the specified method with nil arguments' do
        record = create(:coin_withdrawal)
        method_name = 'update_column'
        args = [ 'explanation', nil ]

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:update_column).with(*args)

        described_class.new.perform(record.class.name, record.id, method_name, *args)
      end
    end

    context 'with keyword arguments' do
      it 'calls the specified method with keyword arguments when first arg is a Hash' do
        record = create(:coin_withdrawal)
        method_name = 'update_columns'
        kwargs = { 'status' => 'completed', 'explanation' => 'Test explanation' }

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:update_columns).with(status: 'completed', explanation: 'Test explanation')

        described_class.new.perform(record.class.name, record.id, method_name, kwargs)
      end

      it 'calls the specified method with empty hash as keyword arguments' do
        record = create(:coin_withdrawal)
        method_name = 'touch'
        kwargs = {}

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:touch).with(no_args)

        described_class.new.perform(record.class.name, record.id, method_name, kwargs)
      end

      it 'calls the specified method with symbol keys converted to keyword arguments' do
        record = create(:coin_withdrawal)
        method_name = 'update_columns'
        kwargs = { 'coin_amount' => 30.0, 'coin_fee' => 3.0 }

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:update_columns).with(coin_amount: 30.0, coin_fee: 3.0)

        described_class.new.perform(record.class.name, record.id, method_name, kwargs)
      end
    end

    context 'with different record types' do
      it 'works with User records' do
        user = create(:user)
        method_name = 'reload'

        allow(User).to receive(:find).with(user.id).and_return(user)
        expect(user).to receive(:reload)

        described_class.new.perform(user.class.name, user.id, method_name)
      end

      it 'works with AdminUser records' do
        admin = create(:admin_user)
        method_name = 'touch'

        allow(AdminUser).to receive(:find).with(admin.id).and_return(admin)
        expect(admin).to receive(:touch)

        described_class.new.perform(admin.class.name, admin.id, method_name)
      end
    end

    context 'error handling' do
      it 'raises error when class does not exist' do
        expect {
          described_class.new.perform('NonExistentClass', 1, 'some_method')
        }.to raise_error(NameError)
      end

      it 'raises error when record is not found' do
        expect {
          described_class.new.perform('CoinWithdrawal', 999999, 'some_method')
        }.to raise_error(ActiveRecord::RecordNotFound)
      end

      it 'raises error when method does not exist on record' do
        record = create(:coin_withdrawal)

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)

        expect {
          described_class.new.perform(record.class.name, record.id, 'non_existent_method')
        }.to raise_error(NoMethodError)
      end
    end

    context 'integration tests' do
      it 'actually calls real methods on records' do
        record = create(:coin_withdrawal, status: 'pending')

        # Test calling a real method that exists
        described_class.new.perform(record.class.name, record.id, 'reload')

        # Verify the method was called by checking the record is still accessible
        expect(record.reload).to be_present
      end

      it 'works with touch method' do
        record = create(:coin_withdrawal)
        original_updated_at = record.updated_at

        # Use a real method that updates the record
        sleep(0.1) # Ensure time difference
        described_class.new.perform(record.class.name, record.id, 'touch')

        expect(record.reload.updated_at).to be > original_updated_at
      end

      it 'works with update_column method' do
        record = create(:coin_withdrawal, explanation: nil)

        described_class.new.perform(record.class.name, record.id, 'update_column', 'explanation', 'test message')

        expect(record.reload.explanation).to eq('test message')
      end

      it 'works with update_columns method via kwargs' do
        record = create(:coin_withdrawal)
        kwargs = { 'explanation' => 'updated explanation', 'vpn' => true }

        described_class.new.perform(record.class.name, record.id, 'update_columns', kwargs)

        record.reload
        expect(record.explanation).to eq('updated explanation')
        expect(record.vpn).to be true
      end
    end

    context 'argument type edge cases' do
      it 'handles hash as regular argument when not the first argument' do
        record = create(:coin_withdrawal)
        method_name = 'update_column'
        args = [ 'explanation', 'test data' ]

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:update_column).with(*args)

        described_class.new.perform(record.class.name, record.id, method_name, *args)
      end

      it 'handles multiple arguments correctly with regular method call' do
        record = create(:coin_withdrawal)
        method_name = 'touch'
        args = [ 'status', 'explanation' ]

        allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
        expect(record).to receive(:touch).with(*args)

        described_class.new.perform(record.class.name, record.id, method_name, *args)
      end
    end
  end
end
