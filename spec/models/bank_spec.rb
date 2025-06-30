require 'rails_helper'

RSpec.describe Bank, type: :model do
  describe 'validations' do
    subject { build(:bank) }

    it { is_expected.to validate_presence_of(:name) }
    it { is_expected.to validate_presence_of(:code) }
    it { is_expected.to validate_uniqueness_of(:code) }
    it { is_expected.to validate_presence_of(:bin) }
    it { is_expected.to validate_uniqueness_of(:bin) }
    it { is_expected.to validate_presence_of(:short_name) }
    it { is_expected.to validate_presence_of(:support) }
    it { is_expected.to validate_numericality_of(:support).only_integer.is_greater_than_or_equal_to(0) }
    it { is_expected.to validate_inclusion_of(:transfer_supported).in_array([ true, false ]) }
    it { is_expected.to validate_inclusion_of(:lookup_supported).in_array([ true, false ]) }
    it { is_expected.to validate_inclusion_of(:is_transfer).in_array([ true, false ]) }
  end

  describe 'associations' do
    it { is_expected.to belong_to(:country) }
  end

  describe 'scopes' do
    let!(:vietnam) { create(:country, code: 'VN') }
    let!(:nigeria) { create(:country, code: 'NG') }
    let!(:bank_vn) { create(:bank, country: vietnam, name: 'VietinBank', code: 'ICB') }
    let!(:bank_ng) { create(:bank, country: nigeria, name: 'GTBank', code: 'GTB') }

    describe '.ordered' do
      it 'returns banks ordered by name' do
        expect(described_class.ordered).to eq([ bank_ng, bank_vn ])
      end
    end

    describe '.by_country' do
      it 'returns banks for specific country' do
        expect(described_class.by_country('VN')).to eq([ bank_vn ])
        expect(described_class.by_country('NG')).to eq([ bank_ng ])
      end
    end

    describe '.transfer_supported' do
      let!(:bank_transfer) { create(:bank, country: vietnam, transfer_supported: true) }
      let!(:bank_no_transfer) { create(:bank, country: vietnam, transfer_supported: false) }

      it 'returns only banks with transfer support' do
        expect(described_class.transfer_supported).to include(bank_transfer)
        expect(described_class.transfer_supported).not_to include(bank_no_transfer)
      end
    end

    describe '.lookup_supported' do
      let!(:bank_lookup) { create(:bank, country: vietnam, lookup_supported: true) }
      let!(:bank_no_lookup) { create(:bank, country: vietnam, lookup_supported: false) }

      it 'returns only banks with lookup support' do
        expect(described_class.lookup_supported).to include(bank_lookup)
        expect(described_class.lookup_supported).not_to include(bank_no_lookup)
      end
    end
  end

  describe 'factory' do
    it 'has a valid factory' do
      expect(build(:bank)).to be_valid
    end
  end
end
