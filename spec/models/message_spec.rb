# frozen_string_literal: true

require 'rails_helper'

describe Message, type: :model do
  describe 'associations' do
    it 'belongs to trade' do
      expect(described_class.new).to belong_to(:trade)
    end

    it 'belongs to user' do
      expect(described_class.new).to belong_to(:user)
    end
  end

  describe 'validations' do
    it 'validates presence of body' do
      message = build(:message, body: nil)
      expect(message).to be_invalid
      expect(message.errors[:body]).to include("can't be blank")
    end

    it 'validates is_receipt_proof is boolean' do
      message = build(:message, is_receipt_proof: nil)
      expect(message).to be_invalid
      expect(message.errors[:is_receipt_proof]).to include('is not included in the list')
    end

    it 'validates is_system is boolean' do
      message = build(:message, is_system: nil)
      expect(message).to be_invalid
      expect(message.errors[:is_system]).to include('is not included in the list')
    end
  end

  describe 'scopes' do
    it 'filters receipt proofs' do
      receipt_proof = create(:message, is_receipt_proof: true)
      regular_message = create(:message, is_receipt_proof: false)

      receipt_proofs = described_class.receipt_proofs
      expect(receipt_proofs).to include(receipt_proof)
      expect(receipt_proofs).not_to include(regular_message)
    end

    it 'filters system messages' do
      system_message = create(:message, is_system: true)
      user_message = create(:message, is_system: false)

      system_messages = described_class.system_messages
      expect(system_messages).to include(system_message)
      expect(system_messages).not_to include(user_message)
    end

    it 'filters user messages' do
      system_message = create(:message, is_system: true)
      user_message = create(:message, is_system: false)

      user_messages = described_class.user_messages
      expect(user_messages).to include(user_message)
      expect(user_messages).not_to include(system_message)
    end

    it 'sorts by created_at in ascending order' do
      old_message = create(:message, created_at: 2.days.ago)
      new_message = create(:message, created_at: 1.day.ago)

      sorted_messages = described_class.sorted
      expect(sorted_messages.first).to eq(old_message)
      expect(sorted_messages.last).to eq(new_message)
    end
  end

  describe 'instance methods' do
    it 'marks message as receipt proof' do
      message = create(:message, is_receipt_proof: false)
      message.mark_as_receipt_proof!
      expect(message.is_receipt_proof).to be true
    end

    it 'marks message as regular message' do
      message = create(:message, is_receipt_proof: true)
      message.mark_as_regular_message!
      expect(message.is_receipt_proof).to be false
    end
  end

  describe '.ransackable_attributes' do
    it 'returns allowed attributes for ransack' do
      expected_attributes = %w[
        id trade_id user_id body
        is_receipt_proof is_system
        created_at updated_at
      ]

      expect(described_class.ransackable_attributes).to match_array(expected_attributes)
    end
  end

  describe '.ransackable_associations' do
    it 'returns allowed associations for ransack' do
      expected_associations = %w[trade user]
      expect(described_class.ransackable_associations).to match_array(expected_associations)
    end
  end
end
