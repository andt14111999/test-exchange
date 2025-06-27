package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.BalancesLockEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class to create BalancesLockEvent objects for testing purposes
 */
public class BalancesLockEventFactory {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
    private static final ActionType DEFAULT_ACTION_TYPE = ActionType.TRADE;
    private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
    private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.BALANCES_LOCK_CREATE;
    private static final String DEFAULT_LOCK_ID = "lock-" + UUID.randomUUID().toString().substring(0, 8);
    private static final List<String> DEFAULT_ACCOUNT_KEYS = Arrays.asList("account1", "account2");
    private static final String DEFAULT_IDENTIFIER = "test-identifier";

    /**
     * Create a base model for BalancesLockEvent with default values
     *
     * @return a model for BalancesLockEvent
     */
    public static Model<BalancesLockEvent> model() {
        return Instancio.of(BalancesLockEvent.class)
                .set(field(BalancesLockEvent::getEventId), DEFAULT_EVENT_ID)
                .set(field(BalancesLockEvent::getActionType), DEFAULT_ACTION_TYPE)
                .set(field(BalancesLockEvent::getActionId), DEFAULT_ACTION_ID)
                .set(field(BalancesLockEvent::getOperationType), DEFAULT_OPERATION_TYPE)
                .set(field(BalancesLockEvent::getLockId), DEFAULT_LOCK_ID)
                .set(field(BalancesLockEvent::getAccountKeys), DEFAULT_ACCOUNT_KEYS)
                .set(field(BalancesLockEvent::getIdentifier), DEFAULT_IDENTIFIER)
                .toModel();
    }

    /**
     * Create a BalancesLockEvent with default values
     *
     * @return a valid BalancesLockEvent
     */
    public static BalancesLockEvent create() {
        return Instancio.create(model());
    }

    /**
     * Create a BalancesLockEvent for CREATE operation
     *
     * @return a BalancesLockEvent with BALANCES_LOCK_CREATE operation type
     */
    public static BalancesLockEvent forCreate() {
        return Instancio.of(model())
                .set(field(BalancesLockEvent::getOperationType), OperationType.BALANCES_LOCK_CREATE)
                .set(field(BalancesLockEvent::getLockId), (String) null) // No lockId for create
                .create();
    }

    /**
     * Create a BalancesLockEvent for RELEASE operation
     *
     * @return a BalancesLockEvent with BALANCES_LOCK_RELEASE operation type
     */
    public static BalancesLockEvent forRelease() {
        return Instancio.of(model())
                .set(field(BalancesLockEvent::getOperationType), OperationType.BALANCES_LOCK_RELEASE)
                .set(field(BalancesLockEvent::getAccountKeys), (List<String>) null) // No accountKeys for release
                .set(field(BalancesLockEvent::getIdentifier), (String) null) // No identifier for release
                .create();
    }

    /**
     * Create a BalancesLockEvent with specific lockId
     *
     * @param lockId the lock ID to set
     * @return a BalancesLockEvent with the specified lockId
     */
    public static BalancesLockEvent withLockId(String lockId) {
        return Instancio.of(model())
                .set(field(BalancesLockEvent::getLockId), lockId)
                .create();
    }

    /**
     * Create a BalancesLockEvent with specific account keys
     *
     * @param accountKeys the account keys to set
     * @return a BalancesLockEvent with the specified account keys
     */
    public static BalancesLockEvent withAccountKeys(List<String> accountKeys) {
        return Instancio.of(model())
                .set(field(BalancesLockEvent::getAccountKeys), accountKeys)
                .create();
    }

    /**
     * Create a BalancesLockEvent with specific identifier
     *
     * @param identifier the identifier to set
     * @return a BalancesLockEvent with the specified identifier
     */
    public static BalancesLockEvent withIdentifier(String identifier) {
        return Instancio.of(model())
                .set(field(BalancesLockEvent::getIdentifier), identifier)
                .create();
    }

    /**
     * Create a BalancesLockEvent with specific operation type
     *
     * @param operationType the operation type to set
     * @return a BalancesLockEvent with the specified operation type
     */
    public static BalancesLockEvent withOperationType(OperationType operationType) {
        return Instancio.of(model())
                .set(field(BalancesLockEvent::getOperationType), operationType)
                .create();
    }

    /**
     * Create a BalancesLockEvent with specific action type
     *
     * @param actionType the action type to set
     * @return a BalancesLockEvent with the specified action type
     */
    public static BalancesLockEvent withActionType(ActionType actionType) {
        return Instancio.of(model())
                .set(field(BalancesLockEvent::getActionType), actionType)
                .create();
    }

    /**
     * Create a JsonNode representation of a BalancesLockEvent
     *
     * @param balancesLockEvent the balances lock event to convert to JsonNode
     * @return a JsonNode representation
     */
    public static JsonNode toJsonNode(BalancesLockEvent balancesLockEvent) {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", balancesLockEvent.getEventId());
        jsonNode.put("actionType", balancesLockEvent.getActionType().getValue());
        jsonNode.put("actionId", balancesLockEvent.getActionId());
        jsonNode.put("operationType", balancesLockEvent.getOperationType().getValue());
        
        if (balancesLockEvent.getLockId() != null) {
            jsonNode.put("lockId", balancesLockEvent.getLockId());
        }
        
        if (balancesLockEvent.getAccountKeys() != null) {
            ArrayNode accountKeysArray = objectMapper.createArrayNode();
            for (String accountKey : balancesLockEvent.getAccountKeys()) {
                accountKeysArray.add(accountKey);
            }
            jsonNode.set("accountKeys", accountKeysArray);
        }
        
        if (balancesLockEvent.getIdentifier() != null) {
            jsonNode.put("identifier", balancesLockEvent.getIdentifier());
        }

        return jsonNode;
    }

    /**
     * Create a JsonNode representation of a valid BalancesLockEvent with default values
     *
     * @return a JsonNode representation of a valid BalancesLockEvent
     */
    public static JsonNode createJsonNode() {
        return toJsonNode(create());
    }

    /**
     * Create a JsonNode for CREATE operation
     *
     * @return a JsonNode for BALANCES_LOCK_CREATE operation
     */
    public static JsonNode createJsonNodeForCreate() {
        return toJsonNode(forCreate());
    }

    /**
     * Create a JsonNode for RELEASE operation
     *
     * @return a JsonNode for BALANCES_LOCK_RELEASE operation
     */
    public static JsonNode createJsonNodeForRelease() {
        return toJsonNode(forRelease());
    }

    /**
     * Create a JsonNode with specific account keys
     *
     * @param accountKeys the account keys to include
     * @return a JsonNode with specified account keys
     */
    public static JsonNode createJsonNodeWithAccountKeys(List<String> accountKeys) {
        return toJsonNode(withAccountKeys(accountKeys));
    }
} 