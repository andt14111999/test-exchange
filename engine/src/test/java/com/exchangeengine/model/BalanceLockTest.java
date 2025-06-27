package com.exchangeengine.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exchangeengine.factory.BalanceLockFactory;
import com.exchangeengine.extension.CombinedTestExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, CombinedTestExtension.class})
@DisplayName("BalanceLock Tests")
class BalanceLockTest {

    @Test
    @DisplayName("Constructor with required parameters should initialize all fields correctly")
    void constructor_ShouldInitializeAllFieldsCorrectly() {
        // Arrange
        ActionType actionType = ActionType.TRADE;
        String actionId = "test-action-id";
        List<String> accountKeys = Arrays.asList("account1", "account2");
        String identifier = "test-identifier";

        // Act
        BalanceLock balanceLock = new BalanceLock(actionType, actionId, accountKeys, identifier);

        // Assert
        assertNotNull(balanceLock.getLockId());
        assertEquals(actionType, balanceLock.getActionType());
        assertEquals(actionId, balanceLock.getActionId());
        assertEquals(accountKeys, balanceLock.getAccountKeys());
        assertEquals(identifier, balanceLock.getIdentifier());
        assertNotNull(balanceLock.getLockedBalances());
        assertTrue(balanceLock.getLockedBalances().isEmpty());
        assertTrue(balanceLock.getCreatedAt() > 0);
        assertEquals("LOCKED", balanceLock.getStatus());
    }

    @Test
    @DisplayName("Constructor with full parameters should initialize all fields correctly")
    void constructorWithFullParameters_ShouldInitializeAllFieldsCorrectly() {
        // Arrange
        ActionType actionType = ActionType.OFFER;
        String actionId = "test-action-id";
        String lockId = "test-lock-id";
        List<String> accountKeys = Arrays.asList("account1", "account2");
        String identifier = "test-identifier";
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        lockedBalances.put("account1", new BigDecimal("100.00"));
        lockedBalances.put("account2", new BigDecimal("200.00"));
        String status = "RELEASED";

        // Act
        BalanceLock balanceLock = new BalanceLock(actionType, actionId, lockId, accountKeys, identifier, lockedBalances, status);

        // Assert
        assertEquals(lockId, balanceLock.getLockId());
        assertEquals(actionType, balanceLock.getActionType());
        assertEquals(actionId, balanceLock.getActionId());
        assertEquals(accountKeys, balanceLock.getAccountKeys());
        assertEquals(identifier, balanceLock.getIdentifier());
        assertEquals(lockedBalances, balanceLock.getLockedBalances());
        assertTrue(balanceLock.getCreatedAt() > 0);
        assertEquals(status, balanceLock.getStatus());
    }

    @Test
    @DisplayName("Constructor with null locked balances should initialize empty map")
    void constructorWithNullLockedBalances_ShouldInitializeEmptyMap() {
        // Arrange
        ActionType actionType = ActionType.TRADE;
        String actionId = "test-action-id";
        String lockId = "test-lock-id";
        List<String> accountKeys = Arrays.asList("account1");
        String identifier = "test-identifier";
        String status = "LOCKED";

        // Act
        BalanceLock balanceLock = new BalanceLock(actionType, actionId, lockId, accountKeys, identifier, null, status);

        // Assert
        assertNotNull(balanceLock.getLockedBalances());
        assertTrue(balanceLock.getLockedBalances().isEmpty());
    }

