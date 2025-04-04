require 'rails_helper'

RSpec.describe Admin::StatusHelper, type: :helper do
  describe '#status_class' do
    context 'when status is pending' do
      it 'returns warning' do
        expect(helper.status_class('pending')).to eq('warning')
      end
    end

    context 'when status is verified' do
      it 'returns ok' do
        expect(helper.status_class('verified')).to eq('ok')
      end
    end

    context 'when status is locked' do
      it 'returns error' do
        expect(helper.status_class('locked')).to eq('error')
      end
    end

    context 'when status is rejected' do
      it 'returns error' do
        expect(helper.status_class('rejected')).to eq('error')
      end
    end

    context 'when status is forged' do
      it 'returns error' do
        expect(helper.status_class('forged')).to eq('error')
      end
    end

    context 'when status is not defined' do
      it 'returns nil' do
        expect(helper.status_class('unknown')).to be_nil
      end
    end
  end
end 