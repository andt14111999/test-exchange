# frozen_string_literal: true

# Disable webdrivers gem to let Selenium Manager handle driver management
# The webdrivers gem is deprecated in favor of Selenium Manager built into Selenium 4.x

# This prevents webdrivers from intercepting driver paths
if defined?(Webdrivers)
  # Set cache time to 0 to disable caching
  Webdrivers.cache_time = 0

  # Disable webdrivers from registering its finders
  # This allows Selenium Manager to take over
  if defined?(Selenium::WebDriver::Chrome)
    Selenium::WebDriver::Chrome::Service.driver_path = nil
  end

  if defined?(Selenium::WebDriver::Firefox)
    Selenium::WebDriver::Firefox::Service.driver_path = nil
  end

  if defined?(Selenium::WebDriver::Edge)
    Selenium::WebDriver::Edge::Service.driver_path = nil
  end
end

# Alternative approach: Set environment variable to let Selenium Manager work
ENV['SE_MANAGER_PATH'] ||= nil  # Ensure Selenium Manager is used
