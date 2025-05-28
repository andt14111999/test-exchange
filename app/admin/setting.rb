# frozen_string_literal: true

ActiveAdmin.register_page 'Settings' do
  menu priority: 1, label: 'Settings'

  content title: 'Settings' do
    active_admin_form_for :settings, url: admin_settings_update_settings_path, method: :post do |f|
      f.inputs 'USDT Exchange Rates' do
        f.input :usdt_to_vnd_rate, label: 'USDT to VND Rate',
                input_html: { value: Setting.usdt_to_vnd_rate, step: 'any', type: 'number' }
        f.input :usdt_to_php_rate, label: 'USDT to PHP Rate',
                input_html: { value: Setting.usdt_to_php_rate, step: 'any', type: 'number' }
        f.input :usdt_to_ngn_rate, label: 'USDT to NGN Rate',
                input_html: { value: Setting.usdt_to_ngn_rate, step: 'any', type: 'number' }
      end

      f.inputs 'USDT Withdrawal Fees' do
        f.input :usdt_erc20_withdrawal_fee, label: 'USDT ERC20 Withdrawal Fee',
                input_html: { value: Setting.usdt_erc20_withdrawal_fee, step: 'any', type: 'number' }
        f.input :usdt_bep20_withdrawal_fee, label: 'USDT BEP20 Withdrawal Fee',
                input_html: { value: Setting.usdt_bep20_withdrawal_fee, step: 'any', type: 'number' }
        f.input :usdt_solana_withdrawal_fee, label: 'USDT Solana Withdrawal Fee',
                input_html: { value: Setting.usdt_solana_withdrawal_fee, step: 'any', type: 'number' }
        f.input :usdt_trc20_withdrawal_fee, label: 'USDT TRC20 Withdrawal Fee',
                input_html: { value: Setting.usdt_trc20_withdrawal_fee, step: 'any', type: 'number' }
      end

      f.inputs 'Trading Fee Ratios (%)' do
        f.input :vnd_trading_fee_ratio, label: 'VND Trading Fee Ratio',
                input_html: { value: Setting.vnd_trading_fee_ratio, step: '0.0001', type: 'number' }
        f.input :php_trading_fee_ratio, label: 'PHP Trading Fee Ratio',
                input_html: { value: Setting.php_trading_fee_ratio, step: '0.0001', type: 'number' }
        f.input :ngn_trading_fee_ratio, label: 'NGN Trading Fee Ratio',
                input_html: { value: Setting.ngn_trading_fee_ratio, step: '0.0001', type: 'number' }
        f.input :default_trading_fee_ratio, label: 'Default Trading Fee Ratio',
                input_html: { value: Setting.default_trading_fee_ratio, step: '0.0001', type: 'number' }
      end

      f.inputs 'Fixed Trading Fees' do
        f.input :vnd_fixed_trading_fee, label: 'VND Fixed Trading Fee',
                input_html: { value: Setting.vnd_fixed_trading_fee, step: 'any', type: 'number' }
        f.input :php_fixed_trading_fee, label: 'PHP Fixed Trading Fee',
                input_html: { value: Setting.php_fixed_trading_fee, step: 'any', type: 'number' }
        f.input :ngn_fixed_trading_fee, label: 'NGN Fixed Trading Fee',
                input_html: { value: Setting.ngn_fixed_trading_fee, step: 'any', type: 'number' }
        f.input :default_fixed_trading_fee, label: 'Default Fixed Trading Fee',
                input_html: { value: Setting.default_fixed_trading_fee, step: 'any', type: 'number' }
      end

      f.actions do
        f.action :submit, label: 'Update Settings'
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

    panel 'Current Trading Fees' do
      attributes_table_for Setting do
        row 'VND Trading Fee Ratio' do
          "#{(Setting.vnd_trading_fee_ratio.to_f * 100).round(4)}%"
        end
        row 'PHP Trading Fee Ratio' do
          "#{(Setting.php_trading_fee_ratio.to_f * 100).round(4)}%"
        end
        row 'NGN Trading Fee Ratio' do
          "#{(Setting.ngn_trading_fee_ratio.to_f * 100).round(4)}%"
        end
        row 'Default Trading Fee Ratio' do
          "#{(Setting.default_trading_fee_ratio.to_f * 100).round(4)}%"
        end
        row 'VND Fixed Trading Fee' do
          Setting.vnd_fixed_trading_fee
        end
        row 'PHP Fixed Trading Fee' do
          Setting.php_fixed_trading_fee
        end
        row 'NGN Fixed Trading Fee' do
          Setting.ngn_fixed_trading_fee
        end
        row 'Default Fixed Trading Fee' do
          Setting.default_fixed_trading_fee
        end
      end
    end
  end

  page_action :update_settings, method: :post do
    if params[:settings].present?
      params[:settings].each do |key, value|
        next if value.blank?

        Setting.send("#{key}=", value) if Setting.respond_to?("#{key}=")
      end
      redirect_to admin_settings_path, notice: 'Settings updated successfully'
    else
      redirect_to admin_settings_path, alert: 'No settings provided'
    end
  end
end
