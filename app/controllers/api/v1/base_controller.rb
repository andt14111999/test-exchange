# frozen_string_literal: true

module Api
  module V1
    class BaseController < ActionController::API
      include ActionController::MimeResponds
      include ActionController::Cookies
      include ActionController::RequestForgeryProtection

      protect_from_forgery with: :null_session
    end
  end
end
