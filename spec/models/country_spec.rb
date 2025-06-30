require 'rails_helper'

RSpec.describe Country, type: :model do
  describe 'validations' do
    it { is_expected.to validate_presence_of(:name) }
    it { is_expected.to validate_presence_of(:code) }
    it { is_expected.to validate_uniqueness_of(:code) }
  end

  describe 'associations' do
    it { is_expected.to have_many(:banks).dependent(:restrict_with_error) }
  end

  describe 'scopes' do
    let!(:vietnam) { create(:country, name: 'Vietnam', code: 'VN') }
    let!(:nigeria) { create(:country, name: 'Nigeria', code: 'NG') }
    let!(:ghana) { create(:country, name: 'Ghana', code: 'GH') }

    describe '.ordered' do
      it 'returns countries ordered by name' do
        expect(Country.ordered).to eq([ ghana, nigeria, vietnam ])
      end
    end
  end

  describe 'factory' do
    it 'has a valid factory' do
      expect(build(:country)).to be_valid
    end
  end
end
