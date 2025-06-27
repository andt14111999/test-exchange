package com.exchangeengine.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.factory.TickBitmapFactory;
import com.exchangeengine.model.TickBitmap;

@ExtendWith(MockitoExtension.class)
@DisplayName("TickUtils")
class TickUtilsTest {

  @Mock
  private TickBitmap mockTickBitmap;

  private TickBitmapFactory tickBitmapFactory;

  @BeforeEach
  void setUp() {
    tickBitmapFactory = new TickBitmapFactory();
  }

  @Test
  @DisplayName("updateTickBitmapIfFlipped should call flipBit when flipped")
  void updateTickBitmapIfFlipped_ShouldCallFlipBit_WhenFlipped() {
    // Given
    int tickIndex = 100;
    BigDecimal before = BigDecimal.ZERO;
    BigDecimal after = BigDecimal.ONE;

    // We know this will result in flipped=true
    when(mockTickBitmap.flipBit(tickIndex, true, true)).thenReturn(true);

    // When
    boolean result = TickUtils.updateTickBitmapIfFlipped(mockTickBitmap, tickIndex, after, before);

    // Then
    verify(mockTickBitmap).flipBit(tickIndex, true, true);
    assertTrue(result, "Result should be true when bitmap changed");
  }

  @Test
  @DisplayName("updateTickBitmapIfFlipped should not call flipBit when not flipped")
  void updateTickBitmapIfFlipped_ShouldNotCallFlipBit_WhenNotFlipped() {
    // Given
    int tickIndex = 100;
    BigDecimal before = BigDecimal.ONE;
    BigDecimal after = new BigDecimal("2");

    // When
    boolean result = TickUtils.updateTickBitmapIfFlipped(mockTickBitmap, tickIndex, after, before);

    // Then
    verify(mockTickBitmap, never()).flipBit(anyInt(), anyBoolean(), anyBoolean());
    assertFalse(result, "Result should be false when not flipped");
  }

  @ParameterizedTest
  @CsvSource({
      "0, 1, true", // 0->1: flipped, initialized
      "1, 0, false", // 1->0: flipped, not initialized
      "1, 2, false", // 1->2: not flipped
      "2, 1, false", // 2->1: not flipped
      "0, 0, false" // 0->0: not flipped
  })
  @DisplayName("updateTickBitmapIfFlipped should handle all liquidity transitions")
  void updateTickBitmapIfFlipped_ShouldHandleAllLiquidityTransitions(
      int beforeValue, int afterValue, boolean expectedInitialized) {
    // Given
    int tickIndex = 100;
    BigDecimal before = new BigDecimal(beforeValue);
    BigDecimal after = new BigDecimal(afterValue);

    boolean flipped = (before.compareTo(BigDecimal.ZERO) == 0) != (after.compareTo(BigDecimal.ZERO) == 0);

    TickBitmap realTickBitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");

    if (beforeValue > 0) {
      // Set the bit if we're simulating an already initialized tick
      realTickBitmap.setBit(tickIndex);
    }

    // When
    boolean result = TickUtils.updateTickBitmapIfFlipped(realTickBitmap, tickIndex, after, before);

    // Then
    assertEquals(flipped, result, "Result should match flipped status");

    if (flipped) {
      assertEquals(expectedInitialized, realTickBitmap.isSet(tickIndex),
          "Bit should be " + (expectedInitialized ? "set" : "unset") +
              " after transition from " + beforeValue + " to " + afterValue);
    } else {
      assertEquals(beforeValue > 0, realTickBitmap.isSet(tickIndex),
          "Bit should remain unchanged when not flipped");
    }
  }

  @Test
  @DisplayName("updateTickBitmapIfFlipped should integrate with real implementations")
  void updateTickBitmapIfFlipped_ShouldIntegrateWithRealImplementations() {
    // Given
    TickBitmap realTickBitmap = tickBitmapFactory.createEmptyBitmap("BTC-USDT");
    int tickIndex = 100;

    // When - flip from uninitialized to initialized (0 -> 1)
    boolean result1 = TickUtils.updateTickBitmapIfFlipped(
        realTickBitmap, tickIndex, BigDecimal.ONE, BigDecimal.ZERO);

    // Then
    assertTrue(result1, "Result should be true for 0->1 transition");
    assertTrue(realTickBitmap.isSet(tickIndex), "Bit should be set for initialized tick");

    // When - flip from initialized to uninitialized (1 -> 0)
    boolean result2 = TickUtils.updateTickBitmapIfFlipped(
        realTickBitmap, tickIndex, BigDecimal.ZERO, BigDecimal.ONE);

    // Then
    assertTrue(result2, "Result should be true for 1->0 transition");
    assertFalse(realTickBitmap.isSet(tickIndex), "Bit should be unset for uninitialized tick");

    // When - no flip (0 -> 0)
    boolean result3 = TickUtils.updateTickBitmapIfFlipped(
        realTickBitmap, tickIndex, BigDecimal.ZERO, BigDecimal.ZERO);

    // Then
    assertFalse(result3, "Result should be false for 0->0 transition");
    assertFalse(realTickBitmap.isSet(tickIndex), "Bit should remain unset for 0->0 transition");
  }
}
