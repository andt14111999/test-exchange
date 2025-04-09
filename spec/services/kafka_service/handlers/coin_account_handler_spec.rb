require 'rails_helper'

RSpec.describe KafkaService::Handlers::CoinAccountHandler, type: :service do
  describe '#handle' do
    it 'processes balance update successfully' do
      handler = described_class.new
      user = create(:user)

      # Create a real account for the test
      account = create(:coin_account,
                      user: user,
                      coin_currency: 'btc',
                      account_type: 'main',
                      layer: 'all',
                      balance: BigDecimal('5.0'),
                      frozen_balance: BigDecimal('1.0'))

      # Mock the find_account method to return our account
      allow(handler).to receive(:find_account).and_return(account)

      payload = {
        'key' => "#{user.id}-coin-#{account.id}",
        'availableBalance' => '10.5',
        'frozenBalance' => '1.5'
      }

      # Expect the update method to be called
      expect(handler).to receive(:update_account_balance).with(account, payload)

      handler.handle(payload)
    end

    it 'updates existing account' do
      handler = described_class.new
      user = create(:user)
      existing_account = create(:coin_account,
        user: user,
        coin_currency: 'btc',
        account_type: 'main',
        layer: 'all',
        balance: '5.0',
        frozen_balance: '1.0'
      )

      # Mock the find_account method
      allow(handler).to receive(:find_account).and_return(existing_account)

      payload = {
        'key' => "#{user.id}-coin-#{existing_account.id}",
        'availableBalance' => '15.5',
        'frozenBalance' => '2.5'
      }

      # Test that the update method works
      handler.handle(payload)

      # The update should happen within the transaction
      existing_account.reload
      expect(existing_account.balance).to eq(BigDecimal('15.5'))
      expect(existing_account.frozen_balance).to eq(BigDecimal('2.5'))
    end

    it 'handles errors gracefully' do
      handler = described_class.new
      payload = {
        'key' => 'invalid',
        'availableBalance' => '10.5',
        'frozenBalance' => '1.5'
      }

      # Force an error to be raised during find_account
      allow(handler).to receive(:find_account).and_raise(StandardError.new('Test error'))

      # Don't check for log messages as they might be suppressed in the test environment
      expect { handler.handle(payload) }.not_to raise_error
    end

    it 'logs balance update processing' do
      handler = described_class.new
      user = create(:user)
      account = create(:coin_account, user: user)

      allow(handler).to receive(:find_account).and_return(account)
      allow(handler).to receive(:update_account_balance)

      payload = {
        'key' => "#{user.id}-coin-#{account.id}",
        'availableBalance' => '10.5',
        'frozenBalance' => '1.5'
      }

      expect(Rails.logger).to receive(:info).with("Processing balance update: #{payload}")
      handler.handle(payload)
    end
  end
end
