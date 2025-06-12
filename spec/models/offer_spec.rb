# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Offer, type: :model do
  describe 'associations' do
    it 'belongs to user' do
      expect(described_class.reflect_on_association(:user).macro).to eq :belongs_to
    end

    it 'belongs to payment method optionally' do
      association = described_class.reflect_on_association(:payment_method)
      expect(association.macro).to eq :belongs_to
      expect(association.options[:optional]).to be true
    end

    it 'has many trades' do
      association = described_class.reflect_on_association(:trades)
      expect(association.macro).to eq :has_many
      expect(association.options[:dependent]).to eq :nullify
    end
  end

  describe 'validations' do
    it 'validates presence of user_id' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:user_id]).to include("can't be blank")
    end

    it 'validates presence of offer_type' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:offer_type]).to include("can't be blank")
    end

    it 'validates inclusion of offer_type in OFFER_TYPES' do
      offer = described_class.new(offer_type: 'invalid_type')
      offer.valid?
      expect(offer.errors[:offer_type]).to include("is not included in the list")
    end

    it 'validates presence of coin_currency' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:coin_currency]).to include("can't be blank")
    end

    it 'validates presence of currency' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:currency]).to include("can't be blank")
    end

    it 'validates presence and numericality of price' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:price]).to include("can't be blank")

      offer.price = 0
      offer.valid?
      expect(offer.errors[:price]).to include("must be greater than 0")
    end

    it 'validates presence and numericality of min_amount' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:min_amount]).to include("can't be blank")

      offer.min_amount = 0
      offer.valid?
      expect(offer.errors[:min_amount]).to include("must be greater than 0")
    end

    it 'validates presence and numericality of max_amount' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:max_amount]).to include("can't be blank")

      offer.max_amount = 0
      offer.valid?
      expect(offer.errors[:max_amount]).to include("must be greater than 0")
    end

    it 'validates presence and numericality of total_amount' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:total_amount]).to include("can't be blank")

      offer.total_amount = 0
      offer.valid?
      expect(offer.errors[:total_amount]).to include("must be greater than 0")
    end

    it 'validates numericality of payment_time when present' do
      offer = described_class.new(payment_time: 0)
      offer.valid?
      expect(offer.errors[:payment_time]).to include("must be greater than 0")

      offer.payment_time = 1.5
      offer.valid?
      expect(offer.errors[:payment_time]).to include("must be an integer")
    end

    it 'validates presence of country_code' do
      offer = described_class.new
      offer.valid?
      expect(offer.errors[:country_code]).to include("can't be blank")
    end

    it 'validates min_amount is less than max_amount' do
      offer = described_class.new(min_amount: 100, max_amount: 50)
      offer.valid?
      expect(offer.errors[:min_amount]).to include("must be less than or equal to max amount")
    end

    it 'validates max_amount is less than total_amount' do
      offer = described_class.new(max_amount: 100, total_amount: 50)
      offer.valid?
      expect(offer.errors[:max_amount]).to include("must be less than or equal to total amount")
    end

    it 'validates schedule_start_time is before schedule_end_time' do
      offer = described_class.new(
        schedule_start_time: Time.zone.now + 2.hours,
        schedule_end_time: Time.zone.now
      )
      offer.valid?
      expect(offer.errors[:schedule_start_time]).to include("must be earlier than end time")
    end
  end

  describe 'scopes' do
    it 'has an active scope' do
      expect(described_class).to respond_to(:active)
    end

    it 'has a disabled scope' do
      expect(described_class).to respond_to(:disabled)
    end

    it 'has a deleted scope' do
      expect(described_class).to respond_to(:deleted)
    end

    it 'has a scheduled scope' do
      expect(described_class).to respond_to(:scheduled)
    end

    it 'has a currently_active scope' do
      expect(described_class).to respond_to(:currently_active)
    end

    describe 'currently_active scope' do
      let(:user) { create(:user) }

      # Test the SQL definition of the scope directly
      it 'has the correct definition' do
        current_time = Time.zone.now
        allow(Time.zone).to receive(:now).and_return(current_time)

        # Create a relation matching what the scope should do
        expected_relation = described_class.active.where(
          '(schedule_start_time IS NULL AND schedule_end_time IS NULL) OR ' \
          '((schedule_start_time IS NULL OR schedule_start_time <= ?) AND ' \
          '(schedule_end_time IS NULL OR schedule_end_time >= ?))',
          current_time, current_time
        )

        # Extract the SQL from both relations
        expected_sql = expected_relation.to_sql
        actual_sql = described_class.currently_active.to_sql

        # Compare SQL equality (ignoring minor whitespace differences)
        expect(actual_sql.gsub(/\s+/, ' ')).to eq(expected_sql.gsub(/\s+/, ' '))
      end

      # Test basic expected behavior without relying on specific SQL implementation
      it 'includes active standard offers and excludes disabled/deleted offers' do
        active_offer = create(:offer, user: user, disabled: false, deleted: false)
        disabled_offer = create(:offer, user: user, disabled: true)
        deleted_offer = create(:offer, user: user, deleted: true)

        expect(described_class.currently_active).to include(active_offer)
        expect(described_class.currently_active).not_to include(disabled_offer)
        expect(described_class.currently_active).not_to include(deleted_offer)
      end

      it 'includes active offer with no schedule times' do
        active_offer = create(:offer, user: user, disabled: false, deleted: false)
        expect(described_class.currently_active).to include(active_offer)
      end

      it 'includes active offer when current time is after schedule_start_time with no end time' do
        offer = create(:offer,
          user: user,
          disabled: false,
          deleted: false,
          schedule_start_time: Time.zone.now - 1.hour,
          schedule_end_time: nil
        )
        expect(described_class.currently_active).to include(offer)
      end

      it 'includes active offer when current time is before schedule_end_time with no start time' do
        offer = create(:offer,
          user: user,
          disabled: false,
          deleted: false,
          schedule_start_time: nil,
          schedule_end_time: Time.zone.now + 1.hour
        )
        expect(described_class.currently_active).to include(offer)
      end

      it 'includes active offer when current time is between schedule times' do
        offer = create(:offer,
          user: user,
          disabled: false,
          deleted: false,
          schedule_start_time: Time.zone.now - 1.hour,
          schedule_end_time: Time.zone.now + 1.hour
        )
        expect(described_class.currently_active).to include(offer)
      end

      it 'excludes active offer when current time is before schedule_start_time' do
        offer = create(:offer,
          user: user,
          disabled: false,
          deleted: false,
          schedule_start_time: Time.zone.now + 1.hour,
          schedule_end_time: Time.zone.now + 2.hours
        )
        expect(described_class.currently_active).not_to include(offer)
      end

      it 'excludes active offer when current time is after schedule_end_time' do
        offer = create(:offer,
          user: user,
          disabled: false,
          deleted: false,
          schedule_start_time: Time.zone.now - 2.hours,
          schedule_end_time: Time.zone.now - 1.hour
        )
        expect(described_class.currently_active).not_to include(offer)
      end
    end

    it 'has a buy_offers scope' do
      expect(described_class).to respond_to(:buy_offers)
    end

    it 'has a sell_offers scope' do
      expect(described_class).to respond_to(:sell_offers)
    end

    it 'has an online scope' do
      expect(described_class).to respond_to(:online)
    end

    it 'has an offline scope' do
      expect(described_class).to respond_to(:offline)
    end

    it 'has an automatic scope' do
      expect(described_class).to respond_to(:automatic)
    end

    it 'has a manual scope' do
      expect(described_class).to respond_to(:manual)
    end

    it 'has an of_currency scope' do
      expect(described_class).to respond_to(:of_currency)
    end

    it 'has an of_coin_currency scope' do
      expect(described_class).to respond_to(:of_coin_currency)
    end

    it 'has an of_country scope' do
      expect(described_class).to respond_to(:of_country)
    end

    it 'has a price_asc scope' do
      expect(described_class).to respond_to(:price_asc)
    end

    it 'has a price_desc scope' do
      expect(described_class).to respond_to(:price_desc)
    end

    it 'has a newest_first scope' do
      expect(described_class).to respond_to(:newest_first)
    end

    it 'has an oldest_first scope' do
      expect(described_class).to respond_to(:oldest_first)
    end
  end

  describe 'callbacks' do
    it 'ensures bank_names is an array before save' do
      offer = described_class.new(bank_names: nil)
      offer.send(:ensure_bank_names_array)
      expect(offer.bank_names).to eq([])
    end

    it 'updates price from margin when margin changes' do
      offer = described_class.new(margin: 0.05)

      # Mock the private method directly
      allow(offer).to receive(:margin_changed?).and_return(true)
      allow(offer).to receive(:update_price_from_market!)

      offer.send(:update_price_from_margin)
      expect(offer).to have_received(:update_price_from_market!)
    end

    it 'does not update price from margin when margin is not changed' do
      offer = described_class.new(margin: 0.05)
      allow(offer).to receive(:margin_changed?).and_return(false)
      allow(offer).to receive(:update_price_from_market!)

      # Call the callback directly
      offer.run_callbacks(:save) { true }

      expect(offer).not_to have_received(:update_price_from_market!)
    end
  end

  describe '#buy?' do
    it 'returns true when offer_type is buy' do
      offer = described_class.new(offer_type: 'buy')
      expect(offer.buy?).to be true
    end

    it 'returns false when offer_type is not buy' do
      offer = described_class.new(offer_type: 'sell')
      expect(offer.buy?).to be false
    end
  end

  describe '#sell?' do
    it 'returns true when offer_type is sell' do
      offer = described_class.new(offer_type: 'sell')
      expect(offer.sell?).to be true
    end

    it 'returns false when offer_type is not sell' do
      offer = described_class.new(offer_type: 'buy')
      expect(offer.sell?).to be false
    end
  end

  describe '#active?' do
    it 'returns true when offer is active' do
      offer = described_class.new(disabled: false, deleted: false)
      expect(offer.active?).to be true
    end

    it 'returns false when offer is disabled' do
      offer = described_class.new(disabled: true, deleted: false)
      expect(offer.active?).to be false
    end

    it 'returns false when offer is deleted' do
      offer = described_class.new(disabled: false, deleted: true)
      allow(offer).to receive(:deleted?).and_return(true)
      expect(offer.active?).to be false
    end
  end

  describe '#scheduled?' do
    it 'returns true when schedule_start_time is present' do
      offer = described_class.new(schedule_start_time: Time.zone.now)
      expect(offer.scheduled?).to be true
    end

    it 'returns true when schedule_end_time is present' do
      offer = described_class.new(schedule_end_time: Time.zone.now)
      expect(offer.scheduled?).to be true
    end

    it 'returns false when neither schedule time is present' do
      offer = described_class.new
      expect(offer.scheduled?).to be false
    end
  end

  describe '#currently_active?' do
    it 'returns false when offer is disabled' do
      offer = described_class.new(disabled: true)
      expect(offer.currently_active?).to be false
    end

    it 'returns false when offer is deleted' do
      offer = described_class.new(deleted: true)
      allow(offer).to receive(:deleted?).and_return(true)
      expect(offer.currently_active?).to be false
    end

    it 'returns true when offer is active and not scheduled' do
      offer = described_class.new(disabled: false, deleted: false)
      expect(offer.currently_active?).to be true
    end

    it 'returns true when offer is active and current time is within schedule' do
      offer = described_class.new(
        disabled: false,
        deleted: false,
        schedule_start_time: Time.zone.now - 1.hour,
        schedule_end_time: Time.zone.now + 1.hour
      )
      expect(offer.currently_active?).to be true
    end

    it 'returns false when offer is active but current time is before schedule_start_time' do
      offer = described_class.new(
        disabled: false,
        deleted: false,
        schedule_start_time: Time.zone.now + 1.hour
      )
      expect(offer.currently_active?).to be false
    end

    it 'returns false when offer is active but current time is after schedule_end_time' do
      offer = described_class.new(
        disabled: false,
        deleted: false,
        schedule_end_time: Time.zone.now - 1.hour
      )
      expect(offer.currently_active?).to be false
    end
  end

  describe '#disable!' do
    it 'disables the offer and sets disable_reason' do
      offer = described_class.new

      allow(offer).to receive(:update).with(disabled: true).and_return(true)
      allow(offer).to receive(:send_offer_disable_to_kafka).and_return(true)

      offer.disable!('Test reason')

      expect(offer.disable_reason).to eq 'Test reason'
    end

    it 'uses default reason when none is provided' do
      offer = described_class.new

      allow(offer).to receive(:update).with(disabled: true).and_return(true)
      allow(offer).to receive(:send_offer_disable_to_kafka).and_return(true)

      offer.disable!

      expect(offer.disable_reason).to eq 'Disabled by user'
    end

    it 'returns false when Kafka error occurs' do
      offer = described_class.new(disabled: false)

      allow(offer).to receive(:update).with(disabled: true).and_return(true)
      allow(offer).to receive(:send_offer_disable_to_kafka).and_raise(StandardError.new('Kafka error'))

      result = offer.disable!

      expect(result).to be false
      expect(offer.errors[:base]).to include('Kafka error')
    end
  end

  describe '#record_disable_reason' do
    it 'sets reason when provided' do
      offer = described_class.new
      offer.send(:record_disable_reason, 'Custom reason')
      expect(offer.disable_reason).to eq 'Custom reason'
    end

    it 'keeps existing reason when no reason provided but disable_reason exists' do
      offer = described_class.new(disable_reason: 'Existing reason')
      offer.send(:record_disable_reason)
      expect(offer.disable_reason).to eq 'Existing reason'
    end

    it 'sets default reason when no reason provided and no disable_reason exists' do
      offer = described_class.new
      offer.send(:record_disable_reason)
      expect(offer.disable_reason).to eq 'Disabled by user'
    end
  end

  describe '#enable!' do
    it 'enables the offer and clears disable_reason' do
      offer = described_class.new(disabled: true, disable_reason: 'Some reason')

      allow(offer).to receive(:update).with(disabled: false).and_return(true)
      allow(offer).to receive(:send_offer_enable_to_kafka).and_return(true)

      offer.enable!

      expect(offer.disable_reason).to be_nil
    end

    it 'returns false when Kafka error occurs' do
      offer = described_class.new(disabled: true)

      allow(offer).to receive(:update).with(disabled: false).and_return(true)
      allow(offer).to receive(:send_offer_enable_to_kafka).and_raise(StandardError.new('Kafka error'))

      result = offer.enable!

      expect(result).to be false
      expect(offer.errors[:base]).to include('Kafka error')
    end
  end

  describe '#delete!' do
    it 'returns false when trades in progress exist' do
      offer = described_class.new
      trades = instance_double(ActiveRecord::Relation)
      trade_class = class_double(Trade, in_progress: trades)

      allow(offer).to receive(:trades).and_return(trade_class)
      allow(trades).to receive(:exists?).and_return(true)

      expect(offer.delete!).to be false
    end

    it 'sends offer delete to Kafka when no trades in progress' do
      offer = described_class.new
      trades = instance_double(ActiveRecord::Relation)
      trade_class = class_double(Trade, in_progress: trades)

      allow(offer).to receive_messages(
        trades: trade_class,
        send_offer_delete_to_kafka: true,
        update: true
      )
      allow(trades).to receive(:exists?).and_return(false)

      offer.delete!
      expect(offer).to have_received(:send_offer_delete_to_kafka)
    end
  end

  describe '#set_online!' do
    it 'updates online status to true' do
      offer = described_class.new(online: false)

      allow(offer).to receive(:update!).with(online: true).and_return(true)

      offer.set_online!
      expect(offer).to have_received(:update!).with(online: true)
    end
  end

  describe '#set_offline!' do
    it 'updates online status to false' do
      offer = described_class.new(online: true)

      allow(offer).to receive(:update!).with(online: false).and_return(true)

      offer.set_offline!
      expect(offer).to have_received(:update!).with(online: false)
    end
  end

  describe '#schedule!' do
    it 'updates schedule times' do
      offer = described_class.new
      start_time = Time.zone.now
      end_time = Time.zone.now + 1.hour

      allow(offer).to receive(:update!).with(
        schedule_start_time: start_time,
        schedule_end_time: end_time
      ).and_return(true)

      offer.schedule!(start_time, end_time)

      expect(offer.temp_start_time).to eq start_time
      expect(offer.temp_end_time).to eq end_time
      expect(offer).to have_received(:update!).with(
        schedule_start_time: start_time,
        schedule_end_time: end_time
      )
    end
  end

  describe '#update_price_from_market!' do
    it 'returns early when margin is not present' do
      offer = described_class.new
      expect(offer.update_price_from_market!).to be_nil
    end

    it 'updates price for buy offers' do
      market_price = 100
      margin = 0.05
      expected_price = market_price * (1 - margin)

      offer = described_class.new(offer_type: 'buy', margin: margin)
      allow(offer).to receive(:fetch_market_price).and_return(market_price)
      allow(offer).to receive(:update!)

      offer.update_price_from_market!
      expect(offer).to have_received(:update!).with(price: expected_price)
    end

    it 'updates price for sell offers' do
      market_price = 100
      margin = 0.05
      expected_price = market_price * (1 + margin)

      offer = described_class.new(offer_type: 'sell', margin: margin)
      allow(offer).to receive(:fetch_market_price).and_return(market_price)
      allow(offer).to receive(:update!)

      offer.update_price_from_market!
      expect(offer).to have_received(:update!).with(price: expected_price)
    end

    it 'uses provided market price if given' do
      market_price = 100
      margin = 0.05
      expected_price = market_price * (1 - margin)

      offer = described_class.new(offer_type: 'buy', margin: margin)
      allow(offer).to receive(:update!)

      offer.update_price_from_market!(market_price)
      expect(offer).to have_received(:update!).with(price: expected_price)
    end

    it 'returns early when market price is not present' do
      offer = described_class.new(margin: 0.05)
      allow(offer).to receive(:fetch_market_price).and_return(nil)
      allow(offer).to receive(:update!)

      offer.update_price_from_market!
      expect(offer).not_to have_received(:update!)
    end
  end

  describe '#available_amount' do
    it 'calculates available amount correctly' do
      offer = described_class.new(total_amount: 1.0)

      allow(offer).to receive(:in_progress_amount).and_return(0.25)
      expect(offer.available_amount).to eq 0.75
    end
  end

  describe '#in_progress_amount' do
    it 'sums coin_amount from in-progress trades' do
      offer = described_class.new
      trades = instance_double(ActiveRecord::Relation)
      trade_class = class_double(Trade, in_progress: trades)

      allow(offer).to receive(:trades).and_return(trade_class)
      allow(trades).to receive(:sum).with(:coin_amount).and_return(0.3)

      expect(offer.in_progress_amount).to eq 0.3
    end
  end

  describe '#has_available_amount?' do
    it 'returns true when amount is less than available amount' do
      offer = described_class.new
      allow(offer).to receive(:available_amount).and_return(1.0)

      expect(offer.has_available_amount?(0.5)).to be true
    end

    it 'returns true when amount equals available amount' do
      offer = described_class.new
      allow(offer).to receive(:available_amount).and_return(1.0)

      expect(offer.has_available_amount?(1.0)).to be true
    end

    it 'returns false when amount is greater than available amount' do
      offer = described_class.new
      allow(offer).to receive(:available_amount).and_return(1.0)

      expect(offer.has_available_amount?(1.5)).to be false
    end
  end

  describe 'Kafka event methods' do
    it 'sends offer create to Kafka' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:create).with(offer: offer)

      offer.send_offer_create_to_kafka
      expect(service).to have_received(:create).with(offer: offer)
    end

    it 'logs error when sending offer create to Kafka fails' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)
      error_message = 'Kafka connection error'

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:create).and_raise(StandardError.new(error_message))
      allow(Rails.logger).to receive(:error)

      offer.send_offer_create_to_kafka
      expect(Rails.logger).to have_received(:error).with("Error sending offer create to Kafka: #{error_message}")
    end

    it 'sends offer update to Kafka' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:update).with(offer: offer)

      offer.send_offer_update_to_kafka
      expect(service).to have_received(:update).with(offer: offer)
    end

    it 'logs error when sending offer update to Kafka fails' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)
      error_message = 'Kafka connection error'

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:update).and_raise(StandardError.new(error_message))
      allow(Rails.logger).to receive(:error)

      offer.send_offer_update_to_kafka
      expect(Rails.logger).to have_received(:error).with("Error sending offer update to Kafka: #{error_message}")
    end

    it 'sends offer disable to Kafka' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:disable).with(offer: offer)

      offer.send_offer_disable_to_kafka
      expect(service).to have_received(:disable).with(offer: offer)
    end

    it 'logs error when sending offer disable to Kafka fails' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)
      error_message = 'Kafka connection error'

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:disable).and_raise(StandardError.new(error_message))
      allow(Rails.logger).to receive(:error)

      offer.send_offer_disable_to_kafka
      expect(Rails.logger).to have_received(:error).with("Error sending offer disable to Kafka: #{error_message}")
    end

    it 'sends offer enable to Kafka' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:enable).with(offer: offer)

      offer.send_offer_enable_to_kafka
      expect(service).to have_received(:enable).with(offer: offer)
    end

    it 'logs error when sending offer enable to Kafka fails' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)
      error_message = 'Kafka connection error'

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:enable).and_raise(StandardError.new(error_message))
      allow(Rails.logger).to receive(:error)

      offer.send_offer_enable_to_kafka
      expect(Rails.logger).to have_received(:error).with("Error sending offer enable to Kafka: #{error_message}")
    end

    it 'sends offer delete to Kafka' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:delete).with(offer: offer)

      offer.send_offer_delete_to_kafka
      expect(service).to have_received(:delete).with(offer: offer)
    end

    it 'logs error when sending offer delete to Kafka fails' do
      offer = described_class.new
      service = instance_double(KafkaService::Services::Offer::OfferService)
      error_message = 'Kafka connection error'

      allow(offer).to receive(:offer_service).and_return(service)
      allow(service).to receive(:delete).and_raise(StandardError.new(error_message))
      allow(Rails.logger).to receive(:error)

      offer.send_offer_delete_to_kafka
      expect(Rails.logger).to have_received(:error).with("Error sending offer delete to Kafka: #{error_message}")
    end
  end

  describe 'ransackable_attributes' do
    it 'returns allowed attributes for search' do
      expected_attributes = %w[
        id user_id offer_type coin_currency currency
        price min_amount max_amount total_amount
        payment_method_id payment_time payment_details
        country_code disabled deleted automatic online
        terms_of_trade disable_reason margin fixed_coin_price
        bank_names schedule_start_time schedule_end_time
        created_at updated_at
      ]
      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe 'ransackable_associations' do
    it 'returns allowed associations for search' do
      expected_associations = %w[user payment_method trades]
      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end
end
