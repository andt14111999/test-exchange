#!/usr/bin/env ruby
# frozen_string_literal: true

# Simple GitHub Secrets Setup using GitHub CLI
# Requires: brew install gh (or equivalent)

require 'io/console'

class SimpleGitHubSecretsManager
  def initialize
    check_gh_cli
    check_authentication
  end

  def setup_all_secrets
    puts "🚀 Setting up GitHub Secrets for Exchange CI/CD"
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

    puts "\n🔧 Setting secrets..."

    secrets.each do |name, value|
      if set_secret(name, value)
        puts "✅ Successfully set secret: #{name}"
      else
        puts "❌ Failed to set secret: #{name}"
      end
    end

    puts "\n🎉 GitHub Secrets setup complete!"
    puts "\n📋 Next steps:"
    puts "1. Verify secrets: gh secret list"
    puts "2. Update domain URLs in deploy-production.yml if needed"
    puts "3. Run the CI/CD pipeline!"
  end

  private

  def check_gh_cli
    unless system('which gh > /dev/null 2>&1')
      puts "❌ GitHub CLI not found!"
      puts "\n📋 Install GitHub CLI:"
      puts "macOS: brew install gh"
      puts "Ubuntu: sudo apt install gh"
      puts "Windows: winget install GitHub.cli"
      puts "\nOr visit: https://cli.github.com/"
      exit 1
    end
  end

  def check_authentication
    unless system('gh auth status > /dev/null 2>&1')
      puts "❌ Not authenticated with GitHub CLI!"
      puts "\n🔑 Run: gh auth login"
      puts "Select: Login with a web browser"
      exit 1
    end
  end

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
      'Used for Rails asset compilation (from config/master.key or config/credentials/production.key)'
    )

    # Google OAuth
    secrets['GOOGLE_CLIENT_ID'] = get_secret_value(
      'GOOGLE_CLIENT_ID',
      'Google OAuth Client ID',
      'Used for Google authentication in frontend'
    )

    # Rollbar
    secrets['ROLLBAR_ACCESS_TOKEN'] = get_secret_value(
      'ROLLBAR_ACCESS_TOKEN',
      'Rollbar Access Token',
      'Used for error tracking and deployment notifications'
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

    if env_var.include?('SECRET') || env_var.include?('KEY') || env_var.include?('TOKEN')
      value = STDIN.noecho(&:gets)&.chomp
      puts "   🔒 [HIDDEN]"
    else
      value = gets&.chomp
    end

    value
  end

  def set_secret(name, value)
    # Use GitHub CLI to set the secret with proper input handling
    IO.popen([ 'gh', 'secret', 'set', name ], 'w') do |io|
      io.write(value)
    end
    $?.success?
  rescue => e
    puts "❌ Error setting secret #{name}: #{e.message}"
    false
  end
end

def show_help
  puts <<~HELP
    🔧 GitHub Secrets Setup for Exchange CI/CD
    ========================================

    This script sets up all required GitHub secrets for your CI/CD pipeline.

    📋 Prerequisites:
    1. Install GitHub CLI: brew install gh
    2. Authenticate: gh auth login
    3. Have your secret values ready

    🚀 Usage:

    # Option 1: Interactive setup
    ruby script/setup_github_secrets_simple.rb

    # Option 2: Environment variables
    export AWS_ACCESS_KEY_ID="your-key"
    export AWS_SECRET_ACCESS_KEY="your-secret"
    export RAILS_MASTER_KEY="your-rails-key"
    export GOOGLE_CLIENT_ID="your-google-id"
    export ROLLBAR_ACCESS_TOKEN="your-rollbar-token"
    ruby script/setup_github_secrets_simple.rb

    # Option 3: One-liner with secrets
    AWS_ACCESS_KEY_ID=xxx AWS_SECRET_ACCESS_KEY=yyy ruby script/setup_github_secrets_simple.rb

    📝 Required Secrets:
    - AWS_ACCESS_KEY_ID: AWS access key for ECS deployment
    - AWS_SECRET_ACCESS_KEY: AWS secret key for ECS deployment
    - RAILS_MASTER_KEY: Rails master key for asset compilation
    - GOOGLE_CLIENT_ID: Google OAuth client ID for frontend
    - ROLLBAR_ACCESS_TOKEN: Rollbar token for error tracking
  HELP
end

def main
  if ARGV.include?('--help') || ARGV.include?('-h')
    show_help
    return
  end

  manager = SimpleGitHubSecretsManager.new
  manager.setup_all_secrets
end

if __FILE__ == $0
  main
end
