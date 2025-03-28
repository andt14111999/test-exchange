# frozen_string_literal: true

class BalanceBroadcastService
  def self.call(user)
    new(user).call
  end

  def initialize(user)
    @user = user
  end

  def call
    broadcast_balance
  end

  private

  def broadcast_balance
    begin
      BalanceChannel.broadcast_to(@user, balance_data_with_status)
      true
    rescue => e
      false
    end
  end

  def balance_data_with_status
    {
      status: 'success',
      data: balance_data
    }
  end

  def balance_data
    {
      coin_accounts: coin_account_data,
      fiat_accounts: fiat_account_data
    }
  end

  def coin_account_data
    ::CoinAccount::SUPPORTED_NETWORKS.keys.map do |coin_currency|
      main_account = @user.coin_accounts.of_coin(coin_currency).main

      {
        coin_currency: coin_currency,
        balance: main_account&.balance || 0,
        frozen_balance: main_account&.frozen_balance || 0
      }
    end
  end

  def fiat_account_data
    ::FiatAccount::SUPPORTED_CURRENCIES.keys.map do |currency|
      account = @user.fiat_accounts.find_by(currency: currency)
      {
        currency: currency,
        balance: account&.balance || 0,
        frozen_balance: account&.frozen_balance || 0
      }
    end
  end
end
