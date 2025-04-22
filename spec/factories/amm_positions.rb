# frozen_string_literal: true

FactoryBot.define do
  factory :amm_position do
    user
    amm_pool
    sequence(:identifier) { |n| "amm_position_#{n}" }
    status { 'pending' }
    liquidity { 0 }
    slippage { 1.0 }
    tick_lower_index { -100 }
    tick_upper_index { 100 }
    amount0 { 0 }
    amount1 { 0 }
    amount0_initial { 0 }
    amount1_initial { 0 }
    fee_growth_inside0_last { 0 }
    fee_growth_inside1_last { 0 }
    tokens_owed0 { 0 }
    tokens_owed1 { 0 }
    fee_collected0 { 0 }
    fee_collected1 { 0 }
  end
end
