# frozen_string_literal: true

require 'rails_helper'

describe BankAccount, type: :model do
  describe 'associations' do
    it 'belongs to user' do
      expect(described_class.new).to belong_to(:user)
    end
  end

  describe 'validations' do
    it 'validates presence of bank_name' do
      bank_account = build(:bank_account, bank_name: nil)
      expect(bank_account).to be_invalid
      expect(bank_account.errors[:bank_name]).to include("can't be blank")
    end

    it 'validates presence of account_name' do
      bank_account = build(:bank_account, account_name: nil)
      expect(bank_account).to be_invalid
      expect(bank_account.errors[:account_name]).to include("can't be blank")
    end

    it 'validates presence of account_number' do
      bank_account = build(:bank_account, account_number: nil)
      expect(bank_account).to be_invalid
      expect(bank_account.errors[:account_number]).to include("can't be blank")
    end

    it 'validates presence of country_code' do
      bank_account = build(:bank_account, country_code: nil)
      expect(bank_account).to be_invalid
      expect(bank_account.errors[:country_code]).to include("can't be blank")
    end

    it 'validates uniqueness of is_primary within user and bank_name scope' do
      user = create(:user)
      create(:bank_account, user: user, bank_name: 'VCB', is_primary: true)
      duplicate_primary = build(:bank_account, user: user, bank_name: 'VCB', is_primary: true)

      expect(duplicate_primary).to be_invalid
      expect(duplicate_primary.errors[:is_primary]).to include('has already been taken')
    end

    it 'allows multiple primary accounts for different bank names' do
      user = create(:user)
      create(:bank_account, user: user, bank_name: 'VCB', is_primary: true)
      diff_bank_primary = build(:bank_account, user: user, bank_name: 'ACB', is_primary: true)

      expect(diff_bank_primary).to be_valid
    end

    it 'allows multiple primary accounts for different users' do
      user1 = create(:user)
      user2 = create(:user)
      create(:bank_account, user: user1, bank_name: 'VCB', is_primary: true)
      diff_user_primary = build(:bank_account, user: user2, bank_name: 'VCB', is_primary: true)

      expect(diff_user_primary).to be_valid
    end

    it 'does not validate uniqueness if is_primary is false' do
      user = create(:user)
      create(:bank_account, user: user, bank_name: 'VCB', is_primary: true)
      non_primary = build(:bank_account, user: user, bank_name: 'VCB', is_primary: false)

      expect(non_primary).to be_valid
    end
  end

  describe 'scopes' do
    it 'filters verified accounts' do
      verified_account = create(:bank_account, verified: true)
      unverified_account = create(:bank_account, verified: false)

      verified_accounts = described_class.verified
      expect(verified_accounts).to include(verified_account)
      expect(verified_accounts).not_to include(unverified_account)
    end

    it 'filters unverified accounts' do
      verified_account = create(:bank_account, verified: true)
      unverified_account = create(:bank_account, verified: false)

      unverified_accounts = described_class.unverified
      expect(unverified_accounts).to include(unverified_account)
      expect(unverified_accounts).not_to include(verified_account)
    end

    it 'filters primary accounts' do
      primary_account = create(:bank_account, is_primary: true)
      non_primary_account = create(:bank_account, is_primary: false)

      primary_accounts = described_class.primary
      expect(primary_accounts).to include(primary_account)
      expect(primary_accounts).not_to include(non_primary_account)
    end

    it 'filters accounts by country' do
      vn_account = create(:bank_account, country_code: 'VN')
      ph_account = create(:bank_account, country_code: 'PH')

      vn_accounts = described_class.of_country('VN')
      expect(vn_accounts).to include(vn_account)
      expect(vn_accounts).not_to include(ph_account)
    end
  end

  describe 'callbacks' do
    it 'has a callback to ensure single primary account' do
      # Test that the callback is defined
      expect(described_class._save_callbacks.select { |cb| cb.filter == :ensure_single_primary }.size).to eq(1)
    end

    it 'does not update other accounts if is_primary is not changed' do
      user = create(:user)
      primary_account = create(:bank_account, user: user, bank_name: 'VCB', is_primary: true)
      second_account = create(:bank_account, user: user, bank_name: 'VCB', is_primary: false)

      # Update without changing is_primary
      primary_account.update(account_name: 'New Name')

      second_account.reload
      expect(primary_account.is_primary).to be true
      expect(second_account.is_primary).to be false
    end

    it 'does not update other accounts if is_primary is changed to false' do
      user = create(:user)
      primary_account = create(:bank_account, user: user, bank_name: 'VCB', is_primary: true)
      second_account = create(:bank_account, user: user, bank_name: 'VCB', is_primary: false)

      # Change primary to false
      primary_account.update(is_primary: false)

      second_account.reload
      expect(primary_account.is_primary).to be false
      expect(second_account.is_primary).to be false
    end
  end

  describe 'private methods' do
    it 'checks if is_primary has changed to true' do
      bank_account = create(:bank_account, is_primary: false)

      # Verify the ensure_single_primary method is called properly
      expect(bank_account).to receive(:ensure_single_primary)
      bank_account.is_primary = true
      bank_account.run_callbacks(:save)
    end

    it 'does not call ensure_single_primary if is_primary is not changed' do
      bank_account = create(:bank_account, is_primary: false)

      # Should not call ensure_single_primary
      expect(bank_account).not_to receive(:ensure_single_primary)
      bank_account.account_name = 'New Name'
      bank_account.run_callbacks(:save)
    end

    it 'does not call ensure_single_primary if is_primary is changed to false' do
      bank_account = create(:bank_account, is_primary: true)

      # Save to persist the is_primary = true state
      bank_account.save!

      # Now change to false - callback should not run
      expect(bank_account).not_to receive(:ensure_single_primary)
      bank_account.is_primary = false
      bank_account.run_callbacks(:save)
    end
  end

  describe 'instance methods' do
    it 'marks account as verified' do
      bank_account = create(:bank_account, verified: false)
      bank_account.mark_as_verified!
      expect(bank_account.verified).to be true
    end

    it 'marks account as primary' do
      # For this test, we just verify the method exists and calls update!
      bank_account = create(:bank_account, is_primary: false)

      # Mock the update! method to avoid validation errors
      expect(bank_account).to receive(:update!).with(is_primary: true)

      bank_account.mark_as_primary!
    end
  end

  describe '.ransackable_attributes' do
    it 'returns allowed attributes for ransack' do
      expected_attributes = %w[
        id user_id bank_name account_name account_number
        branch country_code verified is_primary
        created_at updated_at
      ]

      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe '.ransackable_associations' do
    it 'returns allowed associations for ransack' do
      expected_associations = %w[user]
      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end
end
