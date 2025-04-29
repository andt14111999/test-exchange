# frozen_string_literal: true

ActiveAdmin.register SiteCountry do
  permit_params :country_code, :name, :currency, :timezone, :enabled,
                :min_trade_fiat, :max_trade_fiat, :max_total_amount_of_offer_for_fiat_token,
                supported_payment_methods: [], supported_banks: {}

  scope :all
  scope :enabled
  scope :disabled

  filter :id
  filter :country_code
  filter :name
  filter :currency
  filter :timezone
  filter :enabled
  filter :created_at
  filter :updated_at

  index do
    selectable_column
    id_column
    column :country_code
    column :name
    column :currency
    column :timezone
    column :enabled do |country|
      status_tag country.enabled? ? 'Enabled' : 'Disabled'
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :country_code
      row :name
      row :currency
      row :timezone
      row :enabled do |country|
        status_tag country.enabled? ? 'Enabled' : 'Disabled'
      end
      row :min_trade_fiat
      row :max_trade_fiat
      row :max_total_amount_of_offer_for_fiat_token
      row :supported_payment_methods do |country|
        ul do
          country.supported_payment_methods.each do |method|
            li method
          end
        end
      end
      row :supported_banks do |country|
        if country.supported_banks.present?
          table_for country.supported_banks.keys do
            column :bank_name do |bank_name|
              bank_name
            end
            column :details do |bank_name|
              pre { JSON.pretty_generate(country.supported_banks[bank_name]) }
            end
          end
        end
      end
      row :created_at
      row :updated_at
    end
  end

  form do |f|
    f.inputs 'Site Country Details' do
      f.input :country_code
      f.input :name
      f.input :currency
      f.input :timezone
      f.input :enabled
      f.input :min_trade_fiat
      f.input :max_trade_fiat
      f.input :max_total_amount_of_offer_for_fiat_token

      f.inputs 'Payment Methods' do
        f.input :supported_payment_methods, as: :select, multiple: true, collection: PaymentMethod.pluck(:name), input_html: { class: 'chosen-select' }
      end

      f.inputs 'Supported Banks' do
        f.input :supported_banks, as: :text, hint: 'JSON format, e.g.: {"Bank A": {"swift_code": "ABCDEFGH"}, "Bank B": {"swift_code": "IJKLMNOP"}}'
      end
    end
    f.actions
  end

  # Custom actions
  action_item :enable, only: :show, if: proc { !resource.enabled? } do
    link_to 'Enable', enable_admin_site_country_path(resource), method: :put
  end

  action_item :disable, only: :show, if: proc { resource.enabled? } do
    link_to 'Disable', disable_admin_site_country_path(resource), method: :put
  end

  action_item :add_payment_method, only: :show do
    link_to 'Add Payment Method', add_payment_method_admin_site_country_path(resource), method: :get
  end

  action_item :add_bank, only: :show do
    link_to 'Add Bank Support', add_bank_admin_site_country_path(resource), method: :get
  end

  member_action :enable, method: :put do
    resource.enable!
    redirect_to admin_site_country_path(resource), notice: 'Country has been enabled'
  end

  member_action :disable, method: :put do
    resource.disable!
    redirect_to admin_site_country_path(resource), notice: 'Country has been disabled'
  end

  member_action :add_payment_method, method: :get do
    @site_country = resource
    @available_methods = PaymentMethod.where(country_code: resource.country_code).where.not(name: resource.supported_payment_methods).pluck(:name)
    render 'admin/site_countries/add_payment_method'
  end

  member_action :submit_payment_method, method: :post do
    resource.add_payment_method(params[:payment_method])
    redirect_to admin_site_country_path(resource), notice: 'Payment method has been added'
  end

  member_action :remove_payment_method, method: :delete do
    resource.remove_payment_method(params[:payment_method])
    redirect_to admin_site_country_path(resource), notice: 'Payment method has been removed'
  end

  member_action :add_bank, method: :get do
    @site_country = resource
    render 'admin/site_countries/add_bank'
  end

  member_action :submit_bank, method: :post do
    details = JSON.parse(params[:bank_details]) rescue {}
    resource.add_bank_support(params[:bank_name], details)
    redirect_to admin_site_country_path(resource), notice: 'Bank has been added'
  end

  member_action :remove_bank, method: :delete do
    resource.remove_bank_support(params[:bank_name])
    redirect_to admin_site_country_path(resource), notice: 'Bank has been removed'
  end
end
