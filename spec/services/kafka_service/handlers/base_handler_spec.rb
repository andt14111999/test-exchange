# frozen_string_literal: true

require 'rails_helper'

RSpec.describe KafkaService::Handlers::BaseHandler, type: :service do
  describe '#handle' do
    it 'raises NotImplementedError' do
      handler = described_class.new
      expect { handler.handle({}) }.to raise_error(NotImplementedError, 'Subclasses must implement handle method')
    end
  end
end
