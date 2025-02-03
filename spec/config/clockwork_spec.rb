# frozen_string_literal: true

require 'rails_helper'
require_relative '../../config/clockwork'

RSpec.describe Clockwork do
  def events_at(time)
    Clockwork.manager.send(:events_to_run, time)
  end

  def jobs_at(time)
    events_at(time).map(&:job)
  end

  # it 'runs TestingJob' do
  #   time = Time.zone.local(2025, 1, 16, 0, 0, 0)
  #   job_description = 'finish_sprint'
  #   expect(jobs_at(time)).to include(job_description)
  #   event = events_at(time).find { |e| e.job == job_description }
  #   expect(TestingJob).to receive(:perform_async)
  #   event.run(time)
  # end
end
