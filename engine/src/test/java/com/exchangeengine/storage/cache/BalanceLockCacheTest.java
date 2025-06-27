package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.BalanceLockFactory;
import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.BalanceLock;
import com.exchangeengine.storage.rocksdb.BalanceLockRocksDB;

/**
 * Test cho BalanceLockCache
 */
@ExtendWith({MockitoExtension.class, CombinedTestExtension.class})
@DisplayName("BalanceLockCache Tests")
public class BalanceLockCacheTest {

    @Mock
    private BalanceLockRocksDB mockBalanceLockRocksDB;

    private BalanceLockCache balanceLockCache;
    private AutoCloseable closeable;
    private Map<String, BalanceLock> locksMap;
    private Map<String, BalanceLock> lockBatchMap;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);

        // Reset BalanceLockCache instance
        Field instanceField = BalanceLockCache.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Mock BalanceLockRocksDB instance
        Field rocksDBField = BalanceLockRocksDB.class.getDeclaredField("instance");
        rocksDBField.setAccessible(true);
        rocksDBField.set(null, mockBalanceLockRocksDB);

        // Setup default mock behavior
        lenient().when(mockBalanceLockRocksDB.getAllBalanceLocks()).thenReturn(new ArrayList<>());

        balanceLockCache = BalanceLockCache.getInstance();

        // Get access to internal maps for testing
        Field locksField = BalanceLockCache.class.getDeclaredField("locks");
        locksField.setAccessible(true);
        locksMap = (Map<String, BalanceLock>) locksField.get(balanceLockCache);

        Field lockBatchField = BalanceLockCache.class.getDeclaredField("lockBatch");
        lockBatchField.setAccessible(true);
        lockBatchMap = (Map<String, BalanceLock>) lockBatchField.get(balanceLockCache);

        // Clear all maps
        locksMap.clear();
        lockBatchMap.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();

        // Reset instances
        Field instanceField = BalanceLockCache.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Field rocksDBField = BalanceLockRocksDB.class.getDeclaredField("instance");
        rocksDBField.setAccessible(true);
        rocksDBField.set(null, null);
    }

    @Test
    @DisplayName("getInstance should return same instance")
    void getInstance_ShouldReturnSameInstance() {
        // Act
        BalanceLockCache instance1 = BalanceLockCache.getInstance();
        BalanceLockCache instance2 = BalanceLockCache.getInstance();

        // Assert
        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("setTestInstance should set specific instance")
    void setTestInstance_ShouldSetSpecificInstance() {
        // Arrange
        BalanceLockCache testInstance = mock(BalanceLockCache.class);

        // Act
        BalanceLockCache.setTestInstance(testInstance);
        BalanceLockCache result = BalanceLockCache.getInstance();

        // Assert
        assertSame(testInstance, result);

        // Reset for subsequent tests
        BalanceLockCache.resetInstance();
    }

    @Test
    @DisplayName("resetInstance should clear instance")
    void resetInstance_ShouldClearInstance() {
        // Arrange
        BalanceLockCache firstInstance = BalanceLockCache.getInstance();

        // Act
        BalanceLockCache.resetInstance();
        BalanceLockCache secondInstance = BalanceLockCache.getInstance();

        // Assert
        assertNotSame(firstInstance, secondInstance);
    }

    @Test
    @DisplayName("getBalanceLock should return lock from cache")
    void getBalanceLock_ShouldReturnLockFromCache() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();
        locksMap.put(lock.getLockId(), lock);

        // Act
        Optional<BalanceLock> result = balanceLockCache.getBalanceLock(lock.getLockId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(lock, result.get());
    }

    @Test
    @DisplayName("addBalanceLock should add lock to cache")
    void addBalanceLock_ShouldAddLockToCache() {
        // Arrange
        List<String> accountKeys = Arrays.asList("account1", "account2");
        BalanceLock lock = BalanceLockFactory.create(ActionType.TRADE, "action-id", accountKeys, "identifier");

        // Act
        BalanceLock result = balanceLockCache.addBalanceLock(lock);

        // Assert
        assertEquals(lock, result);
        assertTrue(locksMap.containsKey(lock.getLockId()));
        assertEquals(lock, locksMap.get(lock.getLockId()));
    }

    @Test
    @DisplayName("addBalanceLockToBatch should add lock to batch")
    void addBalanceLockToBatch_ShouldAddLockToBatch() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();

        // Act
        BalanceLock result = balanceLockCache.addBalanceLockToBatch(lock);

        // Assert
        assertEquals(lock, result);
        assertTrue(lockBatchMap.containsKey(lock.getLockId()));
        assertEquals(lock, lockBatchMap.get(lock.getLockId()));
    }

    @Test
    @DisplayName("getAccountLocks should return locks for account")
    void getAccountLocks_ShouldReturnLocksForAccount() {
        // Arrange
        String accountKey = "account1";
        List<String> accountKeys = Arrays.asList(accountKey, "account2");
        BalanceLock lock = BalanceLockFactory.create(ActionType.TRADE, "action-id", accountKeys, "identifier");
        balanceLockCache.addBalanceLock(lock);

        // Act
        Set<String> result = balanceLockCache.getAccountLocks(accountKey);

        // Assert
        assertFalse(result.isEmpty());
        assertTrue(result.contains(lock.getLockId()));
    }

    @Test
    @DisplayName("getAccountLocks should return empty set for unknown account")
    void getAccountLocks_ShouldReturnEmptySet_ForUnknownAccount() {
        // Act
        Set<String> result = balanceLockCache.getAccountLocks("unknown-account");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getActiveAccountLocks should return only locked status locks")
    void getActiveAccountLocks_ShouldReturnOnlyLockedStatusLocks() {
        // Arrange
        String accountKey = "account1";
        List<String> accountKeys = Arrays.asList(accountKey, "account2");
        
        // Add a locked balance lock
        BalanceLock lockedLock = BalanceLockFactory.create(ActionType.TRADE, "action-id-1", accountKeys, "identifier");
        balanceLockCache.addBalanceLock(lockedLock);
        
        // Add a released balance lock
        BalanceLock releasedLock = BalanceLockFactory.createReleased();
        releasedLock.setAccountKeys(accountKeys);
        balanceLockCache.addBalanceLock(releasedLock);

        // Act
        Set<String> result = balanceLockCache.getActiveAccountLocks(accountKey);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(lockedLock.getLockId()));
        assertFalse(result.contains(releasedLock.getLockId()));
    }

    @Test
    @DisplayName("removeBalanceLock should remove lock from cache")
    void removeBalanceLock_ShouldRemoveLockFromCache() {
        // Arrange
        List<String> accountKeys = Arrays.asList("account1", "account2");
        BalanceLock lock = BalanceLockFactory.create(ActionType.TRADE, "action-id", accountKeys, "identifier");
        balanceLockCache.addBalanceLock(lock);

        // Act
        balanceLockCache.removeBalanceLock(lock.getLockId());

        // Assert
        assertFalse(locksMap.containsKey(lock.getLockId()));
        
        // Verify lock is no longer returned by getAccountLocks
        assertTrue(balanceLockCache.getAccountLocks("account1").isEmpty());
        assertTrue(balanceLockCache.getAccountLocks("account2").isEmpty());
    }

    @Test
    @DisplayName("removeBalanceLock should handle non-existent lock")
    void removeBalanceLock_ShouldHandleNonExistentLock() {
        // Act & Assert - Should not throw
        balanceLockCache.removeBalanceLock("nonexistent");
    }

    @Test
    @DisplayName("clearBalanceLocks should clear all locks")
    void clearBalanceLocks_ShouldClearAllLocks() {
        // Arrange
        BalanceLock lock1 = BalanceLockFactory.create();
        BalanceLock lock2 = BalanceLockFactory.create();
        balanceLockCache.addBalanceLock(lock1);
        balanceLockCache.addBalanceLock(lock2);

        // Act
        balanceLockCache.clearBalanceLocks();

        // Assert
        assertTrue(locksMap.isEmpty());
        assertTrue(balanceLockCache.getAccountLocks("any-account").isEmpty());
    }

    @Test
    @DisplayName("getBalanceLocks should return all locks")
    void getBalanceLocks_ShouldReturnAllLocks() {
        // Arrange
        BalanceLock lock1 = BalanceLockFactory.create();
        BalanceLock lock2 = BalanceLockFactory.create();
        balanceLockCache.addBalanceLock(lock1);
        balanceLockCache.addBalanceLock(lock2);

        // Act
        Map<String, BalanceLock> result = balanceLockCache.getBalanceLocks();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.containsKey(lock1.getLockId()));
        assertTrue(result.containsKey(lock2.getLockId()));
    }

    @Test
    @DisplayName("loadBalanceLocksFromRocksDB should load locks from RocksDB")
    void loadBalanceLocksFromRocksDB_ShouldLoadLocksFromRocksDB() {
        // Arrange
        BalanceLock lock1 = BalanceLockFactory.create();
        BalanceLock lock2 = BalanceLockFactory.create();
        List<BalanceLock> locks = Arrays.asList(lock1, lock2);
        when(mockBalanceLockRocksDB.getAllBalanceLocks()).thenReturn(locks);

        // Act
        balanceLockCache.loadBalanceLocksFromRocksDB();

        // Assert
        assertEquals(2, locksMap.size());
        assertTrue(locksMap.containsKey(lock1.getLockId()));
        assertTrue(locksMap.containsKey(lock2.getLockId()));
        verify(mockBalanceLockRocksDB).getAllBalanceLocks();
    }

    @Test
    @DisplayName("loadBalanceLocksFromRocksDB should handle exception gracefully")
    void loadBalanceLocksFromRocksDB_ShouldHandleExceptionGracefully() {
        // Arrange
        when(mockBalanceLockRocksDB.getAllBalanceLocks()).thenThrow(new RuntimeException("Test exception"));

        // Act & Assert - Should not throw
        balanceLockCache.loadBalanceLocksFromRocksDB();
        
        // Verify the method was called despite exception
        verify(mockBalanceLockRocksDB).getAllBalanceLocks();
    }

    @Test
    @DisplayName("saveBalanceLockBatch should save batch to RocksDB and clear batch")
    void saveBalanceLockBatch_ShouldSaveBatchToRocksDBAndClearBatch() {
        // Arrange
        BalanceLock lock1 = BalanceLockFactory.create();
        BalanceLock lock2 = BalanceLockFactory.create();
        balanceLockCache.addBalanceLockToBatch(lock1);
        balanceLockCache.addBalanceLockToBatch(lock2);

        // Act
        balanceLockCache.saveBalanceLockBatch();

        // Assert
        verify(mockBalanceLockRocksDB).saveBalanceLockBatch(lockBatchMap);
        assertTrue(lockBatchMap.isEmpty());
    }

    @Test
    @DisplayName("saveBalanceLockBatch should handle exception gracefully")
    void saveBalanceLockBatch_ShouldHandleExceptionGracefully() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();
        balanceLockCache.addBalanceLockToBatch(lock);
        
        doThrow(new RuntimeException("Test exception"))
            .when(mockBalanceLockRocksDB).saveBalanceLockBatch(any());

        // Act - Should not throw
        balanceLockCache.saveBalanceLockBatch();

        // Assert
        verify(mockBalanceLockRocksDB).saveBalanceLockBatch(lockBatchMap);
        assertTrue(lockBatchMap.isEmpty());
    }

    @Test
    @DisplayName("addBalanceLock should handle lock with null account keys")
    void addBalanceLock_ShouldHandleLockWithNullAccountKeys() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();
        lock.setAccountKeys(null);

        // Act - Should not throw
        BalanceLock result = balanceLockCache.addBalanceLock(lock);

        // Assert
        assertEquals(lock, result);
        assertTrue(locksMap.containsKey(lock.getLockId()));
    }

    @Test
    @DisplayName("removeBalanceLock should handle lock with null account keys")
    void removeBalanceLock_ShouldHandleLockWithNullAccountKeys() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();
        lock.setAccountKeys(null);
        balanceLockCache.addBalanceLock(lock);

        // Act - Should not throw
        balanceLockCache.removeBalanceLock(lock.getLockId());

        // Assert
        assertFalse(locksMap.containsKey(lock.getLockId()));
    }

    @Test
    @DisplayName("getActiveAccountLocks should handle account not in any locks")
    void getActiveAccountLocks_ShouldHandleAccountNotInAnyLocks() {
        // Act
        Set<String> result = balanceLockCache.getActiveAccountLocks("unknown-account");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getActiveAccountLocks should filter locks that don't contain the account")
    void getActiveAccountLocks_ShouldFilterLocksNotContainingAccount() {
        // Arrange
        String targetAccount = "target-account";
        String otherAccount = "other-account";
        
        // Add a lock that contains target account
        BalanceLock lockWithAccount = BalanceLockFactory.create();
        lockWithAccount.setAccountKeys(Arrays.asList(targetAccount, otherAccount));
        balanceLockCache.addBalanceLock(lockWithAccount);
        
        // Add a lock that doesn't contain target account
        BalanceLock lockWithoutAccount = BalanceLockFactory.create();
        lockWithoutAccount.setAccountKeys(Arrays.asList(otherAccount));
        balanceLockCache.addBalanceLock(lockWithoutAccount);

        // Act
        Set<String> result = balanceLockCache.getActiveAccountLocks(targetAccount);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(lockWithAccount.getLockId()));
        assertFalse(result.contains(lockWithoutAccount.getLockId()));
    }
} 