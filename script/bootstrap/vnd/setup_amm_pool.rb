# Add 100k liquidity USDT/VND from price range 26000-26500
# First create AMM pool if not exists
amm_pool = AmmPool.find_or_create_by!(
  pair: 'usdt_vnd',  # Using underscore instead of slash
  token0: 'USDT',
  token1: 'VND',
  tick_spacing: 10,
  fee_percentage: 0.003, # 0.3%
  fee_protocol_percentage: 0.0005, # 0.05%
  init_price: 26_100 # Initial price in VND
)

# Activate the AMM pool and send update event
amm_pool.send_event_update_amm_pool(
  status: 'active',
  fee_percentage: 0.003,
  fee_protocol_percentage: 0.0005,
  init_price: 26_100
)

puts "Created/activated AMM pool: #{amm_pool.pair} (ID: #{amm_pool.id})" 