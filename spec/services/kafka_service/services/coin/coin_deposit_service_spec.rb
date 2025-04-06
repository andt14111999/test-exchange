require 'rails_helper'

RSpec.describe KafkaService::Services::Coin::CoinDepositService, type: :service do
  let(:service) { described_class.new }
  let(:producer) { instance_double(KafkaService::Base::Producer) }
  let(:user_id) { 1 }
  let(:coin) { 'BTC' }
  let(:account_key) { 'test_account_key' }
  let(:amount) { 1.0 }
  let(:coin_account) { create(:coin_account, layer: 'bitcoin', address: '0xabc', coin_currency: coin.downcase) }
  let(:deposit) { create(:coin_deposit, coin_account: coin_account, tx_hash: '0x123') }
  let(:identifier) { "deposit-#{deposit.id}" }
  let(:timestamp) { 1743916726000 }

  before do
    allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
    allow(producer).to receive(:send_message)
    allow(producer).to receive(:send_messages_batch)
    allow(SecureRandom).to receive(:uuid).and_return('test-uuid')
    allow(Time).to receive(:current).and_return(Time.at(timestamp / 1000))
  end

  describe '#create' do
    it 'sends deposit create event with correct data' do
      expect(producer).to receive(:send_message).with(
        topic: KafkaService::Config::Topics::COIN_DEPOSIT,
        key: identifier,
        payload: {
          identifier: identifier,
          operationType: KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
          actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
          actionId: 'test-uuid',
          eventId: 'test-uuid',
          userId: user_id,
          status: 'pending',
          accountKey: account_key,
          amount: amount,
          coin: coin,
          txHash: deposit.tx_hash,
          layer: coin_account.layer,
          depositAddress: coin_account.address,
          timestamp: timestamp
        }
      )

      service.create(
        user_id: user_id,
        coin: coin,
        account_key: account_key,
        deposit: deposit,
        amount: amount
      )
    end
  end

  describe '#create_batch' do
    let(:deposits) do
      [
        {
          identifier: identifier,
          user_id: user_id,
          coin: coin,
          account_key: account_key,
          deposit: deposit,
          amount: amount
        }
      ]
    end

    let(:expected_messages) do
      [
        {
          topic: KafkaService::Config::Topics::COIN_DEPOSIT,
          key: user_id,
          payload: {
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
            actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
            actionId: 'test-uuid',
            userId: user_id,
            status: 'pending',
            accountKey: account_key,
            amount: amount,
            coin: coin,
            txHash: deposit.tx_hash,
            layer: coin_account.layer,
            depositAddress: coin_account.address
          }
        }
      ]
    end

    it 'sends batch deposit create events with correct data' do
      expect(producer).to receive(:send_messages_batch).with(expected_messages, 1000)
      service.create_batch(deposits)
    end

    it 'sends batch deposit create events with custom batch size' do
      batch_size = 500
      expect(producer).to receive(:send_messages_batch).with(expected_messages, batch_size)
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
      identifier = service.send(:generate_deposit_identifier, deposit_id: deposit.id)
      expect(identifier).to eq("deposit-#{deposit.id}")
    end
  end

  describe '#build_deposit_data' do
    it 'builds deposit data with correct attributes' do
      data = service.send(:build_deposit_data,
        identifier: identifier,
        user_id: user_id,
        coin: coin,
        account_key: account_key,
        deposit: deposit,
        amount: amount
      )

      expect(data).to eq({
        identifier: identifier,
        operationType: KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
        actionType: KafkaService::Config::ActionTypes::COIN_TRANSACTION,
        actionId: 'test-uuid',
        userId: user_id,
        status: 'pending',
        accountKey: account_key,
        amount: amount,
        coin: coin,
        txHash: deposit.tx_hash,
        layer: coin_account.layer,
        depositAddress: coin_account.address
      })
    end
  end
end
