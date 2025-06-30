FactoryBot.define do
  factory :country do
    sequence(:name) { |n| "Country #{n}" }
    sequence(:code) { |n| "C#{n.to_s.rjust(1, '0')}" }

    trait :vietnam do
      name { "Vietnam" }
      code { "VN" }
    end

    trait :nigeria do
      name { "Nigeria" }
      code { "NG" }
    end

    trait :ghana do
      name { "Ghana" }
      code { "GH" }
    end
  end
end
