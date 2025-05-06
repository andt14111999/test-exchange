# frozen_string_literal: true

# Use Rails.application.config.to_prepare to ensure models are loaded
Rails.application.config.to_prepare do
  # The TRANSACTION_TYPES constant is already defined in the FiatTransaction model
  # No need to redefine it here
end

# Default withdrawal fees by currency
Rails.application.config.withdrawal_fees = {
  'VND' => 0.01, # 1%
  'PHP' => 0.01, # 1%
  'NGN' => 0.01  # 1%
}.freeze

# Default deposit fees by currency
Rails.application.config.deposit_fees = {
  'VND' => 0.01, # 1%
  'PHP' => 0.01, # 1%
  'NGN' => 0.01  # 1%
}.freeze

# Minimum withdrawal amounts
Rails.application.config.min_withdrawal_amounts = {
  'VND' => 100_000,
  'PHP' => 500,
  'NGN' => 5_000
}.freeze

# Maximum withdrawal amounts
Rails.application.config.max_withdrawal_amounts = {
  'VND' => 100_000_000,
  'PHP' => 500_000,
  'NGN' => 5_000_000
}.freeze

# Daily withdrawal limits
Rails.application.config.withdrawal_daily_limits = {
  'VND' => 200_000_000,
  'PHP' => 1_000_000,
  'NGN' => 10_000_000
}.freeze

# Weekly withdrawal limits
Rails.application.config.withdrawal_weekly_limits = {
  'VND' => 1_000_000_000,
  'PHP' => 5_000_000,
  'NGN' => 50_000_000
}.freeze

# Default trade fee
Rails.application.config.default_trade_fee_ratio = 0.005 # 0.5%

# Timeout settings (in hours)
Rails.application.config.timeouts = {
  'deposit_pending' => 168, # 7 days
  'trade_payment' => 24,
  'trade_dispute' => 72,
  'withdrawal_processing' => 48
}.freeze

# P2P / Fiat Token specific settings
Rails.application.config.fiat_token = {
  'banker_pickup_timeout' => 15, # minutes
  'required_confirmations' => 1,
  'automatic_release_timeout' => 24 # hours
}.freeze
