FactoryBot.define do
  factory :account do
    account_type { 'wallet' }
    coin_currency { 'BTC' }
    layer { 'bitcoin' }
    user
  end
end 