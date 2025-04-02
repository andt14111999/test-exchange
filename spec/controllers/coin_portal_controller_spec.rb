# frozen_string_literal: true

require 'rails_helper'

RSpec.describe CoinPortalController, type: :controller do
  let(:user) { create(:user) }

  # Use find_or_create_by to avoid uniqueness validation errors
  let(:coin_account) do
    user.coin_accounts.find_or_create_by(
      coin_currency: 'btc',
      layer: 'bitcoin',
      account_type: 'deposit'
    ) do |account|
      account.balance = 0
      account.frozen_balance = 0
      account.address = 'btc_test_address'
    end
  end

  let(:params) do
    {
      type: 'deposit',
      coin: 'btc',
      address: coin_account.address,
      amount: '1.0',
      txid: 'tx123',
      timestamp: Time.current.to_i
    }
  end

  before do
    allow_any_instance_of(described_class).to receive(:authenticate_request).and_return(true)
    allow_any_instance_of(User).to receive(:active?).and_return(true)
    allow(CoinAccount).to receive(:portal_coin_to_coin_currency).with('btc').and_return('btc')

    kafka_deposit_service = instance_double(KafkaService::Services::Coin::CoinDepositService)
    allow(kafka_deposit_service).to receive(:create).and_return(true)
    allow(KafkaService::Services::Coin::CoinDepositService).to receive(:new).and_return(kafka_deposit_service)

    kafka_account_service = instance_double(KafkaService::Services::Coin::CoinAccountService)
    allow(kafka_account_service).to receive(:create).and_return(true)
    allow(KafkaService::Services::Coin::CoinAccountService).to receive(:new).and_return(kafka_account_service)
  end

  describe '#authenticate_request' do
    it 'returns unauthorized when signature is missing' do
      allow_any_instance_of(described_class).to receive(:authenticate_request).and_call_original

      request.headers['X-Timestamp'] = Time.current.to_i.to_s

      get :index, params: { type: 'unknown' }
      expect(response).to have_http_status(:unauthorized)
      expect(response.body).to eq('Missing signature')
    end

    it 'returns unauthorized when timestamp is missing' do
      allow_any_instance_of(described_class).to receive(:authenticate_request).and_call_original

      request.headers['X-Signature'] = 'fake_signature'

      get :index, params: { type: 'unknown' }
      expect(response).to have_http_status(:unauthorized)
      expect(response.body).to eq('Missing timestamp')
    end
  end

  describe '#handle_deposit' do
    context 'when deposit is successful' do
      it 'handles successful deposit' do
        relation = instance_double(ActiveRecord::Relation)
        allow(relation).to receive(:first).and_return(coin_account)
        allow(CoinAccount).to receive(:where).and_return(relation)

        deposit = instance_double(CoinDeposit, id: 1, persisted?: true)
        allow(coin_account).to receive(:handle_deposit).and_return([ deposit, true ])

        post :index, params: params
        expect(response).to have_http_status(:ok)
        expect(JSON.parse(response.body)).to have_key('created_at')
      end
    end

    context 'when deposit fails' do
      it 'returns bad request for failed deposit' do
        relation = instance_double(ActiveRecord::Relation)
        allow(relation).to receive(:first).and_return(coin_account)
        allow(CoinAccount).to receive(:where).and_return(relation)

        allow(coin_account).to receive(:handle_deposit).and_return([ 'error', false ])

        post :index, params: params
        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)).to eq({ 'error' => 'error' })
      end
    end

    context 'when account is not found' do
      it 'returns bad request' do
        relation = instance_double(ActiveRecord::Relation)
        allow(relation).to receive(:first).and_return(nil)
        allow(CoinAccount).to receive(:where).and_return(relation)

        post :index, params: params
        expect(response).to have_http_status(:bad_request)
        expect(JSON.parse(response.body)).to eq({ 'error' => 'Account not found' })
      end
    end
  end
end
