# frozen_string_literal: true

require 'rails_helper'

RSpec.describe MerchantEscrow, type: :model do
  describe 'associations' do
    it { is_expected.to belong_to(:user) }
    it { is_expected.to belong_to(:usdt_account).class_name('CoinAccount') }
    it { is_expected.to belong_to(:fiat_account).class_name('FiatAccount') }
    it { is_expected.to have_many(:merchant_escrow_operations).dependent(:destroy) }
  end

  describe 'validations' do
    it { is_expected.to validate_presence_of(:usdt_amount) }
    it { is_expected.to validate_numericality_of(:usdt_amount).is_greater_than(0) }
    it { is_expected.to validate_presence_of(:fiat_amount) }
    it { is_expected.to validate_numericality_of(:fiat_amount).is_greater_than(0) }
    it { is_expected.to validate_presence_of(:fiat_currency) }
    it { is_expected.to validate_presence_of(:status) }
    it { is_expected.to validate_inclusion_of(:status).in_array(%w[pending active cancelled]) }
    it { is_expected.to validate_numericality_of(:exchange_rate).is_greater_than(0).allow_nil }

    describe 'validate_user_is_merchant' do
      it 'is valid when user is a merchant' do
        merchant = create(:user, :merchant)
        merchant_escrow = build(:merchant_escrow, user: merchant)

        expect(merchant_escrow).to be_valid
      end

      it 'is invalid when user is not a merchant' do
        user = create(:user)
        merchant_escrow = build(:merchant_escrow, user: user)

        expect(merchant_escrow).to be_invalid
        expect(merchant_escrow.errors[:user]).to include('must be a merchant')
      end
    end
  end

  describe 'scopes' do
    it 'sorted returns records in descending order of creation' do
      old_escrow = create(:merchant_escrow, created_at: 2.days.ago)
      new_escrow = create(:merchant_escrow, created_at: 1.day.ago)

      expect(described_class.sorted).to eq([ new_escrow, old_escrow ])
    end

    it 'active returns records with active status' do
      active_escrow = create(:merchant_escrow, status: 'active')
      create(:merchant_escrow, status: 'pending')

      expect(described_class.active).to eq([ active_escrow ])
    end

    it 'cancelled returns records with cancelled status' do
      cancelled_escrow = create(:merchant_escrow, status: 'cancelled')
      create(:merchant_escrow, status: 'pending')

      expect(described_class.cancelled).to eq([ cancelled_escrow ])
    end

    it 'pending returns records with pending status' do
      pending_escrow = create(:merchant_escrow, status: 'pending')
      create(:merchant_escrow, status: 'active')

      expect(described_class.pending).to eq([ pending_escrow ])
    end
  end

  describe 'state machine' do
    it 'starts in pending state' do
      merchant_escrow = create(:merchant_escrow)
      expect(merchant_escrow).to be_pending
    end

    it 'transitions from pending to active' do
      merchant_escrow = create(:merchant_escrow, status: 'pending')
      expect(merchant_escrow.activate).to be true
      expect(merchant_escrow).to be_active
    end

    it 'transitions from pending to cancelled' do
      merchant_escrow = create(:merchant_escrow, status: 'pending')
      expect(merchant_escrow.cancel).to be true
      expect(merchant_escrow).to be_cancelled
    end

    it 'transitions from active to cancelled' do
      merchant_escrow = create(:merchant_escrow, status: 'active')
      expect(merchant_escrow.cancel).to be true
      expect(merchant_escrow).to be_cancelled
    end
  end

  describe 'ransackable attributes and associations' do
    it 'returns allowed ransackable attributes' do
      expected_attributes = %w[
        id user_id usdt_account_id fiat_account_id usdt_amount fiat_amount
        fiat_currency exchange_rate status created_at updated_at
      ]

      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end

    it 'returns allowed ransackable associations' do
      expected_associations = %w[user usdt_account fiat_account merchant_escrow_operations]

      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end

  describe '#can_cancel?' do
    it 'returns true when status is pending' do
      merchant_escrow = create(:merchant_escrow, status: 'pending')
      expect(merchant_escrow.can_cancel?).to be true
    end

    it 'returns true when status is active' do
      merchant_escrow = create(:merchant_escrow, status: 'active')
      expect(merchant_escrow.can_cancel?).to be true
    end

    it 'returns false when status is cancelled' do
      merchant_escrow = create(:merchant_escrow, status: 'cancelled')
      expect(merchant_escrow.can_cancel?).to be false
    end
  end

  describe '#activate!' do
    it 'updates status to active' do
      merchant_escrow = create(:merchant_escrow, status: 'pending')
      merchant_escrow.activate!
      expect(merchant_escrow.reload.status).to eq('active')
    end
  end

  describe '#cancel!' do
    it 'updates status to cancelled' do
      merchant_escrow = create(:merchant_escrow, status: 'active')
      merchant_escrow.cancel!
      expect(merchant_escrow.reload.status).to eq('cancelled')
    end
  end

  describe '#find_user_usdt_account' do
    it 'returns the main USDT account of the user' do
      user = create(:user, :merchant)
      usdt_account = create(:coin_account, :usdt_main, user: user)
      merchant_escrow = create(:merchant_escrow, user: user)

      expect(merchant_escrow.find_user_usdt_account).to eq(usdt_account)
    end
  end

  describe '#find_user_fiat_account' do
    it 'returns the first fiat account of the user with matching currency' do
      user = create(:user, :merchant)
      fiat_account = create(:fiat_account, :vnd, user: user)
      merchant_escrow = create(:merchant_escrow, user: user, fiat_currency: 'VND')

      expect(merchant_escrow.find_user_fiat_account).to eq(fiat_account)
    end
  end
end
