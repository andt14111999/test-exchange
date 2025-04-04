require 'rails_helper'

RSpec.describe KafkaService::Handlers::CoinDepositHandler, type: :service do
  describe '#handle' do
    context 'when operation type is COIN_DEPOSIT_CREATE' do
      it 'processes deposit created successfully' do
        user = create(:user)
        coin_account = user.coin_accounts.find_by!(coin_currency: 'usdt', layer: 'erc20')
        deposit = create(:coin_deposit, user: user, coin_account: coin_account, coin_currency: 'usdt', coin_amount: 1.5, tx_hash: '0x123')
        payload = {
          'object' => {
            'operationType' => KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
            'identifier' => "deposit-#{deposit.id}",
            'accountKey' => coin_account.id,
            'amount' => 1.5,
            'coin' => 'USDT',
            'txHash' => '0x123'
          },
          'isSuccess' => true
        }

        expect do
          described_class.new.handle(payload)
        end.to change(CoinDepositOperation, :count).by(1)

        operation = CoinDepositOperation.last
        expect(operation.coin_account).to eq(coin_account)
        expect(operation.coin_amount).to eq(1.5)
        expect(operation.coin_currency).to eq('usdt')
        expect(operation.coin_deposit).to eq(deposit)
        expect(operation.coin_fee).to eq(0)
        expect(operation.platform_fee).to eq(0)
        expect(operation.tx_hash).to eq('0x123')
        expect(operation.out_index).to eq(deposit.out_index)
        expect(operation.status).to eq('completed')
      end

      it 'does not process when deposit is not found' do
        payload = {
          'object' => {
            'operationType' => KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
            'identifier' => 'deposit-999999',
            'accountKey' => 1,
            'amount' => 1.5,
            'coin' => 'USDT',
            'txHash' => '0x123'
          },
          'isSuccess' => true
        }

        expect do
          described_class.new.handle(payload)
        end.not_to change(CoinDepositOperation, :count)
      end

      it 'does not process when isSuccess is false' do
        user = create(:user)
        coin_account = user.coin_accounts.find_by!(coin_currency: 'usdt', layer: 'erc20')
        deposit = create(:coin_deposit, user: user, coin_account: coin_account, coin_currency: 'usdt', coin_amount: 1.5, tx_hash: '0x123')
        payload = {
          'object' => {
            'operationType' => KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
            'identifier' => "deposit-#{deposit.id}",
            'accountKey' => coin_account.id,
            'amount' => 1.5,
            'coin' => 'USDT',
            'txHash' => '0x123'
          },
          'isSuccess' => false
        }

        expect do
          described_class.new.handle(payload)
        end.not_to change(CoinDepositOperation, :count)
      end

      it 'does not process when coin account is not found' do
        user = create(:user)
        coin_account = user.coin_accounts.find_by!(coin_currency: 'usdt', layer: 'erc20')
        deposit = create(:coin_deposit, user: user, coin_account: coin_account, coin_currency: 'usdt', coin_amount: 1.5, tx_hash: '0x123')
        payload = {
          'object' => {
            'operationType' => KafkaService::Config::OperationTypes::COIN_DEPOSIT_CREATE,
            'identifier' => "deposit-#{deposit.id}",
            'accountKey' => 999999,
            'amount' => 1.5,
            'coin' => 'USDT',
            'txHash' => '0x123'
          },
          'isSuccess' => true
        }

        expect do
          described_class.new.handle(payload)
        end.not_to change(CoinDepositOperation, :count)
      end

      it 'handles other operation types' do
        payload = {
          'object' => {
            'operationType' => 'OTHER_OPERATION',
            'identifier' => 'deposit-1',
            'accountKey' => 1,
            'amount' => 1.5,
            'coin' => 'USDT',
            'txHash' => '0x123'
          },
          'isSuccess' => true
        }

        expect do
          described_class.new.handle(payload)
        end.not_to change(CoinDepositOperation, :count)
      end
    end
  end
end 