# frozen_string_literal: true

class AdminAbility
  include CanCan::Ability
  attr_reader :current_user

  def initialize(user)
    @current_user = user

    if user.superadmin?
      # Superadmins can manage everything (includes all actions)
      can :manage, :all
      # But explicitly cannot destroy anything
      cannot :destroy, :all
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
    can :read, User
    can :read, SocialAccount
    can :read, ApiKey

    # Coin Management
    can :read, CoinAccount
    can :read, CoinDeposit
    can :read, CoinDepositOperation
    can :read, CoinWithdrawal
    can :read, CoinWithdrawalOperation
    can :read, CoinTransaction
    can :read, BalanceLock
    can :read, BalanceLockOperation
    can :read, Setting
    can :read, CoinSetting

    # Fiat Management
    can :read, FiatDeposit
    can :read, FiatWithdrawal
    can :read, FiatTransaction
    can :read, BankAccount
    can :read, PaymentMethod

    # Trade Management
    can :read, Trade
    can :read, Message
    can :read, Offer

    # AMM Management
    can :read, AmmPool
    can :read, AmmPosition
    can :read, AmmOrder

    # System
    can :read, Notification
    can :read, KafkaEvent
  end
end
