package com.exchangeengine.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.exchangeengine.factory.TickFactory;
import com.exchangeengine.util.ammPool.AmmPoolConfig;

public class TickTest {

  private TickFactory tickFactory;

  @BeforeEach
  void setUp() {
    tickFactory = new TickFactory();
  }

  @Nested
  @DisplayName("Tick initialization tests")
  class TickInitializationTests {

    @Test
    @DisplayName("Should initialize tick with correct values")
    void shouldInitializeTickWithCorrectValues() {
      // Given
      String poolPair = "BTC-USDT";
      int tickIndex = 123;

      // When
      Tick tick = new Tick(poolPair, tickIndex);

      // Then
      assertEquals(poolPair, tick.getPoolPair());
      assertEquals(tickIndex, tick.getTickIndex());
      assertEquals(BigDecimal.ZERO, tick.getLiquidityGross());
      assertEquals(BigDecimal.ZERO, tick.getLiquidityNet());
      assertEquals(BigDecimal.ZERO, tick.getFeeGrowthOutside0());
      assertEquals(BigDecimal.ZERO, tick.getFeeGrowthOutside1());
      assertFalse(tick.isInitialized());
      assertTrue(tick.getTickInitializedTimestamp() > 0);
      assertTrue(tick.getCreatedAt() > 0);
      assertEquals(tick.getCreatedAt(), tick.getUpdatedAt());
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
      // Given
      Tick invalidTick = new Tick();

      // When
      List<String> errors = invalidTick.validateRequiredFields();

      // Then
      assertFalse(errors.isEmpty());
      assertTrue(errors.stream().anyMatch(error -> error.contains("Pool pair is required")));
    }
  }

  @Nested
  @DisplayName("Tick utility function tests")
  class TickUtilityTests {

    @Test
    @DisplayName("Should generate correct tick key")
    void shouldGenerateCorrectTickKey() {
      // Given
      String poolPair = "ETH-BTC";
      int tickIndex = 456;
      Tick tick = new Tick(poolPair, tickIndex);

      // When
      String tickKey = tick.getTickKey();

      // Then
      assertEquals(poolPair + "-" + tickIndex, tickKey);
    }

    @Test
    @DisplayName("toMessageJson should return correct JSON representation")
    void toMessageJson_ShouldReturnCorrectJsonRepresentation() {
      // Given
      String poolPair = "BTC-USDT";
      int tickIndex = 1000;
      BigDecimal liquidityGross = new BigDecimal("100");
      BigDecimal liquidityNet = new BigDecimal("50");
      BigDecimal feeGrowthOutside0 = new BigDecimal("10");
      BigDecimal feeGrowthOutside1 = new BigDecimal("20");
      boolean initialized = true;

      Tick tick = tickFactory.createFullTick(
          poolPair,
          tickIndex,
          liquidityGross,
          liquidityNet,
          feeGrowthOutside0,
          feeGrowthOutside1,
          initialized);

      long createdAt = tick.getCreatedAt();
      long updatedAt = tick.getUpdatedAt();
      long initializedTimestamp = tick.getTickInitializedTimestamp();

      // When
      Map<String, Object> json = tick.toMessageJson();

      // Then
      assertNotNull(json);
      assertEquals(poolPair, json.get("poolPair"));
      assertEquals(tickIndex, json.get("tickIndex"));
      // Skip tickKey check as it's not included in the JSON
      assertEquals(liquidityGross, json.get("liquidityGross"));
      assertEquals(liquidityNet, json.get("liquidityNet"));
      assertEquals(feeGrowthOutside0, json.get("feeGrowthOutside0"));
      assertEquals(feeGrowthOutside1, json.get("feeGrowthOutside1"));
      assertEquals(initialized, json.get("initialized"));
      assertEquals(initializedTimestamp, json.get("tickInitializedTimestamp"));
      assertEquals(createdAt, json.get("createdAt"));
      assertEquals(updatedAt, json.get("updatedAt"));
    }

