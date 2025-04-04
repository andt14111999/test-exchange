# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Handlers::CoinWithdrawalHandler, type: :service do
  let(:handler) { described_class.new }
  let(:identifier) { SecureRandom.uuid }

  describe '#handle' do
    context 'when operation type is COIN_WITHDRAWAL_CREATE' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE,
          'identifier' => identifier
        }
      end

      it 'processes withdrawal creation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal create: #{identifier}")
        handler.handle(payload)
      end
    end

    context 'when operation type is COIN_WITHDRAWAL_RELEASING' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_RELEASING,
          'identifier' => identifier
        }
      end

      it 'processes withdrawal releasing' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal releasing: #{identifier}")
        handler.handle(payload)
      end
    end

    context 'when operation type is COIN_WITHDRAWAL_FAILED' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_FAILED,
          'identifier' => identifier
        }
      end

      it 'processes withdrawal failure' do
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
        expect(Rails.logger).not_to receive(:info)
        handler.handle(payload)
      end
    end

    context 'when payload is missing identifier' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE
        }
      end

      it 'logs with nil identifier' do
        expect(Rails.logger).to receive(:info).with('Processing withdrawal create: ')
        handler.handle(payload)
      end
    end
  end

  describe 'inheritance' do
    it 'inherits from BaseHandler' do
      expect(described_class.superclass).to eq(KafkaService::Handlers::BaseHandler)
    end
  end
end
