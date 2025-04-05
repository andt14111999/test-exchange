# frozen_string_literal: true

module V1
  class Root < Grape::API
    mount V1::Test::Api
    mount V1::Auth::Api
    mount V1::Balances::Api
    mount V1::CoinAccounts::Api
    mount V1::CoinTransactions::Api
    mount V1::Notifications::Api
    mount V1::Users::Api
    mount V1::Merchant::Escrows
    mount V1::Users::MerchantRegistration
    mount V1::Settings::Api
    mount V1::AmmPools::Api
  end
end
