# frozen_string_literal: true

class FiatTransactionService
  def initialize(user)
    @user = user
  end

  def create_deposit(currency, country_code, amount)
    FiatDeposit.transaction do
      fiat_account = @user.fiat_accounts.find_by(currency: currency.upcase)

      deposit = FiatDeposit.create!(
        user: @user,
        fiat_account: fiat_account,
        currency: currency,
        country_code: country_code,
        fiat_amount: amount,
        original_fiat_amount: amount,
        status: 'awaiting'
      )

      # A direct deposit doesn't have a payable
      deposit.mark_as_pending! if deposit.may_mark_as_pending?

      deposit
    end
  end

  def create_withdrawal(currency, country_code, amount, bank_account_id)
    FiatWithdrawal.transaction do
      fiat_account = @user.fiat_accounts.find_by(currency: currency.upcase)

      # Check if user has sufficient balance
      if fiat_account.available_balance < amount
        raise StandardError, 'Insufficient fiat balance for withdrawal'
      end

      withdrawal = FiatWithdrawal.create!(
        user: @user,
        fiat_account: fiat_account,
        bank_account_id: bank_account_id,
        currency: currency,
        country_code: country_code,
        fiat_amount: amount,
        status: 'awaiting'
      )

      # Lock funds in fiat account
      fiat_account.lock_amount!(amount, "Withdrawal #{withdrawal.id}")

      # A direct withdrawal doesn't have a withdrawable
      withdrawal.mark_as_pending! if withdrawal.may_mark_as_pending?

      withdrawal
    end
  end

  # Methods for processing fiat token trades
  def process_deposit_for_trade(trade)
    return false unless trade.is_fiat_token_deposit_trade?
    return false unless trade.released?

    deposit = trade.fiat_token_deposit
    return false unless deposit&.may_process?

    deposit.process!
    create_fiat_deposit_transaction(deposit)
  end

  def process_withdrawal_for_trade(trade)
    return false unless trade.is_fiat_token_withdrawal_trade?
    return false unless trade.released?

    withdrawal = trade.fiat_token_withdrawal
    return false unless withdrawal&.may_process?

    withdrawal.process!
    create_fiat_withdrawal_transaction(withdrawal)
  end

  def cancel_deposit_for_trade(trade, reason = nil)
    return false unless trade.is_fiat_token_deposit_trade?
    return false if trade.released?

    deposit = trade.fiat_token_deposit
    return false unless deposit&.may_cancel?

    deposit.cancel_reason_param = reason if reason.present?
    deposit.cancel!
  end

  def cancel_withdrawal_for_trade(trade, reason = nil)
    return false unless trade.is_fiat_token_withdrawal_trade?
    return false if trade.released?

    withdrawal = trade.fiat_token_withdrawal
    return false unless withdrawal&.may_cancel?

    withdrawal.cancel_reason_param = reason if reason.present?
    withdrawal.cancel!

    # Unlock the funds if cancelled
    fiat_account = withdrawal.fiat_account
    fiat_account.unlock_amount!(withdrawal.fiat_amount)
  end

  private

  def create_fiat_deposit_transaction(deposit)
    FiatTransaction.create!(
      fiat_account: deposit.fiat_account,
      transactable: deposit,
      amount: deposit.amount_after_fee,
      original_amount: deposit.fiat_amount,
      currency: deposit.currency,
      transaction_type: 'deposit',
      status: 'completed'
    )

    # Update fiat account balance
    deposit.fiat_account.deposit!(deposit.amount_after_fee)
  end

  def create_fiat_withdrawal_transaction(withdrawal)
    FiatTransaction.create!(
      fiat_account: withdrawal.fiat_account,
      transactable: withdrawal,
      amount: withdrawal.fiat_amount,
      fee: withdrawal.fee,
      currency: withdrawal.currency,
      transaction_type: 'withdrawal',
      status: 'completed'
    )

    # Update fiat account balance - no need to modify balance here
    # as the funds were already locked during withdrawal creation
    withdrawal.fiat_account.withdraw_locked!(withdrawal.fiat_amount)
  end
end
