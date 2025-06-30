ActiveAdmin.register Country do
  menu priority: 1, parent: 'Settings', label: 'Countries'

  actions :all, except: [ :destroy ]

  permit_params :name, :code

  index do
    selectable_column
    id_column
    column :name
    column :code
    column :banks_count do |country|
      country.banks.count
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :name
      row :code
      row :banks_count do |country|
        country.banks.count
      end
      row :created_at
      row :updated_at
    end

    panel 'Banks in this Country' do
      table_for resource.banks.ordered do
        column :name do |bank|
          link_to bank.name, admin_bank_path(bank)
        end
        column :code
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
      end
    end
  end

  form do |f|
    f.inputs 'Country Details' do
      f.input :name, hint: 'Full country name (e.g., Vietnam)'
      f.input :code, hint: 'ISO country code (e.g., VN)', input_html: { maxlength: 3, style: 'text-transform: uppercase;' }
    end
    f.actions
  end

  filter :name
  filter :code
  filter :created_at
  filter :updated_at

  # Custom actions
  member_action :banks, method: :get do
    @country = resource
    @banks = resource.banks.ordered
    render template: 'admin/countries/banks', layout: 'active_admin'
  end

  action_item :view_banks, only: :show do
    link_to 'View Banks', banks_admin_country_path(resource), class: 'button'
  end
end
