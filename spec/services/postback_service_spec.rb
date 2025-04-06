require 'rails_helper'

RSpec.describe PostbackService, type: :service do
  describe '#post' do
    context 'when request is successful' do
      it 'sends a signed POST request' do
        target_url = 'https://example.com/api'
        payload = { key: 'value' }
        options = { timeout: 10 }
        service = described_class.new(target_url: target_url, payload: payload, options: options)

        # Mock ENV and signing key
        allow(ENV).to receive(:fetch).with('EXCHANGE_SIGNING_KEY').and_return('a' * 64)
        signing_key = instance_double(Ed25519::SigningKey)
        allow(Ed25519::SigningKey).to receive(:new).and_return(signing_key)
        allow(signing_key).to receive(:sign).and_return('signature')
        allow(HTTParty).to receive(:post).and_return(
          instance_double(HTTParty::Response, success?: true, code: 200, body: '{}')
        )

        response = service.post

        expect(HTTParty).to have_received(:post).with(
          target_url,
          body: payload.to_json,
          headers: hash_including(
            'Content-Type': 'application/json',
            'X-Signature': be_present,
            'X-Timestamp': be_present,
            'X-App-Name': 'exchange'
          ),
          timeout: 10
        )
        expect(response).to be_success
      end
    end

    context 'when request fails' do
      it 'raises RequestError' do
        target_url = 'https://example.com/api'
        payload = { key: 'value' }
        service = described_class.new(target_url: target_url, payload: payload)

        # Mock ENV and signing key
        allow(ENV).to receive(:fetch).with('EXCHANGE_SIGNING_KEY').and_return('a' * 64)
        signing_key = instance_double(Ed25519::SigningKey)
        allow(Ed25519::SigningKey).to receive(:new).and_return(signing_key)
        allow(signing_key).to receive(:sign).and_return('signature')
        allow(HTTParty).to receive(:post).and_return(
          instance_double(HTTParty::Response, success?: false, code: 500, body: 'Internal Server Error')
        )

        expect { service.post }.to raise_error(PostbackService::RequestError)
      end
    end

    context 'when signing key is not set' do
      it 'raises KeyError' do
        target_url = 'https://example.com/api'
        payload = { key: 'value' }
        service = described_class.new(target_url: target_url, payload: payload)

        allow(ENV).to receive(:fetch).with('EXCHANGE_SIGNING_KEY').and_raise(KeyError.new('key not found: "EXCHANGE_SIGNING_KEY"'))

        expect { service.post }.to raise_error(KeyError, 'key not found: "EXCHANGE_SIGNING_KEY"')
      end
    end

    context 'when signing key is invalid' do
      it 'raises ConfigurationError' do
        target_url = 'https://example.com/api'
        payload = { key: 'value' }
        service = described_class.new(target_url: target_url, payload: payload)

        allow(ENV).to receive(:fetch).with('EXCHANGE_SIGNING_KEY').and_return('invalid_key')
        allow(Ed25519::SigningKey).to receive(:new).and_raise(StandardError.new('Invalid key'))

        expect { service.post }.to raise_error(PostbackService::ConfigurationError, 'Invalid ED25519 private key: Invalid key')
      end
    end
  end

  describe '#signing_key' do
    it 'returns the same signing key instance' do
      target_url = 'https://example.com/api'
      payload = { key: 'value' }
      service = described_class.new(target_url: target_url, payload: payload)

      allow(ENV).to receive(:fetch).with('EXCHANGE_SIGNING_KEY').and_return('a' * 64)
      signing_key = instance_double(Ed25519::SigningKey)
      allow(Ed25519::SigningKey).to receive(:new).and_return(signing_key)

      first_key = service.send(:signing_key)
      second_key = service.send(:signing_key)

      expect(first_key).to eq(second_key)
      expect(Ed25519::SigningKey).to have_received(:new).once
    end
  end

  describe '#public_key_hex' do
    it 'returns the public key in hex format' do
      target_url = 'https://example.com/api'
      payload = { key: 'value' }
      service = described_class.new(target_url: target_url, payload: payload)

      verify_key = instance_double(Ed25519::VerifyKey, to_bytes: 'bytes')
      signing_key = instance_double(Ed25519::SigningKey, verify_key: verify_key)
      allow(service).to receive(:signing_key).and_return(signing_key)

      expect(service.send(:public_key_hex)).to eq('6279746573')
    end
  end
end
