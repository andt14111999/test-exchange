# frozen_string_literal: true

module KafkaService
  module Services
    module Trade
      class TradeService < KafkaService::Base::Service
        def create(trade:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_trade_identifier(
            trade_id: trade.id
          )

          send_event(
            topic: KafkaService::Config::Topics::TRADE,
            key: identifier,
            data: build_trade_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::TRADE_CREATE,
              trade: trade
            )
          )
        end

        def update(trade:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_trade_identifier(
            trade_id: trade.id
          )

          send_event(
            topic: KafkaService::Config::Topics::TRADE,
            key: identifier,
            data: build_trade_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::TRADE_UPDATE,
              trade: trade
            )
          )
        end

        def complete(trade:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_trade_identifier(
            trade_id: trade.id
          )

          send_event(
            topic: KafkaService::Config::Topics::TRADE,
            key: identifier,
            data: build_trade_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::TRADE_COMPLETE,
              trade: trade
            )
          )
        end

        def cancel(trade:)
          identifier = KafkaService::Services::IdentifierBuilderService.build_trade_identifier(
            trade_id: trade.id
          )

          send_event(
            topic: KafkaService::Config::Topics::TRADE,
            key: identifier,
            data: build_trade_data(
              identifier: identifier,
              operation_type: KafkaService::Config::OperationTypes::TRADE_CANCEL,
              trade: trade
            )
          )
        end

        private

        def build_trade_data(identifier:, operation_type:, trade:)
          buyerAccountKey = AccountKeyBuilderService.build_fiat_account_key(
            user_id: trade.buyer.id,
            account_id: trade.buyer.fiat_accounts.find_by(currency: trade.fiat_currency.upcase).id
          )
          sellerAccountKey = AccountKeyBuilderService.build_fiat_account_key(
            user_id: trade.seller.id,
            account_id: trade.seller.fiat_accounts.find_by(currency: trade.fiat_currency.upcase).id
          )
          offerKey = IdentifierBuilderService.build_offer_identifier(
            offer_id: trade.offer_id
          )

          {
            identifier: identifier,
            operationType: operation_type,
            actionType: KafkaService::Config::ActionTypes::TRADE,
            actionId: trade.id,
            ref: trade.ref,
            buyerAccountKey: buyerAccountKey,
            sellerAccountKey: sellerAccountKey,
            offerKey: offerKey,
            coinCurrency: trade.coin_currency,
            fiatCurrency: trade.fiat_currency,
            coinAmount: trade.coin_amount,
            fiatAmount: trade.fiat_amount,
            price: trade.price,
            feeRatio: trade.fee_ratio,
            coinTradingFee: trade.coin_trading_fee,
            paymentMethod: trade.payment_method,
            takerSide: trade.taker_side,
            status: trade.status,
            paymentProofStatus: trade.payment_proof_status,
            hasPaymentProof: trade.has_payment_proof,
            paidAt: trade.paid_at&.to_i,
            releasedAt: trade.released_at&.to_i,
            cancelledAt: trade.cancelled_at&.to_i,
            disputedAt: trade.disputed_at&.to_i,
            createdAt: trade.created_at.to_i
          }
        end
      end
    end
  end
end
