# frozen_string_literal: true

require 'rails_helper'

RSpec.describe Ransackable, type: :model do
  before do
    ActiveRecord::Base.connection.create_table :test_models, force: true do |t|
      t.string :name
      t.string :email
      t.timestamps
    end
stub_const('TestModel', test_model_class)
  end

  after do
    ActiveRecord::Base.connection.drop_table :test_models
  end

  let(:test_model_class) do
    Class.new(ApplicationRecord) do
      include Ransackable
      self.table_name = 'test_models'

      has_many :test_associations
      has_one :test_singular_association
    end
  end


  describe 'class methods' do
    describe '.ransackable_attributes' do
      it 'returns all column names by default' do
        expect(TestModel.ransackable_attributes).to contain_exactly('id', 'name', 'email', 'created_at', 'updated_at')
      end

      it 'excludes disabled attributes' do
        TestModel.define_singleton_method(:disabled_ransackable_attributes) do
          [ 'email' ]
        end

        expect(TestModel.ransackable_attributes).to contain_exactly('id', 'name', 'created_at', 'updated_at')
      end

      it 'caches the result' do
        expect(TestModel).to receive(:column_names).once.and_return([ 'id', 'name' ])
        TestModel.ransackable_attributes
        TestModel.ransackable_attributes
      end
    end

    describe '.ransackable_associations' do
      it 'returns all association names by default' do
        expect(TestModel.ransackable_associations).to contain_exactly('test_associations', 'test_singular_association')
      end

      it 'excludes disabled associations' do
        TestModel.define_singleton_method(:disabled_ransackable_associations) do
          [ 'test_singular_association' ]
        end

        expect(TestModel.ransackable_associations).to contain_exactly('test_associations')
      end

      it 'caches the result' do
        expect(TestModel).to receive(:reflect_on_all_associations).once.and_return([])
        TestModel.ransackable_associations
        TestModel.ransackable_associations
      end
    end

    describe '.disabled_ransackable_attributes' do
      it 'returns empty array by default' do
        expect(TestModel.disabled_ransackable_attributes).to eq([])
      end
    end

    describe '.disabled_ransackable_associations' do
      it 'returns empty array by default' do
        expect(TestModel.disabled_ransackable_associations).to eq([])
      end
    end
  end
end
