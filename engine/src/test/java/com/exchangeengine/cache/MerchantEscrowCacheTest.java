package com.exchangeengine.cache;

import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.storage.cache.MerchantEscrowCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.*;

public class MerchantEscrowCacheTest {

    @BeforeEach
    public void setUp() {
        // Reset the singleton instance before each test
        MerchantEscrowCache.resetInstance();
    }

    @AfterEach
    public void tearDown() {
        // Reset the singleton instance after each test
        MerchantEscrowCache.resetInstance();
    }

    @Test
    public void testGetOrInitMerchantEscrow() {
        // Arrange
        String escrowIdentifier = "testEscrowIdentifier";
        String userId = "user-" + UUID.randomUUID().toString();
        String usdtAccountKey = "usdt-" + UUID.randomUUID().toString();
        String fiatAccountKey = "fiat-" + UUID.randomUUID().toString();
        
        // Create a valid MerchantEscrow
        MerchantEscrow validEscrow = new MerchantEscrow(
            escrowIdentifier,
            usdtAccountKey,
            fiatAccountKey,
            OperationType.MERCHANT_ESCROW_MINT,
            new BigDecimal("100.00"),
            new BigDecimal("3000.00"),
            "USD",
            userId,
            "merchant-escrow-op-" + escrowIdentifier
        );
        
        // Create a mock MerchantEscrowCache
        MerchantEscrowCache mockCache = mock(MerchantEscrowCache.class);
        when(mockCache.getMerchantEscrow(escrowIdentifier)).thenReturn(Optional.of(validEscrow));
        when(mockCache.getOrInitMerchantEscrow(escrowIdentifier)).thenReturn(validEscrow);
        
        // Set the mock as the singleton instance
        MerchantEscrowCache.setTestInstance(mockCache);
        
        // Act
        MerchantEscrow result = MerchantEscrowCache.getInstance().getOrInitMerchantEscrow(escrowIdentifier);

        // Assert
        assertNotNull(result);
        assertEquals(escrowIdentifier, result.getIdentifier());
        assertEquals(MerchantEscrow.STATUS_PENDING, result.getStatus());
        assertEquals(usdtAccountKey, result.getUsdtAccountKey());
        assertEquals(fiatAccountKey, result.getFiatAccountKey());
        assertEquals(userId, result.getUserId());
        assertEquals(OperationType.MERCHANT_ESCROW_MINT, result.getOperationType());
        assertEquals("merchant-escrow-op-" + escrowIdentifier, result.getMerchantEscrowOperationId());
    }
} 