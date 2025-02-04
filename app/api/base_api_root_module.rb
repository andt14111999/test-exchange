# frozen_string_literal: true

module BaseApiRootModule
  def self.included(base)
    base.class_eval do
      prefix 'api'
      format :json
      formatter :json, Grape::Formatter::ActiveModelSerializers
      version :v1, using: :path

      rescue_from ActiveRecord::RecordNotFound do |e|
        class_name = e.message.match(/^Couldn't find (.*) with?/).try(:[], 1)
        class_name ||= 'Record'
        error!(
          {
            error: I18n.t('error.record_not_found'),
            class_name: class_name
          },
          :not_found
        )
      end
    end
  end
end
