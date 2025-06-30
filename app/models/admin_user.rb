# frozen_string_literal: true

class AdminUser < ApplicationRecord
  include Ransackable
  # Include default devise modules. Others available are:
  # :confirmable, :lockable, :timeoutable, :trackable and :omniauthable
  devise :database_authenticatable, :recoverable, :rememberable, :validatable

  ROLES = %w[superadmin operator].freeze

  before_validation :sanitize_roles
  before_validation :set_random_password, on: :create

  validates :email, presence: true, uniqueness: true
  validates :roles, presence: true
  validate :validate_roles

  after_commit :schedule_password_reset, on: :create
  encrypts :authenticator_key
  delegate :verify_otp, to: :otp_verifier

  scope :active, -> { where(deactivated: false) }
  scope :deactivated, -> { where(deactivated: true) }

  def self.disabled_ransackable_attributes
    %w[encrypted_password]
  end

  def self.ransackable_attributes(auth_object = nil)
    super + %w[deactivated]
  end

  def active?
    !deactivated?
  end

  # Devise method to check if user can authenticate
  # Deactivated users cannot login
  def active_for_authentication?
    super && !deactivated?
  end

  # Devise method to customize the message for inactive users
  def inactive_message
    deactivated? ? :deactivated : super
  end

  def superadmin?
    role?('superadmin')
  end

  def operator?
    role?('operator')
  end

  def admin?
    superadmin? || operator?
  end

  ROLES.each do |role|
    define_method(:"#{role}?") do
      role?(role)
    end
  end

  def assign_authenticator_key
    self.authenticator_key = ROTP::Base32.random_base32
  end

  def generate_provisioning_uri
    return '' if authenticator_key.blank? || email.blank?

    ROTP::TOTP.new(authenticator_key, issuer: 'SnowFox Exchange').provisioning_uri(email)
  end

  def disable_authenticator!
    self.authenticator_enabled = false
    self.authenticator_key = nil
  end

  private

  def role?(role)
    roles.split(',').include?(role)
  end

  def sanitize_roles
    return if roles.blank?

    self.roles = if roles.is_a?(Array)
                   roles.filter_map(&:strip).reject(&:empty?).join(',')
    else
                   roles.split(',').map(&:strip).reject(&:empty?).join(',')
    end
  end

  def validate_roles
    return unless roles.present? && !roles.split(',').all? { |role| ROLES.include?(role.strip) }

    errors.add(:roles, 'contains invalid or unrecognized roles')
  end

  def schedule_password_reset
    AdminUserPasswordResetJob.perform_async(id)
  end

  def set_random_password
    return if password.present?

    self.password = self.password_confirmation = SecureRandom.hex(12)
  end

  def otp_verifier
    @otp_verifier ||= OtpVerifier.new(self)
  end
end
