# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaEvent, type: :model do
  describe 'validations' do
    subject { build(:kafka_event) }

    it { is_expected.to validate_presence_of(:event_id) }
    it { is_expected.to validate_presence_of(:topic_name) }
    it { is_expected.to validate_uniqueness_of(:event_id).scoped_to(:topic_name) }

    context 'status validations' do
      it 'is invalid if status is blank' do
        event = build(:kafka_event)
        event.status = nil
        # Disable callbacks
        allow(event).to receive(:set_status)
        allow(event).to receive(:update_status)
        expect(event).to be_invalid
        expect(event.errors[:status]).to be_present
      end

      it 'is invalid if status is not in allowed values' do
        event = build(:kafka_event, status: 'invalid')
        # Disable callbacks
        allow(event).to receive(:set_status)
        allow(event).to receive(:update_status)
        expect(event).to be_invalid
        expect(event.errors[:status]).to be_present
      end
    end
  end

  describe 'scopes' do
    let!(:processed_event) { create(:kafka_event, status: 'processed', processed_at: Time.current) }
    let!(:pending_event) { create(:kafka_event, status: 'pending') }
    let!(:failed_event) { create(:kafka_event, status: 'failed', payload: { 'errorMessage' => 'Test error' }) }

    describe '.processed' do
      it 'returns events with processed status' do
        expect(described_class.processed).to include(processed_event)
        expect(described_class.processed).not_to include(pending_event, failed_event)
      end
    end

    describe '.unprocessed' do
      it 'returns events with pending status' do
        expect(described_class.unprocessed).to include(pending_event)
        expect(described_class.unprocessed).not_to include(processed_event, failed_event)
      end
    end

    describe '.failed' do
      it 'returns events with failed status' do
        expect(described_class.failed).to include(failed_event)
        expect(described_class.failed).not_to include(processed_event, pending_event)
      end
    end

    describe '.recent' do
      it 'returns events ordered by created_at desc' do
        expect(described_class.recent.to_a).to eq([ failed_event, pending_event, processed_event ])
      end
    end
  end

  describe '#processing_time' do
    let(:event) { create(:kafka_event) }

    context 'when processed_at is present' do
      it 'returns the time difference between processed_at and created_at' do
        event.update!(processed_at: event.created_at + 5.seconds)
        expect(event.processing_time).to eq(5.0)
      end
    end

    context 'when processed_at is nil' do
      it 'returns nil' do
        expect(event.processing_time).to be_nil
      end
    end
  end

  describe '#error_message' do
    let(:event) { create(:kafka_event) }

    context 'when payload contains errorMessage' do
      it 'returns the error message' do
        event.update!(payload: { 'errorMessage' => 'Test error' })
        expect(event.error_message).to eq('Test error')
      end
    end

    context 'when payload does not contain errorMessage' do
      it 'returns nil' do
        event.update!(payload: { 'data' => 'test' })
        expect(event.error_message).to be_nil
      end
    end

    context 'when payload is nil' do
      it 'returns nil' do
        event.update!(payload: nil)
        expect(event.error_message).to be_nil
      end
    end
  end

  describe '#operation_type' do
    let(:event) { create(:kafka_event) }

    context 'when payload contains object.operationType' do
      it 'returns the operation type' do
        event.update!(payload: { 'object' => { 'operationType' => 'create' } })
        expect(event.operation_type).to eq('create')
      end
    end

    context 'when payload contains operation_type' do
      it 'returns the operation type' do
        event.update!(payload: { 'operation_type' => 'update' })
        expect(event.operation_type).to eq('update')
      end
    end

    context 'when payload does not contain operation type' do
      it 'returns nil' do
        event.update!(payload: { 'data' => 'test' })
        expect(event.operation_type).to be_nil
      end
    end

    context 'when payload is nil' do
      it 'returns nil' do
        event.update!(payload: nil)
        expect(event.operation_type).to be_nil
      end
    end
  end

  describe '#object_identifier' do
    let(:event) { create(:kafka_event) }

    context 'when payload contains object.identifier' do
      it 'returns the identifier' do
        event.update!(payload: { 'object' => { 'identifier' => '123' } })
        expect(event.object_identifier).to eq('123')
      end
    end

    context 'when payload contains object.key' do
      it 'returns the key' do
        event.update!(payload: { 'object' => { 'key' => '456' } })
        expect(event.object_identifier).to eq('456')
      end
    end

    context 'when payload contains key' do
      it 'returns the key' do
        event.update!(payload: { 'key' => '789' })
        expect(event.object_identifier).to eq('789')
      end
    end

    context 'when payload does not contain identifier' do
      it 'returns nil' do
        event.update!(payload: { 'data' => 'test' })
        expect(event.object_identifier).to be_nil
      end
    end

    context 'when payload is nil' do
      it 'returns nil' do
        event.update!(payload: nil)
        expect(event.object_identifier).to be_nil
      end
    end
  end

  describe 'callbacks' do
    describe 'before_validation :set_status' do
      it 'sets status to pending on create' do
        event = build(:kafka_event, status: nil)
        event.valid?
        expect(event.status).to eq('pending')
      end
    end

    describe 'before_save :update_status' do
      let(:event) { create(:kafka_event) }

      context 'when error_message is present' do
        it 'sets status to failed' do
          event.update!(payload: { 'errorMessage' => 'Test error' })
          expect(event.status).to eq('failed')
        end
      end

      context 'when processed_at is present' do
        it 'sets status to processed' do
          event.update!(processed_at: Time.current)
          expect(event.status).to eq('processed')
        end
      end

      context 'when neither error_message nor processed_at is present' do
        it 'sets status to pending' do
          event.update!(payload: { 'data' => 'test' })
          expect(event.status).to eq('pending')
        end
      end
    end
  end

  describe '.ransackable_attributes' do
    it 'returns the correct attributes' do
      expected_attributes = %w[
        created_at
        event_id
        id
        payload
        processed_at
        status
        topic_name
        updated_at
      ]
      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe '.ransackable_associations' do
    it 'returns an empty array' do
      expect(described_class.ransackable_associations).to eq([])
    end
  end
end
