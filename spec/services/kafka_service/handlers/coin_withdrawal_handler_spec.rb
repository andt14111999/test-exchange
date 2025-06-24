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

    context 'when payload contains valid object' do
      it 'calls process_transaction_response' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }

        expect(handler).to receive(:process_transaction_response).with(payload)
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
      it 'updates withdrawal status to failed' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => false,
          'errorMessage' => 'Network error'
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(Rails.logger).to receive(:info).twice

        expect { handler.send(:process_transaction_response, payload) }
          .to change { coin_withdrawal.reload.status }.from('pending').to('failed')

        expect(coin_withdrawal.reload.explanation).to eq('Network error')
      end
    end

    context 'when status is COMPLETED' do
      it 'updates withdrawal status to completed' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(Rails.logger).to receive(:info).twice

        expect { handler.send(:process_transaction_response, payload) }
          .to change { coin_withdrawal.reload.status }.from('pending').to('completed')
      end
    end

    context 'when status is FAILED' do
      it 'updates withdrawal status to failed with explanation' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'FAILED',
            'statusExplanation' => 'Transaction failed'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(Rails.logger).to receive(:info).twice

        expect { handler.send(:process_transaction_response, payload) }
          .to change { coin_withdrawal.reload.status }.from('pending').to('failed')

        expect(coin_withdrawal.reload.explanation).to eq('Transaction failed')
      end
    end

    context 'when status is PROCESSING' do
      it 'updates withdrawal status to processing' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'PROCESSING'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(Rails.logger).to receive(:info).twice

        expect { handler.send(:process_transaction_response, payload) }
          .to change { coin_withdrawal.reload.status }.from('pending').to('processing')
      end
    end

    context 'when status is CANCELLED' do
      it 'updates withdrawal status to cancelled' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'CANCELLED'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(Rails.logger).to receive(:info).twice

        expect { handler.send(:process_transaction_response, payload) }
          .to change { coin_withdrawal.reload.status }.from('pending').to('cancelled')
      end
    end

    context 'when status explanation is provided but status is not FAILED' do
      it 'does not include explanation in update' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED',
            'statusExplanation' => 'Some explanation'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        expect(Rails.logger).to receive(:info).twice

        expect { handler.send(:process_transaction_response, payload) }
          .to change { coin_withdrawal.reload.status }.from('pending').to('completed')

        expect(coin_withdrawal.reload.explanation).to be_nil
      end
    end

    context 'logging' do
      it 'logs processing information' do
        payload = {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'PROCESSING'
          },
          'isSuccess' => true
        }

        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)

        expect(Rails.logger).to receive(:info).with(
          "Processing Kafka event for withdrawal_id=#{coin_withdrawal.id}, current status: pending, kafka status: PROCESSING, isSuccess: true"
        )
        expect(Rails.logger).to receive(:info).with(
          "Coin withdrawal status updated for withdrawal_id=#{coin_withdrawal.id} from pending to processing"
        )

        handler.send(:process_transaction_response, payload)
      end
    end
  end

  describe '#update_kafka_event_message' do
    let(:kafka_event) do
      create(
        :kafka_event,
        event_id: 'test-event-123',
        topic_name: KafkaService::Config::Topics::COIN_WITHDRAWAL_UPDATE,
        process_message: nil
      )
    end

    context 'when event_id is found in payload' do
      let(:payload) do
        {
          'eventId' => 'test-event-123',
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }
      end

      it 'adds process message to existing kafka event' do
        kafka_event # Create the event
        message = 'Coin withdrawal status updated from pending to completed'

        handler.send(:update_kafka_event_message, payload, message)

        kafka_event.reload
        expect(kafka_event.process_message).to include(message)
        expect(kafka_event.process_message).to match(/\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\]/)
      end

      it 'appends to existing process message' do
        kafka_event.update(process_message: '[2025-01-01 12:00:00] Previous message')
        message = 'New message'

        handler.send(:update_kafka_event_message, payload, message)

        kafka_event.reload
        expect(kafka_event.process_message).to include('Previous message')
        expect(kafka_event.process_message).to include('New message')
        expect(kafka_event.process_message.lines.count).to eq(2)
      end
    end

    context 'when event_id is not found in payload' do
      let(:payload) do
        {
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }
      end

      it 'returns early without updating' do
        kafka_event # Create the event
        message = 'Test message'

        handler.send(:update_kafka_event_message, payload, message)

        kafka_event.reload
        expect(kafka_event.process_message).to be_nil
      end
    end

    context 'when kafka event is not found' do
      let(:payload) do
        {
          'eventId' => 'non-existent-event',
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }
      end

      it 'returns early without error' do
        message = 'Test message'

        expect { handler.send(:update_kafka_event_message, payload, message) }.not_to raise_error
      end
    end

    context 'when update fails' do
      let(:payload) do
        {
          'eventId' => 'test-event-123',
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }
      end

      it 'logs error and continues' do
        kafka_event # Create the event
        allow(kafka_event).to receive(:update).and_raise(StandardError.new('Update failed'))
        allow(KafkaEvent).to receive(:find_by).and_return(kafka_event)

        expect(Rails.logger).to receive(:error).with('Failed to update KafkaEvent process_message: Update failed')

        handler.send(:update_kafka_event_message, payload, 'Test message')
      end
    end
  end

  describe 'process_message integration' do
    let(:kafka_event) do
      create(
        :kafka_event,
        event_id: 'test-event-123',
        topic_name: KafkaService::Config::Topics::COIN_WITHDRAWAL_UPDATE
      )
    end

    context 'when withdrawal succeeds' do
      it 'updates kafka event with success message' do
        payload = {
          'eventId' => 'test-event-123',
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'COMPLETED'
          },
          'isSuccess' => true
        }

        kafka_event # Create the event
        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        allow(Rails.logger).to receive(:info)

        handler.send(:process_transaction_response, payload)

        kafka_event.reload
        expect(kafka_event.process_message).to include('Coin withdrawal status updated from pending to completed')
      end
    end

    context 'when withdrawal fails' do
      it 'updates kafka event with failure message' do
        payload = {
          'eventId' => 'test-event-123',
          'object' => {
            'identifier' => coin_withdrawal.id.to_s,
            'status' => 'FAILED'
          },
          'isSuccess' => false,
          'errorMessage' => 'Network error'
        }

        kafka_event # Create the event
        allow(CoinWithdrawal).to receive(:find_by).with(id: coin_withdrawal.id.to_s).and_return(coin_withdrawal)
        allow(Rails.logger).to receive(:info)

        handler.send(:process_transaction_response, payload)

        kafka_event.reload
        expect(kafka_event.process_message).to include('Coin withdrawal failed: Network error')
      end
    end
  end

  describe 'inheritance' do
    it 'inherits from BaseHandler' do
      expect(described_class.superclass).to eq(KafkaService::Handlers::BaseHandler)
    end
  end
end
