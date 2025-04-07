require 'rails_helper'

RSpec.describe KafkaService::Services::Coin::CoinAccountService, type: :service do
  let(:producer) { instance_double(KafkaService::Base::Producer) }
  let(:service) { described_class.new }
  let(:user_id) { 1 }
  let(:coin) { 'BTC' }
  let(:account_key) { 'test_account_key' }

  before do
    allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
    allow(producer).to receive(:send_message)
    # Bypass the global mock in rails_helper.rb
    allow_any_instance_of(described_class).to receive(:create).and_call_original
  end

  describe '#create' do
    it 'sends create account event with correct data' do
      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT,
        key: account_key,
        payload: hash_including(
          operationType: KafkaService::Config::OperationTypes::COIN_ACCOUNT_CREATE,
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          userId: user_id,
          coin: coin,
          accountKey: account_key
        )
      )

      service.create(user_id: user_id, coin: coin, account_key: account_key)
    end

    it 'sends create account event without account_key' do
      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT,
        key: nil,
        payload: hash_including(
          operationType: KafkaService::Config::OperationTypes::COIN_ACCOUNT_CREATE,
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          userId: user_id,
          coin: coin,
          accountKey: nil
        )
      )

      service.create(user_id: user_id, coin: coin)
    end
  end

  describe '#query_balance' do
    it 'sends query balance event with correct data' do
      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_QUERY,
        key: account_key,
        payload: hash_including(
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          actionId: instance_of(String),
          operationType: KafkaService::Config::OperationTypes::BALANCE_QUERY,
          accountKey: account_key
        )
      )

      service.query_balance(account_key: account_key)
    end

    it 'generates unique action_id for each query' do
      first_action_id = nil
      second_action_id = nil

      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_QUERY,
        key: account_key,
        payload: hash_including(
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          operationType: KafkaService::Config::OperationTypes::BALANCE_QUERY,
          accountKey: account_key
        )
      ) do |args|
        first_action_id = args[:payload][:actionId]
      end

      service.query_balance(account_key: account_key)

      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_QUERY,
        key: account_key,
        payload: hash_including(
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          operationType: KafkaService::Config::OperationTypes::BALANCE_QUERY,
          accountKey: account_key
        )
      ) do |args|
        second_action_id = args[:payload][:actionId]
      end

      service.query_balance(account_key: account_key)

      expect(first_action_id).not_to eq(second_action_id)
    end
  end

  describe '#reset_balance' do
    it 'sends reset balance event with correct data' do
      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_RESET,
        key: account_key,
        payload: hash_including(
          accountKey: account_key
        )
      )

      service.reset_balance(account_key: account_key)
    end
  end
end
