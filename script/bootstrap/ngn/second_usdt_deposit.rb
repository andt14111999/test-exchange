# Second deposit 200k USDT
user = User.find_by!(email: 'mikeng@example.com')

usdt_deposit_account = user.coin_accounts.of_coin('usdt').find_by(layer: 'erc20', account_type: 'deposit')

coin_deposit = CoinDeposit.create!(
  user: user,
  coin_account: usdt_deposit_account,
  coin_currency: 'usdt',
  coin_amount: 200_000,
  tx_hash: "fake_tx_hash_#{SecureRandom.hex(16)}",
  out_index: 0,
  confirmations_count: 1,
  required_confirmations_count: 1
)

# Create deposit operation
CoinDepositOperation.create!(
  coin_deposit: coin_deposit,
  coin_account: usdt_deposit_account,
  coin_currency: 'usdt',
  coin_amount: 200_000,
  tx_hash: coin_deposit.tx_hash,
  out_index: 0
)

puts "Created second USDT deposit: #{coin_deposit.coin_amount} USDT (TX: #{coin_deposit.tx_hash})" 