package com.exchangeengine.storage.rocksdb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rocksdb.ColumnFamilyHandle;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.BalanceLockFactory;
import com.exchangeengine.model.BalanceLock;

/**
 * Test cho BalanceLockRocksDB
 */
@ExtendWith({MockitoExtension.class, CombinedTestExtension.class})
@DisplayName("BalanceLockRocksDB Tests")
public class BalanceLockRocksDBTest {

    @Mock
    private RocksDBService mockRocksDBService;

    @Mock
    private ColumnFamilyHandle mockColumnFamilyHandle;

    private BalanceLockRocksDB balanceLockRocksDB;
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);

        // Reset BalanceLockRocksDB instance
        Field instanceField = BalanceLockRocksDB.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Mock RocksDBService instance
        Field rocksDBField = RocksDBService.class.getDeclaredField("instance");
        rocksDBField.setAccessible(true);
        rocksDBField.set(null, mockRocksDBService);

        // Setup default mock behavior
        lenient().when(mockRocksDBService.getBalanceLockCF()).thenReturn(mockColumnFamilyHandle);

        balanceLockRocksDB = BalanceLockRocksDB.getInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();

        // Reset instances
        Field instanceField = BalanceLockRocksDB.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Field rocksDBField = RocksDBService.class.getDeclaredField("instance");
        rocksDBField.setAccessible(true);
        rocksDBField.set(null, null);
    }

    @Test
    @DisplayName("getInstance should return same instance")
    void getInstance_ShouldReturnSameInstance() {
        // Act
        BalanceLockRocksDB instance1 = BalanceLockRocksDB.getInstance();
        BalanceLockRocksDB instance2 = BalanceLockRocksDB.getInstance();

        // Assert
        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("setTestInstance should set specific instance")
    void setTestInstance_ShouldSetSpecificInstance() {
        // Arrange
        BalanceLockRocksDB testInstance = mock(BalanceLockRocksDB.class);

        // Act
        BalanceLockRocksDB.setTestInstance(testInstance);
        BalanceLockRocksDB result = BalanceLockRocksDB.getInstance();

        // Assert
        assertSame(testInstance, result);

        // Reset for subsequent tests
        BalanceLockRocksDB.resetInstance();
    }

    @Test
    @DisplayName("resetInstance should clear instance")
    void resetInstance_ShouldClearInstance() {
        // Arrange
        BalanceLockRocksDB firstInstance = BalanceLockRocksDB.getInstance();

        // Act
        BalanceLockRocksDB.resetInstance();
        BalanceLockRocksDB secondInstance = BalanceLockRocksDB.getInstance();

        // Assert
        assertNotSame(firstInstance, secondInstance);
    }

    @Test
    @DisplayName("getBalanceLock should return balance lock when found")
    void getBalanceLock_ShouldReturnBalanceLock_WhenFound() {
        // Arrange
        BalanceLock expectedLock = BalanceLockFactory.create();
        String lockId = expectedLock.getLockId();
        
        when(mockRocksDBService.getObject(
            eq(lockId),
            eq(mockColumnFamilyHandle),
            eq(BalanceLock.class),
            eq("balance_lock")
        )).thenReturn(Optional.of(expectedLock));

        // Act
        Optional<BalanceLock> result = balanceLockRocksDB.getBalanceLock(lockId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedLock, result.get());
        verify(mockRocksDBService).getObject(lockId, mockColumnFamilyHandle, BalanceLock.class, "balance_lock");
    }

    @Test
    @DisplayName("getBalanceLock should return empty when not found")
    void getBalanceLock_ShouldReturnEmpty_WhenNotFound() {
        // Arrange
        String lockId = "nonexistent";
        
        when(mockRocksDBService.getObject(
            eq(lockId),
            eq(mockColumnFamilyHandle),
            eq(BalanceLock.class),
            eq("balance_lock")
        )).thenReturn(Optional.empty());

        // Act
        Optional<BalanceLock> result = balanceLockRocksDB.getBalanceLock(lockId);

        // Assert
        assertFalse(result.isPresent());
        verify(mockRocksDBService).getObject(lockId, mockColumnFamilyHandle, BalanceLock.class, "balance_lock");
    }

    @Test
    @DisplayName("getBalanceLock should return empty for null lockId")
    void getBalanceLock_ShouldReturnEmpty_ForNullLockId() {
        // Act
        Optional<BalanceLock> result = balanceLockRocksDB.getBalanceLock(null);

        // Assert
        assertFalse(result.isPresent());
        verify(mockRocksDBService, never()).getObject(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getBalanceLock should return empty for empty lockId")
    void getBalanceLock_ShouldReturnEmpty_ForEmptyLockId() {
        // Act
        Optional<BalanceLock> result = balanceLockRocksDB.getBalanceLock("");

        // Assert
        assertFalse(result.isPresent());
        verify(mockRocksDBService, never()).getObject(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getAllBalanceLocks should return all balance locks")
    void getAllBalanceLocks_ShouldReturnAllBalanceLocks() {
        // Arrange
        List<BalanceLock> expectedLocks = Arrays.asList(
            BalanceLockFactory.create(),
            BalanceLockFactory.create()
        );
        
        when(mockRocksDBService.getAllObjects(
            eq(mockColumnFamilyHandle),
            eq(BalanceLock.class),
            eq("balance_locks")
        )).thenReturn(expectedLocks);

        // Act
        List<BalanceLock> result = balanceLockRocksDB.getAllBalanceLocks();

        // Assert
        assertEquals(expectedLocks.size(), result.size());
        assertEquals(expectedLocks, result);
        verify(mockRocksDBService).getAllObjects(mockColumnFamilyHandle, BalanceLock.class, "balance_locks");
    }

    @Test
    @DisplayName("getAllBalanceLocks should handle empty result")
    void getAllBalanceLocks_ShouldHandleEmptyResult() {
        // Arrange
        when(mockRocksDBService.getAllObjects(
            eq(mockColumnFamilyHandle),
            eq(BalanceLock.class),
            eq("balance_locks")
        )).thenReturn(Arrays.asList());

        // Act
        List<BalanceLock> result = balanceLockRocksDB.getAllBalanceLocks();

        // Assert
        assertTrue(result.isEmpty());
        verify(mockRocksDBService).getAllObjects(mockColumnFamilyHandle, BalanceLock.class, "balance_locks");
    }

    @Test
    @DisplayName("saveBalanceLock should save balance lock")
    void saveBalanceLock_ShouldSaveBalanceLock() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();

        // Act
        balanceLockRocksDB.saveBalanceLock(lock);

        // Assert
        verify(mockRocksDBService).saveObject(
            eq(lock),
            eq(mockColumnFamilyHandle),
            any(),
            eq("balance_lock")
        );
    }

    @Test
    @DisplayName("saveBalanceLock should handle null balance lock")
    void saveBalanceLock_ShouldHandleNullBalanceLock() {
        // Act
        balanceLockRocksDB.saveBalanceLock(null);

        // Assert
        verify(mockRocksDBService, never()).saveObject(any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveBalanceLockBatch should save batch of balance locks")
    void saveBalanceLockBatch_ShouldSaveBatchOfBalanceLocks() {
        // Arrange
        BalanceLock lock1 = BalanceLockFactory.create();
        BalanceLock lock2 = BalanceLockFactory.create();
        Map<String, BalanceLock> balanceLocks = new HashMap<>();
        balanceLocks.put(lock1.getLockId(), lock1);
        balanceLocks.put(lock2.getLockId(), lock2);

        // Act
        balanceLockRocksDB.saveBalanceLockBatch(balanceLocks);

        // Assert
        verify(mockRocksDBService).saveBatch(
            eq(balanceLocks),
            eq(mockColumnFamilyHandle),
            any(),
            eq("balance_locks")
        );
    }

    @Test
    @DisplayName("saveBalanceLockBatch should handle null map")
    void saveBalanceLockBatch_ShouldHandleNullMap() {
        // Act
        balanceLockRocksDB.saveBalanceLockBatch(null);

        // Assert
        verify(mockRocksDBService, never()).saveBatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveBalanceLockBatch should handle empty map")
    void saveBalanceLockBatch_ShouldHandleEmptyMap() {
        // Arrange
        Map<String, BalanceLock> emptyMap = new HashMap<>();

        // Act
        balanceLockRocksDB.saveBalanceLockBatch(emptyMap);

        // Assert
        verify(mockRocksDBService, never()).saveBatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getBalanceLock should handle RocksDBService exception")
    void getBalanceLock_ShouldHandleRocksDBServiceException() {
        // Arrange
        String lockId = "test-lock-id";
        when(mockRocksDBService.getObject(
            eq(lockId),
            eq(mockColumnFamilyHandle),
            eq(BalanceLock.class),
            eq("balance_lock")
        )).thenThrow(new RuntimeException("RocksDB error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> balanceLockRocksDB.getBalanceLock(lockId));
        verify(mockRocksDBService).getObject(lockId, mockColumnFamilyHandle, BalanceLock.class, "balance_lock");
    }

    @Test
    @DisplayName("getAllBalanceLocks should handle RocksDBService exception")
    void getAllBalanceLocks_ShouldHandleRocksDBServiceException() {
        // Arrange
        when(mockRocksDBService.getAllObjects(
            eq(mockColumnFamilyHandle),
            eq(BalanceLock.class),
            eq("balance_locks")
        )).thenThrow(new RuntimeException("RocksDB error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> balanceLockRocksDB.getAllBalanceLocks());
        verify(mockRocksDBService).getAllObjects(mockColumnFamilyHandle, BalanceLock.class, "balance_locks");
    }

    @Test
    @DisplayName("saveBalanceLock should handle RocksDBService exception")
    void saveBalanceLock_ShouldHandleRocksDBServiceException() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();
        doThrow(new RuntimeException("RocksDB error")).when(mockRocksDBService).saveObject(
            eq(lock),
            eq(mockColumnFamilyHandle),
            any(),
            eq("balance_lock")
        );

        // Act & Assert
        assertThrows(RuntimeException.class, () -> balanceLockRocksDB.saveBalanceLock(lock));
        verify(mockRocksDBService).saveObject(eq(lock), eq(mockColumnFamilyHandle), any(), eq("balance_lock"));
    }

    @Test
    @DisplayName("saveBalanceLockBatch should handle RocksDBService exception")
    void saveBalanceLockBatch_ShouldHandleRocksDBServiceException() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();
        Map<String, BalanceLock> balanceLocks = new HashMap<>();
        balanceLocks.put(lock.getLockId(), lock);
        
        doThrow(new RuntimeException("RocksDB error")).when(mockRocksDBService).saveBatch(
            eq(balanceLocks),
            eq(mockColumnFamilyHandle),
            any(),
            eq("balance_locks")
        );

        // Act & Assert
        assertThrows(RuntimeException.class, () -> balanceLockRocksDB.saveBalanceLockBatch(balanceLocks));
        verify(mockRocksDBService).saveBatch(eq(balanceLocks), eq(mockColumnFamilyHandle), any(), eq("balance_locks"));
    }

    @Test
    @DisplayName("constructor should initialize with RocksDBService instance")
    void constructor_ShouldInitializeWithRocksDBServiceInstance() {
        // This test verifies that the private constructor works correctly
        // by checking that getInstance() returns a valid instance
        
        // Act
        BalanceLockRocksDB instance = BalanceLockRocksDB.getInstance();

        // Assert
        assertNotNull(instance);
    }

    @Test
    @DisplayName("saveBalanceLockBatch should work with single item")
    void saveBalanceLockBatch_ShouldWorkWithSingleItem() {
        // Arrange
        BalanceLock lock = BalanceLockFactory.create();
        Map<String, BalanceLock> singleItemMap = new HashMap<>();
        singleItemMap.put(lock.getLockId(), lock);

        // Act
        balanceLockRocksDB.saveBalanceLockBatch(singleItemMap);

        // Assert
        verify(mockRocksDBService).saveBatch(
            eq(singleItemMap),
            eq(mockColumnFamilyHandle),
            any(),
            eq("balance_locks")
        );
    }
} 