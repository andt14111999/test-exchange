# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Operation, type: :model do
  describe '#display_name' do
    it 'returns class name and id' do
      user = create(:user)
      withdrawal = create(:coin_withdrawal, user: user)
      operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
      expect(operation.display_name).to eq("CoinWithdrawalOperation ##{operation.id}")
    end
  end

  describe '#html_view' do
    it 'returns display_name' do
      user = create(:user)
      withdrawal = create(:coin_withdrawal, user: user)
      operation = create(:coin_withdrawal_operation, coin_withdrawal: withdrawal)
      expect(operation.html_view).to eq("CoinWithdrawalOperation ##{operation.id}")
    end
  end
end
