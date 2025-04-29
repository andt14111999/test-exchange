# frozen_string_literal: true

class BankAccount < ApplicationRecord
  belongs_to :user

  validates :bank_name, presence: true
  validates :account_name, presence: true
  validates :account_number, presence: true
  validates :country_code, presence: true
  validates :is_primary, uniqueness: { scope: [ :user_id, :bank_name ], if: :is_primary? }

  scope :verified, -> { where(verified: true) }
  scope :unverified, -> { where(verified: false) }
  scope :primary, -> { where(is_primary: true) }
  scope :of_country, ->(country_code) { where(country_code: country_code) }

  before_save :ensure_single_primary, if: :is_primary?

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id user_id bank_name account_name account_number
      branch country_code verified is_primary
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user]
  end

  def mark_as_verified!
    update!(verified: true)
  end

  def mark_as_primary!
    update!(is_primary: true)
  end

  private

  def ensure_single_primary
    return unless is_primary_changed? && is_primary?

    user.bank_accounts
        .where(bank_name: bank_name)
        .where.not(id: id)
        .update_all(is_primary: false)
  end
end
