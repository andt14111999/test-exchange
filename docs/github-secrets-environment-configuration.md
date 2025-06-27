# GitHub Secrets & Environment Variables Configuration

## GitHub Secrets Required

Add these secrets in your repository settings (`Settings > Secrets and variables > Actions > Repository secrets`):

### **AWS Configuration**

```
AWS_ACCESS_KEY_ID=<your-aws-access-key>
AWS_SECRET_ACCESS_KEY=<your-aws-secret-key>
```

### **Backend (Rails) Secrets**

```
RAILS_MASTER_KEY=<your-rails-master-key>
```

### **Client (Next.js) Build-time Secrets**

```
GOOGLE_CLIENT_ID=<your-google-client-id>
ROLLBAR_ACCESS_TOKEN=<your-rollbar-token>
```

### **Monitoring**

```
ROLLBAR_ACCESS_TOKEN=<your-rollbar-token>
```

## Environment Variable Strategy by Component

### 🚀 **Backend (Rails)**

**Build-time Variables** (GitHub Secrets → Docker build args):

- `RAILS_MASTER_KEY` - Required for asset compilation
- `RAILS_ENV=production` - Build environment

**Runtime Variables** (Already in ECS task definition):

- `DATABASE_URL`
- `REDIS_URL`
- `KAFKA_BROKER`
- `FRONTEND_URL`
- `COIN_PORTAL_URL`
- `EXCHANGE_SIGNING_KEY`
- `SEED_ENCRYPTION_KEY`
- All other environment variables from your Pulumi stack

**Docker Build Context**: Root folder (`/`)
**Dockerfile**: `Dockerfile` (in root)

### 🎨 **Client (Next.js)**

**Build-time Variables** (Required in Docker build):

- `NEXT_PUBLIC_API_BASE_URL` - Backend API endpoint
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID` - Google OAuth (from GitHub Secrets)
- `NEXT_PUBLIC_WS_URL` - WebSocket endpoint
- `NEXT_PUBLIC_BACKEND_HOSTNAME` - Backend hostname
- `NEXT_PUBLIC_ROLLBAR_ACCESS_TOKEN` - Error tracking (from GitHub Secrets)
- `NEXT_PUBLIC_ROLLBAR_ENVIRONMENT` - Environment name

**Runtime Variables**: None (Next.js is static after build)

**Docker Build Context**: `./client`
**Dockerfile**: `./client/dockerfile`

### ⚙️ **Engine (Java)**

**Build-time Variables** (Optional):

- `BUILD_VERSION` - Git commit SHA for versioning
- `BUILD_TIMESTAMP` - Build timestamp

**Runtime Variables** (Set in ECS task definition):

- All Kafka, database, and application configuration
- Environment variables from your Pulumi engine stack

**Docker Build Context**: `./engine`
**Dockerfile**: `./engine/Exchange-Engine-Docker/Dockerfile`

## Configuration Instructions

### 1. **Set GitHub Repository Secrets**

Go to your repository:

1. `Settings` → `Secrets and variables` → `Actions`
2. Click `New repository secret`
3. Add each secret listed above

### 2. **Update Domain Configuration**

In the CI/CD workflow, update these domains for your environment:

```yaml
# For prod2 environment, update these URLs:
NEXT_PUBLIC_API_BASE_URL=https://your-backend-domain.com/api/v1
NEXT_PUBLIC_WS_URL=wss://your-backend-domain.com
NEXT_PUBLIC_BACKEND_HOSTNAME=your-backend-domain.com
```

### 3. **Environment-Specific Configuration**

For different environments (prod2, production), you can:

**Option A**: Use different GitHub Environments

```yaml
environment: prod2 # or production
```

**Option B**: Use environment-specific secrets

```
RAILS_MASTER_KEY_PROD2=<prod2-key>
RAILS_MASTER_KEY_PRODUCTION=<production-key>
```

## Security Best Practices

### ✅ **DO**

- Store all sensitive values as GitHub Secrets
- Use build-time variables only when necessary
- Keep runtime variables in ECS task definitions
- Use different secrets for different environments

### ❌ **DON'T**

- Put sensitive values directly in workflow files
- Use runtime database URLs as build args
- Expose internal service URLs in client build
- Store non-sensitive values as secrets (like environment names)

## Troubleshooting

### Common Issues:

1. **Client build fails with missing environment variables**

   - Ensure all `NEXT_PUBLIC_*` variables are set as build args
   - Check that GitHub Secrets are correctly named

2. **Backend build fails with asset compilation**

   - Verify `RAILS_MASTER_KEY` is set in GitHub Secrets
   - Check that the secret value matches your development key

3. **Runtime environment variables not available**
   - These should be in ECS task definition, not Docker build
   - Use AWS Console or Pulumi to verify ECS environment variables

## Example GitHub Secrets Configuration

```bash
# AWS Deployment
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# Rails Application
RAILS_MASTER_KEY=abc123def456...

# Google OAuth
GOOGLE_CLIENT_ID=123456789-abc.apps.googleusercontent.com

# Error Tracking
ROLLBAR_ACCESS_TOKEN=abcdef123456...
```

This configuration ensures secure, environment-specific deployments while maintaining separation between build-time and runtime concerns.
