package com.exchangeengine.model.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.BalanceLock;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.BalanceLockCache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event để khóa và giải phóng số dư tài khoản
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalancesLockEvent extends BaseEvent {
    private String lockId;
    private List<String> accountKeys;
    private String identifier;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    protected BalanceLockCache getBalanceLockCache() {
        return BalanceLockCache.getInstance();
    }
    
    @Override
    public String getProducerEventId() {
        return this.lockId;
    }
    
    @Override
    public String getEventHandler() {
        return EventHandlerAction.BALANCES_LOCK_EVENT;
    }
    
    /**
     * Lấy BalanceLock từ cache
     *
     * @param raiseException True nếu cần ném exception khi không tìm thấy
     * @return Optional chứa BalanceLock hoặc empty nếu không tìm thấy
     */
    public Optional<BalanceLock> fetchBalanceLock(boolean raiseException) {
        Optional<BalanceLock> lock = lockId != null ? getBalanceLockCache().getBalanceLock(lockId) : Optional.empty();
        if (raiseException && !lock.isPresent()) {
            throw new IllegalStateException("BalanceLock not found with lockId: " + lockId);
        }
        return lock;
    }
    
    /**
     * Tạo mới hoặc lấy BalanceLock hiện có
     *
     * @param raiseException True nếu cần ném exception khi không tìm thấy
     * @return BalanceLock mới hoặc hiện có
     */
    public BalanceLock toBalanceLock(boolean raiseException) {
        BalanceLock lock = fetchBalanceLock(raiseException).orElseGet(() -> {
            if (OperationType.BALANCES_LOCK_CREATE.isEqualTo(getOperationType().getValue()) && lockId != null && !lockId.isEmpty()) {
                // Sử dụng lockId từ event nếu có để tránh lỗi khi publish
                return new BalanceLock(
                    getActionType(),
                    getActionId(),
                    lockId, // Sử dụng lockId từ event
                    accountKeys,
                    identifier,
                    new HashMap<>(), // Khởi tạo lockedBalances trống
                    "LOCKED" // Trạng thái ban đầu là LOCKED
                );
            } else {
                // Nếu không phải CREATE hoặc không có lockId, tạo mới với UUID
                return new BalanceLock(
                    getActionType(),
                    getActionId(),
                    accountKeys,
                    identifier
                );
            }
        });
        
        return lock;
    }
    
    /**
     * Parse dữ liệu từ JsonNode
     *
     * @param messageJson JsonNode chứa dữ liệu
     * @return BalancesLockEvent đã được cập nhật
     */
    public BalancesLockEvent parserData(JsonNode messageJson) {
        String eventId = messageJson.path("eventId").asText();
        ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
        String tmpActionId = messageJson.path("actionId").asText();
        OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());
        String tmpLockId = messageJson.path("lockId").asText();
        String tmpIdentifier = messageJson.path("identifier").asText();
        
        try {
            JsonNode accountKeysNode = messageJson.path("accountKeys");
            List<String> tmpAccountKeys = objectMapper.readValue(
                accountKeysNode.toString(),
                new TypeReference<List<String>>() {}
            );
            this.accountKeys = tmpAccountKeys;
        } catch (Exception e) {
            this.accountKeys = new ArrayList<>();
        }
        
        setEventId(eventId);
        setActionType(tmpActionType);
        setActionId(tmpActionId);
        setOperationType(tmpOperationType);
        this.lockId = tmpLockId;
        this.identifier = tmpIdentifier;
        
        return this;
    }
    
    /**
     * Kiểm tra tính hợp lệ của event
     */
    public void validate() {
        List<String> objectErrors = super.validateRequiredFields();
        
        if (objectErrors.isEmpty()) {
            if (OperationType.BALANCES_LOCK_CREATE.isEqualTo(getOperationType().getValue())) {
                if (accountKeys == null || accountKeys.isEmpty()) {
                    objectErrors.add("AccountKeys list is required for BALANCES_LOCK_CREATE");
                }
                if (identifier == null || identifier.isEmpty()) {
                    objectErrors.add("Identifier is required for BALANCES_LOCK_CREATE");
                }
            } else if (OperationType.BALANCES_LOCK_RELEASE.isEqualTo(getOperationType().getValue())) {
                if (lockId == null || lockId.isEmpty()) {
                    objectErrors.add("LockId is required for BALANCES_LOCK_RELEASE");
                }
            }
        }
        
        if (!objectErrors.isEmpty()) {
            throw new IllegalArgumentException("validate BalancesLockEvent: " + String.join(", ", objectErrors));
        }
    }
    
    @Override
    public Map<String, Object> toOperationObjectMessageJson() {
        Map<String, Object> messageJson = super.toOperationObjectMessageJson();
        messageJson.put("object", fetchBalanceLock(true).map(BalanceLock::toMessageJson).orElse(null));
        
        return messageJson;
    }
} 
