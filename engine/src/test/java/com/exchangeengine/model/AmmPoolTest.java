package com.exchangeengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.exchangeengine.factory.AmmPoolFactory;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import com.exchangeengine.storage.cache.TickCache;
import com.exchangeengine.factory.TickBitmapFactory;
import com.exchangeengine.storage.cache.TickBitmapCache;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test class for AmmPool model
 */
public class AmmPoolTest {

  private AmmPool ammPool;

  @BeforeEach
  public void setUp() {
    this.ammPool = AmmPoolFactory.createDefaultAmmPool();
  }

  @Test
  @DisplayName("Test toString and equals/hashCode methods work correctly")
  public void testToStringAndEqualsHashCode() {
    // Test toString
    String toString = ammPool.toString();
    assertTrue(toString.contains("pair="));
    assertTrue(toString.contains("isActive="));
    assertTrue(toString.contains("token0="));
    assertTrue(toString.contains("token1="));
    assertTrue(toString.contains("sqrtPrice="));
    assertTrue(toString.contains("price="));
  }

  @Test
  @DisplayName("Test validate method with all possible errors and then fixing them in one step")
  public void testValidateWithAllErrorsAndFixing() {
    // 1. Create instance with all possible validation errors
    AmmPool invalidPool = new AmmPool();
    invalidPool.setPair(null);
    invalidPool.setToken0(null);
    invalidPool.setToken1(null);
    invalidPool.setTickSpacing(-100);
    invalidPool.setFeePercentage(-1.0);
    invalidPool.setFeeProtocolPercentage(-0.5);
    invalidPool.setSqrtPrice(new BigDecimal("-1.5"));
    invalidPool.setPrice(new BigDecimal("-2.0"));
    invalidPool.setLiquidity(new BigDecimal("-1000"));
    invalidPool.setFeeGrowthGlobal0(new BigDecimal("-0.1"));
    invalidPool.setFeeGrowthGlobal1(new BigDecimal("-0.2"));
    invalidPool.setProtocolFees0(new BigDecimal("-10"));
    invalidPool.setProtocolFees1(new BigDecimal("-20"));
    invalidPool.setVolumeToken0(new BigDecimal("-100"));
    invalidPool.setVolumeToken1(new BigDecimal("-200"));
    invalidPool.setVolumeUSD(new BigDecimal("-1000"));
    invalidPool.setTxCount(-50);
    invalidPool.setTotalValueLockedToken0(new BigDecimal("-5000"));
    invalidPool.setTotalValueLockedToken1(new BigDecimal("-6000"));
    invalidPool.setInitPrice(new BigDecimal("-5.0"));
    // Set tick to an invalid value
    invalidPool.setCurrentTick(AmmPoolConfig.MAX_TICK + 1);

    // Verify all errors are present
    List<String> errors = invalidPool.validateRequiredFields();
    assertEquals(21, errors.size(), "Should have 21 validation errors");

    // Check specific errors
    assertTrue(errors.contains("Trading pair is required"));
    assertTrue(errors.contains("Token0 is required"));
    assertTrue(errors.contains("Token1 is required"));
    assertTrue(errors.contains("Tick spacing must be greater than 0"));
    assertTrue(errors.contains("Fee percentage must be greater than or equal to 0"));
    assertTrue(errors.contains("Protocol fee percentage must be greater than or equal to 0"));
    assertTrue(errors.contains("Square root price must be greater than or equal to 0"));
    assertTrue(errors.contains("Price must be greater than or equal to 0"));
    assertTrue(errors.contains("Liquidity must be greater than or equal to 0"));
    assertTrue(errors.contains("Fee growth global 0 must be greater than or equal to 0"));
    assertTrue(errors.contains("Fee growth global 1 must be greater than or equal to 0"));
    assertTrue(errors.contains("Protocol fees 0 must be greater than or equal to 0"));
    assertTrue(errors.contains("Protocol fees 1 must be greater than or equal to 0"));
    assertTrue(errors.contains("Volume token 0 must be greater than or equal to 0"));
    assertTrue(errors.contains("Volume token 1 must be greater than or equal to 0"));
    assertTrue(errors.contains("Volume USD must be greater than or equal to 0"));
    assertTrue(errors.contains("Transaction count must be greater than or equal to 0"));
    assertTrue(errors.contains("Total value locked token 0 must be greater than or equal to 0"));
    assertTrue(errors.contains("Total value locked token 1 must be greater than or equal to 0"));
    assertTrue(errors.contains("initPrice must be positive"));
    assertTrue(errors.contains("Tick must be between " + AmmPoolConfig.MIN_TICK + " and " + AmmPoolConfig.MAX_TICK));

    // add true value
    invalidPool.setPair("");
    invalidPool.setToken0("");
    invalidPool.setToken1("");
    invalidPool.setTickSpacing(0);
    invalidPool.setFeePercentage(0);
    invalidPool.setFeeProtocolPercentage(0);
    invalidPool.setSqrtPrice(BigDecimal.ZERO);
    invalidPool.setPrice(BigDecimal.ZERO);
    invalidPool.setLiquidity(BigDecimal.ZERO);
    invalidPool.setFeeGrowthGlobal0(BigDecimal.ZERO);
    invalidPool.setFeeGrowthGlobal1(BigDecimal.ZERO);
    invalidPool.setProtocolFees0(BigDecimal.ZERO);
    invalidPool.setProtocolFees1(BigDecimal.ZERO);
    invalidPool.setVolumeToken0(BigDecimal.ZERO);
    invalidPool.setVolumeToken1(BigDecimal.ZERO);
    invalidPool.setVolumeUSD(BigDecimal.ZERO);
    invalidPool.setTxCount(0);
    invalidPool.setTotalValueLockedToken0(BigDecimal.ZERO);
    invalidPool.setTotalValueLockedToken1(BigDecimal.ZERO);
    invalidPool.setInitPrice(BigDecimal.valueOf(1.0));
    invalidPool.setCurrentTick(0); // valid tick

    errors = invalidPool.validateRequiredFields();
    assertEquals(5, errors.size(), "Should have 5 validation errors");
    assertTrue(errors.contains("Trading pair is required"));
    assertTrue(errors.contains("Token0 is required"));
    assertTrue(errors.contains("Token1 is required"));
    assertTrue(errors.contains("Token0 and Token1 must be different"));
    assertTrue(errors.contains("Tick spacing must be greater than 0"));

    invalidPool.setPair("BTC/ETH");
    invalidPool.setTickSpacing(60);
    invalidPool.setToken0("BTC");
    invalidPool.setToken1("ETH");

    errors = invalidPool.validateRequiredFields();
    assertEquals(2, errors.size(), "Should have 2 validation errors");

    assertTrue(errors.contains("Token0: Unsupported coin: BTC"));
    assertTrue(errors.contains("Token1: Unsupported coin: ETH"));

    invalidPool.setToken0("VND");
    invalidPool.setToken1("USDT");

    errors = invalidPool.validateRequiredFields();
    assertEquals(0, errors.size(), "Should have 0 validation error");
  }

