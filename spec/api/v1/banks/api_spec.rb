# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Banks::Api, type: :request do
  let!(:vietnam) { create(:country, code: 'VN', name: 'Vietnam') }
  let!(:vietinbank) do
    create(:bank,
      country: vietnam,
      name: 'Ngân hàng TMCP Công thương Việt Nam',
      code: 'ICB',
      bin: '970415',
      short_name: 'VietinBank',
      logo: 'https://api.vietqr.io/img/ICB.png',
      transfer_supported: true,
      lookup_supported: true,
      support: 3,
      is_transfer: true,
      swift_code: 'ICBVVNVX'
    )
  end

  describe 'GET /api/v1/banks' do
    context 'when requesting banks data' do
      it 'returns banks for Vietnam when country_code is provided' do
        get '/api/v1/banks', params: { country_code: 'VN' }

        expect(response).to have_http_status(200)
        json_response = JSON.parse(response.body)

        # Kiểm tra response có đúng format không
        expect(json_response['status']).to eq('success')
        expect(json_response['data'].length).to eq(1)

        # Kiểm tra dữ liệu của ngân hàng đầu tiên
        first_bank_in_response = json_response['data'].first

        # Kiểm tra các trường cụ thể
        expect(first_bank_in_response['name']).to eq(vietinbank.name)
        expect(first_bank_in_response['code']).to eq(vietinbank.code)
        expect(first_bank_in_response['bin']).to eq(vietinbank.bin)
        expect(first_bank_in_response['shortName']).to eq(vietinbank.short_name)
        expect(first_bank_in_response['logo']).to eq(vietinbank.logo)
        expect(first_bank_in_response['transferSupported']).to eq(vietinbank.transfer_supported)
        expect(first_bank_in_response['lookupSupported']).to eq(vietinbank.lookup_supported)
        expect(first_bank_in_response['countryCode']).to eq('VN')
        expect(first_bank_in_response['countryName']).to eq('Vietnam')
      end

      it 'returns 400 error when country_code is missing' do
        get '/api/v1/banks'

        expect(response).to have_http_status(400)
        json_response = JSON.parse(response.body)
        expect(json_response['error']).to include('country_code')
      end

      it 'returns empty array for country with no banks' do
        get '/api/v1/banks', params: { country_code: 'NG' }

        expect(response).to have_http_status(200)
        json_response = JSON.parse(response.body)
        expect(json_response['status']).to eq('success')
        expect(json_response['data']).to eq([])
      end
    end
  end
end
