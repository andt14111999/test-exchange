# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Handlers::CoinWithdrawalHandler, type: :service do
  let(:handler) { described_class.new }
  let(:identifier) { SecureRandom.uuid }
  let(:payload) do
    {
      'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE,
      'identifier' => identifier,
      'amount' => '100.0',
      'currency' => 'BTC',
      'address' => '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
    }
  end

  describe '#handle' do
    context 'when operation type is COIN_WITHDRAWAL_CREATE' do
      it 'processes withdrawal creation' do
        expect(handler).to receive(:process_withdrawal_create).with(payload)
        handler.handle(payload)
      end

      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal create: #{identifier}")
        handler.handle(payload)
      end
    end

    context 'when operation type is COIN_WITHDRAWAL_RELEASING' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_RELEASING,
          'identifier' => identifier,
          'amount' => '100.0',
          'currency' => 'BTC',
          'address' => '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        }
      end

      it 'processes withdrawal releasing' do
        expect(handler).to receive(:process_withdrawal_releasing).with(payload)
        handler.handle(payload)
      end

      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal releasing: #{identifier}")
        handler.handle(payload)
      end
    end

    context 'when operation type is COIN_WITHDRAWAL_FAILED' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_FAILED,
          'identifier' => identifier,
          'amount' => '100.0',
          'currency' => 'BTC',
          'address' => '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
          'error' => 'Insufficient funds'
        }
      end

      it 'processes withdrawal failure' do
        expect(handler).to receive(:process_withdrawal_failed).with(payload)
        handler.handle(payload)
      end

      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal failed: #{identifier}")
        handler.handle(payload)
      end
    end

    context 'when operation type is unknown' do
      let(:payload) do
        {
          'operationType' => 'UNKNOWN_OPERATION',
          'identifier' => identifier
        }
      end

      it 'does not process anything' do
        expect(handler).not_to receive(:process_withdrawal_create)
        expect(handler).not_to receive(:process_withdrawal_releasing)
        expect(handler).not_to receive(:process_withdrawal_failed)
        expect(Rails.logger).not_to receive(:info)
        handler.handle(payload)
      end
    end

    context 'when payload is missing identifier' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE,
          'amount' => '100.0',
          'currency' => 'BTC'
        }
      end

      it 'logs with empty identifier' do
        expect(Rails.logger).to receive(:info).with('Processing withdrawal create: ')
        handler.handle(payload)
      end
    end

    context 'when payload is invalid' do
      let(:payload) { nil }

      it 'does not raise an error' do
        expect { handler.handle(payload) }.not_to raise_error
      end

      it 'does not process anything' do
        expect(handler).not_to receive(:process_withdrawal_create)
        expect(handler).not_to receive(:process_withdrawal_releasing)
        expect(handler).not_to receive(:process_withdrawal_failed)
        expect(Rails.logger).not_to receive(:info)
        handler.handle(payload)
      end
    end
  end

  describe 'private methods' do
    describe '#process_withdrawal_create' do
      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal create: #{identifier}")
        handler.send(:process_withdrawal_create, payload)
      end
    end

    describe '#process_withdrawal_releasing' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_RELEASING,
          'identifier' => identifier
        }
      end

      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal releasing: #{identifier}")
        handler.send(:process_withdrawal_releasing, payload)
      end
    end

    describe '#process_withdrawal_failed' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_FAILED,
          'identifier' => identifier,
          'error' => 'Insufficient funds'
        }
      end

      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal failed: #{identifier}")
        handler.send(:process_withdrawal_failed, payload)
      end
    end
  end

  describe 'inheritance' do
    it 'inherits from BaseHandler' do
      expect(described_class.superclass).to eq(KafkaService::Handlers::BaseHandler)
    end
  end
end
