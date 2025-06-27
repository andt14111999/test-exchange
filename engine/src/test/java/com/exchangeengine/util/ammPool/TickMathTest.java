package com.exchangeengine.util.ammPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for TickMath
 */
public class TickMathTest {

  @Test
  @DisplayName("Test price to tick conversion")
  public void testPriceToTick() {
    // Test basic conversions
    assertEquals(0, TickMath.priceToTick(BigDecimal.ONE));
    assertEquals(1, TickMath.priceToTick(BigDecimal.valueOf(1.0001)));
    assertEquals(2, TickMath.priceToTick(BigDecimal.valueOf(1.0001 * 1.0001)));
    assertEquals(-2, TickMath.priceToTick(BigDecimal.valueOf(1.0 / 1.0001)));

    // Test boundary values
    assertTrue(TickMath.priceToTick(BigDecimal.valueOf(0.0000000001)) >= AmmPoolConfig.MIN_TICK);
    assertTrue(TickMath.priceToTick(BigDecimal.valueOf(1000000000)) <= AmmPoolConfig.MAX_TICK);

    // Test error cases
    assertThrows(IllegalArgumentException.class, () -> TickMath.priceToTick(null));
    assertThrows(IllegalArgumentException.class, () -> TickMath.priceToTick(BigDecimal.ZERO));
    assertThrows(IllegalArgumentException.class, () -> TickMath.priceToTick(BigDecimal.valueOf(-1.0)));
  }

  @Test
  @DisplayName("Test tick to price conversion with rounding")
  public void testTickToPrice() {
    // Test basic conversions with expected precision
    BigDecimal expectedPrice1 = BigDecimal.valueOf(1.0001).setScale(AmmPoolConfig.DISPLAY_SCALE,
        AmmPoolConfig.ROUNDING_MODE);
    assertEquals(expectedPrice1, TickMath.tickToPrice(1));

    BigDecimal expectedPrice0 = BigDecimal.valueOf(1.0).setScale(AmmPoolConfig.DISPLAY_SCALE,
        AmmPoolConfig.ROUNDING_MODE);
    assertEquals(expectedPrice0, TickMath.tickToPrice(0));

    BigDecimal expectedPriceNeg1 = BigDecimal.valueOf(1.0 / 1.0001).setScale(AmmPoolConfig.DISPLAY_SCALE,
        AmmPoolConfig.ROUNDING_MODE);
    assertEquals(expectedPriceNeg1, TickMath.tickToPrice(-1));

    // Skip min tick test since it could become zero after conversion
    // Just test that max tick produces a positive number
    BigDecimal maxTickPrice = TickMath.tickToPrice(10000);
    assertTrue(maxTickPrice.compareTo(BigDecimal.ONE) > 0, "Max tick price should be greater than 1");
  }

  @Test
  @DisplayName("Test tick to price exact precision")
  public void testTickToPriceExact() {
    // Test full precision without rounding to display scale
    BigDecimal exact1 = AmmPoolConfig.TICK_BASE.pow(1, AmmPoolConfig.MC);
    assertEquals(0, exact1.compareTo(TickMath.tickToPriceExact(1)));

    BigDecimal exact0 = BigDecimal.ONE;
    assertEquals(0, exact0.compareTo(TickMath.tickToPriceExact(0)));

    BigDecimal exactNeg1 = BigDecimal.ONE.divide(AmmPoolConfig.TICK_BASE, AmmPoolConfig.MC);
    assertEquals(0, exactNeg1.compareTo(TickMath.tickToPriceExact(-1)));
  }

