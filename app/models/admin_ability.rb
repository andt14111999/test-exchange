# frozen_string_literal: true

class AdminAbility
  include CanCan::Ability
  attr_reader :current_user

  def initialize(user)
    @current_user = user

    if user.superadmin?
      can :manage, :all
    elsif user.operator?
      apply_operator_permissions
    end

    apply_general_actions
  end

  def apply_general_actions
    # Common permissions for all admin users
    can %i[read], ActiveAdmin::Page, name: 'Dashboard'
    can %i[read], AdminUser, id: current_user.id
    can %i[manage], ActiveAdmin::Page, name: 'Setup 2FA'
  end

  def apply_operator_permissions
    # User Management
    can :manage, User
    can :manage, SocialAccount
    can :manage, ApiKey

    # Coin Management
    can :manage, CoinAccount
    can :manage, CoinDeposit
    can :manage, CoinDepositOperation
    can :manage, CoinWithdrawal
    can :manage, CoinWithdrawalOperation
    can :manage, CoinTransaction
    can :manage, BalanceLock
    can :manage, BalanceLockOperation
    can :manage, CoinSetting

    # Fiat Management
    can :manage, FiatDeposit
    can :manage, FiatWithdrawal
    can :manage, FiatTransaction
    can :manage, BankAccount
    can :manage, PaymentMethod

    # Trade Management
    can :manage, Trade
    can :manage, Message
    can :manage, Offer

    # AMM Management
    can :manage, AmmPool
    can :manage, AmmPosition
    can :manage, AmmOrder

    # System
    can :manage, Notification
    can :read, KafkaEvent
  end
end
