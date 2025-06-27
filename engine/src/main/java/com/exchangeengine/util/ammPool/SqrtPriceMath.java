package com.exchangeengine.util.ammPool;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Các phương thức tính toán giá và số lượng token dựa trên căn bậc hai của giá
 * Chuyển đổi từ SqrtPriceMath.sol của Uniswap V3
 */
public class SqrtPriceMath {
  private SqrtPriceMath() {
    // Private constructor để ngăn khởi tạo
  }

  /**
   * Tính toán lượng token0 dựa trên sự thay đổi của căn bậc hai giá
   *
   * @param sqrtRatioA Giá trị căn bậc hai ban đầu
   * @param sqrtRatioB Giá trị căn bậc hai sau khi thay đổi
   * @param liquidity  Thanh khoản
   * @param roundUp    Làm tròn lên hay xuống
   * @return Lượng token0
   */
  public static BigDecimal getAmount0Delta(
      BigDecimal sqrtRatioA,
      BigDecimal sqrtRatioB,
      BigDecimal liquidity,
      boolean roundUp) {

    BigDecimal result;
    if (sqrtRatioA.compareTo(sqrtRatioB) > 0) {
      BigDecimal sqrtRatioATemp = sqrtRatioA;
      sqrtRatioA = sqrtRatioB;
      sqrtRatioB = sqrtRatioATemp;
    }

    // Công thức: Δx = L * (sqrtPriceB - sqrtPriceA) / (sqrtPriceA * sqrtPriceB)
    BigDecimal numerator = liquidity.multiply(sqrtRatioB.subtract(sqrtRatioA));
    BigDecimal denominator = sqrtRatioA.multiply(sqrtRatioB);

    RoundingMode roundingMode = roundUp ? RoundingMode.CEILING : RoundingMode.FLOOR;
    result = numerator.divide(denominator, AmmPoolConfig.MC).setScale(AmmPoolConfig.DECIMAL_SCALE, roundingMode);

    return result;
  }

  /**
   * Tính toán lượng token1 dựa trên sự thay đổi của căn bậc hai giá
   *
   * @param sqrtRatioA Giá trị căn bậc hai ban đầu
   * @param sqrtRatioB Giá trị căn bậc hai sau khi thay đổi
   * @param liquidity  Thanh khoản
   * @param roundUp    Làm tròn lên hay xuống
   * @return Lượng token1
   */
  public static BigDecimal getAmount1Delta(
      BigDecimal sqrtRatioA,
      BigDecimal sqrtRatioB,
      BigDecimal liquidity,
      boolean roundUp) {

    BigDecimal result;
    if (sqrtRatioA.compareTo(sqrtRatioB) > 0) {
      BigDecimal sqrtRatioATemp = sqrtRatioA;
      sqrtRatioA = sqrtRatioB;
      sqrtRatioB = sqrtRatioATemp;
    }

    // Công thức: Δy = L * (sqrtPriceB - sqrtPriceA)
    result = liquidity.multiply(sqrtRatioB.subtract(sqrtRatioA));

    RoundingMode roundingMode = roundUp ? RoundingMode.CEILING : RoundingMode.FLOOR;
    result = result.setScale(AmmPoolConfig.DECIMAL_SCALE, roundingMode);

    return result;
  }

  /**
   * Tính toán giá tiếp theo dựa trên input
   *
   * @param sqrtPrice  Giá hiện tại
   * @param liquidity  Thanh khoản
   * @param amountIn   Lượng token đầu vào
   * @param zeroForOne Swap token0 -> token1 (true) hoặc token1 -> token0
   *                   (false)
   * @return Giá căn bậc hai tiếp theo
   */
  public static BigDecimal getNextSqrtPriceFromInput(
      BigDecimal sqrtPrice,
      BigDecimal liquidity,
      BigDecimal amountIn,
      boolean zeroForOne) {

    if (amountIn.compareTo(BigDecimal.ZERO) == 0)
      return sqrtPrice;
    if (liquidity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Liquidity must be positive");
    }

    BigDecimal result;
    if (zeroForOne) {
      BigDecimal numerator = liquidity.multiply(sqrtPrice);
      BigDecimal product = amountIn.multiply(sqrtPrice);
      BigDecimal denominator = liquidity.add(product);
      result = numerator.divide(denominator, AmmPoolConfig.MC);
    } else {
      BigDecimal quotient = amountIn.divide(liquidity, AmmPoolConfig.MC);
      result = sqrtPrice.add(quotient);
    }

    return result;
  }

  /**
   * Tính toán giá tiếp theo dựa trên output
   *
   * @param sqrtPrice  Giá hiện tại
   * @param liquidity  Thanh khoản
   * @param amountOut  Lượng token đầu ra
   * @param zeroForOne Swap token0 -> token1 (true) hoặc token1 -> token0
   *                   (false)
   * @return Giá căn bậc hai tiếp theo
   */
  public static BigDecimal getNextSqrtPriceFromOutput(
      BigDecimal sqrtPrice,
      BigDecimal liquidity,
      BigDecimal amountOut,
      boolean zeroForOne) {

    if (amountOut.compareTo(BigDecimal.ZERO) == 0)
      return sqrtPrice;
    if (liquidity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Liquidity must be positive");
    }

    BigDecimal result;
    if (zeroForOne) {
      BigDecimal quotient = amountOut.divide(liquidity, AmmPoolConfig.MC);
      // Làm tròn lên
      quotient = quotient.setScale(AmmPoolConfig.DECIMAL_SCALE, RoundingMode.CEILING);

      result = sqrtPrice.add(quotient);
    } else {
      BigDecimal numerator = liquidity.multiply(sqrtPrice);
      BigDecimal product = liquidity.subtract(amountOut);
      // Làm tròn xuống
      product = product.setScale(AmmPoolConfig.DECIMAL_SCALE, RoundingMode.FLOOR);
      BigDecimal denominator = product;
      result = numerator.divide(denominator, AmmPoolConfig.MC);
    }

    return result;
  }
}
