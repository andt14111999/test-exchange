# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::Messages' do
  describe 'UI', type: :system do
    let(:admin) { create(:admin_user, roles: 'superadmin') }
    let(:user) { create(:user) }
    let(:trade) { create(:trade) }
    let(:message) { create(:message, trade: trade, user: user, body: 'Test Message', is_receipt_proof: false, is_system: false) }
    let(:receipt_proof_message) { create(:message, :receipt_proof, trade: trade, user: user) }
    let(:system_message) { create(:message, :system_message, trade: trade, user: user) }

    before do
      sign_in admin, scope: :admin_user
      message
      receipt_proof_message
      system_message
    end

    describe '#index' do
      it 'displays messages list' do
        visit admin_messages_path
        expect(page).to have_content('Messages')
        expect(page).to have_content('Test Message')
      end

      it 'has filters available' do
        visit admin_messages_path
        within '.filter_form' do
          expect(page).to have_content('Id')
          expect(page).to have_content('Trade')
          expect(page).to have_content('User')
          expect(page).to have_content('Body')
          expect(page).to have_content('Is receipt proof')
          expect(page).to have_content('Is system')
        end
      end

      it 'has scopes available' do
        visit admin_messages_path
        within '.scopes' do
          expect(page).to have_link('All')
          expect(page).to have_link('Receipt Proofs')
          expect(page).to have_link('System Messages')
          expect(page).to have_link('User Messages')
          expect(page).to have_link('Sorted')
        end
      end

      it 'displays receipt proofs using scope' do
        visit admin_messages_path
        within '.scopes' do
          click_link 'Receipt Proofs'
        end
        expect(page).to have_content('I have completed the payment')
      end

      it 'displays system messages using scope' do
        visit admin_messages_path
        within '.scopes' do
          click_link 'System Messages'
        end
        expect(page).to have_content('System notification')
      end

      it 'displays user messages using scope' do
        visit admin_messages_path
        within '.scopes' do
          click_link 'User Messages'
        end
        expect(page).to have_content('Test Message')
      end

      it 'displays sorted messages using scope' do
        visit admin_messages_path
        within '.scopes' do
          click_link 'Sorted'
        end
        expect(page).to have_content('Test Message')
      end
    end

    describe '#show' do
      it 'displays message details' do
        visit admin_message_path(message)

        expect(page).to have_content(message.id)
        expect(page).to have_content('Trade #')
        expect(page).to have_content('Test Message')
        expect(page).to have_content('No') # For is_receipt_proof
      end
    end

    describe '#edit' do
      it 'allows editing message' do
        visit edit_admin_message_path(message)

        within 'form' do
          fill_in 'Body', with: 'Updated Test Message'
          if has_field?('Is receipt proof')
            check 'Is receipt proof' unless page.has_checked_field?('Is receipt proof')
          end
          click_button 'Update Message'
        end

        expect(page).to have_current_path(%r{admin/messages})
      end
    end

    describe 'custom actions' do
      it 'shows mark as receipt proof button for regular messages' do
        visit admin_message_path(message)
        expect(page).to have_link('Mark as Receipt Proof')
      end

      it 'shows mark as regular message button for receipt proof messages' do
        visit admin_message_path(receipt_proof_message)
        expect(page).to have_link('Mark as Regular Message')
      end

      it 'does not show mark as receipt proof button for receipt proof messages' do
        visit admin_message_path(receipt_proof_message)
        expect(page).not_to have_link('Mark as Receipt Proof')
      end

      it 'does not show mark as regular message button for regular messages' do
        visit admin_message_path(message)
        expect(page).not_to have_link('Mark as Regular Message')
      end
    end
  end

  describe 'API', type: :request do
    let(:admin) { create(:admin_user, roles: 'superadmin') }
    let(:user) { create(:user) }
    let(:trade) { create(:trade) }
    let(:message) { create(:message, trade: trade, user: user, is_receipt_proof: false) }
    let(:receipt_proof_message) { create(:message, trade: trade, user: user, is_receipt_proof: true) }

    before do
      sign_in admin, scope: :admin_user
      message
      receipt_proof_message
    end

    it 'marks message as receipt proof' do
      put "/admin/messages/#{message.id}/mark_as_receipt_proof"

      expect(response).to redirect_to("/admin/messages/#{message.id}")
      follow_redirect!
      expect(response.body).to include('Message has been marked as a receipt proof')

      # Verify the message is now a receipt proof
      expect(message.reload.is_receipt_proof).to be true
    end

    it 'marks message as regular message' do
      put "/admin/messages/#{receipt_proof_message.id}/mark_as_regular_message"

      expect(response).to redirect_to("/admin/messages/#{receipt_proof_message.id}")
      follow_redirect!
      expect(response.body).to include('Message has been marked as a regular message')

      # Verify the message is now a regular message
      expect(receipt_proof_message.reload.is_receipt_proof).to be false
    end
  end
end
