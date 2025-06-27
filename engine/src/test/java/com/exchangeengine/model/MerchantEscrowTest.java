package com.exchangeengine.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.exchangeengine.factory.MerchantEscrowFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantEscrow Tests")
class MerchantEscrowTest {

    @Test
    @DisplayName("Constructor should initialize all fields correctly")
    void constructor_ShouldInitializeAllFieldsCorrectly() {
        // Arrange
        String identifier = "test-escrow-id";
        String usdtAccountKey = "usdt-account-key";
        String fiatAccountKey = "fiat-account-key";
        OperationType operationType = OperationType.MERCHANT_ESCROW_MINT;
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("3000.00");
        String fiatCurrency = "USD";
        String userId = "test-user-id";
        String merchantEscrowOperationId = "merchant-escrow-op-id";

        // Act
        MerchantEscrow escrow = new MerchantEscrow(
            identifier,
            usdtAccountKey,
            fiatAccountKey,
            operationType,
            usdtAmount,
            fiatAmount,
            fiatCurrency,
            userId,
            merchantEscrowOperationId
        );

        // Assert
        assertEquals(identifier, escrow.getIdentifier());
        assertEquals(usdtAccountKey, escrow.getUsdtAccountKey());
        assertEquals(fiatAccountKey, escrow.getFiatAccountKey());
        assertEquals(operationType, escrow.getOperationType());
        assertEquals(usdtAmount, escrow.getUsdtAmount());
        assertEquals(fiatAmount, escrow.getFiatAmount());
        assertEquals(fiatCurrency, escrow.getFiatCurrency());
        assertEquals(userId, escrow.getUserId());
        assertEquals(merchantEscrowOperationId, escrow.getMerchantEscrowOperationId());
        assertEquals(MerchantEscrow.STATUS_PENDING, escrow.getStatus());
        assertTrue(escrow.getCreatedAt() > 0);
        assertTrue(escrow.getUpdatedAt() > 0);
        assertEquals(escrow.getCreatedAt(), escrow.getUpdatedAt());
        assertEquals("", escrow.getStatusExplanation());
    }

    @Test
    @DisplayName("isPending should return true when status is PENDING")
    void isPending_ShouldReturnTrue_WhenStatusIsPending() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setStatus(MerchantEscrow.STATUS_PENDING);

