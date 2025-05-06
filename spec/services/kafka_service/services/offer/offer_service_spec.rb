# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Services::Offer::OfferService, type: :service do
  describe 'service operations' do
    let(:offer_attributes) do
      {
        id: 123,
        user_id: 456,
        offer_type: 'buy',
        coin_currency: 'BTC',
        currency: 'USD',
        price: 45000,
        min_amount: 0.001,
        max_amount: 0.1,
        total_amount: 1.0,
        available_amount: 0.9,
        payment_method_id: 789,
        payment_time: 30,
        country_code: 'US',
        disabled: false,
        deleted: false,
        automatic: true,
        online: true,
        margin: 0.05,
        created_at: Time.zone.now,
        updated_at: Time.zone.now
      }
    end

    describe '#create' do
      it 'sends create offer event with correct data' do
        service = described_class.new
        producer = instance_double(KafkaService::Base::Producer)
        offer = instance_double(Offer, offer_attributes)
        identifier = 'offer-123'

        allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
        allow(producer).to receive(:send_message)
        allow(KafkaService::Services::IdentifierBuilderService).to receive(:build_offer_identifier)
          .with(offer_id: offer.id)
          .and_return(identifier)

        expect(service).to receive(:send_event).with(
          topic: KafkaService::Config::Topics::OFFER,
          key: identifier,
          data: hash_including(
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::OFFER_CREATE,
            actionType: KafkaService::Config::ActionTypes::OFFER,
            actionId: offer.id,
            userId: offer.user_id,
            offerType: offer.offer_type,
            coinCurrency: offer.coin_currency,
            currency: offer.currency,
            price: offer.price
          )
        )

        service.create(offer: offer)
      end
    end

    describe '#update' do
      it 'sends update offer event with correct data' do
        service = described_class.new
        producer = instance_double(KafkaService::Base::Producer)
        offer = instance_double(Offer, offer_attributes)
        identifier = 'offer-123'

        allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
        allow(producer).to receive(:send_message)
        allow(KafkaService::Services::IdentifierBuilderService).to receive(:build_offer_identifier)
          .with(offer_id: offer.id)
          .and_return(identifier)

        expect(service).to receive(:send_event).with(
          topic: KafkaService::Config::Topics::OFFER,
          key: identifier,
          data: hash_including(
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::OFFER_UPDATE,
            actionType: KafkaService::Config::ActionTypes::OFFER,
            actionId: offer.id
          )
        )

        service.update(offer: offer)
      end
    end

    describe '#disable' do
      it 'sends disable offer event with correct data' do
        service = described_class.new
        producer = instance_double(KafkaService::Base::Producer)
        offer = instance_double(Offer, offer_attributes)
        identifier = 'offer-123'

        allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
        allow(producer).to receive(:send_message)
        allow(KafkaService::Services::IdentifierBuilderService).to receive(:build_offer_identifier)
          .with(offer_id: offer.id)
          .and_return(identifier)

        expect(service).to receive(:send_event).with(
          topic: KafkaService::Config::Topics::OFFER,
          key: identifier,
          data: hash_including(
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::OFFER_DISABLE,
            actionType: KafkaService::Config::ActionTypes::OFFER,
            actionId: offer.id
          )
        )

        service.disable(offer: offer)
      end
    end

    describe '#enable' do
      it 'sends enable offer event with correct data' do
        service = described_class.new
        producer = instance_double(KafkaService::Base::Producer)
        offer = instance_double(Offer, offer_attributes)
        identifier = 'offer-123'

        allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
        allow(producer).to receive(:send_message)
        allow(KafkaService::Services::IdentifierBuilderService).to receive(:build_offer_identifier)
          .with(offer_id: offer.id)
          .and_return(identifier)

        expect(service).to receive(:send_event).with(
          topic: KafkaService::Config::Topics::OFFER,
          key: identifier,
          data: hash_including(
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::OFFER_ENABLE,
            actionType: KafkaService::Config::ActionTypes::OFFER,
            actionId: offer.id
          )
        )

        service.enable(offer: offer)
      end
    end

    describe '#delete' do
      it 'sends delete offer event with correct data' do
        service = described_class.new
        producer = instance_double(KafkaService::Base::Producer)
        offer = instance_double(Offer, offer_attributes)
        identifier = 'offer-123'

        allow(KafkaService::Base::Producer).to receive(:new).and_return(producer)
        allow(producer).to receive(:send_message)
        allow(KafkaService::Services::IdentifierBuilderService).to receive(:build_offer_identifier)
          .with(offer_id: offer.id)
          .and_return(identifier)

        expect(service).to receive(:send_event).with(
          topic: KafkaService::Config::Topics::OFFER,
          key: identifier,
          data: hash_including(
            identifier: identifier,
            operationType: KafkaService::Config::OperationTypes::OFFER_DELETE,
            actionType: KafkaService::Config::ActionTypes::OFFER,
            actionId: offer.id
          )
        )

        service.delete(offer: offer)
      end
    end

    describe '#build_offer_data' do
      it 'builds offer data with all required fields' do
        service = described_class.new
        offer = instance_double(Offer, offer_attributes)
        identifier = 'offer-123'

        data = service.send(:build_offer_data,
                          identifier: identifier,
                          operation_type: KafkaService::Config::OperationTypes::OFFER_CREATE,
                          offer: offer)

        expect(data).to include(
          identifier: identifier,
          operationType: KafkaService::Config::OperationTypes::OFFER_CREATE,
          actionType: KafkaService::Config::ActionTypes::OFFER,
          actionId: offer.id,
          userId: offer.user_id,
          offerType: offer.offer_type,
          coinCurrency: offer.coin_currency,
          currency: offer.currency,
          price: offer.price,
          minAmount: offer.min_amount,
          maxAmount: offer.max_amount,
          totalAmount: offer.total_amount,
          availableAmount: offer.available_amount,
          paymentMethodId: offer.payment_method_id,
          paymentTime: offer.payment_time,
          countryCode: offer.country_code,
          disabled: offer.disabled,
          deleted: offer.deleted,
          automatic: offer.automatic,
          online: offer.online,
          margin: offer.margin,
          createdAt: offer.created_at.to_i,
          updatedAt: offer.updated_at.to_i
        )
      end
    end
  end
end
