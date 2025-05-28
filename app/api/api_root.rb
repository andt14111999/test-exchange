# frozen_string_literal: true

class ApiRoot < Grape::API
  include BaseApiRootModule
  include Grape::Rails::Cache

  helpers Base::Helpers::Meta

  mount V1::Root
  mount V2::Root
end
