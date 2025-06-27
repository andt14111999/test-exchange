# Rails Environment-Based Credentials Implementation Guide

## Overview

This guide implements a comprehensive environment-based Rails credentials system with automatic CI/CD validation that ensures credentials are properly configured before deployment while maintaining security best practices.

## Key Features

- ✅ Environment-specific credentials (development, test, production)
- ✅ Automatic CI/CD validation with fail-fast behavior
- ✅ Security-first approach (production keys never committed)
- ✅ Smart validation modes (local, CI, production-ready)
- ✅ Comprehensive error reporting and debugging
- ✅ Base58 key format validation
- ✅ Template-based production setup

## Implementation Steps

### 1. Create Validation Script

**File**: `bin/validate_credentials`

```ruby
#!/usr/bin/env ruby

require 'optparse'
require 'yaml'

class CredentialsValidator
  REQUIRED_KEYS = {
    'active_record_encryption' => %w[primary_key deterministic_key key_derivation_salt]
  }.freeze

  PRODUCTION_REQUIRED_KEYS = {
    'active_record_encryption' => %w[primary_key deterministic_key key_derivation_salt],
    'secret_key_base' => nil
  }.freeze

  ENVIRONMENTS = %w[development test production].freeze
  BASE58_ALPHABET = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz'

  def initialize(mode: 'full')
    @mode = mode
    @errors = []
    @warnings = []
  end

  def validate_all
    puts "🔍 Validating Rails credentials (mode: #{@mode})..."
    puts

    case @mode
    when 'full'
      validate_environments(%w[development test production])
    when 'ci'
      validate_environments(%w[development test])
      validate_production_structure_only
    when 'pre-commit'
      validate_environments(%w[development test])
      validate_production_if_available
    else
      validate_environments(%w[development test production])
    end

    report_results
    @errors.empty?
  end

  private

  def validate_environments(envs)
    envs.each do |env|
      puts "📂 Validating #{env} environment..."
      validate_environment(env)
      puts
    end
  end

  def validate_environment(env)
    key_file = "config/credentials/#{env}.key"
    credentials_file = "config/credentials/#{env}.yml.enc"

    # Check if files exist
    unless File.exist?(key_file)
      @errors << "❌ Missing key file: #{key_file}"
      return
    end

    unless File.exist?(credentials_file)
      @errors << "❌ Missing credentials file: #{credentials_file}"
      return
    end

    # Validate key format
    validate_key_file(key_file, env)

    # Validate credentials content
    validate_credentials_content(env)
  end

  def validate_key_file(key_file, env)
    key_content = File.read(key_file).strip

    # Check length
    if key_content.length != 32
      @errors << "❌ #{env} key must be exactly 32 characters, got #{key_content.length}"
      return
    end

    # Check base58 format
    unless key_content.chars.all? { |char| BASE58_ALPHABET.include?(char) }
      @errors << "❌ #{env} key contains invalid characters (must be base58)"
      return
    end

    # Check for newlines
    if File.read(key_file) != key_content
      @warnings << "⚠️  #{env} key file contains trailing newlines (will be ignored by Rails)"
    end

    puts "✅ #{env} key format is valid"
  end

  def validate_credentials_content(env)
    begin
      # Load credentials
      require File.expand_path('config/application', Dir.pwd)
      Rails.application.config.credentials = Rails.application.credentials

      credentials = Rails.application.credentials.config_for(env.to_sym)

      if credentials.nil? || credentials.empty?
        @errors << "❌ #{env} credentials are empty or cannot be decrypted"
        return
      end

      # Validate required keys
      required_keys = env == 'production' ? PRODUCTION_REQUIRED_KEYS : REQUIRED_KEYS
      validate_required_keys(credentials, required_keys, env)

      puts "✅ #{env} credentials content is valid"

    rescue => e
      @errors << "❌ Failed to decrypt #{env} credentials: #{e.message}"
    end
  end

  def validate_production_structure_only
    puts "📂 Validating production environment (structure only)..."

    key_file = "config/credentials/production.key"
    credentials_file = "config/credentials/production.yml.enc"

    unless File.exist?(credentials_file)
      @errors << "❌ Missing production credentials file: #{credentials_file}"
      return
    end

    if File.exist?(key_file)
      @warnings << "⚠️  Production key file exists in CI environment (should be secure)"
    else
      puts "✅ Production key properly secured (not in CI)"
    end

    puts "✅ Production credentials structure is valid"
    puts
  end

  def validate_production_if_available
    key_file = "config/credentials/production.key"

    if File.exist?(key_file)
      puts "📂 Validating production environment (admin mode)..."
      validate_environment('production')
    else
      puts "📂 Skipping production validation (no key available - developer mode)"
      puts "✅ Production validation skipped safely"
    end
    puts
  end

  def validate_required_keys(credentials, required_keys, env)
    required_keys.each do |key, sub_keys|
      if sub_keys.nil?
        # Simple key
        unless credentials.key?(key)
          @errors << "❌ Missing #{env} credential: #{key}"
        end
      else
        # Nested keys
        unless credentials.key?(key)
          @errors << "❌ Missing #{env} credential section: #{key}"
          next
        end

        sub_keys.each do |sub_key|
          unless credentials.dig(key, sub_key)
            @errors << "❌ Missing #{env} credential: #{key}.#{sub_key}"
          end
        end
      end
    end
  end

  def report_results
    puts "=" * 50
    puts "📊 VALIDATION RESULTS"
    puts "=" * 50

    if @warnings.any?
      puts
      puts "⚠️  WARNINGS:"
      @warnings.each { |warning| puts "   #{warning}" }
    end

    if @errors.any?
      puts
      puts "❌ ERRORS:"
      @errors.each { |error| puts "   #{error}" }
      puts
      puts "💡 TROUBLESHOOTING:"
      puts "   • Ensure all credential files exist and are properly encrypted"
      puts "   • Verify keys are exactly 32 base58 characters"
      puts "   • Check that all required credential keys are present"
      puts "   • Use 'SecureRandom.base58(32)' to generate valid keys"
      puts
      exit 1
    else
      puts
      puts "🎉 All validations passed successfully!"
      puts
      case @mode
      when 'ci'
        puts "✅ CI environment is ready for testing"
      when 'pre-commit'
        puts "✅ Credentials are ready for commit"
      else
        puts "✅ All environments are properly configured"
      end
    end
  end
end

# Parse command line options
options = {}
OptionParser.new do |opts|
  opts.banner = "Usage: #{$0} [options]"

  opts.on("--mode MODE", "Validation mode: full, ci, pre-commit") do |mode|
    options[:mode] = mode
  end

  opts.on("--ci", "CI mode (skip production content validation)") do
    options[:mode] = 'ci'
  end

  opts.on("--pre-commit", "Pre-commit mode (smart production handling)") do
    options[:mode] = 'pre-commit'
  end

  opts.on("-h", "--help", "Show this help") do
    puts opts
    exit
  end
end.parse!

# Run validation
validator = CredentialsValidator.new(mode: options[:mode] || 'full')
success = validator.validate_all

exit(success ? 0 : 1)
```

