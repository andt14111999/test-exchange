# frozen_string_literal: true

class PaymentMethod < ApplicationRecord
  has_many :offers, dependent: :nullify

  validates :name, presence: true, uniqueness: true
  validates :display_name, presence: true
  validates :country_code, presence: true
  validates :fields_required, presence: true

  scope :enabled, -> { where(enabled: true) }
  scope :disabled, -> { where(enabled: false) }
  scope :of_country, ->(country_code) { where(country_code: country_code) }

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id name display_name description
      country_code enabled icon_url
      fields_required created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[offers]
  end

  def enable!
    update!(enabled: true)
  end

  def disable!
    update!(enabled: false)
  end

  def required_fields
    fields_required.symbolize_keys
  end
end
