ActiveAdmin.register Bank do
  menu priority: 2, parent: 'Settings', label: 'Banks'

  actions :all

  permit_params :name, :code, :bin, :short_name, :logo, :transfer_supported,
                :lookup_supported, :support, :is_transfer, :swift_code, :country_id

  index do
    selectable_column
    id_column
    column :name
    column :code
    column :country do |bank|
      link_to bank.country.name, admin_country_path(bank.country) if bank.country
    end
    column :bin
    column :short_name
    column :transfer_supported do |bank|
      bank.transfer_supported ? 'Yes' : 'No'
    end
    column :lookup_supported do |bank|
      bank.lookup_supported ? 'Yes' : 'No'
    end
    column :support
    column :is_transfer do |bank|
      bank.is_transfer ? 'Yes' : 'No'
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :name
      row :code
      row :country do |bank|
        link_to bank.country.name, admin_country_path(bank.country) if bank.country
      end
      row :bin
      row :short_name
      row :logo do |bank|
        if bank.logo.present? && bank.logo.include?('http')
          image_tag bank.logo, size: '100x50'
        else
          'No logo'
        end
      end
      row :transfer_supported do |bank|
        bank.transfer_supported ? 'Yes' : 'No'
      end
      row :lookup_supported do |bank|
        bank.lookup_supported ? 'Yes' : 'No'
      end
      row :support
      row :is_transfer do |bank|
        bank.is_transfer ? 'Yes' : 'No'
      end
      row :swift_code
      row :created_at
      row :updated_at
    end

    panel 'Bank Accounts using this Bank' do
      bank_accounts = BankAccount.where(bank_name: bank.name)

      if bank_accounts.exists?
        table_for bank_accounts.includes(:user) do
          column :id
          column :user do |account|
            link_to account.user.email, admin_user_path(account.user) if account.user
          end
          column :account_name
          column :account_number
          column :is_primary do |account|
            account.is_primary ? 'Yes' : 'No'
          end
          column :verified do |account|
            account.verified ? 'Verified' : 'Pending'
          end
          column :created_at
        end
      else
        div 'No bank accounts using this bank yet.'
      end
    end
  end

  form do |f|
    f.inputs 'Bank Details' do
      f.input :country, as: :select, collection: Country.ordered.pluck(:name, :id),
              prompt: 'Select Country', hint: 'Country where this bank is located'
      f.input :name, hint: 'Full bank name (e.g., Vietcombank)'
      f.input :code, hint: 'Bank code (e.g., VCB)', input_html: { style: 'text-transform: uppercase;' }
      f.input :bin, hint: 'Bank Identification Number'
      f.input :short_name, hint: 'Short name or abbreviation'
      f.input :logo, hint: 'URL to bank logo image'
      f.input :swift_code, hint: 'SWIFT/BIC code for international transfers'
    end

    f.inputs 'Bank Features' do
      f.input :transfer_supported, as: :boolean, hint: 'Does this bank support transfers?'
      f.input :lookup_supported, as: :boolean, hint: 'Does this bank support account lookup?'
      f.input :is_transfer, as: :boolean, hint: 'Is this a transfer bank?'
      f.input :support, as: :number, hint: 'Support level (0-100)',
              input_html: { min: 0, max: 100 }
    end

    f.actions
  end

  filter :name
  filter :code
  filter :country, as: :select, collection: -> { Country.ordered.pluck(:name, :id) }
  filter :bin
  filter :short_name
  filter :transfer_supported
  filter :lookup_supported
  filter :is_transfer
  filter :support
  filter :created_at
  filter :updated_at

  # Scopes for quick filtering
  scope :all, default: true
  scope :transfer_supported
  scope :lookup_supported

  # Batch actions
  batch_action :enable_transfer_support do |ids|
    Bank.where(id: ids).update_all(transfer_supported: true)
    redirect_to collection_path, notice: 'Transfer support enabled for selected banks.'
  end

  batch_action :disable_transfer_support do |ids|
    Bank.where(id: ids).update_all(transfer_supported: false)
    redirect_to collection_path, notice: 'Transfer support disabled for selected banks.'
  end

  batch_action :enable_lookup_support do |ids|
    Bank.where(id: ids).update_all(lookup_supported: true)
    redirect_to collection_path, notice: 'Lookup support enabled for selected banks.'
  end

  batch_action :disable_lookup_support do |ids|
    Bank.where(id: ids).update_all(lookup_supported: false)
    redirect_to collection_path, notice: 'Lookup support disabled for selected banks.'
  end

  # CSV export
  csv do
    column :id
    column :name
    column :code
    column :country do |bank|
      bank.country&.name
    end
    column :bin
    column :short_name
    column :transfer_supported
    column :lookup_supported
    column :support
    column :is_transfer
    column :swift_code
    column :created_at
    column :updated_at
  end
end
