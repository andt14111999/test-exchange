# frozen_string_literal: true

module V1
  module Banks
    class Entity < Grape::Entity
      expose :name
      expose :code
      expose :bin
      expose :shortName
      expose :logo
      expose :transferSupported
      expose :lookupSupported
      expose :short_name
      expose :support
      expose :isTransfer
      expose :swift_code
    end
  end
end
