# frozen_string_literal: true

class EntityMeta < Grape::Entity
  expose :current_page
  expose :next_page
  expose :total_pages
  expose :per_page
end
