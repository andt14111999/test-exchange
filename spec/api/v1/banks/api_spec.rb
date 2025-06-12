# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Banks::Api, type: :request do
  describe 'GET /api/v1/banks' do
    context 'when requesting banks data' do
      let(:sample_banks_data) do
        {
          'data' => [
            {
              'name' => 'Ngân hàng TMCP Công thương Việt Nam',
              'code' => 'ICB',
              'bin' => '970415',
              'shortName' => 'VietinBank',
              'logo' => 'https://api.vietqr.io/img/ICB.png',
              'transferSupported' => 1,
              'lookupSupported' => 1,
              'short_name' => 'VietinBank',
              'support' => 3,
              'isTransfer' => 1,
              'swift_code' => 'ICBVVNVX'
            },
            {
              'name' => 'Ngân hàng TMCP Ngoại Thương Việt Nam',
              'code' => 'VCB',
              'bin' => '970436',
              'shortName' => 'Vietcombank',
              'logo' => 'https://api.vietqr.io/img/VCB.png',
              'transferSupported' => 1,
              'lookupSupported' => 1,
              'short_name' => 'Vietcombank',
              'support' => 3,
              'isTransfer' => 1,
              'swift_code' => 'BFTVVNVX'
            }
          ]
        }
      end

      before do
        # Cho phép đọc file bất kỳ và trả về dữ liệu thật
        allow(File).to receive(:read).and_call_original

        # Nhưng đối với file banks.json, trả về dữ liệu mẫu
        allow(File).to receive(:read).with(Rails.root.join('data', 'banks.json')).and_return(sample_banks_data.to_json)
      end

      it 'returns all banks from the JSON file' do
        get '/api/v1/banks'

        expect(response).to have_http_status(200)
        json_response = JSON.parse(response.body)

        # Kiểm tra response có đúng format không
        expect(json_response['status']).to eq('success')
        expect(json_response['data'].length).to eq(sample_banks_data['data'].length)
        expect(json_response['data'].length).to be > 0

        # Kiểm tra dữ liệu của ngân hàng đầu tiên
        first_bank_in_file = sample_banks_data['data'].first
        first_bank_in_response = json_response['data'].first

        # Kiểm tra các trường cụ thể
        expect(first_bank_in_response['name']).to eq(first_bank_in_file['name'])
        expect(first_bank_in_response['code']).to eq(first_bank_in_file['code'])
        expect(first_bank_in_response['bin']).to eq(first_bank_in_file['bin'])
        expect(first_bank_in_response['shortName']).to eq(first_bank_in_file['shortName'])
        expect(first_bank_in_response['logo']).to eq(first_bank_in_file['logo'])
        expect(first_bank_in_response['transferSupported']).to eq(first_bank_in_file['transferSupported'])
        expect(first_bank_in_response['lookupSupported']).to eq(first_bank_in_file['lookupSupported'])
      end
    end
  end
end
