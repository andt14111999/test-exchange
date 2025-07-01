def create_amm_position
  # Calculate tick indices for price range 26000-26500
  # Using Uniswap V3 tick calculation: tick = log(price) * 2^96 / log(1.0001)
  # We'll use the actual Uniswap V3 formula: tick = log(price) / log(1.0001)

  # Admin-provided tick values
  admin_tick_lower = 101664  # Approximately 26000 VND/USDT
  admin_tick_upper = 101854  # Approximately 26500 VND/USDT

  # Calculate tick values using Uniswap V3 formula: tick = log(price) / log(1.0001)
  price_lower = 26_000  # VND per USDT
  price_upper = 26_500  # VND per USDT

  formula_tick_lower = (Math.log(price_lower) / Math.log(1.0001)).round
  formula_tick_upper = (Math.log(price_upper) / Math.log(1.0001)).round

  puts "=" * 60
  puts "AMM POSITION TICK VALIDATION"
  puts "=" * 60
  puts "Price Range: #{price_lower} - #{price_upper} VND/USDT"
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
  puts "=" * 60

  # Panic exit if values don't match
  if admin_tick_lower != formula_tick_lower
    puts "❌ PANIC: Lower tick mismatch for AMM position!"
    puts "   Expected (formula): #{formula_tick_lower}"
    puts "   Got (admin): #{admin_tick_lower}"
    exit 1
  end

  if admin_tick_upper != formula_tick_upper
    puts "❌ PANIC: Upper tick mismatch for AMM position!"
    puts "   Expected (formula): #{formula_tick_upper}"
    puts "   Got (admin): #{admin_tick_upper}"
    exit 1
  end

  puts "✅ AMM position tick values validated successfully!"
  puts ""

user = User.find_by!(email: 'mikevn@example.com')
amm_pool = AmmPool.find_by!(pair: 'usdt_vnd')

# Create AMM position
amm_position = AmmPosition.new(
  user: user,
  amm_pool: amm_pool,
    tick_lower_index: admin_tick_lower,
    tick_upper_index: admin_tick_upper,
  amount0_initial: 100_000, # USDT
  amount1_initial: 2_652_485_667.11, # VND
  slippage: 1 # 1% slippage
)

# Generate identifier before saving
amm_position.generate_identifier
amm_position.save!

puts "Created AMM position: #{amm_position.amount0_initial} USDT / #{amm_position.amount1_initial} VND (ID: #{amm_position.id})"
end

# Execute the method
create_amm_position
