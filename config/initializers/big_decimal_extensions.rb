# frozen_string_literal: true

module BigDecimalSafeConverter
  def safe_convert(value, default = 0)
    return BigDecimal(default) if value.nil? || value.to_s.strip.empty?
    BigDecimal(value.to_s)
  rescue ArgumentError
    BigDecimal(default)
  end
end

BigDecimal.extend(BigDecimalSafeConverter)
