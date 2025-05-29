# frozen_string_literal: true

class PopulateTradingFeeSettings < ActiveRecord::Migration[8.0]
  def up
    # Import values from config files to settings table
    safety_assured do
      # Trading Fee Ratios
      execute <<-SQL
        INSERT INTO settings (var, value, created_at, updated_at)
        VALUES#{' '}
          ('vnd_trading_fee_ratio', '0.001', NOW(), NOW()),
          ('php_trading_fee_ratio', '0.001', NOW(), NOW()),
          ('ngn_trading_fee_ratio', '0.001', NOW(), NOW()),
          ('default_trading_fee_ratio', '0.001', NOW(), NOW())
        ON CONFLICT (var) DO UPDATE
        SET value = EXCLUDED.value, updated_at = NOW()
      SQL

      # Fixed Trading Fees
      execute <<-SQL
        INSERT INTO settings (var, value, created_at, updated_at)
        VALUES#{' '}
          ('vnd_fixed_trading_fee', '5000', NOW(), NOW()),
          ('php_fixed_trading_fee', '10', NOW(), NOW()),
          ('ngn_fixed_trading_fee', '300', NOW(), NOW()),
          ('default_fixed_trading_fee', '0', NOW(), NOW())
        ON CONFLICT (var) DO UPDATE
        SET value = EXCLUDED.value, updated_at = NOW()
      SQL
    end
  end

  def down
    safety_assured do
      # Remove the trading fee settings
      execute <<-SQL
        DELETE FROM settings WHERE var IN (
          'vnd_trading_fee_ratio',
          'php_trading_fee_ratio',
          'ngn_trading_fee_ratio',
          'default_trading_fee_ratio',
          'vnd_fixed_trading_fee',
          'php_fixed_trading_fee',
          'ngn_fixed_trading_fee',
          'default_fixed_trading_fee'
        )
      SQL
    end
  end
end
