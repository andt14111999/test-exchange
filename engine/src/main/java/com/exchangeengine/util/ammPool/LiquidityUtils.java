package com.exchangeengine.util.ammPool;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.model.Tick;

/**
 * Lớp tiện ích cung cấp các phương thức tính toán liên quan đến thanh khoản AMM
 */
public class LiquidityUtils {
  private static final Logger logger = LoggerFactory.getLogger(LiquidityUtils.class);

  private LiquidityUtils() {
    // Private constructor để ngăn khởi tạo
  }

  /**
   * Tính toán thanh khoản từ số lượng token và các giá trị tick
   *
   * @param pool     AmmPool chứa thông tin tick hiện tại
   * @param position AmmPosition chứa thông tin token và tick
   * @return BigDecimal Thanh khoản tính toán được
   */
  public static BigDecimal calculateLiquidity(AmmPool pool, AmmPosition position) {
    try {
      // Lấy các giá trị sqrt price cho các tick
      BigDecimal sqrtRatioCurrentTick = TickMath.getSqrtRatioAtTick(pool.getCurrentTick());
      BigDecimal sqrtRatioLowerTick = TickMath.getSqrtRatioAtTick(position.getTickLowerIndex());
      BigDecimal sqrtRatioUpperTick = TickMath.getSqrtRatioAtTick(position.getTickUpperIndex());

      BigDecimal amount0 = position.getAmount0Initial();
      BigDecimal amount1 = position.getAmount1Initial();

      return calculateLiquidityForAmounts(
          sqrtRatioCurrentTick,
          sqrtRatioLowerTick,
          sqrtRatioUpperTick,
          amount0,
          amount1);
    } catch (Exception e) {
      logger.error("Error calculating liquidity: {}", e.getMessage(), e);
      return BigDecimal.ZERO;
    }
  }

  /**
   * Tính toán thanh khoản từ số lượng token và các giá trị tick
   * Dựa trên phương pháp tính của Uniswap v3
   *
   * @param sqrtRatioCurrentTick Căn bậc hai của giá ở tick hiện tại
   * @param sqrtRatioLowerTick   Căn bậc hai của giá ở tick dưới
   * @param sqrtRatioUpperTick   Căn bậc hai của giá ở tick trên
   * @param amount0              Số lượng token 0
   * @param amount1              Số lượng token 1
   * @return BigDecimal Thanh khoản tính toán được
   */
  public static BigDecimal calculateLiquidityForAmounts(
      BigDecimal sqrtRatioCurrentTick,
      BigDecimal sqrtRatioLowerTick,
      BigDecimal sqrtRatioUpperTick,
      BigDecimal amount0,
      BigDecimal amount1) {
    try {
      // Nếu tick hiện tại <= tick dưới
      if (sqrtRatioCurrentTick.compareTo(sqrtRatioLowerTick) <= 0) {
        // Chỉ sử dụng token 0
        return amount0
            .multiply(sqrtRatioLowerTick)
            .multiply(sqrtRatioUpperTick)
            .divide(sqrtRatioUpperTick.subtract(sqrtRatioLowerTick), AmmPoolConfig.DECIMAL_SCALE,
                AmmPoolConfig.ROUNDING_MODE);
      }

      // Nếu tick hiện tại >= tick trên
      if (sqrtRatioCurrentTick.compareTo(sqrtRatioUpperTick) >= 0) {
        // Chỉ sử dụng token 1
        return amount1
            .divide(sqrtRatioUpperTick.subtract(sqrtRatioLowerTick), AmmPoolConfig.DECIMAL_SCALE,
                AmmPoolConfig.ROUNDING_MODE);
      }

      // Trường hợp tick hiện tại nằm giữa tick dưới và tick trên
      // Tính toán thanh khoản từ token 0
      BigDecimal liquidity0 = amount0
          .multiply(sqrtRatioCurrentTick)
          .multiply(sqrtRatioUpperTick)
          .divide(sqrtRatioUpperTick.subtract(sqrtRatioCurrentTick), AmmPoolConfig.DECIMAL_SCALE,
              AmmPoolConfig.ROUNDING_MODE);

      // Tính toán thanh khoản từ token 1
      BigDecimal liquidity1 = amount1
          .divide(sqrtRatioCurrentTick.subtract(sqrtRatioLowerTick), AmmPoolConfig.DECIMAL_SCALE,
              AmmPoolConfig.ROUNDING_MODE);

      // Lấy giá trị nhỏ hơn để đảm bảo đủ cả 2 token
      return liquidity0.min(liquidity1);
    } catch (Exception e) {
      logger.error("Error in calculateLiquidityForAmounts: {}", e.getMessage(), e);
      return BigDecimal.ZERO;
    }
  }

