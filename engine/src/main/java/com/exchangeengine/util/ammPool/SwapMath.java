package com.exchangeengine.util.ammPool;

import java.math.BigDecimal;

/**
 * Lớp tiện ích tính toán kết quả của swap trong một tick
 * Chuyển đổi từ Uniswap V3 SwapMath.sol sang Java
 */
public class SwapMath {

  private SwapMath() {
    // Private constructor để ngăn khởi tạo
  }

  /**
   * Tính toán kết quả của việc swap một lượng token vào hoặc ra, dựa trên tham số
   * của swap.
   *
   * @param sqrtRatioCurrent Giá hiện tại của pool dưới dạng căn bậc hai
   * @param sqrtRatioTarget  Giá mục tiêu không được vượt quá
   * @param liquidity        Thanh khoản khả dụng
   * @param amountRemaining  Lượng token còn lại cần swap (dương: exactIn, âm:
   *                         exactOut)
   * @param feePercentage    Phí giao dịch, tính bằng phần trăm (ví dụ: 0.01 = 1%)
   * @return Mảng gồm [sqrtRatioNext, amountIn, amountOut, feeAmount]
   */
  public static BigDecimal[] computeSwapStep(
      BigDecimal sqrtRatioCurrent,
      BigDecimal sqrtRatioTarget,
      BigDecimal liquidity,
      BigDecimal amountRemaining,
      double feePercentage) {

    boolean zeroForOne = sqrtRatioCurrent.compareTo(sqrtRatioTarget) >= 0;
    boolean exactIn = amountRemaining.signum() >= 0;

    BigDecimal sqrtRatioNext = BigDecimal.ZERO;
    BigDecimal amountIn = BigDecimal.ZERO;
    BigDecimal amountOut = BigDecimal.ZERO;
    BigDecimal feeAmount = BigDecimal.ZERO;

    if (exactIn) {
      BigDecimal oneMinusFee = BigDecimal.ONE.subtract(BigDecimal.valueOf(feePercentage))
          .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

      BigDecimal amountRemainingLessFee = amountRemaining.multiply(oneMinusFee);

      if (zeroForOne) {
        amountIn = SqrtPriceMath.getAmount0Delta(
            sqrtRatioTarget,
            sqrtRatioCurrent,
            liquidity,
            true);
      } else {
        amountIn = SqrtPriceMath.getAmount1Delta(
            sqrtRatioCurrent,
            sqrtRatioTarget,
            liquidity,
            true);
      }

      if (amountRemainingLessFee.compareTo(amountIn) >= 0) {
        sqrtRatioNext = sqrtRatioTarget;
      } else {
        sqrtRatioNext = SqrtPriceMath.getNextSqrtPriceFromInput(
            sqrtRatioCurrent,
            liquidity,
            amountRemainingLessFee,
            zeroForOne);
      }
    } else {
      if (zeroForOne) {
        amountOut = SqrtPriceMath.getAmount1Delta(
            sqrtRatioTarget,
            sqrtRatioCurrent,
            liquidity,
            false);
      } else {
        amountOut = SqrtPriceMath.getAmount0Delta(
            sqrtRatioCurrent,
            sqrtRatioTarget,
            liquidity,
            false);
      }

      if (amountRemaining.abs().compareTo(amountOut) >= 0) {
        sqrtRatioNext = sqrtRatioTarget;
      } else {
        sqrtRatioNext = SqrtPriceMath.getNextSqrtPriceFromOutput(
            sqrtRatioCurrent,
            liquidity,
            amountRemaining.abs(),
            zeroForOne);
      }
    }

    boolean max = sqrtRatioTarget.compareTo(sqrtRatioNext) == 0;

    // Tính toán lượng token vào/ra
    if (zeroForOne) {
      if (max && exactIn) {
        // Giữ nguyên giá trị amountIn đã tính
      } else {
        amountIn = SqrtPriceMath.getAmount0Delta(
            sqrtRatioNext,
            sqrtRatioCurrent,
            liquidity,
            true);
      }

      if (max && !exactIn) {
        // Giữ nguyên giá trị amountOut đã tính
      } else {
        amountOut = SqrtPriceMath.getAmount1Delta(
            sqrtRatioNext,
            sqrtRatioCurrent,
            liquidity,
            false);
      }
    } else {
      if (max && exactIn) {
        // Giữ nguyên giá trị amountIn đã tính
      } else {
        amountIn = SqrtPriceMath.getAmount1Delta(
            sqrtRatioCurrent,
            sqrtRatioNext,
            liquidity,
            true);
      }

      if (max && !exactIn) {
        // Giữ nguyên giá trị amountOut đã tính
      } else {
        amountOut = SqrtPriceMath.getAmount0Delta(
            sqrtRatioCurrent,
            sqrtRatioNext,
            liquidity,
            false);
      }
    }

    // Hạn chế lượng đầu ra không vượt quá lượng còn lại
    if (!exactIn && amountOut.compareTo(amountRemaining.abs()) >= 0) {
      amountOut = amountRemaining.abs();
    }

    // Tính phí giao dịch
    if (exactIn && sqrtRatioNext.compareTo(sqrtRatioTarget) != 0) {
      // Không đạt đến giá mục tiêu, lấy phần còn lại làm phí
      feeAmount = amountRemaining.subtract(amountIn);
    } else {
      BigDecimal feeMultiplier = BigDecimal.valueOf(feePercentage)
          .divide(BigDecimal.ONE.subtract(BigDecimal.valueOf(feePercentage)), AmmPoolConfig.DECIMAL_SCALE,
              AmmPoolConfig.ROUNDING_MODE);
      feeAmount = amountIn.multiply(feeMultiplier)
          .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    }

    return new BigDecimal[] { sqrtRatioNext, amountIn, amountOut, feeAmount };
  }

