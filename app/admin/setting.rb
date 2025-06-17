# frozen_string_literal: true

ActiveAdmin.register_page 'Settings' do
  menu priority: 1, label: 'Settings'

  content do
    panel 'Settings' do
      div class: 'settings-container' do
        table_for Setting.keys do
          column :name do |key|
            key.humanize
          end

          column :type do |key|
            value = Setting.send(key)
            value.class.to_s
          end

          column :current_value do |key|
            current_value = Setting.send(key)
            if key.to_s.include?('ratio')
              "#{(current_value.to_f * 100).round(4)}%"
            else
              current_value.to_s
            end
          end

          column :input do |key|
            current_value = Setting.send(key)
            display_options = Setting.display_options[key.to_s]

            if display_options && display_options[:type] == :select
              select_tag "setting_#{key}",
                options_for_select(display_options[:options], current_value),
                id: "setting_#{key}",
                class: 'setting-input',
                data: {
                  key: key,
                  old_value: current_value.to_s
                }
            elsif display_options && display_options[:type] == :boolean
              check_box_tag "setting_#{key}",
                'true',
                current_value == true,
                id: "setting_#{key}",
                class: 'setting-input boolean-input',
                data: {
                  key: key,
                  old_value: current_value.to_s
                }
            elsif display_options && display_options[:type] == :number
              number_field_tag "setting_#{key}",
                current_value.to_s,
                id: "setting_#{key}",
                class: 'setting-input',
                min: display_options[:min],
                max: display_options[:max],
                step: display_options[:step] || 1,
                data: {
                  key: key,
                  old_value: current_value.to_s
                }
            else
              text_area_tag "setting_#{key}",
                current_value.to_s,
                id: "setting_#{key}",
                class: 'setting-input',
                rows: 2,
                data: {
                  key: key,
                  old_value: current_value.to_s
                }
            end
          end
        end
      end
    end

    # Add CSS styles
    style do
      raw <<~CSS
        .settings-container table {
          width: 100%;
        }
        .settings-container th {
          background-color: #f0f0f0;
          padding: 10px;
          text-align: left;
        }
        .settings-container td {
          padding: 8px;
          border-bottom: 1px solid #ddd;
        }
        .setting-input {
          width: 100%;
          padding: 6px;
          border: 1px solid #ccc;
          border-radius: 4px;
        }
        .setting-notification {
          position: fixed;
          top: 20px;
          right: 20px;
          padding: 15px 20px;
          border-radius: 4px;
          color: white;
          font-weight: bold;
          z-index: 9999;
        }
        .setting-notification--success {
          background-color: #4CAF50;
        }
        .setting-notification--error {
          background-color: #f44336;
        }
      CSS
    end

    # Add JavaScript for real-time updates
    script do
      raw <<~JS
        $(document).ready(function() {
          // Define the updateSetting function
          window.updateSetting = function(input) {
            const key = input.dataset.key;
            const oldValue = input.dataset.oldValue;
            let value = input.value;

            // Special handling for checkboxes
            if (input.type === 'checkbox') {
              value = input.checked ? 'true' : 'false';
            }

            if (oldValue === value) return;

            $.ajax({
              url: "/admin/settings/update",
              method: "PATCH",
              data: { id: key, value: value },
              dataType: 'json',
              success: function(data) {
                if (data.success) {
                  // Update the current value display
                  const row = $(input).closest("tr");
                  row.find("td:nth-child(2)").text(data.type);
                  row.find("td:nth-child(3)").text(data.display_value || data.value);
                  input.dataset.oldValue = value;
                  showNotification("success", data.message);
                } else {
                  showNotification("error", data.message || "Failed to update setting");
                  revertValue(input, oldValue);
                }
              },
              error: function(xhr, status, error) {
                let message = "Network error occurred";

                try {
                  // Try to parse JSON response for validation errors
                  const response = JSON.parse(xhr.responseText);
                  if (response.message) {
                    message = response.message;
                  }
                } catch (e) {
                  // If parsing fails, use default message
                  console.error("Error parsing response:", e);
                }

                showNotification("error", message);
                revertValue(input, oldValue);
              },
            });
          };

          // Helper function to revert input value
          function revertValue(input, oldValue) {
            if (input.type === 'checkbox') {
              input.checked = oldValue === 'true';
            } else {
              input.value = oldValue;
            }
          }

          // Define showNotification function
          function showNotification(type, message) {
            console.log("Showing notification:", type, message); // Debug log

            $(".setting-notification").remove();

            const notification = $("<div/>", {
              class: `setting-notification setting-notification--${type}`,
              text: message,
            }).hide();

            $("body").append(notification);
            notification.fadeIn(300);

            setTimeout(function() {
              notification.fadeOut(300, function() {
                $(this).remove();
              });
            }, 3000);
          }

          // Set up event listeners
          $('.setting-input:not(.boolean-input)').on('blur', function() {
            updateSetting(this);
          });

          $('select.setting-input').on('change', function() {
            updateSetting(this);
          });

          $('.boolean-input').on('change', function() {
            updateSetting(this);
          });
        });
      JS
    end
  end

  page_action :update, method: :patch do
    key = params[:id]
    value = params[:value]

    return render json: { success: false, message: 'Unauthorized' } unless authorized?(:manage, Setting)

    begin
      # Use our validation method
      result = Setting.update_with_validation(key, value)

      if result[:success]
        current_value = Setting.send(key)
        display_value = if key.to_s.include?('ratio')
                         "#{(current_value.to_f * 100).round(4)}%"
        else
                         current_value.to_s
        end

        render json: {
          success: true,
          message: 'Setting updated successfully',
          value: current_value.to_s,
          display_value: display_value,
          type: current_value.class.to_s
        }
      else
        render json: {
          success: false,
          message: result[:errors].join(', ')
        }, status: :unprocessable_entity
      end
    rescue StandardError => e
      render json: {
        success: false,
        message: e.message
      }, status: :unprocessable_entity
    end
  end
end