    @Test
    @DisplayName("toMessageJson should handle zero values correctly")
    void toMessageJson_ShouldHandleZeroValuesCorrectly() {
      // Given
      String poolPair = "ETH-USDT";
      int tickIndex = -500;

      // Create a tick with all zero values
      Tick tick = new Tick(poolPair, tickIndex);

      // When
      Map<String, Object> json = tick.toMessageJson();

      // Then
      assertNotNull(json);
      assertEquals(poolPair, json.get("poolPair"));
      assertEquals(tickIndex, json.get("tickIndex"));
      assertEquals(BigDecimal.ZERO, json.get("liquidityGross"));
      assertEquals(BigDecimal.ZERO, json.get("liquidityNet"));
      assertEquals(BigDecimal.ZERO, json.get("feeGrowthOutside0"));
      assertEquals(BigDecimal.ZERO, json.get("feeGrowthOutside1"));
      assertFalse((Boolean) json.get("initialized"));
    }

    @Test
    @DisplayName("toMessageJson should include all required fields")
    void toMessageJson_ShouldIncludeAllRequiredFields() {
      // Given
      Tick tick = tickFactory.createRandomTick("BTC-ETH");

      // When
      Map<String, Object> json = tick.toMessageJson();

      // Then
      assertNotNull(json);
      // Verify all expected fields are present
      assertTrue(json.containsKey("poolPair"));
      assertTrue(json.containsKey("tickIndex"));
      assertTrue(json.containsKey("liquidityGross"));
      assertTrue(json.containsKey("liquidityNet"));
      assertTrue(json.containsKey("feeGrowthOutside0"));
      assertTrue(json.containsKey("feeGrowthOutside1"));
      assertTrue(json.containsKey("initialized"));
      assertTrue(json.containsKey("tickInitializedTimestamp"));
      assertTrue(json.containsKey("createdAt"));
      assertTrue(json.containsKey("updatedAt"));
    }

    @Test
    @DisplayName("toMessageJson should handle negative values correctly")
    void toMessageJson_ShouldHandleNegativeValuesCorrectly() {
      // Given
      String poolPair = "BTC-USDT";
      int tickIndex = -1000;
      BigDecimal liquidityGross = new BigDecimal("100");
      BigDecimal liquidityNet = new BigDecimal("-50"); // Negative net liquidity
      BigDecimal feeGrowthOutside0 = new BigDecimal("10");
      BigDecimal feeGrowthOutside1 = new BigDecimal("20");
      boolean initialized = true;

      Tick tick = tickFactory.createFullTick(
          poolPair,
          tickIndex,
          liquidityGross,
          liquidityNet,
          feeGrowthOutside0,
          feeGrowthOutside1,
          initialized);

      // When
      Map<String, Object> json = tick.toMessageJson();

      // Then
      assertNotNull(json);
      assertEquals(poolPair, json.get("poolPair"));
      assertEquals(tickIndex, json.get("tickIndex"));
      assertEquals(liquidityGross, json.get("liquidityGross"));
      assertEquals(liquidityNet, json.get("liquidityNet")); // Should preserve negative value
      assertEquals(feeGrowthOutside0, json.get("feeGrowthOutside0"));
      assertEquals(feeGrowthOutside1, json.get("feeGrowthOutside1"));
      assertEquals(initialized, json.get("initialized"));
    }
  }

  @Nested
  @DisplayName("Tick update tests")
  class TickUpdateTests {

    @Test
    @DisplayName("Should flip from zero to nonzero")
    void shouldFlipFromZeroToNonzero() {
      // Given
      Tick tick = tickFactory.createTick("BTC-USDT", 0);
      BigDecimal liquidityDelta = BigDecimal.ONE;
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertTrue(flipped);
      assertEquals(BigDecimal.ONE, tick.getLiquidityGross());
      assertEquals(BigDecimal.ONE, tick.getLiquidityNet());
      assertTrue(tick.isInitialized());
    }

    @Test
    @DisplayName("Should not flip from nonzero to greater nonzero")
    void shouldNotFlipFromNonzeroToGreaterNonzero() {
      // Given
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          BigDecimal.ONE, BigDecimal.ONE);
      BigDecimal liquidityDelta = BigDecimal.ONE;
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertFalse(flipped);
      assertEquals(new BigDecimal("2"), tick.getLiquidityGross());
      assertEquals(new BigDecimal("2"), tick.getLiquidityNet());
    }

    @Test
    @DisplayName("Should flip from nonzero to zero")
    void shouldFlipFromNonzeroToZero() {
      // Given
      BigDecimal initialLiquidity = BigDecimal.ONE;
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          initialLiquidity, initialLiquidity);

