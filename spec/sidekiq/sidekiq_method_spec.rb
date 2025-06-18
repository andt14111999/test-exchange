# frozen_string_literal: true

require 'rails_helper'

RSpec.describe SidekiqMethod do
  describe '.enqueue_to' do
    it 'enqueues a job with the correct parameters' do
      record = create(:coin_withdrawal)
      method_name = :create_operations

      expect {
        described_class.enqueue_to('test_queue', record, method_name)
      }.to change(described_class.jobs, :size).by(1)

      job = described_class.jobs.last
      expect(job['args']).to eq([ record.class.name, record.id, method_name.to_s ])
    end
  end

  describe '#perform' do
    it 'calls the specified method on the record with the correct arguments' do
      record = create(:coin_withdrawal)
      method_name = :create_operations

      allow(CoinWithdrawal).to receive(:find).with(record.id).and_return(record)
      expect(record).to receive(method_name)

      described_class.new.perform(record.class.name, record.id, method_name.to_s)
    end
  end
end
