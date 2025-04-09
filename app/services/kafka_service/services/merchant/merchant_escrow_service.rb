# frozen_string_literal: true

module KafkaService
  module Services
    module Merchant
      class MerchantEscrowService < KafkaService::Base::Service
        def create(merchant_escrow:, usdt_account_key:, fiat_account_key:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_merchant_escrow_identifier(escrow_id: merchant_escrow.id)

          send_event(
            topic: KafkaService::Config::Topics::MERCHANT_ESCROW,
            key: identifier,
            data: build_escrow_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::MERCHANT_ESCROW_MINT,
              merchant_escrow: merchant_escrow,
              usdt_account_key: usdt_account_key,
              fiat_account_key: fiat_account_key
            )
          )
        end

        def cancel(merchant_escrow:, usdt_account_key:, fiat_account_key:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_merchant_escrow_identifier(escrow_id: merchant_escrow.id)

          send_event(
            topic: KafkaService::Config::Topics::MERCHANT_ESCROW,
            key: identifier,
            data: build_escrow_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::MERCHANT_ESCROW_BURN,
              merchant_escrow: merchant_escrow,
              usdt_account_key: usdt_account_key,
              fiat_account_key: fiat_account_key
            )
          )
        end

        private

        def build_escrow_data(identifier:, operation_type:, merchant_escrow:, usdt_account_key:, fiat_account_key:)
          {
            identifier: identifier,
            operationType: operation_type,
            actionType: KafkaService::Config::ActionTypes::MERCHANT_ESCROW,
            actionId: merchant_escrow.id,
            userId: merchant_escrow.user_id,
            status: merchant_escrow.status,
            usdtAccountKey: usdt_account_key,
            fiatAccountKey: fiat_account_key,
            usdtAmount: merchant_escrow.usdt_amount,
            fiatAmount: merchant_escrow.fiat_amount,
            fiatCurrency: merchant_escrow.fiat_currency,
            exchangeRate: merchant_escrow.exchange_rate
          }
        end
      end
    end
  end
end
