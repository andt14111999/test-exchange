# frozen_string_literal: true

class MerchantEscrowService
  attr_reader :user, :params

  def initialize(user, params = {})
    @user = user
    @params = params
  end

  # Public Methods

  def create
    validate_merchant!
    validate_accounts!

    escrow = build_escrow
    execute_escrow_transaction(escrow) do
      create_freeze_and_mint_operations(escrow)
      escrow.activate!
    end

    escrow
  end

  def cancel(escrow)
    validate_merchant!
    validate_cancelable_escrow!(escrow)

    execute_escrow_transaction(escrow) do
      create_unfreeze_and_burn_operations(escrow)
      escrow.cancel!
    end

    escrow
  end

  private

  # Validations

  def validate_merchant!
    raise 'User is not a merchant' unless @user.merchant?
  end

  def validate_cancelable_escrow!(escrow)
    raise 'Escrow not found' unless escrow
    raise 'Cannot cancel this escrow' unless escrow.can_cancel?
  end

  def validate_accounts!
    @usdt_account = find_usdt_account
    @fiat_account = find_fiat_account
  end

  def find_usdt_account
    account = @user.coin_accounts.of_coin('usdt').main
    raise 'USDT account not found' unless account
    account
  end

  def find_fiat_account
    account = @user.fiat_accounts.of_currency(@params[:fiat_currency]).first
    raise "Fiat account with currency #{@params[:fiat_currency]} not found" unless account
    account
  end

  # Transaction Handling

  def execute_escrow_transaction(escrow)
    ActiveRecord::Base.transaction do
      if escrow.new_record? ? escrow.save : true
        yield
      end
    rescue StandardError => e
      log_error('merchant escrow', e)
      raise
    end
  end

  def log_error(context, error)
    Rails.logger.error "Failed to process #{context}: #{error.message}"
  end

  # Operations

  def create_freeze_and_mint_operations(escrow)
    create_escrow_operation(escrow, 'freeze')
    create_escrow_operation(escrow, 'mint')
  end

  def create_unfreeze_and_burn_operations(escrow)
    create_escrow_operation(escrow, 'unfreeze')
    create_escrow_operation(escrow, 'burn')
  end

  # Escrow Building

  def build_escrow
    rate = calculate_exchange_rate
    fiat_amount = calculate_fiat_amount(@params[:usdt_amount], @params[:fiat_currency])

    build_escrow_record(@usdt_account, @fiat_account, rate, fiat_amount)
  end

  def calculate_exchange_rate
    Setting.get_exchange_rate('usdt', @params[:fiat_currency])
  end

  def build_escrow_record(usdt_account, fiat_account, rate, fiat_amount)
    MerchantEscrow.new(
      user: @user,
      usdt_account: usdt_account,
      fiat_account: fiat_account,
      usdt_amount: @params[:usdt_amount],
      fiat_currency: @params[:fiat_currency],
      fiat_amount: fiat_amount,
      exchange_rate: rate
    )
  end

  def calculate_fiat_amount(usdt_amount, fiat_currency)
    rate = Setting.get_exchange_rate('usdt', fiat_currency)
    usdt_amount * rate
  end

  def create_escrow_operation(escrow, operation_type)
    operation_attributes = {
      merchant_escrow: escrow,
      usdt_account_id: escrow.usdt_account_id,
      fiat_account_id: escrow.fiat_account_id,
      operation_type: operation_type,
      usdt_amount: escrow.usdt_amount,
      fiat_amount: escrow.fiat_amount,
      fiat_currency: escrow.fiat_currency,
      status: 'pending'
    }

    MerchantEscrowOperation.create!(operation_attributes)
  end
end
