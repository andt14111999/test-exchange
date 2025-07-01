# Create NGN deposit offer (buy NGN)
user = User.find_by!(email: 'mikeng@example.com')

buy_offer = Offer.create!(
  user: user,
  offer_type: 'buy', # Buy NGN (deposit NGN)
  coin_currency: 'ngn',
  currency: 'NGN',
  price: 1, # 1 NGN = 1 NGN (no conversion needed)
  min_amount: 5_000,
  max_amount: 10_000_000,
  total_amount: 200_000_000, # 200M NGN total
  payment_time: 15, # 15 minutes
  country_code: 'NG',
  automatic: true,
  online: true,
  fixed_coin_price: 5_000, # Fixed fee: 5,000 NGN
  margin: 0, # 0% margin
  terms_of_trade: 'Fixed fee: 5,000 NGN per transaction'
)

# Create NGN withdrawal offer (sell NGN)
sell_offer = Offer.create!(
  user: user,
  offer_type: 'sell', # Sell NGN (withdraw NGN)
  coin_currency: 'ngn',
  currency: 'NGN',
  price: 1, # 1 NGN = 1 NGN (no conversion needed)
  min_amount: 5_000,
  max_amount: 10_000_000,
  total_amount: 200_000_000, # 200M NGN total
  payment_time: 15, # 15 minutes
  payment_details: {
    bank_name: 'Access Bank',
    account_name: 'Mike NG',
    account_number: '0987654321',
    branch: 'Lagos'
  },
  country_code: 'NG',
  automatic: true,
  online: true,
  fixed_coin_price: 5_000, # Fixed fee: 5,000 NGN
  margin: 0, # 0% margin
  terms_of_trade: 'Fixed fee: 5,000 NGN per transaction'
)

puts "Created NGN buy offer (ID: #{buy_offer.id}) and sell offer (ID: #{sell_offer.id})" 