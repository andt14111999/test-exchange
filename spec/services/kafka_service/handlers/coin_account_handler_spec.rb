require 'rails_helper'

RSpec.describe KafkaService::Handlers::CoinAccountHandler, type: :service do
  describe '#handle' do
    it 'processes balance update successfully' do
      handler = described_class.new
      user = create(:user)
      payload = {
        'userId' => user.id,
        'coin' => 'BTC',
        'totalBalance' => '10.5',
        'frozenBalance' => '1.5'
      }

      expect { handler.handle(payload) }.to change(CoinAccount, :count).by(1)

      account = CoinAccount.last
      expect(account).to have_attributes(
        user_id: user.id,
        coin_currency: 'btc',
        account_type: 'main',
        layer: 'all'
      )
      expect(account.balance).to eq(BigDecimal('10.5'))
      expect(account.frozen_balance).to eq(BigDecimal('1.5'))
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

      payload = {
        'userId' => user.id,
        'coin' => 'BTC',
        'totalBalance' => '15.5',
        'frozenBalance' => '2.5'
      }

      expect { handler.handle(payload) }.not_to change(CoinAccount, :count)

      existing_account.reload
      expect(existing_account.balance).to eq(BigDecimal('15.5'))
      expect(existing_account.frozen_balance).to eq(BigDecimal('2.5'))
    end

    it 'handles errors gracefully' do
      handler = described_class.new
      payload = {
        'userId' => 'invalid',
        'coin' => 'BTC',
        'totalBalance' => '10.5',
        'frozenBalance' => '1.5'
      }

      expect(Rails.logger).to receive(:error).twice
      expect { handler.handle(payload) }.not_to raise_error
    end

    it 'logs balance update processing' do
      handler = described_class.new
      user = create(:user)
      payload = {
        'userId' => user.id,
        'coin' => 'BTC',
        'totalBalance' => '10.5',
        'frozenBalance' => '1.5'
      }

      expect(Rails.logger).to receive(:info).with("Processing balance update: #{payload}")
      handler.handle(payload)
    end
  end
end
