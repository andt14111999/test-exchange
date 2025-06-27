# frozen_string_literal: true

ActiveAdmin.register FiatDeposit do
  menu priority: 5, parent: 'Fiat Management'
  actions :all, except: [ :destroy ]

  permit_params :status, :explorer_ref, :sender_name, :sender_account_number, :ownership_proof_url

  filter :id
  filter :user
  filter :fiat_account
  filter :currency
  filter :country_code
  filter :fiat_amount
  filter :memo
  filter :status, as: :select, collection: FiatDeposit::STATUSES
  filter :created_at
  filter :processed_at
  filter :cancelled_at
  filter :payable_type
  filter :explorer_ref

  scope :all
  scope :unprocessed
  scope :pending_user_action
  scope :pending_admin_action
  scope :processing
  scope :processed
  scope :cancelled
  scope :refunding
  scope :illegal
  scope :locked
  scope :for_trade
  scope :direct
  scope :needs_ownership_verification

  index do
    selectable_column
    id_column
    column :user
    column :currency
    column :country_code
    column :fiat_amount
    column :memo
    column :status
    column :created_at
    column :processed_at
    column :cancelled_at
    column :payable do |deposit|
      if deposit.payable_type == 'Trade' && deposit.payable.present?
        link_to "Trade ##{deposit.payable.ref}", admin_trade_path(deposit.payable)
      else
        deposit.payable_type
      end
    end
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
      row :original_fiat_amount
      row :deposit_fee
      row :amount_after_fee
      row :explorer_ref
      row :memo
      row :fiat_deposit_details
      row :ownership_proof_url
      row :sender_name
      row :sender_account_number
      row :status
      row :cancel_reason
      row :processed_at
      row :cancelled_at
      row :money_sent_at
      row :created_at
      row :updated_at
      row :payable do |deposit|
        if deposit.payable_type == 'Trade' && deposit.payable.present?
          link_to "Trade ##{deposit.payable.ref}", admin_trade_path(deposit.payable)
        else
          deposit.payable_type
        end
      end
    end

    panel 'Admin Actions' do
      div do
        if resource.awaiting? || resource.pending?
          span { link_to 'Mark as Ready', mark_as_ready_admin_fiat_deposit_path(resource), method: :post, class: 'button' }
        end

        if resource.ready?
          span { link_to 'Mark as Informed', mark_as_informed_admin_fiat_deposit_path(resource), method: :post, class: 'button' }
          span { link_to 'Mark as Verifying', mark_as_verifying_admin_fiat_deposit_path(resource), method: :post, class: 'button' }
        end

        if resource.verifying? || resource.ownership_verifying?
          span { link_to 'Process Deposit', process_deposit_admin_fiat_deposit_path(resource), method: :post, class: 'button' }
        end

        if resource.may_cancel?
          span { link_to 'Cancel Deposit', cancel_admin_fiat_deposit_path(resource), method: :post, class: 'button' }
        end

        if resource.status == 'ownership_verifying'
          span { link_to 'Mark as Locked', mark_as_locked_admin_fiat_deposit_path(resource), method: :post, class: 'button' }
        end

        if resource.may_mark_as_illegal?
          span { link_to 'Mark as Illegal', mark_as_illegal_admin_fiat_deposit_path(resource), method: :post, class: 'button' }
        end
      end
    end
  end

  form do |f|
    f.inputs 'Fiat Deposit Details' do
      f.input :status, as: :select, collection: FiatDeposit::STATUSES
      f.input :explorer_ref
      f.input :sender_name
      f.input :sender_account_number
      f.input :ownership_proof_url
    end
    f.actions
  end

  # Admin Actions
  member_action :mark_as_ready, method: :post do
    resource.mark_as_ready! if resource.may_mark_as_ready?
    redirect_to resource_path, notice: 'Deposit marked as ready'
  end

  member_action :mark_as_informed, method: :post do
    resource.mark_as_informed! if resource.may_mark_as_informed?
    redirect_to resource_path, notice: 'Deposit marked as informed'
  end

  member_action :mark_as_verifying, method: :post do
    resource.mark_as_verifying! if resource.may_mark_as_verifying?
    redirect_to resource_path, notice: 'Deposit marked as verifying'
  end

  member_action :process_deposit, method: :post do
    if resource.may_process?
      result = resource.process!
      Rails.logger.info "Process result: #{result}, New status: #{resource.reload.status}"
      redirect_to resource_path, notice: 'Deposit processed'
    else
      redirect_to resource_path, alert: 'Cannot process deposit in current state'
    end
  end

  member_action :cancel, method: :post do
    if resource.may_cancel?
      resource.update!(cancel_reason: "Cancelled by admin: #{current_admin_user.email}")
      resource.cancel!
      redirect_to resource_path, notice: 'Deposit cancelled'
    else
      redirect_to resource_path, alert: 'Cannot cancel deposit in current state'
    end
  end

  member_action :mark_as_locked, method: :post do
    if resource.may_mark_as_locked?
      resource.cancel_reason_param = "Locked by admin: #{current_admin_user.email}"
      resource.mark_as_locked!
      redirect_to resource_path, notice: 'Deposit locked'
    else
      redirect_to resource_path, alert: 'Cannot lock deposit in current state'
    end
  end

  member_action :mark_as_illegal, method: :post do
    if resource.may_mark_as_illegal?
      resource.mark_as_illegal!
      redirect_to resource_path, notice: 'Deposit marked as illegal'
    else
      redirect_to resource_path, alert: 'Cannot mark deposit as illegal in current state'
    end
  end
end
