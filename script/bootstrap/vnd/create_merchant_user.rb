# Create user mikevn if not exists
user = User.find_or_create_by!(email: 'mikevn@example.com') do |u|
  u.display_name = 'Mike VN'
  u.role = 'merchant' # Must be merchant to use escrow
  u.status = 'active'
end

puts "Created/found merchant user: #{user.email} (ID: #{user.id})" 