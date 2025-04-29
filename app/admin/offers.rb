# frozen_string_literal: true

ActiveAdmin.register Offer do
  permit_params :user_id, :offer_type, :coin_currency, :currency, :price,
                :min_amount, :max_amount, :total_amount, :payment_method_id,
                :payment_time, :payment_details, :country_code, :disabled, :deleted,
                :automatic, :online, :terms_of_trade, :disable_reason, :margin,
                :fixed_coin_price, :bank_names, :schedule_start_time, :schedule_end_time

  scope :all
  scope :active
  scope :disabled
  scope :deleted
  scope :scheduled
  scope :currently_active
  scope :buy_offers
  scope :sell_offers
  scope :online
  scope :offline
  scope :automatic
  scope :manual

  filter :id
  filter :user
  filter :offer_type, as: :select, collection: Offer::OFFER_TYPES
  filter :coin_currency
  filter :currency
  filter :price
  filter :min_amount
  filter :max_amount
  filter :total_amount
  filter :payment_method
  filter :country_code
  filter :created_at
  filter :updated_at

  index do
    selectable_column
    id_column
    column :user
    column :offer_type
    column :coin_currency
    column :currency
    column :price
    column :min_amount
    column :max_amount
    column :total_amount
    column :payment_method
    column :country_code
    column :status do |offer|
      status_tag(
        if offer.deleted?
          'deleted'
        elsif offer.disabled?
          'disabled'
        elsif offer.scheduled?
          if offer.currently_active?
            'scheduled (active)'
          else
            'scheduled (inactive)'
          end
        elsif offer.active?
          'active'
        end
      )
    end
    column :online
    column :automatic
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :user
      row :offer_type
      row :coin_currency
      row :currency
      row :price
      row :min_amount
      row :max_amount
      row :total_amount
      row :available_amount
      row :payment_method
      row :payment_time
      row :payment_details
      row :country_code
      row :status do |offer|
        status_tag(
          if offer.deleted?
            'deleted'
          elsif offer.disabled?
            'disabled'
          elsif offer.scheduled?
            if offer.currently_active?
              'scheduled (active)'
            else
              'scheduled (inactive)'
            end
          elsif offer.active?
            'active'
          end
        )
      end
      row :disable_reason
      row :online
      row :automatic
      row :terms_of_trade
      row :margin
      row :fixed_coin_price
      row :bank_names
      row :schedule_start_time
      row :schedule_end_time
      row :created_at
      row :updated_at
    end

    panel 'Associated Trades' do
      table_for resource.trades do
        column :id do |trade|
          link_to trade.id, admin_trade_path(trade)
        end
        column :buyer
        column :seller
        column :coin_amount
        column :fiat_amount
        column :status
        column :created_at
      end
    end
  end

  form do |f|
    f.inputs 'Offer Details' do
      f.input :user
      f.input :offer_type, as: :select, collection: Offer::OFFER_TYPES
      f.input :coin_currency
      f.input :currency
      f.input :price
      f.input :min_amount
      f.input :max_amount
      f.input :total_amount
      f.input :payment_method
      f.input :payment_time
      f.input :payment_details
      f.input :country_code
      f.input :disabled
      f.input :deleted
      f.input :automatic
      f.input :online
      f.input :terms_of_trade
      f.input :disable_reason
      f.input :margin
      f.input :fixed_coin_price
      f.input :bank_names, as: :text
      f.input :schedule_start_time, as: :datetime_picker
      f.input :schedule_end_time, as: :datetime_picker
    end
    f.actions
  end

  # Custom actions
  action_item :enable, only: :show, if: proc { resource.disabled? } do
    link_to 'Enable', enable_admin_offer_path(resource), method: :put
  end

  action_item :disable, only: :show, if: proc { !resource.disabled? && !resource.deleted? } do
    link_to 'Disable', disable_admin_offer_path(resource), method: :put
  end

  action_item :delete, only: :show, if: proc { !resource.deleted? } do
    link_to 'Delete', delete_admin_offer_path(resource), method: :put, data: { confirm: 'Are you sure? This will cancel all awaiting trades.' }
  end

  member_action :enable, method: :put do
    resource.enable!
    redirect_to admin_offer_path(resource), notice: 'Offer has been enabled'
  end

  member_action :disable, method: :put do
    resource.disable!('Disabled by admin')
    redirect_to admin_offer_path(resource), notice: 'Offer has been disabled'
  end

  member_action :delete, method: :put do
    resource.delete!
    redirect_to admin_offer_path(resource), notice: 'Offer has been deleted'
  end
end
