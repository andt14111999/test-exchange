package com.exchangeengine.model.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.Offer;
import com.exchangeengine.model.Account;
import com.exchangeengine.storage.cache.OfferCache;
import com.exchangeengine.storage.cache.AccountCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.format.DateTimeParseException;

/**
 * Model for Offer events
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferEvent extends BaseEvent {
    private String identifier;
    private String userId;
    private String offerType;
    private String coinCurrency;
    private String currency;  // Changed from fiatCurrency to currency to match Ruby
    private BigDecimal price;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal totalAmount;
    private BigDecimal availableAmount;
    private String paymentMethodId;
    private Integer paymentTime;
    private String countryCode;
    private Boolean disabled;
    private Boolean deleted;
    private Boolean automatic;
    private Boolean online;
    private BigDecimal margin;
    private Instant createdAt;
    private Instant updatedAt;

    protected OfferCache getOfferCache() {
        return OfferCache.getInstance();
    }

    protected AccountCache getAccountCache() {
        return AccountCache.getInstance();
    }

    @Override
    public String getProducerEventId() {
        return this.identifier;
    }

    @Override
    public String getEventHandler() {
        return EventHandlerAction.OFFER_EVENT;
    }

    /**
     * Fetch the offer from cache
     * @param raiseException Whether to raise an exception if not found
     * @return Optional containing the offer if found
     */
    public Optional<Offer> fetchOffer(boolean raiseException) {
        Optional<Offer> offer = getOfferCache().getOffer(identifier);
        if (raiseException && !offer.isPresent()) {
            throw new IllegalStateException("Offer not found identifier: " + identifier);
        }
        return offer;
    }

    /**
     * Get the user account from cache
     * @return Optional containing the account if found
     */
    public Optional<Account> getUserAccount() {
        return getAccountCache().getAccount(userId);
    }

    /**
     * Update an account in the cache
     * @param account The account to update
     */
    public void updateAccount(Account account) {
        getAccountCache().updateAccount(account);
    }

    /**
     * Update offer in the cache
     * @param offer Offer to update
     */
    public void updateOffer(Offer offer) {
        getOfferCache().updateOffer(offer);
    }

    /**
     * Convert this event to an Offer object
     * @param raiseException Whether to raise an exception if not found
     * @return The created or fetched Offer
     */
    public Offer toOffer(boolean raiseException) {
        Offer offer = fetchOffer(raiseException).orElseGet(() -> {
            Offer newOffer = new Offer();
            newOffer.setIdentifier(identifier);
            newOffer.setUserId(userId);
            newOffer.setSymbol(coinCurrency + ":" + currency);  // Use currency
            newOffer.setType(Offer.OfferType.valueOf(offerType.toUpperCase()));
            newOffer.setPrice(price);
            newOffer.setTotalAmount(totalAmount);
            newOffer.setStatus(Offer.OfferStatus.PENDING);
            newOffer.setCreatedAt(createdAt != null ? createdAt : Instant.now());
            newOffer.setUpdatedAt(updatedAt != null ? updatedAt : Instant.now());
            newOffer.setDisabled(disabled);
            newOffer.setDeleted(deleted);
            newOffer.setAutomatic(automatic);
            newOffer.setOnline(online);
            newOffer.setMargin(margin);
            newOffer.setPaymentMethodId(paymentMethodId);
            newOffer.setPaymentTime(paymentTime);
            newOffer.setCountryCode(countryCode);
            newOffer.setMinAmount(minAmount);
            newOffer.setMaxAmount(maxAmount);
            newOffer.setAvailableAmount(availableAmount);
            return newOffer;
        });
        return offer;
    }

    /**
     * Parse data from a JSON message into this event
     * @param messageJson The JSON message to parse
     * @return This event with parsed data
     */
    public OfferEvent parserData(JsonNode messageJson) {
        if (messageJson == null) {
            throw new IllegalArgumentException("MessageJson is required");
        }

        String eventId = messageJson.path("eventId").asText();
        String tmpIdentifier = messageJson.path("identifier").asText();
        ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
        String tmpActionId = messageJson.path("actionId").asText();
        OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());

        setEventId(eventId);
        setActionType(tmpActionType);
        setActionId(tmpActionId);
        setOperationType(tmpOperationType);
        this.identifier = tmpIdentifier;
        this.userId = messageJson.path("userId").asText();
        this.offerType = messageJson.path("offerType").asText();
        this.coinCurrency = messageJson.path("coinCurrency").asText().toLowerCase().trim();
        this.currency = messageJson.path("currency").asText().toLowerCase().trim();
        
        // Handle both numeric and string representation of amounts
        if (messageJson.has("price")) {
            JsonNode node = messageJson.get("price");
            this.price = node.isTextual() 
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        if (messageJson.has("minAmount")) {
            JsonNode node = messageJson.get("minAmount");
            this.minAmount = node.isTextual() 
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        if (messageJson.has("maxAmount")) {
            JsonNode node = messageJson.get("maxAmount");
            this.maxAmount = node.isTextual() 
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        if (messageJson.has("totalAmount")) {
            JsonNode node = messageJson.get("totalAmount");
            this.totalAmount = node.isTextual() 
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        if (messageJson.has("availableAmount")) {
            JsonNode node = messageJson.get("availableAmount");
            this.availableAmount = node.isTextual() 
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        if (messageJson.has("margin")) {
            JsonNode node = messageJson.get("margin");
            this.margin = node.isTextual() 
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        this.paymentMethodId = messageJson.path("paymentMethodId").asText();
        this.paymentTime = messageJson.path("paymentTime").asInt();
        this.countryCode = messageJson.path("countryCode").asText();
        this.disabled = messageJson.path("disabled").asBoolean();
        this.deleted = messageJson.path("deleted").asBoolean();
        this.automatic = messageJson.path("automatic").asBoolean();
        this.online = messageJson.path("online").asBoolean();
        
        // Parse timestamps - handle both ISO-8601 and Unix epoch format
        try {
            // Handle createdAt
            if (messageJson.has("createdAt")) {
                JsonNode createdAtNode = messageJson.get("createdAt");
                if (createdAtNode.isNumber()) {
                    // Unix timestamp (seconds)
                    this.createdAt = Instant.ofEpochSecond(createdAtNode.asLong());
                } else {
                    String createdAtStr = createdAtNode.asText();
                    if (!createdAtStr.isEmpty()) {
                        try {
                            // Try parsing as ISO-8601
                            this.createdAt = Instant.parse(createdAtStr);
                        } catch (DateTimeParseException e) {
                            // Try parsing as Unix timestamp
                            this.createdAt = Instant.ofEpochSecond(Long.parseLong(createdAtStr));
                        }
                    }
                }
            }
            
            // Handle updatedAt
            if (messageJson.has("updatedAt")) {
                JsonNode updatedAtNode = messageJson.get("updatedAt");
                if (updatedAtNode.isNumber()) {
                    // Unix timestamp (seconds)
                    this.updatedAt = Instant.ofEpochSecond(updatedAtNode.asLong());
                } else {
                    String updatedAtStr = updatedAtNode.asText();
                    if (!updatedAtStr.isEmpty()) {
                        try {
                            // Try parsing as ISO-8601
                            this.updatedAt = Instant.parse(updatedAtStr);
                        } catch (DateTimeParseException e) {
                            // Try parsing as Unix timestamp
                            this.updatedAt = Instant.ofEpochSecond(Long.parseLong(updatedAtStr));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing timestamps: " + e.getMessage(), e);
        }

        return this;
    }

    /**
     * Validates that this event contains valid data
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        List<String> objectErrors = super.validateRequiredFields();

        if (objectErrors.size() == 0) {
            if (!OperationType.OFFER_OPERATIONS.contains(getOperationType())) {
                objectErrors.add("Operation type is not supported in list: [" + OperationType.getSupportedOfferValues() + "]");
            }

            if (!ActionType.OFFER.isEqualTo(getActionType().getValue())) {
                objectErrors.add("Action type not matched: expected Offer, got " + getActionType().getValue());
            }
            
            // Validate required fields
            if (identifier == null || identifier.trim().isEmpty()) {
                objectErrors.add("identifier is required");
            }
            
            if (userId == null || userId.trim().isEmpty()) {
                objectErrors.add("userId is required");
            }
            
            if (offerType == null || offerType.trim().isEmpty()) {
                objectErrors.add("offerType is required");
            }
            
            if (coinCurrency == null || coinCurrency.trim().isEmpty()) {
                objectErrors.add("coinCurrency is required");
            }
            
            if (currency == null || currency.trim().isEmpty()) {
                objectErrors.add("currency is required");
            }
            
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                objectErrors.add("price must be greater than 0");
            }
            
            if (minAmount == null || minAmount.compareTo(BigDecimal.ZERO) < 0) {
                objectErrors.add("minAmount cannot be negative");
            }
            
            if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
                objectErrors.add("maxAmount must be greater than 0");
            }
            
            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                objectErrors.add("totalAmount must be greater than 0");
            }
            
            if (availableAmount == null || availableAmount.compareTo(BigDecimal.ZERO) < 0) {
                objectErrors.add("availableAmount cannot be negative");
            }
            
            if (availableAmount != null && totalAmount != null && availableAmount.compareTo(totalAmount) > 0) {
                objectErrors.add("availableAmount cannot be greater than totalAmount");
            }
        }

        if (objectErrors.size() > 0) {
            throw new IllegalArgumentException("validate OfferEvent: " + String.join(", ", objectErrors));
        }
    }

    @Override
    public Map<String, Object> toOperationObjectMessageJson() {
        Map<String, Object> messageJson = super.toOperationObjectMessageJson();
        messageJson.put("actionType", getActionType().getValue());
        messageJson.put("actionId", getActionId());
        messageJson.put("operationType", getOperationType().getValue());
        messageJson.put("object", fetchOffer(false).map(Offer::toMessageJson).orElse(null));
        return messageJson;
    }
}
