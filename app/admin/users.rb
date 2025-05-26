# frozen_string_literal: true

ActiveAdmin.register User do
  menu priority: 2, label: 'Users'

  permit_params :email, :display_name, :avatar_url, :role,
    :phone_verified, :document_verified, :kyc_level, :status, :username

  actions :all

  filter :id
  filter :email
  filter :username
  filter :display_name
  filter :role, as: :select, collection: %w[merchant user]
  filter :status, as: :select, collection: %w[active suspended banned]
  filter :kyc_level, as: :select, collection: [ 0, 1, 2 ]
  filter :phone_verified
  filter :document_verified
  filter :created_at
  filter :updated_at

  scope :all, default: true
  scope :merchants, -> { where(role: 'merchant') }
  scope :regular_users, -> { where(role: 'user') }
  scope('Active Users') { |scope| scope.where(status: 'active') }
  scope('Suspended Users') { |scope| scope.where(status: 'suspended') }
  scope('Banned Users') { |scope| scope.where(status: 'banned') }
  scope('Phone Verified') { |scope| scope.where(phone_verified: true) }
  scope('Document Verified') { |scope| scope.where(document_verified: true) }

  index do
    selectable_column
    id_column
    column :email
    column :username
    column :display_name
    column :avatar_url do |user|
      image_tag user.avatar_url, style: 'width: 50px; height: 50px; border-radius: 50%;' if user.avatar_url.present?
    end
    column :role do |user|
      status_tag user.role, class: user.role == 'merchant' ? 'warning' : 'ok'
    end
    column :status do |user|
      status_classes = { 'active' => 'ok', 'suspended' => 'warning', 'banned' => 'error' }
      status_tag user.status, class: status_classes[user.status]
    end
    column :kyc_level do |user|
      kyc_labels = { 0 => [ 'No KYC', 'error' ], 1 => [ 'Phone KYC', 'warning' ], 2 => [ 'Full KYC', 'ok' ] }
      label, status = kyc_labels[user.kyc_level]
      status_tag label, class: status
    end
    column :phone_verified do |user|
      status_tag user.phone_verified ? 'Verified' : 'Unverified',
        class: user.phone_verified ? 'ok' : 'error'
    end
    column :document_verified do |user|
      status_tag user.document_verified ? 'Verified' : 'Unverified',
        class: user.document_verified ? 'ok' : 'error'
    end
    column :created_at
    actions
  end

  show do
    tabs do
      tab 'User Details' do
        attributes_table do
          row :id
          row :email
          row :username
          row :display_name
          row :avatar_url do |user|
            if user.avatar_url.present?
              image_tag user.avatar_url, style: 'max-width: 200px; max-height: 200px; border-radius: 8px;'
            end
          end
          row :role do |user|
            status_tag user.role, class: user.role == 'merchant' ? 'warning' : 'ok'
          end
          row :status do |user|
            status_classes = { 'active' => 'ok', 'suspended' => 'warning', 'banned' => 'error' }
            status_tag user.status, class: status_classes[user.status]
          end
          row :kyc_level do |user|
            kyc_labels = { 0 => [ 'No KYC', 'error' ], 1 => [ 'Phone KYC', 'warning' ], 2 => [ 'Full KYC', 'ok' ] }
            label, status = kyc_labels[user.kyc_level]
            status_tag label, class: status
          end
          row :phone_verified do |user|
            status_tag user.phone_verified ? 'Verified' : 'Unverified',
              class: user.phone_verified ? 'ok' : 'error'
          end
          row :document_verified do |user|
            status_tag user.document_verified ? 'Verified' : 'Unverified',
              class: user.document_verified ? 'ok' : 'error'
          end
          row :created_at
          row :updated_at
        end
      end

      tab 'Social Accounts' do
        panel 'Connected Social Accounts' do
          table_for resource.social_accounts do
            column :provider do |account|
              status_tag account.provider
            end
            column :email
            column :name
            column :avatar_url do |account|
              if account.avatar_url.present?
                image_tag account.avatar_url,
                  style: 'width: 50px; height: 50px; border-radius: 50%;'
              end
            end
            column :created_at
            column :actions do |account|
              links = []
              links << link_to('View', admin_social_account_path(account))
              links << link_to('Edit', edit_admin_social_account_path(account))
              safe_join(links, ' | ')
            end
          end
        end
      end
    end

    active_admin_comments
  end

  form do |f|
    f.semantic_errors
    tabs do
      tab 'Basic Info' do
        f.inputs do
          f.input :email
          f.input :username,
                  hint: 'Can only be set once. 3-20 characters, letters, numbers, and underscores only.',
                  input_html: { disabled: f.object.username.present? }
          f.input :display_name
          f.input :avatar_url, as: :string
          if f.object.avatar_url.present?
            f.input :current_avatar, required: false, as: :file,
              hint: image_tag(f.object.avatar_url, style: 'max-width: 200px; max-height: 200px;')
          end
        end
      end

      tab 'Status & Role' do
        f.inputs do
          f.input :role, as: :select, collection: %w[merchant user]
          f.input :status, as: :select, collection: %w[active suspended banned]
          f.input :kyc_level, as: :select, collection: [
            [ 'No KYC', 0 ],
            [ 'Phone KYC', 1 ],
            [ 'Full KYC', 2 ]
          ]
        end
      end

      tab 'Verification' do
        f.inputs do
          f.input :phone_verified
          f.input :document_verified
        end
      end
    end
    f.actions
  end

  batch_action :verify_phone do |ids|
    batch_action_collection.find(ids).each do |user|
      user.update(phone_verified: true)
    end
    redirect_to collection_path, notice: 'Phone verification status updated successfully'
  end

  batch_action :verify_document do |ids|
    batch_action_collection.find(ids).each do |user|
      user.update(document_verified: true)
    end
    redirect_to collection_path, notice: 'Document verification status updated successfully'
  end

  batch_action :suspend do |ids|
    batch_action_collection.find(ids).each do |user|
      user.update(status: 'suspended')
    end
    redirect_to collection_path, notice: 'Selected users have been suspended'
  end

  batch_action :activate do |ids|
    batch_action_collection.find(ids).each do |user|
      user.update(status: 'active')
    end
    redirect_to collection_path, notice: 'Selected users have been activated'
  end

  batch_action :ban do |ids|
    batch_action_collection.find(ids).each do |user|
      user.update(status: 'banned')
    end
    redirect_to collection_path, notice: 'Selected users have been banned'
  end

  action_item :activate, only: :show do
    link_to 'Activate User', activate_admin_user_path(resource), method: :put if resource.status != 'active'
  end

  action_item :suspend, only: :show do
    link_to 'Suspend User', suspend_admin_user_path(resource), method: :put if resource.status != 'suspended'
  end

  action_item :ban, only: :show do
    link_to 'Ban User', ban_admin_user_path(resource), method: :put if resource.status != 'banned'
  end

  member_action :activate, method: :put do
    resource.update(status: 'active')
    redirect_to admin_user_path(resource), notice: 'User has been activated'
  end

  member_action :suspend, method: :put do
    resource.update(status: 'suspended')
    redirect_to admin_user_path(resource), notice: 'User has been suspended'
  end

  member_action :ban, method: :put do
    resource.update(status: 'banned')
    redirect_to admin_user_path(resource), notice: 'User has been banned'
  end

  sidebar 'User Stats', only: :show do
    attributes_table do
      row 'Social Accounts' do |user|
        user.social_accounts.count
      end
      row 'Account Age' do |user|
        time_ago_in_words(user.created_at)
      end
      row 'Last Updated' do |user|
        time_ago_in_words(user.updated_at)
      end
    end
  end
end
