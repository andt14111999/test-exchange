def setup_amm_pool
  # Add 100k liquidity USDT/NGN from price range 950-1050
# First create AMM pool if not exists

  # Admin-provided tick values
  admin_tick_lower = 138420  # Approximately 950 NGN/USDT
  admin_tick_upper = 138900  # Approximately 1050 NGN/USDT
  admin_init_price = 1_000   # Initial price in NGN

  # Calculate tick values using Uniswap V3 formula: tick = log(price) / log(1.0001)
  price_lower = 950   # NGN per USDT
  price_upper = 1_050 # NGN per USDT

  formula_tick_lower = (Math.log(price_lower) / Math.log(1.0001)).round
  formula_tick_upper = (Math.log(price_upper) / Math.log(1.0001)).round
  formula_init_tick = (Math.log(admin_init_price) / Math.log(1.0001)).round

  puts "=" * 60
  puts "TICK VALUE VALIDATION"
  puts "=" * 60
  puts "Price Range: #{price_lower} - #{price_upper} NGN/USDT"
  puts "Init Price: #{admin_init_price} NGN/USDT"
  puts ""
  puts "LOWER TICK:"
  puts "  Admin provided: #{admin_tick_lower}"
  puts "  Formula result: #{formula_tick_lower}"
  puts "  Match: #{admin_tick_lower == formula_tick_lower ? '✓' : '✗'}"
  puts ""
  puts "UPPER TICK:"
  puts "  Admin provided: #{admin_tick_upper}"
  puts "  Formula result: #{formula_tick_upper}"
  puts "  Match: #{admin_tick_upper == formula_tick_upper ? '✓' : '✗'}"
  puts ""
  puts "INIT PRICE TICK:"
  puts "  Formula result: #{formula_init_tick}"
  puts "=" * 60

  # Panic exit if values don't match
  if admin_tick_lower != formula_tick_lower
    puts "❌ PANIC: Lower tick mismatch!"
    puts "   Expected (formula): #{formula_tick_lower}"
    puts "   Got (admin): #{admin_tick_lower}"
    exit 1
  end

  if admin_tick_upper != formula_tick_upper
    puts "❌ PANIC: Upper tick mismatch!"
    puts "   Expected (formula): #{formula_tick_upper}"
    puts "   Got (admin): #{admin_tick_upper}"
    exit 1
  end

  puts "✅ All tick values validated successfully!"
  puts ""

amm_pool = AmmPool.find_or_create_by!(
    pair: 'usdt_ngn',  # Using underscore instead of slash
  token0: 'USDT',
    token1: 'NGN',
  tick_spacing: 10,
  fee_percentage: 0.003, # 0.3%
  fee_protocol_percentage: 0.0005, # 0.05%
    init_price: admin_init_price # Initial price in NGN
)

# Activate the AMM pool and send update event
amm_pool.send_event_update_amm_pool(
  status: 'active',
  fee_percentage: 0.003,
  fee_protocol_percentage: 0.0005,
    init_price: admin_init_price
)

puts "Created/activated AMM pool: #{amm_pool.pair} (ID: #{amm_pool.id})" 
end

# Execute the method
setup_amm_pool 