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
        create(:admin_user, roles: 'operator')
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

  describe 'roles' do
    describe 'validations' do
      it { is_expected.to validate_presence_of(:roles) }

      it 'is invalid with invalid roles' do
        admin_user = build(:admin_user, roles: 'invalid_role')
        expect(admin_user).to be_invalid
        expect(admin_user.errors[:roles]).to include('contains invalid or unrecognized roles')
      end

      it 'is valid with valid roles' do
        admin_user = build(:admin_user, roles: 'superadmin')
        expect(admin_user).to be_valid
      end

      it 'is valid with multiple valid roles' do
        admin_user = build(:admin_user, roles: 'superadmin,operator')
        expect(admin_user).to be_valid
      end
    end

    describe 'role methods' do
      it 'returns true for superadmin? when role is superadmin' do
        admin_user = build(:admin_user, roles: 'superadmin')
        expect(admin_user.superadmin?).to be true
      end

      it 'returns false for superadmin? when role is not superadmin' do
        admin_user = build(:admin_user, roles: 'operator')
        expect(admin_user.superadmin?).to be false
      end

      it 'returns true for operator? when role is operator' do
        admin_user = build(:admin_user, roles: 'operator')
        expect(admin_user.operator?).to be true
      end

      it 'returns false for operator? when role is not operator' do
        admin_user = build(:admin_user, roles: 'superadmin')
        expect(admin_user.operator?).to be false
      end

      it 'returns true for admin? when role is superadmin' do
        admin_user = build(:admin_user, roles: 'superadmin')
        expect(admin_user.admin?).to be true
      end

      it 'returns true for admin? when role is operator' do
        admin_user = build(:admin_user, roles: 'operator')
        expect(admin_user.admin?).to be true
      end

      it 'returns false for admin? when role is invalid' do
        admin_user = build(:admin_user, roles: 'invalid')
        expect(admin_user.admin?).to be false
      end
    end

    describe 'role sanitization' do
      it 'sanitizes roles from string' do
        admin_user = build(:admin_user, roles: 'superadmin,  operator  ,')
        admin_user.valid?
        expect(admin_user.roles).to eq('superadmin,operator')
      end

      it 'handles blank roles' do
        admin_user = build(:admin_user, roles: nil)
        admin_user.valid?
        expect(admin_user.roles).to be_nil
      end
    end
  end

  describe 'ransackable attributes' do
    it 'excludes encrypted_password from ransackable attributes' do
      expect(described_class.disabled_ransackable_attributes).to include('encrypted_password')
    end

    it 'includes deactivated in ransackable attributes' do
      expect(described_class.ransackable_attributes).to include('deactivated')
    end
  end

  describe 'deactivated functionality' do
    describe 'default values' do
      it 'defaults deactivated to false' do
        admin_user = build(:admin_user)
        expect(admin_user.deactivated).to be false
      end
    end

    describe 'scopes' do
      it 'returns active admin users' do
        active_admin = create(:admin_user, deactivated: false)
        create(:admin_user, deactivated: true)

        expect(described_class.active).to include(active_admin)
        expect(described_class.active.count).to eq(1)
      end

      it 'returns deactivated admin users' do
        create(:admin_user, deactivated: false)
        deactivated_admin = create(:admin_user, deactivated: true)

        expect(described_class.deactivated).to include(deactivated_admin)
        expect(described_class.deactivated.count).to eq(1)
      end
    end

    describe '#active?' do
      it 'returns true when not deactivated' do
        admin_user = build(:admin_user, deactivated: false)
        expect(admin_user.active?).to be true
      end

      it 'returns false when deactivated' do
        admin_user = build(:admin_user, deactivated: true)
        expect(admin_user.active?).to be false
      end
    end

    describe '#active_for_authentication?' do
      it 'returns true for active admin user' do
        admin_user = build(:admin_user, deactivated: false)
        expect(admin_user.active_for_authentication?).to be true
      end

      it 'returns false for deactivated admin user' do
        admin_user = build(:admin_user, deactivated: true)
        expect(admin_user.active_for_authentication?).to be false
      end
    end

    describe '#inactive_message' do
      it 'returns :deactivated for deactivated admin user' do
        admin_user = build(:admin_user, deactivated: true)
        expect(admin_user.inactive_message).to eq(:deactivated)
      end

      it 'returns default message for active admin user' do
        admin_user = build(:admin_user, deactivated: false)
        expect(admin_user.inactive_message).not_to eq(:deactivated)
      end
    end
  end

  describe '#verify_otp' do
    let(:admin_user) { create(:admin_user) }
    let(:otp_verifier) { instance_double(OtpVerifier) }

    before do
      allow(OtpVerifier).to receive(:new).with(admin_user).and_return(otp_verifier)
    end

    it 'delegates to otp_verifier' do
      expect(otp_verifier).to receive(:verify_otp).with('123456')
      admin_user.verify_otp('123456')
    end
  end
end
