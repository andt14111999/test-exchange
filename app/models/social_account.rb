# frozen_string_literal: true

class SocialAccount < ApplicationRecord
  belongs_to :user

  validates :provider, presence: true, inclusion: { in: %w[google facebook apple] }
  validates :provider_user_id, presence: true
  validates :email, presence: true, format: { with: URI::MailTo::EMAIL_REGEXP }
  validates :provider_user_id, uniqueness: { scope: :provider }

  # Scopes
  scope :google, -> { where(provider: 'google') }
  scope :facebook, -> { where(provider: 'facebook') }
  scope :apple, -> { where(provider: 'apple') }
  scope :valid_tokens, -> { where('token_expires_at > ?', Time.current) }
  scope :expired_tokens, -> { where(token_expires_at: ..Time.current) }

  # Ransack configuration
  def self.ransackable_attributes(_auth_object = nil)
    %w[
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
  end

  def self.ransackable_associations(_auth_object = nil)
    %w[user]
  end
end
