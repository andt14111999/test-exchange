require 'rails_helper'

RSpec.describe MerchantEscrowService, type: :service do
  describe '#create' do
    it 'creates a new escrow with freeze and mint operations' do
      merchant = create(:user, :merchant)
      usdt_account = merchant.coin_accounts.find_by!(coin_currency: 'usdt', account_type: 'main')
      usdt_account.update!(balance: 1000.0)
      params = { usdt_amount: 100, fiat_currency: 'VND' }

      allow(Setting).to receive(:get_exchange_rate).with('usdt', 'VND').and_return(25000.0)

      service = described_class.new(merchant, params)
      escrow = service.create

      expect(escrow).to be_persisted
      expect(escrow.usdt_amount).to eq(100)
      expect(escrow.fiat_currency).to eq('VND')
      expect(escrow.fiat_amount).to eq(2500000.0)
      expect(escrow.exchange_rate).to eq(25000.0)
      expect(escrow.merchant_escrow_operations.count).to eq(2)
      expect(escrow.merchant_escrow_operations.pluck(:operation_type)).to contain_exactly('freeze', 'mint')
    end

    it 'raises error when user is not a merchant' do
      user = create(:user)
      params = { usdt_amount: 100, fiat_currency: 'VND' }

      service = described_class.new(user, params)

      expect { service.create }.to raise_error('User is not a merchant')
    end
  end

  describe '#cancel' do
    it 'cancels an escrow with unfreeze and burn operations' do
      merchant = create(:user, :merchant)
      usdt_account = merchant.coin_accounts.find_by!(coin_currency: 'usdt', account_type: 'main')
      fiat_account = merchant.fiat_accounts.find_by!(currency: 'VND')
      usdt_account.update!(balance: 1000.0, frozen_balance: 100.0)
      fiat_account.update!(balance: 2500000.0)

      escrow = create(:merchant_escrow,
        user: merchant,
        status: 'active',
        usdt_amount: 100.0,
        fiat_amount: 2500000.0,
        usdt_account: usdt_account,
        fiat_account: fiat_account
      )

      service = described_class.new(merchant)
      result = service.cancel(escrow)

      expect(result).to be_cancelled
      expect(result.merchant_escrow_operations.count).to eq(2)
      expect(result.merchant_escrow_operations.pluck(:operation_type)).to contain_exactly('unfreeze', 'burn')
    end

    it 'raises error when escrow is not found' do
      merchant = create(:user, :merchant)
      service = described_class.new(merchant)

      expect { service.cancel(nil) }.to raise_error('Escrow not found')
    end

    it 'raises error when escrow cannot be cancelled' do
      merchant = create(:user, :merchant)
      escrow = create(:merchant_escrow, user: merchant, status: 'cancelled')
      service = described_class.new(merchant)

      expect { service.cancel(escrow) }.to raise_error('Cannot cancel this escrow')
    end
  end
end
