# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Handlers::CoinWithdrawalHandler, type: :service do
  let(:handler) { described_class.new }
  let(:user) { create(:user) }
  let(:coin_account) { create(:coin_account, :usdt_main, user: user, balance: 1000.0) }
  let(:coin_withdrawal) do
    coin_account # Ensure coin account exists
    build(:coin_withdrawal, user: user, coin_currency: 'usdt', coin_amount: 100.0).tap do |withdrawal|
      withdrawal.save(validate: false) # Skip validation to avoid balance checks
    end
  end

  describe '#handle' do
    context 'when payload is nil' do
      it 'returns without processing' do
        expect(handler.handle(nil)).to be_nil
      end
    end

    context 'when payload contains object with COIN_TRANSACTION actionType' do
      it 'calls process_transaction_response' do
        payload = {
          'object' => {
            'actionType' => 'COIN_TRANSACTION',
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }

        expect(handler).to receive(:process_transaction_response).with(payload)
        handler.handle(payload)
      end
    end

    context 'when payload uses legacy format' do
      it 'calls handle_legacy_format' do
        payload = {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE,
          'identifier' => coin_withdrawal.id.to_s
        }

        expect(handler).to receive(:handle_legacy_format).with(payload)
        handler.handle(payload)
      end
    end
  end

  describe '#process_transaction_response' do
    context 'when object is missing' do
      it 'returns early' do
        payload = { 'isSuccess' => true }

        expect(handler.send(:process_transaction_response, payload)).to be_nil
      end
    end

    context 'when identifier is missing' do
      it 'returns early' do
        payload = {
          'object' => { 'status' => 'COMPLETED' },
          'isSuccess' => true
        }

        expect(handler.send(:process_transaction_response, payload)).to be_nil
      end
    end

    context 'when coin withdrawal is not found' do
      it 'returns early' do
        payload = {
          'object' => {
            'identifier' => '999',
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: '999').and_return(nil)

        expect(handler.send(:process_transaction_response, payload)).to be_nil
      end
    end

    context 'when request is not successful' do
      it 'fails the withdrawal' do
        coin_withdrawal.update_column(:status, 'processing')
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => false,
          'errorMessage' => 'Network error'
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)

        expect { handler.send(:process_transaction_response, payload) }
          .to change { coin_withdrawal.reload.status }.from('processing').to('failed')
      end
    end

    context 'when status is COMPLETED' do
      it 'processes completed response' do
        coin_withdrawal.update_column(:status, 'processing')
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(handler).to receive(:process_completed_response).with(coin_withdrawal)

        handler.send(:process_transaction_response, payload)
      end
    end

    context 'when status is FAILED' do
      it 'processes failed response' do
        coin_withdrawal.update_column(:status, 'processing')
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'FAILED',
            'statusExplanation' => 'Transaction failed'
          },
          'isSuccess' => true,
          'errorMessage' => 'Transaction failed'
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(handler).to receive(:process_failed_response).with(coin_withdrawal, 'Transaction failed')

        handler.send(:process_transaction_response, payload)
      end
    end

    context 'when status is PROCESSING' do
      it 'processes processing response' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'PROCESSING'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(handler).to receive(:process_processing_response).with(coin_withdrawal)

        handler.send(:process_transaction_response, payload)
      end
    end

    context 'when status is CANCELLED' do
      it 'processes cancelled response' do
        coin_withdrawal.update_column(:status, 'processing')
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'CANCELLED'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(handler).to receive(:process_cancelled_response).with(coin_withdrawal)

        handler.send(:process_transaction_response, payload)
      end
    end
  end

  describe '#process_completed_response' do
    it 'completes the withdrawal when transition is allowed' do
      coin_withdrawal.update_column(:status, 'processing')

      expect(Rails.logger).to receive(:info).with(/Coin withdrawal completed/)

      expect { handler.send(:process_completed_response, coin_withdrawal) }
        .to change { coin_withdrawal.reload.status }.from('processing').to('completed')
    end

    it 'does not complete when transition is not allowed' do
      coin_withdrawal.update_column(:status, 'completed')

      expect(Rails.logger).to receive(:info).with("Coin withdrawal cannot complete for withdrawal_id=#{coin_withdrawal.id}")

      expect { handler.send(:process_completed_response, coin_withdrawal) }
        .not_to change { coin_withdrawal.reload.status }
    end
  end

  describe '#process_failed_response' do
    it 'fails the withdrawal when transition is allowed' do
      coin_withdrawal.update_column(:status, 'processing')
      error_message = 'Network timeout'

      expect(Rails.logger).to receive(:info).with(/Coin withdrawal failed/)

      expect { handler.send(:process_failed_response, coin_withdrawal, error_message) }
        .to change { coin_withdrawal.reload.status }.from('processing').to('failed')

      expect(coin_withdrawal.reload.explanation).to eq("; #{error_message}")
    end

    it 'does not fail when transition is not allowed' do
      coin_withdrawal.update_column(:status, 'completed')

      expect(Rails.logger).to receive(:info).with("Coin withdrawal cannot fail for withdrawal_id=#{coin_withdrawal.id}")

      expect { handler.send(:process_failed_response, coin_withdrawal, 'Error') }
        .not_to change { coin_withdrawal.reload.status }
    end

    it 'fails without error message' do
      coin_withdrawal.update_column(:status, 'processing')

      expect(Rails.logger).to receive(:info).with(/Coin withdrawal failed/)

      expect { handler.send(:process_failed_response, coin_withdrawal, nil) }
        .to change { coin_withdrawal.reload.status }.from('processing').to('failed')

      expect(coin_withdrawal.reload.explanation).to be_nil
    end
  end

  describe '#process_processing_response' do
    it 'processes the withdrawal when transition is allowed' do
      expect(Rails.logger).to receive(:info).with(/Coin withdrawal processing/)

      expect { handler.send(:process_processing_response, coin_withdrawal) }
        .to change { coin_withdrawal.reload.status }.from('pending').to('processing')
    end

    it 'does not process when transition is not allowed' do
      coin_withdrawal.update_column(:status, 'completed')

      expect(Rails.logger).to receive(:info).with("Coin withdrawal cannot process for withdrawal_id=#{coin_withdrawal.id}")

      expect { handler.send(:process_processing_response, coin_withdrawal) }
        .not_to change { coin_withdrawal.reload.status }
    end
  end

  describe '#process_cancelled_response' do
    it 'cancels the withdrawal when transition is allowed' do
      expect(Rails.logger).to receive(:info).with(/Coin withdrawal cancelled/)

      expect { handler.send(:process_cancelled_response, coin_withdrawal) }
        .to change { coin_withdrawal.reload.status }.from('pending').to('cancelled')
    end

    it 'does not cancel when transition is not allowed' do
      coin_withdrawal.update_column(:status, 'completed')

      expect(Rails.logger).to receive(:info).with("Coin withdrawal cannot cancel for withdrawal_id=#{coin_withdrawal.id}")

      expect { handler.send(:process_cancelled_response, coin_withdrawal) }
        .not_to change { coin_withdrawal.reload.status }
    end
  end

  describe 'legacy format handling' do
    let(:identifier) { SecureRandom.uuid }
    let(:payload) do
      {
        'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE,
        'identifier' => identifier,
        'amount' => '100.0',
        'currency' => 'BTC',
        'address' => '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
      }
    end

    context 'when operation type is COIN_WITHDRAWAL_CREATE' do
      it 'processes withdrawal creation' do
        expect(handler).to receive(:process_withdrawal_create).with(payload)
        handler.send(:handle_legacy_format, payload)
      end

      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal create: #{identifier}")
        handler.send(:handle_legacy_format, payload)
      end
    end

    context 'when operation type is COIN_WITHDRAWAL_RELEASING' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_RELEASING,
          'identifier' => identifier,
          'amount' => '100.0',
          'currency' => 'BTC',
          'address' => '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa'
        }
      end

      it 'processes withdrawal releasing' do
        expect(handler).to receive(:process_withdrawal_releasing).with(payload)
        handler.send(:handle_legacy_format, payload)
      end

      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal releasing: #{identifier}")
        handler.send(:handle_legacy_format, payload)
      end
    end

    context 'when operation type is COIN_WITHDRAWAL_FAILED' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_FAILED,
          'identifier' => identifier,
          'amount' => '100.0',
          'currency' => 'BTC',
          'address' => '1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
          'error' => 'Insufficient funds'
        }
      end

      it 'processes withdrawal failure' do
        expect(handler).to receive(:process_withdrawal_failed).with(payload)
        handler.send(:handle_legacy_format, payload)
      end

      it 'logs the operation' do
        expect(Rails.logger).to receive(:info).with("Processing withdrawal failed: #{identifier}")
        handler.send(:handle_legacy_format, payload)
      end
    end

    context 'when operation type is unknown' do
      let(:payload) do
        {
          'operationType' => 'UNKNOWN_OPERATION',
          'identifier' => identifier
        }
      end

      it 'does not process anything' do
        expect(handler).not_to receive(:process_withdrawal_create)
        expect(handler).not_to receive(:process_withdrawal_releasing)
        expect(handler).not_to receive(:process_withdrawal_failed)
        expect(Rails.logger).not_to receive(:info)
        handler.send(:handle_legacy_format, payload)
      end
    end

    context 'when payload is missing identifier' do
      let(:payload) do
        {
          'operationType' => KafkaService::Config::OperationTypes::COIN_WITHDRAWAL_CREATE,
          'amount' => '100.0',
          'currency' => 'BTC'
        }
      end

      it 'logs with empty identifier' do
        expect(Rails.logger).to receive(:info).with('Processing withdrawal create: ')
        handler.send(:handle_legacy_format, payload)
      end
    end
  end

  describe 'inheritance' do
    it 'inherits from BaseHandler' do
      expect(described_class.superclass).to eq(KafkaService::Handlers::BaseHandler)
    end
  end
end
