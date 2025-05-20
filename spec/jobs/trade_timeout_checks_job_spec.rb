# frozen_string_literal: true

require 'rails_helper'

RSpec.describe TradeTimeoutChecksJob do
  describe '#perform' do
    context 'with unpaid trades' do
      it 'processes timed out unpaid trades' do
        # Create an unpaid trade that has timed out
        timed_out_trade = create(:trade, status: 'unpaid', created_at: 16.minutes.ago)

        # Create another unpaid trade that has not timed out
        not_timed_out_trade = create(:trade, status: 'unpaid', created_at: 10.minutes.ago)

        # Mock the trade service to check for cancel call
        trade_service = instance_double(KafkaService::Services::Trade::TradeService)
        allow(KafkaService::Services::Trade::TradeService).to receive(:new).and_return(trade_service)
        expect(trade_service).to receive(:cancel).with(trade: timed_out_trade)

        # Run the job
        described_class.new.perform

        # Reload the trades to get updated statuses
        timed_out_trade.reload
        not_timed_out_trade.reload

        # The not timed out trade should still be unpaid
        expect(not_timed_out_trade).to be_unpaid
      end
    end

    context 'with paid trades' do
      it 'processes timed out paid trades' do
        # Create a paid trade that has timed out
        timed_out_trade = create(:trade, status: 'paid', paid_at: 16.minutes.ago)

        # Create another paid trade that has not timed out
        not_timed_out_trade = create(:trade, status: 'paid', paid_at: 10.minutes.ago)

        # Run the job
        described_class.new.perform

        # Reload the trades to get updated statuses
        timed_out_trade.reload
        not_timed_out_trade.reload

        # The timed out trade should be disputed
        expect(timed_out_trade).to be_disputed

        # The not timed out trade should still be paid
        expect(not_timed_out_trade).to be_paid
      end
    end

    context 'with errors during processing' do
      it 'handles errors for unpaid trades and continues processing' do
        # Create a trade that will raise an error
        trade = create(:trade, status: 'unpaid', created_at: 16.minutes.ago)

        # Mock the error during perform_timeout_checks!
        allow(trade).to receive(:perform_timeout_checks!).and_raise("Test error")

        # Allow Rails.logger to receive the error message
        expect(Rails.logger).to receive(:error).with(/Error processing timeout for trade/).at_least(:once)

        # Create a scope that includes our trade
        unpaid_scope = double("Unpaid Scope")
        allow(unpaid_scope).to receive(:find_each).and_yield(trade)
        allow(Trade).to receive_messages(unpaid: unpaid_scope, paid: Trade.none)

        # The job should not raise an exception
        expect { described_class.new.perform }.not_to raise_error
      end

      it 'handles errors for paid trades and continues processing' do
        # Create a trade that will raise an error
        trade = create(:trade, status: 'paid', paid_at: 16.minutes.ago)

        # Mock the error during perform_timeout_checks!
        allow(trade).to receive(:perform_timeout_checks!).and_raise("Test error")

        # Allow Rails.logger to receive the error message
        expect(Rails.logger).to receive(:error).with(/Error processing timeout for paid trade/).at_least(:once)

        # Create a scope that includes our trade
        paid_scope = double("Paid Scope")
        allow(paid_scope).to receive(:find_each).and_yield(trade)
        allow(Trade).to receive_messages(unpaid: Trade.none, paid: paid_scope)

        # The job should not raise an exception
        expect { described_class.new.perform }.not_to raise_error
      end
    end
  end
end
