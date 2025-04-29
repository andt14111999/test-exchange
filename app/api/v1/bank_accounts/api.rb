# frozen_string_literal: true

module V1
  module BankAccounts
    class Api < Grape::API
      helpers Api::V1::Helpers::AuthHelper

      before { authenticate_user! }

      resource :bank_accounts do
        desc 'Get list of bank accounts'
        params do
          optional :country_code, type: String, desc: 'Filter by country code'
          optional :bank_name, type: String, desc: 'Filter by bank name'
          optional :verified, type: Boolean, desc: 'Filter by verification status'
          optional :page, type: Integer, default: 1, desc: 'Page number'
          optional :per_page, type: Integer, default: 20, desc: 'Items per page'
        end
        get do
          bank_accounts = current_user.bank_accounts.order(is_primary: :desc, created_at: :desc)

          # Apply filters
          bank_accounts = bank_accounts.of_country(params[:country_code]) if params[:country_code].present?
          bank_accounts = bank_accounts.where(bank_name: params[:bank_name]) if params[:bank_name].present?
          bank_accounts = bank_accounts.where(verified: params[:verified]) if params[:verified].present?

          # Pagination
          bank_accounts = bank_accounts.page(params[:page]).per(params[:per_page])

          present bank_accounts, with: V1::BankAccounts::Entity
        end

        desc 'Get bank account details'
        params do
          requires :id, type: String, desc: 'Bank account ID'
        end
        get ':id' do
          bank_account = current_user.bank_accounts.find(params[:id])
          present bank_account, with: V1::BankAccounts::DetailEntity
        end

        desc 'Create a new bank account'
        params do
          requires :bank_name, type: String, desc: 'Bank name'
          requires :account_name, type: String, desc: 'Account holder name'
          requires :account_number, type: String, desc: 'Account number'
          requires :country_code, type: String, desc: 'Country code'
          optional :branch, type: String, desc: 'Bank branch'
          optional :is_primary, type: Boolean, default: false, desc: 'Set as primary account'
        end
        post do
          # Check bank account limit (if needed)
          max_accounts = 10 # Assume maximum of 10 accounts
          if current_user.bank_accounts.count >= max_accounts
            error!({ error: "You can have at most #{max_accounts} bank accounts" }, 400)
          end

          # If this is the first account, automatically mark it as primary
          params[:is_primary] = true if current_user.bank_accounts.count == 0

          bank_account = current_user.bank_accounts.new(
            bank_name: params[:bank_name],
            account_name: params[:account_name],
            account_number: params[:account_number],
            branch: params[:branch],
            country_code: params[:country_code],
            is_primary: params[:is_primary],
            verified: false # Default to unverified
          )

          if bank_account.save
            present bank_account, with: V1::BankAccounts::DetailEntity
          else
            error!({ error: bank_account.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Update bank account'
        params do
          requires :id, type: String, desc: 'Bank account ID'
          optional :bank_name, type: String, desc: 'Bank name'
          optional :account_name, type: String, desc: 'Account holder name'
          optional :account_number, type: String, desc: 'Account number'
          optional :branch, type: String, desc: 'Bank branch'
          optional :country_code, type: String, desc: 'Country code'
          optional :is_primary, type: Boolean, desc: 'Set as primary account'
        end
        put ':id' do
          bank_account = current_user.bank_accounts.find(params[:id])

          # If account is verified, don't allow changing important information
          if bank_account.verified? && (
            params[:bank_name].present? ||
            params[:account_name].present? ||
            params[:account_number].present? ||
            params[:country_code].present?
          )
            error!({ error: 'Cannot change information of a verified account' }, 400)
          end

          # Update information
          update_params = {}
          update_params[:bank_name] = params[:bank_name] if params[:bank_name].present?
          update_params[:account_name] = params[:account_name] if params[:account_name].present?
          update_params[:account_number] = params[:account_number] if params[:account_number].present?
          update_params[:branch] = params[:branch] if params[:branch].present?
          update_params[:country_code] = params[:country_code] if params[:country_code].present?
          update_params[:is_primary] = params[:is_primary] if params.key?(:is_primary)

          if bank_account.update(update_params)
            present bank_account, with: V1::BankAccounts::DetailEntity
          else
            error!({ error: bank_account.errors.full_messages.join(', ') }, 422)
          end
        end

        desc 'Delete bank account'
        params do
          requires :id, type: String, desc: 'Bank account ID'
        end
        delete ':id' do
          bank_account = current_user.bank_accounts.find(params[:id])

          # Don't allow deleting a bank account being used in active transactions
          if bank_account.is_primary?
            # Check if there are other accounts
            if current_user.bank_accounts.count == 1
              error!({ error: 'Cannot delete the only bank account' }, 400)
            else
              error!({ error: 'Cannot delete primary account. Please set another account as primary first' }, 400)
            end
          end

          # Check if account is being used in any transactions
          # This depends on the business logic of the application

          if bank_account.destroy
            { success: true, message: 'Bank account has been deleted' }
          else
            error!({ error: 'Could not delete bank account' }, 422)
          end
        end

        desc 'Set bank account as primary'
        params do
          requires :id, type: String, desc: 'Bank account ID'
        end
        put ':id/primary' do
          bank_account = current_user.bank_accounts.find(params[:id])

          if bank_account.mark_as_primary!
            present bank_account, with: V1::BankAccounts::DetailEntity
          else
            error!({ error: 'Could not set account as primary' }, 422)
          end
        end
      end
    end
  end
end
