class Country < ApplicationRecord
  has_many :banks, dependent: :restrict_with_error

  validates :name, presence: true
  validates :code, presence: true, uniqueness: true

  scope :ordered, -> { order(:name) }

  def self.ransackable_attributes(_auth_object = nil)
    %w[name code]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[banks]
  end
end
