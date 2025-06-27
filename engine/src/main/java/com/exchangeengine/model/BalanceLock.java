package com.exchangeengine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import com.exchangeengine.util.JsonSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model lưu trữ thông tin khóa số dư của người dùng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceLock {
    @NotBlank(message = "LockId is required")
    private String lockId;
    
    @NotEmpty(message = "AccountKeys list is required and cannot be empty")
    private List<String> accountKeys; // Danh sách các tài khoản bị khóa
    
    @NotBlank(message = "Identifier is required")
    private String identifier; // Định danh đối tượng yêu cầu khóa
    
    private Map<String, BigDecimal> lockedBalances; // Mapping accountKey -> lockedAmount
    
    private long createdAt;
    
    private ActionType actionType;
    
    private String actionId;
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "LOCKED|RELEASED", message = "Status must be LOCKED or RELEASED")
    private String status; // "LOCKED", "RELEASED"
    
    /**
     * Constructor với các tham số cần thiết
     * 
     * @param actionType Loại hành động
     * @param actionId ID hành động
     * @param accountKeys Danh sách khóa tài khoản
     * @param identifier Định danh đối tượng yêu cầu khóa
     */
    public BalanceLock(ActionType actionType, String actionId, List<String> accountKeys, String identifier) {
        this.lockId = UUID.randomUUID().toString();
        this.accountKeys = new ArrayList<>(accountKeys);
        this.identifier = identifier;
        this.actionType = actionType;
        this.actionId = actionId;
        this.lockedBalances = new HashMap<>();
        this.createdAt = Instant.now().toEpochMilli();
        this.status = "LOCKED";
    }
    
    /**
     * Constructor đầy đủ
     * 
     * @param actionType Loại hành động
     * @param actionId ID hành động
     * @param lockId ID khóa
     * @param accountKeys Danh sách khóa tài khoản
     * @param identifier Định danh đối tượng yêu cầu khóa
     * @param lockedBalances Map chứa số dư đã khóa
     * @param status Trạng thái khóa
     */
    public BalanceLock(ActionType actionType, String actionId, String lockId, List<String> accountKeys, 
                String identifier, Map<String, BigDecimal> lockedBalances, String status) {
        this.lockId = lockId;
        this.accountKeys = new ArrayList<>(accountKeys);
        this.identifier = identifier;
        this.actionType = actionType;
        this.actionId = actionId;
        this.lockedBalances = lockedBalances != null ? lockedBalances : new HashMap<>();
        this.createdAt = Instant.now().toEpochMilli();
        this.status = status;
    }
    
    /**
     * Kiểm tra tính hợp lệ của BalanceLock
     * 
     * @return Danh sách lỗi nếu có
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<BalanceLock>> violations = validator.validate(this);
        
        for (ConstraintViolation<BalanceLock> violation : violations) {
            errors.add(violation.getMessage());
        }
        
        return errors;
    }
    
    /**
     * Thêm số dư khóa cho một tài khoản
     * 
     * @param accountKey Khóa tài khoản
     * @param amount Số lượng cần khóa
     */
    public void addLockedBalance(String accountKey, BigDecimal amount) {
        if (accountKey != null && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            lockedBalances.put(accountKey, amount);
        }
    }
    
    /**
     * Kiểm tra xem một tài khoản có trong danh sách bị khóa không
     *
     * @param accountKey Khóa tài khoản cần kiểm tra
     * @return true nếu tài khoản nằm trong danh sách bị khóa
     */
    public boolean containsAccountKey(String accountKey) {
        return accountKeys != null && accountKeys.contains(accountKey);
    }
    
    /**
     * Lấy số dư đã khóa cho một tài khoản cụ thể
     *
     * @param accountKey Khóa tài khoản cần lấy số dư
     * @return Số dư đã khóa, BigDecimal.ZERO nếu không tìm thấy
     */
    public BigDecimal getLockedBalanceForAccount(String accountKey) {
        return lockedBalances.getOrDefault(accountKey, BigDecimal.ZERO);
    }
    
    /**
     * Chuyển đổi BalanceLock thành JSON
     * 
     * @return Map chứa dữ liệu JSON
     */
    public Map<String, Object> toMessageJson() {
        return JsonSerializer.toMap(this);
    }
} 
