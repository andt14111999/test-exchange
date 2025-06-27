package com.exchangeengine.service.engine.amm_pool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.exchangeengine.extension.SingletonResetExtension;
import com.exchangeengine.factory.AmmPoolFactory;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.TickBitmap;
import com.exchangeengine.model.event.AmmPoolEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.TickBitmapCache;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
public class AmmPoolProcessorTest {

  @Mock
  private AmmPoolCache mockAmmPoolCache;

  @Mock
  private TickBitmapCache mockTickBitmapCache;

  @Mock
  private DisruptorEvent mockEvent;

  @Mock
  private AmmPoolEvent mockAmmPoolEvent;

  private AmmPoolProcessor processor;

  @BeforeEach
  void setup() {
    // Setup the mock DisruptorEvent to return our mock AmmPoolEvent
    when(mockEvent.getAmmPoolEvent()).thenReturn(mockAmmPoolEvent);
    // Setup mock để gọi event.successes()
    doNothing().when(mockEvent).successes();

    // Set mock AmmPoolCache instance
    AmmPoolCache.setTestInstance(mockAmmPoolCache);
    // Set mock TickBitmapCache instance
    TickBitmapCache.setTestInstance(mockTickBitmapCache);

    // Initialize the processor with the mock event
    processor = new AmmPoolProcessor(mockEvent);
  }

  @Test
  void process_ShouldCreateAmmPoolSuccessfully() {
    // Setup
    // Setup AmmPoolEvent for create operation
    when(mockAmmPoolEvent.getOperationType()).thenReturn(OperationType.AMM_POOL_CREATE);

    // Create an AmmPool that would be returned by toAmmPool
    AmmPool newPool = AmmPoolFactory.createDefaultAmmPool();
    String poolPair = newPool.getPair();

    // Mock the behavior of fetchAmmPool to return empty (no existing pool)
    when(mockAmmPoolEvent.fetchAmmPool(false)).thenReturn(Optional.empty());

    // Mock the behavior of toAmmPool to return our new pool
    when(mockAmmPoolEvent.toAmmPool(false)).thenReturn(newPool);

    // Mock the behavior of tickBitmapCache
    when(mockTickBitmapCache.getTickBitmap(poolPair)).thenReturn(Optional.empty());

    // Execute
    ProcessResult result = processor.process();

    // Verify
    verify(mockEvent).successes();
    assertTrue(result.getAmmPool().isPresent());
    assertEquals(newPool, result.getAmmPool().get());
    verify(mockAmmPoolCache).updateAmmPool(newPool);
    verify(mockTickBitmapCache).updateTickBitmap(any(TickBitmap.class));
  }

  @Test
  void process_ShouldNotCreateDuplicateTickBitmap() {
    // Setup
    // Setup AmmPoolEvent for create operation
    when(mockAmmPoolEvent.getOperationType()).thenReturn(OperationType.AMM_POOL_CREATE);

    // Create an AmmPool that would be returned by toAmmPool
    AmmPool newPool = AmmPoolFactory.createDefaultAmmPool();
    String poolPair = newPool.getPair();

    // Mock the behavior of fetchAmmPool to return empty (no existing pool)
    when(mockAmmPoolEvent.fetchAmmPool(false)).thenReturn(Optional.empty());

    // Mock the behavior of toAmmPool to return our new pool
    when(mockAmmPoolEvent.toAmmPool(false)).thenReturn(newPool);

    // Mock that bitmap already exists
    when(mockTickBitmapCache.getTickBitmap(poolPair)).thenReturn(Optional.of(new TickBitmap(poolPair)));

    // Execute
    ProcessResult result = processor.process();

    // Verify
    verify(mockEvent).successes();
    assertTrue(result.getAmmPool().isPresent());
    assertEquals(newPool, result.getAmmPool().get());
    verify(mockAmmPoolCache).updateAmmPool(newPool);
    // Verify that updateTickBitmap was not called because bitmap already exists
    verify(mockTickBitmapCache, never()).updateTickBitmap(any(TickBitmap.class));
  }

  @Test
  void process_ShouldHandleExistingAmmPoolOnCreate() {
    // Setup
    // Setup AmmPoolEvent for create operation
    when(mockAmmPoolEvent.getOperationType()).thenReturn(OperationType.AMM_POOL_CREATE);

    // Create an existing AmmPool
    AmmPool existingPool = AmmPoolFactory.createDefaultAmmPool();

    // Mock the behavior of fetchAmmPool to return an existing pool
    when(mockAmmPoolEvent.fetchAmmPool(false)).thenReturn(Optional.of(existingPool));

    // Execute
    ProcessResult result = processor.process();

    // Verify
    assertFalse(result.getEvent().isSuccess()); // Event không thành công vì throw exception
    verify(mockEvent, never()).successes(); // Không gọi successes vì throw exception
    assertTrue(result.getAmmPool().isPresent());
    assertEquals(existingPool, result.getAmmPool().get());
    // Should not update the pool since it already exists
    verify(mockAmmPoolCache, never()).updateAmmPool(any());
    // Should not create a new bitmap
    verify(mockTickBitmapCache, never()).updateTickBitmap(any());
  }

