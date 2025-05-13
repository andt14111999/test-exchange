# frozen_string_literal: true

require 'rails_helper'

describe KafkaService::Handlers::TickHandler do
  describe '#handle' do
    let(:handler) { described_class.new }
    let(:amm_pool) { create(:amm_pool, pair: 'USDT/VND') }

    context 'when payload is nil' do
      it 'returns nil when payload is nil' do
        expect(handler.handle(nil)).to be_nil
      end
    end

    context 'when payload poolPair is present' do
      it 'processes the tick update' do
        payload = {
          'poolPair' => amm_pool.pair,
          'tickIndex' => 100,
          'liquidityGross' => '100',
          'liquidityNet' => '50',
          'updatedAt' => (Time.current.to_f * 1000).to_i,
          'isSuccess' => 'true'
        }

        expect(handler).to receive(:process_tick_update).with(payload)
        handler.handle(payload)
      end
    end

    context 'when payload poolPair is missing' do
      it 'returns nil' do
        payload = { 'someOtherField' => 'value' }
        expect(handler.handle(payload)).to be_nil
      end
    end
  end

  describe '#process_tick_update' do
    let(:handler) { described_class.new }
    let(:amm_pool) { create(:amm_pool, pair: 'USDT/VND') }
    let(:payload) do
      {
        'poolPair' => amm_pool.pair,
        'tickIndex' => 100,
        'liquidityGross' => '100',
        'liquidityNet' => '50',
        'feeGrowthOutside0' => '0',
        'feeGrowthOutside1' => '0',
        'initialized' => true,
        'tickInitializedTimestamp' => (Time.current.to_f * 1000).to_i,
        'createdAt' => (Time.current.to_f * 1000).to_i,
        'updatedAt' => (Time.current.to_f * 1000).to_i
      }
    end

    it 'calls handle_update_response with the payload' do
      expect(handler).to receive(:handle_update_response).with(payload)
      handler.send(:process_tick_update, payload)
    end

    context 'when ActiveRecord::RecordNotFound is raised' do
      it 'logs the error message' do
        # Stub any Kafka-related methods to prevent actual calls
        allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)

        # Stub the specific error we want to test
        allow(handler).to receive(:handle_update_response).and_raise(ActiveRecord::RecordNotFound.new("Test error"))

        # Expect the specific error message we're testing
        expect(Rails.logger).to receive(:error).with("Failed to find record: Test error")

        # Suppress other error messages that might be logged
        allow(Rails.logger).to receive(:error).with(any_args)

        handler.send(:process_tick_update, payload)
      end
    end

    context 'when StandardError is raised' do
      it 'logs the error message and backtrace' do
        # Stub any Kafka-related methods to prevent actual calls
        allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)

        # Create and stub the error
        error = StandardError.new("Test error")
        allow(error).to receive(:backtrace).and_return([ "line1", "line2" ])
        allow(handler).to receive(:handle_update_response).and_raise(error)

        # Expect the specific error messages we're testing
        expect(Rails.logger).to receive(:error).with("Error processing tick update: Test error")
        expect(Rails.logger).to receive(:error).with("line1\nline2")

        # Suppress other error messages that might be logged
        allow(Rails.logger).to receive(:error).with(any_args)

        handler.send(:process_tick_update, payload)
      end
    end

    it 'creates a new tick if it does not exist' do
      expect {
        handler.send(:process_tick_update, payload)
      }.to change(Tick, :count).by(1)

      tick = Tick.last
      expect(tick.pool_pair).to eq(amm_pool.pair)
      expect(tick.tick_index).to eq(100)
      expect(tick.liquidity_gross).to eq(BigDecimal('100'))
      expect(tick.liquidity_net).to eq(BigDecimal('50'))
      expect(tick.status).to eq('active')
    end

    it 'updates an existing tick' do
      tick = create(:tick, amm_pool: amm_pool, pool_pair: amm_pool.pair, tick_index: 100, tick_key: "#{amm_pool.pair}-100", liquidity_net: '10')

      expect {
        handler.send(:process_tick_update, payload)
      }.not_to change(Tick, :count)

      tick.reload
      expect(tick.liquidity_net).to eq(BigDecimal('50'))
      expect(tick.status).to eq('active')
    end

    it 'checks for existing ticks by tick_key' do
      expect(Tick).to receive(:find_by).with(tick_key: "#{amm_pool.pair}-100").and_call_original
      handler.send(:process_tick_update, payload)
    end

    context 'when handling update response' do
      it 'handles StandardError and logs the error message' do
        allow(handler).to receive(:find_or_create_tick).and_return(create(:tick, amm_pool: amm_pool))
        allow(handler).to receive(:extract_params_from_response).and_raise(StandardError.new("Test error"))

        expect(Rails.logger).to receive(:error).with("Error handling tick update response: Test error")

        handler.send(:handle_update_response, payload)
      end

      it 'does not update tick if message is older than the last update' do
        # Stub any Kafka-related methods to prevent actual calls
        allow_any_instance_of(AmmPool).to receive(:send_event_create_amm_pool)

        # Create a tick with a future updated_at timestamp
        future_time = Time.current + 1.day
        old_tick = create(:tick,
          amm_pool: amm_pool,
          pool_pair: amm_pool.pair,
          tick_index: 100,
          tick_key: "#{amm_pool.pair}-100",
          updated_at: future_time
        )

        # Modify the payload to have an older timestamp
        older_payload = payload.deep_dup
        older_payload['updatedAt'] = ((Time.current - 1.day).to_f * 1000).to_i

        # Stub the find_or_create_tick method to return our tick
        allow(handler).to receive(:find_or_create_tick).and_return(old_tick)

        # Verify that the tick is not updated
        expect_any_instance_of(Tick).not_to receive(:update!)

        # Verify that the error is logged
        expect(Rails.logger).to receive(:error).with(/Error handling tick update response: tick message is older than the last update/)

        # Call the method
        handler.send(:handle_update_response, older_payload)

        # Verify the tick wasn't updated
        expect(old_tick.reload.updated_at).to be_within(1.second).of(future_time)
      end
    end
  end
end
