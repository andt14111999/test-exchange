require 'rails_helper'

describe KafkaService::Handlers::AmmPoolHandler do
  describe '#handle' do
    let(:handler) { described_class.new }
    let(:amm_pool) { create(:amm_pool) }

    context 'when payload is nil' do
      it 'returns nil when payload is nil' do
        expect(handler.handle(nil)).to be_nil
      end
    end

    context 'when payload object pair is present' do
      it 'processes the amm pool update' do
        payload = {
          'object' => {
            'pair' => amm_pool.pair,
            'feePercentage' => 0.005,
            'updatedAt' => (Time.current.to_f * 1000).to_i
          },
          'isSuccess' => 'true'
        }

        expect(handler).to receive(:process_amm_pool_update).with(payload)
        handler.handle(payload)
      end
    end

    context 'when payload object pair is missing' do
      it 'does not process the update when object is missing' do
        payload = { 'isSuccess' => 'true' }

        expect(handler).not_to receive(:process_amm_pool_update)
        handler.handle(payload)
      end

      it 'does not process the update when pair is blank' do
        payload = {
          'object' => {
            'pair' => '',
            'feePercentage' => 0.005
          },
          'isSuccess' => 'true'
        }

        expect(handler).not_to receive(:process_amm_pool_update)
        handler.handle(payload)
      end
    end
  end

  describe '#process_amm_pool_update' do
    let(:handler) { described_class.new }
    let(:amm_pool) { create(:amm_pool) }

    context 'when record is not found' do
      it 'logs the error' do
        payload = {
          'object' => {
            'pair' => 'UNKNOWN/PAIR',
            'feePercentage' => 0.005
          }
        }

        allow(Rails.logger).to receive(:error)
        expect(Rails.logger).to receive(:error).with(/Error handling update response: Couldn't find AmmPool/)
        handler.send(:process_amm_pool_update, payload)
      end

      it 'rescues and logs ActiveRecord::RecordNotFound error' do
        payload = {
          'object' => {
            'pair' => 'UNKNOWN/PAIR',
            'feePercentage' => 0.005
          }
        }

        allow(AmmPool).to receive(:find_by!).and_raise(ActiveRecord::RecordNotFound.new('Could not find record'))
        expect(Rails.logger).to receive(:error).with('Error handling update response: Could not find record')

        handler.send(:process_amm_pool_update, payload)
      end

      it 'logs the exact error message for record not found' do
        payload = {
          'object' => {
            'pair' => 'UNKNOWN/PAIR'
          }
        }

        error_message = "Couldn't find AmmPool with pair='UNKNOWN/PAIR'"
        allow(handler).to receive(:handle_update_response).and_raise(ActiveRecord::RecordNotFound.new(error_message))
        expect(Rails.logger).to receive(:error).with("Failed to find record: #{error_message}")

        handler.send(:process_amm_pool_update, payload)
      end
    end

    context 'when standard error occurs' do
      it 'logs the error' do
        payload = {
          'object' => {
            'pair' => amm_pool.pair,
            'feePercentage' => 0.005
          }
        }

        allow(handler).to receive(:handle_update_response).and_raise(StandardError, 'Test error')
        expect(Rails.logger).to receive(:error).with(/Error processing deposit/)
        expect(Rails.logger).to receive(:error) # For backtrace

        handler.send(:process_amm_pool_update, payload)
      end
    end
  end

  describe '#handle_update_response' do
    let(:handler) { described_class.new }
    let(:amm_pool) { create(:amm_pool, updated_at: Time.current) }

    context 'when isSuccess is false' do
      it 'updates only the status explanation' do
        payload = {
          'isSuccess' => 'false',
          'errorMessage' => 'Invalid pool parameters',
          'object' => {
            'pair' => amm_pool.pair
          }
        }

        handler.send(:handle_update_response, payload)
        amm_pool.reload

        expect(amm_pool.status_explanation).to eq('Exchange Engine: Invalid pool parameters')
      end
    end

    context 'when error occurs' do
      it 'logs the error' do
        payload = {
          'isSuccess' => 'true',
          'object' => {
            'pair' => amm_pool.pair
          }
        }

        allow(AmmPool).to receive(:find_by!).and_raise(StandardError, 'Test error')
        expect(Rails.logger).to receive(:error).with(/Error handling update response/)

        handler.send(:handle_update_response, payload)
      end
    end
  end

  describe '#extract_params_from_response' do
    let(:handler) { described_class.new }
    let(:object) do
      {
        'feePercentage' => '0.003',
        'updatedAt' => 1650000000000,
        'isActive' => true
      }
    end

    it 'sets status to active when isActive is true' do
      params = handler.send(:extract_params_from_response, object)
      expect(params[:status]).to eq('active')
    end

    it 'sets status to inactive when isActive is false' do
      object['isActive'] = false
      params = handler.send(:extract_params_from_response, object)
      expect(params[:status]).to eq('inactive')
    end

    it 'compacts the results to remove nil values' do
      minimal_object = {
        'feePercentage' => '0.003',
        'updatedAt' => 1650000000000,
        'isActive' => true
      }
      params = handler.send(:extract_params_from_response, minimal_object)
      expect(params[:fee_percentage]).to eq(BigDecimal('0.003'))
      expect(params[:updated_at]).to be_present
      expect(params[:status]).to eq('active')
    end
  end
end
