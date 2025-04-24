# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Services::Coin::CoinWithdrawalService, type: :service do
  describe '#create' do
    it 'sends a withdrawal create event with correct data' do
      service = described_class.new
      identifier = SecureRandom.uuid
      user_id = 1
      coin = 'BTC'
      account_key = 'account-123'
      amount = '100.0'
      status = 'pending'
      fee = 0.1

      allow(SecureRandom).to receive_messages(uuid: 'mocked-action-id', hex: 'mocked-hex')

      expected_data = {
        identifier: identifier,
        operationType: KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE,
        actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
        actionId: 'mocked-action-id',
        userId: user_id,
        status: status,
        accountKey: account_key,
        amount: amount,
        coin: coin,
        txHash: 'tx-mocked-hex',
        layer: 'L1',
        destinationAddress: 'address-mocked-hex',
        fee: fee
      }

      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::COIN_WITHDRAW,
        key: identifier,
        data: expected_data
      )

      service.create(
        identifier: identifier,
        status: status,
        user_id: user_id,
        coin: coin,
        account_key: account_key,
        amount: amount,
        fee: fee
      )
    end
  end

  describe '#update_status' do
    it 'sends a status update event with correct data' do
      service = described_class.new
      identifier = SecureRandom.uuid
      operation_type = KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_RELEASING

      allow(SecureRandom).to receive(:uuid).and_return('mocked-action-id')

      expected_data = {
        operationType: operation_type,
        actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
        actionId: 'mocked-action-id',
        identifier: identifier
      }

      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::COIN_WITHDRAW,
        key: identifier,
        data: expected_data
      )

      service.update_status(
        identifier: identifier,
        operation_type: operation_type
      )
    end

    it 'handles different operation types' do
      service = described_class.new
      identifier = SecureRandom.uuid
      operation_types = [
        KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_RELEASING,
        KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_FAILED
      ]

      allow(SecureRandom).to receive(:uuid).and_return('mocked-action-id')

      operation_types.each do |operation_type|
        expected_data = {
          operationType: operation_type,
          actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
          actionId: 'mocked-action-id',
          identifier: identifier
        }

        expect(service).to receive(:send_event).with(
          topic: KafkaService::Config::Topics::COIN_WITHDRAW,
          key: identifier,
          data: expected_data
        )

        service.update_status(
          identifier: identifier,
          operation_type: operation_type
        )
      end
    end
  end

  describe 'inheritance' do
    it 'inherits from KafkaService::Base::Service' do
      expect(described_class.superclass).to eq(KafkaService::Base::Service)
    end
  end
end
