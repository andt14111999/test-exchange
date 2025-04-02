# frozen_string_literal: true

ActiveAdmin.register Notification do
  permit_params :user_id, :title, :content, :notification_type, :read, :delivered

  filter :user
  filter :title
  filter :notification_type
  filter :read
  filter :delivered
  filter :created_at
  filter :updated_at

  scope :all, default: true
  scope :unread do |notifications|
    notifications.where(read: false)
  end
  scope :undelivered do |notifications|
    notifications.where(delivered: false)
  end

  index do
    selectable_column
    id_column
    column :user
    column :title
    column :notification_type
    column :read
    column :delivered
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :user
      row :title
      row :content
      row :notification_type
      row :read
      row :delivered
      row :created_at
      row :updated_at
    end
  end

  form do |f|
    f.inputs 'Notification Details' do
      f.input :user
      f.input :title
      f.input :content
      f.input :notification_type, as: :select, collection: [ 'system', 'transaction', 'balance', 'security', 'promotion', 'test' ]
      f.input :read
      f.input :delivered
    end
    f.actions
  end

  action_item :resend_notification, only: :show do
    link_to 'Resend Notification', resend_admin_notification_path(notification), method: :post, data: { confirm: 'Are you sure?' } if !notification.delivered
  end

  member_action :resend, method: :post do
    notification = Notification.find(params[:id])

    if notification.delivered
      redirect_to admin_notification_path(notification), alert: 'Notification has already been delivered'
    else
      # Send notification via ActionCable
      NotificationBroadcastService.call(notification.user, notification)
      redirect_to admin_notification_path(notification), notice: 'Notification has been sent'
    end
  end

  batch_action :mark_as_read do |ids|
    batch_action_collection.find(ids).each do |notification|
      notification.update(read: true)
    end
    redirect_to collection_path, notice: 'Notifications have been marked as read'
  end

  batch_action :resend do |ids|
    batch_action_collection.find(ids).each do |notification|
      NotificationBroadcastService.call(notification.user, notification)
    end
    redirect_to collection_path, notice: 'Notifications have been resent'
  end
end
