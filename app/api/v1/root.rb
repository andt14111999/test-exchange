# frozen_string_literal: true

module V1
  class Root < Grape::API
    mount V1::Test::Api
  end
end
