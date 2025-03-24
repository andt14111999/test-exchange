# frozen_string_literal: true

class AddressGenerationService
  attr_reader :account

  def initialize(account)
    @account = account
  end

  def generate
    response = call_address_api
    parse_address(response)
  end

  private

  def call_address_api
    PostbackService.new(
      target_url: 'https://coin-portal.exchange.snowfoxglobal.org/api/v1/coin_addresses',
      payload: {
        account_type: account.account_type,
        coin: account.coin_currency,
        account_id: account.id
      }
    ).post
  end

  def parse_address(response)
    if response['coin_address'].present?
      response['coin_address']['address']
    else
      log_error
      nil
    end
  end

  def log_error
    return unless Rails.env.production?
    Rails.logger.error("Failed to generate coin address for account #{account.id}")
  end
end
