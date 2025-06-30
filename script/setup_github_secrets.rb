#!/usr/bin/env ruby
# frozen_string_literal: true

require 'net/http'
require 'json'
require 'base64'
require 'openssl'
require 'io/console'

begin
  require 'rbnacl'
rescue LoadError
  puts "❌ Missing required gem: rbnacl"
  puts "📦 Install it with: gem install rbnacl"
  puts "   Or add to Gemfile: gem 'rbnacl'"
  exit 1
end

class GitHubSecretsManager
  GITHUB_API_BASE = 'https://api.github.com'

  def initialize(owner, repo, token)
    @owner = owner
    @repo = repo
    @token = token
    @http = Net::HTTP.new('api.github.com', 443)
    @http.use_ssl = true
  end

  def setup_all_secrets
    puts "🚀 Setting up GitHub Secrets for #{@owner}/#{@repo}"
    puts "=" * 60

    secrets = collect_secrets

    if secrets.empty?
      puts "❌ No secrets to set up. Exiting..."
      return
    end

    puts "\n📝 Summary of secrets to be set:"
    secrets.each { |name, _| puts "  - #{name}" }

    print "\n❓ Proceed with setting these secrets? (y/N): "
    return unless gets.chomp.downcase.start_with?('y')

    public_key = get_repository_public_key

    secrets.each do |name, value|
      if set_secret(name, value, public_key)
        puts "✅ Successfully set secret: #{name}"
      else
        puts "❌ Failed to set secret: #{name}"
      end
    end

    puts "\n🎉 GitHub Secrets setup complete!"
    puts "\n📋 Next steps:"
    puts "1. Verify secrets in GitHub: https://github.com/#{@owner}/#{@repo}/settings/secrets/actions"
    puts "2. Update domain URLs in deploy-production.yml if needed"
    puts "3. Run the CI/CD pipeline!"
  end

  private

  def collect_secrets
    secrets = {}

    puts "\n🔐 Enter your secrets (press Enter to skip):"
    puts "💡 Tip: You can also set environment variables before running this script"

    # AWS Configuration
    secrets['AWS_ACCESS_KEY_ID'] = get_secret_value(
      'AWS_ACCESS_KEY_ID',
      'AWS Access Key ID',
      'Used for AWS ECS deployment'
    )

    secrets['AWS_SECRET_ACCESS_KEY'] = get_secret_value(
      'AWS_SECRET_ACCESS_KEY',
      'AWS Secret Access Key',
      'Used for AWS ECS deployment'
    )

    # Rails Configuration
    secrets['RAILS_MASTER_KEY'] = get_secret_value(
      'RAILS_MASTER_KEY',
      'Rails Master Key',
      'Used for Rails asset compilation (from config/master.key)'
    )

    # Google OAuth
    secrets['GOOGLE_CLIENT_ID'] = get_secret_value(
      'GOOGLE_CLIENT_ID',
      'Google OAuth Client ID',
      'Used for Google authentication in frontend'
    )

    # Rollbar - Server Side
    secrets['ROLLBAR_SERVER_ACCESS_TOKEN'] = get_secret_value(
      'ROLLBAR_SERVER_ACCESS_TOKEN',
      'Rollbar Server Access Token',
      'Used for backend error tracking and deployment notifications (server-side token)'
    )

    # Rollbar - Client Side
    secrets['ROLLBAR_CLIENT_ACCESS_TOKEN'] = get_secret_value(
      'ROLLBAR_CLIENT_ACCESS_TOKEN',
      'Rollbar Client Access Token',
      'Used for frontend error tracking (client-side/public token)'
    )

    # Remove empty values
    secrets.reject { |_, value| value.nil? || value.strip.empty? }
  end

  def get_secret_value(env_var, display_name, description)
    # Try environment variable first
    value = ENV[env_var]
    return value if value && !value.strip.empty?

    # Prompt user
    puts "\n📋 #{display_name}"
    puts "   📝 #{description}"
    print "   🔑 Enter value (or press Enter to skip): "

    if env_var.include?('SECRET') || env_var.include?('KEY') || env_var.include?('TOKEN') || env_var.include?('CLIENT_ID')
      value = STDIN.noecho(&:gets)&.chomp
      puts "   🔒 [HIDDEN]"
    else
      value = gets&.chomp
    end

    value
  end

  def get_repository_public_key
    uri = URI("#{GITHUB_API_BASE}/repos/#{@owner}/#{@repo}/actions/secrets/public-key")
    request = Net::HTTP::Get.new(uri)
    request['Authorization'] = "token #{@token}"
    request['Accept'] = 'application/vnd.github.v3+json'

    response = @http.request(request)

    unless response.code == '200'
      puts "❌ Failed to get repository public key: #{response.code} #{response.message}"
      puts "Response body: #{response.body}" if response.body
      exit 1
    end

    JSON.parse(response.body)
  end

  def encrypt_secret(value, public_key)
    # Decode the public key from base64
    public_key_bytes = Base64.decode64(public_key['key'])

    # Create RbNaCl public key object
    box_public_key = RbNaCl::PublicKey.new(public_key_bytes)

    # Create a box for encryption
    box = RbNaCl::Boxes::Sealed.new(box_public_key)

    # Encrypt the secret value
    encrypted_bytes = box.encrypt(value)

    # Encode the encrypted bytes to base64
    encrypted_value = Base64.strict_encode64(encrypted_bytes)

    {
      encrypted_value: encrypted_value,
      key_id: public_key['key_id']
    }
  rescue => e
    puts "❌ Encryption error: #{e.message}"
    raise
  end

  def set_secret(name, value, public_key)
    uri = URI("#{GITHUB_API_BASE}/repos/#{@owner}/#{@repo}/actions/secrets/#{name}")
    request = Net::HTTP::Put.new(uri)
    request['Authorization'] = "token #{@token}"
    request['Accept'] = 'application/vnd.github.v3+json'
    request['Content-Type'] = 'application/json'

    # Encrypt the secret using proper libsodium encryption
    encrypted_data = encrypt_secret(value, public_key)

    request.body = JSON.generate(encrypted_data)

    response = @http.request(request)

    unless response.code == '201' || response.code == '204'
      puts "❌ Failed to set secret #{name}: #{response.code} #{response.message}"
      puts "Response body: #{response.body}" if response.body
      return false
    end

    true
  rescue => e
    puts "❌ Error setting secret #{name}: #{e.message}"
    false
  end
