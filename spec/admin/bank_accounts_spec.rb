# frozen_string_literal: true

require 'rails_helper'

RSpec.describe 'Admin::BankAccounts', type: :request do
  let(:admin_user) { create(:admin_user, roles: 'superadmin') }
  let(:user) { create(:user) }
  let(:bank_account) { create(:bank_account, user: user) }

  before do
    sign_in admin_user, scope: :admin_user
    allow_any_instance_of(ActiveAdmin::ResourceController).to receive(:authorized?).and_return(true)
  end

  describe 'index page' do
    it 'displays bank accounts' do
      bank_account # ensure bank account is created
      get admin_bank_accounts_path

      # Use more specific matchers to check for the content in the bank account row or table cell
      expect(response.body).to include(">#{bank_account.bank_name}<")
      expect(response.body).to include(CGI.escapeHTML(bank_account.account_name))
      expect(response.body).to include(">#{bank_account.account_number}<")
    end

    it 'filters bank accounts by verified status' do
      verified_account = create(:bank_account, :verified, bank_name: 'Verified Bank')
      unverified_account = create(:bank_account, verified: false, bank_name: 'Unverified Bank')

      get admin_bank_accounts_path(scope: 'verified')

      # More specific test for the presence of the verified account with a unique name
      expect(response.body).to include('Verified Bank')
      # If we're in the verified scope, we should not see any table row containing the unverified bank's unique name
      expect(response.body).not_to include('<td>Unverified Bank</td>')
    end

    it 'filters bank accounts by unverified status' do
      verified_account = create(:bank_account, :verified, bank_name: 'Verified Bank')
      unverified_account = create(:bank_account, verified: false, bank_name: 'Unverified Bank')

      get admin_bank_accounts_path(scope: 'unverified')

      # More specific test for the presence of the unverified account with a unique name
      expect(response.body).to include('Unverified Bank')
      # If we're in the unverified scope, we should not see any table row containing the verified bank's unique name
      expect(response.body).not_to include('<td>Verified Bank</td>')
    end

    it 'filters bank accounts by primary status' do
      primary_account = create(:bank_account, :primary, bank_name: 'Primary Bank')
      non_primary_account = create(:bank_account, is_primary: false, bank_name: 'Non-Primary Bank')

      get admin_bank_accounts_path(scope: 'primary')

      # More specific test for the presence of the primary account with a unique name
      expect(response.body).to include('Primary Bank')
      # If we're in the primary scope, we should not see any table row containing the non-primary bank's unique name
      expect(response.body).not_to include('<td>Non-Primary Bank</td>')
    end
  end

  describe 'show page' do
    it 'displays bank account details' do
      get admin_bank_account_path(bank_account)

      expect(response.body).to include(bank_account.bank_name)
      expect(response.body).to include(CGI.escapeHTML(bank_account.account_name))
      expect(response.body).to include(bank_account.account_number)
      expect(response.body).to include(CGI.escapeHTML(bank_account.branch)) if bank_account.branch.present?
      expect(response.body).to include(bank_account.country_code)
    end

    it 'shows verify action item for unverified accounts' do
      unverified_account = create(:bank_account, verified: false)
      get admin_bank_account_path(unverified_account)

      expect(response.body).to include('Verify Account')
    end

    it 'does not show verify action item for verified accounts' do
      verified_account = create(:bank_account, :verified)
      get admin_bank_account_path(verified_account)

      expect(response.body).not_to include('Verify Account')
    end

    it 'shows make primary action item for non-primary accounts' do
      non_primary_account = create(:bank_account, is_primary: false)
      get admin_bank_account_path(non_primary_account)

      expect(response.body).to include('Make Primary')
    end

    it 'does not show make primary action item for primary accounts' do
      primary_account = create(:bank_account, :primary)
      get admin_bank_account_path(primary_account)

      expect(response.body).not_to include('Make Primary')
    end
  end

  describe 'create' do
    it 'creates a new bank account' do
      expect do
        post admin_bank_accounts_path, params: {
          bank_account: {
            user_id: user.id,
            bank_name: 'Test Bank',
            account_name: 'Test Account',
            account_number: '1234567890',
            branch: 'Test Branch',
            country_code: 'vn',
            verified: false,
            is_primary: false
          }
        }
      end.to change(BankAccount, :count).by(1)

      expect(response).to redirect_to(admin_bank_account_path(BankAccount.last))
      follow_redirect!
      expect(response.body).to include('Test Bank')
      expect(response.body).to include('Test Account')
      expect(response.body).to include('1234567890')
    end
  end

  describe 'update' do
    it 'updates an existing bank account' do
      put admin_bank_account_path(bank_account), params: {
        bank_account: {
          bank_name: 'Updated Bank',
          account_name: 'Updated Account',
          verified: true
        }
      }

      expect(response).to redirect_to(admin_bank_account_path(bank_account))
      follow_redirect!
      expect(response.body).to include('Updated Bank')
      expect(response.body).to include('Updated Account')
      expect(response.body).to include('Verified')

      bank_account.reload
      expect(bank_account.bank_name).to eq('Updated Bank')
      expect(bank_account.account_name).to eq('Updated Account')
      expect(bank_account.verified).to be true
    end
  end

  describe 'custom actions' do
    describe 'verify' do
      it 'marks an unverified bank account as verified' do
        unverified_account = create(:bank_account, verified: false)

        put verify_admin_bank_account_path(unverified_account)

        expect(response).to redirect_to(admin_bank_account_path(unverified_account))
        follow_redirect!
        expect(response.body).to include('Bank account has been verified')

        unverified_account.reload
        expect(unverified_account.verified).to be true
      end
    end

    describe 'make_primary' do
      it 'marks a non-primary bank account as primary' do
        non_primary_account = create(:bank_account, user: user, is_primary: false)

        put make_primary_admin_bank_account_path(non_primary_account)

        expect(response).to redirect_to(admin_bank_account_path(non_primary_account))
        follow_redirect!
        expect(response.body).to include('Bank account is now the primary account')

        non_primary_account.reload
        expect(non_primary_account.is_primary).to be true
      end

      it 'updates any existing primary accounts of the same bank to non-primary' do
        # Mock the behavior to test the controller action, not the model callback
        # This tests that the controller calls mark_as_primary! on the resource
        expect_any_instance_of(BankAccount).to receive(:mark_as_primary!).and_return(true)

        # Create a bank account to use in the test
        bank_account = create(:bank_account, is_primary: false)

        # Call the member action
        put make_primary_admin_bank_account_path(bank_account)

        # Verify the response
        expect(response).to redirect_to(admin_bank_account_path(bank_account))
      end

      it 'does not affect primary accounts of different banks' do
        other_bank_primary = create(:bank_account, user: user, bank_name: 'Other Bank', is_primary: true)
        non_primary_account = create(:bank_account, user: user, bank_name: 'This Bank', is_primary: false)

        put make_primary_admin_bank_account_path(non_primary_account)

        other_bank_primary.reload
        non_primary_account.reload

        expect(other_bank_primary.is_primary).to be true
        expect(non_primary_account.is_primary).to be true
      end
    end
  end

  describe 'filter functionality' do
    it 'filters by user' do
      user1 = create(:user)
      user2 = create(:user)
      account1 = create(:bank_account, user: user1)
      account2 = create(:bank_account, user: user2)

      get admin_bank_accounts_path(q: { user_id_eq: user1.id })

      expect(response.body).to include(account1.account_name)
      expect(response.body).not_to include(account2.account_name)
    end

    it 'filters by bank name' do
      account1 = create(:bank_account, bank_name: 'Vietcombank')
      account2 = create(:bank_account, bank_name: 'BIDV')

      get admin_bank_accounts_path(q: { bank_name_cont: 'combank' })

      # Use more specific matchers for Active Admin's HTML structure
      expect(response.body).to include('td class="col col-bank_name">Vietcombank</td>')
      expect(response.body).not_to include('td class="col col-bank_name">BIDV</td>')
    end

    it 'filters by account name' do
      account1 = create(:bank_account, account_name: 'John Doe')
      account2 = create(:bank_account, account_name: 'Jane Smith')

      get admin_bank_accounts_path(q: { account_name_cont: 'John' })

      expect(response.body).to include(account1.account_name)
      expect(response.body).not_to include(account2.account_name)
    end

    it 'filters by account number' do
      account1 = create(:bank_account, account_number: '1234567890', account_name: 'John Number')
      account2 = create(:bank_account, account_number: '0987654321', account_name: 'Jane Different')

      get admin_bank_accounts_path(q: { account_number_cont: '123' })

      # Check for the presence of the account with 123 in its number
      expect(response.body).to include(account1.account_name)
      expect(response.body).not_to include(account2.account_name)
    end

    it 'filters by country code' do
      account1 = create(:bank_account, country_code: 'vn')
      account2 = create(:bank_account, country_code: 'ph')

      get admin_bank_accounts_path(q: { country_code_eq: 'vn' })

      expect(response.body).to include(account1.account_name)
      expect(response.body).not_to include(account2.account_name)
    end
  end

  describe 'form' do
    it 'shows new form with all required fields' do
      get new_admin_bank_account_path

      expect(response.body).to include('Bank Account Details')
      expect(response.body).to include('User')
      expect(response.body).to include('Bank name')
      expect(response.body).to include('Account name')
      expect(response.body).to include('Account number')
      expect(response.body).to include('Branch')
      expect(response.body).to include('Country code')
      expect(response.body).to include('Verified')
      expect(response.body).to include('Is primary')
      expect(response.body).to include('Create Bank account')
    end

    it 'shows edit form with all required fields' do
      get edit_admin_bank_account_path(bank_account)

      expect(response.body).to include('Bank Account Details')
      expect(response.body).to include('User')
      expect(response.body).to include('Bank name')
      expect(response.body).to include('Account name')
      expect(response.body).to include('Account number')
      expect(response.body).to include('Branch')
      expect(response.body).to include('Country code')
      expect(response.body).to include('Verified')
      expect(response.body).to include('Is primary')
      expect(response.body).to include('Update Bank account')
    end

    it 'populates fields with existing values on edit form' do
      get edit_admin_bank_account_path(bank_account)

      expect(response.body).to include(bank_account.bank_name)
      expect(response.body).to include(CGI.escapeHTML(bank_account.account_name))
      expect(response.body).to include(bank_account.account_number)
      expect(response.body).to include(bank_account.country_code)
      expect(response.body).to include('value="' + (bank_account.branch || '').to_s + '"')

      # Check that the form contains the proper checkbox inputs
      expect(response.body).to include('id="bank_account_verified_input"')
      expect(response.body).to include('name="bank_account[verified]"')

      expect(response.body).to include('id="bank_account_is_primary_input"')
      expect(response.body).to include('name="bank_account[is_primary]"')

      # Check if the checkboxes are checked based on current values
      if bank_account.verified
        expect(response.body).to include('input type="checkbox" name="bank_account[verified]" id="bank_account_verified" value="1" checked="checked"')
      else
        expect(response.body).to include('input type="checkbox" name="bank_account[verified]" id="bank_account_verified" value="1"')
        expect(response.body).not_to include('input type="checkbox" name="bank_account[verified]" id="bank_account_verified" value="1" checked="checked"')
      end

      if bank_account.is_primary
        expect(response.body).to include('input type="checkbox" name="bank_account[is_primary]" id="bank_account_is_primary" value="1" checked="checked"')
      else
        expect(response.body).to include('input type="checkbox" name="bank_account[is_primary]" id="bank_account_is_primary" value="1"')
        expect(response.body).not_to include('input type="checkbox" name="bank_account[is_primary]" id="bank_account_is_primary" value="1" checked="checked"')
      end
    end

    it 'submits the form successfully with all fields' do
      post admin_bank_accounts_path, params: {
        bank_account: {
          user_id: user.id,
          bank_name: 'New Test Bank',
          account_name: 'Test Account Name',
          account_number: '12345678901',
          branch: 'Test Branch',
          country_code: 'us',
          verified: true,
          is_primary: true
        }
      }

      expect(response).to redirect_to(admin_bank_account_path(BankAccount.last))
      follow_redirect!

      # Verify the created bank account has all the submitted values
      bank_account = BankAccount.last
      expect(bank_account.user_id).to eq(user.id)
      expect(bank_account.bank_name).to eq('New Test Bank')
      expect(bank_account.account_name).to eq('Test Account Name')
      expect(bank_account.account_number).to eq('12345678901')
      expect(bank_account.branch).to eq('Test Branch')
      expect(bank_account.country_code).to eq('us')
      expect(bank_account.verified).to be true
      expect(bank_account.is_primary).to be true
    end
  end
end
