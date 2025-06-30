# frozen_string_literal: true

# Configure Capybara with Chrome options for system tests
require 'selenium-webdriver'

# Determine if we should run in headless mode
headless_mode = ENV['CI'].present?

# Debug output for test mode
puts "=" * 60
puts "Capybara Test Mode Configuration:"
puts "CI Environment: #{ENV['CI'].present? ? 'YES' : 'NO'}"
puts "Browser Mode: #{headless_mode ? 'HEADLESS' : 'VISIBLE'}"
puts "=" * 60

# Regular Chrome driver (visible browser)
Capybara.register_driver :selenium_chrome do |app|
  options = Selenium::WebDriver::Chrome::Options.new
  
  # Use Chrome for Testing instead of system Chrome
  options.browser_version = 'stable'
  
  # Standard configuration
  options.add_argument('--no-sandbox')
  options.add_argument('--disable-dev-shm-usage')
  options.add_argument('--disable-gpu')
  options.add_argument('--window-size=1400,1400')
  
  # Additional options for stability
  options.add_argument('--disable-extensions')
  options.add_argument('--disable-translate')
  
  Capybara::Selenium::Driver.new(
    app,
    browser: :chrome,
    options: options
  )
end

# Headless Chrome driver (for CI)
Capybara.register_driver :selenium_chrome_headless do |app|
  options = Selenium::WebDriver::Chrome::Options.new
  
  # Use Chrome for Testing instead of system Chrome
  # This allows Selenium Manager to download a compatible version
  options.browser_version = 'stable'
  
  # Standard headless configuration
  options.add_argument('--headless=new') # Use new headless mode
  options.add_argument('--no-sandbox')
  options.add_argument('--disable-dev-shm-usage')
  options.add_argument('--disable-gpu')
  options.add_argument('--window-size=1400,1400')
  
  # Additional options for stability
  options.add_argument('--disable-extensions')
  options.add_argument('--disable-translate')
  options.add_argument('--disable-background-timer-throttling')
  options.add_argument('--disable-backgrounding-occluded-windows')
  options.add_argument('--disable-renderer-backgrounding')
  options.add_argument('--disable-features=VizDisplayCompositor')
  
  # Create a Chrome Service to bypass webdrivers
  service = Selenium::WebDriver::Chrome::Service.new
  
  # Let Selenium Manager handle the driver
  Capybara::Selenium::Driver.new(
    app,
    browser: :chrome,
    options: options,
    service: service
  )
end

# Set the default JavaScript driver based on CI environment
if ENV['CI'].present?
  Capybara.javascript_driver = :selenium_chrome_headless
else
  Capybara.javascript_driver = :selenium_chrome
end

# Default max wait time for finding elements
Capybara.default_max_wait_time = 5

# Ignore hidden elements by default
Capybara.ignore_hidden_elements = true

# Configure server
Capybara.server = :puma, { Silent: true } 