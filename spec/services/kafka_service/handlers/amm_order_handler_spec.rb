# frozen_string_literal: true

require 'rails_helper'

describe KafkaService::Handlers::AmmOrderHandler do
  let(:handler) { described_class.new }

  describe '#extract_params_from_response' do
    it 'extracts amount_received from payload' do
      object = {
        'amountActual' => 95.0,
        'amountEstimated' => 100.0,
        'amountReceived' => 93.5,
        'beforeTickIndex' => -10,
        'afterTickIndex' => 10,
        'fees' => { 'token0' => '0.5', 'token1' => '0.3' },
        'status' => 'success',
        'updatedAt' => 1650000000000
      }

      params = handler.send(:extract_params_from_response, object)
      expect(params[:amount_received]).to eq(93.5)
    end

    it 'returns compact hash without nil values' do
      object = {
        'amountActual' => 95.0,
        'amountReceived' => nil,
        'updatedAt' => 1650000000000
      }

      params = handler.send(:extract_params_from_response, object)
      expect(params.keys).not_to include(:amount_received)
      expect(params[:amount_actual]).to eq(95.0)
    end
  end

  describe '#update_order_state' do
    let(:amm_order) { double('AmmOrder', error?: false, processing?: true) }

    it 'updates amount_received when provided' do
      params = {
        amount_actual: 95.0,
        amount_received: 93.5,
        status: 'success'
      }

      expect(amm_order).to receive(:succeed!).once
      expect(amm_order).to receive(:update!).with(params).once

      handler.send(:update_order_state, amm_order, params)
    end

    it 'succeeds the order when status is success' do
      params = {
        amount_actual: 95.0,
        amount_received: 93.5,
        status: 'success'
      }

      expect(amm_order).to receive(:succeed!).once
      expect(amm_order).to receive(:update!).with(params).once

      handler.send(:update_order_state, amm_order, params)
    end

    it 'fails the order when error_message is present' do
      params = {
        error_message: 'Test error'
      }

      expect(amm_order).to receive(:fail!).with('Test error').once
      expect(amm_order).not_to receive(:update!)

      handler.send(:update_order_state, amm_order, params)
    end

    it 'just updates params when not success or error' do
      params = {
        amount_actual: 95.0,
        amount_received: 93.5,
        status: 'processing'
      }

      expect(amm_order).not_to receive(:succeed!)
      expect(amm_order).to receive(:update!).with(params).once

      handler.send(:update_order_state, amm_order, params)
    end

    it 'does not call succeed! when order is already in error state' do
      params = {
        amount_actual: 95.0,
        amount_received: 93.5,
        status: 'success'
      }

      allow(amm_order).to receive(:error?).and_return(true)
      allow(amm_order).to receive(:processing?).and_return(false)

      expect(amm_order).not_to receive(:succeed!)
      expect(amm_order).to receive(:update!).with(params).once

      handler.send(:update_order_state, amm_order, params)
    end
  end
end
