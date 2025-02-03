# frozen_string_literal: true

# RailsSettings Model
class Setting < RailsSettings::Base
  scope :toggl do
    field :max_working_hours_continous, type: :integer, default: 6
    field :max_working_hours_learning, type: :integer, default: 7
    field :max_gap_working_continous_minutes, type: :integer, default: 15
    field :max_working_hours_others, type: :integer, default: 100
    field :max_working_hours_cs, type: :integer, default: 120
    field :toggl_prefixes, type: :string, default: 'learning,dev,cs,hr,mkt,bd,ems,eat,mastery,sm,admin'
  end

  def self.toggl_prefixes_arr
    Setting.toggl_prefixes.split(',').map { |i| "[#{i}]" }
  end
end
