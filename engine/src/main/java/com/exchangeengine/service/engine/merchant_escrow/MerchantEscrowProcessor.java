package com.exchangeengine.service.engine.merchant_escrow;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.MerchantEscrowEvent;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.model.OperationType;

/**
 * Processor for merchant escrow events.
 * Handles mint and burn operations for merchant escrow transactions.
 */
public class MerchantEscrowProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MerchantEscrowProcessor.class);
    
    private final DisruptorEvent event;
    private final StorageService storageService;
    private final ProcessResult result;

    /**
     * Constructor with DisruptorEvent.
     *
     * @param event Event to process
     */
    public MerchantEscrowProcessor(DisruptorEvent event) {
        this.event = event;
        this.storageService = StorageService.getInstance();
        this.result = new ProcessResult(event);
    }

    /**
     * Process merchant escrow event
     *
     * @return ProcessResult containing processing result
     */
    public ProcessResult process() {
        MerchantEscrowEvent merchantEscrowEvent = event.getMerchantEscrowEvent();
        
        if (merchantEscrowEvent == null) {
            event.setErrorMessage("Merchant escrow event is null");
            logger.error("MerchantEscrowEvent is null");
            return result;
        }

        try {
            OperationType operationType = merchantEscrowEvent.getOperationType();
            
            switch (operationType) {
                case MERCHANT_ESCROW_MINT:
                    processMintOperation(merchantEscrowEvent);
                    break;
                case MERCHANT_ESCROW_BURN:
                    processBurnOperation(merchantEscrowEvent);
                    break;
                default:
                    event.setErrorMessage("Unsupported operation type: " + operationType);
                    logger.error("Unsupported merchant escrow operation type: {}", operationType);
                    break;
            }
            
            if (event.isSuccess()) {
                logger.info("Successfully processed merchant escrow operation: {}", operationType);
            }
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing merchant escrow event: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Process mint operation:
     * 1. Freezes USDT by moving from available to frozen balance
     * 2. Increases fiat account balance (mints fiat)
     * 3. Changes escrow status from pending to active
     *
     * @param merchantEscrowEvent The merchant escrow event
     */
    private void processMintOperation(MerchantEscrowEvent merchantEscrowEvent) {
        // Extract event data
        String identifier = merchantEscrowEvent.getIdentifier();
        String operationId = merchantEscrowEvent.getMerchantEscrowOperationId();
        BigDecimal usdtAmount = merchantEscrowEvent.getUsdtAmount();
        BigDecimal fiatAmount = merchantEscrowEvent.getFiatAmount();
        
        // Get or create merchant escrow
        MerchantEscrow merchantEscrow = merchantEscrowEvent.toMerchantEscrow(false);
        boolean isExistingEscrow = merchantEscrowEvent.fetchMerchantEscrow(false).isPresent();
        
        if (isExistingEscrow) {
            // Check if it's in pending state
            if (!merchantEscrow.isPending()) {
                event.setErrorMessage("Cannot mint: Merchant escrow is not in pending state: " + identifier);
                logger.error("Cannot mint: Merchant escrow is not in pending state: {}", identifier);
                return;
            }
        }
        
        // Get USDT account
        Optional<Account> usdtAccountOptional = merchantEscrowEvent.getUsdtAccount();
        if (!usdtAccountOptional.isPresent()) {
            event.setErrorMessage("USDT account not found: " + merchantEscrowEvent.getUsdtAccountKey());
            logger.error("USDT account not found: {}", merchantEscrowEvent.getUsdtAccountKey());
            return;
        }
        Account usdtAccount = usdtAccountOptional.get();
        
        // Get Fiat account
        Optional<Account> fiatAccountOptional = merchantEscrowEvent.getFiatAccount();
        if (!fiatAccountOptional.isPresent()) {
            event.setErrorMessage("Fiat account not found: " + merchantEscrowEvent.getFiatAccountKey());
            logger.error("Fiat account not found: {}", merchantEscrowEvent.getFiatAccountKey());
            return;
        }
        Account fiatAccount = fiatAccountOptional.get();
        
        try {
            // Step 1: Kiểm tra và xử lý logic trên USDT account
            if (usdtAccount.getAvailableBalance().compareTo(usdtAmount) < 0) {
                handleInsufficientBalance(merchantEscrow, "USDT", 
                    "Insufficient USDT balance for mint operation");
                return;
            }
            
            // Capture balances before changes
            BigDecimal prevUsdtAvailableBalance = usdtAccount.getAvailableBalance();
            BigDecimal prevUsdtFrozenBalance = usdtAccount.getFrozenBalance();
            
            // Move from available to frozen balance
            usdtAccount.increaseFrozenBalance(usdtAmount, operationId);
            
            // Chuẩn bị USDT account history record
            AccountHistory usdtAccountHistory = createAccountHistory(
                usdtAccount, "freeze", "Merchant escrow mint operation (freeze): " + operationId,
                prevUsdtAvailableBalance, prevUsdtFrozenBalance);
            
            // Step 2: Xử lý logic trên Fiat account
            BigDecimal prevFiatAvailableBalance = fiatAccount.getAvailableBalance();
            BigDecimal prevFiatFrozenBalance = fiatAccount.getFrozenBalance();
            
            // Increase available balance (mint)
            fiatAccount.increaseAvailableBalance(fiatAmount);
            
            // Chuẩn bị fiat account history record
            AccountHistory fiatAccountHistory = createAccountHistory(
                fiatAccount, "mint", "Merchant escrow mint operation: " + operationId,
                prevFiatAvailableBalance, prevFiatFrozenBalance);
            
            // Step 3: Cập nhật trạng thái merchant escrow
            merchantEscrow.activate();
            
            // Step 4: Sau khi xử lý logic thành công, lưu vào cache
            // Lưu USDT account vào cache
            merchantEscrowEvent.updateAccount(usdtAccount);
            
            // Lưu fiat account vào cache
            merchantEscrowEvent.updateAccount(fiatAccount);
            
            // Set success result
            event.successes();
            
            // Set values in result
            result.setFiatAccount(fiatAccount)
                  .setCoinAccount(usdtAccount)
                  .setFiatAccountHistory(fiatAccountHistory)
                  .setCoinAccountHistory(usdtAccountHistory)
                  .setMerchantEscrow(merchantEscrow);
            
            logger.info("Merchant escrow mint successful: identifier={}, usdtAmount={}, fiatAmount={}",
                identifier, usdtAmount, fiatAmount);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing mint operation: {}", e.getMessage(), e);
            
            // If this is a new escrow, create and mark it as cancelled
            if (!isExistingEscrow) {
                MerchantEscrow failedEscrow = new MerchantEscrow();
                failedEscrow.setIdentifier(identifier);
                failedEscrow.setUsdtAccountKey(merchantEscrowEvent.getUsdtAccountKey());
                failedEscrow.setFiatAccountKey(merchantEscrowEvent.getFiatAccountKey());
                failedEscrow.setOperationType(merchantEscrowEvent.getOperationType());
                failedEscrow.setUsdtAmount(usdtAmount);
                failedEscrow.setFiatAmount(fiatAmount);
                failedEscrow.setFiatCurrency(merchantEscrowEvent.getFiatCurrency());
                failedEscrow.setUserId(merchantEscrowEvent.getUserId());
                failedEscrow.setMerchantEscrowOperationId(operationId);
                failedEscrow.cancel();
                
                result.setMerchantEscrow(failedEscrow);
            } else {
                // Leave the existing escrow in pending state
                result.setMerchantEscrow(merchantEscrow);
            }
        }
    }

    /**
     * Process burn operation:
     * 1. Decreases fiat account balance (burns fiat)
     * 2. Unfreezes USDT by moving from frozen to available balance
     * 3. Changes escrow status from active to cancelled
     *
     * @param merchantEscrowEvent The merchant escrow event
     */
    private void processBurnOperation(MerchantEscrowEvent merchantEscrowEvent) {
        // Extract event data
        String identifier = merchantEscrowEvent.getIdentifier();
        BigDecimal usdtAmount = merchantEscrowEvent.getUsdtAmount();
        BigDecimal fiatAmount = merchantEscrowEvent.getFiatAmount();
        String operationId = merchantEscrowEvent.getMerchantEscrowOperationId();
        
        // Get merchant escrow
        MerchantEscrow merchantEscrow = merchantEscrowEvent.toMerchantEscrow(true);
        
        // Check if merchant escrow is active
        if (!merchantEscrow.isActive()) {
            event.setErrorMessage("Cannot burn: Merchant escrow is not active: " + identifier);
            logger.error("Cannot burn: Merchant escrow is not active: {}", identifier);
            return;
        }
        
        // Get USDT account
        Optional<Account> usdtAccountOptional = merchantEscrowEvent.getUsdtAccount();
        if (!usdtAccountOptional.isPresent()) {
            event.setErrorMessage("USDT account not found: " + merchantEscrowEvent.getUsdtAccountKey());
            logger.error("USDT account not found: {}", merchantEscrowEvent.getUsdtAccountKey());
            return;
        }
        Account usdtAccount = usdtAccountOptional.get();
        
        // Get Fiat account
        Optional<Account> fiatAccountOptional = merchantEscrowEvent.getFiatAccount();
        if (!fiatAccountOptional.isPresent()) {
            event.setErrorMessage("Fiat account not found: " + merchantEscrowEvent.getFiatAccountKey());
            logger.error("Fiat account not found: {}", merchantEscrowEvent.getFiatAccountKey());
            return;
        }
        Account fiatAccount = fiatAccountOptional.get();
        
        try {
            // Step 1: Xử lý logic trên Fiat account (Burn)
            if (fiatAccount.getAvailableBalance().compareTo(fiatAmount) < 0) {
                handleInsufficientBalance(merchantEscrow, "fiat", 
                    "Insufficient fiat balance for burn operation");
                return;
            }
            
            // Capture balances before changes
            BigDecimal prevFiatAvailableBalance = fiatAccount.getAvailableBalance();
            BigDecimal prevFiatFrozenBalance = fiatAccount.getFrozenBalance();
            
            // Decrease available balance (burn)
            fiatAccount.decreaseAvailableBalance(fiatAmount);
            
            // Chuẩn bị fiat account history record
            AccountHistory fiatAccountHistory = createAccountHistory(
                fiatAccount, "burn", "Merchant escrow burn operation: " + operationId,
                prevFiatAvailableBalance, prevFiatFrozenBalance);
            
            // Step 2: Xử lý logic trên USDT account (Unfreeze)
            if (usdtAccount.getFrozenBalance().compareTo(usdtAmount) < 0) {
                // Revert fiat burn if USDT unfreeze fails
                fiatAccount.increaseAvailableBalance(fiatAmount);
                handleInsufficientBalance(merchantEscrow, "frozen USDT", 
                    "Insufficient frozen USDT balance for burn operation");
                return;
            }
            
            // Capture balances before changes
            BigDecimal prevUsdtAvailableBalance = usdtAccount.getAvailableBalance();
            BigDecimal prevUsdtFrozenBalance = usdtAccount.getFrozenBalance();
            
            // Move from frozen to available balance
            usdtAccount.decreaseFrozenBalance(usdtAmount);
            usdtAccount.increaseAvailableBalance(usdtAmount);
            
            // Chuẩn bị USDT account history record
            AccountHistory usdtAccountHistory = createAccountHistory(
                usdtAccount, "unfreeze", "Merchant escrow burn operation (unfreeze): " + operationId,
                prevUsdtAvailableBalance, prevUsdtFrozenBalance);
            
            // Step 3: Cập nhật trạng thái merchant escrow
            merchantEscrow.cancel();
            
            // Step 4: Sau khi xử lý logic thành công, lưu vào cache
            // Lưu fiat account vào cache
            merchantEscrowEvent.updateAccount(fiatAccount);
            
            // Lưu USDT account vào cache
            merchantEscrowEvent.updateAccount(usdtAccount);
            
            // Set success result
            event.successes();
            
            // Set values in result
            result.setFiatAccount(fiatAccount)
                  .setCoinAccount(usdtAccount)
                  .setFiatAccountHistory(fiatAccountHistory)
                  .setCoinAccountHistory(usdtAccountHistory)
                  .setMerchantEscrow(merchantEscrow);
            
            logger.info("Merchant escrow burn successful: identifier={}, usdtAmount={}, fiatAmount={}",
                identifier, usdtAmount, fiatAmount);
        } catch (Exception e) {
            event.setErrorMessage(e.getMessage());
            logger.error("Error processing burn operation: {}", e.getMessage(), e);
            
            // Keep the merchant escrow in active state if operation fails
            result.setMerchantEscrow(merchantEscrow);
        }
    }

    /**
     * Create account history
     * 
     * @param account Account
     * @param action Action
     * @param message Message
     * @param prevAvailableBalance Previous available balance
     * @param prevFrozenBalance Previous frozen balance
     * @return Account history
     */
    private AccountHistory createAccountHistory(Account account, String action, String message,
                                              BigDecimal prevAvailableBalance, BigDecimal prevFrozenBalance) {
        // Create account history with account key, identifier, and operation type
        AccountHistory accountHistory = new AccountHistory(account.getKey(), message, action);
        
        // Set balance values
        accountHistory.setBalanceValues(
            prevAvailableBalance,
            account.getAvailableBalance(),
            prevFrozenBalance,
            account.getFrozenBalance()
        );
        
        // Store account history in cache - Add null check and error handling
        try {
            if (storageService != null && storageService.getAccountHistoryCache() != null) {
                storageService.getAccountHistoryCache().addHistoryToBatch(accountHistory);
            } else {
                logger.warn("Cannot store account history: AccountHistoryCache is null");
            }
        } catch (Exception e) {
            logger.warn("Error storing account history: {}", e.getMessage());
        }
        
        return accountHistory;
    }

    /**
     * Handle insufficient balance error
     *
     * @param merchantEscrow Merchant escrow
     * @param assetType Asset type (USDT or fiat)
     * @param errorMessage Error message
     */
    private void handleInsufficientBalance(MerchantEscrow merchantEscrow, 
                                         String assetType, String errorMessage) {
        logger.error("Insufficient {} balance: {}", assetType, errorMessage);
        event.setErrorMessage(errorMessage);
        
        // Mark escrow as cancelled
        merchantEscrow.cancel();
        
        result.setMerchantEscrow(merchantEscrow);
    }
}
