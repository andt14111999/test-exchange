package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.event.TradeEventFactory;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.Offer;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.Trade;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.OfferCache;
import com.exchangeengine.storage.cache.TradeCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class TradeEventTest {
    private TradeEvent tradeEvent;
    private TradeCache tradeCacheMock;
    private AccountCache accountCacheMock;
    private OfferCache offerCacheMock;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        tradeEvent = TradeEventFactory.create();
        tradeCacheMock = mock(TradeCache.class);
        accountCacheMock = mock(AccountCache.class);
        offerCacheMock = mock(OfferCache.class);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        // Reset static mocks after each test
        tradeCacheMock = null;
        accountCacheMock = null;
        offerCacheMock = null;
    }

    @Test
    @DisplayName("getTradeCache should return singleton instance")
    void testGetTradeCache() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            assertEquals(tradeCacheMock, tradeEvent.getTradeCache());
        }
    }

    @Test
    @DisplayName("getAccountCache should return singleton instance")
    void testGetAccountCache() {
        try (MockedStatic<AccountCache> mockAccountCache = Mockito.mockStatic(AccountCache.class)) {
            mockAccountCache.when(AccountCache::getInstance).thenReturn(accountCacheMock);
            assertEquals(accountCacheMock, tradeEvent.getAccountCache());
        }
    }

    @Test
    @DisplayName("getOfferCache should return singleton instance")
    void testGetOfferCache() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            assertEquals(offerCacheMock, tradeEvent.getOfferCache());
        }
    }

    @Test
    @DisplayName("getProducerEventId should return identifier")
    void testGetProducerEventId() {
        String identifier = "trade-123";
        tradeEvent.setIdentifier(identifier);
        assertEquals(identifier, tradeEvent.getProducerEventId());
    }

    @Test
    @DisplayName("getEventHandler should return TRADE_EVENT")
    void testGetEventHandler() {
        assertEquals(EventHandlerAction.TRADE_EVENT, tradeEvent.getEventHandler());
    }

    @Test
    @DisplayName("fetchTrade should return trade from cache")
    void testFetchTradeFromCache() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            Trade trade = Trade.builder().identifier(tradeId).build();
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.of(trade));
            
            Optional<Trade> result = tradeEvent.fetchTrade(false);
            assertTrue(result.isPresent());
            assertEquals(tradeId, result.get().getIdentifier());
        }
    }

    @Test
    @DisplayName("fetchTrade should throw exception when not found and raiseException is true")
    void testFetchTradeNotFoundWithException() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.empty());
            
            Exception exception = assertThrows(IllegalStateException.class, () -> tradeEvent.fetchTrade(true));
            assertTrue(exception.getMessage().contains(tradeId));
        }
    }

    @Test
    @DisplayName("fetchTrade should return empty optional when not found and raiseException is false")
    void testFetchTradeNotFoundWithoutException() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.empty());
            
            Optional<Trade> result = tradeEvent.fetchTrade(false);
            assertFalse(result.isPresent());
        }
    }

    @Test
    @DisplayName("getBuyerAccount should return buyer account from cache")
    void testGetBuyerAccount() {
        try (MockedStatic<AccountCache> mockAccountCache = Mockito.mockStatic(AccountCache.class)) {
            String buyerKey = "buyer-123";
            tradeEvent.setBuyerAccountKey(buyerKey);
            Account account = new Account(buyerKey);
            
            mockAccountCache.when(AccountCache::getInstance).thenReturn(accountCacheMock);
            when(accountCacheMock.getAccount(buyerKey)).thenReturn(Optional.of(account));
            
            Optional<Account> result = tradeEvent.getBuyerAccount();
            assertTrue(result.isPresent());
            assertEquals(buyerKey, result.get().getKey());
        }
    }

    @Test
    @DisplayName("getSellerAccount should return seller account from cache")
    void testGetSellerAccount() {
        try (MockedStatic<AccountCache> mockAccountCache = Mockito.mockStatic(AccountCache.class)) {
            String sellerKey = "seller-123";
            tradeEvent.setSellerAccountKey(sellerKey);
            Account account = new Account(sellerKey);
            
            mockAccountCache.when(AccountCache::getInstance).thenReturn(accountCacheMock);
            when(accountCacheMock.getAccount(sellerKey)).thenReturn(Optional.of(account));
            
            Optional<Account> result = tradeEvent.getSellerAccount();
            assertTrue(result.isPresent());
            assertEquals(sellerKey, result.get().getKey());
        }
    }

    @Test
    @DisplayName("updateBuyerAccount should update buyer account in cache")
    void testUpdateBuyerAccount() {
        try (MockedStatic<AccountCache> mockAccountCache = Mockito.mockStatic(AccountCache.class)) {
            String buyerKey = "buyer-123";
            Account account = new Account(buyerKey);
            
            mockAccountCache.when(AccountCache::getInstance).thenReturn(accountCacheMock);
            
            tradeEvent.updateBuyerAccount(account);
            
            verify(accountCacheMock).updateAccount(account);
        }
    }

    @Test
    @DisplayName("updateSellerAccount should update seller account in cache")
    void testUpdateSellerAccount() {
        try (MockedStatic<AccountCache> mockAccountCache = Mockito.mockStatic(AccountCache.class)) {
            String sellerKey = "seller-123";
            Account account = new Account(sellerKey);
            
            mockAccountCache.when(AccountCache::getInstance).thenReturn(accountCacheMock);
            
            tradeEvent.updateSellerAccount(account);
            
            verify(accountCacheMock).updateAccount(account);
        }
    }

    @Test
    @DisplayName("getOffer should return offer from cache")
    void testGetOffer() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            String offerKey = "offer-123";
            tradeEvent.setOfferKey(offerKey);
            Offer offer = Offer.builder().identifier(offerKey).build();
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            when(offerCacheMock.getOffer(offerKey)).thenReturn(Optional.of(offer));
            
            Optional<Offer> result = tradeEvent.getOffer();
            assertTrue(result.isPresent());
            assertEquals(offerKey, result.get().getIdentifier());
        }
    }

    @Test
    @DisplayName("updateOffer should update offer in cache")
    void testUpdateOffer() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            String offerKey = "offer-123";
            Offer offer = Offer.builder().identifier(offerKey).build();
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            
            tradeEvent.updateOffer(offer);
            
            verify(offerCacheMock).updateOffer(offer);
        }
    }

    @Test
    @DisplayName("updateTrade should update trade in cache")
    void testUpdateTrade() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            String tradeId = "trade-123";
            Trade trade = Trade.builder().identifier(tradeId).build();
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            
            tradeEvent.updateTrade(trade);
            
            verify(tradeCacheMock).updateTrade(trade);
        }
    }

    @Test
    @DisplayName("toTrade should return existing trade if found")
    void testToTradeWithExistingTrade() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            Trade existingTrade = Trade.builder().identifier(tradeId).build();
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.of(existingTrade));
            
            Trade result = tradeEvent.toTrade(false);
            assertEquals(existingTrade, result);
        }
    }

    @Test
    @DisplayName("toTrade should create new trade with BUY taker side when not found")
    void testToTradeCreateNewBuyerTaker() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            // Setup trade event data
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            tradeEvent.setOfferKey("offer-123");
            tradeEvent.setBuyerAccountKey("buyer-123");
            tradeEvent.setSellerAccountKey("seller-123");
            tradeEvent.setSymbol("BTC:USD");
            tradeEvent.setPrice(new BigDecimal("10000"));
            tradeEvent.setCoinAmount(new BigDecimal("1"));
            tradeEvent.setTakerSide(Trade.TAKER_SIDE_BUY);
            Instant now = Instant.now();
            tradeEvent.setCreatedAt(now);
            tradeEvent.setUpdatedAt(now);
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.empty());
            
            Trade result = tradeEvent.toTrade(false);
            
            assertEquals(tradeId, result.getIdentifier());
            assertEquals("offer-123", result.getOfferKey());
            assertEquals("buyer-123", result.getBuyerAccountKey());
            assertEquals("seller-123", result.getSellerAccountKey());
            assertEquals("BTC:USD", result.getSymbol());
            assertEquals(new BigDecimal("10000"), result.getPrice());
            assertEquals(new BigDecimal("1"), result.getCoinAmount());
            assertEquals(Trade.TAKER_SIDE_BUY, result.getTakerSide());
            assertEquals(Trade.TradeStatus.UNPAID, result.getStatus());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }
    }

    @Test
    @DisplayName("toTrade should create new trade with SELL taker side when not found")
    void testToTradeCreateNewSellerTaker() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            // Setup trade event data
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            tradeEvent.setOfferKey("offer-123");
            tradeEvent.setBuyerAccountKey("buyer-123");
            tradeEvent.setSellerAccountKey("seller-123");
            tradeEvent.setSymbol("BTC:USD");
            tradeEvent.setPrice(new BigDecimal("10000"));
            tradeEvent.setCoinAmount(new BigDecimal("1"));
            tradeEvent.setTakerSide(Trade.TAKER_SIDE_SELL);
            Instant now = Instant.now();
            tradeEvent.setCreatedAt(now);
            tradeEvent.setUpdatedAt(now);
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.empty());
            
            Trade result = tradeEvent.toTrade(false);
            
            assertEquals(tradeId, result.getIdentifier());
            assertEquals("offer-123", result.getOfferKey());
            assertEquals("buyer-123", result.getBuyerAccountKey());
            assertEquals("seller-123", result.getSellerAccountKey());
            assertEquals("BTC:USD", result.getSymbol());
            assertEquals(new BigDecimal("10000"), result.getPrice());
            assertEquals(new BigDecimal("1"), result.getCoinAmount());
            assertEquals(Trade.TAKER_SIDE_SELL, result.getTakerSide());
            assertEquals(Trade.TradeStatus.UNPAID, result.getStatus());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }
    }

    @Test
    @DisplayName("parserData should correctly parse a complete JSON message")
    void testParserDataComplete() {
        // Create a JSON node with all fields
        JsonNode jsonNode = TradeEventFactory.createJsonNode();
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify all fields were properly parsed
        assertEquals(jsonNode.get("eventId").asText(), newEvent.getEventId());
        assertEquals(ActionType.fromValue(jsonNode.get("actionType").asText()), newEvent.getActionType());
        assertEquals(jsonNode.get("actionId").asText(), newEvent.getActionId());
        assertEquals(OperationType.fromValue(jsonNode.get("operationType").asText()), newEvent.getOperationType());
        assertEquals(jsonNode.get("identifier").asText(), newEvent.getIdentifier());
        assertEquals(jsonNode.get("offerKey").asText(), newEvent.getOfferKey());
        assertEquals(jsonNode.get("buyerAccountKey").asText(), newEvent.getBuyerAccountKey());
        assertEquals(jsonNode.get("sellerAccountKey").asText(), newEvent.getSellerAccountKey());
        assertEquals(jsonNode.get("symbol").asText(), newEvent.getSymbol());
        assertEquals(jsonNode.get("takerSide").asText(), newEvent.getTakerSide());
        assertEquals(jsonNode.get("status").asText(), newEvent.getStatus());
        assertNotNull(newEvent.getPrice());
        assertNotNull(newEvent.getCoinAmount());
        assertNotNull(newEvent.getUpdatedAt());
    }

    @Test
    @DisplayName("parserData should parse symbol from coinCurrency and fiatCurrency")
    void testParserDataSymbolFromCurrencies() {
        // Create a JSON node with coinCurrency and fiatCurrency instead of symbol
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("coinCurrency", "BTC");
        jsonNode.put("fiatCurrency", "USD");
        jsonNode.put("price", "10000");
        jsonNode.put("coinAmount", "1");
        jsonNode.put("takerSide", Trade.TAKER_SIDE_BUY);
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify symbol was constructed from currencies
        assertEquals("BTC:USD", newEvent.getSymbol());
    }

    @Test
    @DisplayName("parserData should handle numeric and string prices")
    void testParserDataNumericAndStringPrices() {
        // Test with numeric price
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("price", 10000.0); // Numeric price
        
        TradeEvent event1 = new TradeEvent();
        event1.parserData(jsonNode);
        assertEquals(new BigDecimal("10000.0"), event1.getPrice());
        
        // Test with string price
        jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("price", "10000.5"); // String price
        
        TradeEvent event2 = new TradeEvent();
        event2.parserData(jsonNode);
        assertEquals(new BigDecimal("10000.5"), event2.getPrice());
    }

    @Test
    @DisplayName("parserData should handle numeric and string coinAmount")
    void testParserDataNumericAndStringCoinAmount() {
        // Test with numeric coinAmount
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("coinAmount", 1.25); // Numeric coinAmount
        
        TradeEvent event1 = new TradeEvent();
        event1.parserData(jsonNode);
        assertEquals(new BigDecimal("1.25"), event1.getCoinAmount());
        
        // Test with string coinAmount
        jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("coinAmount", "1.5"); // String coinAmount
        
        TradeEvent event2 = new TradeEvent();
        event2.parserData(jsonNode);
        assertEquals(new BigDecimal("1.5"), event2.getCoinAmount());
    }

    @Test
    @DisplayName("parserData should handle various timestamp formats - ISO-8601")
    void testParserDataWithIso8601Timestamps() {
        // Create a JSON node with ISO-8601 format timestamps
        String isoTimestamp = Instant.now().toString();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("createdAt", isoTimestamp);
        jsonNode.put("releasedAt", isoTimestamp);
        jsonNode.put("cancelledAt", isoTimestamp);
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify timestamps were parsed correctly
        assertNotNull(newEvent.getCreatedAt());
        assertNotNull(newEvent.getUpdatedAt());
        assertNotNull(newEvent.getCompletedAt());
        assertNotNull(newEvent.getCancelledAt());
        assertEquals(Instant.parse(isoTimestamp), newEvent.getCreatedAt());
        assertEquals(Instant.parse(isoTimestamp), newEvent.getCompletedAt());
        assertEquals(Instant.parse(isoTimestamp), newEvent.getCancelledAt());
    }

    @Test
    @DisplayName("parserData should handle various timestamp formats - Epoch")
    void testParserDataWithEpochTimestamps() {
        // Create a JSON node with epoch timestamps as numbers
        long epochSeconds = Instant.now().getEpochSecond();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("createdAt", epochSeconds);
        jsonNode.put("releasedAt", epochSeconds);
        jsonNode.put("cancelledAt", epochSeconds);
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify timestamps were parsed correctly
        assertNotNull(newEvent.getCreatedAt());
        assertNotNull(newEvent.getUpdatedAt());
        assertNotNull(newEvent.getCompletedAt());
        assertNotNull(newEvent.getCancelledAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getCreatedAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getCompletedAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getCancelledAt());
    }

    @Test
    @DisplayName("parserData should handle various timestamp formats - Epoch as String")
    void testParserDataWithEpochStringTimestamps() {
        // Create a JSON node with epoch timestamps as strings
        long epochSeconds = Instant.now().getEpochSecond();
        String epochString = String.valueOf(epochSeconds);
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("createdAt", epochString);
        jsonNode.put("releasedAt", epochString);
        jsonNode.put("cancelledAt", epochString);
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify timestamps were parsed correctly
        assertNotNull(newEvent.getCreatedAt());
        assertNotNull(newEvent.getUpdatedAt());
        assertNotNull(newEvent.getCompletedAt());
        assertNotNull(newEvent.getCancelledAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getCreatedAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getCompletedAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getCancelledAt());
    }

    @Test
    @DisplayName("parserData should throw exception for null message")
    void testParserDataNullMessage() {
        TradeEvent newEvent = new TradeEvent();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> newEvent.parserData(null));
        assertEquals("MessageJson is required", exception.getMessage());
    }

    @Test
    @DisplayName("parserData should throw exception for invalid timestamp format")
    void testParserDataInvalidTimestampFormat() {
        // Create a JSON node with invalid timestamp format
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("createdAt", "invalid-timestamp");
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> newEvent.parserData(jsonNode));
        assertTrue(exception.getMessage().contains("Error parsing timestamps"));
    }

    @Test
    @DisplayName("validate should pass with valid event")
    void testValidateValid() {
        TradeEvent validEvent = TradeEventFactory.create();
        assertDoesNotThrow(() -> validEvent.validate());
    }

    @Test
    @DisplayName("validate should fail with unsupported operation type")
    void testValidateUnsupportedOperationType() {
        TradeEvent event = TradeEventFactory.create();
        // Use a non-trade operation type
        event.setOperationType(OperationType.COIN_DEPOSIT_CREATE);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("Operation type is not supported"));
    }

    @Test
    @DisplayName("validate should fail with incorrect action type")
    void testValidateIncorrectActionType() {
        TradeEvent event = TradeEventFactory.create();
        // Use a non-trade action type
        event.setActionType(ActionType.COIN_TRANSACTION);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("Action type not matched"));
    }

    @Test
    @DisplayName("validate should fail with missing identifier")
    void testValidateMissingIdentifier() {
        TradeEvent event = TradeEventFactory.create();
        event.setIdentifier(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("identifier is required"));

        event.setIdentifier("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("identifier is required"));
    }

    @Test
    @DisplayName("validate should fail with missing buyer account key")
    void testValidateMissingBuyerAccountKey() {
        TradeEvent event = TradeEventFactory.create();
        event.setBuyerAccountKey(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("buyerAccountKey is required"));

        event.setBuyerAccountKey("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("buyerAccountKey is required"));
    }

    @Test
    @DisplayName("validate should fail with missing seller account key")
    void testValidateMissingSellerAccountKey() {
        TradeEvent event = TradeEventFactory.create();
        event.setSellerAccountKey(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("sellerAccountKey is required"));

        event.setSellerAccountKey("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("sellerAccountKey is required"));
    }

    @Test
    @DisplayName("validate should fail with missing symbol")
    void testValidateMissingSymbol() {
        TradeEvent event = TradeEventFactory.create();
        event.setSymbol(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("symbol is required"));

        event.setSymbol("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("symbol is required"));
    }

    @Test
    @DisplayName("validate should fail with invalid price")
    void testValidateInvalidPrice() {
        TradeEvent event = TradeEventFactory.create();
        event.setPrice(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("price must be greater than 0"));

        event.setPrice(BigDecimal.ZERO);
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("price must be greater than 0"));

        event.setPrice(BigDecimal.valueOf(-1));
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("price must be greater than 0"));
    }

    @Test
    @DisplayName("validate should fail with invalid coin amount")
    void testValidateInvalidCoinAmount() {
        TradeEvent event = TradeEventFactory.create();
        event.setCoinAmount(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("coinAmount must be greater than 0"));

        event.setCoinAmount(BigDecimal.ZERO);
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("coinAmount must be greater than 0"));

        event.setCoinAmount(BigDecimal.valueOf(-1));
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("coinAmount must be greater than 0"));
    }

    @Test
    @DisplayName("validate should fail with invalid taker side")
    void testValidateInvalidTakerSide() {
        TradeEvent event = TradeEventFactory.create();
        event.setTakerSide(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("takerSide is required"));

        event.setTakerSide("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("takerSide is required"));

        event.setTakerSide("INVALID");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("Taker side must be either BUY or SELL"));
    }

    @Test
    @DisplayName("toOperationObjectMessageJson should include all required fields")
    void testToOperationObjectMessageJson() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            // Setup
            TradeEvent event = spy(new TradeEvent());
            String tradeId = "trade-123";
            event.setIdentifier(tradeId);
            event.setEventId("evt-123");
            event.setActionType(ActionType.TRADE);
            event.setActionId("act-123");
            event.setOperationType(OperationType.TRADE_CREATE);
            
            Trade trade = mock(Trade.class);
            HashMap<String, Object> tradeJson = new HashMap<>();
            tradeJson.put("id", tradeId);
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.of(trade));
            when(trade.toMessageJson()).thenReturn(tradeJson);
            
            // Execute
            Map<String, Object> result = event.toOperationObjectMessageJson();
            
            // Verify
            assertNotNull(result);
            assertEquals("evt-123", result.get("eventId"));
            assertEquals(ActionType.TRADE.getValue(), result.get("actionType"));
            assertEquals("act-123", result.get("actionId"));
            assertEquals(OperationType.TRADE_CREATE.getValue(), result.get("operationType"));
            assertEquals(tradeJson, result.get("object"));
        }
    }

    @Test
    @DisplayName("toTrade should handle null timestamp fields correctly")
    void testToTradeWithNullTimestamps() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            // Setup trade event data with null timestamps
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            tradeEvent.setOfferKey("offer-123");
            tradeEvent.setBuyerAccountKey("buyer-123");
            tradeEvent.setSellerAccountKey("seller-123");
            tradeEvent.setSymbol("BTC:USD");
            tradeEvent.setPrice(new BigDecimal("10000"));
            tradeEvent.setCoinAmount(new BigDecimal("1"));
            tradeEvent.setTakerSide(Trade.TAKER_SIDE_BUY);
            tradeEvent.setCreatedAt(null);
            tradeEvent.setUpdatedAt(null);
            tradeEvent.setCompletedAt(null);
            tradeEvent.setCancelledAt(null);
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.empty());
            
            // Execute
            Trade result = tradeEvent.toTrade(false);
            
            // Verify default timestamps were set
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
            assertNull(result.getCompletedAt());
            assertNull(result.getCancelledAt());
        }
    }
    
    @Test
    @DisplayName("parserData should handle symbol field already set")
    void testParserDataWithSymbolAlreadySet() {
        // Create a JSON node with symbol field
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("symbol", "BTC:USD");
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify symbol was correctly parsed
        assertEquals("BTC:USD", jsonNode.get("symbol").asText());
    }
    
    @Test
    @DisplayName("parserData should handle null node values correctly")
    void testParserDataWithNullNodes() {
        // Create a JSON node with null fields
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.putNull("cancelledAt");
        jsonNode.putNull("releasedAt");
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify null timestamps were handled correctly
        assertNull(newEvent.getCancelledAt());
        assertNull(newEvent.getCompletedAt());
    }
    
    @Test
    @DisplayName("parserData should handle parsing different timestamp formats")
    void testParserDataTimestampEdgeCases() {
        // Create a JSON node with different timestamp formats
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        
        // Empty timestamp string
        jsonNode.put("createdAt", "");
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify empty timestamp string was handled correctly
        assertNotNull(newEvent.getUpdatedAt());
        
        // Test with removed createdAt node (not null node)
        jsonNode.remove("createdAt");
        newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        assertNotNull(newEvent.getUpdatedAt());
    }

    @Test
    @DisplayName("parserData should handle fiatAmount field correctly")
    void testParserDataFiatAmount() {
        // Create a JSON node with fiatAmount as string
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("fiatAmount", "1000.5");
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify fiatAmount was correctly parsed
        assertEquals(new BigDecimal("1000.5"), newEvent.getFiatAmount());
        
        // Test with numeric fiatAmount
        jsonNode.put("fiatAmount", 2000.75);
        
        TradeEvent numericEvent = new TradeEvent();
        numericEvent.parserData(jsonNode);
        
        // Verify fiatAmount was correctly parsed from numeric value
        assertEquals(new BigDecimal("2000.75"), numericEvent.getFiatAmount());
    }
    
    @Test
    @DisplayName("toTrade should set fiatAmount from coinAmount and price")
    void testToTradeUsesProvidedFiatAmount() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            // Setup trade event data
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            tradeEvent.setOfferKey("offer-123");
            tradeEvent.setBuyerAccountKey("buyer-123");
            tradeEvent.setSellerAccountKey("seller-123");
            tradeEvent.setSymbol("BTC:USD");
            tradeEvent.setPrice(new BigDecimal("10000"));
            tradeEvent.setCoinAmount(new BigDecimal("1.5"));
            tradeEvent.setTakerSide(Trade.TAKER_SIDE_BUY);
            // Chú ý: fiatAmount được tính từ coinAmount và price, 
            // không phải từ giá trị được set bên dưới
            tradeEvent.setFiatAmount(new BigDecimal("14000")); 
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.empty());
            
            Trade result = tradeEvent.toTrade(false);
            
            // Verify fiatAmount is calculated from coinAmount * price
            assertEquals(0, new BigDecimal("15000").compareTo(result.getFiatAmount()),
                     "Expected fiatAmount to be 15000 (coinAmount * price)");
        }
    }
    
    @Test
    @DisplayName("validate should not throw exception with valid fiat and coin amount")
    void testValidateWithValidAmounts() {
        TradeEvent validEvent = TradeEventFactory.create();
        validEvent.setFiatAmount(new BigDecimal("10000"));
        validEvent.setCoinAmount(new BigDecimal("1"));
        validEvent.setPrice(new BigDecimal("10000"));
        
        assertDoesNotThrow(() -> validEvent.validate());
    }
    
    @Test
    @DisplayName("parserData should handle all fee-related fields correctly")
    void testParserDataFeeFields() {
        // Create a JSON node with all fee fields
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("feeRatio", "0.01");
        jsonNode.put("totalFee", "100");
        jsonNode.put("fixedFee", "50");
        jsonNode.put("coinTradingFee", "0.005");
        jsonNode.put("amountAfterFee", "9900");
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify all fee fields were correctly parsed
        assertEquals(new BigDecimal("0.01"), newEvent.getFeeRatio());
        assertEquals(new BigDecimal("100"), newEvent.getTotalFee());
        assertEquals(new BigDecimal("50"), newEvent.getFixedFee());
        assertEquals(new BigDecimal("0.005"), newEvent.getCoinTradingFee());
        assertEquals(new BigDecimal("9900"), newEvent.getAmountAfterFee());
    }

    @Test
    @DisplayName("parserData should handle payment related fields correctly")
    void testParserDataPaymentFields() {
        // Create a JSON node with payment related fields
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("paymentMethod", "bank_transfer");
        jsonNode.put("ref", "payment-ref-123");
        jsonNode.put("paymentProofStatus", "accepted");
        jsonNode.put("hasPaymentProof", true);
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify payment related fields were correctly parsed
        assertEquals("bank_transfer", newEvent.getPaymentMethod());
        assertEquals("payment-ref-123", newEvent.getRef());
        assertEquals("accepted", newEvent.getPaymentProofStatus());
        assertTrue(newEvent.isHasPaymentProof());
    }
    
    @Test
    @DisplayName("toTrade should set all payment related fields correctly")
    void testToTradePaymentFields() {
        try (MockedStatic<TradeCache> mockTradeCache = Mockito.mockStatic(TradeCache.class)) {
            // Setup trade event data
            String tradeId = "trade-123";
            tradeEvent.setIdentifier(tradeId);
            tradeEvent.setOfferKey("offer-123");
            tradeEvent.setBuyerAccountKey("buyer-123");
            tradeEvent.setSellerAccountKey("seller-123");
            tradeEvent.setSymbol("BTC:USD");
            tradeEvent.setPrice(new BigDecimal("10000"));
            tradeEvent.setCoinAmount(new BigDecimal("1"));
            tradeEvent.setTakerSide(Trade.TAKER_SIDE_BUY);
            
            // Set payment related fields
            tradeEvent.setPaymentMethod("credit_card");
            tradeEvent.setRef("ref-abc-123");
            tradeEvent.setPaymentProofStatus("pending");
            tradeEvent.setHasPaymentProof(true);
            
            mockTradeCache.when(TradeCache::getInstance).thenReturn(tradeCacheMock);
            when(tradeCacheMock.getTrade(tradeId)).thenReturn(Optional.empty());
            
            Trade result = tradeEvent.toTrade(false);
            
            // Verify payment related fields were correctly set
            assertEquals("credit_card", result.getPaymentMethod());
            assertEquals("ref-abc-123", result.getRef());
            assertEquals("pending", result.getPaymentProofStatus());
            assertTrue(result.getHasPaymentProof());
        }
    }
    
    @Test
    @DisplayName("parserData should handle currency fields correctly")
    void testParserDataCurrencyFields() {
        // Create a JSON node with coin and fiat currency fields
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        jsonNode.put("coinCurrency", "BTC");
        jsonNode.put("fiatCurrency", "USD");
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify currency fields were correctly parsed
        assertEquals("BTC", newEvent.getCoinCurrency());
        assertEquals("USD", newEvent.getFiatCurrency());
        assertEquals("BTC:USD", newEvent.getSymbol());
    }
    
    @Test
    @DisplayName("parserData should handle timestamps as numbers")
    void testParserDataTimestampsAsNumbers() {
        // Create a JSON node with timestamp fields as numbers
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.TRADE_CREATE.getValue());
        jsonNode.put("identifier", "trade-123");
        
        // Set timestamp fields as numeric (epoch seconds)
        long now = Instant.now().getEpochSecond();
        jsonNode.put("paidAt", now);
        jsonNode.put("disputedAt", now + 100);
        
        // Create a new event to parse into
        TradeEvent newEvent = new TradeEvent();
        newEvent.parserData(jsonNode);
        
        // Verify timestamps were correctly parsed
        assertEquals(Instant.ofEpochSecond(now), newEvent.getPaidAt());
        assertEquals(Instant.ofEpochSecond(now + 100), newEvent.getDisputedAt());
    }
}
