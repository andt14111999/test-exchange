# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Handlers::TransactionResponseHandler do
  let(:handler) { described_class.new }

  describe '#handle' do
    context 'when isSuccess is true' do
      let(:payload) { { 'isSuccess' => true } }

      it 'does nothing' do
        expect(Rollbar).not_to receive(:error)
        handler.handle(payload)
      end
    end

    context 'when isSuccess is false' do
      let(:payload) do
        {
          'isSuccess' => false,
          'actionType' => action_type,
          'actionId' => record.id,
          'errorMessage' => 'Transaction failed'
        }
      end

      before do
        allow(Rollbar).to receive(:error)
      end

      context 'when action_type is CoinTransaction' do
        let(:action_type) { 'CoinTransaction' }
        let(:record) { create(:coin_transaction) }

        it 'logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end

        it 'updates status and error_message' do
          expect { handler.handle(payload) }.to change { record.reload.status }.from('pending').to('transaction_error')
          expect(record.error_message).to eq('Transaction failed')
        end
      end

      context 'when action_type is AmmPool' do
        let(:action_type) { 'AmmPool' }
        let(:record) { create(:amm_pool) }

        before do
          # Ensure pool starts with pending status
          record.update_column(:status, 'pending')
        end

        it 'logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end

        it 'updates status and error_message' do
          expect { handler.handle(payload) }.to change { record.reload.status }.from('pending').to('transaction_error')
          expect(record.error_message).to eq('Transaction failed')
        end
      end

      context 'when action_type is AmmPosition' do
        let(:action_type) { 'AmmPosition' }
        let(:record) { create(:amm_position) }

        it 'logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end

        it 'updates status and error_message' do
          expect { handler.handle(payload) }.to change { record.reload.status }.from('pending').to('transaction_error')
          expect(record.error_message).to eq('Transaction failed')
        end
      end

      context 'when action_type is MerchantEscrow' do
        let(:action_type) { 'MerchantEscrow' }
        let(:record) { create(:merchant_escrow) }

        it 'logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end

        it 'updates status and error_message' do
          expect { handler.handle(payload) }.to change { record.reload.status }.from('pending').to('transaction_error')
          expect(record.error_message).to eq('Transaction failed')
        end
      end

      context 'when action_type is AmmOrder' do
        let(:action_type) { 'AmmOrder' }
        let(:record) { create(:amm_order, skip_balance_validation: true) }

        before do
          # Skip the automatic processing callback and set status without validation
          record.update_column(:status, 'pending')
        end

        it 'logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end

        it 'updates status and error_message' do
          expect { handler.handle(payload) }.to change { record.reload.status }.from('pending').to('transaction_error')
          expect(record.error_message).to eq('Transaction failed')
        end
      end

      context 'when action_type is Trade' do
        let(:action_type) { 'Trade' }
        let(:record) { create(:trade) }

        before do
          # Ensure trade starts with awaiting status
          record.update_column(:status, 'awaiting')
        end

        it 'logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end

        it 'updates status and error_message' do
          expect { handler.handle(payload) }.to change { record.reload.status }.from('awaiting').to('transaction_error')
          expect(record.error_message).to eq('Transaction failed')
        end
      end

      context 'when action_type is Offer' do
        let(:action_type) { 'Offer' }
        let(:record) { create(:offer) }

        it 'logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end

        it 'updates error_message' do
          handler.handle(payload)
          expect(record.reload.error_message).to eq('Transaction failed')
        end
      end

      context 'when action_type is BalancesLock' do
        let(:action_type) { 'BalancesLock' }
        let(:record) { create(:balance_lock) }

        it 'logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end

        it 'updates status and error_message' do
          expect { handler.handle(payload) }.to change { record.reload.status }.from('pending').to('transaction_error')
          expect(record.error_message).to eq('Transaction failed')
        end
      end

      context 'when record is not found' do
        let(:action_type) { 'CoinTransaction' }
        let(:record) { double('CoinTransaction', id: 999) }

        it 'only logs error to Rollbar' do
          expect(Rollbar).to receive(:error).with(
            'Transaction failed',
            action_type: action_type,
            record_id: record.id,
            error_message: 'Transaction failed'
          )
          handler.handle(payload)
        end
      end
    end
  end
end
