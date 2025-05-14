# frozen_string_literal: true

ActiveAdmin.register ApiKey do
  menu priority: 5, parent: 'User Management', label: 'API Keys'

  actions :all, except: [ :edit, :update ]

  permit_params :user_id, :name

  index do
    selectable_column
    id_column
    column :user
    column :name
    column :access_key
    column :last_used_at
    column :status do |api_key|
      if api_key.revoked_at.present?
        status_tag 'Revoked', class: 'error'
      else
        status_tag 'Active', class: 'ok'
      end
    end
    column :created_at
    actions
  end

  filter :user
  filter :name
  filter :access_key
  filter :last_used_at
  filter :revoked_at, as: :boolean, label: 'Revoked'
  filter :created_at

  show do
    attributes_table do
      row :id
      row :user
      row :name
      row :access_key
      row :last_used_at
      row :status do |api_key|
        if api_key.revoked_at.present?
          status_tag 'Revoked', class: 'error'
        else
          status_tag 'Active', class: 'ok'
        end
      end
      row :revoked_at
      row :created_at
      row :updated_at
    end
  end

  form do |f|
    f.inputs 'API Key Details' do
      f.input :user
      f.input :name
    end
    f.actions
  end

  batch_action :revoke do |ids|
    batch_action_collection.find(ids).each do |api_key|
      api_key.update(revoked_at: Time.current)
    end
    redirect_to collection_path, notice: 'Selected API keys have been revoked'
  end

  member_action :revoke, method: :put do
    resource.update(revoked_at: Time.current)
    redirect_to admin_api_key_path(resource), notice: 'API key has been revoked'
  end

  action_item :revoke, only: :show do
    link_to 'Revoke API Key', revoke_admin_api_key_path(resource), method: :put if resource.revoked_at.nil?
  end

  # Custom action to regenerate API key
  member_action :regenerate, method: :post do
    # Create a new API key for the user with the same name
    user = resource.user
    name = resource.name

    # Revoke the old key first
    resource.update(revoked_at: Time.current)

    # Create a new key
    new_key = ApiKey.create!(
      user: user,
      name: "#{name} (regenerated)"
    )

    redirect_to admin_api_key_path(new_key), notice: 'New API key has been generated'
  end

  action_item :regenerate, only: :show do
    link_to 'Regenerate API Key', regenerate_admin_api_key_path(resource), method: :post if resource.revoked_at.nil?
  end
end
