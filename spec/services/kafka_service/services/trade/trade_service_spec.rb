# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Services::Trade::TradeService, type: :service do
  let(:producer) { instance_double(KafkaService::Base::Producer) }
  let(:service) { described_class.new }
  let(:trade) { instance_double(Trade, id: 123) }
  let(:identifier) { 'trade-123' }

  before do
    allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
    allow(producer).to receive(:send_message)

    # Stub identifier builder
    allow(KafkaService::Services::IdentifierBuilderService).to receive(:build_trade_identifier)
      .with(trade_id: trade.id)
      .and_return(identifier)

    # Setup trade attributes for build_trade_data method
    buyer = instance_double(User, id: 456)
    seller = instance_double(User, id: 789)
    buyer_fiat_account = instance_double(FiatAccount, id: 111)
    seller_fiat_account = instance_double(FiatAccount, id: 222)
    buyer_fiat_accounts = instance_double(ActiveRecord::Relation)
    seller_fiat_accounts = instance_double(ActiveRecord::Relation)

    allow(trade).to receive_messages(
      buyer: buyer,
      seller: seller,
      fiat_currency: 'usd',
      coin_currency: 'btc',
      coin_amount: 0.1,
      fiat_amount: 1000,
      price: 10000,
      fee_ratio: 0.01,
      coin_trading_fee: 0.001,
      fixed_fee: 0.0,
      total_fee: 0.001,
      amount_after_fee: 0.099,
      payment_method: 'bank_transfer',
      taker_side: 'buy',
      status: 'paid',
      payment_proof_status: 'verified',
      has_payment_proof: true,
      offer_id: 333,
      ref: 'TR123456',
      paid_at: Time.current,
      released_at: nil,
      cancelled_at: nil,
      disputed_at: nil,
      created_at: Time.current
    )

    allow(buyer).to receive(:fiat_accounts).and_return(buyer_fiat_accounts)
    allow(seller).to receive(:fiat_accounts).and_return(seller_fiat_accounts)
    allow(buyer_fiat_accounts).to receive(:find_by).with(currency: 'USD').and_return(buyer_fiat_account)
    allow(seller_fiat_accounts).to receive(:find_by).with(currency: 'USD').and_return(seller_fiat_account)

    # Stub account key builder
    allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_fiat_account_key)
      .with(user_id: buyer.id, account_id: buyer_fiat_account.id)
      .and_return("#{buyer.id}-fiat-#{buyer_fiat_account.id}")
    allow(KafkaService::Services::AccountKeyBuilderService).to receive(:build_fiat_account_key)
      .with(user_id: seller.id, account_id: seller_fiat_account.id)
      .and_return("#{seller.id}-fiat-#{seller_fiat_account.id}")

    # Stub offer identifier builder
    allow(KafkaService::Services::IdentifierBuilderService).to receive(:build_offer_identifier)
      .with(offer_id: trade.offer_id)
      .and_return("offer-#{trade.offer_id}")
  end

  describe '#create' do
    it 'sends create trade event with correct data' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::TRADE,
        key: identifier,
        data: hash_including(
          identifier: identifier,
          operationType: KafkaService::Config::OperationTypes::TRADE_CREATE,
          actionType: KafkaService::Config::ActionTypes::TRADE,
          actionId: trade.id,
          ref: trade.ref
        )
      )

      service.create(trade: trade)
    end
  end

  describe '#update' do
    it 'sends update trade event with correct data' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::TRADE,
        key: identifier,
        data: hash_including(
          identifier: identifier,
          operationType: KafkaService::Config::OperationTypes::TRADE_UPDATE,
          actionType: KafkaService::Config::ActionTypes::TRADE,
          actionId: trade.id,
          ref: trade.ref
        )
      )

      service.update(trade: trade)
    end
  end

  describe '#complete' do
    it 'sends complete trade event with correct data' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::TRADE,
        key: identifier,
        data: hash_including(
          identifier: identifier,
          operationType: KafkaService::Config::OperationTypes::TRADE_COMPLETE,
          actionType: KafkaService::Config::ActionTypes::TRADE,
          actionId: trade.id,
          ref: trade.ref
        )
      )

      service.complete(trade: trade)
    end
  end

  describe '#cancel' do
    it 'sends cancel trade event with correct data' do
      expect(service).to receive(:send_event).with(
        topic: KafkaService::Config::Topics::TRADE,
        key: identifier,
        data: hash_including(
          identifier: identifier,
          operationType: KafkaService::Config::OperationTypes::TRADE_CANCEL,
          actionType: KafkaService::Config::ActionTypes::TRADE,
          actionId: trade.id,
          ref: trade.ref
        )
      )

      service.cancel(trade: trade)
    end
  end

  describe '#build_trade_data' do
    it 'builds trade data with all required fields' do
      data = service.send(:build_trade_data, identifier: identifier,
                        operation_type: KafkaService::Config::OperationTypes::TRADE_CREATE,
                        trade: trade)

      expect(data).to include(
        identifier: identifier,
        operationType: KafkaService::Config::OperationTypes::TRADE_CREATE,
        actionType: KafkaService::Config::ActionTypes::TRADE,
        actionId: trade.id,
        ref: trade.ref,
        buyerAccountKey: "456-fiat-111",
        sellerAccountKey: "789-fiat-222",
        offerKey: "offer-333",
        coinCurrency: trade.coin_currency,
        fiatCurrency: trade.fiat_currency,
        coinAmount: trade.coin_amount,
        fiatAmount: trade.fiat_amount,
        price: trade.price,
        feeRatio: trade.fee_ratio,
        coinTradingFee: trade.coin_trading_fee,
        totalFee: trade.total_fee,
        fixedFee: trade.fixed_fee,
        amountAfterFee: trade.amount_after_fee,
        paymentMethod: trade.payment_method,
        takerSide: trade.taker_side,
        status: trade.status,
        paymentProofStatus: trade.payment_proof_status,
        hasPaymentProof: trade.has_payment_proof,
        paidAt: trade.paid_at.to_i,
        releasedAt: nil,
        cancelledAt: nil,
        disputedAt: nil,
        createdAt: trade.created_at.to_i
      )
    end
  end
end
