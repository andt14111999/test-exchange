# frozen_string_literal: true

module V1
  class Root < Grape::API
    mount V1::Test::Api
    mount V1::Auth::Api
    mount V1::Balances::Api
    mount V1::CoinAccounts::Api
    mount V1::CoinTransactions::Api
    mount V1::CoinWithdrawals::Api
    mount V1::Notifications::Api
    mount V1::Users::Api
    mount V1::Merchant::Escrows
    mount V1::Users::MerchantRegistration
    mount V1::Settings::Api
    mount V1::AmmPools::Api
    mount V1::AmmPositions::Api
    mount V1::AmmOrders::Api
    mount V1::Ticks::Api
    mount V1::Coins::Api
    mount V1::ApiKeys
    mount V1::Banks::Api

    # P2P Trading APIs
    mount V1::Offers::Api
    mount V1::Trades::Api
    mount V1::FiatDeposits::Api
    mount V1::FiatWithdrawals::Api
    mount V1::BankAccounts::Api
    mount V1::PaymentMethods::Api
  end
end
