package com.exchangeengine.factory;

import com.exchangeengine.model.AmmPool;
import com.exchangeengine.util.TestModelFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

/**
 * Factory class for creating AmmPool instances for testing purposes
 */
public class AmmPoolFactory {

  /**
   * Creates a default AmmPool instance with predefined values
   *
   * @return a new AmmPool instance with default values
   */
  public static AmmPool createDefaultAmmPool() {
    AmmPool pool = new AmmPool();

    pool.setPair("USDT/VND");
    pool.setToken0("USDT");
    pool.setToken1("VND");
    pool.setTickSpacing(10);
    pool.setFeePercentage(0.003); // 0.3%
    pool.setFeeProtocolPercentage(0.05); // 5%

    // Đặt các giá trị khác trước khi set active
    pool.setCurrentTick(0);
    pool.setSqrtPrice(BigDecimal.valueOf(1.0));
    pool.setPrice(BigDecimal.valueOf(1.0));
    pool.setLiquidity(BigDecimal.valueOf(1000000));

    pool.setFeeGrowthGlobal0(BigDecimal.ZERO);
    pool.setFeeGrowthGlobal1(BigDecimal.ZERO);

    pool.setProtocolFees0(BigDecimal.ZERO);
    pool.setProtocolFees1(BigDecimal.ZERO);

    pool.setVolumeToken0(BigDecimal.ZERO);
    pool.setVolumeToken1(BigDecimal.ZERO);
    pool.setVolumeUSD(BigDecimal.ZERO);
    pool.setTxCount(0);

    // Khi tạo pool mới, đảm bảo không có liquidity trước khi set initPrice
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);

    pool.setCreatedAt(System.currentTimeMillis());
    pool.setUpdatedAt(System.currentTimeMillis());

    // Đặt initPrice trước khi active
    pool.setInitPrice(BigDecimal.valueOf(1.0));

    // Sau đó mới đặt giá trị liquidity thực tế
    pool.setTotalValueLockedToken0(BigDecimal.valueOf(500000));
    pool.setTotalValueLockedToken1(BigDecimal.valueOf(500000));

    // Đặt active sau khi tạo pool
    pool.setActive(true);

    return pool;
  }

  /**
   * Creates a default AmmPool instance but with no liquidity for testing
   * initialization
   *
   * @return a new AmmPool instance with no liquidity
   */
  public static AmmPool createEmptyPool() {
    AmmPool pool = new AmmPool();

    pool.setPair("USDT/VND");
    pool.setToken0("USDT");
    pool.setToken1("VND");
    pool.setTickSpacing(10);
    pool.setFeePercentage(0.003); // 0.3%
    pool.setFeeProtocolPercentage(0.05); // 5%

    pool.setCurrentTick(0);
    pool.setSqrtPrice(BigDecimal.valueOf(1.0));
    pool.setPrice(BigDecimal.valueOf(1.0));
    pool.setLiquidity(BigDecimal.ZERO);

    pool.setFeeGrowthGlobal0(BigDecimal.ZERO);
    pool.setFeeGrowthGlobal1(BigDecimal.ZERO);

    pool.setProtocolFees0(BigDecimal.ZERO);
    pool.setProtocolFees1(BigDecimal.ZERO);

    pool.setVolumeToken0(BigDecimal.ZERO);
    pool.setVolumeToken1(BigDecimal.ZERO);
    pool.setVolumeUSD(BigDecimal.ZERO);
    pool.setTxCount(0);

    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);

    pool.setCreatedAt(System.currentTimeMillis());
    pool.setUpdatedAt(System.currentTimeMillis());

    // Đặt initPrice khi chưa active
    pool.setInitPrice(BigDecimal.valueOf(1.0));

    // Đảm bảo pool không active
    pool.setActive(false);

    return pool;
  }

  /**
   * Tùy chỉnh AmmPool dựa trên các tham số cung cấp qua Map
   *
   * @param customFields map chứa các tham số tùy chỉnh
   * @return AmmPool đã tùy chỉnh
   */
  public static AmmPool createCustomAmmPool(Map<String, Object> customFields) {
    AmmPool pool = createDefaultAmmPool();
    return TestModelFactory.customize(pool, customFields);
  }

  /**
   * Creates a test AmmPool instance with a specific initial price and initialized
   * price/tick
   *
   * @param pair      the trading pair
   * @param token0    the first token
   * @param token1    the second token
   * @param initPrice the initial price to set
   * @return a new AmmPool with price and tick initialized from initPrice
   */
  public static AmmPool createInitializedPool(String pair, String token0, String token1, BigDecimal initPrice) {
    AmmPool pool = new AmmPool();

    pool.setPair(pair);
    pool.setToken0(token0);
    pool.setToken1(token1);
    pool.setTickSpacing(10);
    pool.setFeePercentage(0.003);
    pool.setFeeProtocolPercentage(0.05);

    pool.setLiquidity(BigDecimal.ZERO);
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);

    // Đặt initPrice và tính toán giá trị
    pool.setInitPrice(initPrice);
    pool.calculateInitialPriceAndTick();

    return pool;
  }

  /**
   * Creates a pool with a specific price for testing
   *
   * @param price the price to set
   * @return a new AmmPool with the specified price
   */
  public static AmmPool createPoolWithPrice(BigDecimal price) {
    AmmPool pool = new AmmPool();

    pool.setPair("USDT/VND");
    pool.setToken0("USDT");
    pool.setToken1("VND");
    pool.setTickSpacing(10);
    pool.setFeePercentage(0.003);
    pool.setFeeProtocolPercentage(0.05);

    pool.setLiquidity(BigDecimal.ZERO);
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);

    // Đặt initPrice
    pool.setInitPrice(price);

    return pool;
  }

  /**
   * Creates a custom AmmPool instance with customizable parameters
   *
   * @param pair          the trading pair
   * @param token0        the first token
   * @param token1        the second token
   * @param feePercentage the fee percentage
   * @return a new AmmPool with the specified parameters
   */
  public static AmmPool createCustomAmmPool(String pair, String token0, String token1, double feePercentage) {
    Map<String, Object> customFields = new HashMap<>();
    customFields.put("pair", pair);
    customFields.put("token0", token0);
    customFields.put("token1", token1);
    customFields.put("feePercentage", feePercentage);
    return createCustomAmmPool(customFields);
  }

  /**
   * Creates a custom AmmPool instance with customizable parameters including
   * initPrice
   *
   * @param pair          the trading pair
   * @param token0        the first token
   * @param token1        the second token
   * @param feePercentage the fee percentage
   * @param initPrice     the initial price
   * @return a new AmmPool with the specified parameters
   */
  public static AmmPool createCustomAmmPool(String pair, String token0, String token1, double feePercentage,
      BigDecimal initPrice) {
    Map<String, Object> customFields = new HashMap<>();
    customFields.put("pair", pair);
    customFields.put("token0", token0);
    customFields.put("token1", token1);
    customFields.put("feePercentage", feePercentage);
    customFields.put("initPrice", initPrice);
    return createCustomAmmPool(customFields);
  }
}
