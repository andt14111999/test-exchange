# frozen_string_literal: true

module CategorizedAccount
  extend ActiveSupport::Concern
  module ClassMethods
    def safe_take_or_create
      cached_take = take
      return cached_take if cached_take.present?

      RedisMutex.with_lock(scope_attributes.to_s, block: 10, expire: 20) do
        ActiveRecord::Base.connected_to(role: :writing) { first_or_create }
      end
    rescue ActiveRecord::RecordNotUnique
      take
    end

    def intermediate_all
      where(account_type: 'intermediate')
    end

    def main_all
      where(account_type: 'main')
    end

    def deposit_all
      where(account_type: 'deposit')
    end

    def main
      main_all.safe_take_or_create
    end

    def deposit
      deposit_all.last || main_all.last || deposit_all.safe_take_or_create
    end

    def deposit_account_existed?
      deposit_all.exists? || main_all.exists?
    end

    def deposit_address_existed?
      deposit_all.pluck(:address).compact.present? || main_all.pluck(:address).compact.present?
    end

    def intermediate
      intermediate_all.safe_take_or_create
    end
  end

  def main?
    account_type == 'main'
  end

  def payable?
    account_type == 'payable'
  end

  def intermediate?
    account_type == 'intermediate'
  end

  def can_hold_balance?
    main? || intermediate?
  end
end
