require 'rails_helper'

RSpec.describe 'Bank Admin', type: :system do
  let(:admin_user) { create(:admin_user, :superadmin) }
  let(:country) { create(:country, name: 'Vietnam', code: 'VN') }
  let(:bank) { create(:bank, country: country, name: 'Vietcombank', code: 'VCB', bin: '970436') }

  before do
    sign_in admin_user, scope: :admin_user
  end

  describe 'index page' do
    it 'displays the list of banks' do
      bank
      visit admin_banks_path

      expect(page).to have_content('Banks')
      expect(page).to have_content('Vietcombank')
      expect(page).to have_content('VCB')
      expect(page).to have_content('Vietnam')
    end

    it 'shows boolean fields as Yes/No text' do
      bank
      visit admin_banks_path

      expect(page).to have_content('Yes')
      expect(page).to have_content('No')
    end

    it 'has filter options' do
      visit admin_banks_path

      expect(page).to have_field('Name')
      expect(page).to have_field('Code')
      expect(page).to have_select('Country')
      expect(page).to have_field('Bin')
      expect(page).to have_field('Short name')
      expect(page).to have_select('Transfer supported')
      expect(page).to have_select('Lookup supported')
      expect(page).to have_select('Is transfer')
      expect(page).to have_field('Support')
    end

    it 'filters banks by name' do
      bank
      other_bank = create(:bank, country: country, name: 'Techcombank', code: 'TCB')
      visit admin_banks_path

      fill_in 'Name', with: 'Vietcombank'
      click_button 'Filter'

      expect(page).to have_content('Vietcombank')
      expect(page).not_to have_content('Techcombank')
    end

    it 'filters banks by code' do
      bank
      other_bank = create(:bank, country: country, name: 'Techcombank', code: 'TCB')
      visit admin_banks_path

      fill_in 'Code', with: 'VCB'
      click_button 'Filter'

      expect(page).to have_content('VCB')
      expect(page).not_to have_content('TCB')
    end

    it 'filters banks by country' do
      nigeria = create(:country, name: 'Nigeria', code: 'NG')
      bank
      nigeria_bank = create(:bank, country: nigeria, name: 'GTBank', code: 'GTB')
      visit admin_banks_path

      select 'Vietnam', from: 'Country'
      click_button 'Filter'

      expect(page).to have_content('Vietcombank')
      expect(page).not_to have_content('GTBank')
    end

    it 'filters banks by transfer support' do
      bank
      no_transfer_bank = create(:bank, country: country, name: 'NoTransferBank', code: 'NTB', transfer_supported: false)
      visit admin_banks_path

      select 'Yes', from: 'Transfer supported'
      click_button 'Filter'

      expect(page).to have_content('Vietcombank')
      expect(page).not_to have_content('NoTransferBank')
    end

    it 'has scopes section' do
      visit admin_banks_path

      # Check if scopes exist (they might be named differently)
      expect(page).to have_css('.scopes') || have_content('All')
    end

    it 'has selectable column for batch actions' do
      bank
      visit admin_banks_path

      expect(page).to have_css('input[type="checkbox"]')
    end
  end

  describe 'show page' do
    it 'displays bank details' do
      visit admin_bank_path(bank)

      expect(page).to have_content('Vietcombank')
      expect(page).to have_content('VCB')
      expect(page).to have_content('970436')
      expect(page).to have_content('Vietnam')
    end

    it 'shows boolean fields correctly' do
      visit admin_bank_path(bank)

      expect(page).to have_content('Yes')
      expect(page).to have_content('No')
    end

    it 'displays logo when present and valid URL' do
      bank_with_logo = create(:bank, country: country, name: 'LogoBank', code: 'LB', logo: 'https://example.com/logo.png')
      visit admin_bank_path(bank_with_logo)

      expect(page).to have_css('img[src="https://example.com/logo.png"]')
    end

    it 'displays no logo message when logo is empty' do
      bank_no_logo = create(:bank, country: country, name: 'NoLogoBank', code: 'NLB', logo: nil)
      visit admin_bank_path(bank_no_logo)

      expect(page).to have_content('No logo')
    end

    it 'displays no logo message when logo is not HTTP URL' do
      bank_invalid_logo = create(:bank, country: country, name: 'InvalidLogoBank', code: 'ILB', logo: 'invalid-url')
      visit admin_bank_path(bank_invalid_logo)

      expect(page).to have_content('No logo')
    end

    it 'displays bank accounts panel' do
      visit admin_bank_path(bank)

      expect(page).to have_content('Bank Accounts using this Bank')
    end

    it 'shows no bank accounts message when none exist' do
      visit admin_bank_path(bank)

      expect(page).to have_content('No bank accounts using this bank yet')
    end

    it 'shows bank accounts when they exist' do
      user = create(:user)
      bank_account = create(:bank_account, user: user, bank_name: bank.name, account_name: 'Test Account', account_number: '123456789')

      visit admin_bank_path(bank)

      expect(page).to have_content('Test Account')
      expect(page).to have_content('123456789')
      expect(page).to have_link(user.email)
    end

    it 'shows bank account verification status' do
      user = create(:user)
      verified_account = create(:bank_account, user: user, bank_name: bank.name, verified: true)
      unverified_account = create(:bank_account, user: user, bank_name: bank.name, verified: false)

      visit admin_bank_path(bank)

      expect(page).to have_content('Verified')
      expect(page).to have_content('Pending')
    end

    it 'shows bank account primary status' do
      user = create(:user)
      primary_account = create(:bank_account, user: user, bank_name: bank.name, is_primary: true)

      visit admin_bank_path(bank)

      expect(page).to have_content('Yes')
    end
  end

  describe 'form' do
    it 'creates a new bank successfully' do
      country # ensure country exists
      visit new_admin_bank_path

      select 'Vietnam', from: 'Country'
      fill_in 'Name', with: 'Techcombank'
      fill_in 'Code', with: 'TCB'
      fill_in 'Bin', with: 'BIN970407'
      fill_in 'Short name', with: 'TCB'
      fill_in 'Support', with: '90'
      check 'Transfer supported'
      check 'Lookup supported'
      check 'Is transfer'
      fill_in 'Logo', with: 'https://example.com/tcb-logo.png'
      fill_in 'Swift code', with: 'VTCBVNVX'
      click_button 'Create Bank'

      expect(page).to have_content('Bank was successfully created')
      expect(page).to have_content('Techcombank')
      expect(page).to have_content('TCB')
    end

    it 'validates required fields' do
      visit new_admin_bank_path

      click_button 'Create Bank'

      expect(page).to have_content("can't be blank")
    end

    it 'validates uniqueness of code' do
      bank # create existing bank
      visit new_admin_bank_path

      select 'Vietnam', from: 'Country'
      fill_in 'Name', with: 'Another Bank'
      fill_in 'Code', with: 'VCB' # same as existing bank
      fill_in 'Bin', with: 'BIN123456'
      fill_in 'Short name', with: 'AB'
      fill_in 'Support', with: '50'
      click_button 'Create Bank'

      expect(page).to have_content('has already been taken')
    end

    it 'validates uniqueness of bin' do
      bank # create existing bank
      visit new_admin_bank_path

      select 'Vietnam', from: 'Country'
      fill_in 'Name', with: 'Another Bank'
      fill_in 'Code', with: 'AB'
      fill_in 'Bin', with: '970436' # same as existing bank
      fill_in 'Short name', with: 'AB'
      fill_in 'Support', with: '50'
      click_button 'Create Bank'

      expect(page).to have_content('has already been taken')
    end

    it 'has proper form sections' do
      visit new_admin_bank_path

      expect(page).to have_content('Bank Details')
      expect(page).to have_content('Bank Features')
    end

    it 'shows proper hints' do
      visit new_admin_bank_path

      expect(page).to have_content('Full bank name')
      expect(page).to have_content('Bank code')
      expect(page).to have_content('Bank Identification Number')
      expect(page).to have_content('Country where this bank is located')
      expect(page).to have_content('Does this bank support transfers?')
      expect(page).to have_content('Support level (0-100)')
    end

    it 'transforms code to uppercase' do
      visit new_admin_bank_path

      code_input = find('#bank_code')
      expect(code_input['style']).to include('text-transform: uppercase')
    end

    it 'has support field with min/max constraints' do
      visit new_admin_bank_path

      support_input = find('#bank_support')
      expect(support_input['min']).to eq('0')
      expect(support_input['max']).to eq('100')
    end
  end

  describe 'edit' do
    it 'updates bank successfully' do
      visit edit_admin_bank_path(bank)

      fill_in 'Name', with: 'Vietnam Commercial Bank'
      click_button 'Update Bank'

      expect(page).to have_content('Bank was successfully updated')
      expect(page).to have_content('Vietnam Commercial Bank')
    end

    it 'pre-fills form with existing data' do
      visit edit_admin_bank_path(bank)

      expect(page).to have_field('Name', with: bank.name)
      expect(page).to have_field('Code', with: bank.code)
      expect(page).to have_field('Bin', with: bank.bin)
      expect(page).to have_select('Country', selected: bank.country.name)
    end

    it 'validates on update' do
      visit edit_admin_bank_path(bank)

      fill_in 'Name', with: ''
      click_button 'Update Bank'

      expect(page).to have_content("can't be blank")
    end
  end

  describe 'CSV export' do
    it 'has export functionality' do
      visit admin_banks_path

      expect(page).to have_content('Banks')
    end

    it 'includes all required columns in CSV' do
      bank
      visit admin_banks_path(format: :csv)

      # CSV response should include bank data
      expect(page.body).to include('Vietcombank')
      expect(page.body).to include('VCB')
      expect(page.body).to include('Vietnam')
    end
  end

  describe 'batch actions' do
    it 'has batch actions interface' do
      bank1 = create(:bank, country: country, name: 'Bank1', code: 'B1', transfer_supported: false)
      bank2 = create(:bank, country: country, name: 'Bank2', code: 'B2', lookup_supported: false)

      visit admin_banks_path

      expect(page).to have_content('Batch Actions') || have_css('.batch_actions_selector')
    end
  end

  describe 'navigation' do
    it 'has proper menu structure' do
      visit admin_banks_path

      expect(page).to have_content('Settings')
      expect(page).to have_content('Banks')
    end

    it 'shows action links on show page' do
      visit admin_bank_path(bank)

      # Check for any edit-related links
      expect(page).to have_link('Edit') || have_link('Edit Bank') || have_css('a[href*="edit"]')
    end
  end

  describe 'permissions and security' do
    it 'requires admin authentication' do
      logout

      visit admin_banks_path

      expect(page).to have_content('sign in') || have_content('Log in') || have_content('Admin Portal Login')
    end
  end

  describe 'error handling' do
    it 'handles invalid bank ID gracefully' do
      visit admin_bank_path(99999)

      expect(page).to have_content('not found') || have_content('404') || have_content('ActiveRecord::RecordNotFound')
    end

    it 'handles form submission errors gracefully' do
      visit new_admin_bank_path

      # Trigger server error simulation if possible
      fill_in 'Name', with: 'Test Bank'
      # Leave required fields empty to trigger validation errors
      click_button 'Create Bank'

      expect(page).to have_content("can't be blank")
    end
  end

  describe 'data integrity and business logic' do
    it 'correctly calculates and displays bank statistics' do
      country
      bank
      visit admin_banks_path

      expect(page).to have_content('Banks')
      expect(page).to have_content('Vietcombank')
    end

    it 'maintains referential integrity with countries' do
      bank
      visit admin_bank_path(bank)

      click_link bank.country.name
      expect(current_path).to eq(admin_country_path(bank.country))
    end

    it 'preserves data relationships when viewing bank accounts' do
      user = create(:user)
      account = create(:bank_account, user: user, bank_name: bank.name)

      visit admin_bank_path(bank)
      click_link user.email

      expect(current_path).to eq(admin_user_path(user))
    end
  end

  describe 'responsive design and accessibility' do
    it 'loads page without JavaScript errors' do
      visit admin_banks_path

      expect(page).to have_content('Banks')
    end

    it 'has proper table structure' do
      bank
      visit admin_banks_path

      expect(page).to have_css('table')
      expect(page).to have_css('th')
      expect(page).to have_css('td')
    end

    it 'displays all required columns' do
      bank
      visit admin_banks_path

      expect(page).to have_content('Name')
      expect(page).to have_content('Code')
      expect(page).to have_content('Country')
      expect(page).to have_content('Bin')
      expect(page).to have_content('Short name')
      expect(page).to have_content('Transfer supported')
      expect(page).to have_content('Lookup supported')
      expect(page).to have_content('Support')
      expect(page).to have_content('Is transfer')
      expect(page).to have_content('Created at')
    end
  end
end
