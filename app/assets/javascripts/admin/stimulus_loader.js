// Load Stimulus for ActiveAdmin
(function() {
  // Import Stimulus from the CDN for ActiveAdmin
  const script = document.createElement('script');
  script.src = 'https://unpkg.com/@hotwired/stimulus/dist/stimulus.umd.js';
  script.onload = function() {
    // Initialize Stimulus application
    const application = window.Stimulus.Application.start();
    
    // Register the inline edit controller
    application.register('inline-edit', class extends window.Stimulus.Controller {
      static targets = ["display", "form", "input", "spinner"]
      static values = { 
        url: String,
        field: String,
        resourceName: String,
        resourceId: String
      }

      connect() {
        // Hide form on initial load
        if (this.hasFormTarget) {
          this.formTarget.style.display = "none";
        }
        if (this.hasSpinnerTarget) {
          this.spinnerTarget.style.display = "none";
        }
      }

      edit(event) {
        event.preventDefault();
        this.displayTarget.style.display = "none";
        this.formTarget.style.display = "inline-block";
        if (this.hasInputTarget) {
          this.inputTarget.focus();
        }
      }

      cancel(event) {
        if (event) event.preventDefault();
        this.formTarget.style.display = "none";
        this.displayTarget.style.display = "inline-block";
      }

      async save(event) {
        event.preventDefault();
        
        // Show spinner
        if (this.hasSpinnerTarget) {
          this.spinnerTarget.style.display = "inline-block";
        }

        const value = this.getValue();
        const url = this.urlValue || `/admin/${this.resourceNameValue}/${this.resourceIdValue}`;
        
        // Get CSRF token - ActiveAdmin might use different methods
        let csrfToken = '';
        const csrfMeta = document.querySelector('meta[name="csrf-token"]');
        if (csrfMeta) {
          csrfToken = csrfMeta.content;
        } else {
          // Try to get from form
          const csrfParam = document.querySelector('meta[name="csrf-param"]')?.content || 'authenticity_token';
          const csrfInput = document.querySelector(`input[name="${csrfParam}"]`);
          if (csrfInput) {
            csrfToken = csrfInput.value;
          }
        }
        
        try {
          const response = await fetch(url, {
            method: 'PATCH',
            headers: {
              'Content-Type': 'application/json',
              'X-CSRF-Token': csrfToken,
              'Accept': 'application/json'
            },
            body: JSON.stringify({
              [this.resourceNameValue]: {
                [this.fieldValue]: value
              }
            })
          });

          // Handle 204 No Content (successful update with no body)
          if (response.status === 204) {
            // Update was successful, update the display with the value we sent
            this.updateDisplay(value);
            
            // Hide form and show display
            this.cancel();
            
            // Show success message
            this.showFlash('success', 'Updated successfully');
            return;
          }
          
          // Parse JSON response for other status codes
          let data;
          try {
            data = await response.json();
          } catch (e) {
            // If JSON parsing fails on error response
            if (!response.ok) {
              this.showFlash('error', 'Failed to update. Server returned an invalid response.');
              return;
            }
          }

          if (!response.ok) {
            // Handle error responses
            let errorMessage = 'Failed to update.';
            
            if (response.status === 403 || response.status === 401) {
              errorMessage = 'You are not authorized to update this field.';
            } else if (data && data.errors) {
              // Handle validation errors
              if (typeof data.errors === 'object' && !Array.isArray(data.errors)) {
                // Rails format: { errors: { field: ["error1", "error2"] } }
                const fieldErrors = data.errors[this.fieldValue];
                if (fieldErrors && fieldErrors.length > 0) {
                  errorMessage = fieldErrors.join(', ');
                } else {
                  // Get first error from any field
                  const firstError = Object.values(data.errors).flat()[0];
                  errorMessage = firstError || errorMessage;
                }
              } else if (Array.isArray(data.errors)) {
                // Simple array format: { errors: ["error1", "error2"] }
                errorMessage = data.errors.join(', ');
              }
            }
            
            this.showFlash('error', errorMessage);
            return;
          }
          
          // If we got JSON response with data, use it to update display
          if (data) {
            const updatedValue = data[this.resourceNameValue] ? 
              data[this.resourceNameValue][this.fieldValue] : 
              data[this.fieldValue];
            
            this.updateDisplay(updatedValue);
          } else {
            // Fallback to the value we sent
            this.updateDisplay(value);
          }
          
          // Hide form and show display
          this.cancel();
          
          // Show success message
          this.showFlash('success', 'Updated successfully');
        } catch (error) {
          console.error('Error:', error);
          // Handle network errors or JSON parsing errors
          this.showFlash('error', 'Failed to update. Please check your connection and try again.');
        } finally {
          // Hide spinner
          if (this.hasSpinnerTarget) {
            this.spinnerTarget.style.display = "none";
          }
        }
      }

      getValue() {
        const input = this.inputTarget;
        if (input.type === 'checkbox') {
          return input.checked;
        } else if (input.type === 'select-one') {
          return input.value;
        } else {
          return input.value;
        }
      }

      updateDisplay(value) {
        const displayElement = this.displayTarget.querySelector('.status_tag') || this.displayTarget;
        
        if (this.fieldValue === 'snowfox_employee' || typeof value === 'boolean') {
          // For boolean fields
          displayElement.textContent = value ? 'YES' : 'NO';
          displayElement.className = 'status_tag ' + (value ? 'yes' : 'no');
        } else {
          // For other fields
          displayElement.textContent = value;
        }
      }

      showFlash(type, message) {
        // Create or update flash message
        let flashContainer = document.querySelector('.flashes');
        if (!flashContainer) {
          flashContainer = document.createElement('div');
          flashContainer.className = 'flashes';
          document.querySelector('#title_bar').insertAdjacentElement('afterend', flashContainer);
        }

        const flash = document.createElement('div');
        flash.className = `flash flash_${type}`;
        flash.textContent = message;
        flashContainer.appendChild(flash);

        // Auto-remove after 3 seconds
        setTimeout(() => {
          flash.remove();
          if (flashContainer.children.length === 0) {
            flashContainer.remove();
          }
        }, 3000);
      }

      handleKeydown(event) {
        if (event.key === 'Escape') {
          this.cancel();
        } else if (event.key === 'Enter' && event.target.type !== 'textarea') {
          event.preventDefault();
          this.save(event);
        }
      }
    });
  };
  document.head.appendChild(script);
})(); 