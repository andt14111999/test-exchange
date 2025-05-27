# frozen_string_literal: true

module V1
  module CoinWithdrawals
    class Entity < Grape::Entity
      format_with(:decimal) { |number| number.to_s }

      expose :id
      expose :coin_currency
      expose :coin_amount, format_with: :decimal
      expose :coin_fee, format_with: :decimal
      expose :status
      expose :created_at
      expose :updated_at

      expose :is_internal_transfer do |withdrawal|
        withdrawal.internal_transfer?
      end

      # Only expose these fields for external withdrawals
      expose :coin_address, if: ->(withdrawal, _) { !withdrawal.internal_transfer? }
      expose :coin_layer, if: ->(withdrawal, _) { !withdrawal.internal_transfer? }
      expose :tx_hash, if: ->(withdrawal, _) { !withdrawal.internal_transfer? }

      # Only expose these fields for internal transfers
      expose :receiver_email, if: ->(withdrawal, _) { withdrawal.internal_transfer? && withdrawal.receiver_email.present? }
      expose :receiver_username, if: ->(withdrawal, _) { withdrawal.internal_transfer? && withdrawal.receiver_username.present? }
      expose :receiver_phone_number, if: ->(withdrawal, _) { withdrawal.internal_transfer? && withdrawal.receiver_phone_number.present? }

      expose :internal_transfer_status, if: ->(withdrawal, _) { withdrawal.internal_transfer? } do |withdrawal|
        withdrawal.coin_internal_transfer_operation&.status
      end
    end
  end
end
