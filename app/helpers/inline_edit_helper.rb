# frozen_string_literal: true

module InlineEditHelper
  # Generates inline editable field for ActiveAdmin
  # Example usage in admin views:
  #
  # For boolean fields:
  #   row :snowfox_employee do |user|
  #     inline_edit_field(user, :snowfox_employee, type: :boolean)
  #   end
  #
  # For text fields:
  #   row :display_name do |user|
  #     inline_edit_field(user, :display_name)
  #   end
  #
  # For select fields:
  #   row :status do |user|
  #     inline_edit_field(user, :status, type: :select, collection: [['Active', 'active'], ['Suspended', 'suspended'], ['Banned', 'banned']])
  #   end
  #
  # In index view:
  #   column :snowfox_employee do |user|
  #     inline_edit_field(user, :snowfox_employee, type: :boolean)
  #   end
  def inline_edit_field(resource, field, options = {})
    field_type = options[:type] || infer_field_type(resource, field)
    current_value = resource.send(field)
    resource_name = resource.class.name.underscore

    content_tag :div,
                data: {
                  controller: 'inline-edit',
                  inline_edit_url_value: options[:url] || admin_resource_path(resource),
                  inline_edit_field_value: field,
                  inline_edit_resource_name_value: resource_name,
                  inline_edit_resource_id_value: resource.id
                },
                class: 'inline-edit-container' do
      display = content_tag :span, data: { inline_edit_target: 'display' } do
        display_value = format_value(current_value, field_type)

        # Only show edit trigger if user can update the resource
        # Check if we're in ActiveAdmin context and user can update
        ability = defined?(current_admin_user) && current_admin_user ? AdminAbility.new(current_admin_user) : nil
        if ability && ability.can?(:update, resource)
          edit_link = link_to '✏️', '#',
                              class: 'inline-edit-trigger',
                              data: { action: 'click->inline-edit#edit' },
                              title: 'Click to edit'
          safe_join([ display_value, ' ', edit_link ])
        else
          display_value
        end
      end

      form = content_tag :span,
                         data: { inline_edit_target: 'form' },
                         class: 'inline-edit-form',
                         style: 'display: none;' do
        input_field = case field_type
        when :boolean
                        check_box_tag "#{resource_name}[#{field}]", '1', current_value,
                                      data: {
                                        inline_edit_target: 'input',
                                        action: 'keydown->inline-edit#handleKeydown'
                                      },
                                      class: 'inline-edit-input'
        when :select
                        select_tag "#{resource_name}[#{field}]",
                                   options_for_select(options[:collection] || [], current_value),
                                   data: {
                                     inline_edit_target: 'input',
                                     action: 'keydown->inline-edit#handleKeydown'
                                   },
                                   class: 'inline-edit-input'
        else
                        text_field_tag "#{resource_name}[#{field}]", current_value,
                                       data: {
                                         inline_edit_target: 'input',
                                         action: 'keydown->inline-edit#handleKeydown'
                                       },
                                       class: 'inline-edit-input'
        end

        save_button = button_tag '✓',
                                 type: 'button',
                                 class: 'inline-edit-save',
                                 data: { action: 'click->inline-edit#save' },
                                 title: 'Save'

        cancel_button = button_tag '✕',
                                   type: 'button',
                                   class: 'inline-edit-cancel',
                                   data: { action: 'click->inline-edit#cancel' },
                                   title: 'Cancel'

        spinner = content_tag :span, '⌛',
                              class: 'inline-edit-spinner',
                              data: { inline_edit_target: 'spinner' },
                              style: 'display: none;'

        safe_join([ input_field, ' ', save_button, ' ', cancel_button, ' ', spinner ])
      end

      safe_join([ display, form ])
    end
  end

  private

  def infer_field_type(resource, field)
    column = resource.class.columns_hash[field.to_s]
    return :unknown unless column

    case column.type
    when :boolean
      :boolean
    when :string, :text
      :text
    when :integer, :decimal, :float
      :number
    else
      :text
    end
  end

  def format_value(value, field_type)
    case field_type
    when :boolean
      # Check if we're in ActiveAdmin context
      if respond_to?(:status_tag)
        status_tag(value ? 'Yes' : 'No', class: value ? 'yes' : 'no')
      else
        # Fallback for non-ActiveAdmin contexts (like tests)
        content_tag :span, (value ? 'Yes' : 'No'), class: "status_tag #{value ? 'yes' : 'no'}"
      end
    else
      value.to_s
    end
  end

  def admin_resource_path(resource)
    url_for([ :admin, resource ])
  end
end
