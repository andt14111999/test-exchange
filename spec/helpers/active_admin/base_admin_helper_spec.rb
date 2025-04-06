require 'rails_helper'

RSpec.describe ActiveAdmin::BaseAdminHelper, type: :helper do
  describe '#api_link_to' do
    it 'adds api-link class to options' do
      result = helper.api_link_to('Test', '/test')
      expect(result).to include('class="api-link"')
    end

    it 'preserves existing classes' do
      result = helper.api_link_to('Test', '/test', class: 'existing-class')
      expect(result).to include('class="existing-class api-link"')
    end

    it 'adds json data type' do
      result = helper.api_link_to('Test', '/test')
      expect(result).to include('data-type="json"')
    end

    it 'preserves existing data attributes' do
      result = helper.api_link_to('Test', '/test', data: { confirm: 'Are you sure?' })
      expect(result).to include('data-type="json"')
      expect(result).to include('data-confirm="Are you sure?"')
    end
  end

  describe '#verify_2fa_and_process' do
    let(:admin_user) { create(:admin_user) }
    let(:success_message) { 'Operation successful' }

    before do
      allow(helper).to receive(:current_admin_user).and_return(admin_user)
    end

    it 'executes block and sets success message when 2FA is enabled and code is correct' do
      allow(admin_user).to receive(:authenticator_enabled?).and_return(true)
      allow(admin_user).to receive(:verify_otp).with('123456').and_return(true)

      block_executed = false
      result = helper.verify_2fa_and_process('123456', success_message) { block_executed = true }

      expect(block_executed).to be true
      expect(flash[:notice]).to eq(success_message)
      expect(result).to eq(:ok)
    end

    it 'sets error message when 2FA is enabled but code is incorrect' do
      allow(admin_user).to receive(:authenticator_enabled?).and_return(true)
      allow(admin_user).to receive(:verify_otp).with('123456').and_return(false)

      block_executed = false
      result = helper.verify_2fa_and_process('123456', success_message) { block_executed = true }

      expect(block_executed).to be false
      expect(flash[:error]).to eq('2Fa code is incorrect.')
      expect(result).to eq(:unprocessable_entity)
    end

    it 'sets error message when 2FA is not enabled' do
      allow(admin_user).to receive(:authenticator_enabled?).and_return(false)

      block_executed = false
      result = helper.verify_2fa_and_process('123456', success_message) { block_executed = true }

      expect(block_executed).to be false
      expect(flash[:error]).to eq('2Fa is not enabled.')
      expect(result).to eq(:unprocessable_entity)
    end
  end

  describe '#open_2fa_button' do
    it 'returns a link with correct attributes' do
      allow(helper).to receive(:admin_setup_2fa_path).and_return('/admin/setup_2fa')

      result = helper.open_2fa_button('Setup')
      expect(result).to include('href="/admin/setup_2fa"')
      expect(result).to include('class="button api-link"')
      expect(result).to include('Setup 2FA')
      expect(result).to include('data-type="json"')
    end
  end
end
