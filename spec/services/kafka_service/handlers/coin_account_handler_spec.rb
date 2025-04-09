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

    it 'does nothing when account is not found' do
      handler = described_class.new
      payload = {
        'key' => '123-coin-456',
        'availableBalance' => '10.5',
        'frozenBalance' => '1.5'
      }

      allow(handler).to receive(:find_account).and_return(nil)

      # Expect update_account_balance not to be called
      expect(handler).not_to receive(:update_account_balance)

      handler.handle(payload)
    end

    it 'logs error messages when exceptions occur' do
      handler = described_class.new
      payload = { 'key' => 'user-coin-123' }
      error = StandardError.new('Test error')
      backtrace = [ 'line1', 'line2' ]
      allow(error).to receive(:backtrace).and_return(backtrace)

      allow(handler).to receive(:find_account).and_raise(error)

      expect(Rails.logger).to receive(:error).with("Failed to update balance: Test error")
      expect(Rails.logger).to receive(:error).with(backtrace.join("\n"))

      handler.handle(payload)
    end
  end

  describe '#find_account' do
    it 'finds a coin account with correct parameters' do
      handler = described_class.new
      user = create(:user)
      account = create(:coin_account,
        user: user,
        account_type: 'main',
        layer: 'all',
        balance: '5.0',
        frozen_balance: '1.0'
      )

      payload = { 'key' => "#{user.id}-coin-#{account.id}" }

      result = handler.send(:find_account, payload)
      expect(result).to eq(account)
    end

    it 'returns nil when coin account is not found' do
      handler = described_class.new
      payload = { 'key' => '999-coin-888' }

      result = handler.send(:find_account, payload)
      expect(result).to be_nil
    end

    it 'finds a fiat account with correct parameters' do
      handler = described_class.new
      user = create(:user)
      account = create(:fiat_account, user: user)

      payload = { 'key' => "#{user.id}-fiat-#{account.id}" }

      result = handler.send(:find_account, payload)
      expect(result).to eq(account)
    end

    it 'returns nil when fiat account is not found' do
      handler = described_class.new
      payload = { 'key' => '999-fiat-888' }

      result = handler.send(:find_account, payload)
      expect(result).to be_nil
    end
  end

  describe '#update_account_balance' do
    it 'updates the account with values from payload' do
      handler = described_class.new
      account = create(:coin_account, balance: '1.0', frozen_balance: '0.5')

      payload = {
        'availableBalance' => '5.5',
        'frozenBalance' => '2.2'
      }

      handler.send(:update_account_balance, account, payload)

      account.reload
      expect(account.balance).to eq(BigDecimal('5.5'))
      expect(account.frozen_balance).to eq(BigDecimal('2.2'))
    end
  end
end
