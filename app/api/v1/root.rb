# frozen_string_literal: true

module V1
  class Root < Grape::API
    mount V1::Test::Api
    mount V1::Auth::Api
    mount V1::Balances::Api
  end
end
