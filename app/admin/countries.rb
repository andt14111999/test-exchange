ActiveAdmin.register Country do
  menu priority: 18, label: 'Countries'

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
end
