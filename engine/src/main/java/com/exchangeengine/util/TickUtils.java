package com.exchangeengine.util;

import java.math.BigDecimal;

import com.exchangeengine.model.Tick;
import com.exchangeengine.model.TickBitmap;

/**
 * Utility class for tick-related operations
 */
public class TickUtils {

  private TickUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Updates the tick bitmap based on the liquidity change in a tick
   *
   * @param tickBitmap           The bitmap to update
   * @param tickIndex            The index of the tick in the bitmap
   * @param liquidityGrossAfter  The liquidity after update
   * @param liquidityGrossBefore The liquidity before update
   * @return true if the bitmap was changed, false otherwise
   */
  public static boolean updateTickBitmapIfFlipped(
      TickBitmap tickBitmap,
      int tickIndex,
      BigDecimal liquidityGrossAfter,
      BigDecimal liquidityGrossBefore) {

    // Check if the tick flipped its initialized state
    boolean flipped = Tick.isFlipped(liquidityGrossAfter, liquidityGrossBefore);

    if (flipped) {
      // Determine the new initialized state based on the liquidity after update
      boolean initialized = liquidityGrossAfter.compareTo(BigDecimal.ZERO) > 0;

      // Update the bitmap and return whether it changed
      return tickBitmap.flipBit(tickIndex, flipped, initialized);
    }

    return false;
  }
}
