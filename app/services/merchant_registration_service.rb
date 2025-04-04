# frozen_string_literal: true

class MerchantRegistrationService
  def initialize(user)
    @user = user
  end

  def call
    return false unless can_register_as_merchant?

    @user.transaction do
      @user.update!(role: 'merchant')
      true
    end
  rescue StandardError => e
    Rails.logger.error "Merchant registration failed: #{e.message}"
    false
  end

  private

  def can_register_as_merchant?
    @user.role == 'user' && @user.active?
  end
end
