require 'rails_helper'

RSpec.describe MerchantRegistrationService, type: :service do
  describe '#call' do
    it 'registers user as merchant successfully' do
      user = create(:user, role: 'user', status: 'active')
      service = described_class.new(user)

      result = service.call

      expect(result).to be true
      expect(user.reload.role).to eq('merchant')
    end

    it 'returns false when user is already a merchant' do
      user = create(:user, role: 'merchant', status: 'active')
      service = described_class.new(user)

      result = service.call

      expect(result).to be false
      expect(user.reload.role).to eq('merchant')
    end

    it 'returns false when user is suspended' do
      user = create(:user, role: 'user', status: 'suspended')
      service = described_class.new(user)

      result = service.call

      expect(result).to be false
      expect(user.reload.role).to eq('user')
    end

    it 'returns false when user is banned' do
      user = create(:user, role: 'user', status: 'banned')
      service = described_class.new(user)

      result = service.call

      expect(result).to be false
      expect(user.reload.role).to eq('user')
    end

    it 'handles transaction failure gracefully' do
      user = create(:user, role: 'user', status: 'active')
      allow(user).to receive(:update!).and_raise(ActiveRecord::RecordInvalid)
      service = described_class.new(user)

      result = service.call

      expect(result).to be false
      expect(user.reload.role).to eq('user')
    end
  end
end 