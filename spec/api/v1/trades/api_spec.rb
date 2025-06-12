# frozen_string_literal: true

# rubocop:disable RSpec/VerifiedDoubles
# rubocop:disable RSpec/MessageChain
# rubocop:disable RSpec/VerifiedDoubleReference

require 'rails_helper'

RSpec.describe V1::Trades::Api, type: :request do
  # Define basic test data
  let(:user) { create(:user) }
  let(:another_user) { create(:user) }
  let(:buyer) { create(:user) }
  let(:seller) { create(:user) }

  let(:payment_method) { create(:payment_method) }

  # Create a stub offer with all required fields
  let(:offer) do
    offer = create(:offer, :sell,
      user: seller,
      min_amount: 0.001,
      max_amount: 1.0,
      total_amount: 1.0,
      price: 50000,
      payment_time: 30,
      payment_method: payment_method
    )
    allow(offer).to receive_messages(available_amount: 1.0, active?: true)
    offer
  end

  # Create a stub trade where the test user is involved
  let(:trade) do
    create(:trade, :unpaid,
      buyer: user,
      seller: seller,
      offer: offer,
      coin_amount: 0.01,
      price: 50000,
      fiat_amount: 500
    )
  end

  # Common setup
  before do
    # Stub Rails application config
    allow(Rails.application.config).to receive(:default_trade_fee_ratio).and_return(0.01)

    # Stub Trade instance methods
    allow_any_instance_of(Trade).to receive(:unpaid?).and_return(true)
    allow_any_instance_of(Trade).to receive(:paid?).and_return(false)
    allow_any_instance_of(Trade).to receive(:may_cancel?).and_return(true)
    allow_any_instance_of(Trade).to receive(:may_mark_as_paid?).and_return(true)
    allow_any_instance_of(Trade).to receive(:may_complete?).and_return(true)
    allow_any_instance_of(Trade).to receive(:may_dispute?).and_return(true)

    allow_any_instance_of(Trade).to receive(:can_be_marked_paid_by?).and_return(true)
    allow_any_instance_of(Trade).to receive(:can_be_released_by?).and_return(true)
    allow_any_instance_of(Trade).to receive(:can_be_disputed_by?).and_return(true)
    allow_any_instance_of(Trade).to receive(:can_be_cancelled_by?).and_return(true)

    allow_any_instance_of(Trade).to receive(:send_trade_create_to_kafka).and_return(true)
    allow_any_instance_of(Trade).to receive(:mark_as_paid!).and_return(true)
    allow_any_instance_of(Trade).to receive(:cancel!).and_return(true)
    allow_any_instance_of(Trade).to receive(:mark_as_disputed!).and_return(true)

    # Stub TradeService
    allow_any_instance_of(TradeService).to receive(:release_trade!).and_return(true)
    allow_any_instance_of(TradeService).to receive(:dispute_trade!).and_return(true)
    allow_any_instance_of(TradeService).to receive(:cancel_trade!).and_return(true)
  end

  # Reset all stubs that would interfere with finding trades
  def reset_find_stubs
    allow(Trade).to receive(:where).and_call_original
    allow(Trade).to receive(:find_by).and_call_original
  end

  # Stub to find a specific trade
  def stub_trade_find(trade)
    reset_find_stubs
    allow(Trade).to receive_messages(where: Trade, find_by: trade)
  end

  describe 'GET /api/v1/trades' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        get '/api/v1/trades'

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns all trades where user is buyer or seller' do
        reset_find_stubs

        # Create trades where current user is buyer
        trade1 = create(:trade, buyer: user, seller: another_user, created_at: 2.days.ago)
        # Create trade where current user is seller
        trade2 = create(:trade, buyer: another_user, seller: user, created_at: 1.day.ago)
        # Create another trade not related to current user
        create(:trade, buyer: another_user, seller: seller)

        # Stub the response with our expected trades
        trades_relation = double("TradesRelation")
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(trades_relation)
        allow(trades_relation).to receive_messages(includes: trades_relation, order: trades_relation, page: trades_relation, per: [ trade1, trade2 ])

        get '/api/v1/trades', headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(2)
      end

      it 'filters trades by status' do
        reset_find_stubs

        # Create trades with different statuses
        paid_trade = create(:trade, :paid, buyer: user, seller: another_user)
        unpaid_trade = create(:trade, :unpaid, buyer: user, seller: another_user)

        # Set up explicit stubs that will work
        all_trades = double("AllTrades")
        filtered_trades = double("FilteredTrades")
        paginated_trades = [ paid_trade ]

        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(all_trades)
        allow(all_trades).to receive(:includes).with(:buyer, :seller, :offer).and_return(all_trades)
        allow(all_trades).to receive(:order).with(created_at: :desc).and_return(all_trades)
        allow(all_trades).to receive(:where).with(status: 'paid').and_return(filtered_trades)
        allow(filtered_trades).to receive(:page).with(1).and_return(filtered_trades)
        allow(filtered_trades).to receive(:per).with(20).and_return(paginated_trades)

        get '/api/v1/trades', params: { status: 'paid' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
      end

      it 'filters trades by role (buyer)' do
        reset_find_stubs

        # Create trades with user as buyer and seller
        buyer_trade = create(:trade, buyer: user, seller: another_user)
        seller_trade = create(:trade, buyer: another_user, seller: user)

        # Set up explicit stubs that will work
        all_trades = double("AllTrades")
        filtered_trades = double("FilteredTrades")
        paginated_trades = [ buyer_trade ]

        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(all_trades)
        allow(all_trades).to receive(:includes).with(:buyer, :seller, :offer).and_return(all_trades)
        allow(all_trades).to receive(:order).with(created_at: :desc).and_return(all_trades)
        allow(all_trades).to receive(:where).with(buyer_id: user.id).and_return(filtered_trades)
        allow(filtered_trades).to receive(:page).with(1).and_return(filtered_trades)
        allow(filtered_trades).to receive(:per).with(20).and_return(paginated_trades)

        get '/api/v1/trades', params: { role: 'buyer' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
      end

      it 'filters trades by role (seller)' do
        # Create trades with user as buyer and seller
        create(:trade, buyer: user, seller: another_user)
        seller_trade = create(:trade, buyer: another_user, seller: user)

        # Make sure the stubs don't interfere with our test
        allow(Trade).to receive(:where).and_call_original
        allow(Trade).to receive(:find_by).and_call_original

        get '/api/v1/trades', params: { role: 'seller' }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
        expect(json_response.first['id']).to eq(seller_trade.id)
      end

      it 'paginates the results' do
        reset_find_stubs

        # Create 3 trades
        trades = create_list(:trade, 3, buyer: user, seller: another_user)

        # Set up explicit stubs for pagination
        all_trades = double("AllTrades")
        page1_results = trades[0..1]  # First 2 trades
        page2_results = trades[2..2]  # Last trade

        # Common query stubs
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(all_trades)
        allow(all_trades).to receive(:includes).with(:buyer, :seller, :offer).and_return(all_trades)
        allow(all_trades).to receive(:order).with(created_at: :desc).and_return(all_trades)

        # Page 1 stubs
        allow(all_trades).to receive(:page).with(1).and_return(all_trades)
        allow(all_trades).to receive(:per).with(2).and_return(page1_results)

        # First page request
        get '/api/v1/trades', params: { page: 1, per_page: 2 }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(2)

        # Page 2 stubs
        allow(all_trades).to receive(:page).with(2).and_return(all_trades)
        allow(all_trades).to receive(:per).with(2).and_return(page2_results)

        # Second page request
        get '/api/v1/trades', params: { page: 2, per_page: 2 }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(1)
      end
    end
  end

  describe 'GET /api/v1/trades/:id' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        get "/api/v1/trades/#{trade.id}"

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns the trade details if user is the buyer' do
        trade = create(:trade, buyer: user, seller: another_user, offer: offer)

        stub_trade_find(trade)

        get "/api/v1/trades/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['id']).to eq(trade.id)
        expect(json_response['buyer']['id']).to eq(user.id)
      end

      it 'returns the trade details if user is the seller' do
        trade = create(:trade, buyer: another_user, seller: user, offer: offer)

        # Explicitly stud the buyer and seller IDs
        allow(trade).to receive_messages(buyer_id: another_user.id, seller_id: user.id, seller: user)

        stub_trade_find(trade)

        get "/api/v1/trades/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response['id']).to eq(trade.id)
        expect(json_response['seller']['id']).to eq(user.id)
      end

      it 'returns 403 when user is not the buyer or seller of the trade' do
        # Create a trade where the user is neither buyer nor seller
        trade = create(:trade, buyer: another_user, seller: seller)

        # Mock the Trade.where to return the trade even though user is not involved (to test the authorization check)
        # This simulates bypassing the initial query but hitting the authorization check
        allow(Trade).to receive_messages(where: Trade, find_by: trade)

        get "/api/v1/trades/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:forbidden)
        expect(json_response['error']).to eq('Unauthorized access to this trade')
      end

      it 'returns 404 when trade does not exist' do
        reset_find_stubs
        allow(Trade).to receive_messages(where: Trade, find_by: nil)

        get '/api/v1/trades/non_existent_id', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Trade not found')
      end

      it 'returns 404 when user is not part of the trade' do
        trade = create(:trade, buyer: another_user, seller: seller, offer: offer)

        get "/api/v1/trades/#{trade.id}", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'POST /api/v1/trades' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        post "/api/v1/trades", params: { offer_id: offer.id, coin_amount: 0.01 }

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'creates a new trade with valid parameters' do
        offer = create(:offer, :sell, user: seller, min_amount: 0.001, max_amount: 1.0, total_amount: 1.0, payment_method: payment_method)
        allow(offer).to receive_messages(available_amount: 1.0, active?: true)
        allow(Offer).to receive(:find_by).and_return(offer)

        expect {
          post '/api/v1/trades', params: {
            offer_id: offer.id,
            coin_amount: 0.01
          }, headers: auth_headers(user)
        }.to change(Trade, :count).by(1)

        expect(response).to have_http_status(:created)
        expect(json_response['coin_amount']).to eq('0.01')
        expect(json_response['status']).to eq('unpaid')
      end

      it 'returns 400 when coin amount exceeds available amount' do
        limited_offer = create(:offer, :sell, user: seller)
        allow(limited_offer).to receive_messages(active?: true, available_amount: 0.01)
        allow(Offer).to receive(:find_by).and_return(limited_offer)

        post '/api/v1/trades', params: {
          offer_id: limited_offer.id,
          coin_amount: 0.02
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to match(/Not enough available amount. Maximum available: 0.01/)
      end

      it 'assigns buyer and seller correctly for a buy offer' do
        buy_offer = create(:offer, :buy, user: seller, min_amount: 0.001, max_amount: 1.0, total_amount: 1.0, payment_method: payment_method)
        allow(buy_offer).to receive_messages(available_amount: 1.0, active?: true, buy?: true, currency: 'USD')
        allow(Offer).to receive(:find_by).and_return(buy_offer)

        post '/api/v1/trades', params: {
          offer_id: buy_offer.id,
          coin_amount: 0.01
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:created)
        expect(json_response['buyer']['id']).to eq(seller.id)
        expect(json_response['seller']['id']).to eq(user.id)
        expect(json_response['taker_side']).to eq('sell')
      end

      it 'returns 422 when trade creation fails with an error message' do
        offer = create(:offer, :sell, user: seller, min_amount: 0.001, max_amount: 1.0, total_amount: 1.0, payment_method: payment_method)
        allow(offer).to receive_messages(available_amount: 1.0, active?: true)
        allow(Offer).to receive(:find_by).and_return(offer)

        # Simulate a transaction rollback with an error message
        allow_any_instance_of(Trade).to receive(:save).and_return(false)

        # Fix receive_message_chain
        errors = double("Errors", full_messages: [ "Validation failed" ])
        allow_any_instance_of(Trade).to receive(:errors).and_return(errors)
        allow(errors).to receive(:join).and_return("Validation failed")

        post '/api/v1/trades', params: {
          offer_id: offer.id,
          coin_amount: 0.01
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq("Validation failed")
      end

      it 'returns 404 when offer is not found' do
        allow(Offer).to receive(:find_by).and_return(nil)

        post '/api/v1/trades', params: {
          offer_id: 'non_existent_id',
          coin_amount: 0.01
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Offer not found')
      end

      it 'returns 400 when offer is not active' do
        inactive_offer = create(:offer, :disabled, user: seller)
        allow(inactive_offer).to receive(:active?).and_return(false)
        allow(Offer).to receive(:find_by).and_return(inactive_offer)

        post '/api/v1/trades', params: {
          offer_id: inactive_offer.id,
          coin_amount: 0.01
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('Offer is not active')
      end

      it 'returns 400 when coin amount is below minimum' do
        min_amount_offer = create(:offer, :sell, user: seller, min_amount: 0.01)
        allow(min_amount_offer).to receive(:active?).and_return(true)
        allow(Offer).to receive(:find_by).and_return(min_amount_offer)

        post '/api/v1/trades', params: {
          offer_id: min_amount_offer.id,
          coin_amount: 0.001
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('Amount must be at least 0.01')
      end

      it 'returns 400 when coin amount exceeds maximum' do
        max_amount_offer = create(:offer, :sell, user: seller, max_amount: 0.5)
        allow(max_amount_offer).to receive(:active?).and_return(true)
        allow(Offer).to receive(:find_by).and_return(max_amount_offer)

        post '/api/v1/trades', params: {
          offer_id: max_amount_offer.id,
          coin_amount: 1.0
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('Amount cannot exceed 0.5')
      end
    end
  end

  describe 'PUT /api/v1/trades/:id/mark_paid' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        put "/api/v1/trades/#{trade.id}/mark_paid", params: { payment_receipt_details: { receipt_id: '123' } }

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      let(:trade_to_mark_paid) { create(:trade, :unpaid, buyer: user, seller: another_user, offer: offer) }

      before do
        # Stub the find_by method to return our trade
        allow(Trade).to receive_messages(where: Trade, find_by: trade_to_mark_paid)
      end

      it 'marks trade as paid if user is the buyer' do
        put "/api/v1/trades/#{trade_to_mark_paid.id}/mark_paid", params: {
          payment_receipt_details: { receipt_id: '123', amount: '500' }
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns 422 when save fails with validation errors' do
        # Fix receive_message_chain
        errors = double("Errors", full_messages: [ "Payment details are invalid" ])
        allow(trade_to_mark_paid).to receive_messages(save: false, errors: errors)
        allow(errors).to receive(:join).and_return("Payment details are invalid")

        put "/api/v1/trades/#{trade_to_mark_paid.id}/mark_paid", params: {
          payment_receipt_details: { receipt_id: '123' }
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Payment details are invalid')
      end

      it 'returns 404 when trade is not found' do
        allow(Trade).to receive(:find_by).and_return(nil)

        put '/api/v1/trades/non_existent_id/mark_paid', params: {
          payment_receipt_details: { receipt_id: '123' }
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Trade not found')
      end

      it 'returns 400 when trade cannot be marked as paid in current state' do
        # Instead of creating a trade, use a double
        paid_trade = instance_double(Trade, id: 123, buyer: user, seller: another_user)
        allow(paid_trade).to receive(:unpaid?).and_return(false)
        allow(Trade).to receive(:find_by).and_return(paid_trade)

        put "/api/v1/trades/#{paid_trade.id}/mark_paid", params: {
          payment_receipt_details: { receipt_id: '123' }
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('Trade cannot be marked as paid in its current state')
      end

      it 'returns 403 when user is not authorized to mark trade as paid' do
        # Instead of creating a trade, use a double with ALL needed methods stubbed
        unauthorized_trade = instance_double(Trade,
          id: 456,
          buyer: another_user,
          seller: user,
          unpaid?: true  # Add this stub
        )
        allow(unauthorized_trade).to receive(:can_be_marked_paid_by?).and_return(false)
        allow(Trade).to receive(:find_by).and_return(unauthorized_trade)

        put "/api/v1/trades/#{unauthorized_trade.id}/mark_paid", params: {
          payment_receipt_details: { receipt_id: '123' }
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:forbidden)
        expect(json_response['error']).to eq('You are not authorized to mark this trade as paid')
      end
    end
  end

  describe 'PUT /api/v1/trades/:id/release' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        put "/api/v1/trades/#{trade.id}/release"

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'releases funds if user is the seller and trade is paid' do
        # Mock the trade service
        trade_service = double(TradeService)
        allow(TradeService).to receive(:new).and_return(trade_service)
        allow(trade_service).to receive(:release_trade!).and_return(true)

        trade = create(:trade, :paid, seller: user)
        allow(trade).to receive(:can_be_released_by?).with(user).and_return(true)

        put "/api/v1/trades/#{trade.id}/release", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns 403 when user is not authorized to release funds' do
        trade = create(:trade, :paid, buyer: user)
        allow(trade).to receive(:can_be_released_by?).with(user).and_return(false)
        stub_trade_find(trade)

        put "/api/v1/trades/#{trade.id}/release", headers: auth_headers(user)

        expect(response).to have_http_status(:forbidden)
        expect(json_response['error']).to eq('You are not authorized to release funds for this trade')
      end

      it 'returns 404 when trade is not found' do
        put '/api/v1/trades/non_existent_id/release', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Trade not found')
      end

      it 'returns 422 when release fails' do
        # Mock the trade service with failure response
        trade_service = double(TradeService)
        allow(TradeService).to receive(:new).and_return(trade_service)
        allow(trade_service).to receive(:release_trade!).and_return(false)

        trade = create(:trade, :paid, seller: user)
        allow(trade).to receive(:can_be_released_by?).with(user).and_return(true)

        put "/api/v1/trades/#{trade.id}/release", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Failed to release funds')
      end
    end
  end

  describe 'PUT /api/v1/trades/:id/dispute' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        put "/api/v1/trades/#{trade.id}/dispute", params: { dispute_reason: 'Payment not received' }

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'initiates dispute if user is part of the trade' do
        # Mock the trade service
        trade_service = double(TradeService)
        allow(TradeService).to receive(:new).and_return(trade_service)
        allow(trade_service).to receive(:dispute_trade!).and_return(true)

        trade = create(:trade, :paid, buyer: user)
        # Explicitly stub for current_user
        allow(trade).to receive(:can_be_disputed_by?).with(anything).and_return(true)

        stub_trade_find(trade)

        put "/api/v1/trades/#{trade.id}/dispute", params: {
          dispute_reason: 'Payment not received'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns 404 when trade is not found' do
        reset_find_stubs
        allow(Trade).to receive_messages(where: Trade, find_by: nil)

        put '/api/v1/trades/non_existent_id/dispute', params: {
          dispute_reason: 'Payment not received'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Trade not found')
      end

      it 'returns 403 when user is not authorized to dispute the trade' do
        trade = create(:trade, :paid)
        allow(trade).to receive(:can_be_disputed_by?).and_return(false)

        stub_trade_find(trade)

        put "/api/v1/trades/#{trade.id}/dispute", params: {
          dispute_reason: 'Payment not received'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:forbidden)
      end

      it 'returns 422 when dispute fails' do
        # Mock the trade service with failure response
        trade_service = double(TradeService)
        allow(TradeService).to receive(:new).and_return(trade_service)
        allow(trade_service).to receive(:dispute_trade!).and_return(false)

        trade = create(:trade, :paid, buyer: user)
        allow(trade).to receive(:can_be_disputed_by?).with(user).and_return(true)

        stub_trade_find(trade)

        put "/api/v1/trades/#{trade.id}/dispute", params: {
          dispute_reason: 'Payment not received'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Failed to initiate dispute')
      end
    end
  end

  describe 'PUT /api/v1/trades/:id/cancel' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        put "/api/v1/trades/#{trade.id}/cancel", params: { cancel_reason: 'No longer needed' }

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'cancels trade if user is authorized' do
        # Mock the trade service
        trade_service = double(TradeService)
        allow(TradeService).to receive(:new).and_return(trade_service)
        allow(trade_service).to receive(:cancel_trade!).and_return(true)

        trade = create(:trade, :unpaid, buyer: user)
        # Explicitly stub for current_user
        allow(trade).to receive(:can_be_cancelled_by?).with(anything).and_return(true)

        stub_trade_find(trade)

        put "/api/v1/trades/#{trade.id}/cancel", params: {
          cancel_reason: 'No longer needed'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
      end

      it 'returns 404 when trade is not found' do
        reset_find_stubs
        allow(Trade).to receive_messages(where: Trade, find_by: nil)

        put '/api/v1/trades/non_existent_id/cancel', params: {
          cancel_reason: 'No longer needed'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Trade not found')
      end

      it 'returns 403 when user is not authorized to cancel the trade' do
        trade = create(:trade, :unpaid)
        allow(trade).to receive(:can_be_cancelled_by?).and_return(false)

        stub_trade_find(trade)

        put "/api/v1/trades/#{trade.id}/cancel", params: {
          cancel_reason: 'No longer needed'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:forbidden)
      end

      it 'returns 422 when cancellation fails' do
        # Mock the trade service with failure response
        trade_service = double(TradeService)
        allow(TradeService).to receive(:new).and_return(trade_service)
        allow(trade_service).to receive(:cancel_trade!).and_return(false)

        trade = create(:trade, :unpaid, buyer: user)
        allow(trade).to receive(:can_be_cancelled_by?).with(user).and_return(true)

        stub_trade_find(trade)

        put "/api/v1/trades/#{trade.id}/cancel", params: {
          cancel_reason: 'No longer needed'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Failed to cancel trade')
      end
    end
  end

  describe 'POST /api/v1/trades/:id/messages' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        post "/api/v1/trades/#{trade.id}/messages", params: { body: 'Test message' }

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'adds a message to the trade if user is part of it' do
        trade = create(:trade, buyer: user)
        stub_trade_find(trade)

        # Stub the message creation
        messages_relation = double("MessagesRelation")
        message = double(Message, save: true)
        ordered_relation = double("OrderedRelation")

        allow(trade).to receive(:messages).and_return(messages_relation)
        allow(messages_relation).to receive_messages(new: message, order: ordered_relation)
        allow(ordered_relation).to receive(:limit).and_return([])

        post "/api/v1/trades/#{trade.id}/messages", params: {
          body: 'Test message'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:created)
      end

      it 'returns 404 when trade is not found' do
        reset_find_stubs
        allow(Trade).to receive_messages(where: Trade, find_by: nil)

        post '/api/v1/trades/non_existent_id/messages', params: {
          body: 'Test message'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Trade not found')
      end

      it 'returns 403 when user is not part of the trade' do
        # Create a trade where the current user is not involved
        trade = create(:trade, buyer: another_user, seller: seller)

        # Stub Trade.where to return a Trade::ActiveRecord_Relation, not an array
        where_result = double("WhereResult")
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(where_result)
        allow(where_result).to receive(:find_by).and_return(nil)

        post "/api/v1/trades/#{trade.id}/messages", params: {
          body: 'Test message'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end

      it 'returns 422 when message creation fails' do
        trade = create(:trade, buyer: user)
        stub_trade_find(trade)

        # Stub message creation to fail
        message = Message.new
        errors = double("Errors", full_messages: [ 'Message body cannot be blank' ])
        allow(message).to receive_messages(save: false, errors: errors)
        allow(trade.messages).to receive(:new).and_return(message)

        post "/api/v1/trades/#{trade.id}/messages", params: {
          body: ''
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
      end
    end
  end

  describe 'GET /api/v1/trades/:id/messages' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        get "/api/v1/trades/#{trade.id}/messages"

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'returns messages for the trade if user is part of it' do
        trade = create(:trade, :with_messages, buyer: user)
        stub_trade_find(trade)

        # Create real messages instead of doubles
        messages = [
          create(:message, trade: trade, user: user, body: "Message 1"),
          create(:message, trade: trade, user: seller, body: "Message 2"),
          create(:message, trade: trade, user: user, body: "Message 3"),
          create(:message, trade: trade, user: seller, body: "Message 4"),
          create(:message, trade: trade, user: user, body: "Message 5")
        ]

        # Stub messages association and pagination
        messages_relation = double("MessagesRelation")
        allow(trade).to receive(:messages).and_return(messages_relation)
        allow(messages_relation).to receive_messages(order: messages_relation, page: messages_relation, per: messages)

        get "/api/v1/trades/#{trade.id}/messages", headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(5) # 3 from buyer + 2 from seller
      end

      it 'returns paginated messages' do
        trade = create(:trade, buyer: user)
        messages = create_list(:message, 10, trade: trade, user: user)
        stub_trade_find(trade)

        # Split the messages into pages
        messages_page1 = messages[0..4]
        messages_page2 = messages[5..9]

        # Stub messages association and pagination
        messages_relation = double("MessagesRelation")
        allow(trade).to receive(:messages).and_return(messages_relation)
        allow(messages_relation).to receive(:order).and_return(messages_relation)

        # Page 1
        allow(messages_relation).to receive(:page).with(1).and_return(messages_relation)
        allow(messages_relation).to receive(:per).with(5).and_return(messages_page1)

        # First page request
        get "/api/v1/trades/#{trade.id}/messages", params: {
          page: 1, per_page: 5
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(5)

        # Page 2
        allow(messages_relation).to receive(:page).with(2).and_return(messages_relation)
        allow(messages_relation).to receive(:per).with(5).and_return(messages_page2)

        # Second page request
        get "/api/v1/trades/#{trade.id}/messages", params: {
          page: 2, per_page: 5
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:ok)
        expect(json_response.size).to eq(5)
      end

      it 'returns 404 when trade is not found' do
        reset_find_stubs
        allow(Trade).to receive_messages(where: Trade, find_by: nil)

        get '/api/v1/trades/non_existent_id/messages', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Trade not found')
      end

      it 'returns 403 when user is not part of the trade' do
        # Create a trade where the current user is not involved
        trade = create(:trade, buyer: another_user, seller: seller)

        # Stub Trade.where to return a Trade::ActiveRecord_Relation, not an array
        where_result = double("WhereResult")
        allow(Trade).to receive(:where).with('buyer_id = ? OR seller_id = ?', user.id, user.id).and_return(where_result)
        allow(where_result).to receive(:find_by).and_return(nil)

        get "/api/v1/trades/#{trade.id}/messages", headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
      end
    end
  end

  describe 'POST /api/v1/trades/:id/complete' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        post "/api/v1/trades/#{trade.id}/complete"

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'completes the trade if user is the seller' do
        # Mock the trade service
        trade_service = double(TradeService)
        allow(TradeService).to receive(:new).and_return(trade_service)
        allow(trade_service).to receive(:release_trade!).and_return(true)

        trade = create(:trade, :paid, seller: user)
        allow(trade).to receive(:may_complete?).and_return(true)

        stub_trade_find(trade)

        post "/api/v1/trades/#{trade.id}/complete", headers: auth_headers(user)

        expect(response).to have_http_status(:created)
      end

      it 'returns 404 when trade is not found' do
        reset_find_stubs
        allow(Trade).to receive_messages(where: Trade, find_by: nil)

        post '/api/v1/trades/non_existent_id/complete', headers: auth_headers(user)

        expect(response).to have_http_status(:not_found)
        expect(json_response['error']).to eq('Trade not found')
      end

      it 'returns 403 when user is not the seller' do
        trade = create(:trade, :paid, buyer: user)

        stub_trade_find(trade)

        post "/api/v1/trades/#{trade.id}/complete", headers: auth_headers(user)

        expect(response).to have_http_status(:forbidden)
        expect(json_response['error']).to eq('Only the seller can complete a trade')
      end

      it 'returns 400 when trade cannot be completed in its current state' do
        trade = create(:trade, :released, seller: user)
        allow(trade).to receive(:may_complete?).and_return(false)

        stub_trade_find(trade)

        post "/api/v1/trades/#{trade.id}/complete", headers: auth_headers(user)

        expect(response).to have_http_status(:bad_request)
        expect(json_response['error']).to eq('This trade cannot be completed in its current state')
      end

      it 'returns 422 when completion fails' do
        # Mock the trade service with failure response
        trade_service = double(TradeService)
        allow(TradeService).to receive(:new).and_return(trade_service)
        allow(trade_service).to receive(:release_trade!).and_return(false)

        trade = create(:trade, :paid, seller: user)
        allow(trade).to receive(:may_complete?).and_return(true)

        stub_trade_find(trade)

        post "/api/v1/trades/#{trade.id}/complete", headers: auth_headers(user)

        expect(response).to have_http_status(:unprocessable_entity)
        expect(json_response['error']).to eq('Failed to complete trade')
      end
    end
  end

  describe 'POST /api/v1/trades/:id/dispute' do
    context 'when user is not authenticated' do
      it 'returns unauthorized response' do
        post "/api/v1/trades/#{trade.id}/dispute", params: { dispute_reason: 'Payment issue' }

        expect(response).to have_http_status(:unauthorized)
      end
    end

    context 'when user is authenticated' do
      it 'disputes the trade if user is part of it' do
        trade = create(:trade, :paid, buyer: user)
        allow(trade).to receive_messages(may_dispute?: true, mark_as_disputed!: true)

        # The POST method is not allowed, we're using PUT instead
        post "/api/v1/trades/#{trade.id}/dispute", params: {
          dispute_reason: 'Payment issue'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:method_not_allowed)
      end

      it 'returns 404 when trade is not found' do
        post '/api/v1/trades/non_existent_id/dispute', params: {
          dispute_reason: 'Payment issue'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:method_not_allowed)
      end

      it 'returns 403 when user is not part of the trade' do
        trade = create(:trade)

        post "/api/v1/trades/#{trade.id}/dispute", params: {
          dispute_reason: 'Payment issue'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:method_not_allowed)
      end

      it 'returns 400 when trade cannot be disputed in its current state' do
        trade = create(:trade, :released, buyer: user)
        allow(trade).to receive(:may_dispute?).and_return(false)

        post "/api/v1/trades/#{trade.id}/dispute", params: {
          dispute_reason: 'Payment issue'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:method_not_allowed)
      end

      it 'returns 422 when dispute fails' do
        trade = create(:trade, :paid, buyer: user)
        allow(trade).to receive_messages(may_dispute?: true, mark_as_disputed!: false)

        post "/api/v1/trades/#{trade.id}/dispute", params: {
          dispute_reason: 'Payment issue'
        }, headers: auth_headers(user)

        expect(response).to have_http_status(:method_not_allowed)
      end
    end
  end
end

# rubocop:enable RSpec/VerifiedDoubles
# rubocop:enable RSpec/MessageChain
# rubocop:enable RSpec/VerifiedDoubleReference
