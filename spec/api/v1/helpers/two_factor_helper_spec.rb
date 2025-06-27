# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Helpers::TwoFactorHelper do
  let(:user) { create(:user, :with_2fa) }
  let(:device_uuid) { SecureRandom.uuid }

  def build_helper(user, params = {}, headers = {})
    klass = Class.new do
      include V1::Helpers::TwoFactorHelper
      include V1::Helpers::DeviceHelper

      define_method(:current_user) { user }
      define_method(:params) { params }
      define_method(:headers) { headers }
      define_method(:request) { OpenStruct.new(env: { 'action_dispatch.remote_ip' => '192.168.1.100' }) }
      define_method(:env) { {} }

      def error!(message, status)
        error = StandardError.new(message.is_a?(Hash) ? message[:message] : message)
        error.define_singleton_method(:status) { status }
        raise error
      end
    end
    klass.new
  end

  describe '#verify_2fa_if_required!' do
    context 'when 2FA is not required' do
      it 'returns true when device is trusted' do
        helper = build_helper(user, {}, { 'Device-Uuid' => device_uuid })
        create(:access_device, :trusted, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))
        expect(helper.verify_2fa_if_required!).to be true
      end
    end

    context 'when 2FA is required' do
             context 'with valid 2FA code' do
         it 'returns true and creates access device' do
           helper = build_helper(user, { two_factor_code: '123456' }, { 'Device-Uuid' => device_uuid })
           allow(user).to receive(:verify_otp).with('123456').and_return(true)

           expect {
             expect(helper.verify_2fa_if_required!).to be true
           }.to change(AccessDevice, :count).by(1)
         end

         it 'does not create duplicate access device' do
           helper = build_helper(user, { two_factor_code: '123456' }, { 'Device-Uuid' => device_uuid })
           allow(user).to receive(:verify_otp).with('123456').and_return(true)
           create(:access_device, user: user, device_uuid_hash: AccessDevice.digest(device_uuid))

           expect {
             expect(helper.verify_2fa_if_required!).to be true
           }.not_to change(AccessDevice, :count)
         end
       end

       context 'with invalid 2FA code' do
         it 'raises error with invalid code' do
           helper = build_helper(user, { two_factor_code: '000000' }, { 'Device-Uuid' => device_uuid })
           allow(user).to receive(:verify_otp).with('000000').and_return(false)

           expect {
             helper.verify_2fa_if_required!
           }.to raise_error(StandardError) do |error|
             expect(error.status).to eq(400)
           end
         end

        it 'raises error when code is missing' do
          helper = build_helper(user, {}, { 'Device-Uuid' => device_uuid })

          expect {
            helper.verify_2fa_if_required!
          }.to raise_error(StandardError) do |error|
            expect(error.status).to eq(400)
          end
        end

        it 'raises error when code is empty' do
          helper = build_helper(user, { two_factor_code: '' }, { 'Device-Uuid' => device_uuid })

          expect {
            helper.verify_2fa_if_required!
          }.to raise_error(StandardError) do |error|
            expect(error.status).to eq(400)
          end
        end
      end

             context 'without device UUID' do
         it 'still validates 2FA but does not create device' do
           helper = build_helper(user, { two_factor_code: '123456' }, {})
           allow(user).to receive(:verify_otp).with('123456').and_return(true)

           expect {
             expect(helper.verify_2fa_if_required!).to be true
           }.not_to change(AccessDevice, :count)
         end
       end
     end
   end
end
