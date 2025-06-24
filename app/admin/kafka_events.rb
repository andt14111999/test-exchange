# frozen_string_literal: true

ActiveAdmin.register KafkaEvent do
  menu priority: 100

  actions :index, :show

  filter :topic_name
  filter :event_id
  filter :status, as: :select, collection: %w[pending processed failed]
  filter :operation_type
  filter :object_identifier
  filter :created_at
  filter :processed_at

  index do
    selectable_column
    id_column

    column :topic_name
    column :event_id
    column :status do |event|
      status_tag event.status
    end
    column :operation_type
    column :object_identifier
    column :processing_time do |event|
      event.processing_time ? "#{event.processing_time}s" : '-'
    end
    column :created_at
    column :processed_at

    actions
  end

  show do
    attributes_table do
      row :id
      row :topic_name
      row :event_id
      row :status do |event|
        status_tag event.status
      end
      row :operation_type
      row :object_identifier
      row :processing_time do |event|
        event.processing_time ? "#{event.processing_time}s" : '-'
      end
      row :error_message
      row :created_at
      row :processed_at
      row :updated_at
    end

    panel 'Payload' do
      pre JSON.pretty_generate(resource.payload)
    end
  end

  sidebar 'Statistics', only: :index do
    div class: 'panel_contents' do
      para "Total Events: #{KafkaEvent.count}"
      para "Processed: #{KafkaEvent.processed.count}"
      para "Pending: #{KafkaEvent.unprocessed.count}"
      para "Failed: #{KafkaEvent.failed.count}"
    end
  end

  controller do
    def scoped_collection
      super
    end
  end

  member_action :reprocess, method: :post do
    kafka_event = KafkaEvent.find(params[:id])
    kafka_event.reprocess!
    redirect_to admin_kafka_event_path(kafka_event), notice: 'Event reprocessed successfully'
  end

  action_item :reprocess, only: :show do
    link_to 'Reprocess Event!', reprocess_admin_kafka_event_path(resource), method: :post
  end
end
