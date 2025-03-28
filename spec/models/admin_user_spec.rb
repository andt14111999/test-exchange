# frozen_string_literal: true

require 'rails_helper'

RSpec.describe AdminUser, type: :model do
  describe 'Validations' do
    it 'is valid with valid attributes' do
      expect(build(:admin_user)).to be_valid
    end

    context 'with presence validations' do
      it { is_expected.to validate_presence_of(:email).with_message("can't be blank") }
    end

    context 'with unique validation' do
      it { is_expected.to validate_uniqueness_of(:email).case_insensitive.with_message('has already been taken') }
    end

    it 'is invalid if password and password_confirmation do not match' do
      admin_user = build(:admin_user, password_confirmation: 'password456')
      expect(admin_user).not_to be_valid
      expect(admin_user.errors[:password_confirmation]).to include("doesn't match Password")
    end

    it 'is invalid with a duplicate email' do
      admin = create(:admin_user)
      duplicate_user = build(:admin_user, email: admin.email)
      expect(duplicate_user).to be_invalid
      expect(duplicate_user.errors[:email]).to include('has already been taken')
    end
  end

  describe 'after create callback' do
    it 'enqueues password reset worker for non-admin users' do
      expect do
        create(:admin_user, roles: 'developer')
      end.to change(AdminUserPasswordResetJob.jobs, :size).by(1)
    end
  end

  describe 'password generation on create' do
    it 'sets random password when creating without password' do
      admin_user = build(:admin_user, password: nil, password_confirmation: nil)
      admin_user.valid?

      expect(admin_user.password).to be_present
      expect(admin_user.password.length).to eq(24) # hex(12) generates 24 characters
      expect(admin_user.password).to eq(admin_user.password_confirmation)
    end

    it 'does not override existing password' do
      existing_password = 'existing_password'
      admin_user = build(:admin_user, password: existing_password, password_confirmation: existing_password)
      admin_user.valid?

      expect(admin_user.password).to eq(existing_password)
    end

    it 'does not set random password on update' do
      admin_user = create(:admin_user)
      original_encrypted_password = admin_user.encrypted_password

      admin_user.update(email: 'new@example.com')

      expect(admin_user.encrypted_password).to eq(original_encrypted_password)
    end
  end

  describe '#assign_authenticator_key' do
    it 'assigns a random base32 key' do
      admin_user = create(:admin_user)
      admin_user.assign_authenticator_key
      expect(admin_user.authenticator_key).to be_present
      expect(admin_user.authenticator_key).to match(/^[A-Z2-7]+=*$/) # Base32 format
    end
  end

  describe '#generate_provisioning_uri' do
    it 'returns empty string when authenticator_key is blank' do
      admin_user = create(:admin_user, authenticator_key: nil)
      expect(admin_user.generate_provisioning_uri).to eq('')
    end

    it 'returns empty string when email is blank' do
      admin_user = build(:admin_user, email: nil)
      admin_user.assign_authenticator_key
      expect(admin_user.generate_provisioning_uri).to eq('')
    end

    it 'generates valid provisioning URI when authenticator_key and email are present' do
      admin_user = create(
        :admin_user,
        email: 'admin@snowfox.com',
        authenticator_key: 'BKXLI7VHSHKIMPVBKJAZGLJ5OOFVWKSR'
      )
      uri = admin_user.generate_provisioning_uri

      start_str = 'otpauth://totp/'
      end_str = '&issuer=SnowFox%20Exchange'
      expect(uri).to eq(
        "#{start_str}SnowFox%20Exchange:admin%40snowfox.com?secret=BKXLI7VHSHKIMPVBKJAZGLJ5OOFVWKSR#{end_str}"
      )
    end
  end

  describe '#disable_authenticator!' do
    it 'disables authenticator and removes key' do
      admin_user = create(:admin_user, authenticator_enabled: true)
      admin_user.assign_authenticator_key
      original_key = admin_user.authenticator_key

      admin_user.disable_authenticator!

      expect(admin_user.authenticator_enabled).to be false
      expect(admin_user.authenticator_key).to be_nil
      expect(admin_user.authenticator_key).not_to eq(original_key)
    end
  end
end
