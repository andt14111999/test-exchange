# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::FiatDeposits', type: :system do
  describe 'index page' do
    it 'displays fiat deposits list' do
      admin_user = create(:admin_user, :admin)
      deposit = create(:fiat_deposit)

      sign_in admin_user, scope: :admin_user
      visit admin_fiat_deposits_path

      expect(page).to have_content('Fiat Deposits')
      expect(page).to have_content(deposit.id)
      expect(page).to have_content('User')
    end

    it 'shows all required columns' do
      admin_user = create(:admin_user, :admin)
      create(:fiat_deposit)

      sign_in admin_user, scope: :admin_user
      visit admin_fiat_deposits_path

      expect(page).to have_content('Id')
      expect(page).to have_content('User')
      expect(page).to have_content('Currency')
      expect(page).to have_content('Country Code')
      expect(page).to have_content('Fiat Amount')
      expect(page).to have_content('Memo')
      expect(page).to have_content('Status')
      expect(page).to have_content('Created At')
      expect(page).to have_content('Processed At')
      expect(page).to have_content('Cancelled At')
      expect(page).to have_content('Payable')
    end

    it 'has available filters' do
      admin_user = create(:admin_user, :admin)
      create(:fiat_deposit)

      sign_in admin_user, scope: :admin_user
      visit admin_fiat_deposits_path

      within '.filter_form' do
        expect(page).to have_field('q_id')
        expect(page).to have_field('q_user_id')
        expect(page).to have_field('q_fiat_account_id')
        expect(page).to have_field('q_currency')
        expect(page).to have_field('q_country_code')
        expect(page).to have_field('q_fiat_amount')
        expect(page).to have_field('q_memo')
        expect(page).to have_field('q_status')
        expect(page).to have_field('q_created_at_gteq')
        expect(page).to have_field('q_processed_at_gteq')
        expect(page).to have_field('q_cancelled_at_gteq')
        expect(page).to have_field('q_payable_type')
        expect(page).to have_field('q_explorer_ref')
      end
    end

    it 'has available scopes' do
      admin_user = create(:admin_user, :admin)
      create(:fiat_deposit)

      sign_in admin_user, scope: :admin_user
      visit admin_fiat_deposits_path

      within '.scopes' do
        expect(page).to have_link('All')
        expect(page).to have_link('Unprocessed')
        expect(page).to have_link('Pending User Action')
        expect(page).to have_link('Pending Admin Action')
        expect(page).to have_link('Processing')
        expect(page).to have_link('Processed')
        expect(page).to have_link('Cancelled')
        expect(page).to have_link('Refunding')
        expect(page).to have_link('Illegal')
        expect(page).to have_link('Locked')
        expect(page).to have_link('For Trade')
        expect(page).to have_link('Direct')
        expect(page).to have_link('Needs Ownership Verification')
      end
    end

    it 'filters fiat deposits using scopes' do
      admin_user = create(:admin_user, :admin)
      create(:fiat_deposit, :awaiting)
      create(:fiat_deposit, :processed)
      create(:fiat_deposit, :cancelled)

      sign_in admin_user, scope: :admin_user
      visit admin_fiat_deposits_path

      within '.scopes' do
        click_link 'Unprocessed'
      end
      expect(page).to have_current_path(/scope=unprocessed/)

      within '.scopes' do
        click_link 'Processed'
      end
      expect(page).to have_current_path(/scope=processed/)

      within '.scopes' do
        click_link 'Cancelled'
      end
      expect(page).to have_current_path(/scope=cancelled/)

      within '.scopes' do
        click_link 'All'
      end
      expect(page).to have_current_path(/admin\/fiat_deposits$|admin\/fiat_deposits\?/)
    end
  end

  describe 'show page' do
    it 'displays fiat deposit details' do
      admin_user = create(:admin_user, :admin)
      deposit = create(:fiat_deposit,
        explorer_ref: 'REF12345',
        memo: 'Test memo',
        ownership_proof_url: 'http://example.com/proof.jpg',
        sender_name: 'John Doe',
        sender_account_number: '123456789'
      )

      sign_in admin_user, scope: :admin_user
      visit admin_fiat_deposit_path(deposit)

      expect(page).to have_content(deposit.id)
      expect(page).to have_content('User')
      expect(page).to have_content(deposit.fiat_account.id) if deposit.fiat_account.present?
      expect(page).to have_content(deposit.currency)
      expect(page).to have_content(deposit.country_code)
      expect(page).to have_content(deposit.fiat_amount)
      expect(page).to have_content(deposit.explorer_ref)
      expect(page).to have_content(deposit.memo)
      expect(page).to have_content(deposit.ownership_proof_url)
      expect(page).to have_content(deposit.sender_name)
      expect(page).to have_content(deposit.sender_account_number)
      expect(page).to have_content(deposit.status)
    end

    context 'with associated trade' do
      it 'displays trade information' do
        admin_user = create(:admin_user, :admin)
        trade = create(:trade)
        deposit = create(:fiat_deposit, payable: trade)

        sign_in admin_user, scope: :admin_user
        visit admin_fiat_deposit_path(deposit)

        expect(page).to have_content('Trade')
        expect(page).to have_link("Trade ##{trade.ref}")
      end
    end

    context 'admin actions' do
      it 'shows Mark as Ready button for awaiting or pending deposits' do
        admin_user = create(:admin_user, :admin)
        awaiting_deposit = create(:fiat_deposit, :awaiting)
        pending_deposit = create(:fiat_deposit, :pending)

        sign_in admin_user, scope: :admin_user

        visit admin_fiat_deposit_path(awaiting_deposit)
        within 'div.panel', text: 'Admin Actions' do
          expect(page).to have_link('Mark as Ready')
        end

        visit admin_fiat_deposit_path(pending_deposit)
        within 'div.panel', text: 'Admin Actions' do
          expect(page).to have_link('Mark as Ready')
        end
      end

      it 'shows Mark as Informed and Mark as Verifying buttons for ready deposits' do
        admin_user = create(:admin_user, :admin)
        ready_deposit = create(:fiat_deposit, :ready)

        sign_in admin_user, scope: :admin_user
        visit admin_fiat_deposit_path(ready_deposit)

        within 'div.panel', text: 'Admin Actions' do
          expect(page).to have_link('Mark as Informed')
          expect(page).to have_link('Mark as Verifying')
        end
      end

      it 'shows Process Deposit button for verifying or ownership_verifying deposits' do
        admin_user = create(:admin_user, :admin)
        verifying_deposit = create(:fiat_deposit, :verifying)
        ownership_verifying_deposit = create(:fiat_deposit, :ownership_verifying)

        sign_in admin_user, scope: :admin_user

        visit admin_fiat_deposit_path(verifying_deposit)
        within 'div.panel', text: 'Admin Actions' do
          expect(page).to have_link('Process Deposit')
        end

        visit admin_fiat_deposit_path(ownership_verifying_deposit)
        within 'div.panel', text: 'Admin Actions' do
          expect(page).to have_link('Process Deposit')
        end
      end

      it 'shows Mark as Locked button for deposits needing ownership verification' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :ownership_verifying)

        sign_in admin_user, scope: :admin_user
        visit admin_fiat_deposit_path(deposit)

        within 'div.panel', text: 'Admin Actions' do
          expect(page).to have_link('Mark as Locked')
        end
      end

      it 'shows Cancel Deposit button for deposits that may be cancelled' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :awaiting) # assuming this status allows cancellation

        sign_in admin_user, scope: :admin_user
        visit admin_fiat_deposit_path(deposit)

        within 'div.panel', text: 'Admin Actions' do
          expect(page).to have_link('Cancel Deposit')
        end
      end

      it 'shows Mark as Illegal button for deposits that may be marked as illegal' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :awaiting) # assuming this status allows marking as illegal

        sign_in admin_user, scope: :admin_user
        visit admin_fiat_deposit_path(deposit)

        within 'div.panel', text: 'Admin Actions' do
          expect(page).to have_link('Mark as Illegal')
        end
      end
    end
  end

  describe 'form' do
    it 'allows editing fiat deposit' do
      admin_user = create(:admin_user, :admin)
      deposit = create(:fiat_deposit)

      sign_in admin_user, scope: :admin_user
      visit edit_admin_fiat_deposit_path(deposit)

      within 'form' do
        select 'processed', from: 'Status'
        fill_in 'Explorer ref', with: 'REF67890'
        fill_in 'Sender name', with: 'Jane Smith'
        fill_in 'Sender account number', with: '987654321'
        fill_in 'Ownership proof url', with: 'http://example.com/new_proof.jpg'
        click_button 'Update Fiat deposit'
      end

      expect(page).to have_current_path(%r{/admin/fiat_deposits/\d+})
      expect(page).to have_content('processed')
      expect(page).to have_content('REF67890')
      expect(page).to have_content('Jane Smith')
      expect(page).to have_content('987654321')
      expect(page).to have_content('http://example.com/new_proof.jpg')
    end
  end

  describe 'admin actions', type: :request do
    before do
      # For request specs, we need to manually set up Devise mappings
      @request.env["devise.mapping"] = Devise.mappings[:admin_user] if @request
    end

    describe 'POST mark_as_ready' do
      it 'marks deposit as ready' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :awaiting)

        sign_in admin_user, scope: :admin_user

        expect {
          post mark_as_ready_admin_fiat_deposit_path(deposit)
        }.to change { deposit.reload.status }.from('awaiting').to('ready')

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Deposit marked as ready')
      end
    end

    describe 'POST mark_as_informed' do
      it 'marks deposit as informed' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :ready)

        sign_in admin_user, scope: :admin_user

        expect {
          post mark_as_informed_admin_fiat_deposit_path(deposit)
        }.to change { deposit.reload.status }.from('ready').to('informed')

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Deposit marked as informed')
      end
    end

    describe 'POST mark_as_verifying' do
      it 'marks deposit as verifying' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :ready)

        sign_in admin_user, scope: :admin_user

        expect {
          post mark_as_verifying_admin_fiat_deposit_path(deposit)
        }.to change { deposit.reload.status }.from('ready').to('verifying')

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Deposit marked as verifying')
      end
    end

    describe 'POST cancel' do
      it 'cancels the deposit' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :awaiting)

        sign_in admin_user, scope: :admin_user

        expect {
          post cancel_admin_fiat_deposit_path(deposit)
        }.to change { deposit.reload.status }.from('awaiting').to('cancelled')

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Deposit cancelled')
        expect(deposit.reload.cancel_reason).to include(admin_user.email)
      end

      it 'does not cancel deposit in invalid state' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :processed)

        sign_in admin_user, scope: :admin_user

        expect {
          post cancel_admin_fiat_deposit_path(deposit)
        }.not_to change { deposit.reload.status }

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Cannot cancel deposit in current state')
      end
    end

    describe 'POST mark_as_locked' do
      it 'locks the deposit' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :ownership_verifying)

        sign_in admin_user, scope: :admin_user

        expect {
          post mark_as_locked_admin_fiat_deposit_path(deposit)
        }.to change { deposit.reload.status }.from('ownership_verifying').to('locked')

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Deposit locked')
        expect(deposit.reload.cancel_reason).to include(admin_user.email)
      end

      it 'does not lock deposit in invalid state' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :processed)

        sign_in admin_user, scope: :admin_user

        expect {
          post mark_as_locked_admin_fiat_deposit_path(deposit)
        }.not_to change { deposit.reload.status }

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Cannot lock deposit in current state')
      end
    end

    describe 'POST mark_as_illegal' do
      it 'marks deposit as illegal' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :ready)

        sign_in admin_user, scope: :admin_user

        expect {
          post mark_as_illegal_admin_fiat_deposit_path(deposit)
        }.to change { deposit.reload.status }.from('ready').to('illegal')

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Deposit marked as illegal')
      end

      it 'does not mark deposit as illegal in invalid state' do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :processed)

        sign_in admin_user, scope: :admin_user

        expect {
          post mark_as_illegal_admin_fiat_deposit_path(deposit)
        }.not_to change { deposit.reload.status }

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Cannot mark deposit as illegal in current state')
      end
    end

    describe 'POST process_deposit' do
      it 'processes the deposit', type: :request do
        admin_user = create(:admin_user, :admin)
        deposit = create(:fiat_deposit, :verifying)

        sign_in admin_user, scope: :admin_user

        post process_deposit_admin_fiat_deposit_path(deposit)

        expect(response).to redirect_to(admin_fiat_deposit_path(deposit))
        follow_redirect!
        expect(response.body).to include('Deposit processed')
      end
    end
  end
end
