class AddExchangeRateToMerchantEscrows < ActiveRecord::Migration[8.0]
  def change
    add_column :merchant_escrows, :exchange_rate, :decimal, precision: 20, scale: 8
  end
end