end

# Script configuration
DEFAULT_OWNER = 'andt14111999'  # Update this to your GitHub username
DEFAULT_REPO = 'test-exchange'

def main
  puts "🔧 GitHub Secrets Setup for Exchange CI/CD"
  puts "=" * 50

  # Get GitHub details
  owner = ENV['GITHUB_OWNER'] || DEFAULT_OWNER
  repo = ENV['GITHUB_REPO'] || DEFAULT_REPO
  token = ENV['GITHUB_TOKEN']

  if token.nil? || token.strip.empty?
    puts "\n❌ GitHub Personal Access Token required!"
    puts "\n📋 Setup instructions:"
    puts "1. Go to: https://github.com/settings/tokens"
    puts "2. Generate new token (classic)"
    puts "3. Select scopes: 'repo' and 'admin:repo_hooks'"
    puts "4. Set environment variable: export GITHUB_TOKEN=your_token"
    puts "\nOr run: GITHUB_TOKEN=your_token ruby script/setup_github_secrets.rb"
    exit 1
  end

  puts "\n📂 Repository: #{owner}/#{repo}"
  puts "🔑 Token: #{token[0..3]}***#{token[-4..-1]}" if token.length > 8

  manager = GitHubSecretsManager.new(owner, repo, token)
  manager.setup_all_secrets
end

if __FILE__ == $0
  main
end
