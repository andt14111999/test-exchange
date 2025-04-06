require 'rails_helper'

RSpec.describe Notification, type: :model do
  describe 'validations' do
    it 'is invalid without title' do
      notification = build(:notification, title: nil)
      expect(notification).to be_invalid
      expect(notification.errors[:title]).to include("can't be blank")
    end

    it 'is invalid without content' do
      notification = build(:notification, content: nil)
      expect(notification).to be_invalid
      expect(notification.errors[:content]).to include("can't be blank")
    end

    it 'is invalid without notification_type' do
      notification = build(:notification, notification_type: nil)
      expect(notification).to be_invalid
      expect(notification.errors[:notification_type]).to include("can't be blank")
    end
  end

  describe 'associations' do
    it 'belongs to user' do
      notification = create(:notification)
      expect(notification.user).to be_present
    end
  end

  describe 'ransackable_attributes' do
    it 'returns correct attributes' do
      expected_attributes = %w[
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
      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe 'ransackable_associations' do
    it 'returns correct associations' do
      expect(described_class.ransackable_associations).to contain_exactly('user')
    end
  end

  describe 'callbacks' do
    it 'broadcasts notification after create' do
      notification = build(:notification)
      expect(NotificationBroadcastService).to receive(:call).with(notification.user, notification)
      notification.save
    end
  end
end
