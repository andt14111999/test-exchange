# frozen_string_literal: true

require 'rails_helper'

RSpec.describe InlineEditHelper, type: :helper do
  describe '#inline_edit_field' do
    context 'with a superadmin user' do
      context 'with boolean field' do
        it 'renders an inline editable checkbox field' do
          superadmin = create(:admin_user, :superadmin)
          allow(helper).to receive(:current_admin_user).and_return(superadmin)

          user = create(:user, snowfox_employee: true)
          allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

          result = helper.inline_edit_field(user, :snowfox_employee, type: :boolean)

          expect(result).to have_css('[data-controller="inline-edit"]')
          expect(result).to have_css('[data-inline-edit-url-value="/admin/users/1"]')
          expect(result).to have_css('[data-inline-edit-field-value="snowfox_employee"]')
          expect(result).to have_css('.inline-edit-trigger')
          expect(result).to have_css('input[type="checkbox"]', visible: false)
          expect(result).to have_css('.status_tag.yes', text: 'Yes')
        end
      end

      context 'with text field' do
        it 'renders an inline editable text field' do
          superadmin = create(:admin_user, :superadmin)
          allow(helper).to receive(:current_admin_user).and_return(superadmin)

          user = create(:user, display_name: 'John Doe')
          allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

          result = helper.inline_edit_field(user, :display_name)

          expect(result).to have_css('[data-controller="inline-edit"]')
          expect(result).to have_css('.inline-edit-trigger')
          expect(result).to have_css('input[type="text"][value="John Doe"]', visible: false)
          expect(result).to include('John Doe')
        end
      end

      context 'with select field' do
        it 'renders an inline editable select field' do
          superadmin = create(:admin_user, :superadmin)
          allow(helper).to receive(:current_admin_user).and_return(superadmin)

          user = create(:user, role: 'user')
          allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

          collection = [ [ 'User', 'user' ], [ 'Merchant', 'merchant' ], [ 'Admin', 'admin' ] ]
          result = helper.inline_edit_field(user, :role, type: :select, collection: collection)

          expect(result).to have_css('[data-controller="inline-edit"]')
          expect(result).to have_css('.inline-edit-trigger')
          expect(result).to have_css('select', visible: false)
          expect(result).to have_css('option[value="user"]', visible: false)
          expect(result).to have_css('option[value="merchant"]', visible: false)
          expect(result).to have_css('option[value="admin"]', visible: false)
        end
      end

      context 'with number field' do
        it 'renders an inline editable text field for numeric values' do
          superadmin = create(:admin_user, :superadmin)
          allow(helper).to receive(:current_admin_user).and_return(superadmin)

          user = create(:user, kyc_level: 2)
          allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

          result = helper.inline_edit_field(user, :kyc_level)

          expect(result).to have_css('[data-controller="inline-edit"]')
          expect(result).to have_css('.inline-edit-trigger')
          expect(result).to have_css('input[type="text"][value="2"]', visible: false)
        end
      end

      context 'with field type inference' do
        it 'infers boolean type correctly' do
          superadmin = create(:admin_user, :superadmin)
          allow(helper).to receive(:current_admin_user).and_return(superadmin)

          user = create(:user)
          allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

          # Don't specify type, let it infer
          result = helper.inline_edit_field(user, :phone_verified)

          expect(result).to have_css('input[type="checkbox"]', visible: false)
        end

        it 'infers text type for string fields' do
          superadmin = create(:admin_user, :superadmin)
          allow(helper).to receive(:current_admin_user).and_return(superadmin)

          user = create(:user)
          allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

          result = helper.inline_edit_field(user, :email)

          expect(result).to have_css('input[type="text"]', visible: false)
        end

        it 'infers number type for integer fields' do
          superadmin = create(:admin_user, :superadmin)
          allow(helper).to receive(:current_admin_user).and_return(superadmin)

          user = create(:user)
          allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

          result = helper.inline_edit_field(user, :kyc_level)

          expect(result).to have_css('input[type="text"]', visible: false)
        end

        it 'handles unknown field gracefully' do
          superadmin = create(:admin_user, :superadmin)
          allow(helper).to receive(:current_admin_user).and_return(superadmin)

          user = create(:user)
          allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

          # Mock a field that doesn't exist in columns_hash
          allow(user.class).to receive(:columns_hash).and_return({})
          allow(user).to receive(:send).with(:non_existent_field).and_return('some value')

          result = helper.inline_edit_field(user, :non_existent_field)

          expect(result).to have_css('input[type="text"]', visible: false)
        end
      end
    end

    context 'with an operator user' do
      it 'does not show edit trigger for users without permission' do
        operator = create(:admin_user, :operator)
        allow(helper).to receive(:current_admin_user).and_return(operator)

        user = create(:user, snowfox_employee: true)
        allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

        result = helper.inline_edit_field(user, :snowfox_employee, type: :boolean)

        expect(result).to have_css('[data-controller="inline-edit"]')
        expect(result).not_to have_css('.inline-edit-trigger')
        expect(result).to have_css('.status_tag.yes', text: 'Yes')
      end
    end

    context 'without current_admin_user' do
      it 'does not show edit trigger when no admin user is present' do
        allow(helper).to receive(:current_admin_user).and_return(nil)

        user = create(:user, display_name: 'Test User')
        allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

        result = helper.inline_edit_field(user, :display_name)

        expect(result).not_to have_css('.inline-edit-trigger')
        expect(result).to include('Test User')
      end
    end

    context 'formatting values' do
      it 'formats boolean false values correctly' do
        superadmin = create(:admin_user, :superadmin)
        allow(helper).to receive(:current_admin_user).and_return(superadmin)

        user = create(:user, snowfox_employee: false)
        allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

        result = helper.inline_edit_field(user, :snowfox_employee, type: :boolean)

        expect(result).to have_css('.status_tag.no', text: 'No')
      end

      it 'handles nil values gracefully' do
        superadmin = create(:admin_user, :superadmin)
        allow(helper).to receive(:current_admin_user).and_return(superadmin)

        user = create(:user, display_name: nil)
        allow(helper).to receive(:admin_resource_path).with(user).and_return('/admin/users/1')

        result = helper.inline_edit_field(user, :display_name)

        expect(result).to have_css('[data-controller="inline-edit"]')
        # The text field for nil will be rendered
        expect(result).to have_css('input[type="text"]', visible: false)
        # The display should be empty
        expect(result).to have_css('[data-inline-edit-target="display"]')
      end
    end
  end
end
