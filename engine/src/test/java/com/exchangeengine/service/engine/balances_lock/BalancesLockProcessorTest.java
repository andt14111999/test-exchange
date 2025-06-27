package com.exchangeengine.service.engine.balances_lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.AccountFactory;
import com.exchangeengine.factory.BalanceLockFactory;
import com.exchangeengine.factory.event.BalancesLockEventFactory;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.BalanceLock;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.event.BalancesLockEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.BalanceLockCache;

/**
 * Test cho BalancesLockProcessor
 */
@ExtendWith({MockitoExtension.class, CombinedTestExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BalancesLockProcessor Tests")
public class BalancesLockProcessorTest {

    @Mock
    private AccountCache mockAccountCache;

    @Mock
    private BalanceLockCache mockBalanceLockCache;

    @Mock
    private StorageService mockStorageService;

    @Mock
    private DisruptorEvent mockEvent;

    @Mock
    private BalancesLockEvent mockBalancesLockEvent;

    private BalancesLockProcessor processor;

    @BeforeEach
    void setup() {
        // Setup the mock DisruptorEvent to return our mock BalancesLockEvent
        when(mockEvent.getBalancesLockEvent()).thenReturn(mockBalancesLockEvent);
        // Setup mock để gọi event.successes()
        doNothing().when(mockEvent).successes();

        // Set mock instances
        AccountCache.setTestInstance(mockAccountCache);
        BalanceLockCache.setTestInstance(mockBalanceLockCache);
        StorageService.setTestInstance(mockStorageService);

        // Setup StorageService to return our mock caches
        when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

        // Initialize the processor with the mock event
        processor = new BalancesLockProcessor(mockEvent);
    }

    @Test
    @DisplayName("process should create balance lock successfully")
    void process_ShouldCreateBalanceLockSuccessfully() {
        // Setup
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.getAccountKeys()).thenReturn(Arrays.asList("account1", "account2"));

        // Create mock accounts with available balance
        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(new BigDecimal("1000.00"));
        account1.setFrozenBalance(new BigDecimal("0.00"));

        Account account2 = AccountFactory.create("account2");
        account2.setAvailableBalance(new BigDecimal("500.00"));
        account2.setFrozenBalance(new BigDecimal("0.00"));

        // Create a BalanceLock that would be returned by toBalanceLock
        BalanceLock newLock = BalanceLockFactory.create();

        // Mock the behavior
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockAccountCache.getAccount("account2")).thenReturn(Optional.of(account2));
        when(mockBalancesLockEvent.toBalanceLock(false)).thenReturn(newLock);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        assertEquals(newLock, result.getBalanceLock().get());
        
        // Verify accounts were processed
        verify(mockAccountCache).addAccountToBatch(account1);
        verify(mockAccountCache).addAccountToBatch(account2);
        verify(mockBalanceLockCache).addBalanceLock(newLock);
        
        // Verify balances were frozen - use compareTo for BigDecimal comparison
        assertEquals(0, account1.getAvailableBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, account1.getFrozenBalance().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, account2.getAvailableBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, account2.getFrozenBalance().compareTo(new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("process should handle accounts with zero balance")
    void process_ShouldHandleAccountsWithZeroBalance() {
        // Setup
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.getAccountKeys()).thenReturn(Arrays.asList("account1", "account2"));

        // Create mock accounts - one with balance, one without
        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(new BigDecimal("1000.00"));
        account1.setFrozenBalance(new BigDecimal("0.00"));

        Account account2 = AccountFactory.create("account2");
        account2.setAvailableBalance(BigDecimal.ZERO);
        account2.setFrozenBalance(new BigDecimal("0.00"));

        BalanceLock newLock = BalanceLockFactory.create();

        // Mock the behavior
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockAccountCache.getAccount("account2")).thenReturn(Optional.of(account2));
        when(mockBalancesLockEvent.toBalanceLock(false)).thenReturn(newLock);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        
        // Verify only account1 was processed (has balance)
        verify(mockAccountCache).addAccountToBatch(account1);
        verify(mockAccountCache, never()).addAccountToBatch(account2);
        
        // Verify account1 balance was frozen, account2 unchanged - use compareTo for BigDecimal comparison
        assertEquals(0, account1.getAvailableBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, account1.getFrozenBalance().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, account2.getAvailableBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, account2.getFrozenBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("process should handle non-existent accounts")
    void process_ShouldHandleNonExistentAccounts() {
        // Setup
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.getAccountKeys()).thenReturn(Arrays.asList("account1", "nonexistent"));

        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(new BigDecimal("1000.00"));
        account1.setFrozenBalance(new BigDecimal("0.00"));

        BalanceLock newLock = BalanceLockFactory.create();

        // Mock the behavior
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockAccountCache.getAccount("nonexistent")).thenReturn(Optional.empty());
        when(mockBalancesLockEvent.toBalanceLock(false)).thenReturn(newLock);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        
        // Verify only existing account was processed
        verify(mockAccountCache).addAccountToBatch(account1);
        verify(mockBalanceLockCache).addBalanceLock(newLock);
    }

    @Test
    @DisplayName("process should release balance lock successfully")
    void process_ShouldReleaseBalanceLockSuccessfully() {
        // Setup
        String lockId = "test-lock-id";
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_RELEASE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(lockId);

        // Create existing lock with locked balances
        BalanceLock existingLock = BalanceLockFactory.create();
        existingLock.setLockId(lockId);
        existingLock.setStatus("LOCKED");
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        lockedBalances.put("account1", new BigDecimal("1000.00"));
        lockedBalances.put("account2", new BigDecimal("500.00"));
        existingLock.setLockedBalances(lockedBalances);

        // Create mock accounts with frozen balance
        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(BigDecimal.ZERO);
        account1.setFrozenBalance(new BigDecimal("1000.00"));

        Account account2 = AccountFactory.create("account2");
        account2.setAvailableBalance(BigDecimal.ZERO);
        account2.setFrozenBalance(new BigDecimal("500.00"));

        // Mock the behavior
        when(mockBalanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(existingLock));
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockAccountCache.getAccount("account2")).thenReturn(Optional.of(account2));

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        assertEquals("RELEASED", result.getBalanceLock().get().getStatus());
        
        // Verify accounts were processed
        verify(mockAccountCache).addAccountToBatch(account1);
        verify(mockAccountCache).addAccountToBatch(account2);
        verify(mockBalanceLockCache).addBalanceLock(existingLock);
        
        // Verify balances were unfrozen - use compareTo for BigDecimal comparison
        assertEquals(0, account1.getAvailableBalance().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, account1.getFrozenBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, account2.getAvailableBalance().compareTo(new BigDecimal("500.00")));
        assertEquals(0, account2.getFrozenBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("process should handle release of non-existent lock")
    void process_ShouldHandleReleaseOfNonExistentLock() {
        // Setup
        String lockId = "nonexistent-lock-id";
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_RELEASE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(lockId);

        // Mock the behavior
        when(mockBalanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.empty());
        
        // Mock the error message
        String expectedErrorMessage = "Không tìm thấy khóa với ID: " + lockId;
        when(mockEvent.getErrorMessage()).thenReturn(expectedErrorMessage);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertFalse(result.getEvent().isSuccess());
        verify(mockEvent).setErrorMessage(argThat(message -> 
            message.contains("Không tìm thấy khóa với ID") && message.contains(lockId)));
    }

    @Test
    @DisplayName("process should handle release of already released lock")
    void process_ShouldHandleReleaseOfAlreadyReleasedLock() {
        // Setup
        String lockId = "already-released-lock-id";
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_RELEASE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(lockId);

        // Create already released lock
        BalanceLock releasedLock = BalanceLockFactory.create();
        releasedLock.setLockId(lockId);
        releasedLock.setStatus("RELEASED");

        // Mock the behavior
        when(mockBalanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(releasedLock));
        
        // Mock the error message
        String expectedErrorMessage = "Khóa đã được giải phóng trước đó: " + lockId;
        when(mockEvent.getErrorMessage()).thenReturn(expectedErrorMessage);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertFalse(result.getEvent().isSuccess());
        verify(mockEvent).setErrorMessage(argThat(message -> 
            message.contains("Khóa đã được giải phóng trước đó") && message.contains(lockId)));
    }

    @Test
    @DisplayName("process should handle unsupported operation type")
    void process_ShouldHandleUnsupportedOperationType() {
        // Setup
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.AMM_POOL_CREATE);
        
        // Mock the error message
        String expectedErrorMessage = "OperationType không được hỗ trợ: " + OperationType.AMM_POOL_CREATE.getValue();
        when(mockEvent.getErrorMessage()).thenReturn(expectedErrorMessage);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertFalse(result.getEvent().isSuccess());
        verify(mockEvent).setErrorMessage(argThat(message -> 
            message.contains("OperationType không được hỗ trợ") && 
            message.contains(OperationType.AMM_POOL_CREATE.getValue())));
    }

    @Test
    @DisplayName("process should handle exception during processing")
    void process_ShouldHandleExceptionDuringProcessing() {
        // Setup
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.toBalanceLock(false)).thenThrow(new RuntimeException("Test exception"));
        
        // Mock the error message
        String expectedErrorMessage = "Lỗi khi xử lý BalancesLockEvent: Test exception";
        when(mockEvent.getErrorMessage()).thenReturn(expectedErrorMessage);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertFalse(result.getEvent().isSuccess());
        verify(mockEvent).setErrorMessage(argThat(message -> 
            message.contains("Lỗi khi xử lý BalancesLockEvent") && message.contains("Test exception")));
    }

    @Test
    @DisplayName("process should handle create lock with no accounts having balance")
    void process_ShouldHandleCreateLockWithNoAccountsHavingBalance() {
        // Setup
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.getAccountKeys()).thenReturn(Arrays.asList("account1", "account2"));

        // Create mock accounts with zero balance
        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(BigDecimal.ZERO);

        Account account2 = AccountFactory.create("account2");
        account2.setAvailableBalance(BigDecimal.ZERO);

        BalanceLock newLock = BalanceLockFactory.create();

        // Mock the behavior
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockAccountCache.getAccount("account2")).thenReturn(Optional.of(account2));
        when(mockBalancesLockEvent.toBalanceLock(false)).thenReturn(newLock);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        
        // Verify lock was still created even though no balances were locked
        verify(mockBalanceLockCache).addBalanceLock(newLock);
        
        // Verify no accounts were added to batch (no balance changes)
        verify(mockAccountCache, never()).addAccountToBatch(any());
    }

    @Test
    @DisplayName("process should handle release with non-existent accounts")
    void process_ShouldHandleReleaseWithNonExistentAccounts() {
        // Setup
        String lockId = "test-lock-id";
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_RELEASE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(lockId);

        // Create existing lock with locked balances for non-existent accounts
        BalanceLock existingLock = BalanceLockFactory.create();
        existingLock.setLockId(lockId);
        existingLock.setStatus("LOCKED");
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        lockedBalances.put("nonexistent1", new BigDecimal("1000.00"));
        lockedBalances.put("nonexistent2", new BigDecimal("500.00"));
        existingLock.setLockedBalances(lockedBalances);

        // Mock the behavior
        when(mockBalanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(existingLock));
        when(mockAccountCache.getAccount("nonexistent1")).thenReturn(Optional.empty());
        when(mockAccountCache.getAccount("nonexistent2")).thenReturn(Optional.empty());

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        assertEquals("RELEASED", result.getBalanceLock().get().getStatus());
        
        // Verify lock was updated even though accounts don't exist
        verify(mockBalanceLockCache).addBalanceLock(existingLock);
        
        // Verify no accounts were processed
        verify(mockAccountCache, never()).addAccountToBatch(any());
    }

    @Test
    @DisplayName("freezeAccountBalance should handle zero amount")
    void freezeAccountBalance_ShouldHandleZeroAmount() {
        // This test verifies the private method behavior indirectly
        // Setup
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.getAccountKeys()).thenReturn(Arrays.asList("account1"));

        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(BigDecimal.ZERO); // Zero balance
        account1.setFrozenBalance(new BigDecimal("100.00"));

        BalanceLock newLock = BalanceLockFactory.create();

        // Mock the behavior
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockBalancesLockEvent.toBalanceLock(false)).thenReturn(newLock);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        
        // Verify account balance unchanged (zero amount should not be processed) - use compareTo for BigDecimal comparison
        assertEquals(0, account1.getAvailableBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, account1.getFrozenBalance().compareTo(new BigDecimal("100.00")));
        
        // Verify account was not added to batch since no changes were made
        verify(mockAccountCache, never()).addAccountToBatch(account1);
    }

    @Test
    @DisplayName("process should set first processed account in result for create")
    void process_ShouldSetFirstProcessedAccountInResultForCreate() {
        // Setup
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.getAccountKeys()).thenReturn(Arrays.asList("account1", "account2"));

        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(new BigDecimal("1000.00"));

        Account account2 = AccountFactory.create("account2");
        account2.setAvailableBalance(new BigDecimal("500.00"));

        BalanceLock newLock = BalanceLockFactory.create();

        // Mock the behavior
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockAccountCache.getAccount("account2")).thenReturn(Optional.of(account2));
        when(mockBalancesLockEvent.toBalanceLock(false)).thenReturn(newLock);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getAccount().isPresent());
        // Should be one of the processed accounts (implementation uses iterator.next())
        assertTrue(result.getAccount().get().getKey().equals("account1") || 
                  result.getAccount().get().getKey().equals("account2"));
    }

    @Test
    @DisplayName("process should set first processed account in result for release")
    void process_ShouldSetFirstProcessedAccountInResultForRelease() {
        // Setup
        String lockId = "test-lock-id";
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_RELEASE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(lockId);

        BalanceLock existingLock = BalanceLockFactory.create();
        existingLock.setLockId(lockId);
        existingLock.setStatus("LOCKED");
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        lockedBalances.put("account1", new BigDecimal("1000.00"));
        lockedBalances.put("account2", new BigDecimal("500.00"));
        existingLock.setLockedBalances(lockedBalances);

        Account account1 = AccountFactory.create("account1");
        account1.setFrozenBalance(new BigDecimal("1000.00"));

        Account account2 = AccountFactory.create("account2");
        account2.setFrozenBalance(new BigDecimal("500.00"));

        // Mock the behavior
        when(mockBalanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(existingLock));
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockAccountCache.getAccount("account2")).thenReturn(Optional.of(account2));

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getAccount().isPresent());
        // Should be one of the processed accounts
        assertTrue(result.getAccount().get().getKey().equals("account1") || 
                  result.getAccount().get().getKey().equals("account2"));
    }

    @Test
    @DisplayName("unfreezeAccountBalance should handle zero amount")
    void unfreezeAccountBalance_ShouldHandleZeroAmount() {
        // Setup
        String lockId = "test-lock-id";
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_RELEASE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(lockId);

        // Create a lock with zero balance
        BalanceLock existingLock = BalanceLockFactory.create();
        existingLock.setLockId(lockId);
        existingLock.setStatus("LOCKED");
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        lockedBalances.put("account1", BigDecimal.ZERO);
        existingLock.setLockedBalances(lockedBalances);

        // Create an account with unchanged balances
        Account account1 = AccountFactory.create("account1");
        BigDecimal originalAvailable = new BigDecimal("100.00");
        BigDecimal originalFrozen = new BigDecimal("50.00");
        account1.setAvailableBalance(originalAvailable);
        account1.setFrozenBalance(originalFrozen);

        // Mock the behavior
        when(mockBalanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(existingLock));
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        assertEquals("RELEASED", result.getBalanceLock().get().getStatus());
        
        // Verify account balances were not changed - use compareTo for BigDecimal comparison
        assertEquals(0, account1.getAvailableBalance().compareTo(originalAvailable));
        assertEquals(0, account1.getFrozenBalance().compareTo(originalFrozen));
        
        // Verify account was not added to batch since zero amount should not cause account changes
        verify(mockAccountCache, never()).addAccountToBatch(account1);
    }

    @Test
    @DisplayName("unfreezeAccountBalance should handle negative amount")
    void unfreezeAccountBalance_ShouldHandleNegativeAmount() {
        // Setup
        String lockId = "test-lock-id";
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_RELEASE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(lockId);

        // Create a lock with negative balance (shouldn't happen in practice, but testing edge case)
        BalanceLock existingLock = BalanceLockFactory.create();
        existingLock.setLockId(lockId);
        existingLock.setStatus("LOCKED");
        Map<String, BigDecimal> lockedBalances = new HashMap<>();
        lockedBalances.put("account1", new BigDecimal("-10.00"));
        existingLock.setLockedBalances(lockedBalances);

        // Create an account with unchanged balances
        Account account1 = AccountFactory.create("account1");
        BigDecimal originalAvailable = new BigDecimal("100.00");
        BigDecimal originalFrozen = new BigDecimal("50.00");
        account1.setAvailableBalance(originalAvailable);
        account1.setFrozenBalance(originalFrozen);

        // Mock the behavior
        when(mockBalanceLockCache.getBalanceLock(lockId)).thenReturn(Optional.of(existingLock));
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        assertEquals("RELEASED", result.getBalanceLock().get().getStatus());
        
        // Verify account balances were not changed - use compareTo for BigDecimal comparison
        assertEquals(0, account1.getAvailableBalance().compareTo(originalAvailable));
        assertEquals(0, account1.getFrozenBalance().compareTo(originalFrozen));
        
        // Verify account was not added to batch since negative amount should not cause account changes
        verify(mockAccountCache, never()).addAccountToBatch(account1);
    }

    @Test
    @DisplayName("process should preserve specified lockId when creating lock")
    void process_ShouldPreserveSpecifiedLockId_WhenCreatingLock() {
        // Setup
        String requestedLockId = "specified-lock-id";
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(requestedLockId);
        when(mockBalancesLockEvent.getAccountKeys()).thenReturn(Arrays.asList("account1"));

        // Create mock account with balance
        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(new BigDecimal("100.00"));
        account1.setFrozenBalance(BigDecimal.ZERO);

        // Create a mock BalanceLock with the requested lockId
        BalanceLock newLock = BalanceLockFactory.create();
        newLock.setLockId(requestedLockId); // Important: set the requested lockId
        
        // Mock the behavior
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockBalancesLockEvent.toBalanceLock(false)).thenReturn(newLock);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        assertEquals(requestedLockId, result.getBalanceLock().get().getLockId(), 
                "Lock ID should match the requested ID");
        
        // Verify the account balance was frozen
        verify(mockAccountCache).addAccountToBatch(account1);
        
        // Verify lock was stored with the specified ID
        verify(mockBalanceLockCache).addBalanceLock(newLock);
    }

    @Test
    @DisplayName("process should handle changed lockId in log message")
    void process_ShouldHandleChangedLockId_InLogMessage() {
        // Setup
        String requestedLockId = "requested-lock-id";
        String actualLockId = "actual-lock-id"; // Different from requested
        
        when(mockBalancesLockEvent.getOperationType()).thenReturn(OperationType.BALANCES_LOCK_CREATE);
        when(mockBalancesLockEvent.getLockId()).thenReturn(requestedLockId);
        when(mockBalancesLockEvent.getAccountKeys()).thenReturn(Arrays.asList("account1"));

        // Create mock account with balance
        Account account1 = AccountFactory.create("account1");
        account1.setAvailableBalance(new BigDecimal("100.00"));
        account1.setFrozenBalance(BigDecimal.ZERO);

        // Create a mock BalanceLock with a different lockId than requested
        BalanceLock newLock = BalanceLockFactory.create();
        newLock.setLockId(actualLockId); // Different from requested
        
        // Mock the behavior
        when(mockAccountCache.getAccount("account1")).thenReturn(Optional.of(account1));
        when(mockBalancesLockEvent.toBalanceLock(false)).thenReturn(newLock);

        // Execute
        ProcessResult result = processor.process();

        // Verify
        assertNotNull(result);
        assertTrue(result.getBalanceLock().isPresent());
        assertEquals(actualLockId, result.getBalanceLock().get().getLockId());
        assertNotEquals(requestedLockId, result.getBalanceLock().get().getLockId());
        
        // We can't directly test the log message, but this code path should be covered
        
        // Verify the account was processed and lock was stored
        verify(mockAccountCache).addAccountToBatch(account1);
        verify(mockBalanceLockCache).addBalanceLock(newLock);
    }
} 