# frozen_string_literal: true

class PaymentReceiptService
  include Rails.application.routes.url_helpers

  def initialize(trade)
    @trade = trade
  end

  def process_payment_receipt(receipt_details, file = nil)
    # Validate file if present
    if file.present?
      validate_file!(file)
      attach_file!(file)
      # Add file URL to receipt details (sau khi attach, file đã có thể lấy URL)
      receipt_details['file_url'] = get_file_url
    end

    # Update payment receipt details (chỉ update 1 lần)
    @trade.update!(
      payment_receipt_details: receipt_details,
      has_payment_proof: true
    )
  end

  private

  def validate_file!(file)
    # Check file size (max 10MB)
    max_size = 10.megabytes
    if file[:tempfile].size > max_size
      raise "File size must be less than #{max_size / 1.megabyte}MB"
    end

    # Check file type
    allowed_types = %w[image/jpeg image/png image/gif application/pdf]
    unless allowed_types.include?(file[:type])
      raise "File type not allowed. Allowed types: #{allowed_types.join(', ')}"
    end
  end

  def attach_file!(file)
    @trade.payment_receipt_file.attach(
      io: file[:tempfile],
      filename: sanitize_filename(file[:filename]),
      content_type: file[:type]
    )
  end

  def get_file_url
    return nil unless @trade.payment_receipt_file.attached?

    # Luôn sử dụng proxy URL qua backend để bảo mật
    # Thay vì direct S3 URLs, Rails sẽ serve files qua backend
    Rails.application.routes.url_helpers.rails_blob_url(
      @trade.payment_receipt_file,
      only_path: false,
      host: get_host_for_environment
    )
  rescue ArgumentError => e
    # Fallback nếu không có host config
    Rails.logger.warn "Could not generate file URL: #{e.message}"
    nil
  end

  def get_host_for_environment
    if Rails.env.production?
      ENV.fetch('EXCHANGE_BACKEND_DOMAIN', 'snow.exchange')
    else
      Rails.application.config.action_mailer.default_url_options[:host] || 'localhost:3969'
    end
  end

  def sanitize_filename(filename)
    # Remove any potentially dangerous characters
    filename.gsub(/[^0-9A-Za-z.\-_]/, '_')
  end
end
