package com.exchangeengine.util.ammPool;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class AmmPoolConfig {

  private AmmPoolConfig() {
  }

  public static final int DECIMAL_SCALE = 20;
  public static final int DISPLAY_SCALE = 6;
  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
  public static final MathContext MC = new MathContext(DECIMAL_SCALE, ROUNDING_MODE);
  public static final BigDecimal TICK_BASE = BigDecimal.valueOf(1.0001);
  public static final int MIN_TICK = -887272;
  public static final int MAX_TICK = 887272;
  public static final BigDecimal MAX_SLIPPAGE = BigDecimal.valueOf(100.0); // 100%
  public static final BigDecimal MIN_SLIPPAGE = BigDecimal.valueOf(0.01); // 0.01%
  public static final BigDecimal DEFAULT_SLIPPAGE = BigDecimal.valueOf(5.0);
  public static final BigDecimal MIN_LIQUIDITY = new BigDecimal("0.01");
  public static final BigDecimal MAX_LIQUIDITY_PER_TICK = new BigDecimal("340282366920938463463374607431768211455");

  public static boolean isValidTick(int tick) {
    return tick >= MIN_TICK && tick <= MAX_TICK;
  }

  public static String validateTick(int tick) {
    if (isValidTick(tick)) {
      return "";
    }
    return "Tick must be between " + MIN_TICK + " and " + MAX_TICK;
  }

  /**
   * Tính toán giá trị liquidity tối đa cho mỗi tick dựa trên khoảng cách tick
   * Dựa trên hàm tickSpacingToMaxLiquidityPerTick từ Uniswap V3
   *
   * @param tickSpacing Khoảng cách giữa các tick
   * @return Giá trị liquidity tối đa cho mỗi tick
   */
  public static BigDecimal calculateMaxLiquidityPerTick(int tickSpacing) {
    if (tickSpacing <= 0) {
      return MAX_LIQUIDITY_PER_TICK;
    }

    int minTick = (MIN_TICK / tickSpacing) * tickSpacing;
    int maxTick = (MAX_TICK / tickSpacing) * tickSpacing;
    int numTicks = ((maxTick - minTick) / tickSpacing) + 1;

    // Chia giá trị uint128.max cho số lượng tick
    return MAX_LIQUIDITY_PER_TICK.divide(new BigDecimal(numTicks), MC);
  }
}
