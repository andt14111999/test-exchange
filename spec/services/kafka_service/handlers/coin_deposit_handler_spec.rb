require 'rails_helper'

RSpec.describe KafkaService::Handlers::CoinDepositHandler, type: :service do
  let(:handler) { described_class.new }
  let(:coin_account) { create(:coin_account) }
  let(:coin_deposit) { create(:coin_deposit) }
  let(:deposit_id) { coin_deposit.id.to_s }
  let(:identifier) { "deposit-#{deposit_id}" }
  let(:amount) { 1.0 }
  let(:coin) { 'BTC' }
  let(:tx_hash) { '0x123' }
  let(:out_index) { 0 }

  describe '#handle' do
    context 'when operation type is COIN_DEPOSIT_CREATE' do
      let(:payload) do
        {
          'object' => {
            'operationType' => KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
            'identifier' => identifier,
            'accountKey' => coin_account.id,
            'amount' => amount,
            'coin' => coin,
            'txHash' => tx_hash
          },
          'isSuccess' => true
        }
      end

      it 'processes deposit created successfully' do
        expect(handler).to receive(:process_deposit_created).with(payload)
        handler.handle(payload)
      end
    end
  end

  describe '#process_deposit_created' do
    let(:payload) do
      {
        'object' => {
          'operationType' => KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
          'identifier' => identifier,
          'accountKey' => coin_account.id,
          'amount' => amount,
          'coin' => coin,
          'txHash' => tx_hash
        },
        'isSuccess' => true
      }
    end

    it 'creates deposit operation when deposit exists and is successful' do
      allow(CoinDeposit).to receive(:find_by).with(id: deposit_id).and_return(coin_deposit)
      expect(CoinDepositOperation).to receive(:create!).with(
        coin_account: coin_account,
        coin_amount: amount,
        coin_currency: coin.downcase,
        coin_deposit: coin_deposit,
        coin_fee: 0,
        platform_fee: 0,
        tx_hash: tx_hash,
        out_index: out_index,
        status: 'completed'
      )

      handler.send(:process_deposit_created, payload)
    end

    it 'does not create deposit operation when deposit does not exist' do
      allow(CoinDeposit).to receive(:find_by).with(id: deposit_id).and_return(nil)
      expect(CoinDepositOperation).not_to receive(:create!)
      handler.send(:process_deposit_created, payload)
    end

    it 'does not create deposit operation when isSuccess is false' do
      payload['isSuccess'] = false
      expect(CoinDepositOperation).not_to receive(:create!)
      handler.send(:process_deposit_created, payload)
    end

    it 'logs error when record is not found' do
      allow(CoinDeposit).to receive(:find_by).with(id: deposit_id).and_raise(ActiveRecord::RecordNotFound)
      expect(Rails.logger).to receive(:error).with(/Failed to find record/)
      handler.send(:process_deposit_created, payload)
    end

    it 'logs error when standard error occurs' do
      allow(CoinDeposit).to receive(:find_by).with(id: deposit_id).and_raise(StandardError)
      expect(Rails.logger).to receive(:error).with(/Error processing deposit/)
      expect(Rails.logger).to receive(:error)
      handler.send(:process_deposit_created, payload)
    end
  end

  describe '#create_deposit_operation' do
    let(:object) do
      {
        'amount' => amount,
        'coin' => coin,
        'txHash' => tx_hash
      }
    end

    it 'creates deposit operation with correct attributes' do
      operation = handler.send(:create_deposit_operation, coin_deposit, coin_account, object)

      expect(operation.coin_account).to eq(coin_account)
      expect(operation.coin_amount).to eq(amount)
      expect(operation.coin_currency).to eq(coin.downcase)
      expect(operation.coin_deposit).to eq(coin_deposit)
      expect(operation.coin_fee).to eq(0)
      expect(operation.platform_fee).to eq(0)
      expect(operation.tx_hash).to eq(tx_hash)
      expect(operation.out_index).to eq(out_index)
      expect(operation.status).to eq('completed')
    end
  end
end