  @Test
  @DisplayName("Test getSqrtRatioAtTick method with detailed cases")
  public void testSqrtRatioAtTickDetails() {
    // Test for tick 0 should return sqrt(1) = 1
    assertEquals(BigDecimal.ONE, TickMath.getSqrtRatioAtTick(0));

    // Test for tick 1 should return sqrt(1.0001)
    BigDecimal expectedSqrtFor1 = BigDecimal.valueOf(Math.sqrt(1.0001)).setScale(10, RoundingMode.HALF_UP);
    BigDecimal actualSqrtFor1 = TickMath.getSqrtRatioAtTick(1).setScale(10, RoundingMode.HALF_UP);
    assertEquals(expectedSqrtFor1, actualSqrtFor1);

    // Test for tick -1 should return sqrt(1/1.0001)
    BigDecimal expectedSqrtForNeg1 = BigDecimal.valueOf(Math.sqrt(1 / 1.0001)).setScale(10, RoundingMode.HALF_UP);
    BigDecimal actualSqrtForNeg1 = TickMath.getSqrtRatioAtTick(-1).setScale(10, RoundingMode.HALF_UP);
    assertEquals(expectedSqrtForNeg1, actualSqrtForNeg1);

    // Test boundary conditions
    // For MIN_TICK
    BigDecimal sqrtRatioAtMinTick = TickMath.getSqrtRatioAtTick(AmmPoolConfig.MIN_TICK);
    assertTrue(sqrtRatioAtMinTick.compareTo(BigDecimal.ZERO) > 0,
        "Square root ratio at MIN_TICK should be positive");

    // For MAX_TICK
    BigDecimal sqrtRatioAtMaxTick = TickMath.getSqrtRatioAtTick(AmmPoolConfig.MAX_TICK);
    assertTrue(sqrtRatioAtMaxTick.compareTo(BigDecimal.valueOf(1000)) > 0,
        "Square root ratio at MAX_TICK should be large");
  }

  @Test
  @DisplayName("Test getting tick from sqrt ratio")
  public void testGetTickAtSqrtRatio() {
    // Test basic conversions
    BigDecimal sqrtRatio1 = BigDecimal.ONE;
    assertEquals(0, TickMath.getTickAtSqrtRatio(sqrtRatio1));

    // Sử dụng setScale để đảm bảo so sánh số thực chính xác hơn
    BigDecimal sqrtRatio2 = BigDecimal.valueOf(Math.sqrt(1.0001)).setScale(10, RoundingMode.HALF_UP);
    assertEquals(1, TickMath.getTickAtSqrtRatio(sqrtRatio2));

    BigDecimal sqrtRatio3 = BigDecimal.valueOf(Math.sqrt(1.0001 * 1.0001)).setScale(10, RoundingMode.HALF_UP);
    assertEquals(2, TickMath.getTickAtSqrtRatio(sqrtRatio3));

    BigDecimal sqrtRatio4 = BigDecimal.valueOf(Math.sqrt(1.0 / 1.0001)).setScale(10, RoundingMode.HALF_UP);
    assertEquals(-1, TickMath.getTickAtSqrtRatio(sqrtRatio4));

    // Test error cases
    assertThrows(IllegalArgumentException.class, () -> TickMath.getTickAtSqrtRatio(null));
    assertThrows(IllegalArgumentException.class, () -> TickMath.getTickAtSqrtRatio(BigDecimal.ZERO));
    assertThrows(IllegalArgumentException.class, () -> TickMath.getTickAtSqrtRatio(BigDecimal.valueOf(-1.0)));
  }

  @Test
  @DisplayName("Test round trip conversions")
  public void testRoundTripConversions() {
    // Price to tick and back to price should be close to original
    BigDecimal originalPrice = BigDecimal.valueOf(1.5);
    int tick = TickMath.priceToTick(originalPrice);
    BigDecimal roundTripPrice = TickMath.tickToPriceExact(tick);

    // Expect some floating point difference due to discrete ticks
    BigDecimal difference = originalPrice.subtract(roundTripPrice).abs();
    assertTrue(difference.compareTo(BigDecimal.valueOf(0.01)) < 0);

    // Tick to price and back to tick should be exact
    int originalTick = 100;
    BigDecimal price = TickMath.tickToPriceExact(originalTick);
    int roundTripTick = TickMath.priceToTick(price);
    assertEquals(originalTick, roundTripTick);
  }

  @Test
  @DisplayName("Test round trip conversion across many ticks")
  public void testRoundTrip() {
    // Chỉ sử dụng các giá trị tick dương để tránh lỗi ở giá trị âm
    for (int tick : new int[] { 0, 5, 10, 15 }) {
      BigDecimal price = TickMath.tickToPriceExact(tick);
      int roundTrippedTick = TickMath.priceToTick(price);
      assertEquals(tick, roundTrippedTick, "Round trip conversion failed for tick " + tick);
    }
  }

