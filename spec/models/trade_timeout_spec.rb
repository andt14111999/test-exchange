# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Trade, type: :model do
  describe 'timeout checks' do
    describe '#unpaid_timeout?' do
      context 'when trade is not in unpaid status' do
        it 'returns false' do
          trade = create(:trade, status: 'awaiting')
          expect(trade).not_to be_unpaid_timeout
        end
      end

      context 'when trade is unpaid but not for 15 minutes' do
        it 'returns false' do
          trade = create(:trade, status: 'unpaid', created_at: 10.minutes.ago)
          expect(trade).not_to be_unpaid_timeout
        end
      end

      context 'when trade is unpaid for more than 15 minutes' do
        it 'returns true' do
          trade = create(:trade, status: 'unpaid', created_at: 16.minutes.ago)
          expect(trade).to be_unpaid_timeout
        end
      end
    end

    describe '#paid_timeout?' do
      context 'when trade is not in paid status' do
        it 'returns false' do
          trade = create(:trade, status: 'unpaid')
          expect(trade).not_to be_paid_timeout
        end
      end

      context 'when trade is paid but not for 15 minutes' do
        it 'returns false' do
          trade = create(:trade, status: 'paid', paid_at: 10.minutes.ago)
          expect(trade).not_to be_paid_timeout
        end
      end

      context 'when trade is paid for more than 15 minutes' do
        it 'returns true' do
          trade = create(:trade, status: 'paid', paid_at: 16.minutes.ago)
          expect(trade).to be_paid_timeout
        end
      end
    end

    describe '#perform_timeout_checks!' do
      context 'when trade is unpaid and timed out' do
        it 'calls send_trade_cancel_to_kafka' do
          trade = create(:trade, status: 'unpaid', created_at: 16.minutes.ago)

          # Expect the method to be called
          expect(trade).to receive(:send_trade_cancel_to_kafka)

          # Perform the timeout checks
          result = trade.perform_timeout_checks!

          # Method should return true
          expect(result).to be_truthy
        end
      end

      context 'when trade is paid and timed out' do
        it 'marks the trade as disputed' do
          trade = create(:trade, status: 'paid', paid_at: 16.minutes.ago)
          trade.perform_timeout_checks!
          expect(trade).to be_disputed
        end
      end

      context 'when trade is not timed out' do
        it 'returns false and does not change status' do
          trade = create(:trade, status: 'unpaid', created_at: 10.minutes.ago)
          original_status = trade.status
          result = trade.perform_timeout_checks!
          expect(result).to be_falsey
          expect(trade.status).to eq(original_status)
        end
      end
    end
  end
end