  /**
   * Kiểm tra xem kết quả swap có nằm trong giới hạn slippage cho phép không.
   *
   * @param amount0        Số lượng token0 trong swap
   * @param amount1        Số lượng token1 trong swap
   * @param estimateAmount Số lượng token dự kiến ban đầu
   * @param zeroForOne     Hướng swap (true: token0 -> token1, false: token1 ->
   *                       token0)
   * @param exactInput     Loại swap (true: exactInput, false: exactOutput)
   * @param slippage       Tỷ lệ trượt giá chấp nhận được (ví dụ: 0.01 = 1%)
   * @return true nếu nằm trong giới hạn slippage, false nếu vượt quá giới hạn
   */
  public static boolean checkSlippage(BigDecimal amount0, BigDecimal amount1, BigDecimal estimateAmount,
      boolean zeroForOne, boolean exactInput, BigDecimal slippage) {

    // Nếu không có slippage (null hoặc 0 hoặc 1), cho phép mọi swap
    if (slippage == null || slippage.compareTo(BigDecimal.ZERO) == 0 || slippage.compareTo(BigDecimal.ONE) == 0) {
      return true;
    }

    if (amount0.compareTo(BigDecimal.ZERO) <= 0 || amount1.compareTo(BigDecimal.ZERO) <= 0 ||
        estimateAmount.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }

    // Tính toán hệ số slippage
    BigDecimal slippageFactor = BigDecimal.ONE.subtract(slippage);

    // Lấy giá trị thực tế (thực tế nhận được hoặc thực tế phải trả)
    BigDecimal actualAmount;

    if (exactInput) {
      // Đối với exactInput, actualAmount là lượng token đầu ra
      actualAmount = zeroForOne ? amount1 : amount0;

      // Tính lượng token đầu ra tối thiểu có thể chấp nhận
      BigDecimal minimumAcceptableAmount = estimateAmount.multiply(slippageFactor)
          .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

      // Kiểm tra xem lượng token đầu ra thực tế có đủ không
      return actualAmount.compareTo(minimumAcceptableAmount) >= 0;
    } else {
      // Đối với exactOutput, actualAmount là lượng token đầu vào
      actualAmount = zeroForOne ? amount0 : amount1;

      // Tính lượng token đầu vào tối đa có thể chấp nhận
      BigDecimal maximumAcceptableAmount = estimateAmount.divide(slippageFactor,
          AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

      // Kiểm tra xem lượng token đầu vào thực tế có quá lớn không
      return actualAmount.compareTo(maximumAcceptableAmount) <= 0;
    }
  }

  /**
   * Tính toán số lượng token dự kiến dựa vào giá ban đầu và các thông số giao
   * dịch.
   *
   * @param amountSpecified  Số lượng token được chỉ định (input hoặc output)
   * @param initialSqrtPrice Giá căn bậc hai ban đầu của pool
   * @param feePercentage    Tỷ lệ phí giao dịch (ví dụ: 0.003 = 0.3%)
   * @param zeroForOne       Hướng swap (true: token0 -> token1, false: token1 ->
   *                         token0)
   * @param exactInput       Loại swap (true: exactInput, false: exactOutput)
   * @return Số lượng token dự kiến (output nếu exactInput=true, input nếu
   *         exactInput=false)
   */
  public static BigDecimal calculateEstimateAmount(
      BigDecimal amountSpecified,
      BigDecimal initialSqrtPrice,
      double feePercentage,
      boolean zeroForOne,
      boolean exactInput) {

    // Tính toán giá ban đầu
    BigDecimal initialPrice = initialSqrtPrice.multiply(initialSqrtPrice)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    // Tính phí giao dịch
    BigDecimal oneMinusFee = BigDecimal.ONE.subtract(BigDecimal.valueOf(feePercentage));

    // Tính giá trị dự kiến dựa trên loại swap
    BigDecimal estimateAmount;

    if (exactInput) {
      // Tính lượng token đầu ra dự kiến
      if (zeroForOne) {
        // token0 -> token1: estimateAmount = input * price * (1 - fee)
        estimateAmount = amountSpecified.multiply(initialPrice).multiply(oneMinusFee)
            .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
      } else {
        // token1 -> token0: estimateAmount = input / price * (1 - fee)
        estimateAmount = amountSpecified.divide(initialPrice, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE)
            .multiply(oneMinusFee).setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
      }
    } else {
      // Tính lượng token đầu vào dự kiến
      if (zeroForOne) {
        // token0 -> token1: estimateAmount = output / price / (1 - fee)
        estimateAmount = amountSpecified.divide(initialPrice, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE)
            .divide(oneMinusFee, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
      } else {
        // token1 -> token0: estimateAmount = output * price / (1 - fee)
        estimateAmount = amountSpecified.multiply(initialPrice)
            .divide(oneMinusFee, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
      }
    }

    return estimateAmount;
  }
}
