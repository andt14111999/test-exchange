require 'rails_helper'

RSpec.describe ActiveAdmin::BaseAdminHelper, type: :helper do
  describe '#api_link_to' do
    it 'adds api-link class and data-type json to link' do
      result = helper.api_link_to('Test', '/test')
      expect(result).to include('class="api-link"')
      expect(result).to include('data-type="json"')
    end

    it 'merges existing classes with api-link' do
      result = helper.api_link_to('Test', '/test', class: 'existing-class')
      expect(result).to include('class="existing-class api-link"')
    end

    it 'preserves existing data attributes' do
      result = helper.api_link_to('Test', '/test', data: { remote: true })
      expect(result).to include('data-remote="true"')
      expect(result).to include('data-type="json"')
    end
  end

  describe '#open_2fa_button' do
    it 'creates api link with correct context' do
      result = helper.open_2fa_button('Enable')
      expect(result).to include('Enable 2FA')
      expect(result).to include('class="button api-link"')
      expect(result).to include('data-type="json"')
    end
  end
end
