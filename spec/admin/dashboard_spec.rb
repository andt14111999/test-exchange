# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::Dashboard', type: :system do
  let(:admin) { create(:admin_user, roles: 'superadmin') }

  before do
    sign_in admin, scope: :admin_user
  end

  it 'displays the dashboard welcome message' do
    visit admin_dashboard_path

    expect(page).to have_content(I18n.t('active_admin.dashboard_welcome.welcome'))
    expect(page).to have_content(I18n.t('active_admin.dashboard_welcome.call_to_action'))
  end

  it 'has dashboard as the active tab' do
    visit admin_dashboard_path

    within '#tabs' do
      expect(page).to have_css('li.current', text: I18n.t('active_admin.dashboard'))
    end
  end

  it 'has the correct title' do
    visit admin_dashboard_path

    expect(page).to have_css('h2', text: I18n.t('active_admin.dashboard'))
  end

  it 'contains the blank slate container' do
    visit admin_dashboard_path

    expect(page).to have_css('div.blank_slate_container#dashboard_default_message')
    expect(page).to have_css('span.blank_slate')
  end
end