  /**
   * Tính toán số lượng token từ thanh khoản và các giá trị tick
   *
   * @param sqrtRatioCurrentTick Căn bậc hai của giá ở tick hiện tại
   * @param sqrtRatioLowerTick   Căn bậc hai của giá ở tick dưới
   * @param sqrtRatioUpperTick   Căn bậc hai của giá ở tick trên
   * @param liquidity            Thanh khoản
   * @return BigDecimal[] Mảng 2 phần tử chứa [amount0, amount1]
   */
  public static BigDecimal[] getAmountsForLiquidity(
      BigDecimal sqrtRatioCurrentTick,
      BigDecimal sqrtRatioLowerTick,
      BigDecimal sqrtRatioUpperTick,
      BigDecimal liquidity) {
    try {
      BigDecimal amount0 = BigDecimal.ZERO;
      BigDecimal amount1 = BigDecimal.ZERO;

      // Nếu tick hiện tại <= tick dưới
      if (sqrtRatioCurrentTick.compareTo(sqrtRatioLowerTick) <= 0) {
        // Chỉ tính token 0
        amount0 = liquidity
            .multiply(sqrtRatioUpperTick.subtract(sqrtRatioLowerTick))
            .divide(sqrtRatioLowerTick.multiply(sqrtRatioUpperTick), AmmPoolConfig.DECIMAL_SCALE,
                AmmPoolConfig.ROUNDING_MODE);
      }
      // Nếu tick hiện tại >= tick trên
      else if (sqrtRatioCurrentTick.compareTo(sqrtRatioUpperTick) >= 0) {
        // Chỉ tính token 1
        amount1 = liquidity
            .multiply(sqrtRatioUpperTick.subtract(sqrtRatioLowerTick));
      }
      // Trường hợp tick hiện tại nằm giữa tick dưới và tick trên
      else {
        // Tính token 0
        amount0 = liquidity
            .multiply(sqrtRatioUpperTick.subtract(sqrtRatioCurrentTick))
            .divide(sqrtRatioCurrentTick.multiply(sqrtRatioUpperTick), AmmPoolConfig.DECIMAL_SCALE,
                AmmPoolConfig.ROUNDING_MODE);

        // Tính token 1
        amount1 = liquidity
            .multiply(sqrtRatioCurrentTick.subtract(sqrtRatioLowerTick));
      }

      return new BigDecimal[] { amount0, amount1 };
    } catch (Exception e) {
      logger.error("Error in getAmountsForLiquidity: {}", e.getMessage(), e);
      return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
    }
  }

  /**
   * Tính toán phí dựa trên thanh khoản
   *
   * @param position   Vị thế AMM
   * @param pool       Pool AMM
   * @param timePeriod Thời gian tính phí (milliseconds)
   * @return BigDecimal[] Mảng 2 phần tử chứa [fee0, fee1]
   */
  public static BigDecimal[] calculateFees(AmmPosition position, AmmPool pool, long timePeriod) {
    try {
      // Tính toán phí dựa trên thanh khoản, thời gian, và tỷ lệ phí
      BigDecimal liquidity = position.getLiquidity();
      BigDecimal feeRate = BigDecimal.valueOf(pool.getFeePercentage());
      BigDecimal volume = liquidity.multiply(BigDecimal.valueOf(0.1)); // Giả định khối lượng giao dịch
      BigDecimal timeInDays = BigDecimal.valueOf(timePeriod)
          .divide(BigDecimal.valueOf(86400000), AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE); // Chuyển ms
                                                                                                           // thành ngày

      // Fees = Volume * FeeRate * TimeInDays
      BigDecimal fee = volume.multiply(feeRate).multiply(timeInDays);
      BigDecimal fee0 = fee.divide(BigDecimal.valueOf(2), AmmPoolConfig.ROUNDING_MODE);
      BigDecimal fee1 = fee.divide(BigDecimal.valueOf(2), AmmPoolConfig.ROUNDING_MODE);

      return new BigDecimal[] { fee0, fee1 };
    } catch (Exception e) {
      logger.error("Error calculating fees: {}", e.getMessage(), e);
      return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
    }
  }

