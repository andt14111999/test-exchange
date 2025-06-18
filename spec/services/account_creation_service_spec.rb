# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AccountCreationService, type: :service do
  describe '#create_all_accounts' do
    it 'creates all necessary accounts for a user' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.create_all_accounts }.to change(CoinAccount, :count).by(13)
        .and change(FiatAccount, :count).by(3)
    end
  end

  describe '#create_base_account' do
    it 'creates a base account for valid network' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.create_base_account('btc', 'bitcoin') }.to change(CoinAccount, :count).by(1)
    end

    it 'does not create account for invalid network' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.create_base_account('invalid', 'bitcoin') }.not_to change(CoinAccount, :count)
    end

    it 'does not create account if it already exists' do
      user = create(:user)
      create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin', account_type: 'deposit')
      service = described_class.new(user)

      expect { service.create_base_account('btc', 'bitcoin') }.not_to change(CoinAccount, :count)
    end
  end

  describe '#create_token_account' do
    it 'creates a token account for valid network' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.create_token_account('usdt', 'erc20') }.to change(CoinAccount, :count).by(1)
    end

    it 'does not create account for invalid network' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.create_token_account('invalid', 'erc20') }.not_to change(CoinAccount, :count)
    end

    it 'does not create account if it already exists' do
      user = create(:user)
      create(:coin_account, user: user, coin_currency: 'usdt', layer: 'erc20', account_type: 'deposit')
      service = described_class.new(user)

      expect { service.create_token_account('usdt', 'erc20') }.not_to change(CoinAccount, :count)
    end
  end

  describe '#create_fiat_accounts' do
    it 'creates fiat accounts for supported currencies when SUPPORTED_CURRENCIES is a hash' do
      user = create(:user)
      service = described_class.new(user)
      supported_currencies = { 'VND' => 'Vietnam Dong', 'PHP' => 'Philippine Peso', 'NGN' => 'Nigerian Naira' }
      stub_const('FiatAccount::SUPPORTED_CURRENCIES', supported_currencies)

      expect { service.create_fiat_accounts }.to change(FiatAccount, :count).by(3)
    end

    it 'creates fiat accounts for supported currencies when SUPPORTED_CURRENCIES is an array' do
      user = create(:user)
      service = described_class.new(user)
      supported_currencies = [ 'VND', 'PHP', 'NGN' ]
      stub_const('FiatAccount::SUPPORTED_CURRENCIES', supported_currencies)

      expect { service.create_fiat_accounts }.to change(FiatAccount, :count).by(3)
    end
  end

  describe '#create_main_accounts' do
    it 'creates main accounts for supported networks' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.send(:create_main_accounts) }.to change(CoinAccount, :count).by(4)
    end
  end

  describe '#create_deposit_accounts' do
    it 'creates deposit accounts for supported networks' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.send(:create_deposit_accounts) }.to change(CoinAccount, :count).by(9)
    end

    it 'does not create duplicate deposit accounts' do
      user = create(:user)
      create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin', account_type: 'deposit')
      service = described_class.new(user)

      expect { service.send(:create_deposit_accounts) }.to change(CoinAccount, :count).by(8)
    end
  end

  describe '#create_main_account' do
    it 'creates a main account and notifies kafka' do
      user = create(:user)
      service = described_class.new(user)
      kafka_service = instance_double(KafkaService::Services::Coin::CoinAccountService, create: true)
      allow(KafkaService::Services::Coin::CoinAccountService).to receive(:new).and_return(kafka_service)

      expect { service.send(:create_main_account, 'btc') }.to change(CoinAccount, :count).by(1)
      expect(KafkaService::Services::Coin::CoinAccountService).to have_received(:new)
    end
  end

  describe '#create_deposit_account' do
    it 'creates a deposit account' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.send(:create_deposit_account, 'btc', 'bitcoin') }.to change(CoinAccount, :count).by(1)
    end
  end

  describe '#create_fiat_account' do
    it 'creates a fiat account' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.send(:create_fiat_account, 'VND') }.to change(FiatAccount, :count).by(1)
    end
  end

  describe '#create_account' do
    it 'creates a coin account with specified attributes' do
      user = create(:user)
      service = described_class.new(user)

      expect { service.send(:create_account, 'btc', 'all', 'main') }.to change(CoinAccount, :count).by(1)
      account = CoinAccount.last
      expect(account.coin_currency).to eq('btc')
      expect(account.layer).to eq('all')
      expect(account.account_type).to eq('main')
    end
  end

  describe '#valid_network?' do
    it 'returns true for valid network' do
      user = create(:user)
      service = described_class.new(user)

      expect(service.send(:valid_network?, 'btc', 'bitcoin')).to be true
    end

    it 'returns false for invalid network' do
      user = create(:user)
      service = described_class.new(user)

      expect(service.send(:valid_network?, 'invalid', 'bitcoin')).to be false
    end
  end

  describe '#valid_token_network?' do
    it 'returns true for valid token network' do
      user = create(:user)
      service = described_class.new(user)
      allow(NetworkConfigurationService).to receive(:is_base_network?).with('usdt', 'erc20').and_return(false)

      expect(service.send(:valid_token_network?, 'usdt', 'erc20')).to be true
    end

    it 'returns false for invalid token network' do
      user = create(:user)
      service = described_class.new(user)

      expect(service.send(:valid_token_network?, 'invalid', 'erc20')).to be false
    end

    it 'returns false for base network' do
      user = create(:user)
      service = described_class.new(user)
      allow(NetworkConfigurationService).to receive(:is_base_network?).with('btc', 'bitcoin').and_return(true)

      expect(service.send(:valid_token_network?, 'btc', 'bitcoin')).to be false
    end
  end

  describe '#account_exists?' do
    it 'returns true if account exists' do
      user = create(:user)
      create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin', account_type: 'deposit')
      service = described_class.new(user)

      expect(service.send(:account_exists?, 'btc', 'bitcoin')).to be true
    end

    it 'returns false if account does not exist' do
      user = create(:user)
      service = described_class.new(user)

      expect(service.send(:account_exists?, 'btc', 'bitcoin')).to be false
    end
  end

  describe '#notify_kafka_service' do
    it 'notifies kafka service about account creation' do
      user = create(:user)
      account = create(:coin_account, user: user)
      service = described_class.new(user)
      kafka_service = instance_double(KafkaService::Services::Coin::CoinAccountService, create: true)
      allow(KafkaService::Services::Coin::CoinAccountService).to receive(:new).and_return(kafka_service)

      # Test the notify_coin_kafka_service method which is the actual method in the service
      service.send(:notify_coin_kafka_service, account)

      expect(KafkaService::Services::Coin::CoinAccountService).to have_received(:new)
      expect(kafka_service).to have_received(:create).with(
        user_id: user.id,
        coin: account.coin_currency,
        account_id: account.id
      )
    end
  end
end
