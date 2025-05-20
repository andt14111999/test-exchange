# frozen_string_literal: true

module V1
  module FiatDeposits
    class Entity < Grape::Entity
      expose :id
      expose :user_id
      expose :fiat_account_id
      expose :currency
      expose :country_code
      expose :fiat_amount
      expose :deposit_fee
      expose :amount_after_fee
      expose :memo
      expose :status
      expose :created_at
      expose :updated_at
      expose :trade_id, if: ->(deposit, _) { deposit.payable.present? } do |deposit|
        deposit.payable.id
      end

      # Add bank details if associated
      expose :bank_name, if: lambda { |deposit, _| deposit.respond_to?(:bank_name) && deposit.bank_name.present? }
      expose :bank_account_number, if: lambda { |deposit, _| deposit.respond_to?(:bank_account_number) && deposit.bank_account_number.present? }
      expose :bank_recipient_name, if: lambda { |deposit, _| deposit.respond_to?(:bank_recipient_name) && deposit.bank_recipient_name.present? }
    end

    class FiatDepositDetail < Entity
      expose :payable_id
      expose :payable_type
      expose :requires_ownership_verification do |deposit, _|
        deposit.status == 'ownership_verifying' || deposit.status == 'locked_due_to_unverified_ownership'
      end
      expose :cancel_reason, if: lambda { |deposit, _| deposit.status == 'cancelled' && deposit.cancel_reason.present? }

      # Trade specific fields
      expose :trade_details, if: lambda { |deposit, _| deposit.payable_type == 'Trade' && deposit.payable.present? } do |deposit, _|
        {
          trade_id: deposit.payable.id,
          trade_ref: deposit.payable.ref,
          trade_status: deposit.payable.status,
          offer_id: deposit.payable.offer_id,
          buyer_id: deposit.payable.buyer_id,
          seller_id: deposit.payable.seller_id
        }
      end

      # P2P specific fields
      expose :sender_name, if: lambda { |deposit, _| deposit.respond_to?(:sender_name) && deposit.sender_name.present? }
      expose :sender_account_number, if: lambda { |deposit, _| deposit.respond_to?(:sender_account_number) && deposit.sender_account_number.present? }
      expose :ownership_proof_url, if: lambda { |deposit, _| deposit.respond_to?(:ownership_proof_url) && deposit.ownership_proof_url.present? }

      # Payment proof fields
      expose :payment_proof_url, if: lambda { |deposit, _| deposit.respond_to?(:payment_proof_url) && deposit.payment_proof_url.present? }
      expose :payment_description, if: lambda { |deposit, _| deposit.respond_to?(:payment_description) && deposit.payment_description.present? }
    end
  end
end
