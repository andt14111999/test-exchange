# Create user mikeng if not exists
# Temporarily patch AccountCreationService to skip Kafka calls

class AccountCreationService
  def notify_coin_kafka_service(account)
    Rails.logger.info "Skipping Kafka notification for coin account #{account.id} in development"
  end

  def notify_fiat_kafka_service(account)
    Rails.logger.info "Skipping Kafka notification for fiat account #{account.id} in development"
  end
end

user = User.find_or_create_by!(email: 'mikeng@example.com') do |u|
  u.display_name = 'Mike NG'
  u.role = 'merchant' # Must be merchant to use escrow
  u.status = 'active'
end

puts "Created/found Nigerian merchant user: #{user.email} (ID: #{user.id})" 