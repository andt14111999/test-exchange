# frozen_string_literal: true

ActiveAdmin.register PaymentMethod do
  menu priority: 6, parent: 'Fiat Management'
  actions :all, except: [ :destroy ]

  permit_params :name, :display_name, :description, :country_code,
                :enabled, :icon_url, :fields_required

  scope :all
  scope :enabled
  scope :disabled

  filter :id
  filter :name
  filter :display_name
  filter :country_code
  filter :enabled
  filter :created_at
  filter :updated_at

  index do
    selectable_column
    id_column
    column :name
    column :display_name
    column :country_code
    column :enabled do |method|
      status_tag method.enabled? ? 'Enabled' : 'Disabled'
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :name
      row :display_name
      row :description
      row :country_code
      row :enabled do |method|
        status_tag method.enabled? ? 'Enabled' : 'Disabled'
      end
      row :icon_url
      row :fields_required do |method|
        pre { JSON.pretty_generate(method.fields_required) }
      end
      row :created_at
      row :updated_at
    end

    panel 'Related Offers' do
      table_for resource.offers do
        column :id do |offer|
          link_to offer.id, admin_offer_path(offer)
        end
        column :user
        column :offer_type
        column :currency
        column :coin_currency
        column :status do |offer|
          status_tag(offer.active? ? 'active' : 'inactive')
        end
      end
    end
  end

  form do |f|
    f.inputs 'Payment Method Details' do
      f.input :name
      f.input :display_name
      f.input :description
      f.input :country_code
      f.input :enabled
      f.input :icon_url
      f.input :fields_required, as: :text, hint: 'JSON format for required fields, e.g.: {"bank_number": "Bank Number", "account_number": "Account Number"}'
    end
    f.actions
  end

  # Custom actions
  action_item :enable, only: :show, if: proc { !resource.enabled? } do
    link_to 'Enable', enable_admin_payment_method_path(resource), method: :put
  end

  action_item :disable, only: :show, if: proc { resource.enabled? } do
    link_to 'Disable', disable_admin_payment_method_path(resource), method: :put
  end

  member_action :enable, method: :put do
    resource.enable!
    redirect_to admin_payment_method_path(resource), notice: 'Payment method has been enabled'
  end

  member_action :disable, method: :put do
    resource.disable!
    redirect_to admin_payment_method_path(resource), notice: 'Payment method has been disabled'
  end
end
