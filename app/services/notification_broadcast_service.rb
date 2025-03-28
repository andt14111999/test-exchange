# frozen_string_literal: true

class NotificationBroadcastService
  def self.call(user, notification)
    new(user, notification).call
  end

  def initialize(user, notification)
    @user = user
    @notification = notification
  end

  def call
    success = broadcast_notification

    if !success && @notification.respond_to?(:delivered)
      @notification.update(delivered: false)
    end

    success
  end

  private

  def broadcast_notification
    begin
      NotificationChannel.broadcast_to(@user, notification_data)
      @notification.update(delivered: true) if @notification.respond_to?(:delivered)
      true
    rescue => e
      false
    end
  end

  def notification_data
    {
      status: 'success',
      data: {
        id: @notification.id,
        title: @notification.title,
        content: @notification.content,
        type: @notification.notification_type,
        read: @notification.read,
        created_at: @notification.created_at
      }
    }
  end
end
