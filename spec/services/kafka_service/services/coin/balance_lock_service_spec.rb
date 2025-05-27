# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Services::Coin::BalanceLockService, type: :service do
  describe '#create' do
    it 'sends balance lock create event with correct data' do
      service = described_class.new
      lock_id = '123'
      account_keys = [ 'account-key-1', 'account-key-2' ]
      identifier = 'balance-lock-123'

      allow(SecureRandom).to receive(:uuid).and_return('mocked-action-id')

      expected_data = {
        lockId: lock_id,
        accountKeys: account_keys,
        identifier: identifier,
        operationType: KafkaService::Config::OperationTypes::BALANCES_LOCK_CREATE,
        actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
        actionId: 'mocked-action-id'
      }

      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::BALANCES_LOCK,
        key: identifier,
        data: expected_data
      )

      service.create(
        lock_id: lock_id,
        account_keys: account_keys,
        identifier: identifier
      )
    end

    it 'sends event to the correct Kafka topic' do
      service = described_class.new
      lock_id = '123'
      account_keys = [ 'account-key-1', 'account-key-2' ]
      identifier = 'balance-lock-123'

      allow(SecureRandom).to receive(:uuid).and_return('mocked-action-id')

      producer = instance_double(KafkaService::Base::Producer)
      allow(service).to receive_messages(producer: producer, default_event_data: {
        eventId: 'mocked-event-id',
        timestamp: 1234567890000
      })

      expect(producer).to receive(:send_message).with(
        hash_including(topic: KafkaService::Config::Topics::BALANCES_LOCK),
        any_args
      )

      service.create(
        lock_id: lock_id,
        account_keys: account_keys,
        identifier: identifier
      )
    end
  end

  describe '#unlock' do
    it 'sends balance unlock event with correct data' do
      service = described_class.new
      lock_id = '123'
      identifier = 'balance-lock-123'

      allow(SecureRandom).to receive(:uuid).and_return('mocked-action-id')

      expected_data = {
        lockId: lock_id,
        identifier: identifier,
        operationType: KafkaService::Config::OperationTypes::BALANCES_LOCK_RELEASE,
        actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
        actionId: 'mocked-action-id'
      }

      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::BALANCES_LOCK,
        key: identifier,
        data: expected_data
      )

      service.unlock(
        lock_id: lock_id,
        identifier: identifier
      )
    end

    it 'sends event to the correct Kafka topic' do
      service = described_class.new
      lock_id = '123'
      identifier = 'balance-lock-123'

      allow(SecureRandom).to receive(:uuid).and_return('mocked-action-id')

      producer = instance_double(KafkaService::Base::Producer)
      allow(service).to receive_messages(producer: producer, default_event_data: {
        eventId: 'mocked-event-id',
        timestamp: 1234567890000
      })

      expect(producer).to receive(:send_message).with(
        hash_including(topic: KafkaService::Config::Topics::BALANCES_LOCK),
        any_args
      )

      service.unlock(
        lock_id: lock_id,
        identifier: identifier
      )
    end
  end

  describe 'error handling' do
    context 'in the BalanceLock model' do
      it 'handles errors when sending lock events to Kafka' do
        # Create a real balance lock
        balance_lock = create(:balance_lock)

        # Mock the Kafka service to raise an error
        kafka_service = instance_double(described_class)
        allow(described_class).to receive(:new).and_return(kafka_service)
        allow(kafka_service).to receive(:create).and_raise(StandardError.new('Connection error'))

        # Capture the log output
        expect(Rails.logger).to receive(:error).with(/Failed to send balance lock event to Kafka: Connection error/)

        # Call the private method
        BalanceLock.instance_method(:send_event_balance_lock_to_kafka).bind(balance_lock).call
      end

      it 'handles errors when sending unlock events to Kafka' do
        # Create a real balance lock
        balance_lock = create(:balance_lock, :locked)

        # Mock the Kafka service to raise an error
        kafka_service = instance_double(described_class)
        allow(described_class).to receive(:new).and_return(kafka_service)
        allow(kafka_service).to receive(:unlock).and_raise(StandardError.new('Connection error'))

        # Capture the log output
        expect(Rails.logger).to receive(:error).with(/Failed to send balance unlock event to Kafka: Connection error/)

        # Call the private method
        BalanceLock.instance_method(:send_event_balance_unlock_to_kafka).bind(balance_lock).call
      end
    end
  end

  describe 'inheritance' do
    it 'inherits from KafkaService::Base::Service' do
      expect(described_class.superclass).to eq(KafkaService::Base::Service)
    end
  end

  describe 'initialization' do
    it 'initializes a logger and producer' do
      logger = instance_double(Logger)
      allow(Logger).to receive(:new).and_return(logger)

      producer = instance_double(KafkaService::Base::Producer)
      allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)

      expect(logger).to receive(:info).with(/Sending event to/).once
      expect(producer).to receive(:send_message).once

      service = described_class.new
      service.create(
        lock_id: '123',
        account_keys: [ 'account-key' ],
        identifier: 'identifier'
      )
    end
  end
end
