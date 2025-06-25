# frozen_string_literal: true

module V1
  module Helpers
    module DeviceHelper
      def get_header(key)
        titleized_key = key.titleize.gsub(' ', '-')
        headers[titleized_key] || headers["X-#{titleized_key}"]
      end

      def device_uuid
        @device_uuid ||= get_header('Device-Uuid')
      end

      def device_type
        get_header('Device-Type') || 'web'
      end

      def device_trusted_header
        get_header('Device-Trusted')&.downcase == 'true'
      end

      def client_request_info
        @client_request_info ||=
          begin
            ip = remote_ip
            geo_info = resolve_geo_info(ip)

            {
              device_type: get_header('Device-Type') || 'web', # FE sẽ gửi: ios, android, web
              browser: get_header('Browser'),
              os: get_header('Os'),
              ip: ip,
              country: geo_info[:country],
              city: geo_info[:city]
            }
          end
      end

      def current_access_device
        return nil unless device_uuid.present?

        @current_access_device ||= current_user.access_devices.find_by_device_uuid(device_uuid)
      end

      def create_or_find_access_device
        return nil unless device_uuid.present?

        device = current_access_device
        return device if device

        # Create new device with trusted = false by default
        device = create_access_device

        # Check if client wants to mark device as trusted
        if device && device_trusted_header
          device.mark_as_trusted!
        end

        device
      end

      def device_trusted?
        device = current_access_device
        device&.trusted || false
      end

      def require_2fa_for_action?
        return false unless current_user.authenticator_enabled
        return false if device_trusted?

        true
      end

      private

      def create_access_device
        is_first_device = current_user.access_devices.empty?

        current_user.access_devices.create(
          device_uuid: device_uuid,
          details: client_request_info,
          first_device: is_first_device,
          trusted: false
        )
      end

      def resolve_geo_info(ip)
        return { country: 'Unknown', city: 'Unknown' } if ip.blank? || ip == '127.0.0.1'

        # Sử dụng Geocoder gem hoặc MaxMind GeoIP2
        # gem 'geocoder' hoặc gem 'maxmind-geoip2'
        if defined?(Geocoder)
          result = Geocoder.search(ip).first
          return {
            country: result&.country || 'Unknown',
            city: result&.city || 'Unknown'
          }
        end

        # Fallback nếu không có gem geo
        { country: 'Unknown', city: 'Unknown' }
      rescue => e
        Rails.logger.warn "Failed to resolve geo info for IP #{ip}: #{e.message}"
        { country: 'Unknown', city: 'Unknown' }
      end

      def remote_ip
        # Sử dụng ActionDispatch::RemoteIp như bạn đề xuất
        request.env['action_dispatch.remote_ip'].to_s
      rescue
        # Fallback cho trường hợp không có request object
        env['HTTP_X_FORWARDED_FOR'] || env['HTTP_X_REAL_IP'] || env['REMOTE_ADDR'] || '127.0.0.1'
      end
    end
  end
end
