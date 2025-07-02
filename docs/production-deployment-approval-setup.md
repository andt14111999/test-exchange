# Production Deployment Approval Setup

This document provides step-by-step instructions for configuring GitHub Environment Protection Rules to require specific user approval before production deployments.

## Overview

Our production deployment workflow uses GitHub's native Environment Protection feature to ensure only authorized users can approve production deployments. This provides:

- **Security**: Only specified users can approve deployments
- **Audit Trail**: All approvals are logged with timestamps and approver details
- **Flexibility**: Easy to add/remove approvers without code changes
- **Reliability**: GitHub-native feature with built-in safeguards

## Configuration Steps

### Step 1: Access Environment Settings

1. Go to your GitHub repository
2. Click on **Settings** tab (requires admin access)
3. In the left sidebar, click **Environments**
4. You should see `prod2` environment listed

### Step 2: Configure prod2 Environment Protection

1. Click on the `prod2` environment
2. You'll see the environment configuration page

### Step 3: Add Required Reviewers

1. In the **Environment protection rules** section
2. Check the box for **Required reviewers**
3. In the text field, add GitHub usernames (one per line):

   ```
   username1
   username2
   username3
   ```

   **Examples:**

   ```
   andinh
   john-doe
   jane-smith
   ```

### Step 4: Configure Additional Protection Rules (Recommended)

**Prevent self-review** (Recommended):

- ☑️ Check "Prevent self-review"
- This prevents the person who triggered the deployment from approving it themselves

**Wait timer** (Optional):

- Set a minimum wait time before deployment can be approved
- Useful for giving time to review changes
- Recommended: 5-10 minutes

**Required approval count**:

- Choose how many reviewers must approve
- Options: "At least 1" or "All reviewers"
- For production: Recommend "At least 1" for flexibility

### Step 5: Branch Protection (Optional but Recommended)

1. Under **Deployment branches**, select **Protected branches only**
2. This ensures deployments only happen from protected branches (like `main`)

### Step 6: Save Configuration

1. Click **Save protection rules**
2. The environment is now configured with approval requirements

## How It Works

### Deployment Process

1. Developer pushes to `main` branch
2. GitHub Actions workflow starts
3. All tests and quality checks run automatically
4. When deployment jobs reach the `environment: prod2` step:
   - Workflow **pauses** and waits for approval
   - GitHub sends notifications to required reviewers
   - Deployment shows "Waiting for approval" status

### Approval Process

1. Required approvers receive GitHub notification
2. Approvers can:
   - View the deployment in GitHub Actions
   - Review the changes being deployed
   - **Approve** or **Reject** the deployment
3. Once approved, deployment continues automatically
4. If rejected, deployment stops

### Notification Settings

Approvers can configure notifications in their GitHub settings:

- Go to GitHub Settings → Notifications
- Enable "Actions" notifications for deployment reviews

## Managing Approvers

### Adding New Approvers

1. Go to repo Settings → Environments → prod2
2. Edit the "Required reviewers" list
3. Add new usernames
4. Save changes

### Removing Approvers

1. Edit the "Required reviewers" list
2. Remove usernames no longer needed
3. Save changes

### Emergency Access

In case primary approvers are unavailable:

1. Repository admins can temporarily modify approver list
2. Or admins can override environment protection (use with caution)

## Current Deployment Phases

The workflow has three phases, each requiring approval:

1. **Backend Deployment** (`deploy-backend`)

   - Deploys Rails API, Sidekiq, Clockwork, Kafka Consumer
   - URL: https://backend.anvo.dev/api/v1/test/ping

2. **Client Deployment** (`deploy-client`)

   - Deploys Next.js frontend
   - URL: https://anvo.dev

3. **Engine Deployment** (`deploy-engine`)
   - Deploys Java trading engine
   - URL: https://backend.anvo.dev

## Monitoring and Troubleshooting

### View Pending Approvals

1. Go to repository **Actions** tab
2. Click on the running workflow
3. You'll see jobs with "Waiting for approval" status
4. Click on the job to see approval interface

### Approval History

1. In the workflow run, approved deployments show:
   - Who approved
   - When approved
   - Any comments

### Common Issues

**Issue**: Approvers not receiving notifications

- **Solution**: Check GitHub notification settings

**Issue**: Can't approve deployment

- **Solution**: Ensure you're listed as required reviewer and have repo access

**Issue**: Emergency deployment needed

- **Solution**: Repository admin can temporarily modify environment rules

## Security Best Practices

1. **Limit Approvers**: Only add users who should approve production deployments
2. **Regular Review**: Periodically review and update approver list
3. **Monitor Deployments**: Review deployment history regularly
4. **Document Changes**: Keep track of who is added/removed as approvers

## Example Workflow

```yaml
environment:
  name: prod2
  url: https://anvo.dev
```

When GitHub sees this configuration, it automatically:

1. Pauses workflow execution
2. Requires approval from configured reviewers
3. Provides audit trail of all approvals
4. Links to the deployment URL for verification

## Support

For questions about:

- **GitHub Environment Protection**: [GitHub Docs](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment)
- **Workflow Issues**: Check repository Actions tab for detailed logs
- **Access Issues**: Contact repository administrators
