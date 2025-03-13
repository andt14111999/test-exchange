# frozen_string_literal: true

module Admin
  module StatusHelper
    def status_class(status)
      {
        'pending' => 'warning',
        'verified' => 'ok',
        'locked' => 'error',
        'rejected' => 'error',
        'forged' => 'error'
      }[status]
    end
  end
end
