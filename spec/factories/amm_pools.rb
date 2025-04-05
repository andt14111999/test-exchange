FactoryBot.define do
  factory :amm_pool do
    sequence(:pair) { |n| "USDT/VND_#{n}" }
    sequence(:token0) { |n| "USDT_#{n}" }
    sequence(:token1) { |n| "VND_#{n}" }
    tick_spacing { 10 }
    fee_percentage { 0.003 }
    fee_protocol_percentage { 0.05 }
    current_tick { 0 }
    sqrt_price { BigDecimal('1.0') }
    price { BigDecimal('1.0') }
    liquidity { BigDecimal('0') }
    fee_growth_global0 { BigDecimal('0') }
    fee_growth_global1 { BigDecimal('0') }
    protocol_fees0 { BigDecimal('0') }
    protocol_fees1 { BigDecimal('0') }
    volume_token0 { BigDecimal('0') }
    volume_token1 { BigDecimal('0') }
    volume_usd { BigDecimal('0') }
    tx_count { 0 }
    total_value_locked_token0 { BigDecimal('0') }
    total_value_locked_token1 { BigDecimal('0') }
    status { 'pending' }
    status_explanation { '' }
  end
end
