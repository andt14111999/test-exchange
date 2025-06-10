# frozen_string_literal: true

ActiveAdmin.register AdminUser do
  extend ActiveAdmin::BaseAdminHelper

  permit_params :email, :fullname, :roles

  before_action :ensure_superadmin_user, only: %i[create update destroy]

  index do
    id_column
    column :email
    column :fullname
    column :roles
    column :created_at
  end

  filter :email
  filter :roles
  filter :fullname
  filter :created_at

  form do |f|
    f.inputs do
      f.input :email
      f.input :roles, as: :select, collection: AdminUser::ROLES
    end
    f.actions
  end

  show do |admin_user|
    attributes_table do
      row :id
      row :email
      row :fullname
      row :roles
      row :created_at
      row :updated_at
      row :authenticator_enabled
      if current_admin_user.id == admin_user.id
        row :authenticator do
          context = admin_user.authenticator_enabled? ? 'Disable' : 'Enable'
          open_2fa_button(context)
        end
      end
    end
  end

  controller do
    private

    def ensure_superadmin_user
      return if current_admin_user.superadmin?

      flash[:error] = 'You are not authorized to perform this action.'
      redirect_to admin_root_path
    end
  end
end
