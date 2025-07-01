# Create user mikeng if not exists
user = User.find_or_create_by!(email: 'mikeng@example.com') do |u|
  u.display_name = 'Mike NG'
  u.role = 'merchant' # Must be merchant to use escrow
  u.status = 'active'
end

puts "Created/found Nigerian merchant user: #{user.email} (ID: #{user.id})"
