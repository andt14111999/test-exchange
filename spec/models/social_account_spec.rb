# frozen_string_literal: true

require 'rails_helper'

RSpec.describe SocialAccount, type: :model do
  describe 'associations' do
    it { is_expected.to belong_to(:user) }
  end

  describe 'validations' do
    it { is_expected.to validate_presence_of(:provider) }
    it { is_expected.to validate_presence_of(:provider_user_id) }
    it { is_expected.to validate_presence_of(:email) }

    it { is_expected.to validate_inclusion_of(:provider).in_array(%w[google facebook apple]) }

    it { is_expected.to allow_value('user@example.com').for(:email) }
    it { is_expected.not_to allow_value('invalid-email').for(:email) }

    describe 'uniqueness validation' do
      it 'validates uniqueness of provider_user_id scoped to provider' do
        create(:social_account, provider: 'google', provider_user_id: '12345')
        duplicate_account = build(:social_account, provider: 'google', provider_user_id: '12345')

        expect(duplicate_account).to be_invalid
        expect(duplicate_account.errors[:provider_user_id]).to include('has already been taken')
      end
    end
  end

  describe 'scopes' do
    describe '.google' do
      it 'returns google social accounts' do
        google_account = create(:social_account, provider: 'google')
        facebook_account = create(:social_account, provider: 'facebook')

        expect(described_class.google).to include(google_account)
        expect(described_class.google).not_to include(facebook_account)
      end
    end

    describe '.facebook' do
      it 'returns facebook social accounts' do
        google_account = create(:social_account, provider: 'google')
        facebook_account = create(:social_account, provider: 'facebook')

        expect(described_class.facebook).to include(facebook_account)
        expect(described_class.facebook).not_to include(google_account)
      end
    end

    describe '.apple' do
      it 'returns apple social accounts' do
        apple_account = create(:social_account, provider: 'apple')
        facebook_account = create(:social_account, provider: 'facebook')

        expect(described_class.apple).to include(apple_account)
        expect(described_class.apple).not_to include(facebook_account)
      end
    end

    describe '.valid_tokens' do
      it 'returns accounts with non-expired tokens' do
        valid_account = create(:social_account, token_expires_at: 1.day.from_now)
        expired_account = create(:social_account, token_expires_at: 1.day.ago)

        expect(described_class.valid_tokens).to include(valid_account)
        expect(described_class.valid_tokens).not_to include(expired_account)
      end
    end

    describe '.expired_tokens' do
      it 'returns accounts with expired tokens' do
        valid_account = create(:social_account, token_expires_at: 1.day.from_now)
        expired_account = create(:social_account, token_expires_at: 1.day.ago)

        expect(described_class.expired_tokens).to include(expired_account)
        expect(described_class.expired_tokens).not_to include(valid_account)
      end
    end
  end

  describe 'ransackable configuration' do
    describe '.ransackable_attributes' do
      it 'returns the allowed attributes for ransack' do
        expected_attributes = %w[
          avatar_url
          created_at
          email
          id
          name
          provider
          provider_user_id
          token_expires_at
          updated_at
          user_id
        ]

        expect(described_class.ransackable_attributes).to match_array(expected_attributes)
      end
    end

    describe '.ransackable_associations' do
      it 'returns the allowed associations for ransack' do
        expect(described_class.ransackable_associations).to contain_exactly('user')
      end
    end
  end
end
