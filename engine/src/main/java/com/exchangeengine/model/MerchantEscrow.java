package com.exchangeengine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.exchangeengine.util.JsonSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model cho Merchant Escrow
 * Đại diện cho một giao dịch escrow giữa merchant và user
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantEscrow {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_FAILED = "FAILED";

    @NotBlank(message = "identifier is required")
    private String identifier;

    @NotBlank(message = "usdtAccountKey is required")
    private String usdtAccountKey;

    @NotBlank(message = "fiatAccountKey is required")
    private String fiatAccountKey;

    @NotNull(message = "operationType is required")
    private OperationType operationType;

    @NotNull(message = "usdtAmount is required")
    @Positive(message = "usdtAmount must be greater than 0")
    private BigDecimal usdtAmount;

    @NotNull(message = "fiatAmount is required")
    @Positive(message = "fiatAmount must be greater than 0")
    private BigDecimal fiatAmount;

    @NotBlank(message = "fiatCurrency is required")
    private String fiatCurrency;

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "merchantEscrowOperationId is required")
    private String merchantEscrowOperationId;

    @NotBlank(message = "status is required")
    private String status = STATUS_PENDING;

    private long createdAt = Instant.now().toEpochMilli();
    private long updatedAt = Instant.now().toEpochMilli();

    private String statusExplanation = "";

    /**
     * Constructor with essential parameters
     *
     * @param identifier                Unique identifier for merchant escrow
     * @param usdtAccountKey            USDT account key
     * @param fiatAccountKey            Fiat account key
     * @param operationType             Operation type
     * @param usdtAmount                USDT amount
     * @param fiatAmount                Fiat amount
     * @param fiatCurrency              Fiat currency code
     * @param userId                    User ID who initiated the escrow
     * @param merchantEscrowOperationId Merchant escrow operation ID
     */
    public MerchantEscrow(String identifier, String usdtAccountKey, String fiatAccountKey,
            OperationType operationType, BigDecimal usdtAmount, BigDecimal fiatAmount,
            String fiatCurrency, String userId, String merchantEscrowOperationId) {
        this.identifier = identifier;
        this.usdtAccountKey = usdtAccountKey;
        this.fiatAccountKey = fiatAccountKey;
        this.operationType = operationType;
        this.usdtAmount = usdtAmount;
        this.fiatAmount = fiatAmount;
        this.fiatCurrency = fiatCurrency;
        this.userId = userId;
        this.merchantEscrowOperationId = merchantEscrowOperationId;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.status = STATUS_PENDING;
        validateRequiredFields();
    }

    /**
     * Check if status is pending
     * 
     * @return true if status is pending
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /**
     * Check if status is active/completed
     * 
     * @return true if status is active
     */
    public boolean isActive() {
        return STATUS_COMPLETED.equals(status);
    }

    /**
     * Activate the escrow (change status to completed)
     */
    public void activate() {
        this.status = STATUS_COMPLETED;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    /**
     * Cancel the escrow (change status to cancelled)
     */
    public void cancel() {
        this.status = STATUS_CANCELLED;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    /**
     * Cập nhật trạng thái
     *
     * @param newStatus   Trạng thái mới
     * @param description Mô tả lý do thay đổi trạng thái
     */
    public void updateStatus(String newStatus, String description) {
        if (!newStatus.equals(this.status)) {
            this.status = newStatus;
            this.statusExplanation = description;
        }
    }

    /**
     * Validate các trường bắt buộc
     *
     * @return Danh sách các lỗi validate
     */
    public List<String> validateRequiredFields() {
        List<String> errors = new ArrayList<>();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<MerchantEscrow>> violations = validator.validate(this);

        errors.addAll(violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList()));

        // Thêm các quy tắc validation tùy chỉnh
        if (usdtAccountKey != null && fiatAccountKey != null && usdtAccountKey.equals(fiatAccountKey)) {
            errors.add("USDT account and Fiat account must be different");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validate MerchantEscrow: " + String.join(", ", errors));
        }

        return errors;
    }

    /**
     * Chuyển đổi sang message JSON
     *
     * @return Message JSON
     */
    public Map<String, Object> toMessageJson() {
        return JsonSerializer.toMap(this);
    }
}
