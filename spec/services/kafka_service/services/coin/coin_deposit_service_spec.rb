require 'rails_helper'

RSpec.describe KafkaService::Services::Coin::CoinDepositService, type: :service do
  let(:service) { described_class.new }
  let(:producer) { instance_double(KafkaService::Base::Producer) }
  let(:coin_account) { create(:coin_account, layer: 'bitcoin', address: '0xabc', coin_currency: 'btc') }
  let(:deposit) { create(:coin_deposit, coin_account: coin_account, tx_hash: '0x123') }
  let(:deposit_data) do
    {
      user_id: 1,
      coin: 'BTC',
      account_key: 'test_account_key',
      amount: 1.0,
      coin_account: coin_account,
      deposit: deposit,
      identifier: "deposit-#{deposit.id}",
      timestamp: 1743916726000
    }
  end

  before do
    allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
    allow(producer).to receive(:send_message)
    allow(producer).to receive(:send_messages_batch)
    allow(SecureRandom).to receive(:uuid).and_return('test-uuid')
    allow(Time).to receive(:current).and_return(Time.at(deposit_data[:timestamp] / 1000))
  end

  describe '#create' do
    it 'sends deposit create event with correct data' do
      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_DEPOSIT,
        key: deposit_data[:identifier],
        payload: hash_including(
          identifier: deposit_data[:identifier],
          operationType: KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
          actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
          eventId: anything,
          userId: deposit_data[:user_id],
          status: 'pending',
          accountKey: deposit_data[:account_key],
          amount: deposit_data[:amount],
          coin: deposit_data[:coin],
          txHash: deposit_data[:deposit].tx_hash,
          layer: deposit_data[:coin_account].layer,
          depositAddress: deposit_data[:coin_account].address,
          timestamp: deposit_data[:timestamp]
        )
      )

      service.create(
        user_id: deposit_data[:user_id],
        coin: deposit_data[:coin],
        account_key: deposit_data[:account_key],
        deposit: deposit_data[:deposit],
        amount: deposit_data[:amount]
      )
    end
  end

  describe '#create_batch' do
    let(:deposits) do
      [
        {
          identifier: deposit_data[:identifier],
          user_id: deposit_data[:user_id],
          coin: deposit_data[:coin],
          account_key: deposit_data[:account_key],
          deposit: deposit_data[:deposit],
          amount: deposit_data[:amount]
        }
      ]
    end

    it 'sends batch deposit create events with correct data' do
      expect(producer).to receive(:send_messages_batch) do |messages, batch_size|
        expect(batch_size).to eq(1000)
        expect(messages.length).to eq(1)
        expect(messages[0][:topic]).to eq(KafkaService::Config::Topics::COIN_DEPOSIT)
        expect(messages[0][:key]).to eq(deposit_data[:user_id])
        expect(messages[0][:payload]).to include(
          identifier: deposit_data[:identifier],
          operationType: KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
          actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
          userId: deposit_data[:user_id],
          status: 'pending',
          accountKey: deposit_data[:account_key],
          amount: deposit_data[:amount],
          coin: deposit_data[:coin],
          txHash: deposit_data[:deposit].tx_hash
        )
      end

      service.create_batch(deposits)
    end

    it 'sends batch deposit create events with custom batch size' do
      batch_size = 500

      expect(producer).to receive(:send_messages_batch) do |messages, actual_batch_size|
        expect(actual_batch_size).to eq(batch_size)
        expect(messages.length).to eq(1)
        expect(messages[0][:topic]).to eq(KafkaService::Config::Topics::COIN_DEPOSIT)
        expect(messages[0][:key]).to eq(deposit_data[:user_id])
      end

      service.create_batch(deposits, batch_size)
    end

    it 'logs info message about batch size' do
      logger = instance_double(Logger)
      allow(Logger).to receive(:new).and_return(logger)
      allow(logger).to receive(:info)
      expect(logger).to receive(:info).with("Preparing batch deposit for 1 deposits")
      service = described_class.new
      service.create_batch(deposits)
    end
  end

  describe '#generate_deposit_identifier' do
    it 'generates correct identifier format' do
      identifier = service.send(:generate_deposit_identifier, deposit_id: deposit_data[:deposit].id)
      expect(identifier).to eq(deposit_data[:identifier])
    end
  end

  describe '#build_deposit_data' do
    it 'builds deposit data with correct attributes' do
      data = service.send(:build_deposit_data,
        identifier: deposit_data[:identifier],
        user_id: deposit_data[:user_id],
        coin: deposit_data[:coin],
        account_key: deposit_data[:account_key],
        deposit: deposit_data[:deposit],
        amount: deposit_data[:amount]
      )

      expect(data).to include(
        identifier: deposit_data[:identifier],
        operationType: KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
        actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
        userId: deposit_data[:user_id],
        status: 'pending',
        accountKey: deposit_data[:account_key],
        amount: deposit_data[:amount],
        coin: deposit_data[:coin],
        txHash: deposit_data[:deposit].tx_hash,
        layer: deposit_data[:coin_account].layer,
        depositAddress: deposit_data[:coin_account].address
      )

      # Verify actionId is present but don't check specific value
      expect(data).to have_key(:actionId)
    end
  end
end
