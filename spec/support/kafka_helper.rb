module KafkaHelper
  def self.included(base)
    base.before do
      # Skip mocking for producer tests
      next if base.metadata[:file_path].include?('producer_spec.rb')

      allow_any_instance_of(KafkaService::Base::Producer).to receive(:send_message)
      allow_any_instance_of(KafkaService::Base::Producer).to receive(:send_messages_batch)
      allow_any_instance_of(KafkaService::Base::Producer).to receive(:close)
    end
  end
end

RSpec.configure do |config|
  config.include KafkaHelper, type: :model
  config.include KafkaHelper, type: :service
end
