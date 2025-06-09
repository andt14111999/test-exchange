# frozen_string_literal: true

require 'rails_helper'
require 'cancan/matchers'

RSpec.describe AdminAbility do
  context 'when user is admin' do
    it 'can manage all' do
      admin = create(:admin_user, :super_admin)
      ability = described_class.new(admin)

      expect(ability).to be_able_to(:manage, :all)
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

  context 'when user is implementor' do
    it 'can manage AmmPool' do
      operator = create(:admin_user, :operator)
      ability = described_class.new(operator)

      expect(ability).to be_able_to(:manage, AmmPool)
    end

    it 'can read AmmPool' do
      operator = create(:admin_user, :operator)
      ability = described_class.new(operator)

      expect(ability).to be_able_to(:read, AmmPool)
    end
  end
end
