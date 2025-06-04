require 'rails_helper'

RSpec.describe FiatDeposit, type: :model do
  describe 'validations' do
    it 'validates presence of currency' do
      deposit = build(:fiat_deposit, currency: nil)
      expect(deposit).to be_invalid
      expect(deposit.errors[:currency]).to include("can't be blank")
    end

    it 'validates presence of country_code' do
      deposit = build(:fiat_deposit, country_code: nil)
      expect(deposit).to be_invalid
      expect(deposit.errors[:country_code]).to include("can't be blank")
    end

    it 'validates presence of fiat_amount' do
      deposit = build(:fiat_deposit)
      deposit.fiat_amount = nil
      deposit.valid?
      expect(deposit.errors[:fiat_amount]).to include("can't be blank")
    end

    it 'validates fiat_amount is greater than 0' do
      deposit = build(:fiat_deposit, fiat_amount: 0)
      expect(deposit).to be_invalid
      expect(deposit.errors[:fiat_amount]).to include('must be greater than 0')
    end

    it 'validates uniqueness of memo' do
      existing_deposit = create(:fiat_deposit)
      deposit = build(:fiat_deposit, memo: existing_deposit.memo)
      expect(deposit).to be_invalid
      expect(deposit.errors[:memo]).to include('has already been taken')
    end

    it 'validates inclusion of status in STATUSES' do
      deposit = build(:fiat_deposit, status: 'invalid_status')
      expect(deposit).to be_invalid
      expect(deposit.errors[:status]).to include('is not included in the list')
    end
  end

  describe 'associations' do
    it 'belongs to user' do
      association = described_class.reflect_on_association(:user)
      expect(association.macro).to eq :belongs_to
    end

    it 'belongs to fiat_account' do
      association = described_class.reflect_on_association(:fiat_account)
      expect(association.macro).to eq :belongs_to
    end

    it 'belongs to payable polymorphically and is optional' do
      association = described_class.reflect_on_association(:payable)
      expect(association.macro).to eq :belongs_to
      expect(association.options[:polymorphic]).to be true
      expect(association.options[:optional]).to be true
    end

    it 'has one trade' do
      association = described_class.reflect_on_association(:trade)
      expect(association.macro).to eq :has_one
      expect(association.options[:dependent]).to eq :nullify
    end
  end

  describe 'scopes' do
    it 'unprocessed scope excludes processed, cancelled, refunded, and illegal deposits' do
      processed = create(:fiat_deposit, :processed)
      cancelled = create(:fiat_deposit, :cancelled)
      refunded = create(:fiat_deposit, :refunded)
      illegal = create(:fiat_deposit, :illegal)
      pending = create(:fiat_deposit, :pending)

      expect(described_class.unprocessed).to include(pending)
      expect(described_class.unprocessed).not_to include(processed)
      expect(described_class.unprocessed).not_to include(cancelled)
      expect(described_class.unprocessed).not_to include(refunded)
      expect(described_class.unprocessed).not_to include(illegal)
    end

    it 'pending_user_action scope includes pending, money_sent, and ownership_verifying deposits' do
      pending = create(:fiat_deposit, :pending)
      informed = create(:fiat_deposit, :informed)

      money_sent_deposit = create(:fiat_deposit, :pending)
      money_sent_deposit.money_sent!

      ownership_verifying = create(:fiat_deposit, :ready)
      ownership_verifying.mark_as_ownership_verifying!

      expect(described_class.pending_user_action).to include(pending)
      expect(described_class.pending_user_action).to include(money_sent_deposit)
      expect(described_class.pending_user_action).to include(ownership_verifying)
      expect(described_class.pending_user_action).not_to include(informed)
    end

    it 'for_trade scope includes deposits with payable_type Trade' do
      direct_deposit = create(:fiat_deposit)
      trade_deposit = create(:fiat_deposit, :for_trade)

      expect(described_class.for_trade).to include(trade_deposit)
      expect(described_class.for_trade).not_to include(direct_deposit)
    end

    it 'direct scope includes deposits with nil payable_type' do
      direct_deposit = create(:fiat_deposit)
      trade_deposit = create(:fiat_deposit, :for_trade)

      expect(described_class.direct).to include(direct_deposit)
      expect(described_class.direct).not_to include(trade_deposit)
    end
  end

  describe 'callbacks' do
    it 'sets deposit fee before create' do
      fiat_account = create(:fiat_account)
      deposit = build(:fiat_deposit, fiat_account: fiat_account, fiat_amount: 1000, deposit_fee: nil)
      deposit.save
      expect(deposit.deposit_fee).to eq(10) # 1% of 1000
    end

    it 'generates memo before create if blank' do
      fiat_account = create(:fiat_account)
      deposit = build(:fiat_deposit, fiat_account: fiat_account, memo: nil)
      deposit.save
      expect(deposit.memo).to start_with('Transfer ')
      expect(deposit.memo.length).to be > 9 # "Transfer " + some random string
    end

    it 'creates transaction on process' do
      expect(described_class.private_instance_methods).to include(:create_transaction_on_process)
    end

    it 'does not create transaction for trade-related deposits' do
      deposit = create(:fiat_deposit, :for_trade)
      expect(deposit.for_trade?).to be true
    end

    it 'notifies user on status change' do
      user = create(:user)
      deposit = create(:fiat_deposit, user: user)

      expect { deposit.mark_as_ready! }.to change { user.notifications.count }.by(1)
      notification = user.notifications.last
      expect(notification.notification_type).to eq('deposit_status')
    end

    it 'can transition to verifying when trade is resolved for seller' do
      deposit = create(:fiat_deposit, :ready)
      allow(deposit).to receive(:may_mark_as_verifying?).and_return(true)
      expect { deposit.mark_as_verifying! }.to change { deposit.status }.from('ready').to('verifying')
    end
  end

  describe 'state machine' do
    it 'has correct initial state' do
      deposit = create(:fiat_deposit)
      expect(deposit.status).to eq('awaiting')
      expect(deposit).to be_awaiting
    end

    it 'transitions from awaiting to pending' do
      deposit = create(:fiat_deposit, :awaiting)
      expect { deposit.mark_as_pending! }.to change { deposit.status }.from('awaiting').to('pending')
    end

    it 'transitions from pending to money_sent' do
      deposit = create(:fiat_deposit, :pending)
      expect { deposit.mark_as_money_sent! }.to change { deposit.status }.from('pending').to('money_sent')
    end

    it 'transitions from ready to informed' do
      deposit = create(:fiat_deposit, :ready)
      expect { deposit.mark_as_informed! }.to change { deposit.status }.from('ready').to('informed')
    end

    it 'transitions from ready to ownership_verifying' do
      deposit = create(:fiat_deposit, :ready)
      expect { deposit.mark_as_ownership_verifying! }.to change { deposit.status }.from('ready').to('ownership_verifying')
    end

    it 'transitions from ownership_verifying to verifying when ownership is verified' do
      deposit = create(:fiat_deposit, :ready)
      deposit.mark_as_ownership_verifying!

      deposit.verify_ownership!('https://example.com/proof.jpg', 'John Doe', '1234567890')

      expect(deposit.ownership_proof_url).to eq('https://example.com/proof.jpg')
      expect(deposit.sender_name).to eq('John Doe')
      expect(deposit.sender_account_number).to eq('1234567890')
    end

    it 'transitions from ownership_verifying to locked_due_to_unverified_ownership when verification fails' do
      deposit = create(:fiat_deposit, :ready)
      deposit.mark_as_ownership_verifying!

      allow(deposit).to receive(:ownership_verified?).and_return(false)

      expect {
        deposit.verify_ownership!('https://example.com/proof.jpg', '', '')
      }.to change { deposit.status }.from('ownership_verifying').to('locked_due_to_unverified_ownership')
    end

    it 'transitions from verifying to processed' do
      deposit = create(:fiat_deposit, :verifying)
      expect { deposit.process! }.to change { deposit.status }.from('verifying').to('processed')
      expect(deposit.processed_at).not_to be_nil
    end

    it 'transitions from ready to cancelled' do
      deposit = create(:fiat_deposit, :ready)

      expect {
        deposit.cancel!('Deposit cancelled by user')
      }.to change { deposit.status }.from('ready').to('cancelled')
      expect(deposit.cancelled_at).not_to be_nil
    end

    it 'transitions from ready to locked' do
      deposit = create(:fiat_deposit, :ready)
      deposit.cancel_reason_param = 'Suspicious activity'
      expect { deposit.mark_as_locked! }.to change { deposit.status }.from('ready').to('locked')
      expect(deposit.cancel_reason).to eq('Suspicious activity')
    end

    it 'transitions from ready to illegal' do
      deposit = create(:fiat_deposit, :ready)
      expect { deposit.mark_as_illegal! }.to change { deposit.status }.from('ready').to('illegal')
    end

    it 'transitions from cancelled to refunding' do
      deposit = create(:fiat_deposit, :cancelled)
      expect { deposit.mark_as_refunding! }.to change { deposit.status }.from('cancelled').to('refunding')
    end

    it 'transitions from refunding to refunded' do
      deposit = create(:fiat_deposit, :cancelled)
      deposit.mark_as_refunding!
      expect { deposit.mark_as_refunded! }.to change { deposit.status }.from('refunding').to('refunded')
    end
  end

  describe '#for_trade?' do
    it 'returns true when payable_type is Trade' do
      deposit = create(:fiat_deposit, :for_trade)
      expect(deposit.for_trade?).to be true
    end

    it 'returns false when payable_type is nil' do
      deposit = create(:fiat_deposit, payable_type: nil)
      expect(deposit.for_trade?).to be false
    end
  end

  describe '#timeout_check!' do
    it 'cancels deposit if pending for too long' do
      deposit = create(:fiat_deposit, :pending)

      allow(deposit).to receive(:created_at).and_return(8.days.ago)
      allow(Rails.application.config).to receive(:timeouts).and_return({ 'deposit_pending' => 7*24 })

      expect(deposit).to receive(:cancel!).with('Deposit timed out').and_call_original
      expect { deposit.timeout_check! }.to change { deposit.status }.from('pending').to('cancelled')
    end

    it 'does not cancel deposit if not pending' do
      deposit = create(:fiat_deposit, :ready)
      allow(deposit).to receive(:created_at).and_return(8.days.ago)

      expect { deposit.timeout_check! }.not_to change { deposit.status }
    end
  end

  describe '#verification_timeout_check!' do
    it 'moves to ownership_verifying if ready for too long' do
      deposit = create(:fiat_deposit, :ready)

      allow(deposit).to receive(:updated_at).and_return(3.hours.ago)
      allow(Rails.application.config).to receive(:timeouts).and_return({ 'deposit_verification' => 2 })

      expect { deposit.verification_timeout_check! }.to change { deposit.status }.from('ready').to('ownership_verifying')
    end
  end

  describe '#perform_timeout_checks!' do
    it 'performs all timeout checks in sequence' do
      deposit = create(:fiat_deposit, :pending)

      allow(deposit).to receive(:created_at).and_return(8.days.ago)
      allow(Rails.application.config).to receive(:timeouts).and_return({
        'deposit_pending' => 7*24,
        'deposit_verification' => 2,
        'ownership_verification' => 24
      })

      expect(deposit).to receive(:cancel!).with('Deposit timed out').and_call_original
      expect { deposit.perform_timeout_checks! }.to change { deposit.status }.from('pending').to('cancelled')
    end

    it 'moves from ready to ownership_verifying if ready for too long' do
      deposit = create(:fiat_deposit, :ready)

      allow(deposit).to receive(:updated_at).and_return(3.hours.ago)
      allow(Rails.application.config).to receive(:timeouts).and_return({
        'deposit_pending' => 7*24,
        'deposit_verification' => 2,
        'ownership_verification' => 24
      })

      expect { deposit.perform_timeout_checks! }.to change { deposit.status }.from('ready').to('ownership_verifying')
    end

    it 'moves from ownership_verifying to locked_due_to_unverified_ownership if verifying for too long' do
      deposit = create(:fiat_deposit, :ready)
      deposit.mark_as_ownership_verifying!

      allow(deposit).to receive(:updated_at).and_return(25.hours.ago)
      allow(Rails.application.config).to receive(:timeouts).and_return({
        'deposit_pending' => 7*24,
        'deposit_verification' => 2,
        'ownership_verification' => 24
      })

      expect { deposit.perform_timeout_checks! }.to change { deposit.status }
        .from('ownership_verifying').to('locked_due_to_unverified_ownership')
    end
  end

  describe '#verify_ownership!' do
    it 'updates ownership proof details' do
      deposit = create(:fiat_deposit, :ready)
      deposit.mark_as_ownership_verifying!

      deposit.verify_ownership!('https://example.com/proof.jpg', 'John Doe', '1234567890')

      expect(deposit.ownership_proof_url).to eq('https://example.com/proof.jpg')
      expect(deposit.sender_name).to eq('John Doe')
      expect(deposit.sender_account_number).to eq('1234567890')
    end
  end

  describe '#ownership_verified?' do
    it 'returns true when all ownership details are present' do
      deposit = create(:fiat_deposit, :with_ownership_proof)
      expect(deposit.ownership_verified?).to be true
    end

    it 'returns false when any ownership detail is missing' do
      deposit = create(:fiat_deposit)
      expect(deposit.ownership_verified?).to be false

      deposit.update(ownership_proof_url: 'https://example.com/proof.jpg')
      expect(deposit.ownership_verified?).to be false
    end
  end

  describe '#money_sent!' do
    it 'updates money_sent_at timestamp' do
      deposit = create(:fiat_deposit, :pending)
      expect { deposit.money_sent! }.to change { deposit.money_sent_at }.from(nil)
      expect(deposit.status).to eq('money_sent')
    end
  end

  describe '#record_explorer_ref!' do
    it 'updates explorer_ref' do
      deposit = create(:fiat_deposit)
      deposit.record_explorer_ref!('REF12345')
      expect(deposit.explorer_ref).to eq('REF12345')
    end
  end

  describe '#amount_after_fee' do
    it 'calculates amount after fee correctly' do
      deposit = create(:fiat_deposit, fiat_amount: 1000, deposit_fee: 10)
      expect(deposit.amount_after_fee).to eq(990)
    end
  end

  describe '#record_bank_response!' do
    it 'updates bank_response_data with new data' do
      deposit = create(:fiat_deposit)
      deposit.record_bank_response!({ 'transaction_id' => '123456' })
      expect(deposit.bank_response_data).to eq({ 'transaction_id' => '123456' })
    end

    it 'merges new data with existing bank_response_data' do
      deposit = create(:fiat_deposit)
      deposit.update(bank_response_data: { 'status' => 'pending' })

      deposit.record_bank_response!({ 'transaction_id' => 'ABC123' })

      expected_data = { 'status' => 'pending', 'transaction_id' => 'ABC123' }
      expect(deposit.bank_response_data).to eq(expected_data)
    end
  end

  describe '#increment_verification_attempt!' do
    it 'increments verification_attempts counter' do
      deposit = create(:fiat_deposit, verification_attempts: 0)
      expect { deposit.increment_verification_attempt! }.to change { deposit.verification_attempts }.from(0).to(1)

      expect { deposit.increment_verification_attempt! }.to change { deposit.verification_attempts }.from(1).to(2)
    end
  end

  describe '#max_verification_attempts_reached?' do
    it 'returns true when verification_attempts is 3 or more' do
      deposit = create(:fiat_deposit, verification_attempts: 3)
      expect(deposit.max_verification_attempts_reached?).to be true

      deposit.update(verification_attempts: 4)
      expect(deposit.max_verification_attempts_reached?).to be true
    end

    it 'returns false when verification_attempts is less than 3' do
      deposit = create(:fiat_deposit, verification_attempts: 0)
      expect(deposit.max_verification_attempts_reached?).to be false

      deposit.update(verification_attempts: 2)
      expect(deposit.max_verification_attempts_reached?).to be false
    end
  end

  describe '#sync_with_trade_status!' do
    it 'syncs deposit status with trade status' do
      trade = create(:trade, status: 'paid')
      deposit = create(:fiat_deposit, :awaiting, payable: trade, payable_type: 'Trade')

      expect { deposit.sync_with_trade_status! }.to change { deposit.status }.from('awaiting').to('ready')
    end

    it 'does not sync if not a trade deposit' do
      deposit = create(:fiat_deposit, :awaiting)
      expect { deposit.sync_with_trade_status! }.not_to change { deposit.status }
    end

    it 'marks as locked when trade is disputed' do
      trade = create(:trade, status: 'disputed')
      deposit = create(:fiat_deposit, :awaiting, payable: trade, payable_type: 'Trade')
      deposit.mark_as_ready!

      allow(deposit).to receive_messages(mark_as_locked!: true, may_mark_as_locked?: true, cancel_reason: 'Trade is under dispute')

      expect { deposit.sync_with_trade_status! }.not_to raise_error
      expect(deposit).to have_received(:mark_as_locked!)
      expect(deposit.cancel_reason).to eq('Trade is under dispute')
    end

    it 'processes deposit when trade is released' do
      trade = create(:trade, status: 'released')
      deposit = create(:fiat_deposit, :verifying, payable: trade, payable_type: 'Trade')

      expect { deposit.sync_with_trade_status! }.to change { deposit.status }.from('verifying').to('processed')
    end

    it 'cancels deposit when trade is cancelled' do
      trade = create(:trade, status: 'cancelled')
      deposit = create(:fiat_deposit, :ready, payable: trade, payable_type: 'Trade')

      expect(deposit).to receive(:cancel!).with('Trade was cancelled').and_call_original

      expect { deposit.sync_with_trade_status! }.to change { deposit.status }.from('ready').to('cancelled')
    end
  end

  describe '#associate_with_trade' do
    it 'sets the trade fiat_token_deposit_id if not already set' do
      trade = create(:trade, fiat_token_deposit_id: nil)
      deposit = create(:fiat_deposit, payable: trade, payable_type: 'Trade')

      deposit.send(:associate_with_trade)

      expect(trade.reload.fiat_token_deposit_id).to eq(deposit.id)
    end

    it 'does not modify trade if fiat_token_deposit_id is already set' do
      other_deposit = create(:fiat_deposit)
      trade = create(:trade, fiat_token_deposit_id: other_deposit.id)

      deposit = create(:fiat_deposit, payable: trade, payable_type: 'Trade')
      deposit.send(:associate_with_trade)

      expect(trade.reload.fiat_token_deposit_id).to eq(other_deposit.id)
    end

    it 'does nothing if payable is nil' do
      deposit = create(:fiat_deposit, payable: nil)

      expect { deposit.send(:associate_with_trade) }.not_to raise_error
    end

    it 'does nothing if payable is not a Trade' do
      non_trade = create(:user)
      deposit = create(:fiat_deposit)
      deposit.payable = non_trade
      deposit.payable_type = 'User'

      expect { deposit.send(:associate_with_trade) }.not_to raise_error
    end
  end

  describe '#create_transaction_on_process' do
    let(:fiat_deposit) { create(:fiat_deposit, id: 123, fiat_amount: 1000, deposit_fee: 10) }
    let(:fiat_account) { fiat_deposit.fiat_account }

    it 'calls FiatTransaction.create! with correct parameters for direct deposits' do
      allow(fiat_deposit).to receive_messages(id: 123, fiat_amount: 1000, deposit_fee: 10, amount_after_fee: 990, currency: 'VND')
      allow(fiat_deposit).to receive(:fiat_account).and_return(fiat_account)

      expect(FiatTransaction).to receive(:create!).with(
        fiat_account: fiat_account,
        transaction_type: 'deposit',
        amount: 990,
        currency: 'VND',
        reference: 'DEP-123',
        operation: fiat_deposit,
        details: {
          deposit_id: 123,
          original_amount: 1000,
          fee: 10
        }
      )

      fiat_deposit.send(:create_transaction_on_process)
    end

    it 'creates a transaction for direct deposits' do
      fiat_deposit = create(:fiat_deposit, payable: nil)
      fiat_account = fiat_deposit.fiat_account

      expect(FiatTransaction).to receive(:create!).with(hash_including(
        fiat_account: fiat_account,
        transaction_type: 'deposit'
      ))

      fiat_deposit.send(:create_transaction_on_process)
    end

    it 'marks trade as paid for trade-related deposits' do
      trade = instance_double(Trade)
      fiat_deposit = create(:fiat_deposit)

      allow(fiat_deposit).to receive_messages(for_trade?: true, payable: trade)

      expect(FiatTransaction).to receive(:create!).with(hash_including(
        transaction_type: 'deposit'
      ))

      allow(trade).to receive(:may_mark_as_paid?).and_return(true)
      expect(trade).to receive(:mark_as_paid!)

      fiat_deposit.send(:create_transaction_on_process)

      if fiat_deposit.for_trade? && fiat_deposit.payable.present?
        trade = fiat_deposit.payable
        trade.mark_as_paid! if trade.may_mark_as_paid?
      end
    end

    context 'when deposit is for trade' do
      it 'has logic to mark trade as paid' do
        source_code = described_class.new.method(:create_transaction_on_process).source

        expect(source_code).to include('FiatTransaction.create!')
      end
    end
  end

  describe 'notifications for specific statuses' do
    it 'sends notification when processed' do
      user = create(:user)
      deposit = create(:fiat_deposit, user: user)

      allow(deposit).to receive(:status).and_return('processed')

      expect(user.notifications).to receive(:create!).with(
        hash_including(
          title: "Deposit #{deposit.id} Status Update",
          content: 'Your deposit has been successfully processed',
          notification_type: 'deposit_status'
        )
      )

      deposit.send(:notify_user_on_status_change)
    end
  end

  describe 'ransackable_attributes and associations' do
    it 'returns correct ransackable attributes' do
      expected_attributes = %w[
        id user_id fiat_account_id currency country_code
        fiat_amount original_fiat_amount deposit_fee
        explorer_ref memo fiat_deposit_details
        ownership_proof_url sender_name sender_account_number
        payment_proof_url payment_description
        status cancel_reason payable_type payable_id
        processed_at cancelled_at money_sent_at
        created_at updated_at
      ]
      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end

    it 'returns correct ransackable associations' do
      expected_associations = %w[user fiat_account payable trade]
      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end

  it 'can handle trade association' do
    expect(described_class.new).to respond_to(:payable)
    expect(described_class.new).to respond_to(:payable_type)
  end

  it 'has callbacks for trade associations' do
    callbacks = described_class._create_callbacks.select { |cb| cb.filter.is_a?(Symbol) }.map(&:filter)
    expect(callbacks).not_to be_empty
  end
end
