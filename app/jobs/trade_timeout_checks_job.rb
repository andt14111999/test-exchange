# frozen_string_literal: true

class TradeTimeoutChecksJob
  include Sidekiq::Job

  def perform
    Rails.logger.info "Running trade timeout checks at #{Time.zone.now}"

    # Process unpaid trades
    Trade.unpaid.find_each do |trade|
      next unless trade.unpaid_timeout?

      begin
        Rails.logger.info "Processing timeout for unpaid trade #{trade.ref}"
        trade.perform_timeout_checks!
      rescue StandardError => e
        Rails.logger.error "Error processing timeout for trade #{trade.ref}: #{e.message}"
      end
    end

    # Process paid trades
    Trade.paid.find_each do |trade|
      next unless trade.paid_timeout?

      begin
        Rails.logger.info "Processing timeout for paid trade #{trade.ref}"
        trade.perform_timeout_checks!
      rescue StandardError => e
        Rails.logger.error "Error processing timeout for paid trade #{trade.ref}: #{e.message}"
      end
    end

    Rails.logger.info "Completed trade timeout checks at #{Time.zone.now}"
  end
end
