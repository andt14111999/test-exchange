FactoryBot.define do
  factory :coin_deposit do
    user
    coin_account
    coin_currency { 'USDT' }
    coin_amount { 1.5 }
    coin_fee { 0 }
    tx_hash { '0x123' }
    out_index { 0 }
    status { 'pending' }
  end
end 