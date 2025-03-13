# frozen_string_literal: true

ActiveAdmin.register CoinDepositOperation do
  menu priority: 5, parent: 'Coin Management', label: 'Deposit Operations'

  actions :index, :show

  filter :coin_account
  filter :coin_deposit
  filter :coin_currency
  filter :coin_amount
  filter :coin_fee
  filter :tx_hash
  filter :out_index
  filter :status
  filter :created_at

  index do
    selectable_column
    id_column
    column :coin_account
    column :coin_currency
    column :coin_amount do |op|
      number_with_precision(op.coin_amount, precision: 8)
    end
    column :coin_fee do |op|
      number_with_precision(op.coin_fee, precision: 8)
    end
    column :amount_after_fee do |op|
      number_with_precision(op.coin_amount_after_fee, precision: 8)
    end
    column :tx_hash do |op|
      if op.tx_hash.present?
        link_to op.tx_hash&.truncate(20), "#{blockchain_explorer_url(op.coin_currency)}/tx/#{op.tx_hash}",
          target: '_blank', rel: 'noopener'
      end
    end
    column :status do |op|
      status_tag op.status
    end
    column :created_at
    actions
  end

  show do
    attributes_table do
      row :id
      row :coin_account
      row :coin_deposit
      row :coin_currency
      row :coin_amount do |op|
        number_with_precision(op.coin_amount, precision: 8)
      end
      row :coin_fee do |op|
        number_with_precision(op.coin_fee, precision: 8)
      end
      row :amount_after_fee do |op|
        number_with_precision(op.coin_amount_after_fee, precision: 8)
      end
      row :tx_hash do |op|
        if op.tx_hash.present?
          link_to op.tx_hash, "#{blockchain_explorer_url(op.coin_currency)}/tx/#{op.tx_hash}",
            target: '_blank', rel: 'noopener'
        end
      end
      row :out_index
      row :status do |op|
        status_tag op.status
      end
      row :created_at
      row :updated_at
    end

    panel 'Related Transactions' do
      table_for resource.coin_transactions do
        column :id do |tx|
          link_to tx.id, admin_coin_transaction_path(tx)
        end
        column :amount do |tx|
          number_with_precision(tx.amount, precision: 8)
        end
        column :snapshot_balance do |tx|
          number_with_precision(tx.snapshot_balance, precision: 8)
        end
        column :snapshot_frozen_balance do |tx|
          number_with_precision(tx.snapshot_frozen_balance, precision: 8)
        end
        column :created_at
      end
    end
  end
end
