# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Services::IdentifierBuilderService, type: :service do
  describe '.build_merchant_escrow_identifier' do
    it 'returns correctly formatted merchant escrow identifier' do
      escrow_id = 123
      result = described_class.build_merchant_escrow_identifier(escrow_id: escrow_id)
      expect(result).to eq("merchant-escrow-#{escrow_id}")
    end
  end

  describe '.build_deposit_identifier' do
    it 'returns correctly formatted deposit identifier' do
      deposit_id = 456
      result = described_class.build_deposit_identifier(deposit_id: deposit_id)
      expect(result).to eq("deposit-#{deposit_id}")
    end
  end

  describe '.build_withdrawal_identifier' do
    it 'returns correctly formatted withdrawal identifier' do
      withdrawal_id = 789
      result = described_class.build_withdrawal_identifier(withdrawal_id: withdrawal_id)
      expect(result).to eq("withdrawal-#{withdrawal_id}")
    end
  end

  describe '.build_fiat_deposit_identifier' do
    it 'returns correctly formatted fiat deposit identifier' do
      deposit_id = 101112
      result = described_class.build_fiat_deposit_identifier(deposit_id: deposit_id)
      expect(result).to eq("fiat-deposit-#{deposit_id}")
    end
  end

  describe '.build_fiat_withdrawal_identifier' do
    it 'returns correctly formatted fiat withdrawal identifier' do
      withdrawal_id = 131415
      result = described_class.build_fiat_withdrawal_identifier(withdrawal_id: withdrawal_id)
      expect(result).to eq("fiat-withdrawal-#{withdrawal_id}")
    end
  end
end
