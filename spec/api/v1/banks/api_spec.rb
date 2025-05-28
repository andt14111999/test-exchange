# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Banks::Api, type: :request do
  describe 'GET /api/v1/banks' do
    context 'when requesting banks data' do
      it 'returns all banks from the JSON file' do
        banks_data = JSON.parse(File.read(Rails.root.join('storage', 'banks.json')))

        get '/api/v1/banks'

        expect(response).to have_http_status(200)
        json_response = JSON.parse(response.body)

        # Kiểm tra response có đúng format không
        expect(json_response['status']).to eq('success')
        expect(json_response['data'].length).to eq(banks_data['data'].length)
        expect(json_response['data'].length).to be > 0

        # Kiểm tra dữ liệu của ngân hàng đầu tiên
        first_bank_in_file = banks_data['data'].first
        first_bank_in_response = json_response['data'].first

        # Kiểm tra các trường cụ thể
        expect(first_bank_in_response['name']).to eq(first_bank_in_file['name'])
        expect(first_bank_in_response['code']).to eq(first_bank_in_file['code'])
        expect(first_bank_in_response['bin']).to eq(first_bank_in_file['bin'])
        expect(first_bank_in_response['shortName']).to eq(first_bank_in_file['shortName'])
        expect(first_bank_in_response['logo']).to eq(first_bank_in_file['logo'])
        expect(first_bank_in_response['transferSupported']).to eq(first_bank_in_file['transferSupported'])
        expect(first_bank_in_response['lookupSupported']).to eq(first_bank_in_file['lookupSupported'])

        # Kiểm tra ngân hàng ở vị trí thứ 5
        fifth_bank_in_file = banks_data['data'][4]
        fifth_bank_in_response = json_response['data'][4]
        expect(fifth_bank_in_response['name']).to eq(fifth_bank_in_file['name'])
        expect(fifth_bank_in_response['code']).to eq(fifth_bank_in_file['code'])

        # Kiểm tra ngân hàng cuối cùng
        last_bank_in_file = banks_data['data'].last
        last_bank_in_response = json_response['data'].last
        expect(last_bank_in_response['name']).to eq(last_bank_in_file['name'])
        expect(last_bank_in_response['code']).to eq(last_bank_in_file['code'])
      end
    end
  end
end
