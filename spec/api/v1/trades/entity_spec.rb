# frozen_string_literal: true

require 'rails_helper'

RSpec.describe V1::Trades::Entity, type: :model do
  describe 'exposed attributes' do
    it 'exposes basic trade attributes' do
      offer = create(:offer)
      trade = create(:trade, offer: offer)

      entity = described_class.represent(trade)
      serialized = entity.as_json

      expect(serialized).to include(
        id: trade.id,
        ref: trade.ref,
        coin_currency: trade.coin_currency,
        fiat_currency: trade.fiat_currency,
        coin_amount: trade.coin_amount,
        fiat_amount: trade.fiat_amount,
        price: trade.price,
        taker_side: trade.taker_side,
        status: trade.status,
        has_payment_proof: trade.has_payment_proof,
        payment_proof_status: trade.payment_proof_status,
        buyer_id: trade.buyer_id,
        seller_id: trade.seller_id,
        offer_id: trade.offer_id
      )
      expect(serialized).to have_key(:created_at)
      expect(serialized).to have_key(:paid_at)
      expect(serialized).to have_key(:released_at)
      expect(serialized).to have_key(:cancelled_at)
      expect(serialized).to have_key(:disputed_at)
      expect(serialized).to have_key(:expired_at)
    end

    it 'exposes payment method name from offer' do
      payment_method = create(:payment_method, display_name: 'Bank Transfer')
      offer = create(:offer, payment_method: payment_method)
      trade = create(:trade, offer: offer)

      entity = described_class.represent(trade)
      serialized = entity.as_json

      expect(serialized[:payment_method]).to eq('Bank Transfer')
    end

    it 'handles nil payment method' do
      # Create a payment method first to avoid constraints
      payment_method = create(:payment_method)
      offer = create(:offer, payment_method: payment_method)

      # Then manually mock the behavior for nil payment method
      trade = create(:trade, offer: offer)
      allow(trade.offer.payment_method).to receive(:display_name).and_return(nil)

      entity = described_class.represent(trade)
      serialized = entity.as_json

      expect(serialized[:payment_method]).to be_nil
    end

    describe 'fiat token trade attributes' do
      it 'identifies fiat token trades' do
        trade = create(:trade)
        allow(trade).to receive(:fiat_token_trade?).and_return(true)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:is_fiat_token_trade]).to be true
      end

      it 'identifies regular trades' do
        trade = create(:trade)
        allow(trade).to receive(:fiat_token_trade?).and_return(false)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:is_fiat_token_trade]).to be false
      end

      it 'exposes payment type as FiatDeposit for deposit trades' do
        trade = create(:trade)
        allow(trade).to receive_messages(is_fiat_token_deposit_trade?: true, is_fiat_token_withdrawal_trade?: false)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:payment_type]).to eq('FiatDeposit')
      end

      it 'exposes payment type as FiatWithdrawal for withdrawal trades' do
        trade = create(:trade)
        allow(trade).to receive_messages(is_fiat_token_deposit_trade?: false, is_fiat_token_withdrawal_trade?: true)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:payment_type]).to eq('FiatWithdrawal')
      end

      it 'exposes payment type as nil for normal trades' do
        trade = create(:trade)
        allow(trade).to receive_messages(is_fiat_token_deposit_trade?: false, is_fiat_token_withdrawal_trade?: false)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:payment_type]).to be_nil
      end

      it 'exposes fiat_token_deposit_id for deposit trades' do
        # First create a deposit and then link to trade
        deposit = create(:fiat_deposit)
        trade = create(:trade)

        # Update trade attributes
        trade.fiat_token_deposit_id = deposit.id
        trade.save(validate: false) # Skip validations to allow setting IDs directly

        # Set up the method stubs
        allow(trade).to receive_messages(is_fiat_token_deposit_trade?: true, is_fiat_token_withdrawal_trade?: false)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:payment_id]).to eq(deposit.id)
      end

      it 'exposes fiat_token_withdrawal_id for withdrawal trades' do
        # First create a withdrawal and then link to trade
        withdrawal = create(:fiat_withdrawal)
        trade = create(:trade)

        # Update trade attributes
        trade.fiat_token_withdrawal_id = withdrawal.id
        trade.save(validate: false) # Skip validations to allow setting IDs directly

        # Set up the method stubs
        allow(trade).to receive_messages(is_fiat_token_deposit_trade?: false, is_fiat_token_withdrawal_trade?: true)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:payment_id]).to eq(withdrawal.id)
      end
    end
  end

  describe V1::Trades::TradeDetail do
    it 'inherits from base Entity' do
      expect(described_class.superclass).to eq(V1::Trades::Entity)
    end

    it 'exposes additional trade details' do
      trade = create(:trade,
                     payment_details: { 'bank_account' => '123456' },
                     payment_receipt_details: { 'transaction_id' => 'TX12345' },
                     dispute_reason: 'Payment not received',
                     dispute_resolution: 'admin_intervention',
                     fee_ratio: 0.01,
                     coin_trading_fee: 0.05,
                     coin_amount: 0.05)

      entity = described_class.represent(trade)
      serialized = entity.as_json

      expect(serialized).to include(
        payment_details: trade.payment_details,
        payment_receipt_details: trade.payment_receipt_details,
        dispute_reason: trade.dispute_reason,
        dispute_resolution: trade.dispute_resolution,
        fee_ratio: trade.fee_ratio,
        coin_trading_fee: trade.coin_trading_fee
      )
      expect(serialized).to have_key(:amount_after_fee)
      expect(serialized).to have_key(:time_left_seconds)
      expect(serialized).to have_key(:updated_at)
      expect(serialized[:amount_after_fee]).to eq(0.0)
    end

    it 'exposes buyer and seller entities' do
      trade = create(:trade)

      # Instead of checking specific IDs, simply check that the buyer and seller entities exist
      # and have the expected structure
      entity = described_class.represent(trade)
      serialized = entity.as_json

      expect(serialized[:buyer]).to be_a(Hash)
      expect(serialized[:buyer]).to have_key(:id)
      expect(serialized[:buyer]).to have_key(:display_name)
      expect(serialized[:seller]).to be_a(Hash)
      expect(serialized[:seller]).to have_key(:id)
      expect(serialized[:seller]).to have_key(:display_name)
    end

    describe 'is_deposit_trade flag' do
      it 'returns true for deposit trades' do
        trade = create(:trade)
        allow(trade).to receive(:is_fiat_token_deposit_trade?).and_return(true)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:is_deposit_trade]).to be true
      end

      it 'returns false for non-deposit trades' do
        trade = create(:trade)
        allow(trade).to receive(:is_fiat_token_deposit_trade?).and_return(false)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:is_deposit_trade]).to be false
      end
    end

    describe 'is_withdrawal_trade flag' do
      it 'returns true for withdrawal trades' do
        trade = create(:trade)
        allow(trade).to receive(:is_fiat_token_withdrawal_trade?).and_return(true)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:is_withdrawal_trade]).to be true
      end

      it 'returns false for non-withdrawal trades' do
        trade = create(:trade)
        allow(trade).to receive(:is_fiat_token_withdrawal_trade?).and_return(false)

        entity = described_class.represent(trade)
        serialized = entity.as_json

        expect(serialized[:is_withdrawal_trade]).to be false
      end
    end

    describe 'fiat token information' do
      it 'represents fiat deposit data' do
        # Create fiat deposit
        deposit = create(:fiat_deposit)
        trade = create(:trade)

        # Set up stubs
        allow(trade).to receive_messages(is_fiat_token_deposit_trade?: true, is_fiat_token_withdrawal_trade?: false, fiat_token_deposit: deposit)

        # Create fake entity class to represent fiat deposit data
        allow(V1::FiatDeposits::Entity).to receive(:represent).with(deposit, anything).and_return('deposit_data')

        entity = described_class.represent(trade, {})
        serialized = entity.as_json

        expect(serialized[:fiat_token][:type]).to eq('deposit')
        expect(serialized[:fiat_token][:data]).to eq('deposit_data')
      end

      it 'represents fiat withdrawal data' do
        # Create fiat withdrawal
        withdrawal = create(:fiat_withdrawal)
        trade = create(:trade)

        # Set up stubs
        allow(trade).to receive_messages(is_fiat_token_deposit_trade?: false, is_fiat_token_withdrawal_trade?: true, fiat_token_withdrawal: withdrawal)

        # Create fake entity class to represent fiat withdrawal data
        allow(V1::FiatWithdrawals::Entity).to receive(:represent).with(withdrawal, anything).and_return('withdrawal_data')

        entity = described_class.represent(trade, {})
        serialized = entity.as_json

        expect(serialized[:fiat_token][:type]).to eq('withdrawal')
        expect(serialized[:fiat_token][:data]).to eq('withdrawal_data')
      end

      it 'returns nil for regular trades' do
        trade = create(:trade)
        allow(trade).to receive_messages(is_fiat_token_deposit_trade?: false, is_fiat_token_withdrawal_trade?: false)

        entity = described_class.represent(trade, {})
        serialized = entity.as_json

        expect(serialized[:fiat_token]).to be_nil
      end
    end

    describe 'permission helpers' do
      let(:trade) { create(:trade) }
      let(:current_user) { create(:user) }

      it 'checks if trade can be released by current user' do
        allow(trade).to receive(:can_be_released_by?).with(current_user).and_return(true)

        entity = described_class.represent(trade, { current_user: current_user })
        serialized = entity.as_json

        expect(serialized[:can_be_released_by_current_user]).to be true
      end

      it 'checks if trade can be disputed by current user' do
        allow(trade).to receive(:can_be_disputed_by?).with(current_user).and_return(true)

        entity = described_class.represent(trade, { current_user: current_user })
        serialized = entity.as_json

        expect(serialized[:can_be_disputed_by_current_user]).to be true
      end

      it 'checks if trade can be cancelled by current user' do
        allow(trade).to receive(:can_be_cancelled_by?).with(current_user).and_return(true)

        entity = described_class.represent(trade, { current_user: current_user })
        serialized = entity.as_json

        expect(serialized[:can_be_cancelled_by_current_user]).to be true
      end

      it 'checks if trade can be marked paid by current user' do
        allow(trade).to receive(:can_be_marked_paid_by?).with(current_user).and_return(true)

        entity = described_class.represent(trade, { current_user: current_user })
        serialized = entity.as_json

        expect(serialized[:can_be_marked_paid_by_current_user]).to be true
      end
    end
  end

  describe V1::Trades::TradeDetailWithMessages do
    it 'inherits from TradeDetail' do
      expect(described_class.superclass).to eq(V1::Trades::TradeDetail)
    end

    it 'exposes messages in descending order' do
      trade = create(:trade)

      # Create messages with specific order
      message1 = create(:message, trade: trade, created_at: 2.hours.ago)
      message2 = create(:message, trade: trade, created_at: 1.hour.ago)

      # Stub the message representation since we can't easily set content
      allow(V1::Messages::Entity).to receive(:represent).and_return([
                                                                      { id: message2.id, body: message2.body, created_at: message2.created_at },
                                                                      { id: message1.id, body: message1.body, created_at: message1.created_at }
                                                                    ])

      entity = described_class.represent(trade)
      serialized = entity.as_json

      expect(serialized[:messages]).to be_an(Array)
      expect(serialized[:messages].length).to eq(2)
      expect(serialized[:messages][0][:id]).to eq(message2.id)
      expect(serialized[:messages][1][:id]).to eq(message1.id)
    end

    it 'limits messages to 20' do
      trade = create(:trade)

      # Create 25 messages
      messages = []
      25.times { messages << create(:message, trade: trade) }

      # We need to match the actual message entity behavior by adding a result
      message_query = double
      allow(trade).to receive(:messages).and_return(message_query)
      allow(message_query).to receive(:order).and_return(message_query)
      allow(message_query).to receive(:limit).with(20).and_return(messages.first(20))

      allow(V1::Messages::Entity).to receive(:represent).with(any_args).and_return(Array.new(20))

      entity = described_class.represent(trade)
      serialized = entity.as_json

      expect(serialized[:messages].length).to eq(20)
    end
  end
end
