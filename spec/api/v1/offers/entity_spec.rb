# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Offers::Entity do
  describe 'exposed attributes' do
    it 'exposes basic offer attributes' do
      user = create(:user, username: 'merchant123')
      offer = create(:offer, user: user)

      entity = described_class.represent(offer)
      serialized = entity.as_json

      expect(serialized).to include(
        id: offer.id,
        user_id: offer.user_id,
        merchant_display_name: 'merchant123',
        offer_type: offer.offer_type,
        coin_currency: offer.coin_currency,
        currency: offer.currency,
        price: offer.price,
        min_amount: offer.min_amount,
        max_amount: offer.max_amount,
        total_amount: offer.total_amount,
        available_amount: offer.available_amount,
        payment_time: offer.payment_time,
        country_code: offer.country_code,
        online: offer.online,
        automatic: offer.automatic
      )
      expect(serialized).to have_key(:created_at)
    end

    describe 'status exposure' do
      it 'returns deleted when offer is deleted' do
        offer = create(:offer)
        allow(offer).to receive(:deleted?).and_return(true)

        entity = described_class.represent(offer)
        expect(entity.as_json[:status]).to eq('deleted')
      end

      it 'returns disabled when offer is disabled' do
        offer = create(:offer)
        allow(offer).to receive_messages(deleted?: false, disabled?: true)

        entity = described_class.represent(offer)
        expect(entity.as_json[:status]).to eq('disabled')
      end

      it 'returns scheduled_active when offer is scheduled and currently active' do
        offer = create(:offer)
        allow(offer).to receive_messages(deleted?: false, disabled?: false, scheduled?: true, currently_active?: true)

        entity = described_class.represent(offer)
        expect(entity.as_json[:status]).to eq('scheduled_active')
      end

      it 'returns scheduled_inactive when offer is scheduled but not currently active' do
        offer = create(:offer)
        allow(offer).to receive_messages(deleted?: false, disabled?: false, scheduled?: true, currently_active?: false)

        entity = described_class.represent(offer)
        expect(entity.as_json[:status]).to eq('scheduled_inactive')
      end

      it 'returns active when offer is not deleted, disabled, or scheduled' do
        offer = create(:offer)
        allow(offer).to receive_messages(deleted?: false, disabled?: false, scheduled?: false)

        entity = described_class.represent(offer)
        expect(entity.as_json[:status]).to eq('active')
      end
    end
  end

  describe V1::Offers::OfferDetail do
    describe 'exposed attributes' do
      it 'exposes additional offer details' do
        user = create(:user, display_name: 'Merchant Name')
        payment_method = create(:payment_method)
        offer = create(:offer,
          user: user,
          payment_method: payment_method,
          payment_details: { 'bank_account' => '123456' },
          terms_of_trade: 'Terms and conditions',
          disable_reason: 'Maintenance',
          margin: 0.05,
          fixed_coin_price: true,
          bank_names: [ 'Bank A', 'Bank B' ],
          schedule_start_time: Time.zone.now,
          schedule_end_time: Time.zone.now + 1.day
        )

        entity = described_class.represent(offer)
        serialized = entity.as_json

        expect(serialized).to include(
          user_id: offer.user_id,
          payment_method_id: offer.payment_method_id,
          payment_details: offer.payment_details,
          terms_of_trade: offer.terms_of_trade,
          disable_reason: offer.disable_reason,
          margin: offer.margin,
          fixed_coin_price: offer.fixed_coin_price,
          bank_names: offer.bank_names,
          schedule_start_time: offer.schedule_start_time,
          schedule_end_time: offer.schedule_end_time
        )
        expect(serialized).to have_key(:updated_at)
      end

      it 'inherits all attributes from base Entity' do
        expect(described_class.superclass).to eq(V1::Offers::Entity)
      end
    end
  end
end
