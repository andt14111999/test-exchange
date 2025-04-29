class AddPaymentProofToFiatDeposits < ActiveRecord::Migration[8.0]
  def change
    add_column :fiat_deposits, :payment_proof_url, :string
    add_column :fiat_deposits, :payment_description, :text
  end
end