  /**
   * Tính toán tăng trưởng phí bên trong phạm vi tick
   *
   * @param lowerTick        tick ranh giới dưới
   * @param upperTick        tick ranh giới trên
   * @param currentTick      tick hiện tại của pool
   * @param feeGrowthGlobal0 tăng trưởng phí toàn cục cho token0
   * @param feeGrowthGlobal1 tăng trưởng phí toàn cục cho token1
   * @return mảng [feeGrowthInside0, feeGrowthInside1]
   */
  public static BigDecimal[] getFeeGrowthInside(
      Tick lowerTick,
      Tick upperTick,
      int currentTick,
      BigDecimal feeGrowthGlobal0,
      BigDecimal feeGrowthGlobal1) {
    try {
      BigDecimal feeGrowthBelow0;
      BigDecimal feeGrowthBelow1;

      // Tính toán tăng trưởng phí bên dưới
      if (currentTick >= lowerTick.getTickIndex()) {
        feeGrowthBelow0 = lowerTick.getFeeGrowthOutside0();
        feeGrowthBelow1 = lowerTick.getFeeGrowthOutside1();
      } else {
        feeGrowthBelow0 = feeGrowthGlobal0.subtract(lowerTick.getFeeGrowthOutside0());
        feeGrowthBelow1 = feeGrowthGlobal1.subtract(lowerTick.getFeeGrowthOutside1());
      }

      BigDecimal feeGrowthAbove0;
      BigDecimal feeGrowthAbove1;

      // Tính toán tăng trưởng phí bên trên
      if (currentTick < upperTick.getTickIndex()) {
        feeGrowthAbove0 = upperTick.getFeeGrowthOutside0();
        feeGrowthAbove1 = upperTick.getFeeGrowthOutside1();
      } else {
        feeGrowthAbove0 = feeGrowthGlobal0.subtract(upperTick.getFeeGrowthOutside0());
        feeGrowthAbove1 = feeGrowthGlobal1.subtract(upperTick.getFeeGrowthOutside1());
      }

      // Tính toán feeGrowthInside theo công thức của Uniswap V3
      BigDecimal feeGrowthInside0 = feeGrowthGlobal0.subtract(feeGrowthBelow0).subtract(feeGrowthAbove0);
      BigDecimal feeGrowthInside1 = feeGrowthGlobal1.subtract(feeGrowthBelow1).subtract(feeGrowthAbove1);

      // Đảm bảo feeGrowthInside không âm - trong Uniswap V3, feeGrowth là uint256 nên
      // không thể âm
      // Trong Java, chúng ta sử dụng BigDecimal nên cần đảm bảo giá trị không âm
      feeGrowthInside0 = feeGrowthInside0.max(BigDecimal.ZERO);
      feeGrowthInside1 = feeGrowthInside1.max(BigDecimal.ZERO);

      return new BigDecimal[] { feeGrowthInside0, feeGrowthInside1 };
    } catch (Exception e) {
      logger.error("Error calculating fee growth inside: {}", e.getMessage(), e);
      return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
    }
  }

  /**
   * Tính toán phí đã tích lũy dựa trên feeGrowth
   *
   * @param liquidity           Thanh khoản của vị thế
   * @param feeGrowthInside     Giá trị fee growth inside hiện tại
   * @param feeGrowthInsideLast Giá trị fee growth inside lần cuối
   * @return Số lượng token fee đã tích lũy
   */
  public static BigDecimal calculateFeesOwed(BigDecimal liquidity, BigDecimal feeGrowthInside,
      BigDecimal feeGrowthInsideLast) {
    try {
      // Số phí = liquidity * (feeGrowthInside - feeGrowthInsideLast)
      BigDecimal feeGrowthDelta = feeGrowthInside.subtract(feeGrowthInsideLast);

      // Nếu delta âm, không có phí hoặc đã thu phí rồi
      if (feeGrowthDelta.compareTo(BigDecimal.ZERO) <= 0) {
        return BigDecimal.ZERO;
      }

      // Tính toán phí bằng cách nhân liquidity với feeGrowthDelta
      // Không cần chia cho Q128 vì chúng ta đã dùng BigDecimal
      BigDecimal feesOwed = liquidity.multiply(feeGrowthDelta)
          .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

      return feesOwed;
    } catch (Exception e) {
      logger.error("Error calculating fees owed: {}", e.getMessage(), e);
      return BigDecimal.ZERO;
    }
  }
}
