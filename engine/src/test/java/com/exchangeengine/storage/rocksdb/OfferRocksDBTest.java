package com.exchangeengine.storage.rocksdb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.rocksdb.ColumnFamilyHandle;

import com.exchangeengine.extension.SingletonResetExtension;
import com.exchangeengine.factory.OfferFactory;
import com.exchangeengine.model.Offer;

/**
 * Unit test cho OfferRocksDB class
 * Đảm bảo coverage 100%
 */
@ExtendWith({MockitoExtension.class, SingletonResetExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
public class OfferRocksDBTest {

    @Mock
    private RocksDBService mockRocksDBService;
    
    @Mock
    private ColumnFamilyHandle mockOfferCF;
    
    private MockedStatic<RocksDBService> mockedStaticRocksDBService;
    
    private OfferRocksDB offerRocksDB;
    
    @BeforeEach
    void setup() {
        // Mock static method for RocksDBService
        mockedStaticRocksDBService = mockStatic(RocksDBService.class);
        mockedStaticRocksDBService.when(RocksDBService::getInstance).thenReturn(mockRocksDBService);
        
        // Setup column family handle mock
        when(mockRocksDBService.getOfferCF()).thenReturn(mockOfferCF);
        
        // Reset singleton
        OfferRocksDB.resetInstance();
        
        // Create test instance with mock
        offerRocksDB = OfferRocksDB.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        if (mockedStaticRocksDBService != null) {
            mockedStaticRocksDBService.close();
        }
    }
    
    @Test
    @DisplayName("getInstance nên trả về cùng một instance (singleton)")
    void getInstance_ShouldReturnSameInstance() {
        // Act: Lấy instance hai lần
        OfferRocksDB instance1 = OfferRocksDB.getInstance();
        OfferRocksDB instance2 = OfferRocksDB.getInstance();
        
        // Assert: Cả hai instance phải là cùng một object
        assertSame(instance1, instance2, "getInstance nên trả về cùng một instance");
        assertNotNull(instance1, "getInstance không được trả về null");
    }
    
    @Test
    @DisplayName("setTestInstance nên thiết lập instance cho testing")
    void setTestInstance_ShouldSetInstance() {
        // Arrange: Tạo một instance mock
        OfferRocksDB mockInstance = mock(OfferRocksDB.class);
        
        // Act: Thiết lập test instance
        OfferRocksDB.setTestInstance(mockInstance);
        
        // Assert: getInstance phải trả về instance đã thiết lập
        assertSame(mockInstance, OfferRocksDB.getInstance(), "setTestInstance nên thiết lập singleton instance");
    }
    
    @Test
    @DisplayName("resetInstance nên reset instance về null")
    void resetInstance_ShouldResetInstanceToNull() {
        // Arrange: Lấy một instance trước
        OfferRocksDB firstInstance = OfferRocksDB.getInstance();
        
        // Act: Reset instance
        OfferRocksDB.resetInstance();
        
        // Act tiếp: Lấy instance mới
        OfferRocksDB secondInstance = OfferRocksDB.getInstance();
        
        // Assert: Hai instance phải khác nhau
        assertNotSame(firstInstance, secondInstance, "resetInstance nên tạo instance mới khi gọi getInstance tiếp theo");
    }
    
    @Test
    @DisplayName("Constructor với RocksDBService đã được khởi tạo nên hoạt động đúng")
    void constructor_WithPreInitializedRocksDBService_ShouldWork() {
        // Arrange & Act: Tạo instance mới với RocksDBService đã mock
        OfferRocksDB customInstance = new OfferRocksDB(mockRocksDBService);
        
        // Assert: Instance không được null
        assertNotNull(customInstance, "Constructor với RocksDBService nên tạo instance hợp lệ");
    }
    
    @Test
    @DisplayName("getOffer nên trả về Optional.empty với identifier null")
    void getOffer_WithNullIdentifier_ShouldReturnEmptyOptional() {
        // Act: Gọi với identifier null
        Optional<Offer> result = offerRocksDB.getOffer(null);
        
        // Assert: Phải trả về Optional rỗng
        assertFalse(result.isPresent(), "getOffer nên trả về Optional.empty với identifier null");
        
        // Verify: Phương thức của RocksDBService không được gọi
        verify(mockRocksDBService, never()).getObject(anyString(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("getOffer nên trả về Optional.empty với identifier rỗng")
    void getOffer_WithEmptyIdentifier_ShouldReturnEmptyOptional() {
        // Act: Gọi với identifier rỗng
        Optional<Offer> result = offerRocksDB.getOffer("");
        
        // Assert: Phải trả về Optional rỗng
        assertFalse(result.isPresent(), "getOffer nên trả về Optional.empty với identifier rỗng");
        
        // Verify: Phương thức của RocksDBService không được gọi
        verify(mockRocksDBService, never()).getObject(anyString(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("getOffer nên gọi RocksDBService.getObject với identifier hợp lệ")
    void getOffer_WithValidIdentifier_ShouldCallRocksDBService() {
        // Arrange: Tạo data test và thiết lập mock
        String testId = "test-offer-1";
        Offer expectedOffer = OfferFactory.create();
        
        when(mockRocksDBService.getObject(eq(testId), eq(mockOfferCF), eq(Offer.class), eq("offer")))
            .thenReturn(Optional.of(expectedOffer));
        
        // Act: Gọi getOffer
        Optional<Offer> result = offerRocksDB.getOffer(testId);
        
        // Assert: Phải trả về Optional có chứa offer
        assertTrue(result.isPresent(), "getOffer nên trả về Optional có chứa offer");
        assertSame(expectedOffer, result.get(), "getOffer nên trả về đúng offer từ RocksDBService");
        
        // Verify: RocksDBService.getObject phải được gọi với tham số đúng
        verify(mockRocksDBService).getObject(eq(testId), eq(mockOfferCF), eq(Offer.class), eq("offer"));
    }
    
    @Test
    @DisplayName("getAllOffers nên gọi RocksDBService.getAllObjects")
    void getAllOffers_ShouldCallRocksDBService() {
        // Arrange: Tạo data test và thiết lập mock
        List<Offer> expectedOffers = new ArrayList<>();
        expectedOffers.add(OfferFactory.create());
        expectedOffers.add(OfferFactory.create());
        
        when(mockRocksDBService.getAllObjects(eq(mockOfferCF), eq(Offer.class), eq("offers")))
            .thenReturn(expectedOffers);
        
        // Act: Gọi getAllOffers
        List<Offer> result = offerRocksDB.getAllOffers();
        
        // Assert: Phải trả về danh sách offers từ RocksDBService
        assertSame(expectedOffers, result, "getAllOffers nên trả về danh sách offers từ RocksDBService");
        assertEquals(2, result.size(), "Số lượng offers trả về phải khớp");
        
        // Verify: RocksDBService.getAllObjects phải được gọi với tham số đúng
        verify(mockRocksDBService).getAllObjects(eq(mockOfferCF), eq(Offer.class), eq("offers"));
    }
    
    @Test
    @DisplayName("saveOffer nên không làm gì khi offer là null")
    void saveOffer_WithNullOffer_ShouldDoNothing() {
        // Act: Gọi saveOffer với null
        offerRocksDB.saveOffer(null);
        
        // Verify: RocksDBService.saveObject không được gọi
        verify(mockRocksDBService, never()).saveObject(any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("saveOffer nên gọi RocksDBService.saveObject với offer hợp lệ")
    void saveOffer_WithValidOffer_ShouldCallRocksDBService() {
        // Arrange: Tạo data test
        Offer testOffer = OfferFactory.create();
        
        // Act: Gọi saveOffer
        offerRocksDB.saveOffer(testOffer);
        
        // Verify: RocksDBService.saveObject phải được gọi với tham số đúng
        verify(mockRocksDBService).saveObject(
            eq(testOffer), 
            eq(mockOfferCF), 
            any(), // KeyExtractor
            eq("offer")
        );
    }
    
    @Test
    @DisplayName("saveOfferBatch nên không làm gì khi map offers là null")
    void saveOfferBatch_WithNullMap_ShouldDoNothing() {
        // Act: Gọi saveOfferBatch với null
        offerRocksDB.saveOfferBatch(null);
        
        // Verify: RocksDBService.saveBatch không được gọi
        verify(mockRocksDBService, never()).saveBatch(any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("saveOfferBatch nên không làm gì khi map offers rỗng")
    void saveOfferBatch_WithEmptyMap_ShouldDoNothing() {
        // Arrange: Tạo map rỗng
        Map<String, Offer> emptyOffers = new HashMap<>();
        
        // Act: Gọi saveOfferBatch với map rỗng
        offerRocksDB.saveOfferBatch(emptyOffers);
        
        // Verify: RocksDBService.saveBatch không được gọi
        verify(mockRocksDBService, never()).saveBatch(any(), any(), any(), anyString());
    }
    
    @Test
    @DisplayName("saveOfferBatch nên gọi RocksDBService.saveBatch với map offers hợp lệ")
    void saveOfferBatch_WithValidMap_ShouldCallRocksDBService() {
        // Arrange: Tạo data test
        Map<String, Offer> testOffers = new HashMap<>();
        testOffers.put("id1", OfferFactory.create());
        testOffers.put("id2", OfferFactory.create());
        testOffers.put("id3", OfferFactory.create());
        
        // Act: Gọi saveOfferBatch
        offerRocksDB.saveOfferBatch(testOffers);
        
        // Verify: RocksDBService.saveBatch phải được gọi với tham số đúng
        verify(mockRocksDBService).saveBatch(
            eq(testOffers), 
            eq(mockOfferCF), 
            any(), // KeyExtractor
            eq("offers")
        );
    }
}
