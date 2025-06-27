package com.exchangeengine.service.engine.merchant_escrow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.exchangeengine.extension.SingletonResetExtension;
import com.exchangeengine.factory.AccountFactory;
import com.exchangeengine.factory.MerchantEscrowFactory;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.MerchantEscrowEvent;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AccountHistoryCache;
import com.exchangeengine.storage.cache.MerchantEscrowCache;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
public class MerchantEscrowProcessorTest {

    @Mock
    private MerchantEscrowCache mockMerchantEscrowCache;

    @Mock
    private AccountCache mockAccountCache;

    @Mock
    private AccountHistoryCache mockAccountHistoryCache;

    @Mock
    private StorageService mockStorageService;

    @Mock
    private DisruptorEvent mockEvent;

    @Mock
    private MerchantEscrowEvent mockMerchantEscrowEvent;

    private MerchantEscrowProcessor processor;

    @BeforeEach
    void setup() {
        // Setup the mock DisruptorEvent to return our mock MerchantEscrowEvent
        when(mockEvent.getMerchantEscrowEvent()).thenReturn(mockMerchantEscrowEvent);
        // Setup mock để gọi event.successes()
        doNothing().when(mockEvent).successes();

        // Set mock instances - Order is important
        StorageService.setTestInstance(mockStorageService);
        MerchantEscrowCache.setTestInstance(mockMerchantEscrowCache);
        AccountCache.setTestInstance(mockAccountCache);
        AccountHistoryCache.setTestInstance(mockAccountHistoryCache);
        
        // Setup mockStorageService with lenient mocks to avoid strict verification errors
        lenient().when(mockStorageService.getAccountHistoryCache()).thenReturn(mockAccountHistoryCache);
        lenient().when(mockStorageService.getMerchantEscrowCache()).thenReturn(mockMerchantEscrowCache);
        
        // Setup doNothing for addHistoryToBatch
        doNothing().when(mockAccountHistoryCache).addHistoryToBatch(any());
        
        // Initialize the processor with the mock event
        processor = new MerchantEscrowProcessor(mockEvent);
    }

