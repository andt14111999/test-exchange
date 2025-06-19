ActiveAdmin.register CoinSetting do
  menu priority: 2, parent: 'Settings', label: 'Coin Settings'
  permit_params :currency, :deposit_enabled, :withdraw_enabled, :swap_enabled, :layers

  json_editor

  index do
    selectable_column
    id_column
    column :currency
    column :deposit_enabled
    column :withdraw_enabled
    column :swap_enabled
    column :layers do |cs|
      cs.layers&.map { |l| l['layer'] }.join(', ')
    end
    actions
  end

  show do
    attributes_table do
      row :id
      row :currency
      row :deposit_enabled
      row :withdraw_enabled
      row :swap_enabled
      row :layers do |cs|
        table_for(cs.layers || []) do
          column('Layer') { |l| l['layer'] }
          column('Deposit') { |l| status_tag(l['deposit_enabled'] ? 'yes' : 'no') }
          column('Withdraw') { |l| status_tag(l['withdraw_enabled'] ? 'yes' : 'no') }
          column('Swap') { |l| status_tag(l['swap_enabled'] ? 'yes' : 'no') }
          column('Maintenance') { |l| status_tag(l['maintenance'] ? 'yes' : 'no') }
        end
      end
      row :created_at
      row :updated_at
    end
  end

  form do |f|
    f.inputs do
      f.input :currency
      f.input :deposit_enabled
      f.input :withdraw_enabled
      f.input :swap_enabled
      f.input :layers, as: :jsonb, label: 'Layers (JSON)', input_html: { 'data-mode': 'tree' }, hint: '<span style="font-size:12px;color:#888;">Nhập array các layer, ví dụ: <code>[{"layer":"erc20","deposit_enabled":true,"withdraw_enabled":true,"swap_enabled":true,"maintenance":false}]</code></span>'.html_safe
    end
    f.actions
  end

  controller do
    def edit
      @coin_setting = CoinSetting.find(params[:id])
      @coin_setting.define_singleton_method(:fake_layers) do
        @fake_layers ||= (layers || []).map { |l| OpenStruct.new(l) }
      end
      @coin_setting.define_singleton_method(:fake_layers_attributes=) do |attrs|
        @fake_layers = attrs.values.map { |v| OpenStruct.new(v) }
      end
    end

    def update
      fake_layers = params[:coin_setting].delete(:fake_layers_attributes)
      if fake_layers
        params[:coin_setting][:layers] = fake_layers.values.map do |v|
          v.permit(:layer, :deposit_enabled, :withdraw_enabled, :swap_enabled, :maintenance).to_h
        end
      end
      super
    end

    def create
      fake_layers = params[:coin_setting].delete(:fake_layers_attributes)
      if fake_layers
        params[:coin_setting][:layers] = fake_layers.values.map do |v|
          v.permit(:layer, :deposit_enabled, :withdraw_enabled, :swap_enabled, :maintenance).to_h
        end
      end
      super
    end
  end
end
