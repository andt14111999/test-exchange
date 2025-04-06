# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Auth::Helpers::JwtHelper, type: :helper do
  let(:helper_class) do
    Class.new do
      include V1::Auth::Helpers::JwtHelper
    end
  end

  let(:helper) { helper_class.new }
  let(:user) { create(:user) }

  describe '#generate_jwt_token' do
    it 'generates a valid JWT token' do
      token = helper.generate_jwt_token(user)

      expect(token).to be_a(String)
      expect(token.split('.').length).to eq(3) # JWT has 3 parts: header, payload, signature

      decoded_token = JWT.decode(token, Rails.application.secret_key_base, true, { algorithm: 'HS256' }).first
      expect(decoded_token['user_id']).to eq(user.id)
      expect(decoded_token['email']).to eq(user.email)
      expect(decoded_token['exp']).to be > Time.zone.now.to_i
    end
  end
end
