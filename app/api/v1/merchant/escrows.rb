# frozen_string_literal: true

module V1
  module Merchant
    class Escrows < Grape::API
      before { authenticate_user! }
      before { authorize_merchant! }

      resource :merchant_escrows do
        desc 'Create a new merchant escrow'
        params do
          requires :usdt_amount, type: BigDecimal, desc: 'Amount in USDT'
          requires :fiat_amount, type: BigDecimal, desc: 'Amount in fiat'
          requires :fiat_currency, type: String, values: ::FiatAccount::SUPPORTED_CURRENCIES.keys,
            desc: 'Fiat currency (VNDS/PHPS)'
        end
        post do
          escrow = MerchantEscrowService.create_escrow(
            user: current_user,
            usdt_amount: params[:usdt_amount],
            fiat_amount: params[:fiat_amount],
            fiat_currency: params[:fiat_currency]
          )
          present escrow, with: V1::Entities::MerchantEscrow
        end

        desc 'Get merchant escrows'
        params do
          optional :fiat_currency, type: String, values: ::FiatAccount::SUPPORTED_CURRENCIES.keys
        end
        get do
          escrows = current_user.merchant_escrows.sorted
          escrows = escrows.of_currency(params[:fiat_currency]) if params[:fiat_currency].present?
          present escrows, with: V1::Entities::MerchantEscrow
        end

        desc 'Cancel merchant escrow'
        params do
          requires :id, type: Integer, desc: 'Escrow ID'
        end
        post ':id/cancel' do
          escrow = current_user.merchant_escrows.find(params[:id])
          error!('Cannot cancel escrow', 422) unless escrow.may_cancel?

          escrow.cancel!
          present escrow, with: V1::Entities::MerchantEscrow
        end
      end

      helpers do
        def authorize_merchant!
          error!('Unauthorized', 401) unless current_user.merchant?
        end
      end
    end
  end
end
