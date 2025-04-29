# frozen_string_literal: true

ActiveAdmin.register BankAccount do
  permit_params :user_id, :bank_name, :account_name, :account_number,
                :branch, :country_code, :verified, :is_primary

  scope :all
  scope :verified
  scope :unverified
  scope :primary

  filter :id
  filter :user
  filter :bank_name
  filter :account_name
  filter :account_number
  filter :country_code
  filter :verified
  filter :is_primary
  filter :created_at
  filter :updated_at

  index do
    selectable_column
    id_column
    column :user
    column :bank_name
    column :account_name
    column :account_number
    column :branch
    column :country_code
    column :verified do |account|
      status_tag account.verified? ? 'Verified' : 'Unverified'
    end
    column :is_primary
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :user
      row :bank_name
      row :account_name
      row :account_number
      row :branch
      row :country_code
      row :verified do |account|
        status_tag account.verified? ? 'Verified' : 'Unverified'
      end
      row :is_primary
      row :created_at
      row :updated_at
    end
  end

  form do |f|
    f.inputs 'Bank Account Details' do
      f.input :user
      f.input :bank_name
      f.input :account_name
      f.input :account_number
      f.input :branch
      f.input :country_code
      f.input :verified
      f.input :is_primary
    end
    f.actions
  end

  # Custom actions
  action_item :verify, only: :show, if: proc { !resource.verified? } do
    link_to 'Verify Account', verify_admin_bank_account_path(resource), method: :put
  end

  action_item :make_primary, only: :show, if: proc { !resource.is_primary? } do
    link_to 'Make Primary', make_primary_admin_bank_account_path(resource), method: :put
  end

  member_action :verify, method: :put do
    resource.mark_as_verified!
    redirect_to admin_bank_account_path(resource), notice: 'Bank account has been verified'
  end

  member_action :make_primary, method: :put do
    resource.mark_as_primary!
    redirect_to admin_bank_account_path(resource), notice: 'Bank account is now the primary account'
  end
end
