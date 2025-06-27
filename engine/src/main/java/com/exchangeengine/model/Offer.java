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
public class Offer {
    // Status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PARTIALLY_FILLED = "PARTIALLY_FILLED";
    public static final String STATUS_FILLED = "FILLED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @NotBlank(message = "Offer ID is required")
    private String identifier;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotNull(message = "Offer type is required")
    private OfferType type;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;
    
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;
    
    @NotNull(message = "Status is required")
    private OfferStatus status;
    
    @NotNull(message = "Created at timestamp is required")
    private Instant createdAt;
    
    @NotNull(message = "Updated at timestamp is required")
    private Instant updatedAt;
    
    private Boolean disabled;
    private Boolean deleted;
    private Boolean automatic;
    private Boolean online;
    private BigDecimal margin;
    private String paymentMethodId;
    private Integer paymentTime;
    private String countryCode;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    
    @NotNull(message = "Available amount is required")
    private BigDecimal availableAmount;
    
    private String statusExplanation = "";

    public Offer(String id, String userId, String offerType, String coinCurrency, String fiatCurrency,
                BigDecimal price, BigDecimal minAmount, BigDecimal maxAmount, BigDecimal totalAmount,
                BigDecimal availableAmount, String paymentMethodId, Integer paymentTime, String countryCode,
                Boolean disabled, Boolean deleted, Boolean automatic, Boolean online, BigDecimal margin,
                Instant createdAt, Instant updatedAt) {
        this.identifier = id;
        this.userId = userId;
        this.symbol = coinCurrency + ":" + fiatCurrency;
        this.type = OfferType.valueOf(offerType.toUpperCase());
        this.price = price;
        this.totalAmount = totalAmount;
        this.availableAmount = availableAmount;
        this.status = OfferStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.disabled = disabled;
        this.deleted = deleted;
        this.automatic = automatic;
        this.online = online;
        this.margin = margin;
        this.paymentMethodId = paymentMethodId;
        this.paymentTime = paymentTime;
        this.countryCode = countryCode;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        validateRequiredFields();
    }

    public enum OfferType {
        BUY, SELL
    }

    public enum OfferStatus {
        PENDING, PARTIALLY_FILLED, FILLED, CANCELLED
    }

    /**
     * Check if offer is active (can be filled)
     * @return true if offer is active
     */
    public boolean isActive() {
        return status == OfferStatus.PENDING || status == OfferStatus.PARTIALLY_FILLED;
    }

    /**
     * Check if offer is fully filled
     * @return true if offer is fully filled
     */
    public boolean isFullyFilled() {
        return status == OfferStatus.FILLED;
    }

    /**
     * Check if offer can be filled with the requested quantity
     * @param requestedQuantity Quantity to check
     * @return true if offer can be filled
     */
    public boolean canBeFilled(BigDecimal requestedQuantity) {
        return availableAmount.compareTo(requestedQuantity) >= 0;
    }

    /**
     * Mark offer as filled
     */
    public void fill() {
        this.status = OfferStatus.FILLED;
        this.availableAmount = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark offer as partially filled
     * @param filledQuantity Quantity that was filled
     */
    public void partiallyFill(BigDecimal filledQuantity) {
        this.availableAmount = this.availableAmount.subtract(filledQuantity);
        this.status = OfferStatus.PARTIALLY_FILLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Cancel the offer
     */
    public void cancel() {
        this.status = OfferStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Update offer status
     * @param newStatus New status
     * @param description Status explanation
     */
    public void updateStatus(OfferStatus newStatus, String description) {
        if (newStatus != this.status) {
            this.status = newStatus;
            this.statusExplanation = description;
            this.updatedAt = Instant.now();
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
     * Convert to message JSON
     * @return Message JSON
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
        Set<ConstraintViolation<Offer>> violations = validator.validate(this);
        
        errors.addAll(violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList()));
        
        // Additional validations
        if (availableAmount != null && totalAmount != null && availableAmount.compareTo(totalAmount) > 0) {
            errors.add("Available amount cannot be greater than total amount");
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validate Offer: " + String.join(", ", errors));
        }
        
        return errors;
    }
}
