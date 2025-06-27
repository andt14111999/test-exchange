package com.exchangeengine.model.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.MerchantEscrowCache;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.model.Account;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Model for MerchantEscrow events
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantEscrowEvent extends BaseEvent {
    // Fields for merchant escrow operations
    private String identifier;
    private String usdtAccountKey;
    private String fiatAccountKey;
    private OperationType operationType; // mint, burn
    private BigDecimal usdtAmount;
    private BigDecimal fiatAmount;
    private String fiatCurrency;
    private String userId;
    private String merchantEscrowOperationId;

    @Override
    public String getProducerEventId() {
        return this.identifier;
    }

    @Override
    public String getEventHandler() {
        return EventHandlerAction.MERCHANT_ESCROW_EVENT;
    }
    
    /**
     * Parses data from a JSON message into this event
     * @param messageJson The JSON message to parse
     * @return This event with parsed data
     */
    public MerchantEscrowEvent parserData(JsonNode messageJson) {
        if (messageJson == null) {
            throw new IllegalArgumentException("MessageJson is required");
        }

        setEventId(messageJson.path("eventId").asText());
        setActionType(ActionType.MERCHANT_ESCROW);
        setActionId(messageJson.path("actionId").asText());
        OperationType parsedOperationType = OperationType.fromValue(messageJson.path("operationType").asText());
        super.setOperationType(parsedOperationType);
        this.operationType = parsedOperationType;
        this.identifier = messageJson.path("identifier").asText();
        this.usdtAccountKey = messageJson.path("usdtAccountKey").asText("");
        this.fiatAccountKey = messageJson.path("fiatAccountKey").asText("");
        
        // Handle both numeric and string representation of amounts
        if (messageJson.has("usdtAmount")) {
            JsonNode node = messageJson.get("usdtAmount");
            this.usdtAmount = node.isTextual() 
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        if (messageJson.has("fiatAmount")) {
            JsonNode node = messageJson.get("fiatAmount");
            this.fiatAmount = node.isTextual()
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        this.fiatCurrency = messageJson.path("fiatCurrency").asText();
        this.userId = messageJson.path("userId").asText();
        this.merchantEscrowOperationId = messageJson.path("merchantEscrowOperationId").asText("");
        if (this.merchantEscrowOperationId.isEmpty()) {
            this.merchantEscrowOperationId = getActionId();
        }

        return this;
    }
    
    /**
     * Validates that this event contains valid data
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        List<String> objectErrors = super.validateRequiredFields();
        
        if (!ActionType.MERCHANT_ESCROW.isEqualTo(getActionType().getValue())) {
            objectErrors.add("Action type not matched: expected MerchantEscrow, got " + getActionType().getValue());
        }
                
        // Check that the operation type is a merchant escrow operation
        if (!OperationType.MERCHANT_ESCROW_OPERATIONS.contains(getOperationType())) {
            objectErrors.add("Operation type is not supported in list: [" + 
                OperationType.getSupportedMerchantEscrowValues() + "]");
        }

        // Validate required fields
        if (identifier == null || identifier.trim().isEmpty()) {
            objectErrors.add("identifier is required");
        }
        
        if (usdtAccountKey == null || usdtAccountKey.trim().isEmpty()) {
            objectErrors.add("usdtAccountKey is required");
        }
        
        if (fiatAccountKey == null || fiatAccountKey.trim().isEmpty()) {
            objectErrors.add("fiatAccountKey is required");
        }
        
        if (operationType == null) {
            objectErrors.add("operationType is required");
        }
        
        if (usdtAmount == null || usdtAmount.compareTo(BigDecimal.ZERO) <= 0) {
            objectErrors.add("usdtAmount must be greater than 0");
        }
        
        if (fiatAmount == null || fiatAmount.compareTo(BigDecimal.ZERO) <= 0) {
            objectErrors.add("fiatAmount must be greater than 0");
        }
        
        if (fiatCurrency == null || fiatCurrency.trim().isEmpty()) {
            objectErrors.add("fiatCurrency is required");
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            objectErrors.add("userId is required");
        }
        
        if (merchantEscrowOperationId == null || merchantEscrowOperationId.trim().isEmpty()) {
            objectErrors.add("merchantEscrowOperationId is required");
        }
        
        if (objectErrors.size() > 0) {
            throw new IllegalArgumentException("Validate MerchantEscrowEvent: " + String.join(", ", objectErrors));
        }
    }

    protected MerchantEscrowCache getMerchantEscrowCache() {
        return MerchantEscrowCache.getInstance();
    }
    
    protected AccountCache getAccountCache() {
        return AccountCache.getInstance();
    }

    public Optional<Account> getUsdtAccount() {
        return getAccountCache().getAccount(usdtAccountKey);
    }
    
    public Optional<Account> getFiatAccount() {
        return getAccountCache().getAccount(fiatAccountKey);
    }

    public void updateAccount(Account account) {
        getAccountCache().updateAccount(account);
    }

    public Optional<MerchantEscrow> fetchMerchantEscrow(boolean raiseException) {
        Optional<MerchantEscrow> escrow = getMerchantEscrowCache().getMerchantEscrow(identifier);
        if (raiseException && !escrow.isPresent()) {
            throw new IllegalStateException("MerchantEscrow " + identifier + " not found");
        }
        return escrow;
    }

    public MerchantEscrow toMerchantEscrow(boolean raiseException) {
        MerchantEscrow escrow = fetchMerchantEscrow(raiseException).orElseGet(() -> {
            MerchantEscrow newEscrow = new MerchantEscrow();
            newEscrow.setIdentifier(identifier);
            newEscrow.setUsdtAccountKey(usdtAccountKey);
            newEscrow.setFiatAccountKey(fiatAccountKey);
            newEscrow.setOperationType(operationType);
            newEscrow.setUsdtAmount(usdtAmount);
            newEscrow.setFiatAmount(fiatAmount);
            newEscrow.setFiatCurrency(fiatCurrency);
            newEscrow.setUserId(userId);
            newEscrow.setMerchantEscrowOperationId(merchantEscrowOperationId);
            return newEscrow;
        });
        return escrow;
    }

    @Override
    public Map<String, Object> toOperationObjectMessageJson() {
        Map<String, Object> messageJson = super.toOperationObjectMessageJson();
        messageJson.put("actionType", getActionType().getValue());
        messageJson.put("actionId", getActionId());
        messageJson.put("operationType", getOperationType().getValue());
        messageJson.put("object", fetchMerchantEscrow(true).map(MerchantEscrow::toMessageJson).orElse(null));

        return messageJson;
    }
} 