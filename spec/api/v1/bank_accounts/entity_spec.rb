require 'rails_helper'

RSpec.describe V1::BankAccounts::Entity do
  describe 'exposed attributes' do
    it 'exposes the correct attributes' do
      bank_account = {
        id: 1,
        bank_name: 'Test Bank',
        account_name: 'John Doe',
        account_number: '123456789',
        branch: 'Main Branch',
        country_code: 'US',
        verified: true,
        is_primary: true,
        created_at: Time.zone.now
      }

      entity = described_class.represent(bank_account)
      serialized = entity.as_json

      expect(serialized).to include(
        id: bank_account[:id],
        bank_name: bank_account[:bank_name],
        account_name: bank_account[:account_name],
        account_number: bank_account[:account_number],
        branch: bank_account[:branch],
        country_code: bank_account[:country_code],
        verified: bank_account[:verified],
        is_primary: bank_account[:is_primary]
      )
      expect(serialized).to have_key(:created_at)
    end

    it 'does not expose user_id and updated_at' do
      bank_account = {
        id: 1,
        bank_name: 'Test Bank',
        account_name: 'John Doe',
        account_number: '123456789',
        branch: 'Main Branch',
        country_code: 'US',
        verified: true,
        is_primary: true,
        created_at: Time.zone.now,
        user_id: 123,
        updated_at: Time.zone.now
      }

      entity = described_class.represent(bank_account)
      serialized = entity.as_json

      expect(serialized).not_to have_key(:user_id)
      expect(serialized).not_to have_key(:updated_at)
    end
  end

  describe V1::BankAccounts::DetailEntity do
    describe 'exposed attributes' do
      it 'exposes all attributes including those from parent' do
        bank_account = {
          id: 1,
          bank_name: 'Test Bank',
          account_name: 'John Doe',
          account_number: '123456789',
          branch: 'Main Branch',
          country_code: 'US',
          verified: true,
          is_primary: true,
          created_at: Time.zone.now,
          user_id: 123,
          updated_at: Time.zone.now
        }

        entity = described_class.represent(bank_account)
        serialized = entity.as_json

        expect(serialized).to include(
          id: bank_account[:id],
          bank_name: bank_account[:bank_name],
          account_name: bank_account[:account_name],
          account_number: bank_account[:account_number],
          branch: bank_account[:branch],
          country_code: bank_account[:country_code],
          verified: bank_account[:verified],
          is_primary: bank_account[:is_primary],
          user_id: bank_account[:user_id]
        )
        expect(serialized).to have_key(:created_at)
        expect(serialized).to have_key(:updated_at)
      end
    end

    describe 'inheritance' do
      it 'inherits from Entity' do
        expect(described_class.superclass).to eq(V1::BankAccounts::Entity)
      end
    end
  end
end
