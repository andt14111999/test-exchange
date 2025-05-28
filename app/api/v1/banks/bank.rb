# frozen_string_literal: true

module V1
  module Banks
    class Bank
      attr_accessor :name, :code, :bin, :shortName, :logo, :transferSupported,
                    :lookupSupported, :short_name, :support, :isTransfer, :swift_code

      def initialize(attributes = {})
        attributes.each do |key, value|
          send("#{key}=", value) if respond_to?("#{key}=")
        end
      end

      def self.from_json_array(json_array)
        json_array.map { |bank_hash| new(bank_hash) }
      end
    end
  end
end
