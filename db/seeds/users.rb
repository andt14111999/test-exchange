# frozen_string_literal: true

puts 'Creating test users...'

# Temporarily skip callbacks to avoid Kafka dependency during seeding
User.skip_callback(:create, :after, :create_default_accounts)

begin
  # User 1: Regular verified user
  user1 = User.find_or_create_by!(email: 'john.doe@example.com') do |u|
    u.display_name = 'John Doe'
    u.username = 'johndoe'
    u.role = 'user'
    u.status = 'active'
    u.kyc_level = 1
    u.phone_verified = true
    u.document_verified = false
    u.avatar_url = 'https://via.placeholder.com/150/0000FF/FFFFFF?text=JD'
  end

  # User 2: Merchant user with high KYC (also a Snowfox employee)
  user2 = User.find_or_create_by!(email: 'jane.smith@example.com') do |u|
    u.display_name = 'Jane Smith'
    u.username = 'janesmith'
    u.role = 'merchant'
    u.status = 'active'
    u.kyc_level = 2
    u.phone_verified = true
    u.document_verified = true
    u.snowfox_employee = true
    u.avatar_url = 'https://via.placeholder.com/150/FF0000/FFFFFF?text=JS'
  end

  # User 3: Basic user with minimal verification
  user3 = User.find_or_create_by!(email: 'bob.wilson@example.com') do |u|
    u.display_name = 'Bob Wilson'
    u.username = 'bobwilson'
    u.role = 'user'
    u.status = 'active'
    u.kyc_level = 0
    u.phone_verified = false
    u.document_verified = false
    u.avatar_url = 'https://via.placeholder.com/150/00FF00/FFFFFF?text=BW'
  end
ensure
  # Re-enable callbacks
  User.set_callback(:create, :after, :create_default_accounts)
end

puts "Created users:"
puts "- #{user1.display_name} (#{user1.email}) - Role: #{user1.role}, KYC: #{user1.kyc_level}"
puts "- #{user2.display_name} (#{user2.email}) - Role: #{user2.role}, KYC: #{user2.kyc_level}"
puts "- #{user3.display_name} (#{user3.email}) - Role: #{user3.role}, KYC: #{user3.kyc_level}"

puts 'Test users created successfully!'
