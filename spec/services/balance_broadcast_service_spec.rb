# frozen_string_literal: true

require 'rails_helper'

RSpec.describe BalanceBroadcastService, type: :service do
  describe '.call' do
    it 'initializes and calls the service' do
      user = create(:user)
      service = instance_double(described_class)

      expect(described_class).to receive(:new).with(user).and_return(service)
      expect(service).to receive(:call)

      described_class.call(user)
    end
  end

  describe '#call' do
    it 'broadcasts balance data' do
      user = create(:user)
      service = described_class.new(user)

      expect(service).to receive(:broadcast_balance).and_return(true)
      service.call
    end
  end

  describe '#broadcast_balance' do
    let(:expected_data) do
      {
        status: 'success',
        data: {
          coin_accounts: ::CoinAccount::SUPPORTED_NETWORKS.keys.map do |coin_currency|
            {
              coin_currency: coin_currency,
              balance: 0,
              frozen_balance: 0
            }
          end,
          fiat_accounts: ::FiatAccount::SUPPORTED_CURRENCIES.keys.map do |currency|
            {
              currency: currency,
              balance: 0,
              frozen_balance: 0
            }
          end
        }
      }
    end

    context 'when broadcast is successful' do
      it 'returns true' do
        user = create(:user)
        service = described_class.new(user)

        expect(BalanceChannel).to receive(:broadcast_to_user)
          .with(user, expected_data)
          .and_return(true)

        expect(service.send(:broadcast_balance)).to be true
      end
    end

    context 'when broadcast fails' do
      it 'returns false' do
        user = create(:user)
        service = described_class.new(user)

        expect(BalanceChannel).to receive(:broadcast_to_user)
          .with(user, expected_data)
          .and_raise(StandardError)

        expect(service.send(:broadcast_balance)).to be false
      end
    end
  end

  describe '#balance_data' do
    it 'returns formatted balance data with coin and fiat accounts' do
      user = create(:user)
      service = described_class.new(user)

      expect(service).to receive(:coin_account_data).and_return([ { coin_data: 'test' } ])
      expect(service).to receive(:fiat_account_data).and_return([ { fiat_data: 'test' } ])

      result = service.send(:balance_data)

      expect(result).to eq(
        coin_accounts: [ { coin_data: 'test' } ],
        fiat_accounts: [ { fiat_data: 'test' } ]
      )
    end
  end

  describe '#coin_account_data' do
    it 'returns formatted coin account data for all supported networks' do
      user = create(:user)
      btc_account = create(:coin_account, :main, user: user, coin_currency: 'usdt', layer: 'all', balance: 1.5, frozen_balance: 0.5)
      service = described_class.new(user)

      result = service.send(:coin_account_data)

      expect(result).to include(
        {
          coin_currency: btc_account.coin_currency,
          balance: btc_account.balance,
          frozen_balance: btc_account.frozen_balance
        }
      )

      # Verify other supported networks return zero balances when accounts don't exist
      other_networks = ::CoinAccount::SUPPORTED_NETWORKS.keys - [ btc_account.coin_currency ]
      other_networks.each do |network|
        expect(result).to include(
          {
            coin_currency: network,
            balance: 0,
            frozen_balance: 0
          }
        )
      end
    end
  end

  describe '#fiat_account_data' do
    it 'returns formatted fiat account data for all supported currencies' do
      user = create(:user)
      vnd_account = create(:fiat_account, user: user, currency: 'VND', balance: 1000, frozen_balance: 200)
      service = described_class.new(user)

      result = service.send(:fiat_account_data)

      expect(result).to include(
        {
          currency: vnd_account.currency,
          balance: vnd_account.balance,
          frozen_balance: vnd_account.frozen_balance
        }
      )

      # Verify other supported currencies return zero balances when accounts don't exist
      other_currencies = ::FiatAccount::SUPPORTED_CURRENCIES.keys - [ vnd_account.currency ]
      other_currencies.each do |currency|
        expect(result).to include(
          {
            currency: currency,
            balance: 0,
            frozen_balance: 0
          }
        )
      end
    end
  end
end
