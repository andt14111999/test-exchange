def create_amm_position
  # Calculate tick indices for price range 950-1050
  # Using Uniswap V3 tick calculation: tick = log(price) * 2^96 / log(1.0001)
  # We'll use the actual Uniswap V3 formula: tick = log(price) / log(1.0001)

  # Admin-provided tick values
  admin_tick_lower = 68568   # Approximately 950 NGN/USDT
  admin_tick_upper = 69569   # Approximately 1050 NGN/USDT

  # Calculate tick values using Uniswap V3 formula: tick = log(price) / log(1.0001)
  price_lower = 950   # NGN per USDT
  price_upper = 1_050 # NGN per USDT

  formula_tick_lower = (Math.log(price_lower) / Math.log(1.0001)).round
  formula_tick_upper = (Math.log(price_upper) / Math.log(1.0001)).round

  puts "=" * 60
  puts "AMM POSITION TICK VALIDATION"
  puts "=" * 60
  puts "Price Range: #{price_lower} - #{price_upper} NGN/USDT"
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

  user = User.find_by!(email: 'mikeng@example.com')
  amm_pool = AmmPool.find_by!(pair: 'usdt_ngn')

# Create AMM position
amm_position = AmmPosition.new(
  user: user,
  amm_pool: amm_pool,
    tick_lower_index: admin_tick_lower,
    tick_upper_index: admin_tick_upper,
  amount0_initial: 100_000, # USDT
    amount1_initial: 100_000_000, # NGN (100M NGN for 100k USDT)
  slippage: 1 # 1% slippage
)

# Generate identifier before saving
amm_position.generate_identifier
amm_position.save!

  puts "Created AMM position: #{amm_position.amount0_initial} USDT / #{amm_position.amount1_initial} NGN (ID: #{amm_position.id})"
end

# Execute the method
create_amm_position
