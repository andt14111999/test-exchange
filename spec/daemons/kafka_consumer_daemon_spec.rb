# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Daemons::KafkaConsumerDaemon, type: :daemon do
  let(:manager) { instance_double(KafkaService::ConsumerManager) }

  before do
    allow(KafkaService::ConsumerManager).to receive(:new).and_return(manager)
    allow(manager).to receive(:start)
    allow(manager).to receive(:stop)
    allow(Rails.logger).to receive(:info)
    allow(Kernel).to receive(:exit)
  end

  describe '.run' do
    it 'creates a new instance and calls run' do
      expect(described_class).to receive(:new).and_call_original
      expect_any_instance_of(described_class).to receive(:run)

      described_class.run
    end
  end

  describe '#run' do
    it 'sets up signal handlers and starts the manager' do
      daemon = described_class.new
      expect(daemon).to receive(:setup_signal_handlers)
      expect(daemon).to receive(:start_manager)

      # Mock the infinite loop to prevent hanging
      allow(daemon).to receive(:loop).and_yield
      allow(daemon).to receive(:sleep)

      daemon.run
    end

    it 'logs that the daemon has started' do
      daemon = described_class.new

      # Mock the infinite loop to prevent hanging
      allow(daemon).to receive(:loop).and_yield
      allow(daemon).to receive(:sleep)

      daemon.run

      expect(Rails.logger).to have_received(:info).with('Kafka consumer daemon started')
    end
  end

  describe 'signal handling' do
    it 'sets up signal handlers' do
      daemon = described_class.new

      # Mock Signal.trap to capture both calls
      signal_handlers = {}
      allow(Signal).to receive(:trap) do |signal, &block|
        signal_handlers[signal] = block
      end

      daemon.send(:setup_signal_handlers)

      expect(signal_handlers.keys).to contain_exactly('TERM', 'INT')
      expect(signal_handlers['TERM']).to be_a(Proc)
      expect(signal_handlers['INT']).to be_a(Proc)
    end
  end

  describe '#start_manager' do
    it 'creates and starts a new consumer manager' do
      daemon = described_class.new
      daemon.send(:start_manager)

      expect(KafkaService::ConsumerManager).to have_received(:new)
      expect(manager).to have_received(:start)
    end
  end

  describe '#shutdown' do
    it 'logs shutdown message and stops the manager' do
      daemon = described_class.new
      daemon.instance_variable_set(:@manager, manager)
      daemon.send(:shutdown)

      expect(Rails.logger).to have_received(:info).with('Shutting down Kafka consumer daemon...')
      expect(manager).to have_received(:stop)
      expect(Kernel).to have_received(:exit)
    end

    it 'handles case when manager is nil' do
      daemon = described_class.new
      daemon.instance_variable_set(:@manager, nil)
      daemon.send(:shutdown)

      expect(Rails.logger).to have_received(:info).with('Shutting down Kafka consumer daemon...')
      expect(manager).not_to have_received(:stop)
      expect(Kernel).to have_received(:exit)
    end
  end
end
