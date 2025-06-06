# frozen_string_literal: true

module KafkaService
  module Handlers
    class BalanceLockHandler < BaseHandler
      def handle(payload)
        return if payload.nil?

        if payload['object'] && payload['object']['actionType'] == 'COIN_TRANSACTION'
          process_transaction_response(payload)
        end
      end

      private

      def process_transaction_response(payload)
        object = payload['object']
        return unless object

        identifier = object['identifier']
        return unless identifier

        balance_lock = BalanceLock.find_by(id: identifier)
        return unless balance_lock

        status = object['status']
        is_success = payload['isSuccess']
        error_message = payload['errorMessage']
        locked_balances = parse_locked_balances(object['lockedBalances'])
        lock_id = object['lockId']

        return balance_lock.fail!(error_message) unless is_success

        case status
        when 'LOCKED'
          process_locked_response(balance_lock, locked_balances, lock_id)
        when 'RELEASED'
          process_released_response(balance_lock)
        end
      end

      def parse_locked_balances(locked_balances)
        result = {}
        locked_balances.each do |account_key, amount|
          _, account_type, account_id = account_key.split('-')

          if account_type == 'coin'
            coin_account = CoinAccount.find_by(id: account_id)
            result[coin_account&.coin_currency || account_key] = amount
          elsif account_type == 'fiat'
            fiat_account = FiatAccount.find_by(id: account_id)
            result[fiat_account&.currency || account_key] = amount
          else
            result[account_key] = amount
          end
        end
        result
      end

      def process_locked_response(balance_lock, locked_balances, lock_id)
        balance_lock.locked_balances = locked_balances
        balance_lock.engine_lock_id = lock_id
        if balance_lock.pending?
          balance_lock.mark_as_locked!
          Rails.logger.info("Balance lock successful for lock_id=#{balance_lock.id}")
        end
      end

      def process_released_response(balance_lock)
        balance_lock.release!
        Rails.logger.info("Balance unlock successful for lock_id=#{balance_lock.id}")
      end
    end
  end
end
