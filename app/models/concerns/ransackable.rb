# frozen_string_literal: true

module Ransackable
  extend ActiveSupport::Concern

  class_methods do
    def ransackable_attributes(_auth_object = nil)
      @ransackable_attributes ||= column_names - disabled_ransackable_attributes
    end

    def ransackable_associations(_auth_object = nil)
      @ransackable_associations ||= reflect_on_all_associations.map { |a|
 a.name.to_s
      } - disabled_ransackable_associations
    end

    def disabled_ransackable_attributes
      []
    end

    def disabled_ransackable_associations
      []
    end
  end
end
