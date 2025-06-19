# frozen_string_literal: true

class SeedCoinSettings < ActiveRecord::Migration[8.0]
  def up
    CoinAccount::SUPPORTED_NETWORKS.each do |currency, layers|
      CoinSetting.find_or_create_by!(currency: currency) do |setting|
        setting.deposit_enabled = true
        setting.withdraw_enabled = true
        setting.swap_enabled = true
        setting.layers = layers.map do |layer|
          {
            'layer' => layer,
            'deposit_enabled' => true,
            'withdraw_enabled' => true,
            'swap_enabled' => true,
            'maintenance' => false
          }
        end
      end
    end
  end

  def down
    CoinAccount::SUPPORTED_NETWORKS.each_key do |currency|
      CoinSetting.where(currency: currency).delete_all
    end
  end
end
