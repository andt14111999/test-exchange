# frozen_string_literal: true

module V1
  module Ticks
    class Api < Grape::API
      resource :ticks do
        desc 'Get all active ticks grouped by pool pair'
        get do
          # Get all active ticks
          active_ticks = ::Tick.active.includes(:amm_pool).order(:tick_index)

          # Group ticks by pool pair
          grouped_ticks = active_ticks.group_by(&:pool_pair).transform_values do |ticks|
            ticks.map { |tick| V1::Ticks::Entity.represent(tick) }
          end

          # Format the response
          result = {}
          grouped_ticks.each do |pair, ticks|
            result[pair] = { ticks: ticks }
          end

          present result
        end
      end
    end
  end
end
