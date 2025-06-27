package com.exchangeengine.storage.rocksdb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.AmmOrderFactory;
import com.exchangeengine.model.AmmOrder;

/**
 * Unit test cho AmmOrderRocksDB class
 */
@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
public class AmmOrderRocksDBTest {

  @Mock
  private RocksDBService mockRocksDBService;

  @Mock
  private ColumnFamilyHandle mockAmmOrdersCF;

  private AmmOrderRocksDB ammOrderRocksDB;

  @BeforeEach
  void setUp() throws Exception {
    // Set up mock RocksDBService
    lenient().when(mockRocksDBService.getAmmOrdersCF()).thenReturn(mockAmmOrdersCF);

    // Set mockRocksDBService as the instance used by AmmOrderRocksDB
    try (MockedStatic<RocksDBService> mockedRocksDBService = Mockito.mockStatic(RocksDBService.class)) {
      mockedRocksDBService.when(RocksDBService::getInstance).thenReturn(mockRocksDBService);

      // Reset AmmOrderRocksDB instance
      AmmOrderRocksDB.resetInstance();

      // Get a fresh instance that will use our mock
      ammOrderRocksDB = AmmOrderRocksDB.getInstance();
    }
  }

  @Test
  @DisplayName("getInstance should always return the same instance")
  void getInstance_ReturnsSameInstance() {
    // Act
    AmmOrderRocksDB instance1 = AmmOrderRocksDB.getInstance();
    AmmOrderRocksDB instance2 = AmmOrderRocksDB.getInstance();

    // Assert
    assertSame(instance1, instance2, "getInstance() should always return the same instance");
  }

  @Test
  @DisplayName("saveAmmOrder should call RocksDBService.saveObject")
  void saveAmmOrder_CallsSaveObject() {
    // Arrange
    AmmOrder order = AmmOrderFactory.create();
    order.setStatus(AmmOrder.STATUS_PROCESSING);

    // Act
    ammOrderRocksDB.saveAmmOrder(order);

    // Assert
    verify(mockRocksDBService).saveObject(eq(order), eq(mockAmmOrdersCF), any(), eq("amm_order"));
  }

  @Test
  @DisplayName("saveAmmOrder passes null order to RocksDBService")
  void saveAmmOrder_PassesNullOrder() {
    // Act
    ammOrderRocksDB.saveAmmOrder(null);

    // Assert - checking that saveObject was called with null (implementation does
    // not filter null)
    verify(mockRocksDBService).saveObject(isNull(), eq(mockAmmOrdersCF), any(), eq("amm_order"));
  }

  @Test
  @DisplayName("saveAmmOrder propagates exceptions from RocksDBService")
  void saveAmmOrder_PropagatesException() {
    // Arrange
    AmmOrder order = AmmOrderFactory.create();
    order.setStatus(AmmOrder.STATUS_PROCESSING);

    doThrow(new RuntimeException("DB Error")).when(mockRocksDBService).saveObject(any(), any(), any(), any());

    // Act & Assert - should throw exception because implementation does not catch
    // it
    assertThrows(RuntimeException.class, () -> ammOrderRocksDB.saveAmmOrder(order));
  }

  @Test
  @DisplayName("getOrder should call RocksDBService.getObject")
  void getOrder_CallsGetObject() {
    // Arrange
    String identifier = "test_order_id";
    AmmOrder expectedOrder = AmmOrderFactory.createCustomAmmOrder(Map.of("identifier", identifier));
    expectedOrder.setStatus(AmmOrder.STATUS_PROCESSING);

    when(mockRocksDBService.getObject(eq(identifier), eq(mockAmmOrdersCF), eq(AmmOrder.class), eq("amm_order")))
        .thenReturn(Optional.of(expectedOrder));

    // Act
    Optional<AmmOrder> result = ammOrderRocksDB.getOrder(identifier);

    // Assert
    assertTrue(result.isPresent(), "getOrder should return non-empty Optional when order exists");
    assertEquals(expectedOrder, result.get(), "getOrder should return the correct order");
  }

  @Test
  @DisplayName("getOrder should return empty Optional when order doesn't exist")
  void getOrder_ReturnsEmptyWhenNotExists() {
    // Arrange
    String identifier = "nonexistent_id";
    when(mockRocksDBService.getObject(eq(identifier), eq(mockAmmOrdersCF), eq(AmmOrder.class), eq("amm_order")))
        .thenReturn(Optional.empty());

    // Act
    Optional<AmmOrder> result = ammOrderRocksDB.getOrder(identifier);

    // Assert
    assertFalse(result.isPresent(), "getOrder should return empty Optional when order doesn't exist");
  }

