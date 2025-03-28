# frozen_string_literal: true

class Notification < ApplicationRecord
  belongs_to :user

  validates :title, presence: true
  validates :content, presence: true
  validates :notification_type, presence: true

  after_create :broadcast_notification

  private

  def broadcast_notification
    NotificationBroadcastService.call(user, self)
  end
end
