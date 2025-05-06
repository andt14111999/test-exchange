# frozen_string_literal: true

# Create default payment methods
puts 'Creating default payment methods...'

# Local Bank Transfer (Generic)
PaymentMethod.find_or_create_by!(name: 'local_bank') do |pm|
  pm.display_name = 'Local Bank Transfer'
  pm.description = 'Transfer money to a local bank account'
  pm.country_code = 'ALL'
  pm.enabled = true
  pm.fields_required = {
    bank_name: true,
    bank_account_name: true,
    bank_account_number: true,
    branch: false
  }
end

puts 'Payment methods created!'
