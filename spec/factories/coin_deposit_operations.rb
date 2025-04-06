FactoryBot.define do
  factory :coin_deposit_operation do
    out_index { 0 }
    coin_amount { 1.5 }
    coin_fee { 0.1 }
    platform_fee { 0.05 }
    tx_hash { '0x1234567890abcdef' }
    coin_currency { 'btc' }
    association :coin_account, :main
    association :coin_deposit
  end
end