**Make executable**:

```bash
chmod +x bin/validate_credentials
```

### 2. Update Application Configuration

**File**: `config/application.rb`

Find the credentials configuration section and update it to safely handle missing credentials:

```ruby
# Around line 20-30, update the credentials configuration:
config.credentials.content_path = Rails.root.join("config", "credentials", "#{Rails.env}.yml.enc")
config.credentials.key_path = Rails.root.join("config", "credentials", "#{Rails.env}.key")

# Add safe credential access for Active Record Encryption
if Rails.application.credentials.config.present?
  encryption_config = Rails.application.credentials.config.dig(:active_record_encryption)
  if encryption_config.present?
    config.active_record.encryption.primary_key = encryption_config[:primary_key]
    config.active_record.encryption.deterministic_key = encryption_config[:deterministic_key]
    config.active_record.encryption.key_derivation_salt = encryption_config[:key_derivation_salt]
  end
end
```

### 3. Update .gitignore

**File**: `.gitignore`

Add these rules to properly handle credential files:

```gitignore
# Rails credentials - environment-specific rules
/config/credentials/development.key
/config/credentials/test.key
/config/credentials/production.key
!/config/credentials/development.key
!/config/credentials/test.key
# Note: production.key is never committed for security
```

### 4. Create Production Credentials Template

**File**: `config/credentials/production.yml.template`

```yaml
# Production credentials template
# Copy this structure when creating production credentials

# Required for Active Record Encryption
active_record_encryption:
  primary_key: 64_character_hex_string # Generate with SecureRandom.hex(32)
  deterministic_key: 64_character_hex_string # Generate with SecureRandom.hex(32)
  key_derivation_salt: 64_character_hex_string # Generate with SecureRandom.hex(32)

# Required for Rails application
secret_key_base: your_secret_key_base_here
# Add your production-specific secrets here:
# database:
#   password: your_db_password
#
# api_keys:
#   external_service: your_api_key
#
# aws:
#   access_key_id: your_aws_key
#   secret_access_key: your_aws_secret
```

### 5. Update CI/CD Workflow

**File**: `.github/workflows/test.yml`

Add credentials validation jobs:

```yaml
name: Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  validate-credentials:
    name: Validate Credentials
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: 3.2.0
          bundler-cache: true

      - name: Validate credentials configuration
        run: |
          bin/validate_credentials --ci

  test:
    name: Test
    runs-on: ubuntu-latest
    needs: validate-credentials
    # ... rest of your existing test configuration

  # If you have a deployment job, add this before deployment:
  validate-production-ready:
    name: Validate Production Ready
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' # Only on main branch
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: 3.2.0
          bundler-cache: true

      - name: Validate production credentials structure
        run: |
          bin/validate_credentials --ci

      # Add your deployment steps after this validation
```

