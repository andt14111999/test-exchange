package com.exchangeengine.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaTopicsTest {

  @Test
  @DisplayName("TOPICS array should contain all topic constants")
  void topics_ShouldContainAllTopicConstants() {
    // Verify that each constant is included in the TOPICS array
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.COIN_ACCOUNT_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.COIN_DEPOSIT_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.COIN_WITHDRAWAL_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.RESET_BALANCE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.COIN_WITHDRAWAL_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.TRANSACTION_RESPONSE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.AMM_POOL_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.AMM_POOL_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.MERCHANT_ESCROW_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.MERCHANT_ESCROW_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.AMM_POSITION_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.AMM_POSITION_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.AMM_ORDER_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.AMM_ORDER_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.TRADE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.OFFER_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.TRADE_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.OFFER_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.BALANCES_LOCK_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.BALANCES_LOCK_UPDATE_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.TICK_QUERY_TOPIC);
    assertArrayContains(KafkaTopics.TOPICS, KafkaTopics.TICK_UPDATE_TOPIC);

    // Verify correct number of topics
    assertEquals(24, KafkaTopics.TOPICS.length);
  }

  @Test
  @DisplayName("QUERY_TOPICS array should contain only query-related topics")
  void queryTopics_ShouldContainOnlyQueryRelatedTopics() {
    assertArrayContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC);
    assertArrayContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.RESET_BALANCE_TOPIC);
    assertArrayContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.TICK_QUERY_TOPIC);

    // Verify that other topics are NOT in QUERY_TOPICS
    assertArrayNotContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.COIN_ACCOUNT_TOPIC);
    assertArrayNotContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.COIN_DEPOSIT_TOPIC);
    assertArrayNotContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.COIN_WITHDRAWAL_TOPIC);
    assertArrayNotContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC);
    assertArrayNotContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.TRANSACTION_RESPONSE_TOPIC);
    assertArrayNotContains(KafkaTopics.QUERY_TOPICS, KafkaTopics.TICK_UPDATE_TOPIC);
    // Verify correct number of query topics
    assertEquals(3, KafkaTopics.QUERY_TOPICS.length);
  }

  @Test
  @DisplayName("LOGIC_TOPICS chỉ nên chứa các logic processing topic")
  void logicTopics_ShouldContainOnlyLogicProcessingTopics() {
    // Kiểm tra số lượng topic đã đúng
    assertEquals(11, KafkaTopics.LOGIC_TOPICS.length);

    // Kiểm tra từng topic có trong mảng
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.COIN_ACCOUNT_TOPIC);
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.COIN_DEPOSIT_TOPIC);
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.COIN_WITHDRAWAL_TOPIC);
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.AMM_POOL_TOPIC);
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.MERCHANT_ESCROW_TOPIC);
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.AMM_POSITION_TOPIC);
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.AMM_ORDER_TOPIC);
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.TRADE_TOPIC);
    assertArrayContains(KafkaTopics.LOGIC_TOPICS, KafkaTopics.OFFER_TOPIC);
  }

  @Test
  @DisplayName("Input and output topics should follow naming convention")
  void topicNames_ShouldFollowNamingConvention() {
    // Input topics should start with EE.I.
    assertTrue(KafkaTopics.COIN_ACCOUNT_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.COIN_DEPOSIT_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.COIN_WITHDRAWAL_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.RESET_BALANCE_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.AMM_POOL_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.MERCHANT_ESCROW_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.AMM_POSITION_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.AMM_ORDER_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.TRADE_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.OFFER_TOPIC.startsWith("EE.I."));
    assertTrue(KafkaTopics.TICK_QUERY_TOPIC.startsWith("EE.I."));

    // Output topics should start with EE.O.
    assertTrue(KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.COIN_WITHDRAWAL_UPDATE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.TRANSACTION_RESPONSE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.AMM_POOL_UPDATE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.MERCHANT_ESCROW_UPDATE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.AMM_POSITION_UPDATE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.AMM_ORDER_UPDATE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.TRADE_UPDATE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.OFFER_UPDATE_TOPIC.startsWith("EE.O."));
    assertTrue(KafkaTopics.TICK_UPDATE_TOPIC.startsWith("EE.O."));
  }

  @Test
  @DisplayName("Topic constants should have appropriate values")
  void topicConstants_ShouldHaveAppropriateValues() {
    assertEquals("EE.I.coin_account", KafkaTopics.COIN_ACCOUNT_TOPIC);
    assertEquals("EE.I.coin_deposit", KafkaTopics.COIN_DEPOSIT_TOPIC);
    assertEquals("EE.I.coin_withdraw", KafkaTopics.COIN_WITHDRAWAL_TOPIC);
    assertEquals("EE.I.coin_account_query", KafkaTopics.COIN_ACCOUNT_QUERY_TOPIC);
    assertEquals("EE.I.coin_account_reset", KafkaTopics.RESET_BALANCE_TOPIC);
    assertEquals("EE.O.coin_account_update", KafkaTopics.COIN_ACCOUNT_UPDATE_TOPIC);
    assertEquals("EE.O.coin_withdrawal_update", KafkaTopics.COIN_WITHDRAWAL_UPDATE_TOPIC);
    assertEquals("EE.O.transaction_response", KafkaTopics.TRANSACTION_RESPONSE_TOPIC);
    assertEquals("EE.I.amm_pool", KafkaTopics.AMM_POOL_TOPIC);
    assertEquals("EE.O.amm_pool_update", KafkaTopics.AMM_POOL_UPDATE_TOPIC);
    assertEquals("EE.I.merchant_escrow", KafkaTopics.MERCHANT_ESCROW_TOPIC);
    assertEquals("EE.O.merchant_escrow_update", KafkaTopics.MERCHANT_ESCROW_UPDATE_TOPIC);
    assertEquals("EE.I.amm_position", KafkaTopics.AMM_POSITION_TOPIC);
    assertEquals("EE.O.amm_position_update", KafkaTopics.AMM_POSITION_UPDATE_TOPIC);
    assertEquals("EE.I.amm_order", KafkaTopics.AMM_ORDER_TOPIC);
    assertEquals("EE.O.amm_order_update", KafkaTopics.AMM_ORDER_UPDATE_TOPIC);
    assertEquals("EE.I.trade", KafkaTopics.TRADE_TOPIC);
    assertEquals("EE.I.offer", KafkaTopics.OFFER_TOPIC);
    assertEquals("EE.O.trade_update", KafkaTopics.TRADE_UPDATE_TOPIC);
    assertEquals("EE.O.offer_update", KafkaTopics.OFFER_UPDATE_TOPIC);
    assertEquals("EE.I.tick_query", KafkaTopics.TICK_QUERY_TOPIC);
    assertEquals("EE.O.tick_update", KafkaTopics.TICK_UPDATE_TOPIC);
  }

  // Helper method to check if array contains a specific string
  private void assertArrayContains(String[] array, String value) {
    boolean found = false;
    for (String item : array) {
      if (item.equals(value)) {
        found = true;
        break;
      }
    }
    assertTrue(found, "Array should contain " + value);
  }

  // Helper method to check if array does not contain a specific string
  private void assertArrayNotContains(String[] array, String value) {
    boolean found = false;
    for (String item : array) {
      if (item.equals(value)) {
        found = true;
        break;
      }
    }
    assertFalse(found, "Array should not contain " + value);
  }
}
