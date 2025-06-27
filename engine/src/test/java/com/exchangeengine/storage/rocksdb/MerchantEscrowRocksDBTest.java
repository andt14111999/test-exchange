package com.exchangeengine.storage.rocksdb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.rocksdb.ColumnFamilyHandle;

import com.exchangeengine.extension.SingletonResetExtension;
import com.exchangeengine.model.MerchantEscrow;
import com.exchangeengine.model.OperationType;

import java.math.BigDecimal;

@ExtendWith({MockitoExtension.class, SingletonResetExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
public class MerchantEscrowRocksDBTest {

    @Mock
    private RocksDBService mockRocksDBService;
    
    @Mock
    private ColumnFamilyHandle mockColumnFamilyHandle;
    
    private MockedStatic<RocksDBService> mockedStaticRocksDBService;
    
    private MerchantEscrowRocksDB escrowRocksDB;
    
    @BeforeEach
    void setup() {
        // Mock static method for RocksDBService
        mockedStaticRocksDBService = mockStatic(RocksDBService.class);
        mockedStaticRocksDBService.when(RocksDBService::getInstance).thenReturn(mockRocksDBService);
        
        // Setup column family handle mock
        when(mockRocksDBService.getMerchantEscrowCF()).thenReturn(mockColumnFamilyHandle);
        
        // Reset singleton
        MerchantEscrowRocksDB.resetInstance();
        
        // Create test instance with mock
        escrowRocksDB = new MerchantEscrowRocksDB(mockRocksDBService);
    }
    
    @AfterEach
    void tearDown() {
        if (mockedStaticRocksDBService != null) {
            mockedStaticRocksDBService.close();
        }
    }
    
    @Test
    @DisplayName("getInstance should return singleton instance")
    void getInstance_ShouldReturnSingletonInstance() {
        // Act: Get instance twice
        MerchantEscrowRocksDB instance1 = MerchantEscrowRocksDB.getInstance();
        MerchantEscrowRocksDB instance2 = MerchantEscrowRocksDB.getInstance();
        
        // Assert: Both instances should be the same
        assertSame(instance1, instance2, "getInstance should return the same instance");
        assertNotNull(instance1, "getInstance should not return null");
    }

    @Test
    @DisplayName("setTestInstance should set the instance")
    void setTestInstance_ShouldSetInstance() {
        // Arrange: Create a mocked instance
        MerchantEscrowRocksDB mockInstance = mock(MerchantEscrowRocksDB.class);
        
        // Act: Set the test instance
        MerchantEscrowRocksDB.setTestInstance(mockInstance);
        
        // Assert: getInstance should return our mocked instance
        assertSame(mockInstance, MerchantEscrowRocksDB.getInstance(), "setTestInstance should set the singleton instance");
    }
    
    @Test
    @DisplayName("resetInstance should reset the instance to null")
    void resetInstance_ShouldResetInstanceToNull() {
        // Arrange: Get an instance first
        MerchantEscrowRocksDB firstInstance = MerchantEscrowRocksDB.getInstance();
        
        // Act: Reset the instance
        MerchantEscrowRocksDB.resetInstance();
        
        // Act again: Get a new instance
        MerchantEscrowRocksDB secondInstance = MerchantEscrowRocksDB.getInstance();
        
        // Assert: Instances should be different
        assertNotSame(firstInstance, secondInstance, "resetInstance should create a new instance on next getInstance call");
    }
    
    @Test
    @DisplayName("getMerchantEscrow should return empty Optional for null identifier")
    void getMerchantEscrow_ShouldReturnEmptyOptionalForNullIdentifier() {
        // Act: Call with null
        Optional<MerchantEscrow> result = escrowRocksDB.getMerchantEscrow(null);
        
        // Assert: Should return empty Optional
        assertFalse(result.isPresent(), "getMerchantEscrow should return empty Optional for null identifier");
        
        // Verify: RocksDBService methods should not be called
        verify(mockRocksDBService, never()).getObject(anyString(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("getMerchantEscrow should return empty Optional for empty identifier")
    void getMerchantEscrow_ShouldReturnEmptyOptionalForEmptyIdentifier() {
        // Act: Call with empty string
        Optional<MerchantEscrow> result = escrowRocksDB.getMerchantEscrow("");
        
        // Assert: Should return empty Optional
        assertFalse(result.isPresent(), "getMerchantEscrow should return empty Optional for empty identifier");
        
        // Verify: RocksDBService methods should not be called
        verify(mockRocksDBService, never()).getObject(anyString(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("getMerchantEscrow should return MerchantEscrow for valid identifier")
    void getMerchantEscrow_ShouldReturnMerchantEscrowForValidIdentifier() {
        // Arrange: Create test data
        String identifier = "test-id";
        MerchantEscrow escrow = createTestEscrow(identifier);
        
        // Mock RocksDBService behavior
        when(mockRocksDBService.getObject(eq(identifier), eq(mockColumnFamilyHandle), eq(MerchantEscrow.class), anyString()))
            .thenReturn(Optional.of(escrow));
        
        // Act: Call with valid ID
        Optional<MerchantEscrow> result = escrowRocksDB.getMerchantEscrow(identifier);
        
        // Assert: Should return MerchantEscrow
        assertTrue(result.isPresent(), "getMerchantEscrow should return Optional with MerchantEscrow for valid identifier");
        assertEquals(identifier, result.get().getIdentifier(), "Returned MerchantEscrow should have correct identifier");
        
        // Verify: RocksDBService methods should be called with correct parameters
        verify(mockRocksDBService).getObject(eq(identifier), eq(mockColumnFamilyHandle), eq(MerchantEscrow.class), anyString());
    }
    
    @Test
    @DisplayName("getAllMerchantEscrows should return all escrows from RocksDB")
    void getAllMerchantEscrows_ShouldReturnAllEscrowsFromRocksDB() {
        // Arrange: Create test data
        List<MerchantEscrow> escrows = new ArrayList<>();
        escrows.add(createTestEscrow("id1"));
        escrows.add(createTestEscrow("id2"));
        escrows.add(createTestEscrow("id3"));
        
        // Mock RocksDBService behavior
        when(mockRocksDBService.getAllObjects(eq(mockColumnFamilyHandle), eq(MerchantEscrow.class), anyString()))
            .thenReturn(escrows);
        
        // Act: Call getAllMerchantEscrows
        List<MerchantEscrow> result = escrowRocksDB.getAllMerchantEscrows();
        
        // Assert: Should return all escrows
        assertNotNull(result, "getAllMerchantEscrows should not return null");
        assertEquals(3, result.size(), "getAllMerchantEscrows should return the correct number of escrows");
        assertEquals("id1", result.get(0).getIdentifier(), "First escrow should have correct ID");
        assertEquals("id2", result.get(1).getIdentifier(), "Second escrow should have correct ID");
        assertEquals("id3", result.get(2).getIdentifier(), "Third escrow should have correct ID");
        
        // Verify: RocksDBService methods should be called with correct parameters
        verify(mockRocksDBService).getAllObjects(eq(mockColumnFamilyHandle), eq(MerchantEscrow.class), anyString());
    }
    
    @Test
    @DisplayName("saveMerchantEscrow should save escrow to RocksDB")
    void saveMerchantEscrow_ShouldSaveEscrowToRocksDB() {
        // Arrange: Create test data
        MerchantEscrow escrow = createTestEscrow("test-id");
        
        // Act: Call saveMerchantEscrow
        escrowRocksDB.saveMerchantEscrow(escrow);
        
        // Verify: RocksDBService methods should be called with correct parameters
        verify(mockRocksDBService).saveObject(
            eq(escrow), 
            eq(mockColumnFamilyHandle), 
            any(), // KeyExtractor
            anyString()
        );
    }
    
    @Test
    @DisplayName("saveMerchantEscrow should not call RocksDBService for null escrow")
    void saveMerchantEscrow_ShouldNotCallRocksDBServiceForNullEscrow() {
        // Act: Call saveMerchantEscrow with null
        escrowRocksDB.saveMerchantEscrow(null);
        
        // Verify: RocksDBService methods should not be called
        verify(mockRocksDBService, never()).saveObject(any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("saveMerchantEscrowBatch should save batch of escrows to RocksDB")
    void saveMerchantEscrowBatch_ShouldSaveBatchOfEscrowsToRocksDB() {
        // Arrange: Create test data
        Map<String, MerchantEscrow> escrows = new HashMap<>();
        escrows.put("id1", createTestEscrow("id1"));
        escrows.put("id2", createTestEscrow("id2"));
        escrows.put("id3", createTestEscrow("id3"));
        
        // Act: Call saveMerchantEscrowBatch
        escrowRocksDB.saveMerchantEscrowBatch(escrows);
        
        // Verify: RocksDBService methods should be called with correct parameters
        verify(mockRocksDBService).saveBatch(
            eq(escrows), 
            eq(mockColumnFamilyHandle), 
            any(), // KeyExtractor
            anyString()
        );
    }
    
    @Test
    @DisplayName("saveMerchantEscrowBatch should not call RocksDBService for null map")
    void saveMerchantEscrowBatch_ShouldNotCallRocksDBServiceForNullMap() {
        // Act: Call saveMerchantEscrowBatch with null
        escrowRocksDB.saveMerchantEscrowBatch(null);
        
        // Verify: RocksDBService methods should not be called
        verify(mockRocksDBService, never()).saveBatch(any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("saveMerchantEscrowBatch should not call RocksDBService for empty map")
    void saveMerchantEscrowBatch_ShouldNotCallRocksDBServiceForEmptyMap() {
        // Arrange: Create empty map
        Map<String, MerchantEscrow> escrows = new HashMap<>();
        
        // Act: Call saveMerchantEscrowBatch with empty map
        escrowRocksDB.saveMerchantEscrowBatch(escrows);
        
        // Verify: RocksDBService methods should not be called
        verify(mockRocksDBService, never()).saveBatch(any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("Constructor with RocksDBService parameter should use the provided service")
    void constructor_ShouldUseProvidedRocksDBService() {
        // Arrange: Create a new mock for RocksDBService
        RocksDBService anotherMockService = mock(RocksDBService.class);
        when(anotherMockService.getMerchantEscrowCF()).thenReturn(mockColumnFamilyHandle);
        
        // Act: Create a new instance with the mock
        MerchantEscrowRocksDB instance = new MerchantEscrowRocksDB(anotherMockService);
        
        // Arrange: Prepare for method call
        MerchantEscrow escrow = createTestEscrow("test-id");
        
        // Act: Call method that uses the RocksDBService
        instance.saveMerchantEscrow(escrow);
        
        // Verify: The provided RocksDBService should be used
        verify(anotherMockService).saveObject(any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("Default constructor should use RocksDBService singleton")
    void defaultConstructor_ShouldUseRocksDBServiceSingleton() {
        // Arrange: Reset the instance to use the default constructor
        MerchantEscrowRocksDB.resetInstance();
        
        // Act: Get a new instance using getInstance()
        MerchantEscrowRocksDB instance = MerchantEscrowRocksDB.getInstance();
        
        // Assert: The instance should not be null
        assertNotNull(instance, "Default constructor should create a valid instance");
        
        // Verify: RocksDBService.getInstance should be called
        mockedStaticRocksDBService.verify(RocksDBService::getInstance);
    }
    
    /**
     * Helper method to create a test MerchantEscrow
     * 
     * @param identifier Identifier for the escrow
     * @return A test MerchantEscrow
     */
    private MerchantEscrow createTestEscrow(String identifier) {
        return new MerchantEscrow(
            identifier,
            "usdt-account-" + identifier,
            "fiat-account-" + identifier,
            OperationType.MERCHANT_ESCROW_MINT,
            new BigDecimal("100.00"),
            new BigDecimal("3000.00"),
            "USD",
            "user-" + identifier,
            "merchant-escrow-op-" + identifier
        );
    }
} 