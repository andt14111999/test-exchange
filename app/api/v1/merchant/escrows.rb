# frozen_string_literal: true

module V1
  module Merchant
    class Escrows < Grape::API
      include V1::Merchant::Defaults

      resource :merchant_escrows do
        desc 'Create a new escrow'
        params do
          requires :usdt_amount, type: BigDecimal, desc: 'Amount of USDT to lock'
          requires :fiat_currency, type: String, desc: 'Fiat currency to receive'
        end
        post do
          authorize_merchant!

          service = MerchantEscrowService.new(current_user, declared(params))
          escrow = service.create

          if escrow.save
            present escrow, with: V1::Merchant::EscrowEntity
          else
            error!(escrow.errors.full_messages.join(', '), 422)
          end
        end

        desc 'List all escrows'
        get do
          authorize_merchant!

          escrows = MerchantEscrowService.new(current_user).list
          present escrows, with: V1::Merchant::EscrowEntity
        end

        desc 'Get a specific escrow'
        params do
          requires :id, type: Integer, desc: 'Escrow ID'
        end
        get ':id' do
          authorize_merchant!

          escrow = MerchantEscrowService.new(current_user).find(params[:id])
          if escrow
            present escrow, with: V1::Merchant::EscrowEntity
          else
            error!('Escrow not found', 404)
          end
        end

        desc 'Cancel an escrow'
        params do
          requires :id, type: Integer, desc: 'Escrow ID'
        end
        post ':id/cancel' do
          authorize_merchant!

          service = MerchantEscrowService.new(current_user)
          escrow = service.find(params[:id])

          if escrow.nil?
            error!('Escrow not found', 404)
          elsif !escrow.can_cancel?
            error!('Cannot cancel this escrow', 422)
          else
            begin
              cancelled_escrow = service.cancel(escrow)
              present cancelled_escrow, with: V1::Merchant::EscrowEntity
            rescue StandardError => e
              error!(e.message, 422)
            end
          end
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
