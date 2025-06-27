package com.exchangeengine.factory;

import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.model.OperationType;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class for creating MerchantEscrow instances for testing purposes
 */
public class MerchantEscrowFactory {

    /**
     * Creates a base model for MerchantEscrow instances with common properties
     *
     * @return a model for MerchantEscrow instances
     */
    private static Model<MerchantEscrow> baseModel() {
        return Instancio.of(MerchantEscrow.class)
            .set(field(MerchantEscrow::getIdentifier), "test-escrow-" + UUID.randomUUID().toString().substring(0, 8))
            .set(field(MerchantEscrow::getUsdtAccountKey), "usdt-account-" + UUID.randomUUID().toString().substring(0, 8))
            .set(field(MerchantEscrow::getFiatAccountKey), "fiat-account-" + UUID.randomUUID().toString().substring(0, 8))
            .set(field(MerchantEscrow::getOperationType), OperationType.MERCHANT_ESCROW_MINT)
            .set(field(MerchantEscrow::getUsdtAmount), new BigDecimal("100.00"))
            .set(field(MerchantEscrow::getFiatAmount), new BigDecimal("1000000.00"))
            .set(field(MerchantEscrow::getFiatCurrency), "VND")
            .set(field(MerchantEscrow::getUserId), "user-" + UUID.randomUUID().toString().substring(0, 8))
            .set(field(MerchantEscrow::getMerchantEscrowOperationId), "merchant-escrow-op-" + UUID.randomUUID().toString().substring(0, 8))
            .toModel();
    }

    /**
     * Creates a new MerchantEscrow instance with default values.
     *
     * @return a new MerchantEscrow instance with default values
     */
    public static MerchantEscrow createDefault() {
        // Create a MerchantEscrow using the full constructor to ensure proper initialization
        MerchantEscrow merchantEscrow = new MerchantEscrow(
            "test-escrow-1",
            "usdt-account-1",
            "fiat-account-1",
            OperationType.MERCHANT_ESCROW_MINT,
            new BigDecimal("100.00"),
            new BigDecimal("1000000.00"),
            "VND",
            "test-user-1",
            "merchant-escrow-op-1"
        );
        
        return merchantEscrow;
    }

    /**
     * Creates a new MerchantEscrow instance with cancelled status.
     *
     * @return a new MerchantEscrow instance with cancelled status
     */
    public static MerchantEscrow createCancelledMerchantEscrow() {
        MerchantEscrow escrow = createDefault();
        escrow.activate(); // First activate, then cancel
        escrow.cancel();
        return escrow;
    }

    /**
     * Creates a new MerchantEscrow instance with completed status.
     *
     * @return a new MerchantEscrow instance with completed status
     */
    public static MerchantEscrow createCompletedMerchantEscrow() {
        MerchantEscrow merchantEscrow = createDefault();
        merchantEscrow.activate(); // First activate to follow the flow
        merchantEscrow.setStatus(MerchantEscrow.STATUS_COMPLETED);
        return merchantEscrow;
    }

    /**
     * Creates a new MerchantEscrow instance with specified values.
     *
     * @param identifier the escrow identifier
     * @param usdtAccountKey the USDT account key
     * @param fiatAccountKey the fiat account key
     * @param operationType the operation type
     * @param usdtAmount the USDT amount
     * @param fiatAmount the fiat amount
     * @param fiatCurrency the fiat currency
     * @param userId the user ID
     * @param merchantEscrowOperationId the merchant escrow operation ID
     * @return a new MerchantEscrow instance with specified values
     */
    public static MerchantEscrow createMerchantEscrow(
            String identifier,
            String usdtAccountKey,
            String fiatAccountKey,
            OperationType operationType,
            BigDecimal usdtAmount,
            BigDecimal fiatAmount,
            String fiatCurrency,
            String userId,
            String merchantEscrowOperationId) {
        
        return new MerchantEscrow(
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
    }

    /**
     * Creates a custom MerchantEscrow instance with specified parameters
     *
     * @param merchantEscrowId Unique ID for merchant escrow
     * @param usdtAccountId USDT account ID
     * @param fiatAccountId Fiat account ID
     * @param usdtAmount USDT amount
     * @param fiatAmount Fiat amount
     * @param fiatCurrency Fiat currency code
     * @param userId User ID who initiated the escrow
     * @return a new MerchantEscrow with the specified parameters
     */
    public static MerchantEscrow createCustomMerchantEscrow(
            String merchantEscrowId,
            String usdtAccountId,
            String fiatAccountId,
            BigDecimal usdtAmount,
            BigDecimal fiatAmount,
            String fiatCurrency,
            String userId
    ) {
        String operationId = "merchant-escrow-op-" + UUID.randomUUID();
        
        return new MerchantEscrow(
            merchantEscrowId,
            usdtAccountId,
            fiatAccountId,
            OperationType.MERCHANT_ESCROW_MINT,
            usdtAmount,
            fiatAmount,
            fiatCurrency,
            userId,
            operationId
        );
    }

    /**
     * Creates a MerchantEscrow instance with a specific USDT and fiat amount
     *
     * @param usdtAmount USDT amount
     * @param fiatAmount Fiat amount
     * @param fiatCurrency Fiat currency code
     * @return a new MerchantEscrow with the specified amounts
     */
    public static MerchantEscrow createMerchantEscrowWithAmount(
            BigDecimal usdtAmount,
            BigDecimal fiatAmount,
            String fiatCurrency
    ) {
        String merchantEscrowId = UUID.randomUUID().toString();
        String usdtAccountId = "usdt_" + UUID.randomUUID().toString().substring(0, 8);
        String fiatAccountId = "fiat_" + UUID.randomUUID().toString().substring(0, 8);
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String operationId = "merchant-escrow-op-" + UUID.randomUUID();

        return new MerchantEscrow(
            merchantEscrowId,
            usdtAccountId,
            fiatAccountId,
            OperationType.MERCHANT_ESCROW_MINT,
            usdtAmount,
            fiatAmount,
            fiatCurrency,
            userId,
            operationId
        );
    }

    /**
     * Creates a MerchantEscrow instance with specific accounts
     *
     * @param usdtAccountId USDT account ID
     * @param fiatAccountId Fiat account ID
     * @return a new MerchantEscrow with the specified accounts
     */
    public static MerchantEscrow createMerchantEscrowWithAccounts(
            String usdtAccountId,
            String fiatAccountId
    ) {
        String merchantEscrowId = UUID.randomUUID().toString();
        BigDecimal usdtAmount = new BigDecimal("1000.00");
        BigDecimal fiatAmount = new BigDecimal("24000000.00");
        String fiatCurrency = "VND";
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String operationId = "merchant-escrow-op-" + UUID.randomUUID();

        return new MerchantEscrow(
            merchantEscrowId,
            usdtAccountId,
            fiatAccountId,
            OperationType.MERCHANT_ESCROW_MINT,
            usdtAmount,
            fiatAmount,
            fiatCurrency,
            userId,
            operationId
        );
    }
} 