# frozen_string_literal: true

class SidekiqMethod
  include Sidekiq::Worker

  def perform(class_name, id, method_name, *args)
    klass = class_name.constantize
    record = klass.find(id)
    if args.first.is_a?(Hash)
      kwargs = args.first.transform_keys(&:to_sym)
      record.send(method_name, **kwargs)
    else
      record.send(method_name, *args)
    end
  end

  def self.enqueue_to(queue, record, method_name, *args)
    set(queue: queue).perform_in(2.seconds, record.class.name, record.id, method_name.to_s, *args)
  end
end
