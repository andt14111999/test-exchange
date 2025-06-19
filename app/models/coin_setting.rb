class CoinSetting < ApplicationRecord
  validates :currency, presence: true, uniqueness: true
  validates :layers, presence: true

  def self.ransackable_attributes(auth_object = nil)
    %w[
      id
      currency
      deposit_enabled
      withdraw_enabled
      swap_enabled
      layers
      created_at
      updated_at
    ]
  end
end
