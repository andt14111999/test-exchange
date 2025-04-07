require 'rails_helper'

RSpec.describe AddressGenerationService, type: :service do
  subject(:service) { described_class.new(account) }

  before do
    User.skip_callback(:create, :after, :create_default_accounts)
  end

  after do
    User.set_callback(:create, :after, :create_default_accounts)
  end

  describe '#generate in development environment' do
    before do
      allow(Rails).to receive(:env).and_return(ActiveSupport::EnvironmentInquirer.new('development'))
    end

    context 'with erc20 layer' do
      let(:user) { create(:user) }
      let(:account) { create(:coin_account, user: user, coin_currency: 'usdt', layer: 'erc20', account_type: 'deposit') }

      it 'returns a valid ERC20 address' do
        address = service.generate
        expect(address).to match(/^0x[a-f0-9]{40}$/)
      end
    end

    context 'with bep20 layer' do
      let(:user) { create(:user) }
      let(:account) { create(:coin_account, user: user, coin_currency: 'usdt', layer: 'bep20', account_type: 'deposit') }

      it 'returns a valid BEP20 address' do
        address = service.generate
        expect(address).to match(/^0x[a-f0-9]{40}$/)
      end
    end

    context 'with trc20 layer' do
      let(:user) { create(:user) }
      let(:account) { create(:coin_account, user: user, coin_currency: 'usdt', layer: 'trc20', account_type: 'deposit') }

      it 'returns a valid TRC20 address' do
        address = service.generate
        expect(address).to match(/^T[1-9A-HJ-NP-Za-km-z]{33}$/)
      end
    end

    context 'with bitcoin layer' do
      let(:user) { create(:user) }
      let(:account) { create(:coin_account, user: user, coin_currency: 'btc', layer: 'bitcoin', account_type: 'deposit') }

      it 'returns a valid Bitcoin address' do
        address = service.generate
        expect(address).to match(/^1[1-9A-HJ-NP-Za-km-z]{25,34}$/)
      end
    end

    context 'with unknown layer' do
      let(:user) { create(:user) }
      let(:account) { create(:coin_account, user: user, coin_currency: 'usdt', layer: 'erc20', account_type: 'deposit') }

      before do
        allow(account).to receive(:layer).and_return('unknown')
      end

      it 'returns a random hex string' do
        address = service.generate
        expect(address).to match(/^[a-f0-9]{40}$/)
      end
    end
  end

  describe '#generate in production environment' do
    let(:user) { create(:user) }
    let(:account) { create(:coin_account, user: user, coin_currency: 'usdt', layer: 'erc20', account_type: 'deposit') }

    before do
      allow(Rails).to receive(:env).and_return(ActiveSupport::EnvironmentInquirer.new('production'))
    end

    context 'with successful API call' do
      let(:response) { { 'coin_address' => { 'address' => '0x1234567890abcdef' } } }
      let(:postback_service) { instance_double(PostbackService) }

      before do
        allow(PostbackService).to receive(:new).and_return(postback_service)
        allow(postback_service).to receive(:post).and_return(response)
      end

      it 'returns the address from API response' do
        expect(service.generate).to eq('0x1234567890abcdef')
      end
    end

    context 'with failed API call' do
      let(:postback_service) { instance_double(PostbackService) }

      before do
        allow(PostbackService).to receive(:new).and_return(postback_service)
        allow(postback_service).to receive(:post).and_return({})
        allow(Rails.logger).to receive(:error)
      end

      it 'returns nil' do
        expect(service.generate).to be_nil
      end

      it 'logs an error' do
        service.generate
        expect(Rails.logger).to have_received(:error).with("Failed to generate coin address for account #{account.id}")
      end
    end
  end
end
