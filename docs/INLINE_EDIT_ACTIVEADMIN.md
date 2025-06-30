# Inline Edit for ActiveAdmin

This document explains how to use the inline edit feature in ActiveAdmin.

## Overview

The inline edit feature allows you to edit fields directly in the admin interface without navigating to the edit form. It uses Stimulus JS for interactivity, leverages ActiveAdmin's standard JSON API, and supports various field types.

## Key Benefits

- **No custom controller code required** - Uses ActiveAdmin's built-in JSON API
- **Automatic permission handling** - Respects CanCanCan authorization
- **Scalable** - Works with any ActiveAdmin resource without modification
- **Standard REST API** - Uses standard HTTP PATCH with JSON

## Setup

The inline edit feature is already configured and ready to use. It includes:

1. **Stimulus Controller** (`inline_edit_controller.js`) - Handles the interactive behavior
2. **Helper Module** (`InlineEditHelper`) - Generates the HTML for inline editing
3. **CSS Styles** (`inline_edit.scss`) - Provides visual styling
4. **No custom controller code needed** - Works with ActiveAdmin's standard JSON API

## Usage

### Basic Example (Boolean Field)

```ruby
# In your admin file (e.g., app/admin/users.rb)
show do
  attributes_table do
    row :snowfox_employee do |user|
      inline_edit_field(user, :snowfox_employee, type: :boolean)
    end
  end
end
```

### Text Field Example

```ruby
row :display_name do |user|
  inline_edit_field(user, :display_name)
end
```

### Select Field Example

```ruby
row :status do |user|
  inline_edit_field(user, :status, 
    type: :select, 
    collection: [['Active', 'active'], ['Suspended', 'suspended'], ['Banned', 'banned']]
  )
end
```

### In Index View

You can also use inline editing in the index view:

```ruby
index do
  column :snowfox_employee do |user|
    inline_edit_field(user, :snowfox_employee, type: :boolean)
  end
end
```

## Adding to Other Resources

To add inline editing to other ActiveAdmin resources:

1. Add the field to permitted params:
   ```ruby
   permit_params :your_field_name
   ```

2. Use the helper in your views:
   ```ruby
   row :your_field do |resource|
     inline_edit_field(resource, :your_field)
   end
   ```

That's it! The inline edit feature automatically uses ActiveAdmin's standard JSON API, so no custom controller code is needed.

## Features

- **Always Visible Edit Icon**: Edit icon is always visible for better discoverability
- **Keyboard Support**: 
  - `Enter` to save
  - `Escape` to cancel
- **Visual Feedback**: Loading spinner and flash messages
- **Auto-detection**: Automatically detects field types (boolean, text, number)
- **Customizable**: Can specify field type and collections for selects
- **Standard API**: Uses ActiveAdmin's built-in JSON API (returns 204 No Content on success)
- **Automatic Permission Checks**: Hides edit icon for users without update permission

## Field Types

The helper supports these field types:
- `:boolean` - Renders as checkbox
- `:text` - Renders as text input (default)
- `:number` - Renders as number input
- `:select` - Renders as dropdown (requires `collection` option)

## Security

- Uses Rails CSRF protection
- Respects ActiveAdmin permissions
- Only updates fields in `permit_params`
- Integrates with CanCanCan for attribute-level permissions

## Permission Control with CanCanCan

The inline edit feature respects CanCanCan permissions. You can control access at both the model and field level:

### Model-Level Permissions

To restrict entire model access in `AdminAbility`:
```ruby
# Allow operators to only read users
can :read, User

# Superadmins can manage everything
can :manage, :all
```

### Permission Behavior

When using the inline edit feature with CanCanCan permissions:

- **Without update permission**: The edit icon is automatically hidden
- **Unauthorized API calls**: ActiveAdmin returns 401 (Unauthorized) status
- **Normal form submissions**: ActiveAdmin redirects to admin root with error message

The inline edit feature fully respects your existing CanCanCan permissions without requiring any custom controller code.

## Testing

The inline edit functionality includes comprehensive tests:

1. **Unit/Integration Tests** (`spec/admin/users_snowfox_employee_spec.rb`):
   - Permission checks for different user roles
   - JSON response handling
   - Validation error handling
   - CanCanCan integration

2. **Acceptance/System Tests** (`spec/admin/users_inline_edit_spec.rb`):
   - Full browser-based tests with JavaScript
   - User interaction simulation
   - Visual state changes
   - Permission-based UI changes
   - Edge cases (rapid clicks, form cancellation)

To run the tests:
```bash
bundle exec rspec spec/admin/users_snowfox_employee_spec.rb
bundle exec rspec spec/admin/users_inline_edit_spec.rb
```

### Test Modes

The system tests (with JavaScript) support two modes:

1. **Visible Browser Mode** (default for local development):
   - Browser window opens and shows test execution
   - Useful for debugging and development
   - Automatically used when `CI` environment variable is not set

2. **Headless Mode** (for CI environments):
   - Tests run without visible browser window
   - Faster and suitable for CI/CD pipelines
   - Automatically used when `CI` environment variable is set

```bash
# Run tests with visible browser (local development)
bundle exec rspec spec/admin/users_inline_edit_spec.rb

# Run tests in headless mode (CI environment)
CI=true bundle exec rspec spec/admin/users_inline_edit_spec.rb
```

**Note**: If you encounter timeout errors in visible browser mode during local development, try running the tests in headless mode with `CI=true`, or increase the wait timeout in `spec/support/capybara.rb`. 