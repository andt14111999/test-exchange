FactoryBot.define do
  factory :bank do
    association :country
    sequence(:name) { |n| "Bank #{n}" }
    sequence(:code) { |n| "B#{n.to_s.rjust(2, '0')}" }
    sequence(:bin) { |n| "BIN#{n.to_s.rjust(3, '0')}" }
    short_name { "ShortBank" }
    logo { "https://example.com/logo.png" }
    transfer_supported { true }
    lookup_supported { true }
    support { 3 }
    is_transfer { true }
    swift_code { "SWIFT123" }
  end
end
