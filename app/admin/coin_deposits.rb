# frozen_string_literal: true

ActiveAdmin.register CoinDeposit do
  menu priority: 5, parent: 'Coin Management', label: 'Deposits'

  actions :index, :show

  filter :user
  filter :coin_account
  filter :coin_currency
  filter :coin_amount
  filter :coin_fee
  filter :tx_hash
  filter :out_index
  filter :status, as: :select, collection: lambda {
    CoinDeposit.aasm.states.map(&:name)
  }
  filter :last_seen_ip
  filter :verified_at
  filter :created_at
  filter :updated_at

  scope :all, default: true
  scope :pending
  scope :verified
  scope :locked
  scope :rejected
  scope :forged

  batch_action :verify, if: proc { authorized? :verify, CoinDeposit } do |ids|
    CoinDeposit.find(ids).each do |deposit|
      deposit.verify! if deposit.may_verify?
    end
    redirect_back(fallback_location: admin_coin_deposits_path, notice: 'Selected deposits were verified')
  end

  batch_action :reject, if: proc { authorized? :reject, CoinDeposit } do |ids|
    CoinDeposit.find(ids).each do |deposit|
      deposit.reject! if deposit.may_reject?
    end
    redirect_back(fallback_location: admin_coin_deposits_path, notice: 'Selected deposits were rejected')
  end

  index do
    selectable_column
    id_column
    column :user
    column :coin_currency
    column :coin_amount do |deposit|
      number_with_precision(deposit.coin_amount, precision: 8)
    end
    column :coin_fee do |deposit|
      number_with_precision(deposit.coin_fee, precision: 8)
    end
    column :tx_hash do |deposit|
      if deposit.tx_hash.present?
        link_to deposit.tx_hash.truncate(20),
          blockchain_explorer_url(deposit.coin_currency, deposit.tx_hash),
          target: '_blank',
          rel: 'noopener'
      end
    end
    column :confirmations do |deposit|
      "#{deposit.confirmations_count}/#{deposit.required_confirmations_count}"
    end
    column :status do |deposit|
      status_tag deposit.status, class: helpers.status_class(deposit.status)
    end
    column :last_seen_ip
    column :verified_at
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :user
      row :coin_account
      row :coin_currency
      row :coin_amount do |deposit|
        number_with_precision(deposit.coin_amount, precision: 8)
      end
      row :coin_fee do |deposit|
        number_with_precision(deposit.coin_fee, precision: 8)
      end
      row :tx_hash do |deposit|
        if deposit.tx_hash.present?
          link_to deposit.tx_hash,
            blockchain_explorer_url(deposit.coin_currency, deposit.tx_hash),
            target: '_blank',
            rel: 'noopener'
        end
      end
      row :out_index
      row :confirmations do |deposit|
        "#{deposit.confirmations_count}/#{deposit.required_confirmations_count}"
      end
      row :status do |deposit|
        status_tag deposit.status, class: helpers.status_class(deposit.status)
      end
      row :locked_reason if resource.locked?
      row :last_seen_ip do |deposit|
        if deposit.last_seen_ip.present?
          link_to deposit.last_seen_ip,
            "https://ipinfo.io/#{deposit.last_seen_ip}",
            target: '_blank',
            rel: 'noopener'
        end
      end
      row :verified_at
      row :metadata do |deposit|
        pre code deposit.metadata
      end
      row :created_at
      row :updated_at
    end

    panel 'Deposit Operation' do
      if resource.coin_deposit_operation.present?
        attributes_table_for resource.coin_deposit_operation do
          row :id do |op|
            link_to op.id, admin_coin_deposit_operation_path(op)
          end
          row :status do |op|
            status_tag op.status
          end
          row :coin_amount do |op|
            number_with_precision(op.coin_amount, precision: 8)
          end
          row :coin_fee do |op|
            number_with_precision(op.coin_fee, precision: 8)
          end
          row :platform_fee do |op|
            number_with_precision(op.platform_fee, precision: 8)
          end
          row :created_at
        end
      else
        para 'No deposit operation created yet.'
      end
    end

    active_admin_comments
  end

  sidebar 'State Actions', only: :show do
    if resource.pending?
      if authorized?(:verify, resource)
        button_to 'Verify',
          verify_admin_coin_deposit_path(resource),
          method: :put,
          class: 'button',
          data: { confirm: 'Are you sure?' }
      end

      if authorized?(:reject, resource)
        button_to 'Reject',
          reject_admin_coin_deposit_path(resource),
          method: :put,
          class: 'button',
          data: { confirm: 'Are you sure?' }
      end
    end

    if resource.locked? && authorized?(:release, resource) && authorized?(:release, resource)
        button_to 'Release',
          release_admin_coin_deposit_path(resource),
          method: :put,
          class: 'button',
          data: { confirm: 'Are you sure?' }
    end
  end

  member_action :verify, method: :put do
    resource.verify!
    redirect_back(fallback_location: admin_coin_deposit_path(resource),
      notice: 'Deposit was verified successfully')
  rescue AASM::InvalidTransition
    redirect_back(fallback_location: admin_coin_deposit_path(resource),
      alert: 'Cannot verify this deposit')
  end

  member_action :reject, method: :put do
    resource.reject!
    redirect_back(fallback_location: admin_coin_deposit_path(resource),
      notice: 'Deposit was rejected successfully')
  rescue AASM::InvalidTransition
    redirect_back(fallback_location: admin_coin_deposit_path(resource),
      alert: 'Cannot reject this deposit')
  end

  member_action :release, method: :put do
    resource.release!
    redirect_back(fallback_location: admin_coin_deposit_path(resource),
      notice: 'Deposit was released successfully')
  rescue AASM::InvalidTransition
    redirect_back(fallback_location: admin_coin_deposit_path(resource),
      alert: 'Cannot release this deposit')
  end

  controller do
    def scoped_collection
      super.includes(:user, :coin_account)
    end
  end
end
