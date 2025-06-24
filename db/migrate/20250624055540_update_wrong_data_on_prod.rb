class UpdateWrongDataOnProd < ActiveRecord::Migration[8.0]
  def change
    CoinWithdrawal.where(coin_address: [ 'sftreasure', 'non-existing' ]).each do |withdrawal|
      withdrawal.update_columns(status: 'cancelled')
    end
  end
end
