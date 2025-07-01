# Convert 200k USDT to VND using merchant escrow
user = User.find_by!(email: 'mikevn@example.com')
usdt_main_account = user.main_account('usdt')
vnd_account = user.main_account('VND')

merchant_escrow = MerchantEscrow.create!(
  user: user,
  usdt_account: usdt_main_account,
  fiat_account: vnd_account,
  usdt_amount: 200_000,
  fiat_amount: 200_000 * 27_000,
  fiat_currency: 'VND',
  exchange_rate: 27_000
)

# Send Kafka event for merchant escrow creation
merchant_escrow.send(:send_kafka_event_create)

puts "Created second merchant escrow: #{merchant_escrow.usdt_amount} USDT -> #{merchant_escrow.fiat_amount} VND"
