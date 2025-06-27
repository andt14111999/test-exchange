package com.exchangeengine.model.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.BalanceLockFactory;
import com.exchangeengine.factory.event.BalancesLockEventFactory;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.BalanceLock;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.BalanceLockCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test cho BalancesLockEvent
 */
@ExtendWith({MockitoExtension.class, CombinedTestExtension.class})
@DisplayName("BalancesLockEvent Tests")
public class BalancesLockEventTest {

    @Mock(lenient = true)
    private BalanceLock balanceLock;

    @Mock(lenient = true)
    private BalanceLockCache balanceLockCache;

    @BeforeEach
    void setUp() {
        // Sử dụng setTestInstance thay vì MockedStatic
        BalanceLockCache.setTestInstance(balanceLockCache);
    }

    @AfterEach
    void tearDown() {
        // Reset test instance
        BalanceLockCache.setTestInstance(null);
    }

    @Test
    @DisplayName("Test create valid BalancesLockEvent")
    void testCreateValidBalancesLockEvent() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();

        // Then
        assertNotNull(event);
        assertEquals(ActionType.TRADE, event.getActionType());
        assertTrue(OperationType.BALANCES_LOCK_OPERATIONS.contains(event.getOperationType()));
        assertNotNull(event.getLockId());
        assertNotNull(event.getAccountKeys());
        assertNotNull(event.getIdentifier());
    }

    @Test
    @DisplayName("Test getProducerEventId returns lockId")
    void testGetProducerEventId() {
        // Given
        String lockId = "test-lock-id";
        BalancesLockEvent event = BalancesLockEventFactory.withLockId(lockId);

        // When
        String producerEventId = event.getProducerEventId();

        // Then
        assertEquals(lockId, producerEventId);
    }

    @Test
    @DisplayName("Test getEventHandler returns BALANCES_LOCK_EVENT")
    void testGetEventHandler() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();

        // When
        String handler = event.getEventHandler();

        // Then
        assertEquals(EventHandlerAction.BALANCES_LOCK_EVENT, handler);
    }

    @Test
    @DisplayName("Test fetchBalanceLock when lock exists")
    void testFetchBalanceLockWhenExists() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        String lockId = event.getLockId();

        // Configure mocks
        when(balanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(balanceLock));

        // When
        Optional<BalanceLock> result = event.fetchBalanceLock(false);

        // Then
        assertTrue(result.isPresent());
        assertEquals(balanceLock, result.get());
        verify(balanceLockCache).getBalanceLock(lockId);
    }

    @Test
    @DisplayName("Test fetchBalanceLock when lock does not exist and no exception")
    void testFetchBalanceLockWhenNotExistsNoException() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        String lockId = event.getLockId();

        // Configure mocks
        when(balanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.empty());

        // When
        Optional<BalanceLock> result = event.fetchBalanceLock(false);

        // Then
        assertFalse(result.isPresent());
        verify(balanceLockCache).getBalanceLock(lockId);
    }

    @Test
    @DisplayName("Test fetchBalanceLock when lock does not exist and raise exception")
    void testFetchBalanceLockWhenNotExistsRaiseException() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        String lockId = event.getLockId();

        // Configure mocks
        when(balanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.empty());

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            event.fetchBalanceLock(true);
        });
        assertTrue(exception.getMessage().contains(lockId));
        assertTrue(exception.getMessage().contains("not found"));
        verify(balanceLockCache).getBalanceLock(lockId);
    }

    @Test
    @DisplayName("Test fetchBalanceLock with null lockId")
    void testFetchBalanceLockWithNullLockId() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        event.setLockId(null);

        // When
        Optional<BalanceLock> result = event.fetchBalanceLock(false);

        // Then
        assertFalse(result.isPresent());
        verify(balanceLockCache, never()).getBalanceLock(any());
    }

    @Test
    @DisplayName("Test toBalanceLock creates new lock when not exists")
    void testToBalanceLockCreatesNewLock() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forCreate();
        List<String> accountKeys = Arrays.asList("account1", "account2");
        String identifier = "test-identifier";
        event.setAccountKeys(accountKeys);
        event.setIdentifier(identifier);

        // Configure mocks
        when(balanceLockCache.getBalanceLock(any())).thenReturn(Optional.empty());

        // When
        BalanceLock result = event.toBalanceLock(false);

        // Then
        assertNotNull(result);
        assertEquals(event.getActionType(), result.getActionType());
        assertEquals(event.getActionId(), result.getActionId());
        assertEquals(accountKeys, result.getAccountKeys());
        assertEquals(identifier, result.getIdentifier());
    }

    @Test
    @DisplayName("Test toBalanceLock returns existing lock")
    void testToBalanceLockReturnsExistingLock() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        String lockId = event.getLockId();

        // Configure mocks
        when(balanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(balanceLock));

        // When
        BalanceLock result = event.toBalanceLock(true);

        // Then
        assertNotNull(result);
        assertEquals(balanceLock, result);
        verify(balanceLockCache).getBalanceLock(lockId);
    }

    @Test
    @DisplayName("Test validate with valid BALANCES_LOCK_CREATE event")
    void testValidateValidCreateEvent() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forCreate();
        event.setAccountKeys(Arrays.asList("account1", "account2"));
        event.setIdentifier("test-identifier");

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> event.validate());
    }

    @Test
    @DisplayName("Test validate with valid BALANCES_LOCK_RELEASE event")
    void testValidateValidReleaseEvent() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forRelease();
        event.setLockId("test-lock-id");

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> event.validate());
    }

    @Test
    @DisplayName("Test validate with invalid action type")
    void testValidateInvalidActionType() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        event.setActionType(null);

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("validate BalancesLockEvent"));
    }

    @Test
    @DisplayName("Test validate with CREATE event but missing account keys")
    void testValidateCreateEventMissingAccountKeys() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forCreate();
        event.setAccountKeys(null);
        event.setIdentifier("test-identifier");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("AccountKeys list is required"));
    }

    @Test
    @DisplayName("Test validate with CREATE event but empty account keys")
    void testValidateCreateEventEmptyAccountKeys() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forCreate();
        event.setAccountKeys(new ArrayList<>());
        event.setIdentifier("test-identifier");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("AccountKeys list is required"));
    }

    @Test
    @DisplayName("Test validate with CREATE event but missing identifier")
    void testValidateCreateEventMissingIdentifier() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forCreate();
        event.setAccountKeys(Arrays.asList("account1"));
        event.setIdentifier(null);

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("Identifier is required"));
    }

    @Test
    @DisplayName("Test validate with CREATE event but empty identifier")
    void testValidateCreateEventEmptyIdentifier() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forCreate();
        event.setAccountKeys(Arrays.asList("account1"));
        event.setIdentifier("");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("Identifier is required"));
    }

    @Test
    @DisplayName("Test validate with RELEASE event but missing lockId")
    void testValidateReleaseEventMissingLockId() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forRelease();
        event.setLockId(null);

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("LockId is required"));
    }

    @Test
    @DisplayName("Test validate with RELEASE event but empty lockId")
    void testValidateReleaseEventEmptyLockId() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forRelease();
        event.setLockId("");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("LockId is required"));
    }

    @Test
    @DisplayName("Test toOperationObjectMessageJson when lock exists")
    void testToOperationObjectMessageJsonLockExists() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        String lockId = event.getLockId();
        BalanceLock mockLock = BalanceLockFactory.create();

        // Configure mocks
        when(balanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(mockLock));

        // When
        Map<String, Object> result = event.toOperationObjectMessageJson();

        // Then
        assertNotNull(result);
        assertNotNull(result.get("object"));
        verify(balanceLockCache).getBalanceLock(lockId);
    }

    @Test
    @DisplayName("Test toOperationObjectMessageJson when lock does not exist")
    void testToOperationObjectMessageJsonLockNotExists() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        String lockId = event.getLockId();

        // Configure mocks
        when(balanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.empty());

        // When/Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            event.toOperationObjectMessageJson();
        });
        assertTrue(exception.getMessage().contains("not found"));
        verify(balanceLockCache).getBalanceLock(lockId);
    }

    @Test
    @DisplayName("Test parserData from JsonNode")
    void testParserData() {
        // Given
        BalancesLockEvent event = new BalancesLockEvent();
        JsonNode messageJson = BalancesLockEventFactory.createJsonNode();

        // When
        BalancesLockEvent result = event.parserData(messageJson);

        // Then
        assertNotNull(result);
        assertEquals(messageJson.path("eventId").asText(), result.getEventId());
        assertEquals(ActionType.fromValue(messageJson.path("actionType").asText()), result.getActionType());
        assertEquals(messageJson.path("actionId").asText(), result.getActionId());
        assertEquals(OperationType.fromValue(messageJson.path("operationType").asText()), result.getOperationType());
        assertEquals(messageJson.path("lockId").asText(), result.getLockId());
        assertEquals(messageJson.path("identifier").asText(), result.getIdentifier());
        assertNotNull(result.getAccountKeys());
    }

    @Test
    @DisplayName("Test parserData parses all fields correctly")
    void testParserDataParsesAllFields() {
        // Given
        BalancesLockEvent event = new BalancesLockEvent();
        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
        jsonNode.put("eventId", "test-event-id");
        jsonNode.put("actionType", ActionType.OFFER.getValue());
        jsonNode.put("actionId", "test-action-id");
        jsonNode.put("operationType", OperationType.BALANCES_LOCK_RELEASE.getValue());
        jsonNode.put("lockId", "test-lock-id");
        jsonNode.put("identifier", "test-identifier");
        
        ArrayNode accountKeysArray = JsonNodeFactory.instance.arrayNode();
        accountKeysArray.add("account1");
        accountKeysArray.add("account2");
        jsonNode.set("accountKeys", accountKeysArray);

        // When
        BalancesLockEvent result = event.parserData(jsonNode);

        // Then
        assertEquals("test-event-id", result.getEventId());
        assertEquals(ActionType.OFFER, result.getActionType());
        assertEquals("test-action-id", result.getActionId());
        assertEquals(OperationType.BALANCES_LOCK_RELEASE, result.getOperationType());
        assertEquals("test-lock-id", result.getLockId());
        assertEquals("test-identifier", result.getIdentifier());
        assertEquals(2, result.getAccountKeys().size());
        assertTrue(result.getAccountKeys().contains("account1"));
        assertTrue(result.getAccountKeys().contains("account2"));
    }

    @Test
    @DisplayName("Test parserData handles invalid accountKeys JSON")
    void testParserDataHandlesInvalidAccountKeysJson() {
        // Given
        BalancesLockEvent event = new BalancesLockEvent();
        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
        jsonNode.put("eventId", "test-event-id");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "test-action-id");
        jsonNode.put("operationType", OperationType.BALANCES_LOCK_CREATE.getValue());
        jsonNode.put("lockId", "test-lock-id");
        jsonNode.put("identifier", "test-identifier");
        jsonNode.put("accountKeys", "invalid-json");

        // When
        BalancesLockEvent result = event.parserData(jsonNode);

        // Then
        assertEquals("test-event-id", result.getEventId());
        assertNotNull(result.getAccountKeys());
        assertTrue(result.getAccountKeys().isEmpty()); // Should default to empty list
    }

    @Test
    @DisplayName("Test parserData handles missing accountKeys field")
    void testParserDataHandlesMissingAccountKeysField() {
        // Given
        BalancesLockEvent event = new BalancesLockEvent();
        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
        jsonNode.put("eventId", "test-event-id");
        jsonNode.put("actionType", ActionType.TRADE.getValue());
        jsonNode.put("actionId", "test-action-id");
        jsonNode.put("operationType", OperationType.BALANCES_LOCK_CREATE.getValue());
        jsonNode.put("lockId", "test-lock-id");
        jsonNode.put("identifier", "test-identifier");

        // When
        BalancesLockEvent result = event.parserData(jsonNode);

        // Then
        assertEquals("test-event-id", result.getEventId());
        assertNotNull(result.getAccountKeys());
        assertTrue(result.getAccountKeys().isEmpty()); // Should default to empty list
    }

    @Test
    @DisplayName("Test getBalanceLockCache returns instance")
    void testGetBalanceLockCache() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();

        // When
        BalanceLockCache result = event.getBalanceLockCache();

        // Then
        assertNotNull(result);
        assertEquals(balanceLockCache, result);
    }

    @Test
    @DisplayName("Test validate with unsupported operation type")
    void testValidateUnsupportedOperationType() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        event.setOperationType(OperationType.AMM_POOL_CREATE); // Unsupported operation

        // When/Then
        // The validation should pass at the event level since it only checks for required fields
        // The operation type validation would be handled by the processor
        assertDoesNotThrow(() -> event.validate());
    }

    @Test
    @DisplayName("Test validate throws exception when there are validation errors")
    void testValidateThrowsExceptionWithErrors() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.forCreate();
        event.setActionType(null); // This will cause validation error
        event.setAccountKeys(Arrays.asList("account1"));
        event.setIdentifier("test-identifier");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            event.validate();
        });
        assertTrue(exception.getMessage().contains("validate BalancesLockEvent"));
    }

    @Test
    @DisplayName("Test toBalanceLock preserves specified lockId with CREATE operation")
    void testToBalanceLockPreservesLockIdWithCreateOperation() {
        // Given
        String specifiedLockId = "custom-lock-id";
        BalancesLockEvent event = BalancesLockEventFactory.forCreate();
        event.setLockId(specifiedLockId);
        List<String> accountKeys = Arrays.asList("account1", "account2");
        String identifier = "test-identifier";
        event.setAccountKeys(accountKeys);
        event.setIdentifier(identifier);

        // Configure mocks
        when(balanceLockCache.getBalanceLock(specifiedLockId)).thenReturn(Optional.empty());

        // When
        BalanceLock result = event.toBalanceLock(false);

        // Then
        assertNotNull(result);
        assertEquals(specifiedLockId, result.getLockId(), "Lock ID should be preserved");
        assertEquals(event.getActionType(), result.getActionType());
        assertEquals(event.getActionId(), result.getActionId());
        assertEquals(accountKeys, result.getAccountKeys());
        assertEquals(identifier, result.getIdentifier());
        assertEquals("LOCKED", result.getStatus());
        assertTrue(result.getLockedBalances().isEmpty());
    }

    @Test
    @DisplayName("Test parserData with missing fields sets default values")
    void testParserDataWithMissingFields() {
        // Given
        BalancesLockEvent event = new BalancesLockEvent();
        
        // Create a JsonNode with minimal fields
        ObjectNode messageJson = JsonNodeFactory.instance.objectNode();
        messageJson.put("eventId", "test-event-id");
        // Intentionally missing most fields
        
        // When
        event.parserData(messageJson);
        
        // Then
        assertEquals("test-event-id", event.getEventId());
        assertNull(event.getActionType());
        assertEquals("", event.getActionId());
        assertNull(event.getOperationType());
        assertEquals("", event.getLockId());
        assertEquals("", event.getIdentifier());
        assertNotNull(event.getAccountKeys());
        assertTrue(event.getAccountKeys().isEmpty());
    }

    @Test
    @DisplayName("Test parserData with missing accountKeys node")
    void testParserDataWithMissingAccountKeysNode() {
        // Given
        BalancesLockEvent event = new BalancesLockEvent();
        
        // Create a JsonNode with all fields except accountKeys
        ObjectNode messageJson = JsonNodeFactory.instance.objectNode();
        messageJson.put("eventId", "test-event-id");
        messageJson.put("actionType", "TRADE");
        messageJson.put("actionId", "test-action-id");
        messageJson.put("operationType", "BALANCES_LOCK_CREATE");
        messageJson.put("lockId", "test-lock-id");
        messageJson.put("identifier", "test-identifier");
        // Intentionally missing accountKeys field
        
        // When
        event.parserData(messageJson);
        
        // Then
        assertEquals("test-event-id", event.getEventId());
        assertEquals(ActionType.TRADE, event.getActionType());
        assertEquals("test-action-id", event.getActionId());
        assertEquals(OperationType.BALANCES_LOCK_CREATE, event.getOperationType());
        assertEquals("test-lock-id", event.getLockId());
        assertEquals("test-identifier", event.getIdentifier());
        assertNotNull(event.getAccountKeys());
        assertTrue(event.getAccountKeys().isEmpty(), "AccountKeys should be empty when node is missing");
    }

    @Test
    @DisplayName("Test parserData with invalid JSON for accountKeys")
    void testParserDataWithInvalidJsonForAccountKeys() {
        // Given
        BalancesLockEvent event = new BalancesLockEvent();
        
        // Create a JsonNode with invalid accountKeys (not an array)
        ObjectNode messageJson = JsonNodeFactory.instance.objectNode();
        messageJson.put("eventId", "test-event-id");
        messageJson.put("actionType", "TRADE");
        messageJson.put("actionId", "test-action-id");
        messageJson.put("operationType", "BALANCES_LOCK_CREATE");
        messageJson.put("lockId", "test-lock-id");
        messageJson.put("identifier", "test-identifier");
        messageJson.put("accountKeys", "not-an-array"); // Invalid - should be an array
        
        // When
        event.parserData(messageJson);
        
        // Then
        assertEquals("test-event-id", event.getEventId());
        assertEquals("test-lock-id", event.getLockId());
        assertNotNull(event.getAccountKeys());
        assertTrue(event.getAccountKeys().isEmpty(), "AccountKeys should be empty when JSON is invalid");
    }

    @Test
    @DisplayName("Test parserData with null JsonNode")
    void testParserDataWithNullJsonNode() {
        // Given
        BalancesLockEvent event = BalancesLockEventFactory.create();
        String originalLockId = event.getLockId();
        List<String> originalAccountKeys = new ArrayList<>(event.getAccountKeys());
        
        // The current implementation doesn't handle null JsonNode, so we'll 
        // test that it throws a NullPointerException as expected
        assertThrows(NullPointerException.class, () -> {
            event.parserData(null);
        });
        
        // Values should remain unchanged since the exception prevents updates
        assertEquals(originalLockId, event.getLockId());
        assertEquals(originalAccountKeys, event.getAccountKeys());
    }
} 