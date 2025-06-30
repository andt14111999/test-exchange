import { Controller } from "@hotwired/stimulus"

// Reusable inline edit controller for ActiveAdmin
export default class extends Controller {
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
      this.formTarget.style.display = "none"
    }
    if (this.hasSpinnerTarget) {
      this.spinnerTarget.style.display = "none"
    }
  }

  edit(event) {
    event.preventDefault()
    this.displayTarget.style.display = "none"
    this.formTarget.style.display = "inline-block"
    if (this.hasInputTarget) {
      this.inputTarget.focus()
    }
  }

  cancel(event) {
    if (event) event.preventDefault()
    this.formTarget.style.display = "none"
    this.displayTarget.style.display = "inline-block"
  }

  async save(event) {
    event.preventDefault()
    
    // Show spinner
    if (this.hasSpinnerTarget) {
      this.spinnerTarget.style.display = "inline-block"
    }

    const value = this.getValue()
    const url = this.urlValue || `/admin/${this.resourceNameValue}/${this.resourceIdValue}`
    
    try {
      const response = await fetch(url, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-Token': document.querySelector('[name="csrf-token"]').content,
          'Accept': 'application/json'
        },
        body: JSON.stringify({
          [this.resourceNameValue]: {
            [this.fieldValue]: value
          },
          inline_edit: true
        })
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const data = await response.json()
      
      // Update display value
      this.updateDisplay(data[this.fieldValue])
      
      // Hide form and show display
      this.cancel()
      
      // Show success message
      this.showFlash('success', 'Updated successfully')
    } catch (error) {
      console.error('Error:', error)
      this.showFlash('error', 'Failed to update. Please try again.')
    } finally {
      // Hide spinner
      if (this.hasSpinnerTarget) {
        this.spinnerTarget.style.display = "none"
      }
    }
  }

  getValue() {
    const input = this.inputTarget
    if (input.type === 'checkbox') {
      return input.checked
    } else if (input.type === 'select-one') {
      return input.value
    } else {
      return input.value
    }
  }

  updateDisplay(value) {
    const displayElement = this.displayTarget.querySelector('.status_tag') || this.displayTarget
    
    if (this.fieldValue === 'snowfox_employee' || typeof value === 'boolean') {
      // For boolean fields
      displayElement.textContent = value ? 'Yes' : 'No'
      displayElement.className = 'status_tag ' + (value ? 'yes' : 'no')
    } else {
      // For other fields
      displayElement.textContent = value
    }
  }

  showFlash(type, message) {
    // Create or update flash message
    let flashContainer = document.querySelector('.flashes')
    if (!flashContainer) {
      flashContainer = document.createElement('div')
      flashContainer.className = 'flashes'
      document.querySelector('#title_bar').insertAdjacentElement('afterend', flashContainer)
    }

    const flash = document.createElement('div')
    flash.className = `flash flash_${type}`
    flash.textContent = message
    flashContainer.appendChild(flash)

    // Auto-remove after 3 seconds
    setTimeout(() => {
      flash.remove()
      if (flashContainer.children.length === 0) {
        flashContainer.remove()
      }
    }, 3000)
  }

  handleKeydown(event) {
    if (event.key === 'Escape') {
      this.cancel()
    } else if (event.key === 'Enter' && event.target.type !== 'textarea') {
      event.preventDefault()
      this.save(event)
    }
  }
} 