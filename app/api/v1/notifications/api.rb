# frozen_string_literal: true

module V1
  module Notifications
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :notifications do
        desc 'Get all notifications for current user'
        params do
          optional :page, type: Integer, default: 1, desc: 'Page number for pagination'
          optional :per_page, type: Integer, default: 20, desc: 'Number of items per page'
        end

        get do
          notifications = current_user.notifications
            .order(created_at: :desc)
            .page(params[:page])
            .per(params[:per_page])

          present({
            status: 'success',
            data: {
              notifications: notifications.map do |notification|
                {
                  id: notification.id,
                  title: notification.title,
                  content: notification.content,
                  type: notification.notification_type,
                  read: notification.read,
                  created_at: notification.created_at
                }
              end,
              pagination: {
                current_page: notifications.current_page,
                total_pages: notifications.total_pages,
                total_count: notifications.total_count,
                per_page: notifications.limit_value
              }
            }
          })
        end

        desc 'Mark a notification as read'
        params do
          requires :id, type: Integer, desc: 'Notification ID'
        end

        put ':id/read' do
          notification = current_user.notifications.find_by(id: params[:id])

          if notification
            notification.update!(read: true)
            present({
              status: 'success',
              data: {
                id: notification.id,
                title: notification.title,
                content: notification.content,
                type: notification.notification_type,
                read: notification.read,
                created_at: notification.created_at
              }
            })
          else
            error!({
              status: 'error',
              message: 'Notification not found'
            }, 404)
          end
        end

        desc 'Mark all notifications as read'
        put :mark_all_read do
          current_user.notifications.where(read: false).update_all(read: true)

          present({
            status: 'success',
            message: 'All notifications marked as read'
          })
        end
      end
    end
  end
end
