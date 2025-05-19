# frozen_string_literal: true

module KafkaService
  module Handlers
    class MerchantEscrowHandler < BaseHandler
      def handle(payload)
        return if payload.nil?

        case payload['operationType']
        when KafkaService::Config::OperationTypes::MERCHANT_ESCROW_MINT
          process_escrow_mint(payload)
        when KafkaService::Config::OperationTypes::MERCHANT_ESCROW_BURN
          process_escrow_burn(payload)
        end
      end

      private

      def process_escrow_mint(payload)
        Rails.logger.info("Processing merchant escrow mint: #{payload['object']['identifier']}")

        escrow = MerchantEscrow.find_by(id: payload['actionId'])
        return unless escrow

        # Extract account IDs from keys
        usdt_account_id = extract_account_id_from_key(payload['object']['usdtAccountKey'])
        fiat_account_id = extract_account_id_from_key(payload['object']['fiatAccountKey'])

        # Create the operation
        create_merchant_escrow_operation(
          merchant_escrow: escrow,
          usdt_account_id: usdt_account_id,
          fiat_account_id: fiat_account_id,
          operation_type: 'mint',
          usdt_amount: payload['object']['usdtAmount'],
          fiat_amount: payload['object']['fiatAmount'],
          fiat_currency: payload['object']['fiatCurrency'],
        )

        escrow.activate! if escrow.pending?
      rescue StandardError => e
        Rails.logger.error("Error processing merchant escrow mint: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      def process_escrow_burn(payload)
        Rails.logger.info("Processing merchant escrow burn: #{payload['object']['identifier']}")

        escrow = MerchantEscrow.find_by(id: payload['actionId'])
        return unless escrow

        usdt_account_id = extract_account_id_from_key(payload['object']['usdtAccountKey'])
        fiat_account_id = extract_account_id_from_key(payload['object']['fiatAccountKey'])

        create_merchant_escrow_operation(
          merchant_escrow: escrow,
          usdt_account_id: usdt_account_id,
          fiat_account_id: fiat_account_id,
          operation_type: 'burn',
          usdt_amount: payload['object']['usdtAmount'],
          fiat_amount: payload['object']['fiatAmount'],
          fiat_currency: payload['object']['fiatCurrency'],
        )

        escrow.cancel! unless escrow.cancelled?
      rescue StandardError => e
        Rails.logger.error("Error processing merchant escrow burn: #{e.message}")
        Rails.logger.error(e.backtrace.join("\n"))
      end

      def create_merchant_escrow_operation(merchant_escrow:, usdt_account_id:, fiat_account_id:, operation_type:, usdt_amount:, fiat_amount:, fiat_currency:)
        MerchantEscrowOperation.create!(
          merchant_escrow: merchant_escrow,
          usdt_account_id: usdt_account_id,
          fiat_account_id: fiat_account_id,
          operation_type: operation_type,
          usdt_amount: BigDecimal.safe_convert(usdt_amount),
          fiat_amount: BigDecimal.safe_convert(fiat_amount),
          fiat_currency: fiat_currency,
          status: 'completed',
        )
      end

      def extract_account_id_from_key(key)
        return nil unless key

        begin
          # Format: "{user_id}-{type}-{account_id}"
          parts = key.split('-')
          return nil unless parts.size >= 3

          parts.last.to_i
        rescue StandardError => e
          Rails.logger.error("Error extracting account ID from key '#{key}': #{e.message}")
          nil
        end
      end
    end
  end
end
