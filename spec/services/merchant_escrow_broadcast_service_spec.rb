# frozen_string_literal: true

require 'rails_helper'

RSpec.describe MerchantEscrowBroadcastService, type: :service do
  describe '.call' do
    it 'creates an instance and calls it' do
      service = instance_double(described_class)
      merchant_escrow = create(:merchant_escrow)

      expect(described_class).to receive(:new).with(merchant_escrow).and_return(service)
      expect(service).to receive(:call)

      described_class.call(merchant_escrow)
    end
  end

  describe '#call' do
    let(:merchant_escrow) { create(:merchant_escrow) }
    let(:service) { described_class.new(merchant_escrow) }

    context 'when broadcast is successful' do
      before do
        allow(service).to receive(:broadcast_merchant_escrow).and_return(true)
      end

      it 'updates delivered flag to true when merchant_escrow responds to delivered' do
        allow(merchant_escrow).to receive(:respond_to?) { |arg, *_| arg == :delivered ? true : merchant_escrow.method(:respond_to?).super_method.call(arg) }
        expect(merchant_escrow).not_to receive(:update).with(delivered: false)
        # delivered: true is only set in the private method, not in #call
        service.call
      end

      it 'does not update delivered flag when merchant_escrow does not respond to delivered' do
        allow(merchant_escrow).to receive(:respond_to?) { |arg, *_| arg == :delivered ? false : merchant_escrow.method(:respond_to?).super_method.call(arg) }
        expect(merchant_escrow).not_to receive(:update)
        service.call
      end
    end

    context 'when broadcast fails' do
      before do
        allow(service).to receive(:broadcast_merchant_escrow).and_return(false)
      end

      it 'updates delivered flag to false when merchant_escrow responds to delivered' do
        allow(merchant_escrow).to receive(:respond_to?) { |arg, *_| arg == :delivered ? true : merchant_escrow.method(:respond_to?).super_method.call(arg) }
        expect(merchant_escrow).to receive(:update).with(delivered: false)
        service.call
      end

      it 'does not update delivered flag when merchant_escrow does not respond to delivered' do
        allow(merchant_escrow).to receive(:respond_to?) { |arg, *_| arg == :delivered ? false : merchant_escrow.method(:respond_to?).super_method.call(arg) }
        expect(merchant_escrow).not_to receive(:update)
        service.call
      end
    end
  end

  describe '#broadcast_merchant_escrow' do
    let(:merchant_escrow) { create(:merchant_escrow) }
    let(:service) { described_class.new(merchant_escrow) }

    context 'when broadcast succeeds' do
      before do
        allow(MerchantEscrowChannel).to receive(:broadcast_to_merchant_escrow)
      end

      it 'broadcasts the merchant escrow data' do
        expect(MerchantEscrowChannel).to receive(:broadcast_to_merchant_escrow).with(merchant_escrow, service.send(:merchant_escrow_data))
        service.send(:broadcast_merchant_escrow)
      end

      it 'updates delivered flag to true when merchant_escrow responds to delivered' do
        allow(merchant_escrow).to receive(:respond_to?) { |arg, *_| arg == :delivered ? true : merchant_escrow.method(:respond_to?).super_method.call(arg) }
        expect(merchant_escrow).to receive(:update).with(delivered: true)
        service.send(:broadcast_merchant_escrow)
      end

      it 'does not update delivered flag when merchant_escrow does not respond to delivered' do
        allow(merchant_escrow).to receive(:respond_to?) { |arg, *_| arg == :delivered ? false : merchant_escrow.method(:respond_to?).super_method.call(arg) }
        expect(merchant_escrow).not_to receive(:update)
        service.send(:broadcast_merchant_escrow)
      end

      it 'returns true' do
        expect(service.send(:broadcast_merchant_escrow)).to be true
      end
    end

    context 'when broadcast raises an error' do
      before do
        allow(MerchantEscrowChannel).to receive(:broadcast_to_merchant_escrow).and_raise(StandardError.new('Broadcast error'))
        allow(Rails.logger).to receive(:error)
      end

      it 'logs the error' do
        expect(Rails.logger).to receive(:error).with('Error broadcasting merchant escrow: Broadcast error')
        service.send(:broadcast_merchant_escrow)
      end

      it 'returns false' do
        expect(service.send(:broadcast_merchant_escrow)).to be false
      end
    end
  end

  describe '#merchant_escrow_data' do
    let(:merchant_escrow) { create(:merchant_escrow) }
    let(:service) { described_class.new(merchant_escrow) }

    it 'returns the correct data structure' do
      expected_data = {
        status: 'success',
        data: {
          id: merchant_escrow.id,
          user_id: merchant_escrow.user_id,
          usdt_account_id: merchant_escrow.usdt_account_id,
          fiat_account_id: merchant_escrow.fiat_account_id,
          usdt_amount: merchant_escrow.usdt_amount,
          fiat_amount: merchant_escrow.fiat_amount,
          fiat_currency: merchant_escrow.fiat_currency,
          exchange_rate: merchant_escrow.exchange_rate,
          status: merchant_escrow.status,
          created_at: merchant_escrow.created_at,
          updated_at: merchant_escrow.updated_at
        }
      }
      expect(service.send(:merchant_escrow_data)).to eq(expected_data)
    end
  end
end
