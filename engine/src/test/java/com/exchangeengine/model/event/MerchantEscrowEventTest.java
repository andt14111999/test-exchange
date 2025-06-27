package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.factory.event.MerchantEscrowEventFactory;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.MerchantEscrowCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class MerchantEscrowEventTest {

    @Mock(lenient = true)
    private MerchantEscrow merchantEscrow;

    @Mock(lenient = true)
    private MerchantEscrowCache merchantEscrowCache;

    @BeforeEach
    void setUp() {
        MerchantEscrowCache.setTestInstance(merchantEscrowCache);
    }

    @AfterEach
    void tearDown() {
        MerchantEscrowCache.setTestInstance(null);
    }

    @Test
    @DisplayName("Test create valid MerchantEscrowEvent")
    void testCreateValidMerchantEscrowEvent() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();

        // Then
        assertNotNull(event);
        assertEquals(ActionType.MERCHANT_ESCROW, event.getActionType());
        assertTrue(OperationType.MERCHANT_ESCROW_OPERATIONS.contains(event.getOperationType()));
        assertNotNull(event.getIdentifier());
        assertNotNull(event.getUsdtAccountKey());
        assertNotNull(event.getFiatAccountKey());
        assertNotNull(event.getUsdtAmount());
        assertNotNull(event.getFiatAmount());
        assertNotNull(event.getFiatCurrency());
        assertNotNull(event.getUserId());
        assertNotNull(event.getMerchantEscrowOperationId());
    }

    @Test
    @DisplayName("Test getProducerEventId returns identifier")
    void testGetProducerEventId() {
        // Given
        String identifier = "escrow-123";
        MerchantEscrowEvent event = MerchantEscrowEventFactory.withIdentifier(identifier);

        // When
        String producerEventId = event.getProducerEventId();

        // Then
        assertEquals(identifier, producerEventId);
    }

    @Test
    @DisplayName("Test getEventHandler returns MERCHANT_ESCROW_EVENT")
    void testGetEventHandler() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();

        // When
        String handler = event.getEventHandler();

        // Then
        assertEquals(EventHandlerAction.MERCHANT_ESCROW_EVENT, handler);
    }

    @Test
    @DisplayName("Test fetchMerchantEscrow when escrow exists")
    void testFetchMerchantEscrowWhenExists() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        String identifier = event.getIdentifier();

        // Configure mocks
        when(merchantEscrowCache.getMerchantEscrow(identifier)).thenReturn(Optional.of(merchantEscrow));

        // When
        Optional<MerchantEscrow> result = event.fetchMerchantEscrow(false);

        // Then
        assertTrue(result.isPresent());
        assertEquals(merchantEscrow, result.get());
        verify(merchantEscrowCache).getMerchantEscrow(identifier);
    }

    @Test
    @DisplayName("Test fetchMerchantEscrow when escrow does not exist and no exception")
    void testFetchMerchantEscrowWhenNotExistsNoException() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        String identifier = event.getIdentifier();

        // Configure mocks
        when(merchantEscrowCache.getMerchantEscrow(identifier)).thenReturn(Optional.empty());

        // When
        Optional<MerchantEscrow> result = event.fetchMerchantEscrow(false);

        // Then
        assertFalse(result.isPresent());
        verify(merchantEscrowCache).getMerchantEscrow(identifier);
    }

    @Test
    @DisplayName("Test fetchMerchantEscrow when escrow does not exist and raise exception")
    void testFetchMerchantEscrowWhenNotExistsRaiseException() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        String identifier = event.getIdentifier();

        // Configure mocks
        when(merchantEscrowCache.getMerchantEscrow(identifier)).thenReturn(Optional.empty());

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            event.fetchMerchantEscrow(true);
        });
        assertTrue(exception.getMessage().contains(identifier));
        assertTrue(exception.getMessage().contains("not found"));
        verify(merchantEscrowCache).getMerchantEscrow(identifier);
    }

    @Test
    @DisplayName("Test toMerchantEscrow creates new escrow when not exists")
    void testToMerchantEscrowCreatesNewEscrow() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        String identifier = event.getIdentifier();
        String usdtAccountKey = event.getUsdtAccountKey();
        String fiatAccountKey = event.getFiatAccountKey();
        BigDecimal usdtAmount = event.getUsdtAmount();
        BigDecimal fiatAmount = event.getFiatAmount();
        String fiatCurrency = event.getFiatCurrency();
        String userId = event.getUserId();

        // Configure mocks
        when(merchantEscrowCache.getMerchantEscrow(identifier)).thenReturn(Optional.empty());

        // When
        MerchantEscrow result = event.toMerchantEscrow(false);

        // Then
        assertNotNull(result);
        assertEquals(identifier, result.getIdentifier());
        assertEquals(usdtAccountKey, result.getUsdtAccountKey());
        assertEquals(fiatAccountKey, result.getFiatAccountKey());
        assertEquals(usdtAmount, result.getUsdtAmount());
        assertEquals(fiatAmount, result.getFiatAmount());
        assertEquals(fiatCurrency, result.getFiatCurrency());
        assertEquals(userId, result.getUserId());
        verify(merchantEscrowCache).getMerchantEscrow(identifier);
    }

    @Test
    @DisplayName("Test toMerchantEscrow returns existing escrow")
    void testToMerchantEscrowReturnsExistingEscrow() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        String identifier = event.getIdentifier();

        // Configure mocks
        when(merchantEscrowCache.getMerchantEscrow(identifier)).thenReturn(Optional.of(merchantEscrow));

        // When
        MerchantEscrow result = event.toMerchantEscrow(true);

        // Then
        assertNotNull(result);
        assertEquals(merchantEscrow, result);
        verify(merchantEscrowCache).getMerchantEscrow(identifier);
    }

    @Test
    @DisplayName("Test validate with valid MERCHANT_ESCROW_MINT event")
    void testValidateValidMintEvent() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();

        // When/Then
        assertDoesNotThrow(() -> event.validate());
    }

    @Test
    @DisplayName("Test validate should always keep action type as MERCHANT_ESCROW")
    void testValidateActionTypeIsAlwaysMerchantEscrow() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        
        // Try to set a different actionType
        event.setActionType(ActionType.AMM_POOL);
        
        // Validation should throw exception when actionType is not MERCHANT_ESCROW
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("Action type not matched: expected MerchantEscrow"));
    }

    @Test
    @DisplayName("Test validate with unsupported operation type")
    void testValidateUnsupportedOperationType() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setOperationType(OperationType.AMM_POOL_CREATE);

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("Operation type is not supported in list"));
    }

    @Test
    @DisplayName("Test parserData parses all fields correctly")
    void testParserDataParsesAllFields() {
        // Given
        MerchantEscrowEvent event = new MerchantEscrowEvent();
        JsonNode messageJson = MerchantEscrowEventFactory.createJsonNode();

        // When
        event.parserData(messageJson);

        // Then
        assertEquals(messageJson.path("eventId").asText(), event.getEventId());
        assertEquals(ActionType.fromValue(messageJson.path("actionType").asText()), event.getActionType());
        assertEquals(messageJson.path("actionId").asText(), event.getActionId());
        assertEquals(OperationType.fromValue(messageJson.path("operationType").asText()), event.getOperationType());
        assertEquals(messageJson.path("identifier").asText(), event.getIdentifier());
        assertEquals(messageJson.path("usdtAccountKey").asText(), event.getUsdtAccountKey());
        assertEquals(messageJson.path("fiatAccountKey").asText(), event.getFiatAccountKey());
        assertEquals(new BigDecimal(messageJson.path("usdtAmount").asText()), event.getUsdtAmount());
        assertEquals(new BigDecimal(messageJson.path("fiatAmount").asText()), event.getFiatAmount());
        assertEquals(messageJson.path("fiatCurrency").asText(), event.getFiatCurrency());
        assertEquals(messageJson.path("userId").asText(), event.getUserId());
        assertEquals(messageJson.path("merchantEscrowOperationId").asText(), event.getMerchantEscrowOperationId());
    }

    @Test
    @DisplayName("Test toOperationObjectMessageJson")
    void testToOperationObjectMessageJson() {
        // Given
        String eventId = "test-event-id";
        String actionId = "test-action-id";
        String identifier = "test-identifier";
        String usdtAccountId = "test-usdt-account";
        String fiatAccountId = "test-fiat-account";
        BigDecimal usdtAmount = new BigDecimal("100");
        BigDecimal fiatAmount = new BigDecimal("3000");
        String fiatCurrency = "USD";
        String userId = "test-user";
        String merchantEscrowOperationId = "test-operation";
        
        MerchantEscrowEvent event = new MerchantEscrowEvent();
        event.setEventId(eventId);
        event.setOperationType(OperationType.MERCHANT_ESCROW_MINT);
        event.setActionType(ActionType.MERCHANT_ESCROW);
        event.setActionId(actionId);
        event.setIdentifier(identifier);
        event.setUsdtAccountKey(usdtAccountId);
        event.setFiatAccountKey(fiatAccountId);
        event.setOperationType(OperationType.MERCHANT_ESCROW_MINT);
        event.setUsdtAmount(usdtAmount);
        event.setFiatAmount(fiatAmount);
        event.setFiatCurrency(fiatCurrency);
        event.setUserId(userId);
        event.setMerchantEscrowOperationId(merchantEscrowOperationId);
        
        Map<String, Object> escrowJson = Map.of("id", identifier);
        
        // Configure mocks
        MerchantEscrowCache.setTestInstance(merchantEscrowCache);
        when(merchantEscrowCache.getMerchantEscrow(identifier)).thenReturn(Optional.of(merchantEscrow));
        when(merchantEscrow.toMessageJson()).thenReturn(escrowJson);

        // When
        Map<String, Object> result = event.toOperationObjectMessageJson();

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.get("eventId"));
        assertEquals(ActionType.MERCHANT_ESCROW.getValue(), result.get("actionType"));
        assertEquals(actionId, result.get("actionId"));
        assertEquals(OperationType.MERCHANT_ESCROW_MINT.getValue(), result.get("operationType"));
        assertEquals(escrowJson, result.get("object"));

        // Verify these fields are NOT in the message json
        assertNull(result.get("identifier"));
        assertNull(result.get("usdtAccountId"));
        assertNull(result.get("fiatAccountId"));
        assertNull(result.get("usdtAmount"));
        assertNull(result.get("fiatAmount"));
        assertNull(result.get("fiatCurrency"));
        assertNull(result.get("userId"));
        assertNull(result.get("merchantEscrowOperationId"));

        // Clean up
        MerchantEscrowCache.setTestInstance(null);
    }

    @Test
    @DisplayName("Test validate with missing identifier")
    void testValidateMissingIdentifier() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setIdentifier("");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("identifier is required"));
    }

    @Test
    @DisplayName("Test validate with missing usdtAccountKey")
    void testValidateMissingUsdtAccountKey() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setUsdtAccountKey("");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("usdtAccountKey is required"));
    }

    @Test
    @DisplayName("Test validate with missing fiatAccountKey")
    void testValidateMissingFiatAccountKey() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setFiatAccountKey("");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("fiatAccountKey is required"));
    }

    @Test
    @DisplayName("Test validate with missing operationType")
    void testValidateMissingOperationType() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setOperationType(null);

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("operationType is required"));
    }

    @Test
    @DisplayName("Test validate with invalid usdtAmount")
    void testValidateInvalidUsdtAmount() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setUsdtAmount(BigDecimal.ZERO);

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("usdtAmount must be greater than 0"));
    }

    @Test
    @DisplayName("Test validate with invalid fiatAmount")
    void testValidateInvalidFiatAmount() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setFiatAmount(BigDecimal.ZERO);

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("fiatAmount must be greater than 0"));
    }

    @Test
    @DisplayName("Test validate with missing fiatCurrency")
    void testValidateMissingFiatCurrency() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setFiatCurrency("");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("fiatCurrency is required"));
    }

    @Test
    @DisplayName("Test validate with missing userId")
    void testValidateMissingUserId() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setUserId("");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("userId is required"));
    }

    @Test
    @DisplayName("Test validate with missing merchantEscrowOperationId")
    void testValidateMissingMerchantEscrowOperationId() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setMerchantEscrowOperationId("");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("merchantEscrowOperationId is required"));
    }

    @Test
    @DisplayName("Test getUsdtAccount returns account from cache")
    void testGetUsdtAccount() {
        // Given
        String usdtAccountKey = "usdt-account-123";
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setUsdtAccountKey(usdtAccountKey);
        
        // Use the already mocked cache
        AccountCache.setTestInstance(mock(AccountCache.class));
        Account mockAccount = mock(Account.class);
        
        when(AccountCache.getInstance().getAccount(usdtAccountKey)).thenReturn(Optional.of(mockAccount));
        
        // When
        Optional<Account> result = event.getUsdtAccount();
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(mockAccount, result.get());
        verify(AccountCache.getInstance()).getAccount(usdtAccountKey);
        
        // Clean up
        AccountCache.setTestInstance(null);
    }

    @Test
    @DisplayName("Test getFiatAccount returns account from cache")
    void testGetFiatAccount() {
        // Given
        String fiatAccountKey = "fiat-account-123";
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        event.setFiatAccountKey(fiatAccountKey);
        
        // Use the already mocked cache
        AccountCache.setTestInstance(mock(AccountCache.class));
        Account mockAccount = mock(Account.class);
        
        when(AccountCache.getInstance().getAccount(fiatAccountKey)).thenReturn(Optional.of(mockAccount));
        
        // When
        Optional<Account> result = event.getFiatAccount();
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(mockAccount, result.get());
        verify(AccountCache.getInstance()).getAccount(fiatAccountKey);
        
        // Clean up
        AccountCache.setTestInstance(null);
    }

    @Test
    @DisplayName("Test updateAccount calls accountCache.updateAccount")
    void testUpdateAccount() {
        // Given
        MerchantEscrowEvent event = MerchantEscrowEventFactory.create();
        Account mockAccount = mock(Account.class);
        
        // Use the already mocked cache  
        AccountCache.setTestInstance(mock(AccountCache.class));
        
        // When
        event.updateAccount(mockAccount);
        
        // Then
        verify(AccountCache.getInstance()).updateAccount(mockAccount);
        
        // Clean up
        AccountCache.setTestInstance(null);
    }

    @Test
    @DisplayName("Test parserData with null input throws IllegalArgumentException")
    void testParserDataWithNullInput() {
        // Given
        MerchantEscrowEvent event = new MerchantEscrowEvent();
        
        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.parserData(null);
        });
        assertTrue(exception.getMessage().contains("MessageJson is required"));
    }
    
    @Test
    @DisplayName("Test parserData with numeric amounts")
    void testParserDataWithNumericAmounts() {
        // Given
        MerchantEscrowEvent event = new MerchantEscrowEvent();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        
        // Required fields
        jsonNode.put("eventId", "test-event-id");
        jsonNode.put("actionType", ActionType.MERCHANT_ESCROW.getValue());
        jsonNode.put("actionId", "test-action-id");
        jsonNode.put("operationType", OperationType.MERCHANT_ESCROW_MINT.getValue());
        jsonNode.put("identifier", "test-identifier");
        jsonNode.put("usdtAccountKey", "test-usdt-account");
        jsonNode.put("fiatAccountKey", "test-fiat-account");
        jsonNode.put("usdtAmount", 100.50); // Numeric amount
        jsonNode.put("fiatAmount", 1000.75); // Numeric amount
        jsonNode.put("fiatCurrency", "USD");
        jsonNode.put("userId", "test-user");
        jsonNode.put("merchantEscrowOperationId", "test-operation");
        
        // When
        event.parserData(jsonNode);
        
        // Then
        assertEquals("test-event-id", event.getEventId());
        assertEquals(ActionType.MERCHANT_ESCROW, event.getActionType());
        assertEquals("test-action-id", event.getActionId());
        assertEquals(OperationType.MERCHANT_ESCROW_MINT, event.getOperationType());
        assertEquals("test-identifier", event.getIdentifier());
        assertEquals("test-usdt-account", event.getUsdtAccountKey());
        assertEquals("test-fiat-account", event.getFiatAccountKey());
        assertEquals(new BigDecimal("100.5"), event.getUsdtAmount());
        assertEquals(new BigDecimal("1000.75"), event.getFiatAmount());
        assertEquals("USD", event.getFiatCurrency());
        assertEquals("test-user", event.getUserId());
        assertEquals("test-operation", event.getMerchantEscrowOperationId());
    }
    
    @Test
    @DisplayName("Test parserData with empty merchantEscrowOperationId defaults to actionId")
    void testParserDataWithEmptyMerchantEscrowOperationId() {
        // Given
        MerchantEscrowEvent event = new MerchantEscrowEvent();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();
        
        String actionId = "test-action-id";
        
        // Required fields
        jsonNode.put("eventId", "test-event-id");
        jsonNode.put("actionType", ActionType.MERCHANT_ESCROW.getValue());
        jsonNode.put("actionId", actionId);
        jsonNode.put("operationType", OperationType.MERCHANT_ESCROW_MINT.getValue());
        jsonNode.put("identifier", "test-identifier");
        jsonNode.put("usdtAccountKey", "test-usdt-account");
        jsonNode.put("fiatAccountKey", "test-fiat-account");
        jsonNode.put("usdtAmount", "100.50");
        jsonNode.put("fiatAmount", "1000.75");
        jsonNode.put("fiatCurrency", "USD");
        jsonNode.put("userId", "test-user");
        jsonNode.put("merchantEscrowOperationId", ""); // Empty operation ID
        
        // When
        event.parserData(jsonNode);
        
        // Then
        assertEquals(actionId, event.getMerchantEscrowOperationId());
    }
    
    @Test
    @DisplayName("Test setActionType behavior")
    void testSetActionTypeAlwaysSetsCorrectType() {
        // Given
        MerchantEscrowEvent event = new MerchantEscrowEvent();
        
        // Initialize with MERCHANT_ESCROW
        event.setActionType(ActionType.MERCHANT_ESCROW);
        assertEquals(ActionType.MERCHANT_ESCROW, event.getActionType());
        
        // Try to set a different action type
        event.setActionType(ActionType.AMM_POOL);
        
        // MerchantEscrowEvent does not override setActionType, so it should change
        assertEquals(ActionType.AMM_POOL, event.getActionType());
        
        // Set it back for other tests
        event.setActionType(ActionType.MERCHANT_ESCROW);
    }
} 