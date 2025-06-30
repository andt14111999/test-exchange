# Create countries
countries_data = [
  { name: "Vietnam", code: "VN" },
  { name: "Nigeria", code: "NG" },
  { name: "Ghana", code: "GH" }
]

countries_data.each do |country_data|
  Country.find_or_create_by!(code: country_data[:code]) do |country|
    country.name = country_data[:name]
  end
end

# Create banks from JSON data
banks_json = JSON.parse(File.read(Rails.root.join('data', 'banks.json')))
vietnam = Country.find_by(code: 'VN')

banks_json['data'].each do |bank_data|
  Bank.find_or_create_by!(code: bank_data['code']) do |bank|
    bank.name = bank_data['name']
    bank.bin = bank_data['bin']
    bank.short_name = bank_data['short_name']
    bank.logo = bank_data['logo']
    bank.transfer_supported = bank_data['transferSupported'] == 1
    bank.lookup_supported = bank_data['lookupSupported'] == 1
    bank.support = bank_data['support']
    bank.is_transfer = bank_data['isTransfer'] == 1
    bank.swift_code = bank_data['swift_code']
    bank.country = vietnam # All banks from JSON are Vietnamese banks
  end
end

puts "Seeded #{Country.count} countries and #{Bank.count} banks"
