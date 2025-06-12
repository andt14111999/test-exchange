# frozen_string_literal: true

module V1
  module Banks
    class Api < Grape::API
      resource :banks do
        desc 'Get all banks from banks.json'
        get do
          cache(key: 'banks-version-2', expires_in: 1.year) do
            banks_data = JSON.parse(File.read(Rails.root.join('data', 'banks.json')))
            json_banks = banks_data['data']

            banks = V1::Banks::Bank.from_json_array(json_banks)

            present :status, 'success'
            present :data, banks, with: V1::Banks::Entity
          end
        end
      end
    end
  end
end
