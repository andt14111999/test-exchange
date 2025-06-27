package com.exchangeengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.exchangeengine.util.JsonSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = "identifier")
public class Trade {
    // Status constants
    public static final String STATUS_UNPAID = "UNPAID";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    
    // Taker side constants
    public static final String TAKER_SIDE_BUY = "BUY";
    public static final String TAKER_SIDE_SELL = "SELL";

    @NotBlank(message = "Trade ID is required")
    private String identifier;
    
    private String offerKey;
    
    @NotBlank(message = "Buyer account key is required")
    private String buyerAccountKey;
    
    @NotBlank(message = "Seller account key is required")
    private String sellerAccountKey;
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;
    
    @NotNull(message = "Coin amount is required")
    @Positive(message = "Coin amount must be greater than 0")
    private BigDecimal coinAmount;
    
    // Thêm các trường mới
    private BigDecimal fiatAmount;
    private BigDecimal feeRatio;
    private BigDecimal totalFee;
    private BigDecimal fixedFee;
    private BigDecimal amountAfterFee;
    private BigDecimal coinTradingFee;
    private String paymentMethod;
    private String ref;
    private String coinCurrency;
    private String fiatCurrency;
    private String paymentProofStatus;
    private Boolean hasPaymentProof;
    private Instant paidAt;
    private Instant releasedAt;
    private Instant disputedAt;
    
    @NotNull(message = "Status is required")
    private TradeStatus status;
    
    @NotBlank(message = "Taker side is required")
    private String takerSide;
    
    @NotNull(message = "Created at timestamp is required")
    private Instant createdAt;
    
    @NotNull(message = "Updated at timestamp is required")
    private Instant updatedAt;
    
    private Instant completedAt;
    private Instant cancelledAt;
    
    private String statusExplanation;

    public enum TradeStatus {
        UNPAID, COMPLETED, CANCELLED
    }
    
    /**
     * Check if trade is unpaid (waiting for buyer to pay)
     * @return true if trade is unpaid
     */
    public boolean isUnpaid() {
        return status == TradeStatus.UNPAID;
    }
    
    /**
     * Check if trade is completed
     * @return true if trade is completed
     */
    public boolean isCompleted() {
        return status == TradeStatus.COMPLETED;
    }
    
    /**
     * Check if trade is cancelled
     * @return true if trade is cancelled
     */
    public boolean isCancelled() {
        return status == TradeStatus.CANCELLED;
    }
    
    /**
     * Check if buyer is the taker
     * @return true if the buyer is the taker
     */
    public boolean isBuyerTaker() {
        return TAKER_SIDE_BUY.equalsIgnoreCase(takerSide);
    }
    
    /**
     * Check if seller is the taker
     * @return true if the seller is the taker
     */
    public boolean isSellerTaker() {
        return TAKER_SIDE_SELL.equalsIgnoreCase(takerSide);
    }
    
    /**
     * Complete the trade
     */
    public void complete() {
        this.status = TradeStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Cancel the trade
     */
    public void cancel() {
        this.status = TradeStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update trade status
     * @param newStatus New status
     * @param description Status explanation
     */
    public void updateStatus(TradeStatus newStatus, String description) {
        if (newStatus != this.status) {
            this.status = newStatus;
            this.statusExplanation = description;
            this.updatedAt = Instant.now();
            
            // Update timestamp based on status
            if (newStatus == TradeStatus.COMPLETED) {
                this.completedAt = Instant.now();
            } else if (newStatus == TradeStatus.CANCELLED) {
                this.cancelledAt = Instant.now();
            }
        }
    }
    
    /**
     * Get createdAt as epoch milliseconds
     * @return createdAt as epoch milliseconds
     */
    public long getCreatedAtEpochMilli() {
        return createdAt != null ? createdAt.toEpochMilli() : 0;
    }
    
    /**
     * Get updatedAt as epoch milliseconds
     * @return updatedAt as epoch milliseconds
     */
    public long getUpdatedAtEpochMilli() {
        return updatedAt != null ? updatedAt.toEpochMilli() : 0;
    }
    
    /**
     * Get completedAt as epoch milliseconds
     * @return completedAt as epoch milliseconds
     */
    public Long getCompletedAtEpochMilli() {
        return completedAt != null ? completedAt.toEpochMilli() : null;
    }
    
    /**
     * Get cancelledAt as epoch milliseconds
     * @return cancelledAt as epoch milliseconds
     */
    public Long getCancelledAtEpochMilli() {
        return cancelledAt != null ? cancelledAt.toEpochMilli() : null;
    }
    
    /**
     * Convert to message JSON
     * 
     * @return Map with trade data
     */
    public Map<String, Object> toMessageJson() {
      return JsonSerializer.toMap(this);
    }

    /**
     * Validate required fields using Jakarta Validation
     * @return List of validation errors
     */
    public List<String> validateRequiredFields() {
        List<String> errors = new ArrayList<>();
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<Trade>> violations = validator.validate(this);
        
        errors.addAll(violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList()));
        
        // Additional validations
        if (takerSide != null && !TAKER_SIDE_BUY.equalsIgnoreCase(takerSide) && 
            !TAKER_SIDE_SELL.equalsIgnoreCase(takerSide)) {
            errors.add("Taker side must be either BUY or SELL");
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validate Trade: " + String.join(", ", errors));
        }
        
        return errors;
    }
}
