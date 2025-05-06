# frozen_string_literal: true

module KafkaService
  module Handlers
    class OfferHandler < BaseHandler
      def handle(payload)
        Rails.logger.info("Processing offer: #{payload['operationType']}")
        return if payload.nil?

        case payload['operationType']
        when KafkaService::Config::OperationTypes::OFFER_CREATE
          process_offer_create(payload)
        when KafkaService::Config::OperationTypes::OFFER_UPDATE
          process_offer_update(payload)
        when KafkaService::Config::OperationTypes::OFFER_DISABLE
          process_offer_disable(payload)
        when KafkaService::Config::OperationTypes::OFFER_ENABLE
          process_offer_enable(payload)
        when KafkaService::Config::OperationTypes::OFFER_DELETE
          process_offer_delete(payload)
        end
      end

      private

      def process_offer_create(payload)
        Rails.logger.info("Processing offer create: #{payload['object']['identifier']}")

        begin
          offer_data = payload['object']

          offer = Offer.find_or_initialize_by(id: payload['actionId'])

          update_offer_attributes(offer, offer_data)

          offer.save!
          Rails.logger.info("Offer created successfully: #{offer.id}")
        rescue StandardError => e
          Rails.logger.error("Error processing offer create: #{e.message}")
          Rails.logger.error(e.backtrace.join("\n"))
        end
      end

      def process_offer_update(payload)
        Rails.logger.info("Processing offer update: #{payload['object']['identifier']}")

        begin
          offer = Offer.find_by(id: payload['actionId'])
          return unless offer

          update_offer_attributes(offer, payload['object'])

          offer.save!
          Rails.logger.info("Offer updated successfully: #{offer.id}")
        rescue StandardError => e
          Rails.logger.error("Error processing offer update: #{e.message}")
          Rails.logger.error(e.backtrace.join("\n"))
        end
      end

      def process_offer_disable(payload)
        Rails.logger.info("Processing offer disable: #{payload['object']['identifier']}")

        begin
          offer = Offer.find_by(id: payload['actionId'])
          return unless offer

          unless offer.disabled?
            update_offer_attributes(offer, payload['object'])
            offer.save!
          end
        rescue StandardError => e
          Rails.logger.error("Error processing offer disable: #{e.message}")
          Rails.logger.error(e.backtrace.join("\n"))
        end
      end

      def process_offer_enable(payload)
        Rails.logger.info("Processing offer enable: #{payload['object']['identifier']}")

        begin
          offer = Offer.find_by(id: payload['actionId'])
          return unless offer

          if offer.disabled?
            update_offer_attributes(offer, payload['object'])
            offer.save!
          end
        rescue StandardError => e
          Rails.logger.error("Error processing offer enable: #{e.message}")
          Rails.logger.error(e.backtrace.join("\n"))
        end
      end

      def process_offer_delete(payload)
        Rails.logger.info("Processing offer delete: #{payload['object']['identifier']}")

        begin
          offer = Offer.find_by(id: payload['actionId'])
          return unless offer

          offer.update!(deleted: true)
          Rails.logger.info("Offer marked as deleted: #{offer.id}")
        rescue StandardError => e
          Rails.logger.error("Error processing offer delete: #{e.message}")
          Rails.logger.error(e.backtrace.join("\n"))
        end
      end

      def update_offer_attributes(offer, offer_data)
        # Extract symbol parts (e.g., "vnd:vnd" => coin_currency: "vnd", currency: "vnd")
        symbol_parts = offer_data['symbol'].split(':')
        coin_currency = symbol_parts[0]
        currency = symbol_parts[1]

        offer.assign_attributes(
          user_id: offer_data['userId'],
          coin_currency: coin_currency,
          currency: currency,
          offer_type: offer_data['type'].downcase,
          price: offer_data['price'],
          total_amount: offer_data['totalAmount'],
          disabled: offer_data['disabled'],
          deleted: offer_data['deleted'],
          automatic: offer_data['automatic'],
          online: offer_data['online'],
          margin: offer_data['margin'],
          payment_method_id: offer_data['paymentMethodId'],
          payment_time: offer_data['paymentTime'],
          country_code: offer_data['countryCode'],
          min_amount: offer_data['minAmount'],
          max_amount: offer_data['maxAmount'],
          terms_of_trade: offer_data['statusExplanation']
        )
      end
    end
  end
end
