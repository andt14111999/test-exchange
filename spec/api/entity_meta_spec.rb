require 'rails_helper'

RSpec.describe EntityMeta do
  describe 'exposed attributes' do
    let(:empty_entity) { described_class.represent({}) }
    let(:serialized) { empty_entity.as_json }

    it 'exposes current_page' do
      expect(serialized).to have_key(:current_page)
    end

    it 'exposes next_page' do
      expect(serialized).to have_key(:next_page)
    end

    it 'exposes total_pages' do
      expect(serialized).to have_key(:total_pages)
    end

    it 'exposes per_page' do
      expect(serialized).to have_key(:per_page)
    end
  end

  describe 'representation' do
    let(:meta_data) do
      {
        current_page: 1,
        next_page: 2,
        total_pages: 5,
        per_page: 20
      }
    end

    it 'represents meta data correctly' do
      entity = described_class.represent(meta_data)
      expect(entity.as_json).to eq(meta_data)
    end

    it 'handles nil values' do
      entity = described_class.represent({})
      expect(entity.as_json).to eq({
        current_page: nil,
        next_page: nil,
        total_pages: nil,
        per_page: nil
      })
    end
  end
end
