# frozen_string_literal: true

class AddAmountReceivedToAmmOrders < ActiveRecord::Migration[6.1]
  def change
    add_column :amm_orders, :amount_received, :decimal, precision: 32, scale: 16, default: 0
  end
end
