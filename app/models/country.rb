class Country < ApplicationRecord
  has_many :banks, dependent: :restrict_with_error

  validates :name, presence: true
  validates :code, presence: true, uniqueness: true

  scope :ordered, -> { order(:name) }
end