  @Test
  @DisplayName("getOrder should handle RuntimeException from RocksDBService")
  void getOrder_HandlesRuntimeException() {
    // Arrange
    String identifier = "test_id";
    when(mockRocksDBService.getObject(eq(identifier), eq(mockAmmOrdersCF), eq(AmmOrder.class), eq("amm_order")))
        .thenThrow(new RuntimeException("DB Error"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> ammOrderRocksDB.getOrder(identifier),
        "getOrder should propagate RuntimeException from RocksDBService");
  }

  @Test
  @DisplayName("getAllOrders should call RocksDBService.getAllObjects")
  void getAllOrders_CallsGetAllObjects() {
    // Arrange
    List<AmmOrder> expectedOrders = new ArrayList<>();
    AmmOrder order1 = AmmOrderFactory.create();
    AmmOrder order2 = AmmOrderFactory.create();
    order1.setStatus(AmmOrder.STATUS_PROCESSING);
    order2.setStatus(AmmOrder.STATUS_PROCESSING);

    expectedOrders.add(order1);
    expectedOrders.add(order2);

    when(mockRocksDBService.getAllObjects(eq(mockAmmOrdersCF), eq(AmmOrder.class), eq("amm_orders")))
        .thenReturn(expectedOrders);

    // Act
    List<AmmOrder> result = ammOrderRocksDB.getAllOrders();

    // Assert
    assertEquals(expectedOrders.size(), result.size(), "getAllOrders should return all orders");
    assertEquals(expectedOrders, result, "getAllOrders should return the correct orders");
  }

  @Test
  @DisplayName("getAllOrders should return empty list when no orders exist")
  void getAllOrders_ReturnsEmptyList() {
    // Arrange
    when(mockRocksDBService.getAllObjects(eq(mockAmmOrdersCF), eq(AmmOrder.class), eq("amm_orders")))
        .thenReturn(new ArrayList<>());

    // Act
    List<AmmOrder> result = ammOrderRocksDB.getAllOrders();

    // Assert
    assertTrue(result.isEmpty(), "getAllOrders should return empty list when no orders exist");
  }

  @Test
  @DisplayName("getAllOrders should handle exception from RocksDBService")
  void getAllOrders_HandlesException() {
    // Arrange
    when(mockRocksDBService.getAllObjects(eq(mockAmmOrdersCF), eq(AmmOrder.class), eq("amm_orders")))
        .thenThrow(new RuntimeException("DB Error"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> ammOrderRocksDB.getAllOrders(),
        "getAllOrders should propagate exception from RocksDBService");
  }

  @Test
  @DisplayName("saveAmmOrderBatch should call RocksDBService.saveBatch")
  void saveAmmOrderBatch_CallsSaveBatch() {
    // Arrange
    Map<String, AmmOrder> orders = new HashMap<>();
    AmmOrder order1 = AmmOrderFactory.create();
    AmmOrder order2 = AmmOrderFactory.create();
    order1.setStatus(AmmOrder.STATUS_PROCESSING);
    order2.setStatus(AmmOrder.STATUS_PROCESSING);

    orders.put("order1", order1);
    orders.put("order2", order2);

    // Act
    ammOrderRocksDB.saveAmmOrderBatch(orders);

    // Assert
    verify(mockRocksDBService).saveBatch(eq(orders), eq(mockAmmOrdersCF), any(), eq("amm_orders"));
  }

  @Test
  @DisplayName("saveAmmOrderBatch passes empty map to RocksDBService")
  void saveAmmOrderBatch_PassesEmptyMap() {
    // Arrange
    Map<String, AmmOrder> emptyOrders = new HashMap<>();

    // Act
    ammOrderRocksDB.saveAmmOrderBatch(emptyOrders);

    // Assert
    verify(mockRocksDBService).saveBatch(eq(emptyOrders), eq(mockAmmOrdersCF), any(), eq("amm_orders"));
  }

  @Test
  @DisplayName("saveAmmOrderBatch passes null map to RocksDBService")
  void saveAmmOrderBatch_PassesNull() {
    // Act
    ammOrderRocksDB.saveAmmOrderBatch(null);

    // Assert - implementation passes null through to RocksDBService
    verify(mockRocksDBService).saveBatch(isNull(), eq(mockAmmOrdersCF), any(), eq("amm_orders"));
  }

  @Test
  @DisplayName("saveAmmOrderBatch propagates exception from RocksDBService")
  void saveAmmOrderBatch_PropagatesException() {
    // Arrange
    Map<String, AmmOrder> orders = new HashMap<>();
    AmmOrder order = AmmOrderFactory.create();
    orders.put("order1", order);

    doThrow(new RuntimeException("DB Error")).when(mockRocksDBService).saveBatch(any(), any(), any(), any());

    // Act & Assert - implementation does not catch exceptions
    assertThrows(RuntimeException.class, () -> ammOrderRocksDB.saveAmmOrderBatch(orders));
  }
}