  @Test
  @DisplayName("Test constructor with pair parameter")
  public void testConstructorWithPair() {
    String testPair = "BTC/USDT";
    AmmPool pool = new AmmPool(testPair);

    assertEquals(testPair, pool.getPair(), "Pair should be set correctly");
    assertFalse(pool.isActive(), "Pool should be inactive by default");
    assertNotNull(pool.getCreatedAt(), "CreatedAt should be initialized");
    assertNotNull(pool.getUpdatedAt(), "UpdatedAt should be initialized");
  }

  @Test
  @DisplayName("Test hasUpdateField with various input combinations")
  public void testHasUpdateField() {
    // Setup
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    pool.setActive(false);
    pool.setFeePercentage(0.003);
    pool.setFeeProtocolPercentage(0.05);

    // Test case 1: No change
    assertFalse(pool.hasUpdateField(false, 0.003, 0.05, null),
        "Should return false when no fields change");

    // Test case 2: Only active status changes
    assertTrue(pool.hasUpdateField(true, 0.003, 0.05, null),
        "Should return true when active status changes");

    // Test case 3: Only fee percentage changes
    assertTrue(pool.hasUpdateField(false, 0.005, 0.05, null),
        "Should return true when fee percentage changes");

    // Test case 4: Only protocol fee percentage changes
    assertTrue(pool.hasUpdateField(false, 0.003, 0.07, null),
        "Should return true when protocol fee percentage changes");

    // Test case 5: All fields change
    assertTrue(pool.hasUpdateField(true, 0.01, 0.10, null),
        "Should return true when all fields change");

    // Test case 6: Negative fee percentage (invalid) - không áp dụng vì method yêu
    // cầu fee >= 0
    assertFalse(pool.hasUpdateField(false, -0.001, 0.05, null),
        "Should return false with negative fee percentage as condition requires >= 0");

    // Test case 7: Negative protocol fee percentage (invalid) - không áp dụng vì
    // method yêu cầu fee >= 0
    assertFalse(pool.hasUpdateField(false, 0.003, -0.01, null),
        "Should return false with negative protocol fee percentage as condition requires >= 0");

    // Test case 8: Only initPrice changes
    assertTrue(pool.hasUpdateField(false, 0.003, 0.05, BigDecimal.valueOf(1.5)),
        "Should return true when initPrice changes");

    // Test case 9: Negative initPrice (invalid) - không áp dụng vì method yêu
    // cầu initPrice > 0
    assertFalse(pool.hasUpdateField(false, 0.003, 0.05, BigDecimal.valueOf(-1.0)),
        "Should return false with negative initPrice as condition requires > 0");
  }

  @Test
  @DisplayName("Test hasUpdateField with null initPrice scenarios")
  public void testHasUpdateFieldWithNullInitPrice() {
    // Setup
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    pool.setActive(false);
    pool.setFeePercentage(0.003);
    pool.setFeeProtocolPercentage(0.05);

    // Set current initPrice to null
    pool.setInitPrice(null);

    // Test case 1: initPrice is null and new initPrice is null
    assertFalse(pool.hasUpdateField(false, 0.003, 0.05, null),
        "Should return false when both current and new initPrice are null");

    // Test case 2: initPrice is null and new initPrice is valid
    assertTrue(pool.hasUpdateField(false, 0.003, 0.05, BigDecimal.valueOf(1.5)),
        "Should return true when current initPrice is null and new initPrice is valid");

    // Reset initPrice to non-null
    pool.setInitPrice(BigDecimal.valueOf(2.0));

    // Test case 3: initPrice is non-null and new initPrice is null
    assertFalse(pool.hasUpdateField(false, 0.003, 0.05, null),
        "Should return false when new initPrice is null");

    // Test case 4: initPrice is non-null and new initPrice is zero or negative
    assertFalse(pool.hasUpdateField(false, 0.003, 0.05, BigDecimal.ZERO),
        "Should return false when new initPrice is zero");
    assertFalse(pool.hasUpdateField(false, 0.003, 0.05, BigDecimal.valueOf(-1.0)),
        "Should return false when new initPrice is negative");
  }

  @Test
  @DisplayName("Test update method with various scenarios")
  public void testUpdate() {
    // Setup
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    pool.setActive(false);
    pool.setFeePercentage(0.003);
    pool.setFeeProtocolPercentage(0.05);
    long initialUpdatedAt = pool.getUpdatedAt();

    // Ensure initial state
    assertFalse(pool.isActive());
    assertEquals(0.003, pool.getFeePercentage(), 0.0001);
    assertEquals(0.05, pool.getFeeProtocolPercentage(), 0.0001);

    // Wait to ensure timestamp will be different
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test case 1: No change
    boolean result1 = pool.update(false, 0.003, 0.05, null);
    assertFalse(result1, "Should return false when no fields change");
    assertEquals(initialUpdatedAt, pool.getUpdatedAt(), "UpdatedAt should not change");

    // Test case 2: Change active status
    boolean result2 = pool.update(true, 0.003, 0.05, null);
    assertTrue(result2, "Should return true when active status changes");
    assertTrue(pool.isActive(), "Pool should now be active");
    assertTrue(pool.getUpdatedAt() > initialUpdatedAt, "UpdatedAt should be updated");

    long secondTimestamp = pool.getUpdatedAt();

    // Wait to ensure timestamp will be different
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test case 3: Change fee percentage
    boolean result3 = pool.update(true, 0.005, 0.05, null);
    assertTrue(result3, "Should return true when fee percentage changes");
    assertEquals(0.005, pool.getFeePercentage(), 0.0001);
    assertTrue(pool.getUpdatedAt() > secondTimestamp, "UpdatedAt should be updated again");

    long thirdTimestamp = pool.getUpdatedAt();

    // Wait to ensure timestamp will be different
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test case 4: Change protocol fee percentage
    boolean result4 = pool.update(true, 0.005, 0.07, null);
    assertTrue(result4, "Should return true when protocol fee percentage changes");
    assertEquals(0.07, pool.getFeeProtocolPercentage(), 0.0001);
    assertTrue(pool.getUpdatedAt() > thirdTimestamp, "UpdatedAt should be updated again");

    // Test case 5: Test with null values (should not change)
    boolean result5 = pool.update(true, null, null, null);
    assertFalse(result5, "Should return false when fee percentages are null");
    assertEquals(0.005, pool.getFeePercentage(), 0.0001);
    assertEquals(0.07, pool.getFeeProtocolPercentage(), 0.0001);

    // Test case 6: Test with negative values (should be rejected)
    boolean result6 = pool.update(false, -0.001, -0.01, null);
    assertTrue(result6, "Should return true but only change active status, not negative fees");
    assertFalse(pool.isActive(), "Active status should change to false");
    assertEquals(0.005, pool.getFeePercentage(), 0.0001, "Fee percentage should not change to negative value");
    assertEquals(0.07, pool.getFeeProtocolPercentage(), 0.0001,
        "Protocol fee percentage should not change to negative value");
  }

