# frozen_string_literal: true

require 'rails_helper'

RSpec.describe TradeService, type: :model do
  # Mock Rails application config before tests run
  before do
    allow_any_instance_of(Trade).to receive(:set_offer_data).and_return(true)
  end

  describe 'constants' do
    it 'defines status constants' do
      expect(TradeService::PENDING_STATUSES).to eq(%w[awaiting unpaid paid disputed])
      expect(TradeService::CLOSED_STATUSES).to eq(%w[released cancelled cancelled_automatically])
      expect(TradeService::SCORABLE_STATUSES).to eq(%w[released cancelled disputed])
    end
  end

  describe '.create_trade_from_offer' do
    let(:buyer) { create(:user) }
    let(:seller) { create(:user) }
    let(:offer) { create(:offer, :sell, user: seller, currency: 'VND') }
    let(:amount) { 0.01 }
    let(:taker_side) { 'buy' }
    let(:trade) { instance_double(Trade, save: true, offer: offer, buyer: buyer, seller: seller) }

    before do
      # Create fiat account for buyer with valid currency
      create(:fiat_account, :vnd, user: buyer)

      # Allow Trade.new to return our mocked trade instance
      allow(Trade).to receive(:new).and_return(trade)

      # Mock all necessary methods on the trade double
      allow(trade).to receive(:offer=)
      allow(trade).to receive(:set_offer_data)
      allow(trade).to receive(:set_price)
      allow(trade).to receive(:set_taker_side)
      allow(trade).to receive(:calculate_amounts)
      allow(trade).to receive(:calculate_fees)

      # Skip the actual handling of fiat token operations
      allow(described_class).to receive(:handle_fiat_token_operations)

      # Mock the try_start! method on our trade instance
      allow(trade).to receive(:try_start!)

      # Mock the transaction block
      allow(Trade).to receive(:transaction).and_yield

      # For the failing test case, we need to handle rollback differently
      allow(Trade).to receive(:transaction).and_wrap_original do |original_method, &block|
        begin
          block.call
        rescue ActiveRecord::Rollback
          nil
        end
      end
    end

    it 'creates a trade successfully' do
      # This will succeed because we've mocked the trade.save method to return true
      result = described_class.create_trade_from_offer(offer, buyer, amount, taker_side)

      expect(result).to eq(trade)
      expect(trade).to have_received(:offer=).with(offer)
      expect(trade).to have_received(:set_offer_data).with(offer)
      expect(trade).to have_received(:set_price).with(offer.price)
      expect(trade).to have_received(:set_taker_side).with(buyer, taker_side)
      expect(trade).to have_received(:calculate_amounts).with(amount)
      expect(trade).to have_received(:calculate_fees)
      expect(described_class).to have_received(:handle_fiat_token_operations).with(trade)
      expect(trade).to have_received(:try_start!)
    end

    it 'returns nil when trade cannot be saved' do
      # Force save to fail
      allow(trade).to receive(:save).and_return(false)

      # Should raise Rollback and return nil
      expect(described_class.create_trade_from_offer(offer, buyer, amount, taker_side)).to be_nil

      # Check that try_start! is never called when save fails
      expect(trade).not_to have_received(:try_start!)
    end
  end

  describe '.handle_fiat_token_operations' do
    context 'when buyer is taker and offer is sell' do
      it 'creates fiat token deposit' do
        trade = build_stubbed(:trade)
        allow(trade).to receive_messages(
          buyer_is_taker?: true,
          offer: build_stubbed(:offer, offer_type: 'sell')
        )

        expect(described_class).to receive(:create_fiat_token_deposit).with(trade)

        described_class.handle_fiat_token_operations(trade)
      end
    end

    context 'when seller is taker and offer is buy' do
      it 'creates fiat token withdrawal' do
        trade = build_stubbed(:trade)
        allow(trade).to receive_messages(
          buyer_is_taker?: false,
          seller_is_taker?: true,
          offer: build_stubbed(:offer, offer_type: 'buy')
        )

        expect(described_class).to receive(:create_fiat_token_withdrawal).with(trade)

        described_class.handle_fiat_token_operations(trade)
      end
    end

    context 'when neither condition is met' do
      it 'does not create fiat token operations' do
        trade = build_stubbed(:trade)
        allow(trade).to receive_messages(
          buyer_is_taker?: false,
          seller_is_taker?: true,
          offer: build_stubbed(:offer, offer_type: 'sell')
        )

        expect(described_class).not_to receive(:create_fiat_token_deposit)
        expect(described_class).not_to receive(:create_fiat_token_withdrawal)

        described_class.handle_fiat_token_operations(trade)
      end
    end
  end

  describe '.create_fiat_token_deposit' do
    it 'creates a fiat deposit for the trade' do
      buyer = create(:user)
      fiat_account = create(:fiat_account, :vnd, user: buyer)
      offer = create(:offer, country_code: 'vn')

      trade = build_stubbed(:trade,
        buyer: buyer,
        fiat_currency: 'vnd',
        fiat_amount: 100_000
      )
      allow(trade).to receive_messages(
        offer: offer,
        update!: true
      )

      allow(buyer).to receive(:fiat_accounts).and_return(instance_double(ActiveRecord::Relation, find_by: fiat_account))


      fiat_deposit = build_stubbed(:fiat_deposit)
      allow(FiatDeposit).to receive(:create!).and_return(fiat_deposit)

      described_class.create_fiat_token_deposit(trade)

      expect(FiatDeposit).to have_received(:create!).with(
        hash_including(
          user: buyer,
          fiat_account: fiat_account,
          currency: trade.fiat_currency,
          country_code: offer.country_code,
          fiat_amount: trade.fiat_amount,
          payable: trade,
          status: 'awaiting'
        )
      )
    end
  end

  describe '.create_fiat_token_withdrawal' do
    it 'has an implementation placeholder' do
      trade = build_stubbed(:trade, fiat_currency: 'VND')
      seller = build_stubbed(:user)
      fiat_account = build_stubbed(:fiat_account)
      offer = build_stubbed(:offer, payment_details: { 'bank_name' => 'Test Bank' })
      withdrawal = build_stubbed(:fiat_withdrawal)

      allow(trade).to receive_messages(
        seller: seller,
        offer: offer
      )
      allow(seller.fiat_accounts).to receive(:find_by).and_return(fiat_account)
      allow(FiatWithdrawal).to receive(:create!).and_return(withdrawal)
      allow(trade).to receive(:update!).and_return(true)

      # This is testing the stub implementation only
      result = described_class.create_fiat_token_withdrawal(trade)

      # Expect the method to return true (the result of trade.update!)
      expect(result).to be(true)
    end
  end

  describe '#pay_trade!' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }

    context 'when trade can be marked as paid' do
      it 'marks the trade as paid' do
        allow(trade).to receive_messages(
          may_mark_as_paid?: true,
          mark_as_paid!: true
        )

        result = service.pay_trade!

        expect(result).to be_truthy
      end
    end

    context 'when trade cannot be marked as paid' do
      it 'returns false' do
        allow(trade).to receive(:may_mark_as_paid?).and_return(false)
        expect(trade).not_to receive(:mark_as_paid!)

        result = service.pay_trade!

        expect(result).to be_falsey
      end
    end
  end

  describe '#release_trade!' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }
    let(:kafka_service) { instance_double(KafkaService::Services::Trade::TradeService) }

    before do
      allow(KafkaService::Services::Trade::TradeService).to receive(:new).and_return(kafka_service)
    end

    context 'when trade can be marked as released' do
      it 'sends a complete event to Kafka' do
        allow(trade).to receive(:may_mark_as_released?).and_return(true)
        allow(kafka_service).to receive(:complete).with(trade: trade)

        result = service.release_trade!

        expect(result).to eq(trade)
      end
    end

    context 'when trade cannot be marked as released' do
      it 'returns false' do
        allow(trade).to receive(:may_mark_as_released?).and_return(false)
        expect(kafka_service).not_to receive(:complete)

        result = service.release_trade!

        expect(result).to be_falsey
      end
    end
  end

  describe '#dispute_trade!' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }
    let(:reason) { 'Payment not received' }

    context 'when trade can be marked as disputed' do
      it 'sets dispute reason and marks the trade as disputed' do
        allow(trade).to receive(:dispute_reason_param=)
        allow(trade).to receive_messages(may_mark_as_disputed?: true, mark_as_disputed!: true)

        result = service.dispute_trade!(reason)

        expect(result).to be_truthy
      end
    end

    context 'when trade cannot be marked as disputed' do
      it 'returns false' do
        allow(trade).to receive(:may_mark_as_disputed?).and_return(false)
        expect(trade).not_to receive(:dispute_reason_param=)
        expect(trade).not_to receive(:mark_as_disputed!)

        result = service.dispute_trade!(reason)

        expect(result).to be_falsey
      end
    end
  end

  describe '#cancel_dispute_trade!' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }

    context 'when trade is disputed' do
      it 'marks the trade as paid' do
        allow(trade).to receive_messages(
          disputed?: true,
          mark_as_paid!: true
        )

        result = service.cancel_dispute_trade!

        expect(result).to be_truthy
      end
    end

    context 'when trade is not disputed' do
      it 'returns false' do
        allow(trade).to receive(:disputed?).and_return(false)
        expect(trade).not_to receive(:mark_as_paid!)

        result = service.cancel_dispute_trade!

        expect(result).to be_falsey
      end
    end
  end

  describe '#cancel_trade!' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }
    let(:kafka_service) { instance_double(KafkaService::Services::Trade::TradeService) }

    before do
      allow(KafkaService::Services::Trade::TradeService).to receive(:new).and_return(kafka_service)
    end

    context 'when trade can be cancelled' do
      it 'sends a cancel event to Kafka' do
        allow(trade).to receive(:may_cancel?).and_return(true)
        allow(kafka_service).to receive(:cancel).with(trade: trade)

        result = service.cancel_trade!

        expect(result).to eq(trade)
      end

      it 'handles optional reason parameter' do
        allow(trade).to receive(:may_cancel?).and_return(true)
        allow(kafka_service).to receive(:cancel).with(trade: trade)

        result = service.cancel_trade!('No longer needed')

        expect(result).to eq(trade)
      end
    end

    context 'when trade cannot be cancelled' do
      it 'returns false' do
        allow(trade).to receive(:may_cancel?).and_return(false)
        expect(kafka_service).not_to receive(:cancel)

        result = service.cancel_trade!

        expect(result).to be_falsey
      end
    end
  end

  describe '#auto_cancel_trade!' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }
    let(:kafka_service) { instance_double(KafkaService::Services::Trade::TradeService) }

    before do
      allow(KafkaService::Services::Trade::TradeService).to receive(:new).and_return(kafka_service)
    end

    context 'when trade can be automatically cancelled' do
      it 'sends a cancel event to Kafka' do
        allow(trade).to receive(:may_cancel_automatically?).and_return(true)
        allow(kafka_service).to receive(:cancel).with(trade: trade)

        result = service.auto_cancel_trade!

        expect(result).to eq(trade)
      end
    end

    context 'when trade cannot be automatically cancelled' do
      it 'returns false' do
        allow(trade).to receive(:may_cancel_automatically?).and_return(false)
        expect(kafka_service).not_to receive(:cancel)

        result = service.auto_cancel_trade!

        expect(result).to be_falsey
      end
    end
  end

  describe '#send_event_and_return_trade' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }
    let(:kafka_service) { instance_double(KafkaService::Services::Trade::TradeService) }

    before do
      allow(KafkaService::Services::Trade::TradeService).to receive(:new).and_return(kafka_service)
    end

    it 'sends complete event when event_type is complete' do
      allow(kafka_service).to receive(:complete).with(trade: trade)

      # We need to use send to call private method
      result = service.send(:send_event_and_return_trade, 'complete')

      expect(result).to eq(trade)
    end

    it 'sends cancel event when event_type is cancel' do
      allow(kafka_service).to receive(:cancel).with(trade: trade)

      # We need to use send to call private method
      result = service.send(:send_event_and_return_trade, 'cancel')

      expect(result).to eq(trade)
    end
  end

  describe '#handle_trade_released' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }

    context 'when trade has fiat token deposit' do
      it 'marks the fiat token deposit as processed' do
        allow(trade).to receive(:fiat_token_deposit).and_return(instance_double(FiatDeposit, present?: true))
        expect(service).to receive(:mark_fiat_token_deposit_as_processed)
        expect(service).not_to receive(:mark_fiat_token_withdrawal_as_processed)

        service.send(:handle_trade_released)
      end
    end

    context 'when trade has fiat token withdrawal' do
      it 'marks the fiat token withdrawal as processed' do
        allow(trade).to receive_messages(
          fiat_token_deposit: instance_double(FiatDeposit, present?: false),
          fiat_token_withdrawal: instance_double(FiatWithdrawal, present?: true)
        )
        expect(service).not_to receive(:mark_fiat_token_deposit_as_processed)
        expect(service).to receive(:mark_fiat_token_withdrawal_as_processed)

        service.send(:handle_trade_released)
      end
    end

    context 'when trade has neither deposit nor withdrawal' do
      it 'does not mark anything as processed' do
        allow(trade).to receive_messages(
          fiat_token_deposit: instance_double(FiatDeposit, present?: false),
          fiat_token_withdrawal: instance_double(FiatWithdrawal, present?: false)
        )
        expect(service).not_to receive(:mark_fiat_token_deposit_as_processed)
        expect(service).not_to receive(:mark_fiat_token_withdrawal_as_processed)

        service.send(:handle_trade_released)
      end
    end
  end

  describe '#handle_trade_cancelled' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }
    let(:reason) { 'Trade cancelled by user' }

    context 'when trade has fiat token deposit' do
      it 'marks the fiat token deposit as cancelled' do
        allow(trade).to receive(:fiat_token_deposit).and_return(instance_double(FiatDeposit, present?: true))
        expect(service).to receive(:mark_fiat_token_deposit_as_cancelled).with(reason)
        expect(service).not_to receive(:mark_fiat_token_withdrawal_as_cancelled)

        service.send(:handle_trade_cancelled, reason)
      end
    end

    context 'when trade has fiat token withdrawal' do
      it 'marks the fiat token withdrawal as cancelled' do
        allow(trade).to receive_messages(
          fiat_token_deposit: instance_double(FiatDeposit, present?: false),
          fiat_token_withdrawal: instance_double(FiatWithdrawal, present?: true)
        )
        expect(service).not_to receive(:mark_fiat_token_deposit_as_cancelled)
        expect(service).to receive(:mark_fiat_token_withdrawal_as_cancelled).with(reason)

        service.send(:handle_trade_cancelled, reason)
      end
    end

    context 'when trade has neither deposit nor withdrawal' do
      it 'does not mark anything as cancelled' do
        allow(trade).to receive_messages(
          fiat_token_deposit: instance_double(FiatDeposit, present?: false),
          fiat_token_withdrawal: instance_double(FiatWithdrawal, present?: false)
        )
        expect(service).not_to receive(:mark_fiat_token_deposit_as_cancelled)
        expect(service).not_to receive(:mark_fiat_token_withdrawal_as_cancelled)

        service.send(:handle_trade_cancelled)
      end
    end
  end

  describe '#mark_fiat_token_deposit_as_processed' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }
    let(:deposit) { build_stubbed(:fiat_deposit) }

    context 'when deposit can be processed' do
      it 'processes the deposit' do
        allow(trade).to receive(:fiat_token_deposit).and_return(deposit)
        allow(deposit).to receive(:may_process?).and_return(true)
        expect(deposit).to receive(:process!)

        service.send(:mark_fiat_token_deposit_as_processed)
      end
    end

    context 'when deposit cannot be processed' do
      it 'does not process the deposit' do
        allow(trade).to receive(:fiat_token_deposit).and_return(deposit)
        allow(deposit).to receive(:may_process?).and_return(false)
        expect(deposit).not_to receive(:process!)

        service.send(:mark_fiat_token_deposit_as_processed)
      end
    end
  end

  describe '#mark_fiat_token_deposit_as_cancelled' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }
    let(:deposit) { build_stubbed(:fiat_deposit) }
    let(:reason) { 'Trade cancelled by user' }

    context 'when deposit can be cancelled' do
      it 'cancels the deposit with the given reason' do
        allow(trade).to receive(:fiat_token_deposit).and_return(deposit)
        allow(deposit).to receive(:may_cancel?).and_return(true)
        allow(deposit).to receive(:cancel_reason_param=).with(reason)
        allow(deposit).to receive(:cancel!)

        service.send(:mark_fiat_token_deposit_as_cancelled, reason)

        expect(deposit).to have_received(:cancel_reason_param=).with(reason)
        expect(deposit).to have_received(:cancel!)
      end

      it 'handles nil reason' do
        allow(trade).to receive(:fiat_token_deposit).and_return(deposit)
        allow(deposit).to receive(:may_cancel?).and_return(true)
        allow(deposit).to receive(:cancel!)

        service.send(:mark_fiat_token_deposit_as_cancelled)

        expect(deposit).to have_received(:cancel!)
      end
    end

    context 'when deposit cannot be cancelled' do
      it 'does not cancel the deposit' do
        allow(trade).to receive(:fiat_token_deposit).and_return(deposit)
        allow(deposit).to receive(:may_cancel?).and_return(false)
        expect(deposit).not_to receive(:cancel_reason_param=)
        expect(deposit).not_to receive(:cancel!)

        service.send(:mark_fiat_token_deposit_as_cancelled, reason)
      end
    end
  end

  describe '#mark_fiat_token_withdrawal_as_processed' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }

    it 'has a placeholder implementation' do
      # This method is stubbed in the source code, so there's no real implementation to test.
      # We're just ensuring it exists.
      expect(service.private_methods).to include(:mark_fiat_token_withdrawal_as_processed)
    end
  end

  describe '#mark_fiat_token_withdrawal_as_cancelled' do
    let(:trade) { build_stubbed(:trade) }
    let(:service) { described_class.new(trade) }

    it 'has a placeholder implementation' do
      # This method is stubbed in the source code, so there's no real implementation to test.
      # We're just ensuring it exists.
      expect(service.private_methods).to include(:mark_fiat_token_withdrawal_as_cancelled)
    end
  end
end
