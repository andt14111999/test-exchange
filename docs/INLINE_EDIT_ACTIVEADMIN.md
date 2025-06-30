# Inline Edit for ActiveAdmin

This document explains how to use the inline edit feature in ActiveAdmin.

## Overview

The inline edit feature allows you to edit fields directly in the admin interface without navigating to the edit form. It uses Stimulus JS for interactivity and supports various field types.

## Setup

The inline edit feature is already configured and ready to use. It includes:

1. **Stimulus Controller** (`inline_edit_controller.js`) - Handles the interactive behavior
2. **Helper Module** (`InlineEditHelper`) - Generates the HTML for inline editing
3. **CSS Styles** (`inline_edit.scss`) - Provides visual styling
4. **Controller Support** - Admin controllers handle JSON requests for inline updates

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

1. The helper is already available globally through ApplicationHelper, so you can use it directly.

2. Update the controller to handle inline edits:
   ```ruby
   controller do
     def update
       if params[:inline_edit]
         resource.assign_attributes(permitted_params[:your_model])
         
         if resource.save
           render json: resource.attributes.slice(*permitted_params[:your_model].keys.map(&:to_s))
         else
           render json: { errors: resource.errors.full_messages }, status: :unprocessable_entity
         end
       else
         super
       end
     end
   end
   ```

3. Add the field to permitted params:
   ```ruby
   permit_params :your_field_name
   ```

4. Use the helper in your views:
   ```ruby
   row :your_field do |resource|
     inline_edit_field(resource, :your_field)
   end
   ```

## Features

- **Hover to Edit**: Edit icon appears on hover
- **Keyboard Support**: 
  - `Enter` to save
  - `Escape` to cancel
- **Visual Feedback**: Loading spinner and flash messages
- **Auto-detection**: Automatically detects field types (boolean, text, number)
- **Customizable**: Can specify field type and collections for selects

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

### Field-Level Permissions

For more granular control, you can restrict specific fields:

1. Define a custom ability in `AdminAbility`:
   ```ruby
   # Allow operators to manage users but not specific fields
   can :manage, User
   cannot :update_snowfox_employee, User
   ```

2. Check the permission in the controller:
   ```ruby
   controller do
     # Handle CanCan access denied for AJAX/JSON requests
     rescue_from CanCan::AccessDenied do |exception|
       if request.format.json? || params[:inline_edit]
         render json: { errors: [ exception.message ] }, status: :forbidden
       else
         redirect_to admin_user_path(resource), alert: exception.message
       end
     end

     def update
       # Check specific field permission if needed
       if params[:user] && params[:user].key?(:snowfox_employee)
         authorize! :update_snowfox_employee, resource
       end
       # ... rest of update logic
     end
   end
   ```

### Response Behavior

- **AJAX/JSON requests**: Return 401 (Unauthorized) status with error message
- **Normal form submissions**: Redirect to admin root with error message

This approach allows fine-grained control over who can edit specific models or fields while maintaining ActiveAdmin's standard permission system. 