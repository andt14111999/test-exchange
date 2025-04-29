# frozen_string_literal: true

module KafkaService
  module Services
    module Offer
      class OfferService < KafkaService::Base::Service
        def create(offer:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_offer_identifier(
            offer_id: offer.id
          )

          send_event(
            topic: KafkaService::Config::Topics::OFFER,
            key: identifier,
            data: build_offer_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::OFFER_CREATE,
              offer: offer
            )
          )
        end

        def update(offer:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_offer_identifier(
            offer_id: offer.id
          )

          send_event(
            topic: KafkaService::Config::Topics::OFFER,
            key: identifier,
            data: build_offer_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::OFFER_UPDATE,
              offer: offer
            )
          )
        end

        def disable(offer:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_offer_identifier(
            offer_id: offer.id
          )

          send_event(
            topic: KafkaService::Config::Topics::OFFER,
            key: identifier,
            data: build_offer_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::OFFER_DISABLE,
              offer: offer
            )
          )
        end

        def enable(offer:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_offer_identifier(
            offer_id: offer.id
          )

          send_event(
            topic: KafkaService::Config::Topics::OFFER,
            key: identifier,
            data: build_offer_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::OFFER_ENABLE,
              offer: offer
            )
          )
        end

        def delete(offer:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_offer_identifier(
            offer_id: offer.id
          )

          send_event(
            topic: KafkaService::Config::Topics::OFFER,
            key: identifier,
            data: build_offer_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::OFFER_DELETE,
              offer: offer
            )
          )
        end

        private

        def build_offer_data(identifier:, operation_type:, offer:)
          {
            identifier: identifier,
            operationType: operation_type,
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
          }
        end
      end
    end
  end
end
