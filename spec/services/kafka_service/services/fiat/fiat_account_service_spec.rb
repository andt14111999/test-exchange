require 'rails_helper'

RSpec.describe KafkaService::Services::Fiat::FiatAccountService, type: :service do
  let(:producer) { instance_double(KafkaService::Base::Producer) }
  let(:service) { described_class.new }
  let(:user_id) { 1 }
  let(:currency) { 'VND' }
  let(:account_id) { 123 }
  let(:account_key) { "#{user_id}-fiat-#{account_id}" }

  before do
    allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
    allow(producer).to receive(:send_message)

    # Stub account key builder
    allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_fiat_account_key)
      .with(user_id: user_id, account_id: account_id)
      .and_return(account_key)
  end

  describe '#create' do
    it 'sends create account event with correct data' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT,
        key: account_key,
        data: {
          operationType: KafkaService::Config::OperationTypes::COIN_ACCOUNT_CREATE,
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          actionId: account_id,
          userId: user_id,
          coin: currency,
          accountKey: account_key
        }
      )

      service.create(user_id: user_id, currency: currency, account_id: account_id)
    end

    it 'handles nil account_id' do
      nil_account_id = nil
      nil_account_key = "#{user_id}-fiat-#{nil_account_id}"

      allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_fiat_account_key)
        .with(user_id: user_id, account_id: nil_account_id)
        .and_return(nil_account_key)

      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT,
        key: nil_account_key,
        data: {
          operationType: KafkaService::Config::OperationTypes::COIN_ACCOUNT_CREATE,
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          actionId: nil_account_id,
          userId: user_id,
          coin: currency,
          accountKey: nil_account_key
        }
      )

      service.create(user_id: user_id, currency: currency)
    end
  end

  describe '#query_balance' do
    it 'sends query balance event with correct data' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_QUERY,
        key: account_key,
        data: {
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          actionId: account_id,
          operationType: KafkaService::Config::OperationTypes::BALANCE_QUERY,
          accountKey: account_key
        }
      )

      service.query_balance(user_id: user_id, account_id: account_id)
    end
  end

  describe '#reset_balance' do
    it 'sends reset balance event with correct data' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_RESET,
        key: account_key,
        data: {
          accountKey: account_key
        }
      )

      service.reset_balance(user_id: user_id, account_id: account_id)
    end
  end
end
