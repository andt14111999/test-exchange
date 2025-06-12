# frozen_string_literal: true

class CoinDepositOperation < CoinOperation
  acts_as_paranoid

  has_many :coin_transactions, as: :operation, dependent: :destroy
  belongs_to :coin_account, optional: true
  belongs_to :coin_deposit, optional: true, touch: true

  validates :out_index, presence: true
  validates :coin_amount, presence: true, numericality: { greater_than: 0 }
  validates :coin_fee, presence: true, numericality: { greater_than_or_equal_to: 0 }
  validates :coin_account, presence: true
  validates :tx_hash, presence: true
  delegate :user, to: :coin_account

  after_create :create_coin_transactions

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id coin_account_id coin_deposit_id coin_currency
      coin_amount coin_fee tx_hash out_index status
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[coin_account coin_deposit coin_transactions]
  end

  def coin_amount_after_fee
    (coin_amount - coin_fee - platform_fee).to_d.floor(8)
  end

  def main_coin_account
    user.coin_accounts.of_coin(coin_currency).main
  end

  private

  def create_coin_transactions
    coin_transactions.create!(
      amount: coin_amount_after_fee,
      coin_currency: coin_currency,
      coin_account: main_coin_account
    )
    if platform_fee.positive?
      coin_transactions.create!(
        amount: platform_fee,
        coin_currency: coin_currency,
        coin_account: main_coin_account
      )
    end
  rescue StandardError => e
    Rails.logger.error("Failed to create coin transactions: #{e.message}")
    raise
  end
end
