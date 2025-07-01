# Convert 100k USDT to NGN using merchant escrow
user = User.find_by!(email: 'mikeng@example.com')
usdt_main_account = user.main_account('usdt')
ngn_account = user.main_account('NGN')

# Create and send merchant escrow event
merchant_escrow = MerchantEscrow.create!(
  user: user,
  usdt_account: usdt_main_account,
  fiat_account: ngn_account,
  usdt_amount: 100_000,
  fiat_amount: 100_000 * 1_000, # Using 1,000 NGN/USDT rate
  fiat_currency: 'NGN',
  exchange_rate: 1_000
)

# Send Kafka event for merchant escrow creation
merchant_escrow.send(:send_kafka_event_create)

puts "Created first merchant escrow: #{merchant_escrow.usdt_amount} USDT -> #{merchant_escrow.fiat_amount} NGN" 