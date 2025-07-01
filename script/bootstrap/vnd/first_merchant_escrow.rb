# Convert 100k USDT to VND using merchant escrow
user = User.find_by!(email: 'mikevn@example.com')
usdt_main_account = user.main_account('usdt')
vnd_account = user.main_account('VND')

# Create and send merchant escrow event
merchant_escrow = MerchantEscrow.create!(
  user: user,
  usdt_account: usdt_main_account,
  fiat_account: vnd_account,
  usdt_amount: 100_000,
  fiat_amount: 100_000 * 27_000, # Using 27,000 VND/USDT rate
  fiat_currency: 'VND',
  exchange_rate: 27_000
)

# Send Kafka event for merchant escrow creation
merchant_escrow.send(:send_kafka_event_create)

puts "Created first merchant escrow: #{merchant_escrow.usdt_amount} USDT -> #{merchant_escrow.fiat_amount} VND"
