# frozen_string_literal: true

module KafkaService
  module Handlers
    class TradeHandler < BaseHandler
      def handle(payload)
        Rails.logger.info("Processing trade: #{payload}")

        return if payload.nil?

        case payload['operationType']
        when KafkaService::Config::OperationTypes::TRADE_CREATE
          process_trade_create(payload)
        when KafkaService::Config::OperationTypes::TRADE_UPDATE
          process_trade_update(payload)
        when KafkaService::Config::OperationTypes::TRADE_CANCEL
          process_trade_cancel(payload)
        when KafkaService::Config::OperationTypes::TRADE_COMPLETE
          process_trade_complete(payload)
        end
      end

      private

      def process_trade_create(payload)
        Rails.logger.info("Processing trade create: #{payload['object']['identifier']}")

        ActiveRecord::Base.transaction do
          begin
            trade_data = payload['object']

            # Extract offer ID and find offer
            offer_id = extract_id_from_key(trade_data['offerKey'])
            offer = Offer.find_by(id: offer_id)
            return unless offer

            # Validate offer is active and has sufficient amount
            coin_amount = trade_data['coinAmount']
            unless offer.active? && offer.has_available_amount?(coin_amount)
              Rails.logger.error("Offer #{offer_id} inactive or insufficient amount")
              return
            end

            # Extract user IDs and other data
            buyer_id = extract_user_id_from_key(trade_data['buyerAccountKey'])
            seller_id = extract_user_id_from_key(trade_data['sellerAccountKey'])

            # Create trade with all required data
            trade = create_trade_from_data(payload['actionId'], trade_data, offer, buyer_id, seller_id)

            # Create fiat deposit or withdrawal based on the trade type
            if offer.buy? && trade_data['takerSide'] == 'sell'
              result, error_message = trade.create_fiat_withdrawal!
              unless result
                Rails.logger.error("Failed to create fiat withdrawal: #{error_message}")
                raise ActiveRecord::Rollback
              end
            elsif offer.sell? && trade_data['takerSide'] == 'buy'
              result, error_message = trade.create_fiat_deposit!
              unless result
                Rails.logger.error("Failed to create fiat deposit: #{error_message}")
                raise ActiveRecord::Rollback
              end
            end

            # Save trade changes after creating fiat deposit/withdrawal
            trade.save!

            Rails.logger.info("Trade created successfully: #{trade.id}")
          rescue StandardError => e
            Rails.logger.error("Error processing trade create: #{e.message}")
            Rails.logger.error(e.backtrace.join("\n"))
            raise ActiveRecord::Rollback
          end
        end
      end

      def process_trade_update(payload)
        Rails.logger.info("Processing trade update: #{payload['object']['identifier']}")

        begin
          trade = Trade.find_by(id: payload['actionId'])
          return unless trade

          trade_data = payload['object']
          status = trade_data['status'].downcase

          # Update status and related fields
          trade.status = status

          # Update timestamps based on status
          case status
          when 'paid'
            trade.paid_at = Time.zone.now if trade.paid_at.nil?
          when 'completed'
            trade.released_at = Time.zone.now if trade.released_at.nil?
          end

          trade.save!
          Rails.logger.info("Trade updated successfully: #{trade.id}")
        rescue StandardError => e
          Rails.logger.error("Error processing trade update: #{e.message}")
          Rails.logger.error(e.backtrace.join("\n"))
        end
      end

      def process_trade_cancel(payload)
        Rails.logger.info("Processing trade cancel: #{payload['object']['identifier']}")

        ActiveRecord::Base.transaction do
          begin
            trade = Trade.find_by(id: payload['actionId'])
            return unless trade

            # Only restore amount if not already cancelled
            if trade.status != 'cancelled'
              # No need to modify available_amount directly as it's calculated
              # Just update the trade status to cancelled
            end

            trade.update!(
              status: 'cancelled',
              cancelled_at: Time.zone.now
            )

            Rails.logger.info("Trade cancelled successfully: #{trade.id}")
          rescue StandardError => e
            Rails.logger.error("Error processing trade cancel: #{e.message}")
            Rails.logger.error(e.backtrace.join("\n"))
            raise ActiveRecord::Rollback
          end
        end
      end

      def process_trade_complete(payload)
        Rails.logger.info("Processing trade complete: #{payload['object']['identifier']}")

        begin
          trade = Trade.find_by(id: payload['actionId'])
          return unless trade

          # Change to use 'released' status which is a valid status in the STATUSES array
          # instead of 'completed', and ensure lowercase
          trade.update!(
            status: 'released',
            released_at: Time.zone.now
          )

          Rails.logger.info("Trade completed successfully: #{trade.id}")
        rescue StandardError => e
          Rails.logger.error("Error processing trade complete: #{e.message}")
          Rails.logger.error(e.backtrace.join("\n"))
        end
      end

      def extract_id_from_key(key)
        return nil unless key

        begin
          # Format: "{type}-{id}"
          parts = key.split('-')
          return nil unless parts.size >= 2

          parts.last.to_i
        rescue StandardError => e
          Rails.logger.error("Error extracting ID from key '#{key}': #{e.message}")
          nil
        end
      end

      def extract_user_id_from_key(key)
        return nil unless key

        begin
          # Format: "{user_id}-{type}-{account_id}"
          parts = key.split('-')
          return nil unless parts.size >= 1

          parts.first.to_i
        rescue StandardError => e
          Rails.logger.error("Error extracting user ID from key '#{key}': #{e.message}")
          nil
        end
      end

      def generate_trade_ref(id)
        "TRADE#{id.to_s.rjust(8, '0')}"
      end

      def calculate_fiat_amount(coin_amount, price)
        coin_amount * price
      end

      def calculate_fee_ratio(buyer_id, seller_id)
        # Default fee ratio, can be customized based on business logic
        0.01
      end

      def create_trade_from_data(id, trade_data, offer, buyer_id, seller_id)
        # Extract symbol parts
        symbol_parts = trade_data['symbol'].split(':')
        coin_currency = symbol_parts[0].downcase
        fiat_currency = symbol_parts[1].downcase

        # Calculate fees
        fee_ratio = calculate_fee_ratio(buyer_id, seller_id)
        fixed_fee = Setting.get_fixed_trading_fee(coin_currency) || 0
        coin_amount = BigDecimal.safe_convert(trade_data['coinAmount'])
        price = BigDecimal.safe_convert(trade_data['price'])
        fiat_amount = calculate_fiat_amount(coin_amount, price)
        coin_trading_fee = calculate_trading_fee(coin_amount, fee_ratio)
        total_fee = fixed_fee + coin_trading_fee
        amount_after_fee = [ coin_amount - total_fee, 0 ].max

        # Create or find trade
        trade = Trade.find_or_initialize_by(id: id)
        trade.assign_attributes(
          ref: generate_trade_ref(id),
          buyer_id: buyer_id,
          seller_id: seller_id,
          offer_id: offer.id,
          coin_currency: coin_currency,
          fiat_currency: fiat_currency,
          coin_amount: coin_amount,
          fiat_amount: fiat_amount,
          price: price,
          status: trade_data['status'].downcase,
          taker_side: trade_data['takerSide'],
          payment_method: offer.payment_method&.name || 'bank_transfer',
          fee_ratio: fee_ratio,
          fixed_fee: fixed_fee,
          coin_trading_fee: coin_trading_fee,
          total_fee: total_fee,
          amount_after_fee: amount_after_fee
        )

        # Set timestamps
        trade.created_at = Time.zone.parse(trade_data['createdAt']) if trade_data['createdAt'].present?
        trade.paid_at = Time.zone.parse(trade_data['paidAt']) if trade_data['paidAt'].present?
        trade.released_at = Time.zone.parse(trade_data['completedAt']) if trade_data['completedAt'].present?
        trade.cancelled_at = Time.zone.parse(trade_data['cancelledAt']) if trade_data['cancelledAt'].present?

        trade.save!
        trade
      end

      def calculate_trading_fee(coin_amount, fee_ratio)
        (coin_amount * BigDecimal.safe_convert(fee_ratio)).round(8)
      end
    end
  end
end
