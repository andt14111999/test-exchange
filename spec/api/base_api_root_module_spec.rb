# frozen_string_literal: true

require 'rails_helper'

RSpec.describe BaseApiRootModule, type: :request do
  before do
    stub_const('TestApi', Class.new(Grape::API) do
      include BaseApiRootModule

      get '/test' do
        User.find(1)
      end
    end)

    Rails.application.routes.draw do
      mount TestApi => '/'
    end
  end

  after do
    Rails.application.reload_routes!
  end

  describe 'configuration' do
    it 'sets correct API configuration' do
      expect(TestApi.prefix).to eq('api')
      expect(TestApi.version).to eq('v1')
      expect(TestApi.default_format).to eq(:json)
      expect(TestApi.content_types[:json]).to eq('application/json')
    end
  end

  describe 'error handling' do
    context 'when ActiveRecord::RecordNotFound is raised' do
      it 'returns correct error response for model not found' do
        allow(User).to receive(:find).with(1).and_raise(
          ActiveRecord::RecordNotFound.new('Couldn\'t find User with id=1')
        )

        get '/api/v1/test'

        expect(response.status).to eq(404)
        expect(JSON.parse(response.body)).to eq(
          {
            'error' => I18n.t('error.record_not_found'),
            'class_name' => 'User'
          }
        )
      end

      it 'returns correct error response for generic not found' do
        allow(User).to receive(:find).with(1).and_raise(
          ActiveRecord::RecordNotFound.new('Record not found')
        )

        get '/api/v1/test'

        expect(response.status).to eq(404)
        expect(JSON.parse(response.body)).to eq(
          {
            'error' => I18n.t('error.record_not_found'),
            'class_name' => 'Record'
          }
        )
      end
    end
  end
end
