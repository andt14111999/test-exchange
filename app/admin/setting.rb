# frozen_string_literal: true

ActiveAdmin.register_page 'Settings' do
  menu priority: 1, label: 'Exchange Rates'

  content title: 'Exchange Rates' do
    active_admin_form_for :settings, url: admin_settings_update_rates_path, method: :post do |f|
      f.inputs 'USDT Exchange Rates' do
        f.input :usdt_to_vnd_rate, label: 'USDT to VND Rate',
                input_html: { value: Setting.usdt_to_vnd_rate, step: 'any', type: 'number' }
        f.input :usdt_to_php_rate, label: 'USDT to PHP Rate',
                input_html: { value: Setting.usdt_to_php_rate, step: 'any', type: 'number' }
        f.input :usdt_to_ngn_rate, label: 'USDT to NGN Rate',
                input_html: { value: Setting.usdt_to_ngn_rate, step: 'any', type: 'number' }
      end
      f.actions do
        f.action :submit, label: 'Update Exchange Rates'
      end
    end

    panel 'Current Exchange Rates' do
      attributes_table_for Setting do
        row 'USDT to VND Rate' do
          Setting.usdt_to_vnd_rate
        end
        row 'USDT to PHP Rate' do
          Setting.usdt_to_php_rate
        end
        row 'USDT to NGN Rate' do
          Setting.usdt_to_ngn_rate
        end
      end
    end
  end

  page_action :update_rates, method: :post do
    if params[:settings].present?
      params[:settings].each do |key, value|
        next if value.blank?

        Setting.send("#{key}=", value) if Setting.respond_to?("#{key}=")
      end
      redirect_to admin_settings_path, notice: 'Exchange rates updated successfully'
    else
      redirect_to admin_settings_path, alert: 'No exchange rates provided'
    end
  end
end
