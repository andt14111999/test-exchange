package com.exchangeengine.storage.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.factory.OfferFactory;
import com.exchangeengine.model.Offer;
import com.exchangeengine.storage.rocksdb.OfferRocksDB;

@ExtendWith(MockitoExtension.class)
class OfferCacheTest {

    @Mock
    private OfferRocksDB mockOfferRocksDB;

    private OfferCache offerCache;
    private MockedStatic<OfferRocksDB> mockedOfferRocksDBStatic;

    // Test data
    private Offer testOffer;

    @BeforeEach
    void setUp() throws Exception {
        // Tạo dữ liệu test sử dụng OfferFactory
        testOffer = OfferFactory.create();

        // Cấu hình mock
        mockedOfferRocksDBStatic = Mockito.mockStatic(OfferRocksDB.class);
        mockedOfferRocksDBStatic.when(OfferRocksDB::getInstance).thenReturn(mockOfferRocksDB);

        // Reset OfferCache instance
        resetSingleton(OfferCache.class, "instance");

        // Khởi tạo OfferCache
        offerCache = OfferCache.getInstance();
        offerCache.updateOffer(testOffer);
        
        // Reset lại mock sau khi updateOffer đã gọi saveOffer
        reset(mockOfferRocksDB);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockedOfferRocksDBStatic != null) {
            mockedOfferRocksDBStatic.close();
        }
        resetSingleton(OfferCache.class, "instance");
    }

    private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
        java.lang.reflect.Field instance = clazz.getDeclaredField(fieldName);
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    @DisplayName("getInstance should return the same instance")
    void getInstance_ShouldReturnSameInstance() {
        OfferCache instance1 = OfferCache.getInstance();
        OfferCache instance2 = OfferCache.getInstance();

        assertSame(instance1, instance2, "getInstance should always return the same instance");
    }

    @Test
    @DisplayName("setTestInstance should set instance for testing")
    void setTestInstance_ShouldSetInstanceForTesting() {
        // Arrange
        OfferCache testInstance = mock(OfferCache.class);

        // Act
        OfferCache.setTestInstance(testInstance);
        OfferCache result = OfferCache.getInstance();

        // Assert
        assertSame(testInstance, result, "getInstance should return the test instance");
    }

    @Test
    @DisplayName("resetInstance should clear instance")
    void resetInstance_ShouldClearInstance() throws Exception {
        // Act
        OfferCache.resetInstance();
        
        // Verify instance is null
        java.lang.reflect.Field instance = OfferCache.class.getDeclaredField("instance");
        instance.setAccessible(true);
        assertNull(instance.get(null), "Instance should be null after reset");
    }

    @Test
    @DisplayName("getOffer should return empty Optional when offer not found")
    void getOffer_ShouldReturnEmptyOptional_WhenOfferNotFound() {
        // Arrange
        String offerId = "nonexistent_offer";

        // Act
        Optional<Offer> result = offerCache.getOffer(offerId);

        // Assert
        assertTrue(result.isEmpty(), "Should return empty Optional when offer not found");
    }

    @Test
    @DisplayName("getOffer should return offer when found")
    void getOffer_ShouldReturnOffer_WhenFound() {
        // Act
        Optional<Offer> result = offerCache.getOffer(testOffer.getIdentifier());

        // Assert
        assertTrue(result.isPresent(), "Should return Optional containing offer when found");
        assertEquals(testOffer.getIdentifier(), result.get().getIdentifier(), "Offer identifier should match");
    }

    @Test
    @DisplayName("getOrInitOffer should create new offer when not found")
    void getOrInitOffer_ShouldCreateNewOffer_WhenNotFound() {
        // Arrange
        String offerId = "new_offer_id";

        // Act
        Offer result = offerCache.getOrInitOffer(offerId);

        // Assert
        assertNotNull(result, "Should return a non-null offer");
        assertEquals(offerId, result.getIdentifier(), "Offer identifier should match");
    }

    @Test
    @DisplayName("getOrInitOffer should return existing offer when found")
    void getOrInitOffer_ShouldReturnExistingOffer_WhenFound() {
        // Act
        Offer result = offerCache.getOrInitOffer(testOffer.getIdentifier());

        // Assert
        assertNotNull(result, "Should return a non-null offer");
        assertEquals(testOffer.getIdentifier(), result.getIdentifier(), "Offer identifier should match");
        assertEquals(testOffer.getPrice(), result.getPrice(), "Offer price should match existing offer");
    }

    @Test
    @DisplayName("getOrCreateOffer should create and add new offer when not found")
    void getOrCreateOffer_ShouldCreateAndAddNewOffer_WhenNotFound() {
        // Arrange
        String offerId = "new_offer_id";

        // Act
        Offer result = offerCache.getOrCreateOffer(offerId);

        // Assert
        assertNotNull(result, "Should return a non-null offer");
        assertEquals(offerId, result.getIdentifier(), "Offer identifier should match");

        // Verify offer was added to cache
        Optional<Offer> cachedOffer = offerCache.getOffer(offerId);
        assertTrue(cachedOffer.isPresent(), "Offer should be present in cache");
        assertEquals(offerId, cachedOffer.get().getIdentifier(), "Cached offer identifier should match");
    }

    @Test
    @DisplayName("updateOffer should update offer in cache")
    void updateOffer_ShouldUpdateOfferInCache() {
        // Arrange
        Offer newOffer = OfferFactory.create();

        // Act
        offerCache.updateOffer(newOffer);

        // Assert
        Optional<Offer> result = offerCache.getOffer(newOffer.getIdentifier());
        assertTrue(result.isPresent(), "Offer should be present in cache after update");
        assertEquals(newOffer.getIdentifier(), result.get().getIdentifier(), "Offer identifier should match");
    }

    @Test
    @DisplayName("updateOffer should not update when offer is null")
    void updateOffer_ShouldNotUpdate_WhenOfferIsNull() {
        // Arrange
        reset(mockOfferRocksDB);  // Đảm bảo reset mock trước khi test
        int initialUpdateCount = 0;
        
        // Act
        offerCache.updateOffer(null);
        
        // Assert
        // Không kiểm tra gọi saveOffer vì có thể đã gọi từ trước
        java.lang.reflect.Field field;
        try {
            field = OfferCache.class.getDeclaredField("updateCounter");
            field.setAccessible(true);
            java.util.concurrent.atomic.AtomicInteger counter = 
                (java.util.concurrent.atomic.AtomicInteger) field.get(offerCache);
            int currentCount = counter.get() - 1; // trừ đi 1 vì đã tăng khi setup
            assertEquals(initialUpdateCount, currentCount, 
                "Update counter should not increase when offer is null");
        } catch (Exception e) {
            fail("Failed to access updateCounter field: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("addOfferToBatch should add offer to batch")
    void addOfferToBatch_ShouldAddOfferToBatch() throws Exception {
        // Arrange
        Offer offer = OfferFactory.create();

        // Clear và cấu hình lại mock
        java.lang.reflect.Field latestOffersField = OfferCache.class.getDeclaredField("latestOffers");
        latestOffersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Offer> latestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        latestOffers.clear();

        // Act
        offerCache.addOfferToBatch(offer);

        // Assert
        Map<String, Offer> updatedLatestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        assertTrue(updatedLatestOffers.containsKey(offer.getIdentifier()), "Offer should be added to batch");
    }

    @Test
    @DisplayName("addOfferToBatch should not add null offer")
    void addOfferToBatch_ShouldNotAdd_WhenOfferIsNull() {
        // Act
        offerCache.addOfferToBatch(null);

        // Assert - verify that saveOfferBatch was not called
        verify(mockOfferRocksDB, never()).saveOfferBatch(any());
    }

    @Test
    @DisplayName("addOfferToBatch should not add offer with null identifier")
    void addOfferToBatch_ShouldNotAdd_WhenOfferIdentifierIsNull() {
        // Arrange
        Offer offer = OfferFactory.create();
        offer.setIdentifier(null);

        // Act
        offerCache.addOfferToBatch(offer);

        // Assert - verify that saveOfferBatch was not called
        verify(mockOfferRocksDB, never()).saveOfferBatch(any());
    }

    @Test
    @DisplayName("addOfferToBatch should update offer when new offer has newer timestamp")
    void addOfferToBatch_ShouldUpdateOffer_WhenNewOfferHasNewerTimestamp() throws Exception {
        // Arrange
        Offer olderOffer = OfferFactory.create();
        olderOffer.setUpdatedAt(Instant.now().minusSeconds(100));
        
        Offer newerOffer = OfferFactory.create();
        newerOffer.setIdentifier(olderOffer.getIdentifier()); // Same ID
        newerOffer.setUpdatedAt(Instant.now()); // But newer timestamp
        
        // Clear map and add older offer
        java.lang.reflect.Field latestOffersField = OfferCache.class.getDeclaredField("latestOffers");
        latestOffersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Offer> latestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        latestOffers.clear();
        latestOffers.put(olderOffer.getIdentifier(), olderOffer);
        
        // Act
        offerCache.addOfferToBatch(newerOffer);
        
        // Assert
        Map<String, Offer> updatedLatestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        assertEquals(newerOffer, updatedLatestOffers.get(newerOffer.getIdentifier()), 
                "Newer offer should replace older offer in batch");
    }

    @Test
    @DisplayName("addOfferToBatch should keep existing offer when new offer has older timestamp")
    void addOfferToBatch_ShouldKeepExistingOffer_WhenNewOfferHasOlderTimestamp() throws Exception {
        // Arrange
        Offer newerOffer = OfferFactory.create();
        newerOffer.setUpdatedAt(Instant.now());
        
        Offer olderOffer = OfferFactory.create();
        olderOffer.setIdentifier(newerOffer.getIdentifier()); // Same ID
        olderOffer.setUpdatedAt(Instant.now().minusSeconds(100)); // But older timestamp
        
        // Clear map and add newer offer
        java.lang.reflect.Field latestOffersField = OfferCache.class.getDeclaredField("latestOffers");
        latestOffersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Offer> latestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        latestOffers.clear();
        latestOffers.put(newerOffer.getIdentifier(), newerOffer);
        
        // Act
        offerCache.addOfferToBatch(olderOffer);
        
        // Assert
        Map<String, Offer> updatedLatestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        assertEquals(newerOffer, updatedLatestOffers.get(newerOffer.getIdentifier()), 
                "Newer offer should be kept when older offer is added");
    }

    @Test
    @DisplayName("addOfferToBatch should flush when batch size reaches threshold")
    void addOfferToBatch_ShouldFlush_WhenBatchSizeReachesThreshold() throws Exception {
        // Arrange
        java.lang.reflect.Field thresholdField = OfferCache.class.getDeclaredField("UPDATE_THRESHOLD");
        thresholdField.setAccessible(true);
        int threshold = (int) thresholdField.get(null);
        
        // Clear latestOffers map
        java.lang.reflect.Field latestOffersField = OfferCache.class.getDeclaredField("latestOffers");
        latestOffersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Offer> latestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        latestOffers.clear();
        
        // Add offers until just below threshold
        for (int i = 0; i < threshold - 1; i++) {
            Offer offer = OfferFactory.create();
            offerCache.addOfferToBatch(offer);
        }
        
        // Verify saveOfferBatch not called yet
        verify(mockOfferRocksDB, never()).saveOfferBatch(any());
        
        // Add one more offer to reach threshold
        Offer finalOffer = OfferFactory.create();
        offerCache.addOfferToBatch(finalOffer);
        
        // Verify saveOfferBatch called
        verify(mockOfferRocksDB, times(1)).saveOfferBatch(any());
    }

    @Test
    @DisplayName("offerCacheShouldFlush should return false when update count is zero")
    void offerCacheShouldFlush_ShouldReturnFalse_WhenUpdateCountIsZero() throws Exception {
        // Arrange
        java.lang.reflect.Field counterField = OfferCache.class.getDeclaredField("updateCounter");
        counterField.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger counter = (java.util.concurrent.atomic.AtomicInteger) counterField.get(offerCache);
        counter.set(0);
        
        // Act
        boolean result = offerCache.offerCacheShouldFlush();
        
        // Assert
        assertFalse(result, "Should return false when update count is zero");
    }

    @Test
    @DisplayName("offerCacheShouldFlush should return true when update count is divisible by threshold")
    void offerCacheShouldFlush_ShouldReturnTrue_WhenUpdateCountIsDivisibleByThreshold() throws Exception {
        // Arrange
        java.lang.reflect.Field thresholdField = OfferCache.class.getDeclaredField("UPDATE_THRESHOLD");
        thresholdField.setAccessible(true);
        int threshold = (int) thresholdField.get(null);
        
        java.lang.reflect.Field counterField = OfferCache.class.getDeclaredField("updateCounter");
        counterField.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger counter = (java.util.concurrent.atomic.AtomicInteger) counterField.get(offerCache);
        counter.set(threshold);
        
        // Act
        boolean result = offerCache.offerCacheShouldFlush();
        
        // Assert
        assertTrue(result, "Should return true when update count is divisible by threshold");
    }

    @Test
    @DisplayName("flushOfferToDisk should not call saveOfferBatch when no offers to flush")
    void flushOfferToDisk_ShouldNotCallSaveOfferBatch_WhenNoOffersToFlush() throws Exception {
        // Arrange
        java.lang.reflect.Field latestOffersField = OfferCache.class.getDeclaredField("latestOffers");
        latestOffersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Offer> latestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        latestOffers.clear();
        
        // Act
        offerCache.flushOfferToDisk();
        
        // Assert
        verify(mockOfferRocksDB, never()).saveOfferBatch(any());
    }

    @Test
    @DisplayName("flushOfferToDisk should call saveOfferBatch when offers exist")
    void flushOfferToDisk_ShouldCallSaveOfferBatch_WhenOffersExist() throws Exception {
        // Arrange
        java.lang.reflect.Field latestOffersField = OfferCache.class.getDeclaredField("latestOffers");
        latestOffersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Offer> latestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        latestOffers.clear();
        
        // Add offer to batch
        Offer offer = OfferFactory.create();
        latestOffers.put(offer.getIdentifier(), offer);
        
        // Act
        offerCache.flushOfferToDisk();
        
        // Assert
        verify(mockOfferRocksDB, times(1)).saveOfferBatch(any());
        assertTrue(latestOffers.isEmpty(), "latestOffers should be cleared after flush");
    }

    @Test
    @DisplayName("removeOffer should remove offer from cache")
    void removeOffer_ShouldRemoveOfferFromCache() {
        // Arrange
        String identifier = testOffer.getIdentifier();
        assertTrue(offerCache.getOffer(identifier).isPresent(), "Offer should be present before removal");
        
        // Act
        offerCache.removeOffer(identifier);
        
        // Assert
        assertFalse(offerCache.getOffer(identifier).isPresent(), "Offer should not be present after removal");
    }

    @Test
    @DisplayName("clearAll should clear all offers from cache")
    void clearAll_ShouldClearAllOffersFromCache() throws Exception {
        // Arrange
        // Ensure there's data in the cache
        assertTrue(offerCache.size() > 0, "Cache should have at least one offer before clearing");
        
        // Add offer to latestOffers
        java.lang.reflect.Field latestOffersField = OfferCache.class.getDeclaredField("latestOffers");
        latestOffersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Offer> latestOffers = (Map<String, Offer>) latestOffersField.get(offerCache);
        Offer offer = OfferFactory.create();
        latestOffers.put(offer.getIdentifier(), offer);
        
        // Act
        offerCache.clearAll();
        
        // Assert
        assertEquals(0, offerCache.size(), "Cache should be empty after clearing");
        assertTrue(latestOffers.isEmpty(), "latestOffers should be empty after clearing");
    }

    @Test
    @DisplayName("size should return number of offers in cache")
    void size_ShouldReturnNumberOfOffersInCache() throws Exception {
        // Arrange
        java.lang.reflect.Field offerCacheField = OfferCache.class.getDeclaredField("offerCache");
        offerCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Offer> offerCacheMap = (ConcurrentHashMap<String, Offer>) offerCacheField.get(offerCache);
        offerCacheMap.clear();
        
        // Add some offers
        for (int i = 0; i < 5; i++) {
            Offer offer = OfferFactory.create();
            offerCacheMap.put(offer.getIdentifier(), offer);
        }
        
        // Act
        long size = offerCache.size();
        
        // Assert
        assertEquals(5, size, "Size should match number of offers in cache");
    }

    @Test
    @DisplayName("initializeOfferCache should update cache with offers from DB")
    void initializeOfferCache_ShouldUpdateCacheWithOffersFromDB() {
        // Arrange
        List<Offer> mockedOffers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Offer offer = OfferFactory.create();
            // Đảm bảo offer có updatedAt mới hơn
            offer.setUpdatedAt(Instant.now());
            mockedOffers.add(offer);
        }
        
        when(mockOfferRocksDB.getAllOffers()).thenReturn(mockedOffers);
        
        // Reset cache
        offerCache.clearAll();
        
        // Act
        offerCache.initializeOfferCache();
        
        // Assert
        assertEquals(5, offerCache.size(), "Cache should contain all offers from DB");
        
        // Đảm bảo tất cả các offers từ mock đều có trong cache
        for (Offer offer : mockedOffers) {
            Optional<Offer> cachedOffer = offerCache.getOffer(offer.getIdentifier());
            assertTrue(cachedOffer.isPresent(), "Offer should be present in cache");
            assertEquals(offer.getIdentifier(), cachedOffer.get().getIdentifier(), 
                    "Cached offer identifier should match");
        }
    }

    @Test
    @DisplayName("initializeOfferCache should handle exception when RocksDB fails")
    void initializeOfferCache_ShouldHandleException_WhenRocksDBFails() {
        // Arrange
        when(mockOfferRocksDB.getAllOffers()).thenThrow(new RuntimeException("DB error"));
        
        // Reset cache
        offerCache.clearAll();
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> offerCache.initializeOfferCache());
    }

    @Test
    @DisplayName("initializeOfferCache should skip offers with null identifiers")
    void initializeOfferCache_ShouldSkipOffers_WithNullIdentifiers() {
        // Arrange
        List<Offer> mockedOffers = new ArrayList<>();
        Offer validOffer = OfferFactory.create();
        
        Offer invalidOffer = OfferFactory.create();
        invalidOffer.setIdentifier(null);
        
        mockedOffers.add(validOffer);
        mockedOffers.add(invalidOffer);
        
        when(mockOfferRocksDB.getAllOffers()).thenReturn(mockedOffers);
        
        // Reset cache
        offerCache.clearAll();
        
        // Act
        offerCache.initializeOfferCache();
        
        // Assert
        assertEquals(1, offerCache.size(), "Cache should only contain valid offers");
        assertTrue(offerCache.getOffer(validOffer.getIdentifier()).isPresent(), "Valid offer should be in cache");
    }

    @Test
    @DisplayName("initializeOfferCache should skip offers with empty identifiers")
    void initializeOfferCache_ShouldSkipOffers_WithEmptyIdentifiers() {
        // Arrange
        List<Offer> mockedOffers = new ArrayList<>();
        Offer validOffer = OfferFactory.create();
        
        Offer invalidOffer = OfferFactory.create();
        invalidOffer.setIdentifier("");
        
        mockedOffers.add(validOffer);
        mockedOffers.add(invalidOffer);
        
        when(mockOfferRocksDB.getAllOffers()).thenReturn(mockedOffers);
        
        // Reset cache
        offerCache.clearAll();
        
        // Act
        offerCache.initializeOfferCache();
        
        // Assert
        assertEquals(1, offerCache.size(), "Cache should only contain valid offers");
        assertTrue(offerCache.getOffer(validOffer.getIdentifier()).isPresent(), "Valid offer should be in cache");
    }

    @Test
    @DisplayName("initializeOfferCache should skip offers that are newer in cache")
    void initializeOfferCache_ShouldSkipOffers_WhenCacheIsNewer() {
        // Arrange
        Offer newerOffer = OfferFactory.create();
        newerOffer.setUpdatedAt(Instant.now());
        
        Offer olderOffer = OfferFactory.create();
        olderOffer.setIdentifier(newerOffer.getIdentifier()); // Same ID
        olderOffer.setUpdatedAt(Instant.now().minusSeconds(100)); // But older timestamp
        
        List<Offer> mockedOffers = new ArrayList<>();
        mockedOffers.add(olderOffer);
        
        when(mockOfferRocksDB.getAllOffers()).thenReturn(mockedOffers);
        
        // Reset and initialize cache with newer offer
        offerCache.clearAll();
        offerCache.updateOffer(newerOffer);
        reset(mockOfferRocksDB); // Reset mock to clear saveOffer call
        
        // Act
        offerCache.initializeOfferCache();
        
        // Assert
        Optional<Offer> result = offerCache.getOffer(newerOffer.getIdentifier());
        assertTrue(result.isPresent(), "Offer should be present in cache");
        assertEquals(newerOffer.getUpdatedAt(), result.get().getUpdatedAt(), 
                "Offer should not be updated with older version from DB");
    }
}