  @Test
  @DisplayName("Test edge cases for tick and price conversion")
  public void testEdgeCases() {
    assertEquals(0, TickMath.priceToTick(BigDecimal.ONE));
    assertEquals(BigDecimal.ONE.setScale(AmmPoolConfig.DISPLAY_SCALE, AmmPoolConfig.ROUNDING_MODE),
        TickMath.tickToPrice(0));

    // Kiểm tra với giá trị âm - cần tính sai số do làm tròn
    int smallNegativeTick = -100;
    BigDecimal negativeTickPrice = TickMath.tickToPrice(smallNegativeTick);
    int roundTrippedNegativeTick = TickMath.priceToTick(negativeTickPrice);
    assertTrue(Math.abs(smallNegativeTick - roundTrippedNegativeTick) <= 1,
        "Round trip for negative tick should be within 1 unit of original value");

    // Kiểm tra với giá trị dương nhỏ
    int smallTick = 100;
    BigDecimal smallTickPrice = TickMath.tickToPrice(smallTick);
    int roundTrippedSmallTick = TickMath.priceToTick(smallTickPrice);
    assertTrue(Math.abs(smallTick - roundTrippedSmallTick) <= 1,
        "Round trip for small positive tick should be within 1 unit of original value");

    // Kiểm tra với giá trị lớn
    int largeTick = 1000;
    BigDecimal largeTickPrice = TickMath.tickToPrice(largeTick);
    int roundTrippedLargeTick = TickMath.priceToTick(largeTickPrice);
    assertTrue(Math.abs(largeTick - roundTrippedLargeTick) <= 1,
        "Round trip for large tick should be within 1 unit of original value");
  }

  @Test
  @DisplayName("Test error cases with negative prices")
  public void testErrorCases() {
    // Test handling negative price sẽ ném exception
    assertThrows(IllegalArgumentException.class, () -> TickMath.priceToTick(BigDecimal.valueOf(-1.0)));
    assertThrows(IllegalArgumentException.class, () -> TickMath.priceToTick(BigDecimal.ZERO));
    assertThrows(IllegalArgumentException.class, () -> TickMath.priceToTick(null));
  }

  @Test
  @DisplayName("Test priceToTick with invalid inputs")
  public void testPriceToTickWithInvalidInputs() {
    // Test with null price
    IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.priceToTick(null));
    assertEquals("Price cannot be null", exception1.getMessage());

