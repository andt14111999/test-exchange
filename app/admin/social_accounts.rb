# frozen_string_literal: true

ActiveAdmin.register SocialAccount do
  menu priority: 3, label: 'Social Accounts'

  permit_params :user_id, :provider, :provider_user_id, :email, :name,
    :access_token, :refresh_token, :token_expires_at, :avatar_url, :profile_data

  actions :all, except: [ :destroy ]

  filter :user, collection: proc { User.all.map { |u| [ u.email, u.id ] } }
  filter :provider, as: :select, collection: -> { SocialAccount.distinct.pluck(:provider) }
  filter :email
  filter :name
  filter :created_at
  filter :updated_at
  filter :token_expires_at

  scope :all, default: true
  scope :google, -> { where(provider: 'google') }
  scope :facebook, -> { where(provider: 'facebook') }
  scope :apple, -> { where(provider: 'apple') }
  scope('Valid Tokens') { |scope| scope.where('token_expires_at > ?', Time.current) }
  scope('Expired Tokens') { |scope| scope.where(token_expires_at: ..Time.current) }

  index do
    selectable_column
    id_column
    column :user do |account|
      link_to account.user.email, admin_user_path(account.user)
    end
    column :provider do |account|
      status_tag account.provider
    end
    column :email
    column :name
    column :avatar_url do |account|
      if account.avatar_url.present?
        image_tag account.avatar_url, style: 'width: 50px; height: 50px; border-radius: 50%;'
      end
    end
    column :token_status do |account|
      if account.token_expires_at.nil?
        status_tag 'No Expiry', class: 'warning'
      elsif account.token_expires_at > Time.current
        status_tag 'Valid', class: 'ok'
      else
        status_tag 'Expired', class: 'error'
      end
    end
    column :created_at
    actions
  end

  show do
    tabs do
      tab 'Account Details' do
        attributes_table do
          row :id
          row :user do |account|
            link_to account.user.email, admin_user_path(account.user)
          end
          row :provider do |account|
            status_tag account.provider
          end
          row :provider_user_id
          row :email
          row :name
          row :avatar_url do |account|
            if account.avatar_url.present?
              image_tag account.avatar_url, style: 'max-width: 200px; max-height: 200px; border-radius: 8px;'
            end
          end
          row :created_at
          row :updated_at
        end
      end

      tab 'Token Information' do
        attributes_table do
          row :access_token do |account|
            if account.access_token.present?
              truncate(account.access_token, length: 50)
            else
              status_tag 'No Token', class: 'error'
            end
          end
          row :refresh_token do |account|
            if account.refresh_token.present?
              truncate(account.refresh_token, length: 50)
            else
              status_tag 'No Refresh Token', class: 'error'
            end
          end
          row :token_expires_at do |account|
            if account.token_expires_at.nil?
              status_tag 'No Expiry', class: 'warning'
            elsif account.token_expires_at > Time.current
              status_tag "Valid (Expires #{time_ago_in_words(account.token_expires_at)} from now)", class: 'ok'
            else
              status_tag "Expired (#{time_ago_in_words(account.token_expires_at)} ago)", class: 'error'
            end
          end
        end
      end

      tab 'Profile Data' do
        panel 'Raw Profile Data' do
          if resource.profile_data.present?
            pre do
              code JSON.pretty_generate(resource.profile_data)
            end
          else
            para 'No profile data available'
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
          f.input :user, collection: User.all.map { |u| [ u.email, u.id ] }
          f.input :provider, as: :select, collection: %w[google facebook apple]
          f.input :provider_user_id
          f.input :email
          f.input :name
          f.input :avatar_url
        end
      end

      tab 'Token Information' do
        f.inputs do
          f.input :access_token
          f.input :refresh_token
          f.input :token_expires_at, as: :datetime_picker
        end
      end

      tab 'Profile Data' do
        f.inputs do
          f.input :profile_data, as: :text,
            input_html: { rows: 10, class: 'jsoneditor-target' }
        end
      end
    end
    f.actions
  end

  batch_action :refresh_tokens, confirm: 'Are you sure you want to refresh tokens for selected accounts?' do |ids|
    batch_action_collection.find(ids).each do |account|
      # TODO: Implement token refresh logic here
    end
    flash[:notice] = 'Token refresh has been initiated for selected accounts'
    redirect_to collection_path
  end

  action_item :refresh_token, only: :show do
    link_to 'Refresh Token', refresh_token_admin_social_account_path(resource), method: :put
  end

  member_action :refresh_token, method: :put do
    redirect_to admin_social_account_path(resource), notice: 'Token refresh has been initiated'
  end

  sidebar 'Account Stats', only: :show do
    attributes_table do
      row 'Token Status' do |account|
        if account.token_expires_at.nil?
          status_tag 'No Expiry', class: 'warning'
        elsif account.token_expires_at > Time.current
          status_tag 'Valid', class: 'ok'
        else
          status_tag 'Expired', class: 'error'
        end
      end
      row 'Account Age' do |account|
        time_ago_in_words(account.created_at)
      end
      row 'Last Updated' do |account|
        time_ago_in_words(account.updated_at)
      end
    end
  end
end
