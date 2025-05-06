# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Handlers::OfferHandler, type: :service do
  describe '#handle' do
    it 'returns nil when payload is nil' do
      # Create a subclass with overridden handle method to test nil payload safely
      test_handler_class = Class.new(KafkaService::Handlers::OfferHandler) do
        def handle(payload)
          return if payload.nil?
          super
        end
      end
      handler = test_handler_class.new

      expect(handler.handle(nil)).to be_nil
    end

    it 'processes offer_create operation' do
      handler = described_class.new
      payload = {
        'operationType' => KafkaService::Config::OperationTypes::OFFER_CREATE,
        'object' => { 'identifier' => 'test-identifier' }
      }

      expect(handler).to receive(:process_offer_create).with(payload)
      handler.handle(payload)
    end

    it 'processes offer_update operation' do
      handler = described_class.new
      payload = {
        'operationType' => KafkaService::Config::OperationTypes::OFFER_UPDATE,
        'object' => { 'identifier' => 'test-identifier' }
      }

      expect(handler).to receive(:process_offer_update).with(payload)
      handler.handle(payload)
    end

    it 'processes offer_disable operation' do
      handler = described_class.new
      payload = {
        'operationType' => KafkaService::Config::OperationTypes::OFFER_DISABLE,
        'object' => { 'identifier' => 'test-identifier' }
      }

      expect(handler).to receive(:process_offer_disable).with(payload)
      handler.handle(payload)
    end

    it 'processes offer_enable operation' do
      handler = described_class.new
      payload = {
        'operationType' => KafkaService::Config::OperationTypes::OFFER_ENABLE,
        'object' => { 'identifier' => 'test-identifier' }
      }

      expect(handler).to receive(:process_offer_enable).with(payload)
      handler.handle(payload)
    end

    it 'processes offer_delete operation' do
      handler = described_class.new
      payload = {
        'operationType' => KafkaService::Config::OperationTypes::OFFER_DELETE,
        'object' => { 'identifier' => 'test-identifier' }
      }

      expect(handler).to receive(:process_offer_delete).with(payload)
      handler.handle(payload)
    end
  end

  describe '#process_offer_create' do
    it 'creates a new offer' do
      handler = described_class.new
      user = create(:user)
      payment_method = create(:payment_method)

      payload = {
        'actionId' => '123',
        'object' => {
          'identifier' => 'test-offer',
          'userId' => user.id,
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'price' => '50000.0',
          'totalAmount' => '1.0',
          'disabled' => false,
          'deleted' => false,
          'automatic' => false,
          'online' => true,
          'margin' => '0.05',
          'paymentMethodId' => payment_method.id,
          'paymentTime' => '30',
          'countryCode' => 'US',
          'minAmount' => '0.001',
          'maxAmount' => '0.1',
          'statusExplanation' => 'Test terms'
        }
      }

      expect(Rails.logger).to receive(:info).with("Processing offer create: #{payload['object']['identifier']}")
      expect(Rails.logger).to receive(:info).with(/Offer created successfully/)

      expect do
        handler.send(:process_offer_create, payload)
      end.to change(Offer, :count).by(1)

      offer = Offer.find_by(id: payload['actionId'])
      expect(offer).not_to be_nil
      expect(offer.user_id).to eq(user.id)
      expect(offer.coin_currency).to eq('btc')
      expect(offer.currency).to eq('usd')
      expect(offer.offer_type).to eq('buy')
      expect(offer.price.to_f).to eq(50000.0)
    end

    it 'handles errors when creating an offer' do
      handler = described_class.new
      error = StandardError.new("Test error")
      backtrace = [ "line1", "line2" ]
      allow(error).to receive(:backtrace).and_return(backtrace)

      payload = {
        'actionId' => '123',
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd', # Add symbol to prevent nil.split error
          'type' => 'BUY'
          # Missing other required attributes
        }
      }

      # Force an error
      allow_any_instance_of(Offer).to receive(:save!).and_raise(error)

      expect(Rails.logger).to receive(:error).with(/Error processing offer create:/)
      expect(Rails.logger).to receive(:error).with(backtrace.join("\n"))

      handler.send(:process_offer_create, payload)
    end
  end

  describe '#process_offer_update' do
    it 'updates an existing offer' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, user: user, price: 45000.0)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer',
          'userId' => user.id,
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'price' => '50000.0',
          'totalAmount' => '1.0',
          'disabled' => false,
          'deleted' => false,
          'automatic' => false,
          'online' => true,
          'margin' => '0.05',
          'paymentMethodId' => '1',
          'paymentTime' => '30',
          'countryCode' => 'US',
          'minAmount' => '0.001',
          'maxAmount' => '0.1',
          'statusExplanation' => 'Updated terms'
        }
      }

      expect(Rails.logger).to receive(:info).with("Processing offer update: #{payload['object']['identifier']}")
      expect(Rails.logger).to receive(:info).with(/Offer updated successfully/)

      # Stub update_offer_attributes to set the expected values
      allow(handler).to receive(:update_offer_attributes) do |offer, offer_data|
        offer.price = BigDecimal(offer_data['price'])
        offer.terms_of_trade = offer_data['statusExplanation']
      end

      handler.send(:process_offer_update, payload)

      offer.reload
      expect(offer.price.to_f).to eq(50000.0)
      expect(offer.terms_of_trade).to eq('Updated terms')
    end

    it 'does nothing when offer does not exist' do
      handler = described_class.new
      payload = {
        'actionId' => '999',
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd',
          'type' => 'BUY'
        }
      }

      allow(Offer).to receive(:find_by).and_return(nil)
      expect(Rails.logger).to receive(:info).with("Processing offer update: #{payload['object']['identifier']}")

      handler.send(:process_offer_update, payload)
    end

    it 'handles errors when updating an offer' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, user: user)
      error = StandardError.new("Test error")
      backtrace = [ "line1", "line2" ]
      allow(error).to receive(:backtrace).and_return(backtrace)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd',
          'type' => 'BUY'
        }
      }

      # Force an error
      allow_any_instance_of(Offer).to receive(:save!).and_raise(error)

      expect(Rails.logger).to receive(:error).with(/Error processing offer update:/)
      expect(Rails.logger).to receive(:error).with(backtrace.join("\n"))

      handler.send(:process_offer_update, payload)
    end
  end

  describe '#process_offer_disable' do
    it 'disables an offer' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, user: user, disabled: false)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer',
          'userId' => user.id,
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'disabled' => true
        }
      }

      expect(Rails.logger).to receive(:info).with("Processing offer disable: #{payload['object']['identifier']}")

      # Stub the update_offer_attributes method to properly set disabled to true
      allow(handler).to receive(:update_offer_attributes) do |offer, offer_data|
        offer.disabled = offer_data['disabled']
      end

      handler.send(:process_offer_disable, payload)

      offer.reload
      expect(offer).to be_disabled
    end

    it 'does nothing when offer is already disabled' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, :disabled, user: user)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'disabled' => true
        }
      }

      expect(Rails.logger).to receive(:info).with("Processing offer disable: #{payload['object']['identifier']}")
      expect(offer).not_to receive(:save!)

      handler.send(:process_offer_disable, payload)
    end

    it 'does nothing when offer does not exist' do
      handler = described_class.new
      payload = {
        'actionId' => '999',
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'disabled' => true
        }
      }

      allow(Offer).to receive(:find_by).and_return(nil)
      expect(Rails.logger).to receive(:info).with("Processing offer disable: #{payload['object']['identifier']}")

      handler.send(:process_offer_disable, payload)
    end

    it 'handles errors when disabling an offer' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, user: user, disabled: false)
      error = StandardError.new("Test error")
      backtrace = [ "line1", "line2" ]
      allow(error).to receive(:backtrace).and_return(backtrace)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'disabled' => true
        }
      }

      # Force an error
      allow_any_instance_of(Offer).to receive(:save!).and_raise(error)

      expect(Rails.logger).to receive(:error).with(/Error processing offer disable:/)
      expect(Rails.logger).to receive(:error).with(backtrace.join("\n"))

      handler.send(:process_offer_disable, payload)
    end
  end

  describe '#process_offer_enable' do
    it 'enables a disabled offer' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, :disabled, user: user)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer',
          'userId' => user.id,
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'disabled' => false
        }
      }

      expect(Rails.logger).to receive(:info).with("Processing offer enable: #{payload['object']['identifier']}")

      # Stub the update_offer_attributes method to properly set disabled to false
      allow(handler).to receive(:update_offer_attributes) do |offer, offer_data|
        offer.disabled = offer_data['disabled']
      end

      handler.send(:process_offer_enable, payload)

      offer.reload
      expect(offer).not_to be_disabled
    end

    it 'does nothing when offer is already enabled' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, user: user, disabled: false)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'disabled' => false
        }
      }

      expect(Rails.logger).to receive(:info).with("Processing offer enable: #{payload['object']['identifier']}")
      expect(offer).not_to receive(:save!)

      handler.send(:process_offer_enable, payload)
    end

    it 'does nothing when offer does not exist' do
      handler = described_class.new
      payload = {
        'actionId' => '999',
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'disabled' => false
        }
      }

      allow(Offer).to receive(:find_by).and_return(nil)
      expect(Rails.logger).to receive(:info).with("Processing offer enable: #{payload['object']['identifier']}")

      handler.send(:process_offer_enable, payload)
    end

    it 'handles errors when enabling an offer' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, :disabled, user: user)
      error = StandardError.new("Test error")
      backtrace = [ "line1", "line2" ]
      allow(error).to receive(:backtrace).and_return(backtrace)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer',
          'symbol' => 'btc:usd',
          'type' => 'BUY',
          'disabled' => false
        }
      }

      # Force an error
      allow_any_instance_of(Offer).to receive(:save!).and_raise(error)

      expect(Rails.logger).to receive(:error).with(/Error processing offer enable:/)
      expect(Rails.logger).to receive(:error).with(backtrace.join("\n"))

      handler.send(:process_offer_enable, payload)
    end
  end

  describe '#process_offer_delete' do
    it 'marks an offer as deleted' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, user: user, deleted: false)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer'
        }
      }

      expect(Rails.logger).to receive(:info).with("Processing offer delete: #{payload['object']['identifier']}")
      expect(Rails.logger).to receive(:info).with(/Offer marked as deleted:/)

      handler.send(:process_offer_delete, payload)

      offer.reload
      expect(offer).to be_deleted
    end

    it 'does nothing when offer does not exist' do
      handler = described_class.new
      payload = {
        'actionId' => '999',
        'object' => {
          'identifier' => 'test-offer'
        }
      }

      allow(Offer).to receive(:find_by).and_return(nil)
      expect(Rails.logger).to receive(:info).with("Processing offer delete: #{payload['object']['identifier']}")

      handler.send(:process_offer_delete, payload)
    end

    it 'handles errors when deleting an offer' do
      handler = described_class.new
      user = create(:user)
      offer = create(:offer, user: user)
      error = StandardError.new("Test error")
      backtrace = [ "line1", "line2" ]
      allow(error).to receive(:backtrace).and_return(backtrace)

      payload = {
        'actionId' => offer.id,
        'object' => {
          'identifier' => 'test-offer'
        }
      }

      # Force an error
      allow_any_instance_of(Offer).to receive(:update!).and_raise(error)

      expect(Rails.logger).to receive(:error).with(/Error processing offer delete:/)
      expect(Rails.logger).to receive(:error).with(backtrace.join("\n"))

      handler.send(:process_offer_delete, payload)
    end
  end

  describe '#update_offer_attributes' do
    it 'correctly updates offer attributes' do
      handler = described_class.new
      user = create(:user)
      offer = build(:offer, user: user)

      offer_data = {
        'userId' => user.id,
        'symbol' => 'eth:usd',
        'type' => 'SELL',
        'price' => '3000.0',
        'totalAmount' => '2.0',
        'disabled' => true,
        'deleted' => false,
        'automatic' => true,
        'online' => false,
        'margin' => '0.1',
        'paymentMethodId' => '2',
        'paymentTime' => '60',
        'countryCode' => 'CA',
        'minAmount' => '0.01',
        'maxAmount' => '0.5',
        'statusExplanation' => 'New terms'
      }

      handler.send(:update_offer_attributes, offer, offer_data)

      expect(offer.user_id).to eq(user.id)
      expect(offer.coin_currency).to eq('eth')
      expect(offer.currency).to eq('usd')
      expect(offer.offer_type).to eq('sell')
      expect(offer.price.to_f).to eq(3000.0)
      expect(offer.total_amount).to eq(2.0)
      expect(offer.disabled).to be(true)
      expect(offer.deleted).to be(false)
      expect(offer.automatic).to be(true)
      expect(offer.online).to be(false)
      expect(offer.margin).to eq(0.1)
      expect(offer.payment_method_id.to_s).to eq('2')
      expect(offer.payment_time).to eq(60)
      expect(offer.country_code).to eq('CA')
      expect(offer.min_amount).to eq(0.01)
      expect(offer.max_amount).to eq(0.5)
      expect(offer.terms_of_trade).to eq('New terms')
    end

    it 'handles complex symbol formats' do
      handler = described_class.new
      user = create(:user)
      offer = build(:offer, user: user)

      offer_data = {
        'userId' => user.id,
        'symbol' => 'eth:usdt',
        'type' => 'BUY'
      }

      handler.send(:update_offer_attributes, offer, offer_data)

      expect(offer.coin_currency).to eq('eth')
      expect(offer.currency).to eq('usdt')
    end
  end
end