      // When removing liquidity, we use the negative of the initial liquidity
      BigDecimal liquidityDelta = initialLiquidity.negate();
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertTrue(flipped);
      assertEquals(BigDecimal.ZERO, tick.getLiquidityGross());
      assertEquals(BigDecimal.ZERO, tick.getLiquidityNet());
      // Note: initialized should remain true even with zero liquidity
      assertTrue(tick.isInitialized());
    }

    @Test
    @DisplayName("Should not flip from nonzero to lesser nonzero")
    void shouldNotFlipFromNonzeroToLesserNonzero() {
      // Given
      BigDecimal initialLiquidity = new BigDecimal("2");
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          initialLiquidity, initialLiquidity);

      // When removing some but not all liquidity, we use a negative value but not
      // enough to make it zero
      BigDecimal liquidityDelta = BigDecimal.ONE.negate();
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertFalse(flipped);
      assertEquals(BigDecimal.ONE, tick.getLiquidityGross());
      assertEquals(BigDecimal.ONE, tick.getLiquidityNet());
      assertTrue(tick.isInitialized());
    }

    @Test
    @DisplayName("Should throw exception if total liquidity gross is greater than max")
    void shouldThrowExceptionIfLiquidityGrossExceedsMax() {
      // Given
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          new BigDecimal("500"), new BigDecimal("500"));
      BigDecimal liquidityDelta = new BigDecimal("600");
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // Then
      assertThrows(IllegalArgumentException.class, () -> {
        tick.update(liquidityDelta, false, maxLiquidity);
      });
    }

    @Test
    @DisplayName("Should net the liquidity based on upper flag")
    void shouldNetLiquidityBasedOnUpperFlag() {
      // Given
      Tick tick = tickFactory.createTick("BTC-USDT", 0);
      BigDecimal liquidityDelta = new BigDecimal("100");
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When updating as upper tick
      tick.update(liquidityDelta, true, maxLiquidity);

      // Then liquidityNet should be negative for upper ticks
      assertEquals(new BigDecimal("100"), tick.getLiquidityGross());
      assertEquals(new BigDecimal("-100"), tick.getLiquidityNet());

      // When updating as lower tick
      tick = tickFactory.createTick("BTC-USDT", 0);
      tick.update(liquidityDelta, false, maxLiquidity);

      // Then liquidityNet should be positive for lower ticks
      assertEquals(new BigDecimal("100"), tick.getLiquidityGross());
      assertEquals(new BigDecimal("100"), tick.getLiquidityNet());
    }

    @Test
    @DisplayName("Should update fee growth outside when tick is below current tick")
    void shouldUpdateFeeGrowthOutsideWhenTickIsBelowCurrentTick() {
      // Given
      Tick tick = tickFactory.createTick("BTC-USDT", 100);
      BigDecimal liquidityDelta = new BigDecimal("100");
      BigDecimal maxLiquidity = new BigDecimal("1000");
      int tickCurrent = 200; // Current tick is above our test tick
      BigDecimal feeGrowthGlobal0 = new BigDecimal("30");
      BigDecimal feeGrowthGlobal1 = new BigDecimal("40");

      // When
      tick.update(liquidityDelta, false, maxLiquidity, tickCurrent, feeGrowthGlobal0, feeGrowthGlobal1);

      // Then
      // Fee growth should be updated since tick is below current tick
      assertEquals(feeGrowthGlobal0, tick.getFeeGrowthOutside0());
      assertEquals(feeGrowthGlobal1, tick.getFeeGrowthOutside1());
      assertTrue(tick.isInitialized());
    }

    @Test
    @DisplayName("Should not update fee growth outside when tick is above current tick")
    void shouldNotUpdateFeeGrowthOutsideWhenTickIsAboveCurrentTick() {
      // Given
      Tick tick = tickFactory.createTick("BTC-USDT", 300);
      BigDecimal liquidityDelta = new BigDecimal("100");
      BigDecimal maxLiquidity = new BigDecimal("1000");
      int tickCurrent = 200; // Current tick is below our test tick
      BigDecimal feeGrowthGlobal0 = new BigDecimal("30");
      BigDecimal feeGrowthGlobal1 = new BigDecimal("40");

      // When
      tick.update(liquidityDelta, false, maxLiquidity, tickCurrent, feeGrowthGlobal0, feeGrowthGlobal1);

      // Then
      // Fee growth should not be updated since tick is above current tick
      assertEquals(BigDecimal.ZERO, tick.getFeeGrowthOutside0());
      assertEquals(BigDecimal.ZERO, tick.getFeeGrowthOutside1());
      assertTrue(tick.isInitialized());
    }

    @Test
    @DisplayName("Should update fee growth outside when tick equals current tick")
    void shouldUpdateFeeGrowthOutsideWhenTickEqualsCurrentTick() {
      // Given
      int tickCurrent = 200;
      Tick tick = tickFactory.createTick("BTC-USDT", tickCurrent); // Same as current tick
      BigDecimal liquidityDelta = new BigDecimal("100");
      BigDecimal maxLiquidity = new BigDecimal("1000");
      BigDecimal feeGrowthGlobal0 = new BigDecimal("30");
      BigDecimal feeGrowthGlobal1 = new BigDecimal("40");

      // When
      tick.update(liquidityDelta, false, maxLiquidity, tickCurrent, feeGrowthGlobal0, feeGrowthGlobal1);

      // Then
      // Fee growth should be updated since tick equals current tick (<=)
      assertEquals(feeGrowthGlobal0, tick.getFeeGrowthOutside0());
      assertEquals(feeGrowthGlobal1, tick.getFeeGrowthOutside1());
      assertTrue(tick.isInitialized());
    }

    @Test
    @DisplayName("Should handle negative liquidity delta correctly")
    void shouldHandleNegativeLiquidityDeltaCorrectly() {
      // Given
      BigDecimal initialLiquidity = new BigDecimal("100");
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          initialLiquidity, initialLiquidity);
      BigDecimal liquidityDelta = new BigDecimal("-50"); // Negative delta
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertFalse(flipped); // Not flipped since we still have liquidity
      assertEquals(new BigDecimal("50"), tick.getLiquidityGross()); // 100 - 50 = 50
      assertEquals(new BigDecimal("50"), tick.getLiquidityNet()); // For lower tick, net is added
    }

    @Test
    @DisplayName("Should handle extremely negative liquidity delta correctly")
    void shouldHandleExtremelyNegativeLiquidityDeltaCorrectly() {
      // Given
      BigDecimal initialLiquidity = new BigDecimal("100");
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          initialLiquidity, initialLiquidity);
      BigDecimal liquidityDelta = new BigDecimal("-200"); // More negative than current liquidity
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertTrue(flipped); // Flipped since liquidity goes to zero
      assertEquals(BigDecimal.ZERO, tick.getLiquidityGross()); // Should be zero, not negative

      // Note: In the current implementation, liquidityNet is updated independently of
      // liquidityGross
      // and is not clamped to zero when liquidityGross becomes zero
      // This is the actual behavior of the implementation
      assertEquals(new BigDecimal("-100"), tick.getLiquidityNet()); // Net is updated by delta

      assertTrue(tick.isInitialized()); // Should remain initialized
    }
  }

  @Nested
  @DisplayName("Tick cross tests")
  class TickCrossTests {

    @Test
    @DisplayName("Should flip growth variables when crossing")
    void shouldFlipGrowthVariablesWhenCrossing() {
      // Given
      BigDecimal initialFeeGrowth0 = new BigDecimal("10");
      BigDecimal initialFeeGrowth1 = new BigDecimal("20");

      Tick tick = tickFactory.createTickWithFeeGrowth("BTC-USDT", 0,
          initialFeeGrowth0, initialFeeGrowth1);

      BigDecimal feeGrowthGlobal0 = new BigDecimal("30");
      BigDecimal feeGrowthGlobal1 = new BigDecimal("40");

      // When
      BigDecimal liquidityNet = tick.cross(feeGrowthGlobal0, feeGrowthGlobal1);

      // Then
      assertEquals(BigDecimal.ZERO, liquidityNet); // No liquidity added
      assertEquals(feeGrowthGlobal0.subtract(initialFeeGrowth0), tick.getFeeGrowthOutside0()); // 30 - 10 = 20
      assertEquals(feeGrowthGlobal1.subtract(initialFeeGrowth1), tick.getFeeGrowthOutside1()); // 40 - 20 = 20
    }

    @Test
    @DisplayName("Two consecutive flips should be a no-op")
    void twoConsecutiveFlipsShouldBeNoOp() {
      // Given
      BigDecimal initialFeeGrowth0 = new BigDecimal("10");
      BigDecimal initialFeeGrowth1 = new BigDecimal("20");

      Tick tick = tickFactory.createTickWithFeeGrowth("BTC-USDT", 0,
          initialFeeGrowth0, initialFeeGrowth1);

      BigDecimal feeGrowthGlobal0 = new BigDecimal("30");
      BigDecimal feeGrowthGlobal1 = new BigDecimal("40");

      // When crossing back and forth
      tick.cross(feeGrowthGlobal0, feeGrowthGlobal1);
      tick.cross(feeGrowthGlobal0, feeGrowthGlobal1);

      // Then we should be back to initial values
      assertEquals(initialFeeGrowth0, tick.getFeeGrowthOutside0());
      assertEquals(initialFeeGrowth1, tick.getFeeGrowthOutside1());
    }

    @Test
    @DisplayName("Should return liquidityNet when crossing")
    void shouldReturnLiquidityNetWhenCrossing() {
      // Given
      BigDecimal initialFeeGrowth0 = new BigDecimal("10");
      BigDecimal initialFeeGrowth1 = new BigDecimal("20");
      BigDecimal liquidityNet = new BigDecimal("100");

      // Create a tick with non-zero liquidityNet
      Tick tick = tickFactory.createFullTick(
          "BTC-USDT",
          0,
          new BigDecimal("100"), // liquidityGross
          liquidityNet, // liquidityNet
          initialFeeGrowth0,
          initialFeeGrowth1,
          true);

      BigDecimal feeGrowthGlobal0 = new BigDecimal("30");
      BigDecimal feeGrowthGlobal1 = new BigDecimal("40");

      // When
      BigDecimal result = tick.cross(feeGrowthGlobal0, feeGrowthGlobal1);

      // Then
      assertEquals(liquidityNet, result); // Should return the liquidityNet value
      assertEquals(feeGrowthGlobal0.subtract(initialFeeGrowth0), tick.getFeeGrowthOutside0());
      assertEquals(feeGrowthGlobal1.subtract(initialFeeGrowth1), tick.getFeeGrowthOutside1());
    }

    @Test
    @DisplayName("Should return negative liquidityNet when crossing")
    void shouldReturnNegativeLiquidityNetWhenCrossing() {
      // Given
      BigDecimal initialFeeGrowth0 = new BigDecimal("10");
      BigDecimal initialFeeGrowth1 = new BigDecimal("20");
      BigDecimal liquidityNet = new BigDecimal("-100"); // Negative liquidityNet

      // Create a tick with negative liquidityNet
      Tick tick = tickFactory.createFullTick(
          "BTC-USDT",
          0,
          new BigDecimal("100"), // liquidityGross
          liquidityNet, // liquidityNet
          initialFeeGrowth0,
          initialFeeGrowth1,
          true);

      BigDecimal feeGrowthGlobal0 = new BigDecimal("30");
      BigDecimal feeGrowthGlobal1 = new BigDecimal("40");

      // When
      BigDecimal result = tick.cross(feeGrowthGlobal0, feeGrowthGlobal1);

      // Then
      assertEquals(liquidityNet, result); // Should return the negative liquidityNet value
      assertEquals(feeGrowthGlobal0.subtract(initialFeeGrowth0), tick.getFeeGrowthOutside0());
      assertEquals(feeGrowthGlobal1.subtract(initialFeeGrowth1), tick.getFeeGrowthOutside1());
    }

    @Test
    @DisplayName("Should update timestamp when crossing")
    void shouldUpdateTimestampWhenCrossing() {
      // Given
      Tick tick = tickFactory.createTickWithFeeGrowth("BTC-USDT", 0,
          BigDecimal.TEN, BigDecimal.TEN);

      long initialUpdatedAt = tick.getUpdatedAt();

      // Wait a little to ensure timestamp difference
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      // When
      tick.cross(new BigDecimal("50"), new BigDecimal("60"));

      // Then
      assertTrue(tick.getUpdatedAt() > initialUpdatedAt);
    }
  }

  @Nested
  @DisplayName("Tick clear tests")
  class TickClearTests {

    @Test
    @DisplayName("Should clear all tick data")
    void shouldClearAllTickData() {
      // Given
      Tick tick = tickFactory.createFullTick("BTC-USDT", 0,
          new BigDecimal("100"),
          new BigDecimal("100"),
          new BigDecimal("50"),
          new BigDecimal("75"),
          true);

      // When
      tick.clear();

      // Then
      assertEquals(BigDecimal.ZERO, tick.getLiquidityGross());
      assertEquals(BigDecimal.ZERO, tick.getLiquidityNet());
      assertEquals(BigDecimal.ZERO, tick.getFeeGrowthOutside0());
      assertEquals(BigDecimal.ZERO, tick.getFeeGrowthOutside1());
      assertFalse(tick.isInitialized());
      assertTrue(tick.getUpdatedAt() >= tick.getCreatedAt()); // updatedAt should be updated
    }
  }

  @Nested
  @DisplayName("Tick validation tests")
  class TickValidationTests {

    @Test
    @DisplayName("Should validate tick index is within bounds")
    void shouldValidateTickIndexWithinBounds() {
      // Given
      Tick tick = new Tick("BTC-USDT", AmmPoolConfig.MIN_TICK + 1);

      // When
      List<String> errors = tick.validateRequiredFields();

      // Then
      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Should detect when tick index is below MIN_TICK")
    void shouldDetectWhenTickIndexIsBelowMinTick() {
      // Given
      Tick tick = new Tick("BTC-USDT", AmmPoolConfig.MIN_TICK - 1);

      // When
      List<String> errors = tick.validateRequiredFields();

      // Then
      assertFalse(errors.isEmpty());
      assertTrue(errors.stream().anyMatch(error -> error.contains("Tick index must be")));
    }

    @Test
    @DisplayName("Should detect when tick index is above MAX_TICK")
    void shouldDetectWhenTickIndexIsAboveMaxTick() {
      // Given
      Tick tick = new Tick("BTC-USDT", AmmPoolConfig.MAX_TICK + 1);

      // When
      List<String> errors = tick.validateRequiredFields();

      // Then
      assertFalse(errors.isEmpty());
      assertTrue(errors.stream().anyMatch(error -> error.contains("Tick index must be")));
    }

    @Test
    @DisplayName("Should include both tick validation and required field errors")
    void shouldIncludeBothTickValidationAndRequiredFieldErrors() {
      // Given
      Tick tick = new Tick();
      tick.setPoolPair(null);
      tick.setTickIndex(AmmPoolConfig.MAX_TICK + 1);

      // When
      List<String> errors = tick.validateRequiredFields();

      // Then
      assertTrue(errors.size() >= 2);
      assertTrue(errors.stream().anyMatch(error -> error.contains("Pool pair is required")));
      assertTrue(errors.stream().anyMatch(error -> error.contains("Tick index must be")));
    }
  }

  @Nested
  @DisplayName("Tick update and flipped status tests")
  class TickUpdateAndFlippedTests {

    @Test
    @DisplayName("Should flip from zero to non-zero liquidity")
    void shouldFlipFromZeroToNonZeroLiquidity() {
      // Given
      Tick tick = tickFactory.createTick("BTC-USDT", 0);
      BigDecimal liquidityDelta = new BigDecimal("10");
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertTrue(flipped);
      assertEquals(new BigDecimal("10"), tick.getLiquidityGross());
      assertTrue(tick.isInitialized());
    }

    @Test
    @DisplayName("Should flip from non-zero to zero liquidity")
    void shouldFlipFromNonZeroToZeroLiquidity() {
      // Given
      BigDecimal initialLiquidity = new BigDecimal("10");
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          initialLiquidity, initialLiquidity);
      BigDecimal liquidityDelta = initialLiquidity.negate();
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertTrue(flipped);
      assertEquals(BigDecimal.ZERO, tick.getLiquidityGross());
    }

    @Test
    @DisplayName("Should not flip when remaining non-zero")
    void shouldNotFlipWhenRemainingNonZero() {
      // Given
      BigDecimal initialLiquidity = new BigDecimal("20");
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          initialLiquidity, initialLiquidity);
      BigDecimal liquidityDelta = new BigDecimal("10").negate();
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When
      boolean flipped = tick.update(liquidityDelta, false, maxLiquidity);

      // Then
      assertFalse(flipped);
      assertEquals(new BigDecimal("10"), tick.getLiquidityGross());
    }

    @Test
    @DisplayName("Should handle upper flag correctly for liquidityNet")
    void shouldHandleUpperFlagCorrectlyForLiquidityNet() {
      // Given
      Tick lowerTick = tickFactory.createTick("BTC-USDT", -10);
      Tick upperTick = tickFactory.createTick("BTC-USDT", 10);
      BigDecimal liquidityDelta = new BigDecimal("50");
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // When adding liquidity
      lowerTick.update(liquidityDelta, false, maxLiquidity); // Lower tick gets positive liquidityNet
      upperTick.update(liquidityDelta, true, maxLiquidity); // Upper tick gets negative liquidityNet

      // Then
      assertEquals(new BigDecimal("50"), lowerTick.getLiquidityNet());
      assertEquals(new BigDecimal("-50"), upperTick.getLiquidityNet());
    }

    @Test
    @DisplayName("Should throw exception when liquidity exceeds maximum")
    void shouldThrowExceptionWhenLiquidityExceedsMaximum() {
      // Given
      BigDecimal initialLiquidity = new BigDecimal("900");
      Tick tick = tickFactory.createInitializedTick("BTC-USDT", 0,
          initialLiquidity, initialLiquidity);
      BigDecimal liquidityDelta = new BigDecimal("200");
      BigDecimal maxLiquidity = new BigDecimal("1000");

      // Then
      assertThrows(IllegalArgumentException.class, () -> {
        tick.update(liquidityDelta, false, maxLiquidity);
      });
    }

    @Test
    @DisplayName("Should initialize timestamp on first update")
    void shouldInitializeTimestampOnFirstUpdate() {
      // Given
      Tick tick = tickFactory.createTick("BTC-USDT", 0);
      long initializedTimestamp = tick.getTickInitializedTimestamp();

      // Wait a little to ensure timestamp difference
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      // When
      tick.update(BigDecimal.ONE, false, new BigDecimal("1000"));

      // Then
      assertTrue(tick.getTickInitializedTimestamp() > initializedTimestamp);
    }

    @Test
    @DisplayName("Should update timestamp at each liquidity change")
    void shouldUpdateTimestampAtEachLiquidityChange() {
      // Given
      Tick tick = tickFactory.createTick("BTC-USDT", 0);

      // First update
      tick.update(BigDecimal.ONE, false, new BigDecimal("1000"));
      long firstUpdateTimestamp = tick.getUpdatedAt();

      // Wait a little to ensure timestamp difference
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      // Second update
      tick.update(BigDecimal.ONE, false, new BigDecimal("1000"));
      long secondUpdateTimestamp = tick.getUpdatedAt();

      // Then
      assertTrue(secondUpdateTimestamp > firstUpdateTimestamp);
    }
  }

  @Nested
  @DisplayName("isFlipped method tests")
  class IsFlippedTests {

    @Test
    @DisplayName("Should detect flipped from zero to non-zero")
    void shouldDetectFlippedFromZeroToNonZero() {
      // Given
      BigDecimal before = BigDecimal.ZERO;
      BigDecimal after = BigDecimal.ONE;

      // When
      boolean flipped = Tick.isFlipped(after, before);

      // Then
      assertTrue(flipped);
    }

    @Test
    @DisplayName("Should detect flipped from non-zero to zero")
    void shouldDetectFlippedFromNonZeroToZero() {
      // Given
      BigDecimal before = BigDecimal.ONE;
      BigDecimal after = BigDecimal.ZERO;

      // When
      boolean flipped = Tick.isFlipped(after, before);

      // Then
      assertTrue(flipped);
    }

    @Test
    @DisplayName("Should not detect flipped from zero to zero")
    void shouldNotDetectFlippedFromZeroToZero() {
      // Given
      BigDecimal before = BigDecimal.ZERO;
      BigDecimal after = BigDecimal.ZERO;

      // When
      boolean flipped = Tick.isFlipped(after, before);

      // Then
      assertFalse(flipped);
    }

    @Test
    @DisplayName("Should not detect flipped from non-zero to non-zero")
    void shouldNotDetectFlippedFromNonZeroToNonZero() {
      // Given
      BigDecimal before = BigDecimal.ONE;
      BigDecimal after = new BigDecimal("2");

      // When
      boolean flipped = Tick.isFlipped(after, before);

      // Then
      assertFalse(flipped);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1, true",
        "1, 0, true",
        "0, 0, false",
        "1, 1, false",
        "10, 5, false",
        "0, 100, true",
        "100, 0, true"
    })
    @DisplayName("Should correctly detect flipped status for various values")
    void shouldCorrectlyDetectFlippedStatus(int afterValue, int beforeValue, boolean expected) {
      // When
      boolean flipped = Tick.isFlipped(new BigDecimal(afterValue), new BigDecimal(beforeValue));

      // Then
      assertEquals(expected, flipped);
    }
  }
}
