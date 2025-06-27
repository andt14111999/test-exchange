package com.exchangeengine.model.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.Trade;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.Offer;
import com.exchangeengine.storage.cache.TradeCache;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.OfferCache;
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
 * Model for Trade events
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeEvent extends BaseEvent {
    private String identifier;
    private String offerKey;
    private String buyerAccountKey;
    private String sellerAccountKey;
    private String symbol;
    private BigDecimal price;
    private BigDecimal coinAmount;
    private BigDecimal amountAfterFee;
    private BigDecimal fiatAmount;
    private BigDecimal feeRatio;
    private BigDecimal totalFee;
    private BigDecimal fixedFee;
    private BigDecimal coinTradingFee;
    private String paymentMethod;
    private String ref;
    private String coinCurrency;
    private String fiatCurrency;
    private String paymentProofStatus;
    private boolean hasPaymentProof;
    private Instant paidAt;
    private Instant releasedAt;
    private Instant disputedAt;
    private String takerSide;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    protected TradeCache getTradeCache() {
        return TradeCache.getInstance();
    }
    
    protected AccountCache getAccountCache() {
        return AccountCache.getInstance();
    }
    
    protected OfferCache getOfferCache() {
        return OfferCache.getInstance();
    }

    @Override
    public String getProducerEventId() {
        return this.identifier;
    }

    @Override
    public String getEventHandler() {
        return EventHandlerAction.TRADE_EVENT;
    }

    /**
     * Fetch the trade from cache
     * @param raiseException Whether to raise an exception if not found
     * @return Optional containing the trade if found
     */
    public Optional<Trade> fetchTrade(boolean raiseException) {
        Optional<Trade> trade = getTradeCache().getTrade(identifier);
        if (raiseException && !trade.isPresent()) {
            throw new IllegalStateException("Trade not found identifier: " + identifier);
        }
        return trade;
    }
    
    /**
     * Get buyer account
     * @return Optional containing the account if found
     */
    public Optional<Account> getBuyerAccount() {
        return getAccountCache().getAccount(buyerAccountKey);
    }
    
    /**
     * Get seller account
     * @return Optional containing the account if found
     */
    public Optional<Account> getSellerAccount() {
        return getAccountCache().getAccount(sellerAccountKey);
    }
    
    /**
     * Update buyer account
     * @param account Account to update
     */
    public void updateBuyerAccount(Account account) {
        getAccountCache().updateAccount(account);
    }
    
    /**
     * Update seller account
     * @param account Account to update
     */
    public void updateSellerAccount(Account account) {
        getAccountCache().updateAccount(account);
    }
    
    /**
     * Get offer from the cache
     * @return Optional containing the offer if found
     */
    public Optional<Offer> getOffer() {
        return getOfferCache().getOffer(offerKey);
    }
    
    /**
     * Update offer in the cache
     * @param offer Offer to update
     */
    public void updateOffer(Offer offer) {
        getOfferCache().updateOffer(offer);
    }

    /**
     * Update trade in cache
     * @param trade Trade to update
     */
    public void updateTrade(Trade trade) {
        getTradeCache().updateTrade(trade);
    }

    /**
     * Convert this event to a Trade object
     * @param raiseException Whether to raise an exception if not found
     * @return The created or fetched Trade
     */
    public Trade toTrade(boolean raiseException) {
        Trade trade = fetchTrade(raiseException).orElseGet(() -> {
            Trade newTrade = new Trade();
            newTrade.setIdentifier(identifier);
            newTrade.setOfferKey(offerKey);
            newTrade.setBuyerAccountKey(buyerAccountKey);
            newTrade.setSellerAccountKey(sellerAccountKey);
            newTrade.setSymbol(symbol);
            newTrade.setPrice(price);
            newTrade.setCoinAmount(coinAmount);
            
            // Cập nhật thêm các trường mới
            newTrade.setAmountAfterFee(amountAfterFee);
            newTrade.setFeeRatio(feeRatio);
            newTrade.setTotalFee(totalFee);
            newTrade.setFixedFee(fixedFee);
            newTrade.setCoinTradingFee(coinTradingFee);
            newTrade.setPaymentMethod(paymentMethod);
            newTrade.setRef(ref);
            newTrade.setCoinCurrency(coinCurrency);
            newTrade.setFiatCurrency(fiatCurrency);
            newTrade.setPaymentProofStatus(paymentProofStatus);
            newTrade.setHasPaymentProof(hasPaymentProof);
            newTrade.setPaidAt(paidAt);
            newTrade.setReleasedAt(releasedAt);
            newTrade.setDisputedAt(disputedAt);
            
            // Tính fiatAmount nếu chưa có (coinAmount * price)
            if (newTrade.getFiatAmount() == null && newTrade.getCoinAmount() != null && newTrade.getPrice() != null) {
                newTrade.setFiatAmount(newTrade.getCoinAmount().multiply(newTrade.getPrice()));
            }
            
            newTrade.setTakerSide(takerSide);
            newTrade.setStatus(Trade.TradeStatus.UNPAID);
            newTrade.setCreatedAt(createdAt != null ? createdAt : Instant.now());
            newTrade.setUpdatedAt(updatedAt != null ? updatedAt : Instant.now());
            newTrade.setCompletedAt(completedAt);
            newTrade.setCancelledAt(cancelledAt);
            return newTrade;
        });
        return trade;
    }

    /**
     * Parse data from a JSON message into this event
     * @param messageJson The JSON message to parse
     * @return This event with parsed data
     */
    public TradeEvent parserData(JsonNode messageJson) {
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
        
        // Handle offer key
        this.offerKey = messageJson.path("offerKey").asText();
        
        // Handle account keys
        this.buyerAccountKey = messageJson.path("buyerAccountKey").asText();
        this.sellerAccountKey = messageJson.path("sellerAccountKey").asText();
        
        // Ref
        this.ref = messageJson.path("ref").asText();
        
        // Currency
        String coinCurrency = messageJson.path("coinCurrency").asText();
        String fiatCurrency = messageJson.path("fiatCurrency").asText();
        this.coinCurrency = coinCurrency;
        this.fiatCurrency = fiatCurrency;
        if (!coinCurrency.isEmpty() && !fiatCurrency.isEmpty()) {
            this.symbol = coinCurrency + ":" + fiatCurrency;
        }
        
        // Handle trade price
        if (messageJson.has("price")) {
            JsonNode node = messageJson.get("price");
            this.price = node.isTextual() 
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        // Set quantity from coinAmount
        if (messageJson.has("coinAmount")) {
            JsonNode node = messageJson.get("coinAmount");
            this.coinAmount = node.isTextual()
                ? new BigDecimal(node.asText())
                : BigDecimal.valueOf(node.asDouble());
        }
        
        // Set fiatAmount
        if (messageJson.has("fiatAmount")) {
            JsonNode node = messageJson.get("fiatAmount");
            this.fiatAmount = node.isTextual()
                ? new BigDecimal(node.asText())
                : node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
        }
        
        // Set feeRatio
        if (messageJson.has("feeRatio")) {
            JsonNode node = messageJson.get("feeRatio");
            this.feeRatio = node.isTextual()
                ? new BigDecimal(node.asText())
                : node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
        }
        
        // Set totalFee
        if (messageJson.has("totalFee")) {
            JsonNode node = messageJson.get("totalFee");
            this.totalFee = node.isTextual()
                ? new BigDecimal(node.asText())
                : node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
        }
        
        // Set fixedFee
        if (messageJson.has("fixedFee")) {
            JsonNode node = messageJson.get("fixedFee");
            this.fixedFee = node.isTextual()
                ? new BigDecimal(node.asText())
                : node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
        }
        
        // Set amountAfterFee
        if (messageJson.has("amountAfterFee")) {
            JsonNode node = messageJson.get("amountAfterFee");
            this.amountAfterFee = node.isTextual()
                ? new BigDecimal(node.asText())
                : node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
        }
        
        // Set coinTradingFee
        if (messageJson.has("coinTradingFee")) {
            JsonNode node = messageJson.get("coinTradingFee");
            this.coinTradingFee = node.isTextual()
                ? new BigDecimal(node.asText())
                : node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
        }
        
        // Set paymentMethod
        this.paymentMethod = messageJson.path("paymentMethod").asText();
        
        // Set payment proof related fields
        this.paymentProofStatus = messageJson.path("paymentProofStatus").asText();
        this.hasPaymentProof = messageJson.path("hasPaymentProof").asBoolean();
        
        // Set taker side
        this.takerSide = messageJson.path("takerSide").asText();
        
        // Set status
        this.status = messageJson.path("status").asText();
        
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
            
            // Use current time for updatedAt if not present
            this.updatedAt = Instant.now();
            
            // Handle paidAt
            if (messageJson.has("paidAt") && !messageJson.path("paidAt").isNull()) {
                JsonNode paidAtNode = messageJson.get("paidAt");
                if (paidAtNode.isNumber()) {
                    // Unix timestamp (seconds)
                    this.paidAt = Instant.ofEpochSecond(paidAtNode.asLong());
                } else {
                    String paidAtStr = paidAtNode.asText();
                    if (!paidAtStr.isEmpty()) {
                        try {
                            // Try parsing as ISO-8601
                            this.paidAt = Instant.parse(paidAtStr);
                        } catch (DateTimeParseException e) {
                            // Try parsing as Unix timestamp
                            this.paidAt = Instant.ofEpochSecond(Long.parseLong(paidAtStr));
                        }
                    }
                }
            }
            
            // Handle releasedAt
            if (messageJson.has("releasedAt") && !messageJson.path("releasedAt").isNull()) {
                JsonNode releasedAtNode = messageJson.get("releasedAt");
                if (releasedAtNode.isNumber()) {
                    // Unix timestamp (seconds)
                    this.releasedAt = Instant.ofEpochSecond(releasedAtNode.asLong());
                } else {
                    String releasedAtStr = releasedAtNode.asText();
                    if (!releasedAtStr.isEmpty()) {
                        try {
                            // Try parsing as ISO-8601
                            this.releasedAt = Instant.parse(releasedAtStr);
                        } catch (DateTimeParseException e) {
                            // Try parsing as Unix timestamp
                            this.releasedAt = Instant.ofEpochSecond(Long.parseLong(releasedAtStr));
                        }
                    }
                }
            }
            
            // Handle cancelledAt 
            if (messageJson.has("cancelledAt") && !messageJson.path("cancelledAt").isNull()) {
                JsonNode cancelledAtNode = messageJson.get("cancelledAt");
                if (cancelledAtNode.isNumber()) {
                    // Unix timestamp (seconds)
                    this.cancelledAt = Instant.ofEpochSecond(cancelledAtNode.asLong());
                } else {
                    String cancelledAtStr = cancelledAtNode.asText();
                    if (!cancelledAtStr.isEmpty()) {
                        try {
                            // Try parsing as ISO-8601
                            this.cancelledAt = Instant.parse(cancelledAtStr);
                        } catch (DateTimeParseException e) {
                            // Try parsing as Unix timestamp
                            this.cancelledAt = Instant.ofEpochSecond(Long.parseLong(cancelledAtStr));
                        }
                    }
                }
            }
            
            // Handle completedAt via releasedAt
            if (this.releasedAt != null) {
                this.completedAt = this.releasedAt;
            }
            
            // Handle disputedAt
            if (messageJson.has("disputedAt") && !messageJson.path("disputedAt").isNull()) {
                JsonNode disputedAtNode = messageJson.get("disputedAt");
                if (disputedAtNode.isNumber()) {
                    // Unix timestamp (seconds)
                    this.disputedAt = Instant.ofEpochSecond(disputedAtNode.asLong());
                } else {
                    String disputedAtStr = disputedAtNode.asText();
                    if (!disputedAtStr.isEmpty()) {
                        try {
                            // Try parsing as ISO-8601
                            this.disputedAt = Instant.parse(disputedAtStr);
                        } catch (DateTimeParseException e) {
                            // Try parsing as Unix timestamp
                            this.disputedAt = Instant.ofEpochSecond(Long.parseLong(disputedAtStr));
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
            if (!OperationType.TRADE_OPERATIONS.contains(getOperationType())) {
                objectErrors.add("Operation type is not supported in list: [" + OperationType.getSupportedTradeValues() + "]");
            }

            if (!ActionType.TRADE.isEqualTo(getActionType().getValue())) {
                objectErrors.add("Action type not matched: expected Trade, got " + getActionType().getValue());
            }
            
            // Validate required fields
            if (identifier == null || identifier.trim().isEmpty()) {
                objectErrors.add("identifier is required");
            }
            
            if (buyerAccountKey == null || buyerAccountKey.trim().isEmpty()) {
                objectErrors.add("buyerAccountKey is required");
            }
            
            if (sellerAccountKey == null || sellerAccountKey.trim().isEmpty()) {
                objectErrors.add("sellerAccountKey is required");
            }
            
            if (symbol == null || symbol.trim().isEmpty()) {
                objectErrors.add("symbol is required");
            }
            
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                objectErrors.add("price must be greater than 0");
            }
            
            if (coinAmount == null || coinAmount.compareTo(BigDecimal.ZERO) <= 0) {
                objectErrors.add("coinAmount must be greater than 0");
            }
            
            if (takerSide == null || takerSide.trim().isEmpty()) {
                objectErrors.add("takerSide is required");
            } else if (!Trade.TAKER_SIDE_BUY.equalsIgnoreCase(takerSide) 
                    && !Trade.TAKER_SIDE_SELL.equalsIgnoreCase(takerSide)) {
                objectErrors.add("Taker side must be either BUY or SELL");
            }
        }

        if (objectErrors.size() > 0) {
            throw new IllegalArgumentException("validate TradeEvent: " + String.join(", ", objectErrors));
        }
    }

    @Override
    public Map<String, Object> toOperationObjectMessageJson() {
        Map<String, Object> messageJson = super.toOperationObjectMessageJson();
        messageJson.put("actionType", getActionType().getValue());
        messageJson.put("actionId", getActionId());
        messageJson.put("operationType", getOperationType().getValue());
        messageJson.put("object", fetchTrade(true).map(Trade::toMessageJson).orElse(null));
        return messageJson;
    }
}
