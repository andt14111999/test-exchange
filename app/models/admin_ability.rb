# frozen_string_literal: true

class AdminAbility
  include CanCan::Ability
  attr_reader :current_user

  def initialize(user)
    @current_user = user
    can :manage, :all if user.admin?

    apply_implementor_actions if user.implementor?
    apply_general_actions
  end

  def apply_general_actions
    can %i[read], ActiveAdmin::Page, name: 'Dashboard'
    can %i[read], AdminUser, { id: current_user.id }
    can %i[manage], ActiveAdmin::Page, name: 'Setup 2FA'
    can :read, AmmPool
    can :manage, SocialAccount
  end

  def apply_implementor_actions
    can :manage, AmmPool
  end
end
