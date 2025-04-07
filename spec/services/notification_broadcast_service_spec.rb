require 'rails_helper'

RSpec.describe NotificationBroadcastService, type: :service do
  describe '.call' do
    it 'creates a new instance and calls #call' do
      user = create(:user)
      notification = create(:notification)
      service = instance_double(described_class)
      allow(described_class).to receive(:new).with(user, notification).and_return(service)
      allow(service).to receive(:call).and_return(true)

      result = described_class.call(user, notification)

      expect(result).to be true
      expect(service).to have_received(:call)
    end
  end

  describe '#call' do
    context 'when broadcast is successful' do
      it 'returns true' do
        user = create(:user)
        notification = create(:notification)
        service = described_class.new(user, notification)
        allow(service).to receive(:broadcast_notification).and_return(true)

        result = service.call

        expect(result).to be true
      end

      it 'does not update notification delivered status' do
        user = create(:user)
        notification = create(:notification)
        service = described_class.new(user, notification)
        allow(service).to receive(:broadcast_notification).and_return(true)

        expect { service.call }.not_to change { notification.reload.delivered }
      end
    end

    context 'when broadcast fails' do
      it 'returns false' do
        user = create(:user)
        notification = create(:notification)
        service = described_class.new(user, notification)
        allow(service).to receive(:broadcast_notification).and_return(false)

        result = service.call

        expect(result).to be false
      end

      it 'updates notification delivered status to false' do
        user = create(:user)
        notification = create(:notification, :delivered)
        service = described_class.new(user, notification)
        allow(service).to receive(:broadcast_notification).and_return(false)

        expect { service.call }.to change { notification.reload.delivered }.from(true).to(false)
      end
    end
  end

  describe '#broadcast_notification' do
    context 'when broadcast is successful' do
      it 'returns true' do
        user = create(:user)
        notification = create(:notification)
        service = described_class.new(user, notification)
        allow(NotificationChannel).to receive(:broadcast_to).with(user, anything).and_return(true)

        result = service.send(:broadcast_notification)

        expect(result).to be true
      end

      it 'updates notification delivered status to true' do
        user = create(:user)
        notification = build(:notification, :not_delivered)
        service = described_class.new(user, notification)

        # Stub the broadcast_to method to simulate successful broadcast
        allow(NotificationChannel).to receive(:broadcast_to) do |user, data|
          expect(data[:status]).to eq('success')
          expect(data[:data][:id]).to eq(notification.id)
          true
        end

        # Verify initial state
        expect(notification.delivered).to be false

        # Perform the broadcast and verify the change
        service.send(:broadcast_notification)
        expect(notification.reload.delivered).to be true
      end
    end

    context 'when broadcast raises an error' do
      it 'returns false' do
        user = create(:user)
        notification = create(:notification)
        service = described_class.new(user, notification)
        allow(NotificationChannel).to receive(:broadcast_to).and_raise(StandardError)

        result = service.send(:broadcast_notification)

        expect(result).to be false
      end

      it 'does not update notification delivered status' do
        user = create(:user)
        notification = create(:notification, :not_delivered)
        service = described_class.new(user, notification)
        allow(NotificationChannel).to receive(:broadcast_to).and_raise(StandardError)

        expect { service.send(:broadcast_notification) }.not_to change { notification.reload.delivered }
      end
    end
  end

  describe '#notification_data' do
    it 'returns formatted notification data' do
      user = create(:user)
      notification = create(:notification)
      service = described_class.new(user, notification)

      result = service.send(:notification_data)

      expect(result).to eq(
        status: 'success',
        data: {
          id: notification.id,
          title: notification.title,
          content: notification.content,
          type: notification.notification_type,
          read: notification.read,
          created_at: notification.created_at
        }
      )
    end
  end
end