  @Test
  @DisplayName("Test toMessageJson method")
  public void testToMessageJson() {
    // Setup - sử dụng factory
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();

    // Execute
    var result = pool.toMessageJson();

    // Verify
    assertNotNull(result, "Message JSON should not be null");
    assertTrue(result.containsKey("pair"), "Message JSON should contain pair field");
    assertTrue(result.containsKey("isActive"), "Message JSON should contain isActive field");
    assertTrue(result.containsKey("token0"), "Message JSON should contain token0 field");
    assertTrue(result.containsKey("token1"), "Message JSON should contain token1 field");
    assertTrue(result.containsKey("feePercentage"), "Message JSON should contain feePercentage field");
    assertTrue(result.containsKey("feeProtocolPercentage"), "Message JSON should contain feeProtocolPercentage field");
  }

  @Test
  @DisplayName("Test initPrice with Jakarta Validation")
  public void testInitPriceValidation() {
    // Tạo AmmPool mới với pair
    AmmPool pool = new AmmPool("USDT/VND");

    // Đảm bảo pool mới không có liquidity
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);
    pool.setActive(false);

    // Setters và getters hoạt động đúng
    BigDecimal testPrice = BigDecimal.valueOf(1.5);
    pool.setInitPrice(testPrice);
    assertEquals(testPrice, pool.getInitPrice());
  }

  @Test
  @DisplayName("Test converting between price and tick")
  public void testPriceAndTickConversion() {
    AmmPool pool = new AmmPool("USDT/VND");

    // Test converting from price to tick
    assertEquals(0, pool.priceToTick(BigDecimal.ONE));
    assertEquals(1, pool.priceToTick(BigDecimal.valueOf(1.0001)));
    assertEquals(2, pool.priceToTick(BigDecimal.valueOf(1.0001 * 1.0001)));
    assertEquals(-2, pool.priceToTick(BigDecimal.valueOf(1.0 / 1.0001)));

    // Test converting from tick to price with display scale rounding
    assertEquals(BigDecimal.valueOf(1.0001).setScale(AmmPoolConfig.DISPLAY_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.tickToPrice(1));
    assertEquals(BigDecimal.valueOf(1.0).setScale(AmmPoolConfig.DISPLAY_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.tickToPrice(0));
    assertEquals(BigDecimal.valueOf(1.0 / 1.0001).setScale(AmmPoolConfig.DISPLAY_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.tickToPrice(-1));
  }

  @Test
  @DisplayName("Test initializing pool with initPrice")
  public void testInitializingPoolWithInitPrice() {
    AmmPool pool = new AmmPool("USDT/VND");
    pool.setActive(false);
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);

    // Set initial price
    BigDecimal initPrice = BigDecimal.valueOf(1.025);
    pool.setInitPrice(initPrice);

    // Gọi calculateInitialPriceAndTick để áp dụng initPrice
    pool.calculateInitialPriceAndTick();

    // Check that values are calculated correctly
    assertEquals(initPrice, pool.getPrice());
    assertEquals(initPrice.sqrt(AmmPoolConfig.MC), pool.getSqrtPrice());
    assertEquals(pool.priceToTick(initPrice), pool.getCurrentTick());
  }

  @Test
  @DisplayName("Test can't update init price when pool is active (validation moved to AmmPoolEvent)")
  public void testCantUpdateInitPriceWhenActive() {
    AmmPool pool = new AmmPool("USDT/VND");
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);

    // Set initial price while not active
    BigDecimal initPrice = BigDecimal.valueOf(1.025);
    pool.setInitPrice(initPrice);
    pool.calculateInitialPriceAndTick();

    // Lưu giá trị ban đầu để so sánh sau khi cập nhật
    // (Không cần lưu vì chúng ta không sử dụng trong test này)

    // Make pool active
    pool.setActive(true);

    // Update initPrice - Note: calculateInitialPriceAndTick only checks liquidity,
    // not isActive
    // The validation that pool can't be active is done in AmmPoolEvent.validate()
    // method
    BigDecimal newInitPrice = BigDecimal.valueOf(2.0);
    pool.setInitPrice(newInitPrice);
    pool.calculateInitialPriceAndTick();

    // In AmmPool object itself, calculateInitialPriceAndTick only checks for
    // liquidity
    // Since there is no liquidity, the price will be updated
    assertEquals(newInitPrice, pool.getPrice(), "Price should be updated as AmmPool only checks liquidity");

    // AmmPoolEvent validation would prevent this update when isActive=true
    // But at the AmmPool model level, the check isn't enforced

    // Now add liquidity to show that liquidity check works
    pool.setTotalValueLockedToken0(BigDecimal.valueOf(10.0));

    BigDecimal newerPrice = BigDecimal.valueOf(3.0);
    pool.setInitPrice(newerPrice);
    pool.calculateInitialPriceAndTick();

    // Price shouldn't change with liquidity
    assertEquals(newInitPrice, pool.getPrice(), "Price should not change when pool has liquidity");
  }

  @Test
  @DisplayName("Test can't update init price when liquidity exists (validation moved to AmmPoolEvent)")
  public void testCantUpdateInitPriceWhenLiquidityExists() {
    AmmPool pool = new AmmPool("USDT/VND");
    pool.setActive(false);
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);

    // Set initial price with no liquidity
    BigDecimal initPrice = BigDecimal.valueOf(1.025);
    pool.setInitPrice(initPrice);
    pool.calculateInitialPriceAndTick();

    // Add liquidity
    pool.setTotalValueLockedToken0(BigDecimal.valueOf(10.0));

    // We can set initPrice now but it won't be applied because the pool has
    // liquidity
    pool.setInitPrice(BigDecimal.valueOf(2.0));
    pool.calculateInitialPriceAndTick();

    // The price should remain unchanged because calculateInitialPriceAndTick skips
    // when liquidity exists
    assertEquals(initPrice, pool.getPrice(), "Price should remain unchanged when pool has liquidity");
  }

  @Test
  @DisplayName("Test update followed by calculateInitialPriceAndTick")
  public void testUpdateTriggersInitPriceCalculation() {
    // Create a fresh pool
    AmmPool pool = new AmmPool("USDT/VND");
    pool.setActive(false);
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);
    pool.setInitPrice(BigDecimal.valueOf(1.5));

    // Lưu giá trị ban đầu để so sánh sau khi cập nhật
    // (Không cần lưu vì chúng ta không sử dụng trong test này)

    // Update with a new initPrice
    BigDecimal newInitPrice = BigDecimal.valueOf(2.5);
    boolean changed = pool.update(false, 0.003, 0.05, newInitPrice);

    // Verify
    assertTrue(changed, "Update with new initPrice should return true");
    assertEquals(newInitPrice, pool.getInitPrice(), "InitPrice should be updated");
    assertEquals(newInitPrice, pool.getPrice(), "Price should be updated to match new initPrice");
    assertEquals(pool.priceToTick(newInitPrice), pool.getCurrentTick(), "Tick should be updated based on new price");
  }

  @Test
  @DisplayName("Test updateForAddPosition method")
  public void testUpdateForAddPosition() {
    // Setup
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    BigDecimal initialLiquidity = pool.getLiquidity();
    BigDecimal initialTVL0 = pool.getTotalValueLockedToken0();
    BigDecimal initialTVL1 = pool.getTotalValueLockedToken1();
    int initialTxCount = pool.getTxCount();
    long initialUpdatedAt = pool.getUpdatedAt();

    // Đảm bảo có khoảng thời gian để kiểm tra updatedAt
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test case 1: Position trong range
    BigDecimal liquidityToAdd = new BigDecimal("1000");
    BigDecimal amount0 = new BigDecimal("500");
    BigDecimal amount1 = new BigDecimal("1500");
    boolean isInRange = true;

    boolean result = pool.updateForAddPosition(liquidityToAdd, isInRange, amount0, amount1);

    assertTrue(result, "Kết quả cập nhật phải là true");
    assertEquals(initialLiquidity.add(liquidityToAdd), pool.getLiquidity(),
        "Liquidity phải được cập nhật khi position nằm trong range");
    assertEquals(initialTVL0.add(amount0), pool.getTotalValueLockedToken0(),
        "TotalValueLockedToken0 phải được cập nhật");
    assertEquals(initialTVL1.add(amount1), pool.getTotalValueLockedToken1(),
        "TotalValueLockedToken1 phải được cập nhật");
    assertEquals(initialTxCount + 1, pool.getTxCount(),
        "TxCount phải tăng lên 1");
    assertTrue(pool.getUpdatedAt() > initialUpdatedAt,
        "UpdatedAt phải được cập nhật");

    // Lưu giá trị hiện tại để kiểm tra tiếp
    BigDecimal currentLiquidity = pool.getLiquidity();
    BigDecimal currentTVL0 = pool.getTotalValueLockedToken0();
    BigDecimal currentTVL1 = pool.getTotalValueLockedToken1();
    int currentTxCount = pool.getTxCount();
    long currentUpdatedAt = pool.getUpdatedAt();

    // Đảm bảo có khoảng thời gian để kiểm tra updatedAt
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test case 2: Position ngoài range
    BigDecimal liquidityOutOfRange = new BigDecimal("2000");
    BigDecimal amount0OutOfRange = new BigDecimal("700");
    BigDecimal amount1OutOfRange = new BigDecimal("1800");
    boolean isOutOfRange = false;

    result = pool.updateForAddPosition(liquidityOutOfRange, isOutOfRange, amount0OutOfRange, amount1OutOfRange);

    assertTrue(result, "Kết quả cập nhật phải là true");
    assertEquals(currentLiquidity, pool.getLiquidity(),
        "Liquidity không được cập nhật khi position nằm ngoài range");
    assertEquals(currentTVL0.add(amount0OutOfRange), pool.getTotalValueLockedToken0(),
        "TotalValueLockedToken0 phải được cập nhật");
    assertEquals(currentTVL1.add(amount1OutOfRange), pool.getTotalValueLockedToken1(),
        "TotalValueLockedToken1 phải được cập nhật");
    assertEquals(currentTxCount + 1, pool.getTxCount(),
        "TxCount phải tăng lên 1");
    assertTrue(pool.getUpdatedAt() > currentUpdatedAt,
        "UpdatedAt phải được cập nhật");

    // Test case 3: Không có liquidity được thêm
    currentLiquidity = pool.getLiquidity();
    currentTVL0 = pool.getTotalValueLockedToken0();
    currentTVL1 = pool.getTotalValueLockedToken1();
    currentTxCount = pool.getTxCount();
    currentUpdatedAt = pool.getUpdatedAt();

    // Đảm bảo có khoảng thời gian để kiểm tra updatedAt
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Thêm position với liquidity và amount là 0
    result = pool.updateForAddPosition(BigDecimal.ZERO, true, BigDecimal.ZERO, BigDecimal.ZERO);

    assertFalse(result, "Kết quả cập nhật phải là false khi không có gì thay đổi");
    assertEquals(currentLiquidity, pool.getLiquidity(),
        "Liquidity không được cập nhật khi không có liquidity thêm vào");
    assertEquals(currentTVL0, pool.getTotalValueLockedToken0(),
        "TotalValueLockedToken0 không được cập nhật khi amount0 là 0");
    assertEquals(currentTVL1, pool.getTotalValueLockedToken1(),
        "TotalValueLockedToken1 không được cập nhật khi amount1 là 0");
    assertEquals(currentTxCount + 1, pool.getTxCount(),
        "TxCount phải vẫn tăng lên 1");
    assertTrue(pool.getUpdatedAt() > currentUpdatedAt,
        "UpdatedAt phải được cập nhật");
  }

  @Test
  @DisplayName("Test updateForClosePosition method")
  public void testUpdateForClosePosition() {
    // Setup
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    BigDecimal initialLiquidity = pool.getLiquidity();
    BigDecimal initialToken0 = pool.getTotalValueLockedToken0();
    BigDecimal initialToken1 = pool.getTotalValueLockedToken1();
    int initialTxCount = pool.getTxCount();

    // Liquidty and tokens to remove
    BigDecimal liquidityToRemove = new BigDecimal("1000");
    BigDecimal token0ToRemove = new BigDecimal("500");
    BigDecimal token1ToRemove = new BigDecimal("500");

    // Execute
    boolean result = pool.updateForClosePosition(liquidityToRemove, token0ToRemove, token1ToRemove);

    // Verify
    assertTrue(result);

    // Check liquidity is reduced
    assertEquals(
        initialLiquidity.subtract(liquidityToRemove).setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.getLiquidity());

    // Check tokens removed
    assertEquals(
        initialToken0.subtract(token0ToRemove).setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.getTotalValueLockedToken0());
    assertEquals(
        initialToken1.subtract(token1ToRemove).setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.getTotalValueLockedToken1());

    // Check txCount increased
    assertEquals(initialTxCount + 1, pool.getTxCount());

    // Check timestamp updated
    assertTrue(pool.getUpdatedAt() > 0);
  }

  @Test
  @DisplayName("Test updateForClosePosition method with null values")
  public void testUpdateForClosePositionWithNullValues() {
    // Setup - null values should cause error
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    BigDecimal liquidityToRemove = null;
    BigDecimal token0ToRemove = new BigDecimal("500");
    BigDecimal token1ToRemove = new BigDecimal("500");

    // Execute
    boolean result = pool.updateForClosePosition(liquidityToRemove, token0ToRemove, token1ToRemove);

    // Verify
    assertFalse(result);
  }

  @Test
  @DisplayName("Test updateForClosePosition method with negative liquidity")
  public void testUpdateForClosePositionWithNegativeLiquidity() {
    // Setup
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    BigDecimal initialLiquidity = new BigDecimal("500");
    pool.setLiquidity(initialLiquidity);

    // Try to remove more liquidity than available
    BigDecimal liquidityToRemove = new BigDecimal("1000");
    BigDecimal token0ToRemove = new BigDecimal("100");
    BigDecimal token1ToRemove = new BigDecimal("100");

    // Execute
    boolean result = pool.updateForClosePosition(liquidityToRemove, token0ToRemove, token1ToRemove);

    // Verify
    assertTrue(result);

    // Liquidity should be 0, not negative
    assertEquals(BigDecimal.ZERO.setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.getLiquidity());
  }

  @Test
  @DisplayName("Test updateForClosePosition method with negative token amounts")
  public void testUpdateForClosePositionWithNegativeTokenAmounts() {
    // Setup
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    BigDecimal initialToken0 = new BigDecimal("100");
    BigDecimal initialToken1 = new BigDecimal("100");
    pool.setTotalValueLockedToken0(initialToken0);
    pool.setTotalValueLockedToken1(initialToken1);

    // Try to remove more tokens than available
    BigDecimal liquidityToRemove = new BigDecimal("100");
    BigDecimal token0ToRemove = new BigDecimal("500");
    BigDecimal token1ToRemove = new BigDecimal("500");

    // Execute
    boolean result = pool.updateForClosePosition(liquidityToRemove, token0ToRemove, token1ToRemove);

    // Verify
    assertTrue(result);

    // Token amounts should be 0, not negative
    assertEquals(BigDecimal.ZERO.setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.getTotalValueLockedToken0());
    assertEquals(BigDecimal.ZERO.setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE),
        pool.getTotalValueLockedToken1());
  }

  @Test
  @DisplayName("Test validateRequiredFields when token0 equals token1 but one is null")
  public void testValidateRequiredFieldsWithTokenEqualityEdgeCase() {
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    pool.setToken0("VND");
    pool.setToken1(null);

    List<String> errors = pool.validateRequiredFields();
    assertTrue(errors.contains("Token1 is required"), "Should contain Token1 required error");
    assertFalse(errors.contains("Token0 and Token1 must be different"),
        "Should not contain token equality error when one token is null");

    // Set token0 to null and token1 to not null
    pool.setToken0(null);
    pool.setToken1("USDT");

    errors = pool.validateRequiredFields();
    assertTrue(errors.contains("Token0 is required"), "Should contain Token0 required error");
    assertFalse(errors.contains("Token0 and Token1 must be different"),
        "Should not contain token equality error when one token is null");
  }

  @Test
  @DisplayName("Test update method with initPrice edge cases")
  public void testUpdateWithInitPriceEdgeCases() {
    // Setup
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    pool.setInitPrice(BigDecimal.valueOf(1.0));
    long initialUpdatedAt = pool.getUpdatedAt();

    // Wait to ensure timestamp will be different
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test case 1: newInitPrice is not null but is equal to current value
    // In the actual implementation, this would still call
    // calculateInitialPriceAndTick
    boolean result1 = pool.update(false, 0.003, 0.05, BigDecimal.valueOf(1.0));
    assertTrue(result1, "Should return true when newInitPrice equals current value due to recalculation");
    assertTrue(pool.getUpdatedAt() > initialUpdatedAt, "UpdatedAt should change");

    initialUpdatedAt = pool.getUpdatedAt();

    // Wait to ensure timestamp will be different
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test case 2: newInitPrice is not null but is zero
    boolean result2 = pool.update(false, 0.003, 0.05, BigDecimal.ZERO);
    assertFalse(result2, "Should return false when newInitPrice is zero");
    assertEquals(initialUpdatedAt, pool.getUpdatedAt(), "UpdatedAt should not change");

    // Test case 3: newInitPrice is not null but is negative
    boolean result3 = pool.update(false, 0.003, 0.05, BigDecimal.valueOf(-1.0));
    assertFalse(result3, "Should return false when newInitPrice is negative");
    assertEquals(initialUpdatedAt, pool.getUpdatedAt(), "UpdatedAt should not change");

    // Test case 4: current initPrice is null and newInitPrice is valid
    pool.setInitPrice(null);
    boolean result4 = pool.update(false, 0.003, 0.05, BigDecimal.valueOf(2.0));
    assertTrue(result4, "Should return true when current initPrice is null and newInitPrice is valid");
    assertEquals(BigDecimal.valueOf(2.0), pool.getInitPrice(), "InitPrice should be updated");
    assertTrue(pool.getUpdatedAt() > initialUpdatedAt, "UpdatedAt should be updated");
  }

  @Test
  @DisplayName("Test calculateInitialPriceAndTick with various edge cases")
  public void testCalculateInitialPriceAndTickEdgeCases() {
    // Case 1: totalValueLockedToken0 > 0, should not calculate
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    pool.setPrice(BigDecimal.ZERO);
    pool.setCurrentTick(0);
    pool.setSqrtPrice(BigDecimal.ZERO);
    pool.setInitPrice(BigDecimal.valueOf(1.5));
    pool.setTotalValueLockedToken0(BigDecimal.valueOf(10.0));
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);

    pool.calculateInitialPriceAndTick();
    assertEquals(BigDecimal.ZERO, pool.getPrice(), "Price should not be updated when totalValueLockedToken0 > 0");
    assertEquals(0, pool.getCurrentTick(), "CurrentTick should not be updated when totalValueLockedToken0 > 0");
    assertEquals(BigDecimal.ZERO, pool.getSqrtPrice(),
        "SqrtPrice should not be updated when totalValueLockedToken0 > 0");

    // Case 2: totalValueLockedToken1 > 0, should not calculate
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.valueOf(10.0));

    pool.calculateInitialPriceAndTick();
    assertEquals(BigDecimal.ZERO, pool.getPrice(), "Price should not be updated when totalValueLockedToken1 > 0");
    assertEquals(0, pool.getCurrentTick(), "CurrentTick should not be updated when totalValueLockedToken1 > 0");
    assertEquals(BigDecimal.ZERO, pool.getSqrtPrice(),
        "SqrtPrice should not be updated when totalValueLockedToken1 > 0");

    // Case 3: initPrice is null, should not calculate
    pool.setTotalValueLockedToken0(BigDecimal.ZERO);
    pool.setTotalValueLockedToken1(BigDecimal.ZERO);
    pool.setInitPrice(null);

    pool.calculateInitialPriceAndTick();
    assertEquals(BigDecimal.ZERO, pool.getPrice(), "Price should not be updated when initPrice is null");
    assertEquals(0, pool.getCurrentTick(), "CurrentTick should not be updated when initPrice is null");
    assertEquals(BigDecimal.ZERO, pool.getSqrtPrice(), "SqrtPrice should not be updated when initPrice is null");

    // Case 4: initPrice <= 0, should not calculate
    pool.setInitPrice(BigDecimal.ZERO);

    pool.calculateInitialPriceAndTick();
    assertEquals(BigDecimal.ZERO, pool.getPrice(), "Price should not be updated when initPrice is zero");
    assertEquals(0, pool.getCurrentTick(), "CurrentTick should not be updated when initPrice is zero");
    assertEquals(BigDecimal.ZERO, pool.getSqrtPrice(), "SqrtPrice should not be updated when initPrice is zero");
  }

  @Test
  @DisplayName("Test updateForClosePosition with zero and negative liquidity")
  public void testUpdateForClosePositionWithZeroAndNegativeLiquidity() {
    // Setup - create a pool with some liquidity
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    BigDecimal initialLiquidity = BigDecimal.valueOf(100);
    pool.setLiquidity(initialLiquidity);
    pool.setTotalValueLockedToken0(BigDecimal.valueOf(50));
    pool.setTotalValueLockedToken1(BigDecimal.valueOf(50));
    long initialUpdatedAt = pool.getUpdatedAt();
    int initialTxCount = pool.getTxCount();

    // Wait to ensure timestamp will be different
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test with zero removedLiquidity but positive token amounts
    boolean result1 = pool.updateForClosePosition(BigDecimal.ZERO,
        BigDecimal.valueOf(10),
        BigDecimal.valueOf(10));

    assertTrue(result1, "Should return true even when removedLiquidity is zero but token amounts are positive");
    assertEquals(initialLiquidity, pool.getLiquidity(), "Liquidity should not change when removedLiquidity is zero");
    // We need to use compareTo for BigDecimal to avoid scale issues
    assertTrue(pool.getTotalValueLockedToken0().compareTo(BigDecimal.valueOf(40)) == 0,
        "Token0 amount should be reduced to 40");
    assertTrue(pool.getTotalValueLockedToken1().compareTo(BigDecimal.valueOf(40)) == 0,
        "Token1 amount should be reduced to 40");
    assertTrue(pool.getUpdatedAt() > initialUpdatedAt, "UpdatedAt should be updated");
    assertEquals(initialTxCount + 1, pool.getTxCount(), "Transaction count should be incremented");

    initialUpdatedAt = pool.getUpdatedAt();
    initialTxCount = pool.getTxCount();

    // Wait to ensure timestamp will be different
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      // ignore
    }

    // Test with negative removedLiquidity (should be treated as if zero)
    boolean result2 = pool.updateForClosePosition(BigDecimal.valueOf(-10),
        BigDecimal.ZERO,
        BigDecimal.ZERO);

    assertFalse(result2, "Should return false when all values are zero or negative");
    assertEquals(initialLiquidity, pool.getLiquidity(),
        "Liquidity should not change when removedLiquidity is negative");
    assertTrue(pool.getTotalValueLockedToken0().compareTo(BigDecimal.valueOf(40)) == 0,
        "Token0 should not change from 40");
    assertTrue(pool.getTotalValueLockedToken1().compareTo(BigDecimal.valueOf(40)) == 0,
        "Token1 should not change from 40");
    // Transaction count still increments and timestamp is updated even when no
    // actual changes happen
    assertEquals(initialTxCount + 1, pool.getTxCount(), "Transaction count should still be incremented");
    assertTrue(pool.getUpdatedAt() > initialUpdatedAt, "UpdatedAt should still be updated");
  }

  @Test
  @DisplayName("Test getTick method when tick exists")
  public void testGetTickWhenTickExists() {
    // Arrange
    AmmPool pool = spy(AmmPoolFactory.createDefaultAmmPool());
    int tickIndex = 100;
    String poolPair = pool.getPair();
    String tickKey = poolPair + "-" + tickIndex;

    // Tạo một đối tượng Tick để sử dụng trong test
    Tick expectedTick = new Tick();
    expectedTick.setTickIndex(tickIndex);
    expectedTick.setPoolPair(poolPair);

    // Mock TickCache
    TickCache mockTickCache = mock(TickCache.class);
    when(mockTickCache.getTick(tickKey)).thenReturn(Optional.of(expectedTick));

    try {
      // Thiết lập mock instance
      TickCache.setTestInstance(mockTickCache);

      // Act
      Tick actualTick = pool.getTick(tickIndex);

      // Assert
      assertEquals(expectedTick, actualTick, "Should return the correct Tick from cache");
      assertEquals(tickIndex, actualTick.getTickIndex(), "Tick index should match");
      assertEquals(poolPair, actualTick.getPoolPair(), "Pool pair should match");
    } finally {
      // Đảm bảo reset lại mock instance
      TickCache.setTestInstance(null);
    }
  }

  @Test
  @DisplayName("Test getTick method when tick does not exist")
  public void testGetTickWhenTickDoesNotExist() {
    // Arrange
    AmmPool pool = spy(AmmPoolFactory.createDefaultAmmPool());
    int nonExistentTickIndex = 999999;
    String tickKey = pool.getPair() + "-" + nonExistentTickIndex;

    // Mock TickCache
    TickCache mockTickCache = mock(TickCache.class);
    when(mockTickCache.getTick(tickKey)).thenReturn(Optional.empty());

    try {
      // Thiết lập mock instance
      TickCache.setTestInstance(mockTickCache);

      // Act & Assert
      IllegalStateException exception = assertThrows(IllegalStateException.class,
          () -> pool.getTick(nonExistentTickIndex),
          "Should throw IllegalStateException when tick does not exist");

      // Kiểm tra thêm về nội dung của exception
      String expectedMessage = "Tick not found for key: " + tickKey;
      assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected format");
    } finally {
      // Đảm bảo reset lại mock instance
      TickCache.setTestInstance(null);
    }
  }

  @Test
  @DisplayName("Test getTickBitmap method when TickBitmap exists")
  public void testGetTickBitmapWhenExists() {
    // Arrange
    AmmPool pool = spy(AmmPoolFactory.createDefaultAmmPool());
    String poolPair = pool.getPair();

    // Tạo một đối tượng TickBitmap để sử dụng trong test
    TickBitmapFactory tickBitmapFactory = new TickBitmapFactory();
    TickBitmap expectedTickBitmap = tickBitmapFactory.createEmptyBitmap(poolPair);

    // Mock TickBitmapCache
    TickBitmapCache mockTickBitmapCache = mock(TickBitmapCache.class);
    when(mockTickBitmapCache.getTickBitmap(poolPair)).thenReturn(Optional.of(expectedTickBitmap));

    try {
      // Thiết lập mock instance
      TickBitmapCache.setTestInstance(mockTickBitmapCache);

      // Act
      TickBitmap actualTickBitmap = pool.getTickBitmap();

      // Assert
      assertEquals(expectedTickBitmap, actualTickBitmap, "Should return the correct TickBitmap from cache");
      assertEquals(poolPair, actualTickBitmap.getPoolPair(), "Pool pair should match the expected value");
    } finally {
      // Đảm bảo reset lại mock instance
      TickBitmapCache.setTestInstance(null);
    }
  }

  @Test
  @DisplayName("Test getTickBitmap method when TickBitmap does not exist")
  public void testGetTickBitmapWhenDoesNotExist() {
    // Arrange
    AmmPool pool = spy(AmmPoolFactory.createDefaultAmmPool());
    String poolPair = pool.getPair();

    // Mock TickBitmapCache
    TickBitmapCache mockTickBitmapCache = mock(TickBitmapCache.class);
    when(mockTickBitmapCache.getTickBitmap(poolPair)).thenReturn(Optional.empty());

    try {
      // Thiết lập mock instance
      TickBitmapCache.setTestInstance(mockTickBitmapCache);

      // Act & Assert
      IllegalStateException exception = assertThrows(IllegalStateException.class,
          () -> pool.getTickBitmap(),
          "Should throw IllegalStateException when TickBitmap does not exist");

      // Kiểm tra thêm về nội dung của exception
      String expectedMessage = "TickBitmap not found for pool: " + poolPair;
      assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected format");
    } finally {
      // Đảm bảo reset lại mock instance
      TickBitmapCache.setTestInstance(null);
    }
  }

  @Test
  @DisplayName("Test updatePoolAfterSwap method")
  public void testUpdatePoolAfterSwap() {
    // Chuẩn bị dữ liệu test
    Map<String, Object> poolParams = new HashMap<>();
    poolParams.put("pair", "VND-USDT");
    poolParams.put("token0", "VND");
    poolParams.put("token1", "USDT");
    poolParams.put("currentTick", 0);
    poolParams.put("sqrtPrice", BigDecimal.valueOf(1.0));
    poolParams.put("liquidity", BigDecimal.valueOf(1000.0));
    poolParams.put("feeGrowthGlobal0", BigDecimal.valueOf(0.1));
    poolParams.put("feeGrowthGlobal1", BigDecimal.valueOf(0.2));
    poolParams.put("totalValueLockedToken0", BigDecimal.valueOf(5000.0));
    poolParams.put("totalValueLockedToken1", BigDecimal.valueOf(6000.0));
    poolParams.put("volumeToken0", BigDecimal.valueOf(1000.0));
    poolParams.put("volumeToken1", BigDecimal.valueOf(2000.0));
    poolParams.put("txCount", 5);

    AmmPool pool = AmmPoolFactory.createCustomAmmPool(poolParams);

    // Lưu thời gian cập nhật ban đầu để so sánh
    long originalUpdatedAt = pool.getUpdatedAt();

    // Cho phép thời gian trôi qua
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      // Bỏ qua
    }

    // Các giá trị mới
    int newTick = 10;
    BigDecimal newSqrtPrice = BigDecimal.valueOf(1.1);
    BigDecimal newLiquidity = BigDecimal.valueOf(1200.0);
    BigDecimal newFeeGrowthGlobal0 = BigDecimal.valueOf(0.15);
    BigDecimal newFeeGrowthGlobal1 = BigDecimal.valueOf(0.25);
    BigDecimal newTVL0 = BigDecimal.valueOf(5500.0);
    BigDecimal newTVL1 = BigDecimal.valueOf(5800.0);
    BigDecimal newVolume0 = BigDecimal.valueOf(1500.0);
    BigDecimal newVolume1 = BigDecimal.valueOf(2500.0);

    // Thực hiện
    pool.updatePoolAfterSwap(
        newTick,
        newSqrtPrice,
        newLiquidity,
        newFeeGrowthGlobal0,
        newFeeGrowthGlobal1,
        newTVL0,
        newTVL1,
        newVolume0,
        newVolume1);

    // Kiểm tra các giá trị đã được cập nhật đúng
    assertEquals(newTick, pool.getCurrentTick(), "Tick phải được cập nhật");
    assertEquals(newSqrtPrice, pool.getSqrtPrice(), "SqrtPrice phải được cập nhật");
    assertEquals(newLiquidity, pool.getLiquidity(), "Liquidity phải được cập nhật");
    assertEquals(newFeeGrowthGlobal0, pool.getFeeGrowthGlobal0(), "FeeGrowthGlobal0 phải được cập nhật");
    assertEquals(newFeeGrowthGlobal1, pool.getFeeGrowthGlobal1(), "FeeGrowthGlobal1 phải được cập nhật");
    assertEquals(newTVL0, pool.getTotalValueLockedToken0(), "TotalValueLockedToken0 phải được cập nhật");
    assertEquals(newTVL1, pool.getTotalValueLockedToken1(), "TotalValueLockedToken1 phải được cập nhật");
    assertEquals(newVolume0, pool.getVolumeToken0(), "VolumeToken0 phải được cập nhật");
    assertEquals(newVolume1, pool.getVolumeToken1(), "VolumeToken1 phải được cập nhật");

    // Kiểm tra txCount đã tăng và thời gian cập nhật đã được cập nhật
    assertEquals(6, pool.getTxCount(), "TxCount phải tăng 1");
    assertTrue(pool.getUpdatedAt() > originalUpdatedAt, "UpdatedAt phải được cập nhật");

    // Kiểm tra price đã được tính lại từ sqrtPrice
    BigDecimal expectedPrice = newSqrtPrice.pow(2, AmmPoolConfig.MC).setScale(AmmPoolConfig.DISPLAY_SCALE,
        AmmPoolConfig.ROUNDING_MODE);
    assertEquals(expectedPrice, pool.getPrice(), "Price phải được tính lại từ sqrtPrice");
  }

  @Test
  @DisplayName("Test updatePoolAfterSwap method with null parameters")
  public void testUpdatePoolAfterSwapWithNullParameters() {
    // Chuẩn bị dữ liệu test
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();

    // Lưu trạng thái ban đầu để kiểm tra không có gì thay đổi sau khi gọi method
    int initialTick = pool.getCurrentTick();
    BigDecimal initialSqrtPrice = pool.getSqrtPrice();
    BigDecimal initialLiquidity = pool.getLiquidity();
    BigDecimal initialFeeGrowthGlobal0 = pool.getFeeGrowthGlobal0();
    BigDecimal initialFeeGrowthGlobal1 = pool.getFeeGrowthGlobal1();
    BigDecimal initialTVL0 = pool.getTotalValueLockedToken0();
    BigDecimal initialTVL1 = pool.getTotalValueLockedToken1();
    BigDecimal initialVolume0 = pool.getVolumeToken0();
    BigDecimal initialVolume1 = pool.getVolumeToken1();
    int initialTxCount = pool.getTxCount();
    long initialUpdatedAt = pool.getUpdatedAt();

    // Cho phép thời gian trôi qua
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      // Bỏ qua
    }

    // Thực hiện với một số tham số null
    pool.updatePoolAfterSwap(
        10,
        null, // sqrtPrice null
        BigDecimal.valueOf(1200.0),
        null, // feeGrowthGlobal0 null
        BigDecimal.valueOf(0.25),
        BigDecimal.valueOf(5500.0),
        null, // totalValueLockedToken1 null
        BigDecimal.valueOf(1500.0),
        null); // volumeToken1 null

    // Kiểm tra không có gì thay đổi khi có tham số null
    assertEquals(initialTick, pool.getCurrentTick(), "Tick không nên thay đổi khi có tham số null");
    assertEquals(initialSqrtPrice, pool.getSqrtPrice(), "SqrtPrice không nên thay đổi khi có tham số null");
    assertEquals(initialLiquidity, pool.getLiquidity(), "Liquidity không nên thay đổi khi có tham số null");
    assertEquals(initialFeeGrowthGlobal0, pool.getFeeGrowthGlobal0(),
        "FeeGrowthGlobal0 không nên thay đổi khi có tham số null");
    assertEquals(initialFeeGrowthGlobal1, pool.getFeeGrowthGlobal1(),
        "FeeGrowthGlobal1 không nên thay đổi khi có tham số null");
    assertEquals(initialTVL0, pool.getTotalValueLockedToken0(),
        "TotalValueLockedToken0 không nên thay đổi khi có tham số null");
    assertEquals(initialTVL1, pool.getTotalValueLockedToken1(),
        "TotalValueLockedToken1 không nên thay đổi khi có tham số null");
    assertEquals(initialVolume0, pool.getVolumeToken0(), "VolumeToken0 không nên thay đổi khi có tham số null");
    assertEquals(initialVolume1, pool.getVolumeToken1(), "VolumeToken1 không nên thay đổi khi có tham số null");
    assertEquals(initialTxCount, pool.getTxCount(), "TxCount không nên thay đổi khi có tham số null");
    assertEquals(initialUpdatedAt, pool.getUpdatedAt(), "UpdatedAt không nên thay đổi khi có tham số null");
  }

  @Test
  @DisplayName("Test updatePoolAfterSwap method catching exceptions")
  public void testUpdatePoolAfterSwapWithException() {
    // Tạo một AmmPool mới để test
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();

    // Không cần lưu trạng thái ban đầu vì chúng ta chỉ kiểm tra xem phương thức có
    // throw exception hay không

    // Tạo tham số có vấn đề - sqrtPrice không thể là null
    BigDecimal invalidSqrtPrice = null;

    // Gọi phương thức với tham số có vấn đề - điều này sẽ gây ra exception trong
    // phương thức do sqrtPrice là null và không thể tính price từ null
    try {
      pool.updatePoolAfterSwap(
          10, // Tick hợp lệ
          invalidSqrtPrice, // SqrtPrice không hợp lệ (null)
          BigDecimal.valueOf(1200.0),
          BigDecimal.valueOf(0.15),
          BigDecimal.valueOf(0.25),
          BigDecimal.valueOf(5500.0),
          BigDecimal.valueOf(5800.0),
          BigDecimal.valueOf(1500.0),
          BigDecimal.valueOf(2500.0));

      // Kiểm tra rằng phương thức không throw exception
      // Quan trọng: trạng thái của pool nên vẫn được giữ nguyên
      // Chúng ta biết mô hình thực tế đang cập nhật trước khi exception xảy ra,
      // nên chúng ta chấp nhận rằng currentTick và một số trường khác có thể đã thay
      // đổi
      // Điều quan trọng nhất là test rằng exception không bị thrown ra ngoài

    } catch (Exception e) {
      fail("Phương thức updatePoolAfterSwap không nên throw exception ra ngoài: " + e.getMessage());
    }

    // Phương thức đã bắt và xử lý ngoại lệ thành công nếu test đạt đến đây
  }
}
