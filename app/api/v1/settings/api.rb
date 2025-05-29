# frozen_string_literal: true

module V1
  module Settings
    class Api < Grape::API
      format :json

      resource :settings do
        desc 'Get exchange rates'
        get :exchange_rates do
          present_exchange_rates
        end

        desc 'Get withdrawal fees'
        get :withdrawal_fees do
          present_withdrawal_fees
        end

        desc 'Get trading fees'
        get :trading_fees do
          present_trading_fees
        end
      end

      helpers do
        def present_exchange_rates
          {
            exchange_rates: {
              usdt_to_vnd: Setting.usdt_to_vnd_rate,
              usdt_to_php: Setting.usdt_to_php_rate,
              usdt_to_ngn: Setting.usdt_to_ngn_rate
            }
          }
        end

        def present_withdrawal_fees
          {
            withdrawal_fees: {
              usdt_erc20: Setting.usdt_erc20_withdrawal_fee,
              usdt_bep20: Setting.usdt_bep20_withdrawal_fee,
              usdt_solana: Setting.usdt_solana_withdrawal_fee,
              usdt_trc20: Setting.usdt_trc20_withdrawal_fee
            }
          }
        end

        def present_trading_fees
          {
            trading_fees: {
              fee_ratios: {
                vnd: Setting.vnd_trading_fee_ratio,
                php: Setting.php_trading_fee_ratio,
                ngn: Setting.ngn_trading_fee_ratio,
                default: Setting.default_trading_fee_ratio
              },
              fixed_fees: {
                vnd: Setting.vnd_fixed_trading_fee,
                php: Setting.php_fixed_trading_fee,
                ngn: Setting.ngn_fixed_trading_fee,
                default: Setting.default_fixed_trading_fee
              }
            }
          }
        end
      end
    end
  end
end
