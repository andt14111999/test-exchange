# frozen_string_literal: true

ActiveAdmin.register Trade do
  menu priority: 3

  permit_params :status, :dispute_resolution, :payment_proof_status

  filter :ref
  filter :buyer
  filter :seller
  filter :coin_currency
  filter :fiat_currency
  filter :status, as: :select, collection: Trade::STATUSES
  filter :created_at
  filter :released_at
  filter :disputed_at

  scope :all
  scope :in_progress
  scope :in_dispute
  scope :needs_admin_intervention
  scope :completed
  scope :for_fiat_token
  scope :normal_trades

  index do
    selectable_column
    id_column
    column :ref
    column :buyer
    column :seller
    column :coin_currency
    column :coin_amount
    column :fiat_currency
    column :fiat_amount
    column :price
    column :status
    column :error_message
    column :created_at
    actions

    column 'Receipt' do |trade|
      if trade.payment_receipt_file.attached?
        if trade.payment_receipt_file.image?
          image_tag(
            rails_representation_path(
              trade.payment_receipt_file.variant(resize_to_limit: [ 50, 50 ]),
              only_path: true
            ),
            alt: 'Receipt',
            title: 'Payment Receipt Available',
            style: 'max-width: 50px; max-height: 50px; border: 1px solid #ddd;'
          )
        else
          span '📄', title: 'File Receipt Available', style: 'font-size: 20px;'
        end
      elsif trade.has_payment_proof
        span '✅', title: 'Payment Details Available', style: 'font-size: 16px; color: green;'
      else
        span '❌', title: 'No Payment Proof', style: 'font-size: 16px; color: #ccc;'
      end
    end

    column 'Admin Actions' do |trade|
      if trade.disputed?
        span { link_to 'Cancel Trade', cancel_trade_admin_trade_path(trade), method: :post, class: 'button', data: { turbo_confirm: 'Are you sure you want to cancel this trade?' } }
        span { link_to 'Release Trade', release_trade_admin_trade_path(trade), method: :post, class: 'button', data: { turbo_confirm: 'Are you sure you want to release this trade?' } }
      end
      span { link_to 'Add Admin Message', new_admin_message_path(trade_id: trade.id), class: 'button' }
    end
  end

  show do
    attributes_table do
      row :id
      row :ref
      row :buyer
      row :seller
      row :offer
      row :coin_currency
      row :coin_amount
      row :fiat_currency
      row :fiat_amount
      row :price
      row :fee_ratio
      row :coin_trading_fee
      row :status
      row :payment_method
      row :paid_at
      row :released_at
      row :expired_at
      row :cancelled_at
      row :disputed_at
      row :dispute_reason
      row :dispute_resolution
      row :has_payment_proof
      row :payment_proof_status
      row :error_message
      row :created_at
      row :updated_at
    end

    # Payment Receipt Panel
    panel 'Payment Receipt' do
      if resource.has_payment_proof
        div do
          if resource.payment_receipt_details.present?
            h4 'Receipt Details:'
            ul do
              resource.payment_receipt_details.each do |key, value|
                li "#{key.humanize}: #{value}" unless key == 'file_url'
              end
            end
          end

          if resource.payment_receipt_file.attached?
            h4 'Receipt File:'
            div do
              if resource.payment_receipt_file.image?
                # Display image with thumbnail
                image_tag(
                  rails_blob_path(resource.payment_receipt_file, only_path: true),
                  alt: 'Payment Receipt',
                  style: 'max-width: 400px; max-height: 300px; border: 1px solid #ddd; margin: 10px 0;'
                )
              else
                # Display file info for non-images (like PDFs)
                div do
                  strong "File: #{resource.payment_receipt_file.filename}"
                  br
                  small "Size: #{number_to_human_size(resource.payment_receipt_file.byte_size)}"
                  br
                  small "Type: #{resource.payment_receipt_file.content_type}"
                end
              end

              div style: 'margin-top: 10px;' do
                link_to(
                  'Download Receipt',
                  rails_blob_path(resource.payment_receipt_file, disposition: 'attachment'),
                  class: 'button',
                  target: '_blank'
                )
                span ' | '
                link_to(
                  'View Full Size',
                  rails_blob_path(resource.payment_receipt_file),
                  class: 'button',
                  target: '_blank'
                )
              end
            end
          end
        end
      else
        div { 'No payment receipt uploaded' }
      end
    end

    panel 'Fiat Token Details' do
      if resource.fiat_token_deposit.present?
        attributes_table_for resource.fiat_token_deposit do
          row :id
          row :memo
          row :currency
          row :fiat_amount
          row :deposit_fee
          row :status
          row :processed_at
          row :cancelled_at
          row :explorer_ref
          row :fiat_deposit_details
        end
      elsif resource.fiat_token_withdrawal.present?
        attributes_table_for resource.fiat_token_withdrawal do
          row :id
          row :reference
          row :currency
          row :fiat_amount
          row :withdrawal_fee
          row :status
          row :processed_at
          row :cancelled_at
          row :bank_account
        end
      else
        div { 'No fiat token operation associated' }
      end
    end

    panel 'Messages' do
      table_for resource.messages.order(created_at: :asc) do
        column :user
        column :is_system
        column :body
        column :created_at
      end
    end

    panel 'Admin Actions' do
      div do
        if resource.disputed?
          span { link_to 'Cancel Trade', cancel_trade_admin_trade_path(resource), method: :post, class: 'button', data: { turbo_confirm: 'Are you sure you want to cancel this trade?' } }
          span { link_to 'Release Trade', release_trade_admin_trade_path(resource), method: :post, class: 'button', data: { turbo_confirm: 'Are you sure you want to release this trade?' } }
        end
        span { link_to 'Add Admin Message', new_admin_message_path(trade_id: resource.id), class: 'button' }
      end
    end
  end

  form do |f|
    f.inputs 'Trade Details' do
      f.input :status, as: :select, collection: Trade::STATUSES, include_blank: false
      f.input :payment_proof_status, as: :select, collection: Trade::PAYMENT_PROOF_STATUSES, include_blank: true
      f.input :dispute_resolution, as: :select, collection: Trade::DISPUTE_RESOLUTIONS, include_blank: true
    end
    f.actions
  end

  member_action :cancel_trade, method: :post do
    resource.send_trade_cancel_to_kafka
    redirect_to resource_path, notice: 'Trade cancelled successfully'
  end

  member_action :release_trade, method: :post do
    resource.send_trade_complete_to_kafka
    redirect_to resource_path, notice: 'Trade released successfully'
  end

  controller do
    def scoped_collection
      end_of_association_chain.includes(:buyer, :seller, :offer)
    end
  end
end
