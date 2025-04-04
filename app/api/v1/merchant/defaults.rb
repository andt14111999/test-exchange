# frozen_string_literal: true

module V1
  module Merchant
    module Defaults
      extend ActiveSupport::Concern

      included do
        helpers V1::Helpers::AuthHelper

        before { authenticate_user! }
      end
    end
  end
end
