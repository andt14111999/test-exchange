# Exchange CI/CD Scripts

## 🚀 Quick Setup (Recommended)

Use the simple script for easy one-command setup:

```bash
# 1. Install GitHub CLI (if not already installed)
brew install gh

# 2. Authenticate with GitHub
gh auth login

# 3. Run the setup script
ruby script/setup_github_secrets_simple.rb
```

## 📋 Scripts Available

### `setup_github_secrets_simple.rb` ⭐ Recommended

- **Uses GitHub CLI** - Much simpler and more reliable
- **Interactive prompts** - Guides you through each secret
- **Environment variable support** - Can read from env vars
- **Secure input** - Hides sensitive values during input

### `setup_github_secrets.rb`

- **Direct GitHub API** - More complex, requires proper encryption
- **Manual token management** - Need to create GitHub personal access token
- **Advanced use cases** - For custom integrations

## 🔧 Usage Examples

### Interactive Setup

```bash
ruby script/setup_github_secrets_simple.rb
```

### Environment Variables

```bash
export AWS_ACCESS_KEY_ID="AKIA..."
export AWS_SECRET_ACCESS_KEY="abc123..."
export RAILS_MASTER_KEY="def456..."
export GOOGLE_CLIENT_ID="123456789-abc.apps.googleusercontent.com"
export ROLLBAR_ACCESS_TOKEN="ghi789..."

ruby script/setup_github_secrets_simple.rb
```

### One-liner for CTO 😎

```bash
AWS_ACCESS_KEY_ID=xxx AWS_SECRET_ACCESS_KEY=yyy RAILS_MASTER_KEY=zzz ruby script/setup_github_secrets_simple.rb
```

## 📝 Required Secrets

| Secret                  | Description                            | Where to Find                                              |
| ----------------------- | -------------------------------------- | ---------------------------------------------------------- |
| `AWS_ACCESS_KEY_ID`     | AWS access key for deployment          | AWS IAM Console                                            |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key for deployment          | AWS IAM Console                                            |
| `RAILS_MASTER_KEY`      | Rails master key for asset compilation | `config/master.key` or `config/credentials/production.key` |
| `GOOGLE_CLIENT_ID`      | Google OAuth client ID                 | Google Cloud Console                                       |
| `ROLLBAR_ACCESS_TOKEN`  | Error tracking token                   | Rollbar Dashboard                                          |

## 🔍 Verification

After running the script, verify secrets are set:

```bash
# List all secrets
gh secret list

# Check specific secret (won't show value, just confirms it exists)
gh secret list | grep AWS_ACCESS_KEY_ID
```

## 🌟 For Your CTO

**"One script, zero hassle"** approach:

1. **Install GitHub CLI**: `brew install gh`
2. **Login once**: `gh auth login`
3. **Run script**: `ruby script/setup_github_secrets_simple.rb`
4. **Done!** ✅

The script will:

- ✅ Check prerequisites automatically
- ✅ Guide through each secret with clear descriptions
- ✅ Hide sensitive input for security
- ✅ Verify everything is set up correctly
- ✅ Show next steps

## 🛠 Troubleshooting

### GitHub CLI not found

```bash
# macOS
brew install gh

# Ubuntu/Debian
sudo apt install gh

# Windows
winget install GitHub.cli
```

### Not authenticated

```bash
gh auth login
# Select: "Login with a web browser"
```

### Permission denied

Make sure you have admin access to the repository or contact the repository owner.

### Script fails

```bash
# Check if you're in the right directory
pwd  # Should be in the exchange repository root

# Check file permissions
chmod +x script/setup_github_secrets_simple.rb

# Run with explicit Ruby
ruby script/setup_github_secrets_simple.rb
```

## 🔒 Security Notes

- ✅ Secrets are encrypted by GitHub automatically
- ✅ Script never logs or stores secret values
- ✅ Uses secure input methods for sensitive data
- ✅ Only repository admins can set secrets
- ❌ Never commit secrets to git
- ❌ Never share secrets in Slack/email
