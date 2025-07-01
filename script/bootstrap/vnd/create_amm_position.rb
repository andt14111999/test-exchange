# Calculate tick indices for price range 26000-26500
# Using Uniswap V3 tick calculation: tick = log(price) * 2^96 / log(1.0001)
# We'll use a simpler calculation for our range
tick_lower = 101660  # Approximately 26000 VND/USDT
tick_upper = 101850   # Approximately 26500 VND/USDT

user = User.find_by!(email: 'mikevn@example.com')
amm_pool = AmmPool.find_by!(pair: 'usdt_vnd')

# Create AMM position
amm_position = AmmPosition.new(
  user: user,
  amm_pool: amm_pool,
  tick_lower_index: tick_lower,
  tick_upper_index: tick_upper,
  amount0_initial: 100_000, # USDT
  amount1_initial: 2_652_485_667.11, # VND
  slippage: 1 # 1% slippage
)

# Generate identifier before saving
amm_position.generate_identifier
amm_position.save!

puts "Created AMM position: #{amm_position.amount0_initial} USDT / #{amm_position.amount1_initial} VND (ID: #{amm_position.id})" 