        // Act & Assert
        assertTrue(escrow.isPending());
    }

    @Test
    @DisplayName("isPending should return false when status is not PENDING")
    void isPending_ShouldReturnFalse_WhenStatusIsNotPending() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        
        // Test all other statuses
        escrow.setStatus(MerchantEscrow.STATUS_COMPLETED);
        assertFalse(escrow.isPending());
        
        escrow.setStatus(MerchantEscrow.STATUS_CANCELLED);
        assertFalse(escrow.isPending());
        
        escrow.setStatus(MerchantEscrow.STATUS_FAILED);
        assertFalse(escrow.isPending());
    }

    @Test
    @DisplayName("isActive should return true when status is COMPLETED")
    void isActive_ShouldReturnTrue_WhenStatusIsCompleted() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setStatus(MerchantEscrow.STATUS_COMPLETED);

        // Act & Assert
        assertTrue(escrow.isActive());
    }

    @Test
    @DisplayName("isActive should return false when status is not COMPLETED")
    void isActive_ShouldReturnFalse_WhenStatusIsNotCompleted() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        
        // Test all other statuses
        escrow.setStatus(MerchantEscrow.STATUS_PENDING);
        assertFalse(escrow.isActive());
        
        escrow.setStatus(MerchantEscrow.STATUS_CANCELLED);
        assertFalse(escrow.isActive());
        
        escrow.setStatus(MerchantEscrow.STATUS_FAILED);
        assertFalse(escrow.isActive());
    }

    @Test
    @DisplayName("activate should change status to COMPLETED and update timestamp")
    void activate_ShouldChangeStatusToCompleted_AndUpdateTimestamp() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        long originalUpdatedAt = escrow.getUpdatedAt();
        
        // Wait to ensure timestamp will be different
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Act
        escrow.activate();
        
        // Assert
        assertEquals(MerchantEscrow.STATUS_COMPLETED, escrow.getStatus());
        assertTrue(escrow.getUpdatedAt() > originalUpdatedAt);
    }

    @Test
    @DisplayName("cancel should change status to CANCELLED and update timestamp")
    void cancel_ShouldChangeStatusToCancelled_AndUpdateTimestamp() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        long originalUpdatedAt = escrow.getUpdatedAt();
        
        // Wait to ensure timestamp will be different
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Act
        escrow.cancel();
        
        // Assert
        assertEquals(MerchantEscrow.STATUS_CANCELLED, escrow.getStatus());
        assertTrue(escrow.getUpdatedAt() > originalUpdatedAt);
    }

    @Test
    @DisplayName("updateStatus should update status when status is different")
    void updateStatus_ShouldUpdateStatus_WhenStatusIsDifferent() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        String newStatus = MerchantEscrow.STATUS_COMPLETED;
        String description = "Status updated for testing";
        
        // Act
        escrow.updateStatus(newStatus, description);
        
        // Assert
        assertEquals(newStatus, escrow.getStatus());
        assertEquals(description, escrow.getStatusExplanation());
    }

    @Test
    @DisplayName("updateStatus should not change anything when status is the same")
    void updateStatus_ShouldNotChangeAnything_WhenStatusIsTheSame() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        String originalStatus = escrow.getStatus();
        String originalExplanation = escrow.getStatusExplanation();
        int originalTransactionCount = 1; // Initial transaction count
        
        // Act
        escrow.updateStatus(originalStatus, "New description for same status");
        
        // Assert
        assertEquals(originalStatus, escrow.getStatus());
        assertEquals(originalExplanation, escrow.getStatusExplanation());
    }
    
    @Test
    @DisplayName("validateRequiredFields should not throw exception for valid escrow")
    void validateRequiredFields_ShouldNotThrowException_ForValidEscrow() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            escrow.validateRequiredFields();
        });
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when identifier is null")
    void validateRequiredFields_ShouldThrowException_WhenIdentifierIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setIdentifier(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("identifier is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when usdtAccountKey is null")
    void validateRequiredFields_ShouldThrowException_WhenUsdtAccountKeyIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setUsdtAccountKey(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("usdtAccountKey is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when fiatAccountKey is null")
    void validateRequiredFields_ShouldThrowException_WhenFiatAccountKeyIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setFiatAccountKey(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("fiatAccountKey is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when operationType is null")
    void validateRequiredFields_ShouldThrowException_WhenOperationTypeIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setOperationType(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("operationType is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when usdtAmount is null")
    void validateRequiredFields_ShouldThrowException_WhenUsdtAmountIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setUsdtAmount(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("usdtAmount is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when usdtAmount is zero or negative")
    void validateRequiredFields_ShouldThrowException_WhenUsdtAmountIsZeroOrNegative() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        
        // Test zero amount
        escrow.setUsdtAmount(BigDecimal.ZERO);
        Exception exceptionZero = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exceptionZero.getMessage().contains("usdtAmount must be greater than 0"));
        
        // Test negative amount
        escrow.setUsdtAmount(new BigDecimal("-1"));
        Exception exceptionNegative = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exceptionNegative.getMessage().contains("usdtAmount must be greater than 0"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when fiatAmount is null")
    void validateRequiredFields_ShouldThrowException_WhenFiatAmountIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setFiatAmount(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("fiatAmount is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when fiatAmount is zero or negative")
    void validateRequiredFields_ShouldThrowException_WhenFiatAmountIsZeroOrNegative() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        
        // Test zero amount
        escrow.setFiatAmount(BigDecimal.ZERO);
        Exception exceptionZero = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exceptionZero.getMessage().contains("fiatAmount must be greater than 0"));
        
        // Test negative amount
        escrow.setFiatAmount(new BigDecimal("-1"));
        Exception exceptionNegative = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exceptionNegative.getMessage().contains("fiatAmount must be greater than 0"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when fiatCurrency is null")
    void validateRequiredFields_ShouldThrowException_WhenFiatCurrencyIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setFiatCurrency(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("fiatCurrency is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when userId is null")
    void validateRequiredFields_ShouldThrowException_WhenUserIdIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setUserId(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("userId is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when merchantEscrowOperationId is null")
    void validateRequiredFields_ShouldThrowException_WhenMerchantEscrowOperationIdIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setMerchantEscrowOperationId(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("merchantEscrowOperationId is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when status is null")
    void validateRequiredFields_ShouldThrowException_WhenStatusIsNull() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setStatus(null);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("status is required"));
    }
    
    @Test
    @DisplayName("validateRequiredFields should throw exception when USDT and Fiat accounts are the same")
    void validateRequiredFields_ShouldThrowException_WhenUsdtAndFiatAccountsAreSame() {
        // Arrange
        String sameAccount = "same-account-key";
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        escrow.setUsdtAccountKey(sameAccount);
        escrow.setFiatAccountKey(sameAccount);
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            escrow.validateRequiredFields();
        });
        assertTrue(exception.getMessage().contains("USDT account and Fiat account must be different"));
    }
    
    @Test
    @DisplayName("toMessageJson should return a map representation of the escrow")
    void toMessageJson_ShouldReturnMapRepresentation() {
        // Arrange
        MerchantEscrow escrow = MerchantEscrowFactory.createDefault();
        
        // Act
        Map<String, Object> json = escrow.toMessageJson();
        
        // Assert
        assertNotNull(json);
        assertEquals(escrow.getIdentifier(), json.get("identifier"));
        assertEquals(escrow.getUsdtAccountKey(), json.get("usdtAccountKey"));
        assertEquals(escrow.getFiatAccountKey(), json.get("fiatAccountKey"));
        assertEquals(escrow.getOperationType().toString(), json.get("operationType"));
        assertEquals(0, escrow.getUsdtAmount().compareTo(new BigDecimal(json.get("usdtAmount").toString())));
        assertEquals(0, escrow.getFiatAmount().compareTo(new BigDecimal(json.get("fiatAmount").toString())));
        assertEquals(escrow.getFiatCurrency(), json.get("fiatCurrency"));
        assertEquals(escrow.getUserId(), json.get("userId"));
        assertEquals(escrow.getMerchantEscrowOperationId(), json.get("merchantEscrowOperationId"));
        assertEquals(escrow.getStatus(), json.get("status"));
        assertEquals(escrow.getCreatedAt(), ((Number) json.get("createdAt")).longValue());
        assertEquals(escrow.getUpdatedAt(), ((Number) json.get("updatedAt")).longValue());
        assertEquals(escrow.getStatusExplanation(), json.get("statusExplanation"));
    }
} 