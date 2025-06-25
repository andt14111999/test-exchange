# frozen_string_literal: true

class AccessDevice < ApplicationRecord
  store_accessor :details, :device_type, :browser, :os, :ip, :city, :country

  belongs_to :user

  validates :device_uuid_hash, presence: true
  validates_uniqueness_of :device_uuid_hash, scope: :user_id
  validates :details, presence: true

  def device_uuid=(device_uuid)
    self.device_uuid_hash = self.class.digest(device_uuid)
  end

  def display_name
    "#{browser} (#{os})".strip
  end

  def location
    "#{city}, #{country}"
  end

  def ip_address
    ip
  end

  def self.digest(key)
    Digest::MD5.hexdigest key if key.present?
  end

  def self.find_by_device_uuid(uuid)
    find_by_device_uuid_hash(digest(uuid))
  end

  def mark_as_trusted!
    update!(trusted: true)
  end
end
