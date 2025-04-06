require 'rails_helper'

RSpec.describe V1::Auth::Helpers::SocialAccountHandler do
  let(:auth) do
    OpenStruct.new(
      provider: 'google',
      uid: '123456',
      info: OpenStruct.new(
        email: 'test@example.com',
        name: 'Test User',
        image: 'http://example.com/avatar.jpg'
      ),
      credentials: OpenStruct.new(
        token: 'access_token',
        expires_at: 1.week.from_now
      ),
      extra: OpenStruct.new(
        raw_info: { 'some' => 'data' }
      )
    )
  end

  let(:test_class) do
    Class.new do
      include V1::Auth::Helpers::SocialAccountHandler
      attr_accessor :params
    end
  end

  let(:social_account_handler) { test_class.new.tap { |s| s.params = {} } }

  describe '#find_or_create_social_account' do
    context 'when social account exists' do
      let!(:social_account) { create(:social_account, provider: 'google', provider_user_id: '123456') }

      it 'returns existing social account' do
        result = social_account_handler.find_or_create_social_account(auth)
        expect(result).to eq(social_account)
      end
    end

    context 'when social account does not exist and user exists' do
      let!(:user) { create(:user, email: 'test@example.com') }

      it 'creates new social account for existing user' do
        expect {
          result = social_account_handler.find_or_create_social_account(auth)
          expect(result).to be_persisted
          expect(result.user).to eq(user)
          expect(result.provider).to eq('google')
          expect(result.provider_user_id).to eq('123456')
          expect(result.email).to eq('test@example.com')
          expect(result.name).to eq('Test User')
          expect(result.access_token).to eq('access_token')
          expect(result.token_expires_at).to be_within(1.second).of(1.week.from_now)
          expect(result.avatar_url).to eq('http://example.com/avatar.jpg')
          expect(result.profile_data).to eq({ 'some' => 'data' })
        }.to change(SocialAccount, :count).by(1)
      end
    end

    context 'when social account does not exist and user does not exist' do
      it 'creates new user and social account' do
        expect {
          result = social_account_handler.find_or_create_social_account(auth)
          expect(result).to be_persisted
          expect(result.user).to be_persisted
          expect(result.user.email).to eq('test@example.com')
          expect(result.user.display_name).to eq('Test User')
          expect(result.user.avatar_url).to eq('http://example.com/avatar.jpg')
          expect(result.user.role).to eq('user')
          expect(result.user.status).to eq('active')
          expect(result.user.kyc_level).to eq(0)
          expect(result.provider).to eq('google')
          expect(result.provider_user_id).to eq('123456')
          expect(result.email).to eq('test@example.com')
          expect(result.name).to eq('Test User')
          expect(result.access_token).to eq('access_token')
          expect(result.token_expires_at).to be_within(1.second).of(1.week.from_now)
          expect(result.avatar_url).to eq('http://example.com/avatar.jpg')
          expect(result.profile_data).to eq({ 'some' => 'data' })
        }.to change(User, :count).by(1)
          .and change(SocialAccount, :count).by(1)
      end
    end

    context 'when social account does not exist and account_type is merchant' do
      before do
        social_account_handler.params = { account_type: 'merchant' }
      end

      it 'creates new merchant user and social account' do
        expect {
          result = social_account_handler.find_or_create_social_account(auth)
          expect(result.user.role).to eq('merchant')
        }.to change(User, :count).by(1)
          .and change(SocialAccount, :count).by(1)
      end
    end
  end

  describe '#validate_existing_account' do
    let(:social_account) { create(:social_account) }

    it 'returns the social account' do
      result = social_account_handler.validate_existing_account(social_account)
      expect(result).to eq(social_account)
    end
  end

  describe '#create_new_account' do
    context 'when user exists' do
      let!(:user) { create(:user, email: 'test@example.com') }

      it 'creates new social account for existing user' do
        expect {
          result = social_account_handler.create_new_account(auth)
          expect(result).to be_persisted
          expect(result.user).to eq(user)
          expect(result.provider).to eq('google')
          expect(result.provider_user_id).to eq('123456')
          expect(result.email).to eq('test@example.com')
          expect(result.name).to eq('Test User')
          expect(result.access_token).to eq('access_token')
          expect(result.token_expires_at).to be_within(1.second).of(1.week.from_now)
          expect(result.avatar_url).to eq('http://example.com/avatar.jpg')
          expect(result.profile_data).to eq({ 'some' => 'data' })
        }.to change(SocialAccount, :count).by(1)
      end
    end

    context 'when user does not exist' do
      it 'creates new user and social account' do
        expect {
          result = social_account_handler.create_new_account(auth)
          expect(result).to be_persisted
          expect(result.user).to be_persisted
          expect(result.user.email).to eq('test@example.com')
          expect(result.user.display_name).to eq('Test User')
          expect(result.user.avatar_url).to eq('http://example.com/avatar.jpg')
          expect(result.user.role).to eq('user')
          expect(result.user.status).to eq('active')
          expect(result.user.kyc_level).to eq(0)
          expect(result.provider).to eq('google')
          expect(result.provider_user_id).to eq('123456')
          expect(result.email).to eq('test@example.com')
          expect(result.name).to eq('Test User')
          expect(result.access_token).to eq('access_token')
          expect(result.token_expires_at).to be_within(1.second).of(1.week.from_now)
          expect(result.avatar_url).to eq('http://example.com/avatar.jpg')
          expect(result.profile_data).to eq({ 'some' => 'data' })
        }.to change(User, :count).by(1)
          .and change(SocialAccount, :count).by(1)
      end
    end
  end

  describe '#create_new_user' do
    let(:user) { User.new(email: 'test@example.com') }

    it 'creates a new user with correct attributes' do
      social_account_handler.create_new_user(user, auth)

      expect(user).to be_persisted
      expect(user.display_name).to eq('Test User')
      expect(user.avatar_url).to eq('http://example.com/avatar.jpg')
      expect(user.role).to eq('user')
      expect(user.status).to eq('active')
      expect(user.kyc_level).to eq(0)
    end

    context 'when account_type is merchant' do
      before do
        social_account_handler.params = { account_type: 'merchant' }
      end

      it 'creates a new merchant user' do
        social_account_handler.create_new_user(user, auth)
        expect(user.role).to eq('merchant')
      end
    end
  end

  describe '#create_social_account' do
    let(:user) { create(:user) }

    it 'creates a new social account with correct attributes' do
      social_account = social_account_handler.create_social_account(user, auth)

      expect(social_account).to be_persisted
      expect(social_account.user).to eq(user)
      expect(social_account.provider).to eq('google')
      expect(social_account.provider_user_id).to eq('123456')
      expect(social_account.email).to eq('test@example.com')
      expect(social_account.name).to eq('Test User')
      expect(social_account.access_token).to eq('access_token')
      expect(social_account.token_expires_at).to be_within(1.second).of(1.week.from_now)
      expect(social_account.avatar_url).to eq('http://example.com/avatar.jpg')
      expect(social_account.profile_data).to eq({ 'some' => 'data' })
    end
  end
end
