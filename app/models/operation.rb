# frozen_string_literal: true

class Operation < ApplicationRecord
  self.abstract_class = true

  def display_name
    "#{self.class} ##{id}"
  end

  def html_view
    display_name
  end
end