    @Test
    @DisplayName("process should handle null MerchantEscrowEvent")
    void process_ShouldHandleNullMerchantEscrowEvent() {
        // Setup
        when(mockEvent.getMerchantEscrowEvent()).thenReturn(null);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).successes();
        verify(mockEvent).setErrorMessage(anyString());
        assertFalse(result.getMerchantEscrow().isPresent());
    }

    @Test
    @DisplayName("process should handle unsupported operation type")
    void process_ShouldHandleUnsupportedOperationType() {
        // Setup
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.COIN_ACCOUNT_CREATE);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).successes();
        verify(mockEvent).setErrorMessage(anyString());
        assertFalse(result.getMerchantEscrow().isPresent());
    }
    
    @Test
    @DisplayName("process should handle general exceptions")
    void process_ShouldHandleGeneralExceptions() {
        // Setup
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_MINT);
        when(mockMerchantEscrowEvent.getIdentifier()).thenThrow(new RuntimeException("Test exception"));
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify
        verify(mockEvent).setErrorMessage("Test exception");
        verify(mockEvent, never()).successes();
    }
    
    @Test
    @DisplayName("process should log success when operation succeeds")
    void process_ShouldLogSuccessWhenOperationSucceeds() {
        // Setup for successful mint operation
        setupSuccessfulMintOperation();
        when(mockEvent.isSuccess()).thenReturn(true);
        
        // Execute
        processor.process();
        
        // Verify
        verify(mockEvent).successes();
    }

    @Test
    @DisplayName("processMintOperation should mint successfully")
    void processMintOperation_ShouldMintSuccessfully() {
        // Setup
        setupSuccessfulMintOperation();

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).setErrorMessage(anyString());
        verify(mockMerchantEscrowEvent, times(2)).updateAccount(any(Account.class));
        verify(mockEvent).successes();
        
        // Check merchant escrow status
        assertTrue(result.getMerchantEscrow().isPresent());
        assertTrue(result.getMerchantEscrow().get().isActive(), "Escrow should be active");
    }

    @Test
    @DisplayName("processMintOperation should fail with insufficient USDT balance")
    void processMintOperation_ShouldFailWithInsufficientUSDTBalance() {
        // Setup
        // Setup basic event properties
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";

        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_MINT);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);

        // Create mock accounts with exact balances
        Account usdtAccount = AccountFactory.createWithBalances(usdtAccountKey, 
            new BigDecimal("50.00"), BigDecimal.ZERO);
        
        Account fiatAccount = AccountFactory.createWithBalances(fiatAccountKey,
            BigDecimal.ZERO, BigDecimal.ZERO);
        
        // Setup account mocks
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.of(usdtAccount));
        when(mockMerchantEscrowEvent.getFiatAccount()).thenReturn(Optional.of(fiatAccount));
        
        // Mock merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createDefault();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(false)).thenReturn(Optional.empty());
        when(mockMerchantEscrowEvent.toMerchantEscrow(false)).thenReturn(merchantEscrow);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).successes();
        verify(mockEvent).setErrorMessage(anyString());
        
        // Check account balances remain unchanged
        assertEquals(BigDecimal.ZERO.setScale(16, RoundingMode.HALF_UP), usdtAccount.getFrozenBalance(), "USDT frozen should not change");
        assertEquals(new BigDecimal("50.00").setScale(16, RoundingMode.HALF_UP), usdtAccount.getAvailableBalance(), "USDT available should not change");
        assertEquals(BigDecimal.ZERO.setScale(16, RoundingMode.HALF_UP), fiatAccount.getAvailableBalance(), "Fiat should not be minted");
        
        // Check merchant escrow status
        assertTrue(result.getMerchantEscrow().isPresent());
        assertTrue(result.getMerchantEscrow().get().getStatus().equals(MerchantEscrow.STATUS_CANCELLED), 
                  "Escrow should be cancelled");
    }
    
    @Test
    @DisplayName("processMintOperation should fail when escrow is not in pending state")
    void processMintOperation_ShouldFailWhenEscrowNotInPendingState() {
        // Setup
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";
        
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_MINT);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);
        
        // Create a merchant escrow that is not in pending state (completed)
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createCompletedMerchantEscrow();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(false)).thenReturn(Optional.of(merchantEscrow));
        when(mockMerchantEscrowEvent.toMerchantEscrow(false)).thenReturn(merchantEscrow);
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify
        verify(mockEvent).setErrorMessage(contains("Cannot mint: Merchant escrow is not in pending state"));
        verify(mockEvent, never()).successes();
    }
    
    @Test
    @DisplayName("processMintOperation should handle exceptions when creating new escrow")
    void processMintOperation_ShouldHandleExceptionsWhenCreatingNewEscrow() {
        // Setup
        setupSuccessfulMintOperation();
        
        // Throw exception when updating the account
        RuntimeException testException = new RuntimeException("Test exception");
        doThrow(testException).when(mockMerchantEscrowEvent).updateAccount(any());
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify
        verify(mockEvent).setErrorMessage(contains("Test exception"));
        verify(mockEvent, never()).successes();
        
        // Check that a new cancelled escrow was returned
        assertTrue(result.getMerchantEscrow().isPresent(), "Should return the merchant escrow");
    }
    
    @Test
    @DisplayName("processMintOperation should handle exceptions when working with existing escrow")
    void processMintOperation_ShouldHandleExceptionsWithExistingEscrow() {
        // Setup
        // Setup basic event properties
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";

        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_MINT);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);

        // Create mock accounts with exact balances
        Account usdtAccount = AccountFactory.createWithBalances(usdtAccountKey, 
            new BigDecimal("200.00"), BigDecimal.ZERO);
        
        Account fiatAccount = AccountFactory.createWithBalances(fiatAccountKey,
            BigDecimal.ZERO, BigDecimal.ZERO);
        
        // Setup account mocks
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.of(usdtAccount));
        when(mockMerchantEscrowEvent.getFiatAccount()).thenReturn(Optional.of(fiatAccount));
        
        // Create an existing merchant escrow in pending state
        MerchantEscrow existingEscrow = MerchantEscrowFactory.createDefault(); // In PENDING state
        
        // Mock that we have an existing escrow
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(false)).thenReturn(Optional.of(existingEscrow));
        when(mockMerchantEscrowEvent.toMerchantEscrow(false)).thenReturn(existingEscrow);
        
        // Throw exception when updating the account to trigger the error path
        // The exception will be thrown after the escrow has been activated
        RuntimeException testException = new RuntimeException("Test exception with existing escrow");
        doThrow(testException).when(mockMerchantEscrowEvent).updateAccount(any());
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify
        verify(mockEvent).setErrorMessage(contains("Test exception with existing escrow"));
        verify(mockEvent, never()).successes();
        
        // Check that the existing escrow was returned
        // Note that the status will be COMPLETED because activate() is called before the updateAccount() exception
        assertTrue(result.getMerchantEscrow().isPresent(), "Should return the merchant escrow");
        assertEquals(MerchantEscrow.STATUS_COMPLETED, result.getMerchantEscrow().get().getStatus(), 
                  "Escrow status should be COMPLETED because the activate() method is called before the exception");
    }
    
    @Test
    @DisplayName("processBurnOperation should burn successfully")
    void processBurnOperation_ShouldBurnSuccessfully() {
        // Setup
        setupSuccessfulBurnOperation();

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).setErrorMessage(anyString());
        verify(mockMerchantEscrowEvent, times(2)).updateAccount(any(Account.class));
        verify(mockEvent).successes();
        
        // Check merchant escrow status
        assertTrue(result.getMerchantEscrow().isPresent());
        assertEquals(MerchantEscrow.STATUS_CANCELLED, result.getMerchantEscrow().get().getStatus());
    }
    
    @Test
    @DisplayName("processBurnOperation should fail when USDT account not found")
    void processBurnOperation_ShouldFailWhenUSDTAccountNotFound() {
        // Setup
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";
        
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_BURN);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);
        
        // Create an active merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createCompletedMerchantEscrow();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(true)).thenReturn(Optional.of(merchantEscrow));
        when(mockMerchantEscrowEvent.toMerchantEscrow(true)).thenReturn(merchantEscrow);
        
        // USDT account not found
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.empty());
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify
        verify(mockEvent).setErrorMessage(contains("USDT account not found"));
        verify(mockEvent, never()).successes();
    }
    
    @Test
    @DisplayName("processBurnOperation should fail when fiat account not found")
    void processBurnOperation_ShouldFailWhenFiatAccountNotFound() {
        // Setup
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";
        
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_BURN);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);
        
        // Create an active merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createCompletedMerchantEscrow();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(true)).thenReturn(Optional.of(merchantEscrow));
        when(mockMerchantEscrowEvent.toMerchantEscrow(true)).thenReturn(merchantEscrow);
        
        // USDT account found
        Account usdtAccount = AccountFactory.createWithBalances(usdtAccountKey,
            BigDecimal.ZERO, new BigDecimal("100.00"));
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.of(usdtAccount));
        
        // Fiat account not found
        when(mockMerchantEscrowEvent.getFiatAccount()).thenReturn(Optional.empty());
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify
        verify(mockEvent).setErrorMessage(contains("Fiat account not found"));
        verify(mockEvent, never()).successes();
    }
    
    @Test
    @DisplayName("processBurnOperation should fail with insufficient frozen USDT balance")
    void processBurnOperation_ShouldFailWithInsufficientFrozenUSDTBalance() {
        // Setup
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";
        
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_BURN);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);
        
        // Create an active merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createCompletedMerchantEscrow();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(true)).thenReturn(Optional.of(merchantEscrow));
        when(mockMerchantEscrowEvent.toMerchantEscrow(true)).thenReturn(merchantEscrow);
        
        // USDT account with insufficient frozen balance
        Account usdtAccount = AccountFactory.createWithBalances(usdtAccountKey,
            new BigDecimal("50.00"), new BigDecimal("50.00"));
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.of(usdtAccount));
        
        // Fiat account with sufficient balance
        Account fiatAccount = AccountFactory.createWithBalances(fiatAccountKey,
            new BigDecimal("1000000.00"), BigDecimal.ZERO);
        when(mockMerchantEscrowEvent.getFiatAccount()).thenReturn(Optional.of(fiatAccount));
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify
        verify(mockEvent).setErrorMessage(contains("Insufficient frozen USDT balance"));
        verify(mockEvent, never()).successes();
        
        // Verify fiat balance was increased back after failure
        assertTrue(result.getMerchantEscrow().isPresent());
        assertEquals(MerchantEscrow.STATUS_CANCELLED, result.getMerchantEscrow().get().getStatus());
    }
    
    @Test
    @DisplayName("processBurnOperation should handle exceptions")
    void processBurnOperation_ShouldHandleExceptions() {
        // Setup successful burn scenario
        setupSuccessfulBurnOperation();
        
        // But make the update method throw an exception
        doThrow(new RuntimeException("Test exception")).when(mockMerchantEscrowEvent).updateAccount(any(Account.class));
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify
        verify(mockEvent).setErrorMessage("Test exception");
        verify(mockEvent, never()).successes();
        assertTrue(result.getMerchantEscrow().isPresent(), "Should return the merchant escrow");
    }
    
    @Test
    @DisplayName("createAccountHistory should handle exception in addHistoryToBatch")
    void createAccountHistory_ShouldHandleExceptionInAddHistoryToBatch() {
        // Setup
        setupSuccessfulMintOperation();
        
        // Make addHistoryToBatch throw an exception
        doThrow(new RuntimeException("Test exception"))
            .when(mockAccountHistoryCache).addHistoryToBatch(any());
        
        // Execute
        ProcessResult result = processor.process();
        
        // Verify - the exception should be caught and the process should continue
        verify(mockEvent, never()).setErrorMessage(anyString());
        verify(mockEvent).successes();
        assertTrue(result.getMerchantEscrow().isPresent());
    }
    
    /**
     * Helper method to set up a successful mint operation scenario
     */
    private void setupSuccessfulMintOperation() {
        // Setup basic event properties
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";

        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_MINT);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);

        // Create mock accounts with exact balances
        Account usdtAccount = AccountFactory.createWithBalances(usdtAccountKey, 
            new BigDecimal("200.00"), BigDecimal.ZERO);
        
        Account fiatAccount = AccountFactory.createWithBalances(fiatAccountKey,
            BigDecimal.ZERO, BigDecimal.ZERO);
        
        // Setup account mocks
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.of(usdtAccount));
        when(mockMerchantEscrowEvent.getFiatAccount()).thenReturn(Optional.of(fiatAccount));
        
        // Mock merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createDefault();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(false)).thenReturn(Optional.empty());
        when(mockMerchantEscrowEvent.toMerchantEscrow(false)).thenReturn(merchantEscrow);
    }
    
    /**
     * Helper method to set up a successful burn operation scenario
     */
    private void setupSuccessfulBurnOperation() {
        // Setup basic event properties
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";

        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_BURN);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);

        // Create mock accounts with exact balances
        Account usdtAccount = AccountFactory.createWithBalances(usdtAccountKey,
            BigDecimal.ZERO, new BigDecimal("100.00"));
        
        Account fiatAccount = AccountFactory.createWithBalances(fiatAccountKey,
            new BigDecimal("1000000.00"), BigDecimal.ZERO);
        
        // Setup account mocks
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.of(usdtAccount));
        when(mockMerchantEscrowEvent.getFiatAccount()).thenReturn(Optional.of(fiatAccount));
        
        // Mock merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createCompletedMerchantEscrow();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(true)).thenReturn(Optional.of(merchantEscrow));
        when(mockMerchantEscrowEvent.toMerchantEscrow(true)).thenReturn(merchantEscrow);
    }

    @Test
    @DisplayName("processMintOperation should fail when USDT account not found")
    void processMintOperation_ShouldFailWhenUSDTAccountNotFound() {
        // Setup
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_MINT);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn("non-existent-account");
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.empty());
        
        // Mock merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createDefault();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(false)).thenReturn(Optional.empty());
        when(mockMerchantEscrowEvent.toMerchantEscrow(false)).thenReturn(merchantEscrow);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).successes();
        verify(mockEvent).setErrorMessage(anyString());
        assertFalse(result.getMerchantEscrow().isPresent());
    }

    @Test
    @DisplayName("processMintOperation should fail when fiat account not found")
    void processMintOperation_ShouldFailWhenFiatAccountNotFound() {
        // Setup
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_MINT);
        
        // Mock USDT account
        Account usdtAccount = AccountFactory.createWithBalances("usdt-account-1",
            new BigDecimal("100.00"), BigDecimal.ZERO);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn("usdt-account-1");
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.of(usdtAccount));
        
        // Mock missing fiat account
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn("non-existent-account");
        when(mockMerchantEscrowEvent.getFiatAccount()).thenReturn(Optional.empty());
        
        // Mock merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createDefault();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(false)).thenReturn(Optional.empty());
        when(mockMerchantEscrowEvent.toMerchantEscrow(false)).thenReturn(merchantEscrow);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).successes();
        verify(mockEvent).setErrorMessage(anyString());
        assertFalse(result.getMerchantEscrow().isPresent());
    }
    
    @Test
    @DisplayName("processBurnOperation should fail with insufficient fiat balance")
    void processBurnOperation_ShouldFailWithInsufficientFiatBalance() {
        // Setup
        // Setup basic event properties
        String identifier = "test-escrow-1";
        String usdtAccountKey = "usdt-account-1";
        String fiatAccountKey = "fiat-account-1";
        BigDecimal usdtAmount = new BigDecimal("100.00");
        BigDecimal fiatAmount = new BigDecimal("1000000.00");
        String operationId = "operation-1";

        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_BURN);
        when(mockMerchantEscrowEvent.getIdentifier()).thenReturn(identifier);
        when(mockMerchantEscrowEvent.getUsdtAccountKey()).thenReturn(usdtAccountKey);
        when(mockMerchantEscrowEvent.getFiatAccountKey()).thenReturn(fiatAccountKey);
        when(mockMerchantEscrowEvent.getUsdtAmount()).thenReturn(usdtAmount);
        when(mockMerchantEscrowEvent.getFiatAmount()).thenReturn(fiatAmount);
        when(mockMerchantEscrowEvent.getMerchantEscrowOperationId()).thenReturn(operationId);

        // Create mock accounts with exact balances
        Account usdtAccount = AccountFactory.createWithBalances(usdtAccountKey,
            BigDecimal.ZERO, new BigDecimal("100.00"));
        
        Account fiatAccount = AccountFactory.createWithBalances(fiatAccountKey,
            new BigDecimal("500000.00"), BigDecimal.ZERO);
        
        // Setup account mocks
        when(mockMerchantEscrowEvent.getUsdtAccount()).thenReturn(Optional.of(usdtAccount));
        when(mockMerchantEscrowEvent.getFiatAccount()).thenReturn(Optional.of(fiatAccount));
        
        // Mock merchant escrow
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createCompletedMerchantEscrow();
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(true)).thenReturn(Optional.of(merchantEscrow));
        when(mockMerchantEscrowEvent.toMerchantEscrow(true)).thenReturn(merchantEscrow);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).successes();
        verify(mockEvent).setErrorMessage(anyString());
        
        // Check account balances remain unchanged
        assertEquals(new BigDecimal("100.00").setScale(16, RoundingMode.HALF_UP), usdtAccount.getFrozenBalance(), "USDT frozen should not change");
        assertEquals(BigDecimal.ZERO.setScale(16, RoundingMode.HALF_UP), usdtAccount.getAvailableBalance(), "USDT available should not change");
        assertEquals(new BigDecimal("500000.00").setScale(16, RoundingMode.HALF_UP), fiatAccount.getAvailableBalance(), "Fiat balance should not change");
        
        // Check merchant escrow status
        assertTrue(result.getMerchantEscrow().isPresent());
        assertTrue(result.getMerchantEscrow().get().getStatus().equals(MerchantEscrow.STATUS_CANCELLED), 
                   "Escrow should be cancelled");
    }

    @Test
    @DisplayName("processBurnOperation should fail if escrow is not active")
    void processBurnOperation_ShouldFailIfEscrowNotActive() {
        // Setup
        // Setup basic event properties
        when(mockMerchantEscrowEvent.getOperationType()).thenReturn(OperationType.MERCHANT_ESCROW_BURN);
        
        // Mock merchant escrow in pending state (not active)
        MerchantEscrow merchantEscrow = MerchantEscrowFactory.createDefault(); // Default is PENDING
        when(mockMerchantEscrowEvent.fetchMerchantEscrow(true)).thenReturn(Optional.of(merchantEscrow));
        when(mockMerchantEscrowEvent.toMerchantEscrow(true)).thenReturn(merchantEscrow);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        verify(mockEvent, never()).successes();
        verify(mockEvent).setErrorMessage(contains("Cannot burn: Merchant escrow is not active"));
        
        // Check merchant escrow status
        if (result.getMerchantEscrow().isPresent()) {
            assertFalse(result.getMerchantEscrow().get().isActive(), 
                       "Escrow should not be active");
        }
    }
} 