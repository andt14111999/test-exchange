package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.event.OfferEventFactory;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.Offer;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.OfferCache;
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
class OfferEventTest {
    private OfferEvent offerEvent;
    private OfferCache offerCacheMock;
    private AccountCache accountCacheMock;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        offerEvent = OfferEventFactory.create();
        offerCacheMock = mock(OfferCache.class);
        accountCacheMock = mock(AccountCache.class);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        // Reset static mocks after each test
        offerCacheMock = null;
        accountCacheMock = null;
    }

    @Test
    @DisplayName("getOfferCache should return singleton instance")
    void testGetOfferCache() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            assertEquals(offerCacheMock, offerEvent.getOfferCache());
        }
    }

    @Test
    @DisplayName("getAccountCache should return singleton instance")
    void testGetAccountCache() {
        try (MockedStatic<AccountCache> mockAccountCache = Mockito.mockStatic(AccountCache.class)) {
            mockAccountCache.when(AccountCache::getInstance).thenReturn(accountCacheMock);
            assertEquals(accountCacheMock, offerEvent.getAccountCache());
        }
    }

    @Test
    @DisplayName("getProducerEventId should return identifier")
    void testGetProducerEventId() {
        String identifier = "offer-123";
        offerEvent.setIdentifier(identifier);
        assertEquals(identifier, offerEvent.getProducerEventId());
    }

    @Test
    @DisplayName("getEventHandler should return OFFER_EVENT")
    void testGetEventHandler() {
        assertEquals(EventHandlerAction.OFFER_EVENT, offerEvent.getEventHandler());
    }

    @Test
    @DisplayName("fetchOffer should return offer from cache")
    void testFetchOfferFromCache() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            String offerId = "offer-123";
            offerEvent.setIdentifier(offerId);
            Offer offer = Offer.builder().identifier(offerId).build();
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            when(offerCacheMock.getOffer(offerId)).thenReturn(Optional.of(offer));
            
            Optional<Offer> result = offerEvent.fetchOffer(false);
            assertTrue(result.isPresent());
            assertEquals(offerId, result.get().getIdentifier());
        }
    }

    @Test
    @DisplayName("fetchOffer should throw exception when not found and raiseException is true")
    void testFetchOfferNotFoundWithException() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            String offerId = "offer-123";
            offerEvent.setIdentifier(offerId);
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            when(offerCacheMock.getOffer(offerId)).thenReturn(Optional.empty());
            
            Exception exception = assertThrows(IllegalStateException.class, () -> offerEvent.fetchOffer(true));
            assertTrue(exception.getMessage().contains(offerId));
        }
    }

    @Test
    @DisplayName("fetchOffer should return empty optional when not found and raiseException is false")
    void testFetchOfferNotFoundWithoutException() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            String offerId = "offer-123";
            offerEvent.setIdentifier(offerId);
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            when(offerCacheMock.getOffer(offerId)).thenReturn(Optional.empty());
            
            Optional<Offer> result = offerEvent.fetchOffer(false);
            assertFalse(result.isPresent());
        }
    }

    @Test
    @DisplayName("getUserAccount should return user account from cache")
    void testGetUserAccount() {
        try (MockedStatic<AccountCache> mockAccountCache = Mockito.mockStatic(AccountCache.class)) {
            String userId = "user-123";
            offerEvent.setUserId(userId);
            Account account = new Account(userId);
            
            mockAccountCache.when(AccountCache::getInstance).thenReturn(accountCacheMock);
            when(accountCacheMock.getAccount(userId)).thenReturn(Optional.of(account));
            
            Optional<Account> result = offerEvent.getUserAccount();
            assertTrue(result.isPresent());
            assertEquals(userId, result.get().getKey());
        }
    }

    @Test
    @DisplayName("updateAccount should update account in cache")
    void testUpdateAccount() {
        try (MockedStatic<AccountCache> mockAccountCache = Mockito.mockStatic(AccountCache.class)) {
            String userId = "user-123";
            Account account = new Account(userId);
            
            mockAccountCache.when(AccountCache::getInstance).thenReturn(accountCacheMock);
            
            offerEvent.updateAccount(account);
            
            verify(accountCacheMock).updateAccount(account);
        }
    }

    @Test
    @DisplayName("updateOffer should update offer in cache")
    void testUpdateOffer() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            String offerId = "offer-123";
            Offer offer = Offer.builder().identifier(offerId).build();
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            
            offerEvent.updateOffer(offer);
            
            verify(offerCacheMock).updateOffer(offer);
        }
    }

    @Test
    @DisplayName("toOffer should return existing offer if found")
    void testToOfferWithExistingOffer() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            String offerId = "offer-123";
            offerEvent.setIdentifier(offerId);
            Offer existingOffer = Offer.builder().identifier(offerId).build();
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            when(offerCacheMock.getOffer(offerId)).thenReturn(Optional.of(existingOffer));
            
            Offer result = offerEvent.toOffer(false);
            assertEquals(existingOffer, result);
        }
    }

    @Test
    @DisplayName("toOffer should create new offer when not found")
    void testToOfferCreateNewOffer() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            // Setup offer event data
            String offerId = "offer-123";
            offerEvent.setIdentifier(offerId);
            offerEvent.setUserId("user-123");
            offerEvent.setCoinCurrency("btc");
            offerEvent.setCurrency("usd");
            offerEvent.setOfferType(Offer.OfferType.BUY.name());
            offerEvent.setPrice(new BigDecimal("10000"));
            offerEvent.setTotalAmount(new BigDecimal("1"));
            offerEvent.setMinAmount(new BigDecimal("0.1"));
            offerEvent.setMaxAmount(new BigDecimal("1"));
            offerEvent.setAvailableAmount(new BigDecimal("0.5"));
            offerEvent.setDisabled(false);
            offerEvent.setDeleted(false);
            offerEvent.setAutomatic(true);
            offerEvent.setOnline(true);
            offerEvent.setMargin(new BigDecimal("1.5"));
            offerEvent.setPaymentMethodId("pm-123");
            offerEvent.setPaymentTime(30);
            offerEvent.setCountryCode("US");
            Instant now = Instant.now();
            offerEvent.setCreatedAt(now);
            offerEvent.setUpdatedAt(now);
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            when(offerCacheMock.getOffer(offerId)).thenReturn(Optional.empty());
            
            Offer result = offerEvent.toOffer(false);
            
            assertEquals(offerId, result.getIdentifier());
            assertEquals("user-123", result.getUserId());
            assertEquals("btc:usd", result.getSymbol());
            assertEquals(Offer.OfferType.BUY, result.getType());
            assertEquals(new BigDecimal("10000"), result.getPrice());
            assertEquals(new BigDecimal("1"), result.getTotalAmount());
            assertEquals(new BigDecimal("0.1"), result.getMinAmount());
            assertEquals(new BigDecimal("1"), result.getMaxAmount());
            assertEquals(new BigDecimal("0.5"), result.getAvailableAmount());
            assertEquals(Offer.OfferStatus.PENDING, result.getStatus());
            assertFalse(result.getDisabled());
            assertFalse(result.getDeleted());
            assertTrue(result.getAutomatic());
            assertTrue(result.getOnline());
            assertEquals(new BigDecimal("1.5"), result.getMargin());
            assertEquals("pm-123", result.getPaymentMethodId());
            assertEquals(Integer.valueOf(30), result.getPaymentTime());
            assertEquals("US", result.getCountryCode());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }
    }

    @Test
    @DisplayName("toOffer should handle null timestamp fields correctly")
    void testToOfferWithNullTimestamps() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            // Setup offer event data with null timestamps
            String offerId = "offer-123";
            offerEvent.setIdentifier(offerId);
            offerEvent.setUserId("user-123");
            offerEvent.setCoinCurrency("btc");
            offerEvent.setCurrency("usd");
            offerEvent.setOfferType(Offer.OfferType.BUY.name());
            offerEvent.setPrice(new BigDecimal("10000"));
            offerEvent.setTotalAmount(new BigDecimal("1"));
            offerEvent.setCreatedAt(null);
            offerEvent.setUpdatedAt(null);
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            when(offerCacheMock.getOffer(offerId)).thenReturn(Optional.empty());
            
            // Execute
            Offer result = offerEvent.toOffer(false);
            
            // Verify default timestamps were set
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }
    }

    @Test
    @DisplayName("parserData should correctly parse a complete JSON message")
    void testParserDataComplete() {
        // Create a JSON node with all fields
        JsonNode jsonNode = OfferEventFactory.createJsonNode();
        
        // Create a new event to parse into
        OfferEvent newEvent = new OfferEvent();
        newEvent.parserData(jsonNode);
        
        // Verify all fields were properly parsed
        assertEquals(jsonNode.get("eventId").asText(), newEvent.getEventId());
        assertEquals(ActionType.fromValue(jsonNode.get("actionType").asText()), newEvent.getActionType());
        assertEquals(jsonNode.get("actionId").asText(), newEvent.getActionId());
        assertEquals(OperationType.fromValue(jsonNode.get("operationType").asText()), newEvent.getOperationType());
        assertEquals(jsonNode.get("identifier").asText(), newEvent.getIdentifier());
        assertEquals(jsonNode.get("userId").asText(), newEvent.getUserId());
        assertEquals(jsonNode.get("offerType").asText(), newEvent.getOfferType());
        assertEquals(jsonNode.get("coinCurrency").asText(), newEvent.getCoinCurrency());
        assertEquals(jsonNode.get("currency").asText(), newEvent.getCurrency());
        assertNotNull(newEvent.getPrice());
        assertNotNull(newEvent.getMinAmount());
        assertNotNull(newEvent.getMaxAmount());
        assertNotNull(newEvent.getTotalAmount());
        assertNotNull(newEvent.getAvailableAmount());
        assertEquals(jsonNode.get("paymentMethodId").asText(), newEvent.getPaymentMethodId());
        assertEquals(jsonNode.get("paymentTime").asInt(), newEvent.getPaymentTime().intValue());
        assertEquals(jsonNode.get("countryCode").asText(), newEvent.getCountryCode());
        assertEquals(jsonNode.get("disabled").asBoolean(), newEvent.getDisabled());
        assertEquals(jsonNode.get("deleted").asBoolean(), newEvent.getDeleted());
        assertEquals(jsonNode.get("automatic").asBoolean(), newEvent.getAutomatic());
        assertEquals(jsonNode.get("online").asBoolean(), newEvent.getOnline());
        assertNotNull(newEvent.getMargin());
        assertNotNull(newEvent.getUpdatedAt());
    }

    @Test
    @DisplayName("parserData should handle numeric and string prices")
    void testParserDataNumericAndStringPrices() {
        // Test with numeric price
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        jsonNode.put("price", 10000.0); // Numeric price
        
        OfferEvent event1 = new OfferEvent();
        event1.parserData(jsonNode);
        assertEquals(new BigDecimal("10000.0"), event1.getPrice());
        
        // Test with string price
        jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        jsonNode.put("price", "10000.5"); // String price
        
        OfferEvent event2 = new OfferEvent();
        event2.parserData(jsonNode);
        assertEquals(new BigDecimal("10000.5"), event2.getPrice());
    }

    @Test
    @DisplayName("parserData should handle numeric and string amounts")
    void testParserDataNumericAndStringAmounts() {
        // Test with numeric amounts
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        jsonNode.put("minAmount", 0.01); // Numeric amount
        jsonNode.put("maxAmount", 1.0); // Numeric amount
        jsonNode.put("totalAmount", 1.0); // Numeric amount
        jsonNode.put("availableAmount", 0.5); // Numeric amount
        jsonNode.put("margin", 1.5); // Numeric margin
        
        OfferEvent event1 = new OfferEvent();
        event1.parserData(jsonNode);
        assertEquals(new BigDecimal("0.01"), event1.getMinAmount());
        assertEquals(new BigDecimal("1.0"), event1.getMaxAmount());
        assertEquals(new BigDecimal("1.0"), event1.getTotalAmount());
        assertEquals(new BigDecimal("0.5"), event1.getAvailableAmount());
        assertEquals(new BigDecimal("1.5"), event1.getMargin());
        
        // Test with string amounts
        jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        jsonNode.put("minAmount", "0.02"); // String amount
        jsonNode.put("maxAmount", "2.0"); // String amount
        jsonNode.put("totalAmount", "2.0"); // String amount
        jsonNode.put("availableAmount", "1.0"); // String amount
        jsonNode.put("margin", "2.5"); // String margin
        
        OfferEvent event2 = new OfferEvent();
        event2.parserData(jsonNode);
        assertEquals(new BigDecimal("0.02"), event2.getMinAmount());
        assertEquals(new BigDecimal("2.0"), event2.getMaxAmount());
        assertEquals(new BigDecimal("2.0"), event2.getTotalAmount());
        assertEquals(new BigDecimal("1.0"), event2.getAvailableAmount());
        assertEquals(new BigDecimal("2.5"), event2.getMargin());
    }

    @Test
    @DisplayName("parserData should handle various timestamp formats - ISO-8601")
    void testParserDataWithIso8601Timestamps() {
        // Create a JSON node with ISO-8601 format timestamps
        String isoTimestamp = Instant.now().toString();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        jsonNode.put("createdAt", isoTimestamp);
        jsonNode.put("updatedAt", isoTimestamp);
        
        // Create a new event to parse into
        OfferEvent newEvent = new OfferEvent();
        newEvent.parserData(jsonNode);
        
        // Verify timestamps were parsed correctly
        assertNotNull(newEvent.getCreatedAt());
        assertNotNull(newEvent.getUpdatedAt());
        assertEquals(Instant.parse(isoTimestamp), newEvent.getCreatedAt());
        assertEquals(Instant.parse(isoTimestamp), newEvent.getUpdatedAt());
    }

    @Test
    @DisplayName("parserData should handle various timestamp formats - Epoch")
    void testParserDataWithEpochTimestamps() {
        // Create a JSON node with epoch timestamps as numbers
        long epochSeconds = Instant.now().getEpochSecond();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        jsonNode.put("createdAt", epochSeconds);
        jsonNode.put("updatedAt", epochSeconds);
        
        // Create a new event to parse into
        OfferEvent newEvent = new OfferEvent();
        newEvent.parserData(jsonNode);
        
        // Verify timestamps were parsed correctly
        assertNotNull(newEvent.getCreatedAt());
        assertNotNull(newEvent.getUpdatedAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getCreatedAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getUpdatedAt());
    }

    @Test
    @DisplayName("parserData should handle various timestamp formats - Epoch as String")
    void testParserDataWithEpochStringTimestamps() {
        // Create a JSON node with epoch timestamps as strings
        long epochSeconds = Instant.now().getEpochSecond();
        String epochString = String.valueOf(epochSeconds);
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        jsonNode.put("createdAt", epochString);
        jsonNode.put("updatedAt", epochString);
        
        // Create a new event to parse into
        OfferEvent newEvent = new OfferEvent();
        newEvent.parserData(jsonNode);
        
        // Verify timestamps were parsed correctly
        assertNotNull(newEvent.getCreatedAt());
        assertNotNull(newEvent.getUpdatedAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getCreatedAt());
        assertEquals(Instant.ofEpochSecond(epochSeconds), newEvent.getUpdatedAt());
    }

    @Test
    @DisplayName("parserData should handle parsing different timestamp formats")
    void testParserDataTimestampEdgeCases() {
        // Create a JSON node with different timestamp formats
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        
        // Empty timestamp string
        jsonNode.put("createdAt", "");
        jsonNode.put("updatedAt", Instant.now().toString()); // Thêm updatedAt để đảm bảo không null
        
        // Create a new event to parse into
        OfferEvent newEvent = new OfferEvent();
        newEvent.parserData(jsonNode);
        
        // Verify empty timestamp string was handled correctly
        assertNotNull(newEvent.getUpdatedAt());
        
        // Test with removed createdAt node (not null node)
        jsonNode.remove("createdAt");
        newEvent = new OfferEvent();
        newEvent.parserData(jsonNode);
        assertNotNull(newEvent.getUpdatedAt());
    }

    @Test
    @DisplayName("parserData should throw exception for null message")
    void testParserDataNullMessage() {
        OfferEvent newEvent = new OfferEvent();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> newEvent.parserData(null));
        assertEquals("MessageJson is required", exception.getMessage());
    }

    @Test
    @DisplayName("parserData should throw exception for invalid timestamp format")
    void testParserDataInvalidTimestampFormat() {
        // Create a JSON node with invalid timestamp format
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", "evt-123");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "act-123");
        jsonNode.put("operationType", OperationType.OFFER_CREATE.getValue());
        jsonNode.put("identifier", "offer-123");
        jsonNode.put("createdAt", "invalid-timestamp");
        
        // Create a new event to parse into
        OfferEvent newEvent = new OfferEvent();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> newEvent.parserData(jsonNode));
        assertTrue(exception.getMessage().contains("Error parsing timestamps"));
    }

    @Test
    @DisplayName("validate should pass with valid event")
    void testValidateValid() {
        OfferEvent validEvent = OfferEventFactory.create();
        assertDoesNotThrow(() -> validEvent.validate());
    }

    @Test
    @DisplayName("validate should fail with unsupported operation type")
    void testValidateUnsupportedOperationType() {
        OfferEvent event = OfferEventFactory.create();
        // Use a non-offer operation type
        event.setOperationType(OperationType.COIN_DEPOSIT_CREATE);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("Operation type is not supported"));
    }

    @Test
    @DisplayName("validate should fail with incorrect action type")
    void testValidateIncorrectActionType() {
        OfferEvent event = OfferEventFactory.create();
        // Use a non-offer action type
        event.setActionType(ActionType.COIN_TRANSACTION);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("Action type not matched"));
    }

    @Test
    @DisplayName("validate should fail with missing identifier")
    void testValidateMissingIdentifier() {
        OfferEvent event = OfferEventFactory.create();
        event.setIdentifier(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("identifier is required"));

        event.setIdentifier("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("identifier is required"));
    }

    @Test
    @DisplayName("validate should fail with missing userId")
    void testValidateMissingUserId() {
        OfferEvent event = OfferEventFactory.create();
        event.setUserId(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("userId is required"));

        event.setUserId("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("userId is required"));
    }

    @Test
    @DisplayName("validate should fail with missing offerType")
    void testValidateMissingOfferType() {
        OfferEvent event = OfferEventFactory.create();
        event.setOfferType(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("offerType is required"));

        event.setOfferType("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("offerType is required"));
    }

    @Test
    @DisplayName("validate should fail with missing coinCurrency")
    void testValidateMissingCoinCurrency() {
        OfferEvent event = OfferEventFactory.create();
        event.setCoinCurrency(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("coinCurrency is required"));

        event.setCoinCurrency("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("coinCurrency is required"));
    }

    @Test
    @DisplayName("validate should fail with missing currency")
    void testValidateMissingCurrency() {
        OfferEvent event = OfferEventFactory.create();
        event.setCurrency(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("currency is required"));

        event.setCurrency("  ");
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("currency is required"));
    }

    @Test
    @DisplayName("validate should fail with invalid price")
    void testValidateInvalidPrice() {
        OfferEvent event = OfferEventFactory.create();
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
    @DisplayName("validate should fail with invalid minAmount")
    void testValidateInvalidMinAmount() {
        OfferEvent event = OfferEventFactory.create();
        event.setMinAmount(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("minAmount cannot be negative"));

        event.setMinAmount(BigDecimal.valueOf(-1));
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("minAmount cannot be negative"));
    }

    @Test
    @DisplayName("validate should fail with invalid maxAmount")
    void testValidateInvalidMaxAmount() {
        OfferEvent event = OfferEventFactory.create();
        event.setMaxAmount(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("maxAmount must be greater than 0"));

        event.setMaxAmount(BigDecimal.ZERO);
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("maxAmount must be greater than 0"));

        event.setMaxAmount(BigDecimal.valueOf(-1));
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("maxAmount must be greater than 0"));
    }

    @Test
    @DisplayName("validate should fail with invalid totalAmount")
    void testValidateInvalidTotalAmount() {
        OfferEvent event = OfferEventFactory.create();
        event.setTotalAmount(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("totalAmount must be greater than 0"));

        event.setTotalAmount(BigDecimal.ZERO);
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("totalAmount must be greater than 0"));

        event.setTotalAmount(BigDecimal.valueOf(-1));
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("totalAmount must be greater than 0"));
    }

    @Test
    @DisplayName("validate should fail with invalid availableAmount")
    void testValidateInvalidAvailableAmount() {
        OfferEvent event = OfferEventFactory.create();
        event.setAvailableAmount(null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("availableAmount cannot be negative"));

        event.setAvailableAmount(BigDecimal.valueOf(-1));
        exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("availableAmount cannot be negative"));
    }

    @Test
    @DisplayName("validate should fail when availableAmount > totalAmount")
    void testValidateAvailableAmountGreaterThanTotalAmount() {
        OfferEvent event = OfferEventFactory.create();
        event.setTotalAmount(new BigDecimal("1"));
        event.setAvailableAmount(new BigDecimal("2"));
        Exception exception = assertThrows(IllegalArgumentException.class, () -> event.validate());
        assertTrue(exception.getMessage().contains("availableAmount cannot be greater than totalAmount"));
    }

    @Test
    @DisplayName("toOperationObjectMessageJson should include all required fields")
    void testToOperationObjectMessageJson() {
        try (MockedStatic<OfferCache> mockOfferCache = Mockito.mockStatic(OfferCache.class)) {
            // Setup
            OfferEvent event = spy(new OfferEvent());
            String offerId = "offer-123";
            event.setIdentifier(offerId);
            event.setEventId("evt-123");
            event.setActionType(ActionType.OFFER);
            event.setActionId("act-123");
            event.setOperationType(OperationType.OFFER_CREATE);
            
            Offer offer = mock(Offer.class);
            HashMap<String, Object> offerJson = new HashMap<>();
            offerJson.put("id", offerId);
            
            mockOfferCache.when(OfferCache::getInstance).thenReturn(offerCacheMock);
            when(offerCacheMock.getOffer(offerId)).thenReturn(Optional.of(offer));
            when(offer.toMessageJson()).thenReturn(offerJson);
            
            // Execute
            Map<String, Object> result = event.toOperationObjectMessageJson();
            
            // Verify
            assertNotNull(result);
            assertEquals("evt-123", result.get("eventId"));
            assertEquals(ActionType.OFFER.getValue(), result.get("actionType"));
            assertEquals("act-123", result.get("actionId"));
            assertEquals(OperationType.OFFER_CREATE.getValue(), result.get("operationType"));
            assertEquals(offerJson, result.get("object"));
        }
    }
} 