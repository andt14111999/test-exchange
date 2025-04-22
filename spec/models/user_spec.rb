# frozen_string_literal: true

require 'rails_helper'

RSpec.describe User, type: :model do
  RSpec::Matchers.define_negated_matcher :not_change, :change

  describe 'associations' do
    it 'has many social accounts' do
      association = described_class.reflect_on_association(:social_accounts)
      expect(association.macro).to eq :has_many
      expect(association.options[:dependent]).to eq :destroy
    end

    it 'has many coin accounts' do
      association = described_class.reflect_on_association(:coin_accounts)
      expect(association.macro).to eq :has_many
      expect(association.options[:dependent]).to eq :destroy
    end

    it 'has many fiat accounts' do
      association = described_class.reflect_on_association(:fiat_accounts)
      expect(association.macro).to eq :has_many
      expect(association.options[:dependent]).to eq :destroy
    end

    it 'has many notifications' do
      association = described_class.reflect_on_association(:notifications)
      expect(association.macro).to eq :has_many
      expect(association.options[:dependent]).to eq :destroy
    end

    it 'has many merchant escrows' do
      association = described_class.reflect_on_association(:merchant_escrows)
      expect(association.macro).to eq :has_many
      expect(association.options[:dependent]).to eq :destroy
      expect(association.options[:inverse_of]).to eq :user
    end

    it 'has many merchant escrow operations through merchant escrows' do
      association = described_class.reflect_on_association(:merchant_escrow_operations)
      expect(association.macro).to eq :has_many
      expect(association.options[:through]).to eq :merchant_escrows
    end

    it 'has many amm_positions' do
      association = described_class.reflect_on_association(:amm_positions)
      expect(association.macro).to eq :has_many
      expect(association.options[:dependent]).to eq :destroy
    end
  end

  describe 'validations' do
    it 'validates presence of email' do
      user = build(:user, email: nil)
      expect(user).to be_invalid
      expect(user.errors[:email]).to include("can't be blank")
    end

    it 'validates uniqueness of email' do
      create(:user, email: 'test@example.com')
      user = build(:user, email: 'test@example.com')
      expect(user).to be_invalid
      expect(user.errors[:email]).to include('has already been taken')
    end

    it 'validates format of email' do
      user = build(:user, email: 'invalid-email')
      expect(user).to be_invalid
      expect(user.errors[:email]).to include('is invalid')
    end

    it 'validates inclusion of role' do
      user = build(:user, role: 'invalid')
      expect(user).to be_invalid
      expect(user.errors[:role]).to include('is not included in the list')
    end

    it 'validates inclusion of status' do
      user = build(:user, status: 'invalid')
      expect(user).to be_invalid
      expect(user.errors[:status]).to include('is not included in the list')
    end

    it 'validates inclusion of kyc_level' do
      user = build(:user, kyc_level: 3)
      expect(user).to be_invalid
      expect(user.errors[:kyc_level]).to include('is not included in the list')
    end
  end

  describe 'scopes' do
    it 'returns merchants' do
      merchant = create(:user, role: 'merchant')
      regular_user = create(:user, role: 'user')
      expect(described_class.merchants).to include(merchant)
      expect(described_class.merchants).not_to include(regular_user)
    end

    it 'returns regular users' do
      merchant = create(:user, role: 'merchant')
      regular_user = create(:user, role: 'user')
      expect(described_class.regular_users).to include(regular_user)
      expect(described_class.regular_users).not_to include(merchant)
    end

    it 'returns active users' do
      active_user = create(:user, status: 'active')
      suspended_user = create(:user, status: 'suspended')
      expect(described_class.active).to include(active_user)
      expect(described_class.active).not_to include(suspended_user)
    end

    it 'returns suspended users' do
      active_user = create(:user, status: 'active')
      suspended_user = create(:user, status: 'suspended')
      expect(described_class.suspended).to include(suspended_user)
      expect(described_class.suspended).not_to include(active_user)
    end

    it 'returns banned users' do
      active_user = create(:user, status: 'active')
      banned_user = create(:user, status: 'banned')
      expect(described_class.banned).to include(banned_user)
      expect(described_class.banned).not_to include(active_user)
    end

    it 'returns phone verified users' do
      verified_user = create(:user, phone_verified: true)
      unverified_user = create(:user, phone_verified: false)
      expect(described_class.phone_verified).to include(verified_user)
      expect(described_class.phone_verified).not_to include(unverified_user)
    end

    it 'returns document verified users' do
      verified_user = create(:user, document_verified: true)
      unverified_user = create(:user, document_verified: false)
      expect(described_class.document_verified).to include(verified_user)
      expect(described_class.document_verified).not_to include(unverified_user)
    end
  end

  describe '.ransackable_attributes' do
    it 'returns allowed attributes for ransack search' do
      expected_attributes = %w[
        avatar_url
        created_at
        display_name
        document_verified
        email
        id
        kyc_level
        phone_verified
        role
        status
        updated_at
      ]

      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe '.ransackable_associations' do
    it 'returns allowed associations for ransack search' do
      expect(described_class.ransackable_associations).to contain_exactly('social_accounts')
    end
  end

  describe '.from_social_auth' do
    context 'when social account exists' do
      it 'returns the associated user' do
        existing_user = create(:user)
        social_account = create(:social_account, user: existing_user)
        auth = OpenStruct.new(
          provider: social_account.provider,
          uid: social_account.provider_user_id
        )

        expect(described_class.from_social_auth(auth)).to eq existing_user
      end
    end

    context 'when social account is new' do
      it 'creates a new user and social account' do
        expires_at = Time.current.to_i
        auth = OpenStruct.new(
          provider: 'google',
          uid: '123456',
          info: OpenStruct.new(
            email: 'new@example.com',
            name: 'New User',
            image: 'https://example.com/avatar.jpg'
          ),
          credentials: OpenStruct.new(
            token: 'access_token',
            refresh_token: 'refresh_token',
            expires_at: expires_at
          ),
          extra: OpenStruct.new(
            raw_info: { 'data' => 'test' }
          )
        )

        account_creation_service = instance_double(AccountCreationService)
        allow(AccountCreationService).to receive(:new).and_return(account_creation_service)
        allow(account_creation_service).to receive(:create_all_accounts)

        expect do
          user = described_class.from_social_auth(auth)
          expect(user).to be_persisted
          expect(user.email).to eq('new@example.com')
          expect(user.display_name).to eq('New User')
          expect(user.avatar_url).to eq('https://example.com/avatar.jpg')
          expect(user.role).to eq('user')
          expect(user.status).to eq('active')

          social_account = user.social_accounts.first
          expect(social_account).to be_persisted
          expect(social_account.provider).to eq('google')
          expect(social_account.provider_user_id).to eq('123456')
          expect(social_account.access_token).to eq('access_token')
          expect(social_account.refresh_token).to eq('refresh_token')
          expect(social_account.token_expires_at).to be_within(1.second).of(Time.zone.at(expires_at))
          expect(social_account.profile_data).to eq({ 'data' => 'test' })
        end.to change(described_class, :count).by(1)
          .and change(SocialAccount, :count).by(1)
      end

      it 'finds existing user by email and creates social account' do
        existing_user = create(:user, email: 'existing@example.com')
        expires_at = Time.current.to_i
        auth = OpenStruct.new(
          provider: 'google',
          uid: '123456',
          info: OpenStruct.new(
            email: existing_user.email,
            name: 'Existing User',
            image: 'https://example.com/avatar.jpg'
          ),
          credentials: OpenStruct.new(
            token: 'access_token',
            refresh_token: 'refresh_token',
            expires_at: expires_at
          ),
          extra: OpenStruct.new(
            raw_info: { 'data' => 'test' }
          )
        )

        expect do
          user = described_class.from_social_auth(auth)
          expect(user).to eq existing_user

          social_account = user.social_accounts.first
          expect(social_account).to be_persisted
          expect(social_account.provider).to eq('google')
          expect(social_account.provider_user_id).to eq('123456')
          expect(social_account.token_expires_at).to be_within(1.second).of(Time.zone.at(expires_at))
        end.to not_change(described_class, :count)
          .and change(SocialAccount, :count).by(1)
      end
    end
  end

  describe '#active?' do
    it 'returns true when status is active' do
      user = build(:user, status: 'active')
      expect(user).to be_active
    end

    it 'returns false when status is not active' do
      user = build(:user, status: 'suspended')
      expect(user).not_to be_active
    end
  end

  describe '#merchant?' do
    it 'returns true when role is merchant' do
      user = build(:user, role: 'merchant')
      expect(user).to be_merchant
    end

    it 'returns false when role is not merchant' do
      user = build(:user, role: 'user')
      expect(user).not_to be_merchant
    end
  end

  describe '#main_account' do
    let(:user) { create(:user) }
    let!(:usdt_account) { create(:coin_account, :main, user: user, coin_currency: 'usdt') }
    let!(:eth_account) { create(:coin_account, :main, user: user, coin_currency: 'eth') }
    let!(:vnd_account) { create(:fiat_account, user: user, currency: 'VND') }

    it 'returns the main coin account for the given currency' do
      expect(user.main_account('usdt')).to eq(usdt_account)
      expect(user.main_account('USDT')).to eq(usdt_account)
    end

    it 'returns the fiat account for the given currency' do
      expect(user.main_account('vnd')).to eq(vnd_account)
      expect(user.main_account('VND')).to eq(vnd_account)
    end

    it 'returns nil when no account exists for the currency' do
      expect(user.main_account('UNKNOWN')).to be_nil
    end
  end

  describe 'callbacks' do
    describe 'after_create' do
      it 'creates default accounts through AccountCreationService', :test_callbacks do
        service_class_spy = class_spy(AccountCreationService).as_stubbed_const
        service_instance = instance_double(AccountCreationService)
        allow(service_class_spy).to receive(:new).and_return(service_instance)
        allow(service_instance).to receive(:create_all_accounts)

        user = create(:user)

        expect(service_class_spy).to have_received(:new).with(user)
        expect(service_instance).to have_received(:create_all_accounts)
      end
    end
  end
end
