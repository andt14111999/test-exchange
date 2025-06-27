package com.exchangeengine.util.ammPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class for AmmPoolConfig
 */
public class AmmPoolConfigTest {

  @Test
  @DisplayName("Test constants in AmmPoolConfig")
  public void testConstants() {
    // Verify decimal scale constants
    assertEquals(20, AmmPoolConfig.DECIMAL_SCALE);
    assertEquals(6, AmmPoolConfig.DISPLAY_SCALE);

    // Verify rounding mode
    assertEquals(RoundingMode.HALF_UP, AmmPoolConfig.ROUNDING_MODE);

    // Verify MathContext
    assertEquals(new MathContext(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE), AmmPoolConfig.MC);

    // Verify tick base
    assertEquals(BigDecimal.valueOf(1.0001), AmmPoolConfig.TICK_BASE);

    // Verify min and max tick
    assertEquals(-887272, AmmPoolConfig.MIN_TICK);
    assertEquals(887272, AmmPoolConfig.MAX_TICK);

    // Verify slippage values
    assertEquals(BigDecimal.valueOf(100.0), AmmPoolConfig.MAX_SLIPPAGE);
    assertEquals(BigDecimal.valueOf(0.01), AmmPoolConfig.MIN_SLIPPAGE);

    assertEquals(BigDecimal.valueOf(5.0), AmmPoolConfig.DEFAULT_SLIPPAGE);

    // Verify minimum liquidity
    assertEquals(new BigDecimal("0.01"), AmmPoolConfig.MIN_LIQUIDITY);

    // Verify max liquidity per tick
    assertEquals(new BigDecimal("340282366920938463463374607431768211455"), AmmPoolConfig.MAX_LIQUIDITY_PER_TICK);
  }

  @Test
  @DisplayName("Test isValidTick method")
  public void testIsValidTick() {
    // Test valid ticks
    assertTrue(AmmPoolConfig.isValidTick(0));
    assertTrue(AmmPoolConfig.isValidTick(100));
    assertTrue(AmmPoolConfig.isValidTick(-100));
    assertTrue(AmmPoolConfig.isValidTick(AmmPoolConfig.MIN_TICK));
    assertTrue(AmmPoolConfig.isValidTick(AmmPoolConfig.MAX_TICK));

    // Test invalid ticks
    assertFalse(AmmPoolConfig.isValidTick(AmmPoolConfig.MIN_TICK - 1));
    assertFalse(AmmPoolConfig.isValidTick(AmmPoolConfig.MAX_TICK + 1));
  }

  @Test
  @DisplayName("Test validateTick method")
  public void testValidateTick() {
    // Test valid ticks
    assertEquals("", AmmPoolConfig.validateTick(0));
    assertEquals("", AmmPoolConfig.validateTick(AmmPoolConfig.MIN_TICK));
    assertEquals("", AmmPoolConfig.validateTick(AmmPoolConfig.MAX_TICK));

    // Test invalid ticks
    String expectedError = "Tick must be between " + AmmPoolConfig.MIN_TICK + " and " + AmmPoolConfig.MAX_TICK;
    assertEquals(expectedError, AmmPoolConfig.validateTick(AmmPoolConfig.MIN_TICK - 1));
    assertEquals(expectedError, AmmPoolConfig.validateTick(AmmPoolConfig.MAX_TICK + 1));
  }

  @Test
  @DisplayName("Test calculateMaxLiquidityPerTick method")
  public void testCalculateMaxLiquidityPerTick() {
    // Test with tickSpacing = 0
    assertEquals(AmmPoolConfig.MAX_LIQUIDITY_PER_TICK, AmmPoolConfig.calculateMaxLiquidityPerTick(0));

    // Test with negative tickSpacing
    assertEquals(AmmPoolConfig.MAX_LIQUIDITY_PER_TICK, AmmPoolConfig.calculateMaxLiquidityPerTick(-1));

    // Test with positive tickSpacing
    int tickSpacing = 10;
    int minTick = (AmmPoolConfig.MIN_TICK / tickSpacing) * tickSpacing;
    int maxTick = (AmmPoolConfig.MAX_TICK / tickSpacing) * tickSpacing;
    int numTicks = ((maxTick - minTick) / tickSpacing) + 1;

    BigDecimal expected = AmmPoolConfig.MAX_LIQUIDITY_PER_TICK.divide(new BigDecimal(numTicks), AmmPoolConfig.MC);
    assertEquals(expected, AmmPoolConfig.calculateMaxLiquidityPerTick(tickSpacing));
  }

  @Test
  @DisplayName("Test MAX_TICK and MIN_TICK relationship")
  public void testMinMaxTickRelationship() {
    // Verify that MIN_TICK and MAX_TICK are symmetrical
    assertEquals(AmmPoolConfig.MIN_TICK, -AmmPoolConfig.MAX_TICK);

    // Verify that MIN_TICK and MAX_TICK are related to prices in the expected way
    BigDecimal minTickPrice = AmmPoolConfig.TICK_BASE.pow(AmmPoolConfig.MIN_TICK, AmmPoolConfig.MC);
    BigDecimal maxTickPrice = AmmPoolConfig.TICK_BASE.pow(AmmPoolConfig.MAX_TICK, AmmPoolConfig.MC);

    // MIN_TICK should correspond to a very small price
    assertTrue(minTickPrice.compareTo(BigDecimal.valueOf(0.01)) < 0);

    // MAX_TICK should correspond to a very large price
    assertTrue(maxTickPrice.compareTo(BigDecimal.valueOf(100)) > 0);

    // Product of min and max prices should be approximately 1 (with some rounding
    // error)
    BigDecimal product = minTickPrice.multiply(maxTickPrice, AmmPoolConfig.MC);
    assertTrue(product.subtract(BigDecimal.ONE).abs().compareTo(BigDecimal.valueOf(0.01)) < 0);
  }
}
