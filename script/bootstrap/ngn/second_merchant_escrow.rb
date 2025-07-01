# Convert 200k USDT to NGN using merchant escrow
user = User.find_by!(email: 'mikeng@example.com')
usdt_main_account = user.main_account('usdt')
ngn_account = user.main_account('NGN')

merchant_escrow = MerchantEscrow.create!(
  user: user,
  usdt_account: usdt_main_account,
  fiat_account: ngn_account,
  usdt_amount: 200_000,
  fiat_amount: 200_000 * 1_000,
  fiat_currency: 'NGN',
  exchange_rate: 1_000
)

# Send Kafka event for merchant escrow creation
merchant_escrow.send(:send_kafka_event_create)

puts "Created second merchant escrow: #{merchant_escrow.usdt_amount} USDT -> #{merchant_escrow.fiat_amount} NGN"