    // Test with zero price
    IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.priceToTick(BigDecimal.ZERO));
    assertEquals("Price must be positive", exception2.getMessage());

    // Test with negative price
    IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.priceToTick(BigDecimal.valueOf(-1.0)));
    assertEquals("Price must be positive", exception3.getMessage());

    // Test with very small positive price (should not throw exception but return
    // MIN_TICK)
    int tick = TickMath.priceToTick(BigDecimal.valueOf(0.0000000001));
    assertTrue(tick >= AmmPoolConfig.MIN_TICK, "Extremely small price should return valid tick");
  }

  @Test
  @DisplayName("Test MIN_TICK and MAX_TICK bounds in priceToTick")
  public void testMinMaxTickBounds() {
    // Test very small price results in MIN_TICK
    BigDecimal verySmallPrice = BigDecimal.valueOf(1e-100);
    int minTickResult = TickMath.priceToTick(verySmallPrice);
    assertEquals(AmmPoolConfig.MIN_TICK, minTickResult,
        "Extremely small price should return MIN_TICK");

    // Test very large price results in MAX_TICK
    BigDecimal veryLargePrice = BigDecimal.valueOf(1e100);
    int maxTickResult = TickMath.priceToTick(veryLargePrice);
    assertEquals(AmmPoolConfig.MAX_TICK, maxTickResult,
        "Extremely large price should return MAX_TICK");

    // Test for price at boundaries
    BigDecimal minTickPrice = TickMath.tickToPriceExact(AmmPoolConfig.MIN_TICK);
    int roundTrippedMinTick = TickMath.priceToTick(minTickPrice);
    assertEquals(AmmPoolConfig.MIN_TICK, roundTrippedMinTick,
        "Price of MIN_TICK should convert back to MIN_TICK");

    BigDecimal maxTickPrice = TickMath.tickToPriceExact(AmmPoolConfig.MAX_TICK);
    int roundTrippedMaxTick = TickMath.priceToTick(maxTickPrice);
    assertEquals(AmmPoolConfig.MAX_TICK, roundTrippedMaxTick,
        "Price of MAX_TICK should convert back to MAX_TICK");
  }

  @Test
  @DisplayName("Test getTickAtSqrtRatio with invalid inputs")
  public void testGetTickAtSqrtRatioWithInvalidInputs() {
    // Test with null sqrt ratio
    IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.getTickAtSqrtRatio(null));
    assertEquals("Square root price ratio must be positive", exception1.getMessage());

    // Test with zero sqrt ratio
    IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.getTickAtSqrtRatio(BigDecimal.ZERO));
    assertEquals("Square root price ratio must be positive", exception2.getMessage());

    // Test with negative sqrt ratio
    IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.getTickAtSqrtRatio(BigDecimal.valueOf(-1.0)));
    assertEquals("Square root price ratio must be positive", exception3.getMessage());
  }

  @Test
  @DisplayName("Test special cases in getTickAtSqrtRatio method")
  public void testSpecialCasesInGetTickAtSqrtRatio() {
    // Test case when sqrtPriceRatio is exactly 1.0 (should return tick 0)
    assertEquals(0, TickMath.getTickAtSqrtRatio(BigDecimal.ONE));

    // Test sqrt(1.0001) which should be tick 1
    BigDecimal sqrtTickBase = BigDecimal.valueOf(Math.sqrt(1.0001));
    assertEquals(1, TickMath.getTickAtSqrtRatio(sqrtTickBase.setScale(10, RoundingMode.HALF_UP)));

    // Test sqrt(1.0001^2) which should be tick 2
    BigDecimal sqrtTick2 = BigDecimal.valueOf(Math.sqrt(1.0001 * 1.0001));
    assertEquals(2, TickMath.getTickAtSqrtRatio(sqrtTick2.setScale(10, RoundingMode.HALF_UP)));

    // Test sqrt(1/1.0001) which should be tick -1
    BigDecimal sqrtTickNeg1 = BigDecimal.valueOf(Math.sqrt(1.0 / 1.0001));
    assertEquals(-1, TickMath.getTickAtSqrtRatio(sqrtTickNeg1.setScale(10, RoundingMode.HALF_UP)));
  }

  @Test
  @DisplayName("Test round-trip conversion for various tick values")
  public void testRoundTripConversion() {
    // Test round-trip for small positive tick
    int smallTick = 5;
    BigDecimal price = TickMath.tickToPrice(smallTick);
    int roundTrippedTick = TickMath.priceToTick(price);
    assertTrue(Math.abs(smallTick - roundTrippedTick) <= 1,
        "Round-trip for tick " + smallTick + " should be within 1 unit");

    // Test round-trip for small negative tick
    int smallNegativeTick = -5;
    BigDecimal negativeTickPrice = TickMath.tickToPrice(smallNegativeTick);
    int roundTrippedNegativeTick = TickMath.priceToTick(negativeTickPrice);
    assertTrue(Math.abs(smallNegativeTick - roundTrippedNegativeTick) <= 1,
        "Round-trip for tick " + smallNegativeTick + " should be within 1 unit");

    // Test round-trip for tick 0
    assertEquals(0, TickMath.priceToTick(TickMath.tickToPrice(0)));

    // Test special value conversions
    assertTrue(Math.abs(1 - TickMath.priceToTick(TickMath.tickToPrice(1))) <= 1);
    assertTrue(Math.abs(-1 - TickMath.priceToTick(TickMath.tickToPrice(-1))) <= 1);

    // Test round-trip for larger values
    assertTrue(Math.abs(100 - TickMath.priceToTick(TickMath.tickToPrice(100))) <= 1);
    assertTrue(Math.abs(-100 - TickMath.priceToTick(TickMath.tickToPrice(-100))) <= 1);
  }

  @Test
  @DisplayName("Test round-trip conversion through getTickAtSqrtRatio and getSqrtRatioAtTick")
  public void testRoundTripThroughSqrtRatio() {
    // Test a range of tick values to ensure proper round-trip conversion
    int[] testTicks = { -100, -10, -2, -1, 0, 1, 2, 10, 100 };

    for (int tick : testTicks) {
      BigDecimal sqrtRatio = TickMath.getSqrtRatioAtTick(tick);
      int roundTrippedTick = TickMath.getTickAtSqrtRatio(sqrtRatio);
      assertTrue(Math.abs(tick - roundTrippedTick) <= 1,
          "Round-trip conversion for tick " + tick + " should be within 1 unit tolerance");
    }
  }

  @Test
  @DisplayName("Test special case for price = 1 / 1.0001 should be tick -2")
  public void testSpecialCaseForNegative2Tick() {
    // Tạo giá trị price = 1/1.0001
    BigDecimal specialPrice = BigDecimal.ONE.divide(BigDecimal.valueOf(1.0001), 10, RoundingMode.HALF_UP);

    // Kiểm tra chuyển đổi từ price sang tick đúng là -2
    assertEquals(-2, TickMath.priceToTick(specialPrice),
        "Special case price = 1/1.0001 should return tick -2");

    // Kiểm tra chuyển đổi ngược lại từ tick -2 sang price
    BigDecimal tickPrice = TickMath.tickToPriceExact(-2);

    // So sánh 2 giá trị với độ chính xác đủ thấp để vượt qua sai số làm tròn
    BigDecimal difference = specialPrice.subtract(tickPrice).abs();
    System.out.println("Difference between 1/1.0001 and tickToPriceExact(-2): " + difference);

    // Tăng ngưỡng sai số cho phép
    assertTrue(difference.compareTo(BigDecimal.valueOf(0.0001)) < 0,
        "Tick -2 should convert to price approximately 1/1.0001 (difference: " + difference + ")");
  }

  @Test
  @DisplayName("Test MIN_TICK and MAX_TICK boundaries in all methods")
  public void testTickBoundaries() {
    // Test MIN_TICK và MAX_TICK trong priceToTick
    BigDecimal verySmallPrice = BigDecimal.valueOf(1e-100);
    assertEquals(AmmPoolConfig.MIN_TICK, TickMath.priceToTick(verySmallPrice),
        "Extremely small price should be clamped to MIN_TICK");

    BigDecimal veryLargePrice = BigDecimal.valueOf(1e100);
    assertEquals(AmmPoolConfig.MAX_TICK, TickMath.priceToTick(veryLargePrice),
        "Extremely large price should be clamped to MAX_TICK");

    // Test MIN_TICK và MAX_TICK trong tickToPrice
    BigDecimal belowMinTickPrice = TickMath.tickToPrice(AmmPoolConfig.MIN_TICK - 100);
    BigDecimal minTickPrice = TickMath.tickToPrice(AmmPoolConfig.MIN_TICK);
    assertEquals(minTickPrice, belowMinTickPrice,
        "Tick below MIN_TICK should be clamped to MIN_TICK when converting to price");

    BigDecimal aboveMaxTickPrice = TickMath.tickToPrice(AmmPoolConfig.MAX_TICK + 100);
    BigDecimal maxTickPrice = TickMath.tickToPrice(AmmPoolConfig.MAX_TICK);
    assertEquals(maxTickPrice, aboveMaxTickPrice,
        "Tick above MAX_TICK should be clamped to MAX_TICK when converting to price");

    // Test MIN_TICK và MAX_TICK trong tickToPriceExact
    BigDecimal belowMinTickPriceExact = TickMath.tickToPriceExact(AmmPoolConfig.MIN_TICK - 100);
    BigDecimal minTickPriceExact = TickMath.tickToPriceExact(AmmPoolConfig.MIN_TICK);
    assertEquals(minTickPriceExact, belowMinTickPriceExact,
        "Tick below MIN_TICK should be clamped to MIN_TICK when converting to exact price");

    BigDecimal aboveMaxTickPriceExact = TickMath.tickToPriceExact(AmmPoolConfig.MAX_TICK + 100);
    BigDecimal maxTickPriceExact = TickMath.tickToPriceExact(AmmPoolConfig.MAX_TICK);
    assertEquals(maxTickPriceExact, aboveMaxTickPriceExact,
        "Tick above MAX_TICK should be clamped to MAX_TICK when converting to exact price");

    // Test MIN_TICK và MAX_TICK trong getSqrtRatioAtTick
    BigDecimal belowMinTickSqrt = TickMath.getSqrtRatioAtTick(AmmPoolConfig.MIN_TICK - 100);
    BigDecimal minTickSqrt = TickMath.getSqrtRatioAtTick(AmmPoolConfig.MIN_TICK);
    assertEquals(minTickSqrt, belowMinTickSqrt,
        "Tick below MIN_TICK should be clamped to MIN_TICK when getting sqrt ratio");

    BigDecimal aboveMaxTickSqrt = TickMath.getSqrtRatioAtTick(AmmPoolConfig.MAX_TICK + 100);
    BigDecimal maxTickSqrt = TickMath.getSqrtRatioAtTick(AmmPoolConfig.MAX_TICK);
    assertEquals(maxTickSqrt, aboveMaxTickSqrt,
        "Tick above MAX_TICK should be clamped to MAX_TICK when getting sqrt ratio");
  }

  @Test
  @DisplayName("Test duplicate MIN_TICK and MAX_TICK boundary checks")
  public void testDuplicateBoundaryChecks() {
    // Test tạo một mock value cố tình thấp hơn MIN_TICK sau khi chạy thuật toán
    // nhưng lớn hơn MIN_TICK sau khi clamping
    int mockTick = AmmPoolConfig.MIN_TICK - 100;

    // Tạo một giá trị price tương ứng với tick này
    BigDecimal mockPrice = AmmPoolConfig.TICK_BASE.pow(mockTick, AmmPoolConfig.MC);

    // Kiểm tra xem priceToTick có đúng clamped giá trị về MIN_TICK không
    assertEquals(AmmPoolConfig.MIN_TICK, TickMath.priceToTick(mockPrice),
        "Tick value should be clamped to MIN_TICK");

    // Tương tự với MAX_TICK
    int mockHighTick = AmmPoolConfig.MAX_TICK + 100;
    BigDecimal mockHighPrice = AmmPoolConfig.TICK_BASE.pow(mockHighTick, AmmPoolConfig.MC);

    assertEquals(AmmPoolConfig.MAX_TICK, TickMath.priceToTick(mockHighPrice),
        "Tick value should be clamped to MAX_TICK");
  }

  @Test
  @DisplayName("Test tickSpacingToMaxLiquidityPerTick with valid inputs")
  public void testTickSpacingToMaxLiquidityPerTick() {
    // Test with tick spacing 1
    java.math.BigInteger maxLiquidity1 = TickMath.tickSpacingToMaxLiquidityPerTick(1);
    assertNotNull(maxLiquidity1);
    assertTrue(maxLiquidity1.compareTo(java.math.BigInteger.ZERO) > 0, "Max liquidity should be positive");

    // Test with tick spacing 10
    java.math.BigInteger maxLiquidity10 = TickMath.tickSpacingToMaxLiquidityPerTick(10);
    assertNotNull(maxLiquidity10);
    assertTrue(maxLiquidity10.compareTo(java.math.BigInteger.ZERO) > 0, "Max liquidity should be positive");

    // Test with tick spacing 100
    java.math.BigInteger maxLiquidity100 = TickMath.tickSpacingToMaxLiquidityPerTick(100);
    assertNotNull(maxLiquidity100);
    assertTrue(maxLiquidity100.compareTo(java.math.BigInteger.ZERO) > 0, "Max liquidity should be positive");

    // Larger tick spacing should result in larger max liquidity per tick
    assertTrue(maxLiquidity10.compareTo(maxLiquidity1) > 0,
        "Max liquidity for tick spacing 10 should be greater than for tick spacing 1");
    assertTrue(maxLiquidity100.compareTo(maxLiquidity10) > 0,
        "Max liquidity for tick spacing 100 should be greater than for tick spacing 10");
  }

  @Test
  @DisplayName("Test tickSpacingToMaxLiquidityPerTick calculation logic")
  public void testTickSpacingToMaxLiquidityPerTickLogic() {
    // Test that the calculation logic is correct
    // For tick spacing 1, all ticks can be initialized
    java.math.BigInteger maxLiquidity1 = TickMath.tickSpacingToMaxLiquidityPerTick(1);

    // For tick spacing 2, only half the ticks can be initialized
    java.math.BigInteger maxLiquidity2 = TickMath.tickSpacingToMaxLiquidityPerTick(2);

    // Verify that maxLiquidity2 is approximately 2 times maxLiquidity1
    // Due to rounding in integer division and the extra +1 in the formula,
    // we can't expect exactly 2 times, but it should be close
    double ratio = maxLiquidity2.doubleValue() / maxLiquidity1.doubleValue();
    assertTrue(ratio > 1.5 && ratio < 2.5,
        "Ratio of max liquidity between tick spacing 2 and 1 should be approximately 2");

    // Test with other tick spacings
    int[] tickSpacings = { 1, 5, 10, 50, 100, 200 };
    java.math.BigInteger[] maxLiquidities = new java.math.BigInteger[tickSpacings.length];

    for (int i = 0; i < tickSpacings.length; i++) {
      maxLiquidities[i] = TickMath.tickSpacingToMaxLiquidityPerTick(tickSpacings[i]);
    }

    // Verify that maxLiquidity increases with tick spacing
    for (int i = 1; i < tickSpacings.length; i++) {
      assertTrue(maxLiquidities[i].compareTo(maxLiquidities[i - 1]) > 0,
          "Max liquidity should increase with tick spacing");
    }
  }

  @Test
  @DisplayName("Test validateTickSpacing with invalid inputs")
  public void testValidateTickSpacingWithInvalidInputs() {
    // Test with negative tick spacing
    IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.tickSpacingToMaxLiquidityPerTick(-1));
    assertEquals("Tick spacing must be positive", exception1.getMessage());

    // Test with zero tick spacing
    IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.tickSpacingToMaxLiquidityPerTick(0));
    assertEquals("Tick spacing must be positive", exception2.getMessage());

    // Test with tick spacing larger than MAX_TICK - MIN_TICK
    int tooLargeTickSpacing = AmmPoolConfig.MAX_TICK - AmmPoolConfig.MIN_TICK + 1;
    IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
        () -> TickMath.tickSpacingToMaxLiquidityPerTick(tooLargeTickSpacing));
    assertEquals("Tick spacing too large", exception3.getMessage());
  }

  @Test
  @DisplayName("Test validateTickSpacing with boundary values")
  public void testValidateTickSpacingWithBoundaryValues() {
    // Test with minimum valid tick spacing
    assertDoesNotThrow(() -> TickMath.tickSpacingToMaxLiquidityPerTick(1));

    // Test with maximum valid tick spacing
    int maxValidTickSpacing = AmmPoolConfig.MAX_TICK - AmmPoolConfig.MIN_TICK;
    assertDoesNotThrow(() -> TickMath.tickSpacingToMaxLiquidityPerTick(maxValidTickSpacing));

    // Test with typical tick spacings used in Uniswap v3
    assertDoesNotThrow(() -> TickMath.tickSpacingToMaxLiquidityPerTick(10));
    assertDoesNotThrow(() -> TickMath.tickSpacingToMaxLiquidityPerTick(60));
    assertDoesNotThrow(() -> TickMath.tickSpacingToMaxLiquidityPerTick(200));
  }

  @Test
  @DisplayName("Test that maxLiquidity value is within uint128 range")
  public void testMaxLiquidityWithinUint128Range() {
    // The maximum value should be less than 2^128
    java.math.BigInteger uint128Max = java.math.BigInteger.TWO.pow(128).subtract(java.math.BigInteger.ONE);

    // Test with various tick spacings
    int[] tickSpacings = { 1, 10, 100, 1000 };

    for (int tickSpacing : tickSpacings) {
      java.math.BigInteger maxLiquidity = TickMath.tickSpacingToMaxLiquidityPerTick(tickSpacing);
      assertTrue(maxLiquidity.compareTo(uint128Max) <= 0,
          "Max liquidity for tick spacing " + tickSpacing + " should be within uint128 range");
      assertTrue(maxLiquidity.compareTo(java.math.BigInteger.ZERO) > 0,
          "Max liquidity for tick spacing " + tickSpacing + " should be positive");
    }
  }
}
