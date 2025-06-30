# frozen_string_literal: true

require 'rails_helper'
require 'cancan/matchers'

RSpec.describe AdminAbility do
  context 'when user is superadmin' do
    it 'can read, create, and update but not destroy' do
      admin = create(:admin_user, :superadmin)
      ability = described_class.new(admin)

      # Test that superadmin can read, create, and update
      expect(ability).to be_able_to(:read, :all)
      expect(ability).to be_able_to(:create, :all)
      expect(ability).to be_able_to(:update, :all)

      # Test that superadmin cannot destroy
      expect(ability).not_to be_able_to(:destroy, :all)
      expect(ability).not_to be_able_to(:destroy, User)
      expect(ability).not_to be_able_to(:destroy, AmmPool)
      expect(ability).not_to be_able_to(:destroy, Trade)
    end
  end

  context 'when general actions for all users' do
    it 'has basic permissions' do
      user = create(:admin_user, :operator)
      other_user = create(:admin_user, :operator)
      ability = described_class.new(user)

      expect(ability).to be_able_to(:read, ActiveAdmin::Page, name: 'Dashboard')
      expect(ability).to be_able_to(:read, user)
      expect(ability).not_to be_able_to(:read, other_user)
      expect(ability).to be_able_to(:manage, ActiveAdmin::Page, name: 'Setup 2FA')
    end
  end

  context 'when user is operator' do
    it 'can only read resources' do
      operator = create(:admin_user, :operator)
      ability = described_class.new(operator)

      # Test read permissions
      expect(ability).to be_able_to(:read, AmmPool)
      expect(ability).to be_able_to(:read, User)
      expect(ability).to be_able_to(:read, CoinDeposit)
      expect(ability).to be_able_to(:read, FiatDeposit)
      expect(ability).to be_able_to(:read, Trade)

      # Test that manage permissions are denied
      expect(ability).not_to be_able_to(:create, AmmPool)
      expect(ability).not_to be_able_to(:update, AmmPool)
      expect(ability).not_to be_able_to(:destroy, AmmPool)
      expect(ability).not_to be_able_to(:manage, AmmPool)
    end
  end
end
