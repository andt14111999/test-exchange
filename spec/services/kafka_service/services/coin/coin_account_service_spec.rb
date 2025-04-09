require 'rails_helper'

RSpec.describe KafkaService::Services::Coin::CoinAccountService, type: :service do
  let(:producer) { instance_double(KafkaService::Base::Producer) }
  let(:service) { described_class.new }
  let(:user_id) { 1 }
  let(:coin) { 'BTC' }
  let(:account_id) { 123 }

  before do
    allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
    allow(producer).to receive(:send_message)
    # Bypass the global mock in rails_helper.rb
    allow_any_instance_of(described_class).to receive(:create).and_call_original

    # Stub account key builder
    allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_coin_account_key)
      .with(user_id: user_id, account_id: anything)
      .and_return("#{user_id}-coin-")
  end

  describe '#create' do
    it 'sends create account event with correct data' do
      expect(producer).to receive(:send_message) do |args|
        expect(args[:topic]).to eq(KafkaService::Config::Topics::COIN_ACCOUNT)
        expect(args[:payload]).to include(
          operationType: KafkaService::Config::OperationTypes::COIN_ACCOUNT_CREATE,
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          userId: user_id,
          coin: coin,
          accountKey: "#{user_id}-coin-"
        )
      end

      service.create(user_id: user_id, coin: coin)
    end
  end

  describe '#query_balance' do
    it 'sends query balance event with correct data' do
      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_QUERY,
        key: anything,
        payload: hash_including(
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          actionId: instance_of(String),
          operationType: KafkaService::Config::OperationTypes::BALANCE_QUERY
        )
      )

      service.query_balance(user_id: user_id, account_id: account_id)
    end

    it 'generates unique action_id for each query' do
      first_action_id = nil
      second_action_id = nil

      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_QUERY,
        key: anything,
        payload: hash_including(
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          operationType: KafkaService::Config::OperationTypes::BALANCE_QUERY
        )
      ) do |args|
        first_action_id = args[:payload][:actionId]
      end

      service.query_balance(user_id: user_id, account_id: account_id)

      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_QUERY,
        key: anything,
        payload: hash_including(
          actionType: KafkaService::Config::ActionTypes::COIN_ACCOUNT,
          operationType: KafkaService::Config::OperationTypes::BALANCE_QUERY
        )
      ) do |args|
        second_action_id = args[:payload][:actionId]
      end

      service.query_balance(user_id: user_id, account_id: account_id)

      expect(first_action_id).not_to eq(second_action_id)
    end
  end

  describe '#reset_balance' do
    it 'sends reset balance event with correct data' do
      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_ACCOUNT_RESET,
        key: anything,
        payload: hash_including(
          accountKey: anything
        )
      )

      service.reset_balance(user_id: user_id, account_id: account_id)
    end
  end
end
