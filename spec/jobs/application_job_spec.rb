require 'rails_helper'

RSpec.describe ApplicationJob, type: :job do
  describe 'inheritance' do
    it 'inherits from ActiveJob::Base' do
      expect(described_class.superclass).to eq(ActiveJob::Base)
    end
  end

  describe 'default queue' do
    it 'uses Sidekiq queue adapter' do
      expect(described_class.queue_adapter).to be_an_instance_of(ActiveJob::QueueAdapters::SidekiqAdapter)
    end
  end

  describe 'retry behavior' do
    it 'does not retry on deadlock by default' do
      expect(described_class.instance_methods).not_to include(:retry_on)
    end
  end

  describe 'error handling' do
    it 'does not discard on deserialization error by default' do
      expect(described_class.instance_methods).not_to include(:discard_on)
    end
  end
end
