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

  context 'when user is deactivated' do
    it 'has no permissions at all' do
      deactivated_superadmin = create(:admin_user, :superadmin, deactivated: true)
      deactivated_operator = create(:admin_user, :operator, deactivated: true)

      superadmin_ability = described_class.new(deactivated_superadmin)
      operator_ability = described_class.new(deactivated_operator)

      # Test that deactivated superadmin has no permissions
      expect(superadmin_ability).not_to be_able_to(:read, :all)
      expect(superadmin_ability).not_to be_able_to(:create, :all)
      expect(superadmin_ability).not_to be_able_to(:update, :all)
      expect(superadmin_ability).not_to be_able_to(:destroy, :all)
      expect(superadmin_ability).not_to be_able_to(:manage, :all)

      # Test that deactivated operator has no permissions
      expect(operator_ability).not_to be_able_to(:read, User)
      expect(operator_ability).not_to be_able_to(:read, AmmPool)
      expect(operator_ability).not_to be_able_to(:read, Trade)
      expect(operator_ability).not_to be_able_to(:read, ActiveAdmin::Page, name: 'Dashboard')
      expect(operator_ability).not_to be_able_to(:manage, ActiveAdmin::Page, name: 'Setup 2FA')

      # Test specific models
      expect(superadmin_ability).not_to be_able_to(:read, CoinDeposit)
      expect(superadmin_ability).not_to be_able_to(:read, FiatDeposit)
      expect(operator_ability).not_to be_able_to(:read, CoinDeposit)
      expect(operator_ability).not_to be_able_to(:read, FiatDeposit)
    end
  end
end
