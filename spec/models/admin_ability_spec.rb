# frozen_string_literal: true

require 'rails_helper'
require 'cancan/matchers'

RSpec.describe AdminAbility do
  context 'when user is admin' do
    it 'can manage all' do
      admin = create(:admin_user, :admin)
      ability = described_class.new(admin)

      expect(ability).to be_able_to(:manage, :all)
    end
  end

  context 'when general actions for all users' do
    it 'has basic permissions' do
      user = create(:admin_user)
      ability = described_class.new(user)

      expect(ability).to be_able_to(:read, ActiveAdmin::Page, name: 'Dashboard')
      expect(ability).to be_able_to(:read, AdminUser.new(id: user.id))
      expect(ability).not_to be_able_to(:read, AdminUser.new(id: user.id + 1))
      expect(ability).to be_able_to(:manage, ActiveAdmin::Page, name: 'Setup 2FA')
    end
  end
end
