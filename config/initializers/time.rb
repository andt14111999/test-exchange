# frozen_string_literal: true

Time.class_eval do
  def self.safe_parse(value, default = nil)
    Time.zone.parse(value.to_s)
  rescue ArgumentError
    default
  end
end
