# frozen_string_literal: true

module V2
  class Root < Grape::API
    version :v2, using: :path

    mount V2::Test::Api
  end
end