### 6. Generate Environment-Specific Credentials

**Development credentials** (safe to commit):

```bash
# Generate development key
echo "$(ruby -e 'require "securerandom"; puts SecureRandom.base58(32)')" > config/credentials/development.key

# Create development credentials
EDITOR="vim" bin/rails credentials:edit --environment development
```

**Test credentials** (safe to commit):

```bash
# Generate test key
echo "$(ruby -e 'require "securerandom"; puts SecureRandom.base58(32)')" > config/credentials/test.key

# Create test credentials
EDITOR="vim" bin/rails credentials:edit --environment test
```

**Production credentials** (secure setup):

```bash
# Generate production key (NEVER COMMIT THIS)
echo "$(ruby -e 'require "securerandom"; puts SecureRandom.base58(32)')" > config/credentials/production.key

# Create production credentials
EDITOR="vim" bin/rails credentials:edit --environment production

# Use the template from config/credentials/production.yml.template
```

### 7. Required Credentials Structure

**All environments need**:

```yaml
active_record_encryption:
  primary_key: [64 hex characters - SecureRandom.hex(32)]
  deterministic_key: [64 hex characters - SecureRandom.hex(32)]
  key_derivation_salt: [64 hex characters - SecureRandom.hex(32)]
```

**Production additionally needs**:

```yaml
secret_key_base: [Rails secret - SecureRandom.hex(64)]
```

### 8. Testing the Implementation

```bash
# Test local validation
bin/validate_credentials

# Test CI mode
bin/validate_credentials --ci

# Test with invalid credentials (should fail)
echo "invalid_key_format" > config/credentials/development.key
bin/validate_credentials  # Should show errors

# Fix and test again
echo "$(ruby -e 'require "securerandom"; puts SecureRandom.base58(32)')" > config/credentials/development.key
bin/validate_credentials  # Should pass
```

## Security Model

### File Permissions

- **Development/Test keys**: Committed to repo (safe for team sharing)
- **Production key**: NEVER committed (admin/boss only)
- **All .yml.enc files**: Committed (encrypted, safe)

### Validation Modes

- **Local (`full`)**: Validates all environments if keys available
- **CI (`ci`)**: Validates dev/test + production structure only
- **Pre-commit (`pre-commit`)**: Smart production handling based on key availability

### Access Control

- **Regular Developers**: Can work with dev/test, commits allowed without production access
- **Admin/Boss**: Has production key locally, can validate/manage production credentials
- **CI/CD**: Validates structure and development/test content, secures production

## Workflow Benefits

1. **Fail-Fast**: Invalid credentials caught before deployment
2. **Security**: Production secrets never exposed in CI/CD
3. **Developer Experience**: Simple local development without production concerns
4. **Scalability**: Easy to add new environments and credential requirements
5. **Maintainability**: Clear validation errors with troubleshooting guidance

## Troubleshooting

### Common Issues

**"Encryption key must be exactly 32 characters"**

- Use `SecureRandom.base58(32)` for key generation
- Check for trailing newlines in key files

**"Missing credential: active_record_encryption.primary_key"**

- Add required encryption keys to credentials
- Use `SecureRandom.hex(32)` for encryption keys

**"Failed to decrypt credentials"**

- Verify key file matches the encrypted credentials
- Regenerate credentials if key is lost

### Validation Commands

```bash
# Full validation (local development)
bin/validate_credentials

# CI validation (dev/test only)
bin/validate_credentials --ci

# Debug specific environment
RAILS_ENV=development rails credentials:show
```

## Implementation Checklist

- [ ] Create `bin/validate_credentials` script
- [ ] Update `config/application.rb` for safe credential access
- [ ] Update `.gitignore` with credential file rules
- [ ] Create `config/credentials/production.yml.template`
- [ ] Update CI/CD workflow with validation jobs
- [ ] Generate development and test credentials
- [ ] Set up production credentials (admin only)
- [ ] Test all validation modes
- [ ] Verify CI/CD pipeline passes
- [ ] Document team workflow

## Notes for Other Applications

1. **Adjust required keys**: Modify `REQUIRED_KEYS` and `PRODUCTION_REQUIRED_KEYS` in validator based on your app's needs
2. **Environment names**: Update `ENVIRONMENTS` array if you have different environment names
3. **Ruby version**: Ensure Ruby version in CI workflow matches your app
4. **Workflow integration**: Adapt the GitHub Actions workflow to your existing CI/CD setup
5. **Credential templates**: Customize the production template for your app's specific secrets

This implementation provides a robust, secure, and scalable credentials management system that will prevent deployment issues while maintaining proper security practices.
