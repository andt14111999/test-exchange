class Bank < ApplicationRecord
  belongs_to :country

  validates :name, presence: true
  validates :code, presence: true, uniqueness: true
  validates :bin, presence: true, uniqueness: true
  validates :short_name, presence: true
  validates :transfer_supported, inclusion: { in: [ true, false ] }
  validates :lookup_supported, inclusion: { in: [ true, false ] }
  validates :support, presence: true, numericality: { only_integer: true, greater_than_or_equal_to: 0 }
  validates :is_transfer, inclusion: { in: [ true, false ] }

  scope :ordered, -> { order(:name) }
  scope :by_country, ->(country_code) { joins(:country).where(countries: { code: country_code }) }
  scope :transfer_supported, -> { where(transfer_supported: true) }
  scope :lookup_supported, -> { where(lookup_supported: true) }
end
