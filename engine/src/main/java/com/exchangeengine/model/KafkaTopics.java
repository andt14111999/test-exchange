package com.exchangeengine.model;

public class KafkaTopics {
        public static final String COIN_ACCOUNT_TOPIC = "EE.I.coin_account";
        public static final String COIN_DEPOSIT_TOPIC = "EE.I.coin_deposit";
        public static final String COIN_WITHDRAWAL_TOPIC = "EE.I.coin_withdraw";
        public static final String AMM_POOL_TOPIC = "EE.I.amm_pool";
        public static final String MERCHANT_ESCROW_TOPIC = "EE.I.merchant_escrow";
        public static final String AMM_POSITION_TOPIC = "EE.I.amm_position";
        public static final String AMM_ORDER_TOPIC = "EE.I.amm_order";
        public static final String COIN_ACCOUNT_QUERY_TOPIC = "EE.I.coin_account_query";
        public static final String RESET_BALANCE_TOPIC = "EE.I.coin_account_reset"; // developer only

        public static final String COIN_ACCOUNT_UPDATE_TOPIC = "EE.O.coin_account_update";
        public static final String COIN_WITHDRAWAL_UPDATE_TOPIC = "EE.O.coin_withdrawal_update";
        public static final String TRANSACTION_RESPONSE_TOPIC = "EE.O.transaction_response";
        public static final String AMM_POOL_UPDATE_TOPIC = "EE.O.amm_pool_update";
        public static final String MERCHANT_ESCROW_UPDATE_TOPIC = "EE.O.merchant_escrow_update";
        public static final String AMM_POSITION_UPDATE_TOPIC = "EE.O.amm_position_update";
        public static final String AMM_ORDER_UPDATE_TOPIC = "EE.O.amm_order_update";

        // New topics for trade and offer events
        public static final String TRADE_TOPIC = "EE.I.trade";
        public static final String OFFER_TOPIC = "EE.I.offer";
        public static final String TRADE_UPDATE_TOPIC = "EE.O.trade_update";
        public static final String OFFER_UPDATE_TOPIC = "EE.O.offer_update";

        // Topics for tick query and update
        public static final String TICK_QUERY_TOPIC = "EE.I.tick_query";
        public static final String TICK_UPDATE_TOPIC = "EE.O.tick_update";
        
        // Topics for balance lock events
        public static final String BALANCES_LOCK_TOPIC = "EE.I.balances_lock";
        public static final String BALANCES_LOCK_UPDATE_TOPIC = "EE.O.balances_lock_update";

        public static final String[] TOPICS = {
                        COIN_ACCOUNT_TOPIC,
                        COIN_DEPOSIT_TOPIC,
                        COIN_WITHDRAWAL_TOPIC,
                        AMM_POOL_TOPIC,
                        MERCHANT_ESCROW_TOPIC,
                        AMM_POSITION_TOPIC,
                        AMM_ORDER_TOPIC,
                        COIN_ACCOUNT_QUERY_TOPIC,
                        RESET_BALANCE_TOPIC,
                        COIN_ACCOUNT_UPDATE_TOPIC,
                        COIN_WITHDRAWAL_UPDATE_TOPIC,
                        TRANSACTION_RESPONSE_TOPIC,
                        AMM_POOL_UPDATE_TOPIC,
                        MERCHANT_ESCROW_UPDATE_TOPIC,
                        AMM_POSITION_UPDATE_TOPIC,
                        AMM_ORDER_UPDATE_TOPIC,
                        TRADE_TOPIC,
                        OFFER_TOPIC,
                        TRADE_UPDATE_TOPIC,
                        OFFER_UPDATE_TOPIC,
                        TICK_QUERY_TOPIC,
                        TICK_UPDATE_TOPIC,
                        BALANCES_LOCK_TOPIC,
                        BALANCES_LOCK_UPDATE_TOPIC,
        };

        // Topics cho các truy vấn và reset, tách riêng để xử lý riêng
        public static final String[] QUERY_TOPICS = {
                        COIN_ACCOUNT_QUERY_TOPIC,
                        RESET_BALANCE_TOPIC,
                        TICK_QUERY_TOPIC,
        };

        // Topics cho các logic xử lý chính (không bao gồm query)
        public static final String[] LOGIC_TOPICS = {
                        COIN_ACCOUNT_TOPIC,
                        COIN_DEPOSIT_TOPIC,
                        COIN_WITHDRAWAL_TOPIC,
                        AMM_POOL_TOPIC,
                        MERCHANT_ESCROW_TOPIC,
                        AMM_POSITION_TOPIC,
                        AMM_ORDER_TOPIC,
                        TRADE_TOPIC,
                        OFFER_TOPIC,
                        BALANCES_LOCK_TOPIC,
                        BALANCES_LOCK_UPDATE_TOPIC,
        };

        private KafkaTopics() {
        }
}