  @Test
  void process_ShouldUpdateAmmPoolSuccessfully() {
    // Setup
    // Setup AmmPoolEvent for update operation
    when(mockAmmPoolEvent.getOperationType()).thenReturn(OperationType.AMM_POOL_UPDATE);

    // Create a mock AmmPool for the update operation
    AmmPool existingPool = spy(AmmPoolFactory.createDefaultAmmPool());
    String poolPair = existingPool.getPair();

    // Mock the behavior of toAmmPool to return our existing pool
    when(mockAmmPoolEvent.toAmmPool(true)).thenReturn(existingPool);

    // Set up other required mocks for the update operation
    when(mockAmmPoolEvent.isActive()).thenReturn(true);
    when(mockAmmPoolEvent.getFeePercentage()).thenReturn(0.003);
    when(mockAmmPoolEvent.getFeeProtocolPercentage()).thenReturn(0.05);

    // Mock the update method to return true (changes made)
    when(existingPool.update(
        mockAmmPoolEvent.isActive(),
        mockAmmPoolEvent.getFeePercentage(),
        mockAmmPoolEvent.getFeeProtocolPercentage(),
        mockAmmPoolEvent.getInitPrice())).thenReturn(true);

    // Mock the behavior of tickBitmapCache
    when(mockTickBitmapCache.getTickBitmap(poolPair)).thenReturn(Optional.empty());

    // Execute
    ProcessResult result = processor.process();

    // Verify
    verify(mockEvent).successes();
    assertTrue(result.getAmmPool().isPresent());
    assertEquals(existingPool, result.getAmmPool().get());
    verify(mockAmmPoolCache).updateAmmPool(existingPool);
    verify(mockTickBitmapCache).updateTickBitmap(any(TickBitmap.class));
  }

  @Test
  void process_ShouldHandleNoChangesInUpdateAmmPool() {
    // Setup
    // Setup AmmPoolEvent for update operation
    when(mockAmmPoolEvent.getOperationType()).thenReturn(OperationType.AMM_POOL_UPDATE);

    // Create a mock AmmPool for the update operation
    AmmPool existingPool = spy(AmmPoolFactory.createDefaultAmmPool());

    // Mock the behavior of toAmmPool to return our existing pool
    when(mockAmmPoolEvent.toAmmPool(true)).thenReturn(existingPool);

    // Set up other required mocks for the update operation
    when(mockAmmPoolEvent.isActive()).thenReturn(true);
    when(mockAmmPoolEvent.getFeePercentage()).thenReturn(0.003);
    when(mockAmmPoolEvent.getFeeProtocolPercentage()).thenReturn(0.05);

    // Mock the update method to return false (no changes made)
    when(existingPool.update(
        mockAmmPoolEvent.isActive(),
        mockAmmPoolEvent.getFeePercentage(),
        mockAmmPoolEvent.getFeeProtocolPercentage(),
        null)).thenReturn(false);

    // Execute
    ProcessResult result = processor.process();

    // Verify - Luôn gọi successes vì chúng ta đã sửa code
    verify(mockEvent).successes();
    // AmmPool vẫn được lưu trong result
    assertTrue(result.getAmmPool().isPresent());
    assertEquals(existingPool, result.getAmmPool().get());
    // Should not update the pool since no changes were made
    verify(mockAmmPoolCache, never()).updateAmmPool(any());
    // Should not create a tickBitmap since no changes were made
    verify(mockTickBitmapCache, never()).updateTickBitmap(any());
  }

  @Test
  void process_ShouldHandleUnsupportedOperationType() {
    // Setup
    // Setup AmmPoolEvent with an unsupported operation type
    when(mockAmmPoolEvent.getOperationType()).thenReturn(OperationType.COIN_ACCOUNT_CREATE);

    // Execute
    ProcessResult result = processor.process();

    // Verify
    assertFalse(result.getEvent().isSuccess());
    verify(mockEvent).setErrorMessage(any());
    verify(mockEvent, never()).successes(); // Không gọi successes vì có exception
    // Should not create a tickBitmap
    verify(mockTickBitmapCache, never()).updateTickBitmap(any());
  }

  @Test
  void process_ShouldHandleExceptionDuringProcessing() {
    // Setup
    // Setup AmmPoolEvent for create operation
    when(mockAmmPoolEvent.getOperationType()).thenReturn(OperationType.AMM_POOL_CREATE);

    // Make fetchAmmPool throw an exception
    when(mockAmmPoolEvent.fetchAmmPool(false)).thenThrow(new RuntimeException("Test exception"));

    // Execute
    ProcessResult result = processor.process();

    // Verify
    assertFalse(result.getEvent().isSuccess());
    verify(mockEvent).setErrorMessage("Test exception");
    verify(mockEvent, never()).successes(); // Không gọi successes vì có exception
    // Should not create a tickBitmap
    verify(mockTickBitmapCache, never()).updateTickBitmap(any());
  }
}
