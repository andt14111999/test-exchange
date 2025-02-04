# frozen_string_literal: true

module Base
  module Helpers
    module Meta
      def generate_meta(object)
        {
          current_page: object.current_page,
          next_page: object.next_page,
          total_pages: object.total_pages,
          per_page: object.limit_value
        }
      end

      def generate_meta_with_default(object)
        {
          current_page: object&.current_page || 1,
          next_page: object&.next_page,
          total_pages: object&.total_pages || 0,
          per_page: object&.limit_value || 9
        }
      end
    end
  end
end
