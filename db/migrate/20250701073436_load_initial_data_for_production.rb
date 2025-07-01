class LoadInitialDataForProduction < ActiveRecord::Migration[8.0]
  def up
    return if Rails.env.test?

    countries_data = [
      { name: "Vietnam", code: "VN" },
      { name: "Nigeria", code: "NG" },
      { name: "Ghana", code: "GH" }
    ]

    countries_data.each do |country_data|
      country = Country.find_or_initialize_by(code: country_data[:code])
      country.assign_attributes(country_data)
      country.save if country.new_record?
    end

    vietnam = Country.find_by(code: 'VN')
    return unless vietnam

    banks_json_path = Rails.root.join('data', 'banks.json')
    return unless File.exist?(banks_json_path)

    begin
      banks_json = JSON.parse(File.read(banks_json_path))
      banks_data = banks_json['data']
      return unless banks_data.is_a?(Array)
    rescue
      return
    end

    banks_data.each do |bank_data|
      required_fields = %w[code name bin short_name]
      next if required_fields.any? { |field| bank_data[field].blank? }

      bank = Bank.find_or_initialize_by(code: bank_data['code'])

      if bank.new_record?
        bank.assign_attributes(
          name: bank_data['name'],
          bin: bank_data['bin'],
          short_name: bank_data['short_name'],
          logo: bank_data['logo'],
          transfer_supported: bank_data['transferSupported'] == 1,
          lookup_supported: bank_data['lookupSupported'] == 1,
          support: bank_data['support'] || 0,
          is_transfer: bank_data['isTransfer'] == 1,
          swift_code: bank_data['swift_code'],
          country: vietnam
        )
        bank.save
      end
    end
  end

  def down
    return if Rails.env.test?

    if Rails.env.production?
      # Disable automatic rollback in production
      return
    end

    Bank.joins(:country).where(countries: { code: 'VN' }).delete_all
    Country.where(code: %w[VN NG GH]).delete_all
  end
end
