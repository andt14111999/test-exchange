# frozen_string_literal: true

class Message < ApplicationRecord
  belongs_to :trade
  belongs_to :user

  validates :body, presence: true
  validates :is_receipt_proof, inclusion: { in: [ true, false ] }
  validates :is_system, inclusion: { in: [ true, false ] }

  scope :receipt_proofs, -> { where(is_receipt_proof: true) }
  scope :system_messages, -> { where(is_system: true) }
  scope :user_messages, -> { where(is_system: false) }
  scope :sorted, -> { order(created_at: :asc) }

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      id trade_id user_id body
      is_receipt_proof is_system
      created_at updated_at
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[trade user]
  end

  def mark_as_receipt_proof!
    update!(is_receipt_proof: true)
  end

  def mark_as_regular_message!
    update!(is_receipt_proof: false)
  end
end
