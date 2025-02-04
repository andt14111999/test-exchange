# frozen_string_literal: true

module V1
  module Test
    class Api < Grape::API
      get :test do
        { message: 'Hello World' }
      end

      resource :notfound do
        get do
          user = AdminUser.find_by!(email: 'test@snowfox.com')
          { id: user.id }
        end
      end

      resource :ok do
        get do
          users = AdminUser.all.page(1).per(1)

          present :users, users, with: Entity
          present :meta, generate_meta(users), with: Api::EntityMeta
        end
      end
    end
  end
end
