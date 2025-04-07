# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::Notifications' do
  describe 'UI', type: :system do
    let(:admin) { create(:admin_user, roles: 'admin') }
    let(:user) { create(:user) }
    let(:notification) do
      create(:notification,
             user: user,
             title: 'Test Notification',
             content: 'This is a test notification',
             notification_type: 'system',
             read: false,
             delivered: false)
    end

    before do
      sign_in admin, scope: :admin_user
      notification
    end

    describe '#index' do
      it 'displays notifications list' do
        visit admin_notifications_path

        expect(page).to have_content(notification.id)
        expect(page).to have_content('Test Notification')
        expect(page).to have_content('system')
      end

      it 'allows filtering by notification type' do
        visit admin_notifications_path

        within '.filter_form' do
          # For notification_type, it uses a text input with a dropdown for search type
          find('#q_notification_type').set('system')
          click_button 'Filter'
        end

        expect(page).to have_content('Test Notification')
      end

      it 'allows filtering by read status' do
        visit admin_notifications_path
        within '.filter_form' do
          select 'No', from: 'Read'
          click_button 'Filter'
        end

        expect(page).to have_content('Test Notification')
      end

      it 'displays unread notifications using scope' do
        visit admin_notifications_path
        within '.scopes' do
          click_link 'Unread'
        end

        expect(page).to have_content('Test Notification')
      end

      it 'displays undelivered notifications using scope' do
        # Ensure notification is undelivered
        notification.update(delivered: false)

        visit admin_notifications_path
        within '.scopes' do
          click_link 'Undelivered'
        end

        expect(page).to have_content('Test Notification')
      end
    end

    describe '#show' do
      it 'displays notification details' do
        visit admin_notification_path(notification)

        expect(page).to have_content(notification.id)
        expect(page).to have_content('Test Notification')
        expect(page).to have_content('This is a test notification')
        expect(page).to have_content('system')
        # Check for "No" instead of "false" for read status
        expect(page).to have_content('No')
      end
    end

    describe '#new' do
      it 'allows creating new notification' do
        visit new_admin_notification_path

        within 'form' do
          first('select[name="notification[user_id]"]').find(:option, text: /User \d+/).select_option
          fill_in 'Title', with: 'New Notification'
          fill_in 'Content', with: 'This is a new notification'
          select 'system', from: 'Notification type'
          click_button 'Create Notification'
        end

        expect(page).to have_current_path(/admin\/notifications/)
      end
    end

    describe '#edit' do
      it 'allows editing notification' do
        visit edit_admin_notification_path(notification)

        within 'form' do
          fill_in 'Title', with: 'Updated Notification'
          select 'transaction', from: 'Notification type'
          click_button 'Update Notification'
        end

        expect(page).to have_current_path(/admin\/notifications/)
      end
    end
  end

  describe 'API', type: :request do
    let(:admin) { create(:admin_user, roles: 'admin') }
    let(:user) { create(:user) }

    before do
      sign_in admin, scope: :admin_user
      # Override the after_create callback in Notification that auto-broadcasts
      allow_any_instance_of(Notification).to receive(:broadcast_notification).and_return(true)
    end

    it 'resends undelivered notification' do
      # Create notification with delivered=false and disable auto-broadcast
      notification = create(:notification, user: user, delivered: false)
      notification.update_column(:delivered, false) # Ensure it's false regardless of callback

      expect(NotificationBroadcastService).to receive(:call)
        .with(notification.user, notification)
        .once
        .and_return(true)

      post "/admin/notifications/#{notification.id}/resend"

      expect(response).to redirect_to("/admin/notifications/#{notification.id}")
      follow_redirect!
      expect(response.body).to include('Notification has been sent')
    end

    it 'does not resend already delivered notification' do
      notification = create(:notification, user: user, delivered: true)
      expect(NotificationBroadcastService).not_to receive(:call)

      post "/admin/notifications/#{notification.id}/resend"

      expect(response).to redirect_to("/admin/notifications/#{notification.id}")
      follow_redirect!
      expect(response.body).to include('Notification has already been delivered')
    end

    it 'marks selected notifications as read with batch action' do
      notifications = create_list(:notification, 3, user: user, read: false)

      post "/admin/notifications/batch_action", params: {
        batch_action: 'mark_as_read',
        collection_selection: notifications.map(&:id)
      }

      expect(response).to redirect_to("/admin/notifications")
      follow_redirect!
      expect(response.body).to include('Notifications have been marked as read')

      # Verify all notifications are now read
      notifications.each do |notification|
        expect(notification.reload.read).to be true
      end
    end

    it 'resends selected notifications with batch action' do
      notifications = create_list(:notification, 3, user: user)
      expect(NotificationBroadcastService).to receive(:call).exactly(3).times

      post "/admin/notifications/batch_action", params: {
        batch_action: 'resend',
        collection_selection: notifications.map(&:id)
      }

      expect(response).to redirect_to("/admin/notifications")
      follow_redirect!
      expect(response.body).to include('Notifications have been resent')
    end
  end
end
