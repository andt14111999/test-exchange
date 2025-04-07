# frozen_string_literal: true

require 'rails_helper'
require 'kaminari'

RSpec.describe Base::Helpers::Meta do
  include described_class

  before do
    stub_const('TestPagination', Class.new do
      def current_page; end
      def next_page; end
      def total_pages; end
      def limit_value; end
    end)
  end

  describe '#generate_meta' do
    it 'returns correct meta information for paginated object' do
      paginated_object = instance_double(TestPagination,
        current_page: 2,
        next_page: 3,
        total_pages: 5,
        limit_value: 10
      )

      meta = generate_meta(paginated_object)

      expect(meta).to eq(
        current_page: 2,
        next_page: 3,
        total_pages: 5,
        per_page: 10
      )
    end
  end

  describe '#generate_meta_with_default' do
    it 'returns default meta information when object is nil' do
      meta = generate_meta_with_default(nil)

      expect(meta).to eq(
        current_page: 1,
        next_page: nil,
        total_pages: 0,
        per_page: 9
      )
    end

    it 'returns correct meta information for paginated object' do
      paginated_object = instance_double(TestPagination,
        current_page: 2,
        next_page: 3,
        total_pages: 5,
        limit_value: 10
      )

      meta = generate_meta_with_default(paginated_object)

      expect(meta).to eq(
        current_page: 2,
        next_page: 3,
        total_pages: 5,
        per_page: 10
      )
    end

    it 'returns default values for nil attributes' do
      paginated_object = instance_double(TestPagination,
        current_page: nil,
        next_page: nil,
        total_pages: nil,
        limit_value: nil
      )

      meta = generate_meta_with_default(paginated_object)

      expect(meta).to eq(
        current_page: 1,
        next_page: nil,
        total_pages: 0,
        per_page: 9
      )
    end
  end
end
