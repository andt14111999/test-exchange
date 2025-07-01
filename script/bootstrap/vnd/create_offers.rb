# Create VND deposit offer (buy VND)
user = User.find_by!(email: 'mikevn@example.com')

buy_offer = Offer.create!(
  user: user,
  offer_type: 'buy', # Buy VND (deposit VND)
  coin_currency: 'vnd',
  currency: 'VND',
  price: 1, # 1 VND = 1 VND (no conversion needed)
  min_amount: 100_000,
  max_amount: 300_000_000,
  total_amount: 5_000_000_000, # 5B VND total
  payment_time: 15, # 15 minutes
  country_code: 'VN',
  automatic: true,
  online: true,
  fixed_coin_price: 100_000, # Fixed fee: 100,000 VND
  margin: 0, # 0% margin
  terms_of_trade: 'Fixed fee: 100,000 VND per transaction'
)

# Create VND withdrawal offer (sell VND)
sell_offer = Offer.create!(
  user: user,
  offer_type: 'sell', # Sell VND (withdraw VND)
  coin_currency: 'vnd',
  currency: 'VND',
  price: 1, # 1 VND = 1 VND (no conversion needed)
  min_amount: 100_000,
  max_amount: 300_000_000,
  total_amount: 5_000_000_000, # 5B VND total
  payment_time: 15, # 15 minutes
  payment_details: {
    bank_name: 'Vietcombank',
    account_name: 'Mike VN',
    account_number: '1234567890',
    branch: 'Ho Chi Minh City'
  },
  country_code: 'VN',
  automatic: true,
  online: true,
  fixed_coin_price: 100_000, # Fixed fee: 100,000 VND
  margin: 0, # 0% margin
  terms_of_trade: 'Fixed fee: 100,000 VND per transaction'
)

puts "Created VND buy offer (ID: #{buy_offer.id}) and sell offer (ID: #{sell_offer.id})" 