    @Test
    @DisplayName("validate should return empty list for valid BalanceLock")
    void validate_ShouldReturnEmptyList_ForValidBalanceLock() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("validate should return error when lockId is null")
    void validate_ShouldReturnError_WhenLockIdIsNull() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setLockId(null);

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertEquals(1, errors.size());
        assertEquals("LockId is required", errors.get(0));
    }

    @Test
    @DisplayName("validate should return error when lockId is empty")
    void validate_ShouldReturnError_WhenLockIdIsEmpty() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setLockId("");

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertEquals(1, errors.size());
        assertEquals("LockId is required", errors.get(0));
    }

    @Test
    @DisplayName("validate should return error when accountKeys is null")
    void validate_ShouldReturnError_WhenAccountKeysIsNull() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setAccountKeys(null);

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertEquals(1, errors.size());
        assertEquals("AccountKeys list is required and cannot be empty", errors.get(0));
    }

    @Test
    @DisplayName("validate should return error when accountKeys is empty")
    void validate_ShouldReturnError_WhenAccountKeysIsEmpty() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.createWithEmptyAccountKeys();

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertEquals(1, errors.size());
        assertEquals("AccountKeys list is required and cannot be empty", errors.get(0));
    }

    @Test
    @DisplayName("validate should return error when identifier is null")
    void validate_ShouldReturnError_WhenIdentifierIsNull() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setIdentifier(null);

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertEquals(1, errors.size());
        assertEquals("Identifier is required", errors.get(0));
    }

    @Test
    @DisplayName("validate should return error when identifier is empty")
    void validate_ShouldReturnError_WhenIdentifierIsEmpty() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setIdentifier("");

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertEquals(1, errors.size());
        assertEquals("Identifier is required", errors.get(0));
    }

    @Test
    @DisplayName("validate should return error when status is null")
    void validate_ShouldReturnError_WhenStatusIsNull() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setStatus(null);

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertEquals(1, errors.size());
        assertEquals("Status is required", errors.get(0));
    }

    @Test
    @DisplayName("validate should return error when status is empty")
    void validate_ShouldReturnError_WhenStatusIsEmpty() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setStatus("");

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertTrue(errors.size() >= 1, "Should have at least one error");
        assertTrue(errors.stream().anyMatch(error -> error.contains("Status is required")),
                "Should contain 'Status is required' error message");
    }

    @Test
    @DisplayName("validate should return error when status is invalid")
    void validate_ShouldReturnError_WhenStatusIsInvalid() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setStatus("INVALID_STATUS");

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertTrue(errors.size() >= 1, "Should have at least one error");
        assertTrue(errors.stream().anyMatch(error -> error.contains("Status must be LOCKED or RELEASED")),
                "Should contain 'Status must be LOCKED or RELEASED' error message");
    }

    @Test
    @DisplayName("validate should return multiple errors when multiple fields are invalid")
    void validate_ShouldReturnMultipleErrors_WhenMultipleFieldsAreInvalid() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.createWithNullFields();

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertTrue(errors.size() >= 4, "Should have at least 4 errors");
        assertTrue(errors.stream().anyMatch(error -> error.contains("LockId is required")),
                "Should contain 'LockId is required' error message");
        assertTrue(errors.stream().anyMatch(error -> error.contains("AccountKeys list is required")),
                "Should contain 'AccountKeys list is required' error message");
        assertTrue(errors.stream().anyMatch(error -> error.contains("Identifier is required")),
                "Should contain 'Identifier is required' error message");
        assertTrue(errors.stream().anyMatch(error -> error.contains("Status is required")),
                "Should contain 'Status is required' error message");
    }

    @Test
    @DisplayName("addLockedBalance should add balance for valid account and amount")
    void addLockedBalance_ShouldAddBalance_ForValidAccountAndAmount() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        String accountKey = "test-account";
        BigDecimal amount = new BigDecimal("150.00");

        // Act
        balanceLock.addLockedBalance(accountKey, amount);

        // Assert
        assertEquals(amount, balanceLock.getLockedBalances().get(accountKey));
    }

    @Test
    @DisplayName("addLockedBalance should not add balance when accountKey is null")
    void addLockedBalance_ShouldNotAddBalance_WhenAccountKeyIsNull() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        BigDecimal amount = new BigDecimal("150.00");
        int originalSize = balanceLock.getLockedBalances().size();

        // Act
        balanceLock.addLockedBalance(null, amount);

        // Assert
        assertEquals(originalSize, balanceLock.getLockedBalances().size());
    }

    @Test
    @DisplayName("addLockedBalance should not add balance when amount is null")
    void addLockedBalance_ShouldNotAddBalance_WhenAmountIsNull() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        String accountKey = "test-account";
        int originalSize = balanceLock.getLockedBalances().size();

        // Act
        balanceLock.addLockedBalance(accountKey, null);

        // Assert
        assertEquals(originalSize, balanceLock.getLockedBalances().size());
    }

    @Test
    @DisplayName("addLockedBalance should not add balance when amount is zero")
    void addLockedBalance_ShouldNotAddBalance_WhenAmountIsZero() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        String accountKey = "test-account";
        BigDecimal amount = BigDecimal.ZERO;
        int originalSize = balanceLock.getLockedBalances().size();

        // Act
        balanceLock.addLockedBalance(accountKey, amount);

        // Assert
        assertEquals(originalSize, balanceLock.getLockedBalances().size());
    }

    @Test
    @DisplayName("addLockedBalance should not add balance when amount is negative")
    void addLockedBalance_ShouldNotAddBalance_WhenAmountIsNegative() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        String accountKey = "test-account";
        BigDecimal amount = new BigDecimal("-50.00");
        int originalSize = balanceLock.getLockedBalances().size();

        // Act
        balanceLock.addLockedBalance(accountKey, amount);

        // Assert
        assertEquals(originalSize, balanceLock.getLockedBalances().size());
    }

    @Test
    @DisplayName("containsAccountKey should return true when account key exists")
    void containsAccountKey_ShouldReturnTrue_WhenAccountKeyExists() {
        // Arrange
        List<String> accountKeys = Arrays.asList("account1", "account2", "account3");
        BalanceLock balanceLock = BalanceLockFactory.create(ActionType.TRADE, "action-id", accountKeys, "identifier");

        // Act & Assert
        assertTrue(balanceLock.containsAccountKey("account1"));
        assertTrue(balanceLock.containsAccountKey("account2"));
        assertTrue(balanceLock.containsAccountKey("account3"));
    }

    @Test
    @DisplayName("containsAccountKey should return false when account key does not exist")
    void containsAccountKey_ShouldReturnFalse_WhenAccountKeyDoesNotExist() {
        // Arrange
        List<String> accountKeys = Arrays.asList("account1", "account2");
        BalanceLock balanceLock = BalanceLockFactory.create(ActionType.TRADE, "action-id", accountKeys, "identifier");

        // Act & Assert
        assertFalse(balanceLock.containsAccountKey("account3"));
        assertFalse(balanceLock.containsAccountKey("nonexistent"));
    }

    @Test
    @DisplayName("containsAccountKey should return false when accountKeys is null")
    void containsAccountKey_ShouldReturnFalse_WhenAccountKeysIsNull() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setAccountKeys(null);

        // Act & Assert
        assertFalse(balanceLock.containsAccountKey("account1"));
    }

    @Test
    @DisplayName("getLockedBalanceForAccount should return correct balance when account exists")
    void getLockedBalanceForAccount_ShouldReturnCorrectBalance_WhenAccountExists() {
        // Arrange
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        lockedBalances.put("account1", new BigDecimal("100.00"));
        lockedBalances.put("account2", new BigDecimal("200.00"));
        
        BalanceLock balanceLock = BalanceLockFactory.createWithBalances(
            ActionType.TRADE, "action-id", Arrays.asList("account1", "account2"), "identifier", lockedBalances);

        // Act & Assert
        assertEquals(new BigDecimal("100.00"), balanceLock.getLockedBalanceForAccount("account1"));
        assertEquals(new BigDecimal("200.00"), balanceLock.getLockedBalanceForAccount("account2"));
    }

    @Test
    @DisplayName("getLockedBalanceForAccount should return zero when account does not exist")
    void getLockedBalanceForAccount_ShouldReturnZero_WhenAccountDoesNotExist() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();

        // Act & Assert
        assertEquals(BigDecimal.ZERO, balanceLock.getLockedBalanceForAccount("nonexistent"));
    }

    @Test
    @DisplayName("toMessageJson should return correct JSON representation")
    void toMessageJson_ShouldReturnCorrectJsonRepresentation() {
        // Arrange
        ActionType actionType = ActionType.OFFER;
        String actionId = "test-action-id";
        String lockId = "test-lock-id";
        List<String> accountKeys = Arrays.asList("account1", "account2");
        String identifier = "test-identifier";
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        lockedBalances.put("account1", new BigDecimal("100.00"));
        lockedBalances.put("account2", new BigDecimal("200.00"));
        String status = "LOCKED";
        
        BalanceLock balanceLock = new BalanceLock(actionType, actionId, lockId, accountKeys, identifier, lockedBalances, status);

        // Act
        Map<String, Object> json = balanceLock.toMessageJson();

        // Assert
        assertEquals(lockId, json.get("lockId"));
        assertEquals(accountKeys, json.get("accountKeys"));
        assertEquals(identifier, json.get("identifier"));
        assertTrue(
            actionType.getValue().equals(json.get("actionType")) || 
            actionType.name().equals(json.get("actionType")),
            "ActionType should be either the value from getValue() or the enum name"
        );
        assertEquals(actionId, json.get("actionId"));
        assertEquals(lockedBalances, json.get("lockedBalances"));
        assertEquals(status, json.get("status"));
        assertNotNull(json.get("createdAt"));
        assertTrue((Long) json.get("createdAt") > 0);
    }

    @Test
    @DisplayName("Status LOCKED should be valid")
    void statusLocked_ShouldBeValid() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.create();
        balanceLock.setStatus("LOCKED");

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Status RELEASED should be valid")
    void statusReleased_ShouldBeValid() {
        // Arrange
        BalanceLock balanceLock = BalanceLockFactory.createReleased();

        // Act
        List<String> errors = balanceLock.validate();

        // Assert
        assertTrue(errors.isEmpty());
        assertEquals("RELEASED", balanceLock.getStatus());
    }

    @Test
    @DisplayName("Constructor should create defensive copy of accountKeys list")
    void constructor_ShouldCreateDefensiveCopyOfAccountKeysList() {
        // Arrange
        List<String> originalAccountKeys = new ArrayList<>(Arrays.asList("account1", "account2"));
        
        // Act
        BalanceLock balanceLock = BalanceLockFactory.create(ActionType.TRADE, "action-id", originalAccountKeys, "identifier");
        
        // Modify original list (this should not affect the BalanceLock)
        originalAccountKeys.add("account3");

        // Assert
        assertEquals(2, balanceLock.getAccountKeys().size());
        assertFalse(balanceLock.containsAccountKey("account3"));
    }

    @Test
    @DisplayName("Full constructor should create defensive copy of accountKeys list")
    void fullConstructor_ShouldCreateDefensiveCopyOfAccountKeysList() {
        // Arrange
        List<String> originalAccountKeys = new ArrayList<>(Arrays.asList("account1", "account2"));
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        
        // Act
        BalanceLock balanceLock = new BalanceLock(ActionType.TRADE, "action-id", "lock-id", 
                                                originalAccountKeys, "identifier", lockedBalances, "LOCKED");
        
        // Modify original list (this should not affect the BalanceLock)
        originalAccountKeys.add("account3");

        // Assert
        assertEquals(2, balanceLock.getAccountKeys().size());
        assertFalse(balanceLock.containsAccountKey("account3"));
    }
} 