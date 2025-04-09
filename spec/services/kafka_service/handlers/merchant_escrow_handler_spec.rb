# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Handlers::MerchantEscrowHandler, type: :service do
  let(:handler) { described_class.new }
  let(:identifier) { SecureRandom.uuid }

  describe '#handle' do
    context 'when operation type is MERCHANT_ESCROW_MINT' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::MERCHANT_ESCROW_MINT,
          'actionId' => '123',
          'object' => {
            'identifier' => identifier,
            'usdtAccountKey' => '1-coin-456',
            'fiatAccountKey' => '1-fiat-789',
            'usdtAmount' => '100.0',
            'fiatAmount' => '100.0',
            'fiatCurrency' => 'VND'
          }
        }
      end

      it 'processes escrow mint' do
        expect(handler).to receive(:process_escrow_mint).with(payload)
        handler.handle(payload)
      end
    end

    context 'when operation type is MERCHANT_ESCROW_BURN' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::MERCHANT_ESCROW_BURN,
          'actionId' => '123',
          'object' => {
            'identifier' => identifier,
            'usdtAccountKey' => '1-coin-456',
            'fiatAccountKey' => '1-fiat-789',
            'usdtAmount' => '100.0',
            'fiatAmount' => '100.0',
            'fiatCurrency' => 'VND'
          }
        }
      end

      it 'processes escrow burn' do
        expect(handler).to receive(:process_escrow_burn).with(payload)
        handler.handle(payload)
      end
    end

    context 'when operation type is unknown' do
      let(:payload) do
        {
          'operationType' => 'UNKNOWN_OPERATION',
          'actionId' => '123'
        }
      end

      it 'does not process anything' do
        expect(handler).not_to receive(:process_escrow_mint)
        expect(handler).not_to receive(:process_escrow_burn)
        handler.handle(payload)
      end
    end

    context 'when payload is nil' do
      let(:payload) { nil }

      it 'does not process anything' do
        expect(handler).not_to receive(:process_escrow_mint)
        expect(handler).not_to receive(:process_escrow_burn)
        expect { handler.handle(payload) }.not_to raise_error
      end
    end
  end

  describe '#process_escrow_mint' do
    let(:merchant) { create(:user, :merchant) }
    let(:usdt_account) { create(:coin_account, coin_currency: 'usdt', user: merchant) }
    let(:fiat_account) { create(:fiat_account, currency: 'VND', user: merchant) }
    let(:escrow) { create(:merchant_escrow, id: '123', user: merchant, status: 'pending', usdt_account: usdt_account, fiat_account: fiat_account) }
    let(:payload) do
      {
        'actionId' => escrow.id,
        'object' => {
          'identifier' => identifier,
          'usdtAccountKey' => "#{merchant.id}-coin-#{usdt_account.id}",
          'fiatAccountKey' => "#{merchant.id}-fiat-#{fiat_account.id}",
          'usdtAmount' => '100.0',
          'fiatAmount' => '100.0',
          'fiatCurrency' => 'VND'
        }
      }
    end

    before do
      allow(Rails.logger).to receive(:info)
      allow(Rails.logger).to receive(:error)
    end

    context 'when processing succeeds' do
      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing merchant escrow mint: #{identifier}")
        allow(handler).to receive(:create_merchant_escrow_operation).and_return(true)
        handler.send(:process_escrow_mint, payload)
      end

      it 'creates a merchant escrow operation' do
        expect(handler).to receive(:create_merchant_escrow_operation).with(
          merchant_escrow: escrow,
          usdt_account_id: usdt_account.id,
          fiat_account_id: fiat_account.id,
          operation_type: 'mint',
          usdt_amount: '100.0',
          fiat_amount: '100.0',
          fiat_currency: 'VND'
        )
        handler.send(:process_escrow_mint, payload)
      end

      it 'activates the escrow if it was pending' do
        allow(handler).to receive(:create_merchant_escrow_operation).and_return(true)
        expect(escrow).to be_pending
        handler.send(:process_escrow_mint, payload)
        expect(escrow.reload).to be_active
      end
    end

    context 'when escrow is not found' do
      let(:payload) do
        {
          'actionId' => 'non-existent-id',
          'object' => {
            'identifier' => identifier,
            'usdtAccountKey' => "#{merchant.id}-coin-#{usdt_account.id}",
            'fiatAccountKey' => "#{merchant.id}-fiat-#{fiat_account.id}",
            'usdtAmount' => '100.0',
            'fiatAmount' => '100.0',
            'fiatCurrency' => 'VND'
          }
        }
      end

      it 'does not create an operation' do
        expect(handler).not_to receive(:create_merchant_escrow_operation)
        handler.send(:process_escrow_mint, payload)
      end

      it 'logs the operation attempt' do
        expect(Rails.logger).to receive(:info).with("Processing merchant escrow mint: #{identifier}")
        handler.send(:process_escrow_mint, payload)
      end
    end

    context 'when escrow is already active' do
      let(:active_escrow) { create(:merchant_escrow, id: '456', user: merchant, status: 'active', usdt_account: usdt_account, fiat_account: fiat_account) }
      let(:payload) do
        {
          'actionId' => active_escrow.id,
          'object' => {
            'identifier' => identifier,
            'usdtAccountKey' => "#{merchant.id}-coin-#{usdt_account.id}",
            'fiatAccountKey' => "#{merchant.id}-fiat-#{fiat_account.id}",
            'usdtAmount' => '100.0',
            'fiatAmount' => '100.0',
            'fiatCurrency' => 'VND'
          }
        }
      end

      it 'creates a merchant escrow operation' do
        expect(handler).to receive(:create_merchant_escrow_operation)
        handler.send(:process_escrow_mint, payload)
      end

      it 'does not change the escrow status' do
        allow(handler).to receive(:create_merchant_escrow_operation).and_return(true)
        expect(active_escrow).to be_active
        handler.send(:process_escrow_mint, payload)
        expect(active_escrow.reload).to be_active
      end
    end

    context 'when an error occurs' do
      before do
        allow(MerchantEscrow).to receive(:find_by).and_raise(StandardError.new('Test error'))
      end

      it 'logs the error' do
        expect(Rails.logger).to receive(:error).with(/Error processing merchant escrow mint: Test error/)
        expect(Rails.logger).to receive(:error)
        handler.send(:process_escrow_mint, payload)
      end

      it 'does not raise an error' do
        expect {
          handler.send(:process_escrow_mint, payload)
        }.not_to raise_error
      end
    end
  end

  describe '#process_escrow_burn' do
    let(:merchant) { create(:user, :merchant) }
    let(:usdt_account) { create(:coin_account, coin_currency: 'usdt', user: merchant) }
    let(:fiat_account) { create(:fiat_account, currency: 'VND', user: merchant) }
    let(:escrow) { create(:merchant_escrow, id: '123', user: merchant, status: 'active', usdt_account: usdt_account, fiat_account: fiat_account) }
    let(:payload) do
      {
        'actionId' => escrow.id,
        'object' => {
          'identifier' => identifier,
          'usdtAccountKey' => "#{merchant.id}-coin-#{usdt_account.id}",
          'fiatAccountKey' => "#{merchant.id}-fiat-#{fiat_account.id}",
          'usdtAmount' => '100.0',
          'fiatAmount' => '100.0',
          'fiatCurrency' => 'VND'
        }
      }
    end

    before do
      allow(Rails.logger).to receive(:info)
      allow(Rails.logger).to receive(:error)
    end

    context 'when processing succeeds' do
      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing merchant escrow burn: #{identifier}")
        allow(handler).to receive(:create_merchant_escrow_operation).and_return(true)
        handler.send(:process_escrow_burn, payload)
      end

      it 'creates a merchant escrow operation' do
        expect(handler).to receive(:create_merchant_escrow_operation).with(
          merchant_escrow: escrow,
          usdt_account_id: usdt_account.id,
          fiat_account_id: fiat_account.id,
          operation_type: 'burn',
          usdt_amount: '100.0',
          fiat_amount: '100.0',
          fiat_currency: 'VND'
        )
        handler.send(:process_escrow_burn, payload)
      end

      it 'cancels the escrow' do
        allow(handler).to receive(:create_merchant_escrow_operation).and_return(true)
        expect(escrow).to be_active
        handler.send(:process_escrow_burn, payload)
        expect(escrow.reload).to be_cancelled
      end
    end

    context 'when escrow is not found' do
      let(:payload) do
        {
          'actionId' => 'non-existent-id',
          'object' => {
            'identifier' => identifier,
            'usdtAccountKey' => "#{merchant.id}-coin-#{usdt_account.id}",
            'fiatAccountKey' => "#{merchant.id}-fiat-#{fiat_account.id}",
            'usdtAmount' => '100.0',
            'fiatAmount' => '100.0',
            'fiatCurrency' => 'VND'
          }
        }
      end

      it 'does not create an operation' do
        expect(handler).not_to receive(:create_merchant_escrow_operation)
        handler.send(:process_escrow_burn, payload)
      end

      it 'logs the operation attempt' do
        expect(Rails.logger).to receive(:info).with("Processing merchant escrow burn: #{identifier}")
        handler.send(:process_escrow_burn, payload)
      end
    end

    context 'when escrow is already cancelled' do
      let(:cancelled_escrow) { create(:merchant_escrow, id: '456', user: merchant, status: 'cancelled', usdt_account: usdt_account, fiat_account: fiat_account) }
      let(:payload) do
        {
          'actionId' => cancelled_escrow.id,
          'object' => {
            'identifier' => identifier,
            'usdtAccountKey' => "#{merchant.id}-coin-#{usdt_account.id}",
            'fiatAccountKey' => "#{merchant.id}-fiat-#{fiat_account.id}",
            'usdtAmount' => '100.0',
            'fiatAmount' => '100.0',
            'fiatCurrency' => 'VND'
          }
        }
      end

      it 'creates a merchant escrow operation' do
        expect(handler).to receive(:create_merchant_escrow_operation)
        handler.send(:process_escrow_burn, payload)
      end

      it 'does not change the escrow status' do
        allow(handler).to receive(:create_merchant_escrow_operation).and_return(true)
        expect(cancelled_escrow).to be_cancelled
        handler.send(:process_escrow_burn, payload)
        expect(cancelled_escrow.reload).to be_cancelled
      end
    end

    context 'when an error occurs' do
      before do
        allow(MerchantEscrow).to receive(:find_by).and_raise(StandardError.new('Test error'))
      end

      it 'logs the error' do
        expect(Rails.logger).to receive(:error).with(/Error processing merchant escrow burn: Test error/)
        expect(Rails.logger).to receive(:error)
        handler.send(:process_escrow_burn, payload)
      end

      it 'does not raise an error' do
        expect {
          handler.send(:process_escrow_burn, payload)
        }.not_to raise_error
      end
    end
  end

  describe '#create_merchant_escrow_operation' do
    let(:merchant) { create(:user, :merchant) }
    let(:usdt_account) { create(:coin_account, coin_currency: 'usdt', user: merchant) }
    let(:fiat_account) { create(:fiat_account, currency: 'VND', user: merchant) }
    let(:escrow) { create(:merchant_escrow, user: merchant, usdt_account: usdt_account, fiat_account: fiat_account) }

    it 'creates a new merchant escrow operation' do
      # Mock the MerchantEscrowOperation create! method to avoid validation errors
      merchant_escrow_operation = instance_double(MerchantEscrowOperation)
      allow(MerchantEscrowOperation).to receive(:create!).and_return(merchant_escrow_operation)

      result = handler.send(:create_merchant_escrow_operation,
        merchant_escrow: escrow,
        usdt_account_id: usdt_account.id,
        fiat_account_id: fiat_account.id,
        operation_type: 'mint',
        usdt_amount: 100.0,
        fiat_amount: 200.0,
        fiat_currency: 'VND'
      )

      expect(result).to eq(merchant_escrow_operation)
      expect(MerchantEscrowOperation).to have_received(:create!).with(
        merchant_escrow: escrow,
        usdt_account_id: usdt_account.id,
        fiat_account_id: fiat_account.id,
        operation_type: 'mint',
        usdt_amount: 100.0,
        fiat_amount: 200.0,
        fiat_currency: 'VND',
        status: 'completed'
      )
    end
  end

  describe '#extract_account_id_from_key' do
    context 'when key has valid format' do
      it 'extracts account ID correctly' do
        key = '123-coin-456'
        expect(handler.send(:extract_account_id_from_key, key)).to eq(456)
      end
    end

    context 'when key has invalid format' do
      it 'returns nil for key with insufficient parts' do
        key = '123-456'
        expect(handler.send(:extract_account_id_from_key, key)).to be_nil
      end

      it 'handles non-numeric account ID' do
        key = '123-coin-abc'
        expect(handler.send(:extract_account_id_from_key, key)).to eq(0)
      end
    end

    context 'when key is nil' do
      it 'returns nil' do
        expect(handler.send(:extract_account_id_from_key, nil)).to be_nil
      end
    end

    context 'when an error occurs during extraction' do
      it 'logs the error and returns nil' do
        allow(Rails.logger).to receive(:error)

        # Instead of trying to mock String#split, which isn't possible,
        # create a situation that will raise an error
        allow_any_instance_of(String).to receive(:split).and_raise(StandardError.new('Test error'))

        expect(Rails.logger).to receive(:error).with(/Error extracting account ID from key/)
        expect(handler.send(:extract_account_id_from_key, '123-coin-456')).to be_nil
      end
    end
  end

  describe 'inheritance' do
    it 'inherits from BaseHandler' do
      expect(described_class.superclass).to eq(KafkaService::Handlers::BaseHandler)
    end
  end
end
