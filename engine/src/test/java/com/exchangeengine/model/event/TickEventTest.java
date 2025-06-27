package com.exchangeengine.model.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.Tick;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.TickBitmapCache;
import com.exchangeengine.storage.cache.TickCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TickEventTest {

    private TickEvent tickEvent;
    private AmmPoolCache mockAmmPoolCache;
    private TickCache mockTickCache;
    private TickBitmapCache mockTickBitmapCache;
    private AmmPool mockAmmPool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        tickEvent = new TickEvent();
        mockAmmPoolCache = mock(AmmPoolCache.class);
        mockTickCache = mock(TickCache.class);
        mockTickBitmapCache = mock(TickBitmapCache.class);
        mockAmmPool = mock(AmmPool.class);
        objectMapper = new ObjectMapper();

        // Setup default values
        tickEvent.setEventId("test-event-id");
        tickEvent.setActionId("test-action-id");
        tickEvent.setActionType(ActionType.COIN_ACCOUNT);
        tickEvent.setOperationType(OperationType.TICK_QUERY);
        tickEvent.setPoolPair("BTC-USDT");
    }

    @Test
    @DisplayName("getProducerEventId should return poolPair")
    void getProducerEventId_ShouldReturnPoolPair() {
        assertEquals("BTC-USDT", tickEvent.getProducerEventId());
    }

    @Test
    @DisplayName("parserData should parse JSON data correctly")
    void parserData_ShouldParseJsonDataCorrectly() throws Exception {
        // Arrange
        String jsonStr = "{\"eventId\":\"event-123\",\"actionType\":\"CoinAccount\",\"actionId\":\"action-123\",\"operationType\":\"TICK_QUERY\",\"poolPair\":\"ETH-USDT\"}";
        JsonNode jsonNode = objectMapper.readTree(jsonStr);

        // Act
        TickEvent result = tickEvent.parserData(jsonNode);

        // Assert
        assertEquals("event-123", result.getEventId());
        assertEquals(ActionType.COIN_ACCOUNT, result.getActionType());
        assertEquals("action-123", result.getActionId());
        assertEquals(OperationType.TICK_QUERY, result.getOperationType());
        assertEquals("ETH-USDT", result.getPoolPair());
    }

    @Test
    @DisplayName("validate should throw exception when poolPair is null")
    void validate_ShouldThrowException_WhenPoolPairIsNull() {
        // Arrange
        tickEvent.setPoolPair(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> tickEvent.validate());
        assertTrue(exception.getMessage().contains("Pool pair is required"));
    }

    @Test
    @DisplayName("validate should throw exception when pool does not exist")
    void validate_ShouldThrowException_WhenPoolDoesNotExist() {
        // Arrange
        try (MockedStatic<AmmPoolCache> mockedStatic = Mockito.mockStatic(AmmPoolCache.class)) {
            mockedStatic.when(AmmPoolCache::getInstance).thenReturn(mockAmmPoolCache);
            when(mockAmmPoolCache.getAmmPool("BTC-USDT")).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> tickEvent.validate());
            assertTrue(exception.getMessage().contains("AMM Pool does not exist"));
        }
    }

    @Test
    @DisplayName("validate should throw exception when pool is not active")
    void validate_ShouldThrowException_WhenPoolIsNotActive() {
        // Arrange
        try (MockedStatic<AmmPoolCache> mockedStatic = Mockito.mockStatic(AmmPoolCache.class)) {
            mockedStatic.when(AmmPoolCache::getInstance).thenReturn(mockAmmPoolCache);
            when(mockAmmPoolCache.getAmmPool("BTC-USDT")).thenReturn(Optional.of(mockAmmPool));
            when(mockAmmPool.isActive()).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> tickEvent.validate());
            assertTrue(exception.getMessage().contains("AMM Pool is not active"));
        }
    }

    @Test
    @DisplayName("validate should pass when all conditions are met")
    void validate_ShouldPass_WhenAllConditionsAreMet() {
        // Arrange
        try (MockedStatic<AmmPoolCache> mockedStatic = Mockito.mockStatic(AmmPoolCache.class)) {
            mockedStatic.when(AmmPoolCache::getInstance).thenReturn(mockAmmPoolCache);
            when(mockAmmPoolCache.getAmmPool("BTC-USDT")).thenReturn(Optional.of(mockAmmPool));
            when(mockAmmPool.isActive()).thenReturn(true);

            // Act & Assert
            assertDoesNotThrow(() -> tickEvent.validate());
        }
    }

    @Test
    @DisplayName("fetchTicksFromBitmap should return empty list when poolPair is null")
    void fetchTicksFromBitmap_ShouldReturnEmptyList_WhenPoolPairIsNull() {
        // Arrange
        tickEvent.setPoolPair(null);

        // Act
        List<Tick> result = tickEvent.fetchTicksFromBitmap();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("fetchTicksFromBitmap should return empty list when bitmap is not found")
    void fetchTicksFromBitmap_ShouldReturnEmptyList_WhenBitmapIsNotFound() {
        // Arrange
        try (MockedStatic<TickBitmapCache> mockedStatic = Mockito.mockStatic(TickBitmapCache.class)) {
            mockedStatic.when(TickBitmapCache::getInstance).thenReturn(mockTickBitmapCache);
            when(mockTickBitmapCache.getTickBitmap("BTC-USDT")).thenReturn(Optional.empty());

            // Act
            List<Tick> result = tickEvent.fetchTicksFromBitmap();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    @DisplayName("fetchTicksFromBitmap should return ticks when bitmap has set bits")
    void fetchTicksFromBitmap_ShouldReturnTicks_WhenBitmapHasSetBits() {
        // Arrange
        try (MockedStatic<TickBitmapCache> tickBitmapMock = Mockito.mockStatic(TickBitmapCache.class);
                MockedStatic<TickCache> tickCacheMock = Mockito.mockStatic(TickCache.class)) {

            // Mock bitmap cache
            tickBitmapMock.when(TickBitmapCache::getInstance).thenReturn(mockTickBitmapCache);

            // Create a mock bitmap with set bits
            com.exchangeengine.model.TickBitmap mockBitmap = mock(com.exchangeengine.model.TickBitmap.class);
            List<Integer> setBits = List.of(100, 200, 300);
            when(mockBitmap.getSetBits()).thenReturn(setBits);
            when(mockTickBitmapCache.getTickBitmap("BTC-USDT")).thenReturn(Optional.of(mockBitmap));

            // Mock tick cache
            tickCacheMock.when(TickCache::getInstance).thenReturn(mockTickCache);

            // Create mock ticks
            Tick tick1 = mock(Tick.class);
            Tick tick2 = mock(Tick.class);
            Tick tick3 = mock(Tick.class);

            // Setup tick cache to return ticks for specific keys
            when(mockTickCache.getTick("BTC-USDT-100")).thenReturn(Optional.of(tick1));
            when(mockTickCache.getTick("BTC-USDT-200")).thenReturn(Optional.of(tick2));
            when(mockTickCache.getTick("BTC-USDT-300")).thenReturn(Optional.of(tick3));

            // Act
            List<Tick> result = tickEvent.fetchTicksFromBitmap();

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.contains(tick1));
            assertTrue(result.contains(tick2));
            assertTrue(result.contains(tick3));
        }
    }
}
