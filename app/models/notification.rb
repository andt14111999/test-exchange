# frozen_string_literal: true

class Notification < ApplicationRecord
  belongs_to :user

  validates :title, presence: true
  validates :content, presence: true
  validates :notification_type, presence: true

  after_create :broadcast_notification

  def self.ransackable_attributes(_auth_object = nil)
    %w[
      content
      created_at
      delivered
      id
      notification_type
      read
      title
      updated_at
      user_id
    ]
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user]
  end

  private

  def broadcast_notification
    NotificationBroadcastService.call(user, self)
  end
end
