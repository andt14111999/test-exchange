# frozen_string_literal: true

puts 'Seeding coin settings...'

# Seed coin settings based on supported networks
CoinAccount::SUPPORTED_NETWORKS.each do |currency, layers|
  CoinSetting.find_or_create_by!(currency: currency) do |setting|
    setting.deposit_enabled = true
    setting.withdraw_enabled = true
    setting.swap_enabled = true
    setting.layers = layers.map do |layer|
      {
        'layer' => layer,
        'deposit_enabled' => true,
        'withdraw_enabled' => true,
        'swap_enabled' => true,
        'maintenance' => false
      }
    end
  end
  puts "Created/updated coin setting for #{currency}"
end

puts "Seeded #{CoinSetting.count} coin settings"
