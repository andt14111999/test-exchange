# frozen_string_literal: true

module V1
  module FiatWithdrawals
    class Entity < Grape::Entity
      expose :id
      expose :currency
      expose :country_code
      expose :fiat_amount
      expose :fee
      expose :amount_after_transfer_fee
      expose :bank_name
      expose :bank_account_name
      expose :bank_account_number
      expose :bank_branch
      expose :status
      expose :processed_at
      expose :cancelled_at
      expose :created_at
      expose :trade_id, if: ->(withdrawal, _) { withdrawal.withdrawable.present? } do |withdrawal|
        withdrawal.withdrawable.id
      end
    end

    class FiatWithdrawalDetail < Entity
      expose :user_id
      expose :fiat_account_id
      expose :retry_count
      expose :error_message
      expose :cancel_reason
      expose :bank_reference
      expose :bank_transaction_date
      expose :bank_response_data
      expose :verification_status
      expose :verification_attempts
      expose :withdrawable_type
      expose :withdrawable_id
      expose :can_be_cancelled do |withdrawal|
        withdrawal.can_be_cancelled?
      end
      expose :can_be_retried do |withdrawal|
        withdrawal.can_be_retried?
      end
      expose :for_trade do |withdrawal|
        withdrawal.for_trade?
      end
      expose :updated_at
    end
  end
end
