# frozen_string_literal: true

require 'sidekiq/web'
require 'sidekiq-status/web'

Rails.application.routes.draw do
  devise_for :admin_users, ActiveAdmin::Devise.config
  ActiveAdmin.routes(self)
  # Define your application routes per the DSL in https://guides.rubyonrails.org/routing.html

  # Reveal health status on /up that returns 200 if the app boots with no exceptions, otherwise 500.
  # Can be used by load balancers and uptime monitors to verify that the app is live.
  get 'up' => 'rails/health#show', as: :rails_health_check

  # Render dynamic PWA files from app/views/pwa/* (remember to link manifest in application.html.erb)
  # get "manifest" => "rails/pwa#manifest", as: :pwa_manifest
  # get "service-worker" => "rails/pwa#service_worker", as: :pwa_service_worker

  # Defines the root path route ("/")
  # root "posts#index"

  authenticate :admin_user do
    mount Sidekiq::Web => '/admin/sidekiq'
  end

  mount ApiRoot => '/'

  namespace :api do
    namespace :v1 do
      get '/auth/:provider/callback', to: 'auth#callback'
      post '/auth/facebook', to: 'auth#facebook'
      post '/auth/google', to: 'auth#google'
      post '/auth/apple', to: 'auth#apple'
      resources :balances, only: [ :index ]
      get 'settings/exchange_rates', to: 'settings#exchange_rates'
    end
  end

  post "coin_portal/#{CoinPortalController::HASH}/:type", controller: :coin_portal, action: :index

  mount ActionCable.server => '/cable'
end
