package com.exchangeengine.factory.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.event.MerchantEscrowEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class to create MerchantEscrowEvent objects for testing purposes
 */
public class MerchantEscrowEventFactory {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_EVENT_ID = "evt-" + UUID.randomUUID().toString().substring(0, 8);
    private static final ActionType DEFAULT_ACTION_TYPE = ActionType.MERCHANT_ESCROW;
    private static final String DEFAULT_ACTION_ID = "act-" + UUID.randomUUID().toString().substring(0, 8);
    private static final OperationType DEFAULT_OPERATION_TYPE = OperationType.MERCHANT_ESCROW_MINT;
    private static final String DEFAULT_IDENTIFIER = "escrow-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String DEFAULT_USDT_ACCOUNT_KEY = "usdt-account-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String DEFAULT_FIAT_ACCOUNT_KEY = "fiat-account-" + UUID.randomUUID().toString().substring(0, 8);
    private static final BigDecimal DEFAULT_USDT_AMOUNT = BigDecimal.valueOf(1000);
    private static final BigDecimal DEFAULT_FIAT_AMOUNT = BigDecimal.valueOf(25000000);
    private static final String DEFAULT_FIAT_CURRENCY = "VND";
    private static final String DEFAULT_USER_ID = "user-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String DEFAULT_OPERATION_ID = "op-" + UUID.randomUUID().toString().substring(0, 8);

    /**
     * Create a base model for MerchantEscrowEvent with default values
     *
     * @return a model for MerchantEscrowEvent
     */
    public static Model<MerchantEscrowEvent> model() {
        return Instancio.of(MerchantEscrowEvent.class)
            .set(field(MerchantEscrowEvent::getEventId), DEFAULT_EVENT_ID)
            .set(field(MerchantEscrowEvent::getActionType), DEFAULT_ACTION_TYPE)
            .set(field(MerchantEscrowEvent::getActionId), DEFAULT_ACTION_ID)
            .set(field(MerchantEscrowEvent::getOperationType), DEFAULT_OPERATION_TYPE)
            .set(field(MerchantEscrowEvent::getIdentifier), DEFAULT_IDENTIFIER)
            .set(field(MerchantEscrowEvent::getUsdtAccountKey), DEFAULT_USDT_ACCOUNT_KEY)
            .set(field(MerchantEscrowEvent::getFiatAccountKey), DEFAULT_FIAT_ACCOUNT_KEY)
            .set(field(MerchantEscrowEvent::getUsdtAmount), DEFAULT_USDT_AMOUNT)
            .set(field(MerchantEscrowEvent::getFiatAmount), DEFAULT_FIAT_AMOUNT)
            .set(field(MerchantEscrowEvent::getFiatCurrency), DEFAULT_FIAT_CURRENCY)
            .set(field(MerchantEscrowEvent::getUserId), DEFAULT_USER_ID)
            .set(field(MerchantEscrowEvent::getMerchantEscrowOperationId), DEFAULT_OPERATION_ID)
            .toModel();
    }

    /**
     * Create a MerchantEscrowEvent with default values
     *
     * @return a valid MerchantEscrowEvent
     */
    public static MerchantEscrowEvent create() {
        return Instancio.create(model());
    }

    /**
     * Create a MerchantEscrowEvent with a specific identifier
     *
     * @param identifier the identifier to set
     * @return a MerchantEscrowEvent with the specified identifier
     */
    public static MerchantEscrowEvent withIdentifier(String identifier) {
        return Instancio.of(model())
            .set(field(MerchantEscrowEvent::getIdentifier), identifier)
            .create();
    }

    /**
     * Create a MerchantEscrowEvent with MERCHANT_ESCROW_BURN operation type
     *
     * @return a MerchantEscrowEvent with MERCHANT_ESCROW_BURN operation type
     */
    public static MerchantEscrowEvent forBurn() {
        return Instancio.of(model())
            .set(field(MerchantEscrowEvent::getOperationType), OperationType.MERCHANT_ESCROW_BURN)
            .create();
    }

    /**
     * Create a JsonNode representation of a MerchantEscrowEvent
     *
     * @param merchantEscrowEvent the merchant escrow event to convert to JsonNode
     * @return a JsonNode representation
     */
    public static JsonNode toJsonNode(MerchantEscrowEvent merchantEscrowEvent) {
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("eventId", merchantEscrowEvent.getEventId());
        jsonNode.put("actionType", merchantEscrowEvent.getActionType().getValue());
        jsonNode.put("actionId", merchantEscrowEvent.getActionId());
        jsonNode.put("operationType", merchantEscrowEvent.getOperationType().getValue());
        jsonNode.put("identifier", merchantEscrowEvent.getIdentifier());
        jsonNode.put("usdtAccountKey", merchantEscrowEvent.getUsdtAccountKey());
        jsonNode.put("fiatAccountKey", merchantEscrowEvent.getFiatAccountKey());
        jsonNode.put("usdtAmount", merchantEscrowEvent.getUsdtAmount().toString());
        jsonNode.put("fiatAmount", merchantEscrowEvent.getFiatAmount().toString());
        jsonNode.put("fiatCurrency", merchantEscrowEvent.getFiatCurrency());
        jsonNode.put("userId", merchantEscrowEvent.getUserId());
        jsonNode.put("merchantEscrowOperationId", merchantEscrowEvent.getMerchantEscrowOperationId());

        return jsonNode;
    }

    /**
     * Create a JsonNode representation of a valid MerchantEscrowEvent with default values
     *
     * @return a JsonNode representation of a valid MerchantEscrowEvent
     */
    public static JsonNode createJsonNode() {
        return toJsonNode(create());
    }
} 