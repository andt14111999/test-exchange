# frozen_string_literal: true

class KafkaEvent < ApplicationRecord
  validates :event_id, presence: true
  validates :topic_name, presence: true
  validates :event_id, uniqueness: { scope: :topic_name }
  validates :status, presence: true, inclusion: { in: %w[pending processed failed] }

  scope :processed, -> { where(status: 'processed') }
  scope :unprocessed, -> { where(status: 'pending') }
  scope :failed, -> { where(status: 'failed') }
  scope :recent, -> { order(created_at: :desc) }

  before_validation :set_status, on: :create
  before_save :update_status

  def processing_time
    return nil unless processed_at
    (processed_at - created_at).round(2)
  end

  def error_message
    payload&.dig('errorMessage')
  end

  def operation_type
    payload&.dig('object', 'operationType') || payload&.dig('operation_type')
  end

  def object_identifier
    payload&.dig('object', 'identifier') || payload&.dig('object', 'key') || payload&.dig('key')
  end

  def reprocess!
    KafkaService::ReprocessService.new.reprocess(self)
  end

  def self.ransackable_attributes(auth_object = nil)
    %w[
      created_at
      event_id
      id
      payload
      process_message
      processed_at
      status
      topic_name
      updated_at
    ]
  end

  def self.ransackable_associations(auth_object = nil)
    []
  end

  private

  def set_status
    self.status = 'pending'
  end

  def update_status
    self.status = if error_message.present?
                   'failed'
    elsif processed_at.present?
                   'processed'
    else
                   'pending'
    end
  end
end
