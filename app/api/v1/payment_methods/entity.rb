module V1
  module PaymentMethods
    class Entity < Grape::Entity
      expose :id
      expose :name
      expose :display_name
      expose :description
      expose :country_code
      expose :enabled
      expose :icon_url
      expose :fields_required
      expose :created_at
      expose :updated_at
    end
  end
end
