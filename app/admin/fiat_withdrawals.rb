# frozen_string_literal: true

ActiveAdmin.register FiatWithdrawal do
  menu priority: 6, parent: 'Fiat Management'

  permit_params :user_id, :fiat_account_id, :currency, :country_code,
                :fiat_amount, :fee, :amount_after_transfer_fee,
                :bank_name, :bank_account_name, :bank_account_number, :bank_branch,
                :status, :retry_count, :error_message, :cancel_reason,
                :bank_reference, :bank_transaction_date, :bank_response_data,
                :verification_status, :verification_attempts,
                :withdrawable_type, :withdrawable_id

  scope :all
  scope :unprocessed
  scope :in_process
  scope :processed
  scope :bank_pending
  scope :bank_sent
  scope :bank_rejected
  scope :cancelled
  scope :for_trade
  scope :direct
  scope :stuck_in_process
  scope :recent_failures
  scope :retry_candidates

  filter :id
  filter :user
  filter :fiat_account
  filter :currency
  filter :country_code
  filter :fiat_amount
  filter :bank_name
  filter :bank_account_name
  filter :bank_account_number
  filter :status, as: :select, collection: FiatWithdrawal::STATUSES
  filter :verification_status, as: :select, collection: FiatWithdrawal::VERIFICATION_STATUSES
  filter :created_at
  filter :processed_at
  filter :cancelled_at

  index do
    selectable_column
    id_column
    column :user
    column :fiat_account
    column :currency
    column :country_code
    column :fiat_amount
    column :fee
    column :amount_after_transfer_fee
    column :bank_name
    column :bank_account_number
    column :status do |withdrawal|
      status_tag withdrawal.status
    end
    column :verification_status do |withdrawal|
      status_tag withdrawal.verification_status if withdrawal.verification_status.present?
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :user
      row :fiat_account
      row :currency
      row :country_code
      row :fiat_amount
      row :fee
      row :amount_after_transfer_fee
      row :bank_name
      row :bank_account_name
      row :bank_account_number
      row :bank_branch
      row :status do |withdrawal|
        status_tag withdrawal.status
      end
      row :verification_status do |withdrawal|
        status_tag withdrawal.verification_status if withdrawal.verification_status.present?
      end
      row :verification_attempts
      row :retry_count
      row :error_message
      row :cancel_reason
      row :bank_reference
      row :bank_transaction_date
      row :bank_response_data do |withdrawal|
        if withdrawal.bank_response_data.present?
          pre { JSON.pretty_generate(withdrawal.bank_response_data) }
        end
      end
      row :withdrawable_type
      row :withdrawable do |withdrawal|
        if withdrawal.withdrawable.present?
          if withdrawal.withdrawable_type == 'Trade'
            link_to "Trade #{withdrawal.withdrawable.ref}", admin_trade_path(withdrawal.withdrawable)
          else
            "#{withdrawal.withdrawable_type} ##{withdrawal.withdrawable_id}"
          end
        end
      end
      row :processed_at
      row :cancelled_at
      row :created_at
      row :updated_at
    end

    panel 'Admin Actions' do
      div do
        if resource.pending?
          span { button_to 'Mark as Processing', mark_as_processing_admin_fiat_withdrawal_path(resource), method: :post, class: 'button' }
        end

        if resource.processing?
          span { button_to 'Mark as Bank Pending', mark_as_bank_pending_admin_fiat_withdrawal_path(resource), method: :post, class: 'button' }
        end

        if resource.bank_pending?
          span { button_to 'Mark as Bank Sent', mark_as_bank_sent_admin_fiat_withdrawal_path(resource), method: :post, class: 'button' }
          span { link_to 'Mark as Bank Rejected', mark_as_bank_rejected_admin_fiat_withdrawal_path(resource), method: :get, class: 'button' }
        end

        if resource.bank_sent?
          span { button_to 'Process Withdrawal', process_withdrawal_admin_fiat_withdrawal_path(resource), method: :post,
                data: { confirm: 'Are you sure? This will mark the withdrawal as processed.' }, class: 'button' }
        end

        if resource.bank_rejected? && resource.can_be_retried?
          span { button_to 'Retry Withdrawal', retry_admin_fiat_withdrawal_path(resource), method: :post, class: 'button' }
        end

        if resource.can_be_cancelled?
          span { link_to 'Cancel Withdrawal', cancel_admin_fiat_withdrawal_path(resource), method: :get, class: 'button' }
        end

        # Verification actions
        if resource.verification_status == 'unverified' || resource.verification_status == 'failed'
          span { button_to 'Mark as Verifying', mark_as_verifying_admin_fiat_withdrawal_path(resource), method: :post, class: 'button' }
        end

        if resource.verification_status == 'verifying'
          span { button_to 'Mark as Verified', mark_as_verified_admin_fiat_withdrawal_path(resource), method: :post, class: 'button' }
          span { link_to 'Mark Verification Failed', mark_as_verification_failed_admin_fiat_withdrawal_path(resource), method: :get, class: 'button' }
        end
      end
    end
  end

  form do |f|
    f.inputs 'Fiat Withdrawal Details' do
      f.input :user
      f.input :fiat_account
      f.input :currency
      f.input :country_code
      f.input :fiat_amount
      f.input :fee
      f.input :amount_after_transfer_fee
      f.input :bank_name
      f.input :bank_account_name
      f.input :bank_account_number
      f.input :bank_branch
      f.input :status, as: :select, collection: FiatWithdrawal::STATUSES
      f.input :verification_status, as: :select, collection: FiatWithdrawal::VERIFICATION_STATUSES
      f.input :verification_attempts
      f.input :retry_count
      f.input :error_message
      f.input :cancel_reason
      f.input :bank_reference
      f.input :bank_transaction_date, as: :datetime_picker
      f.input :withdrawable_type
      f.input :withdrawable_id
    end
    f.actions
  end

  # Implement member_actions
  member_action :mark_as_processing, method: :post do
    resource.mark_as_processing!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal marked as processing'
  end

  member_action :mark_as_bank_pending, method: :post do
    resource.mark_as_bank_pending!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal marked as bank pending'
  end

  member_action :mark_as_bank_sent, method: :post do
    resource.mark_as_bank_sent!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal marked as bank sent'
  end

  member_action :process_withdrawal, method: :post do
    resource.process!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal has been processed'
  end

  member_action :mark_as_bank_rejected, method: :get do
    @fiat_withdrawal = resource
    render 'admin/fiat_withdrawals/mark_as_bank_rejected'
  end

  member_action :submit_bank_rejected, method: :post do
    resource.error_message_param = params[:error_message]
    resource.mark_as_bank_rejected!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal marked as bank rejected'
  end

  member_action :retry, method: :post do
    if resource.retry!
      redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal retry initiated'
    else
      redirect_to admin_fiat_withdrawal_path(resource), alert: 'Cannot retry. Maximum retry count reached or wrong status.'
    end
  end

  member_action :cancel, method: :get do
    @fiat_withdrawal = resource
    render 'admin/fiat_withdrawals/cancel'
  end

  member_action :submit_cancel, method: :post do
    resource.cancel_reason_param = params[:cancel_reason]
    resource.cancel!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal has been cancelled'
  end

  member_action :mark_as_verifying, method: :post do
    resource.start_verification
    resource.save!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal marked as verifying'
  end

  member_action :mark_as_verified, method: :post do
    resource.verify
    resource.save!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal marked as verified'
  end

  member_action :mark_as_verification_failed, method: :get do
    @fiat_withdrawal = resource
    render 'admin/fiat_withdrawals/mark_as_verification_failed'
  end

  member_action :submit_verification_failed, method: :post do
    resource.verification_failure_reason = params[:error_message]
    resource.fail_verification
    resource.save!
    redirect_to admin_fiat_withdrawal_path(resource), notice: 'Withdrawal verification failed'
  end
end
