package com.exchangeengine.util.ammPool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.model.Tick;

@ExtendWith(MockitoExtension.class)
public class LiquidityUtilsTest {

  @Mock
  private AmmPool mockPool;

  @Mock
  private AmmPosition mockPosition;

  @Mock
  private Tick mockLowerTick;

  @Mock
  private Tick mockUpperTick;

  private BigDecimal sqrtRatioCurrentTick;
  private BigDecimal sqrtRatioLowerTick;
  private BigDecimal sqrtRatioUpperTick;
  private BigDecimal amount0;
  private BigDecimal amount1;
  private BigDecimal liquidity;

  @BeforeEach
  public void setUp() {
    // Thiết lập các giá trị mặc định cho test
    sqrtRatioCurrentTick = BigDecimal.valueOf(1.5);
    sqrtRatioLowerTick = BigDecimal.valueOf(1.0);
    sqrtRatioUpperTick = BigDecimal.valueOf(2.0);
    amount0 = BigDecimal.valueOf(100);
    amount1 = BigDecimal.valueOf(100);
    liquidity = BigDecimal.valueOf(50);
  }

  @Test
  @DisplayName("Test calculateLiquidity khi hiện tại nằm giữa lower và upper")
  public void testCalculateLiquidity_CurrentBetweenLowerAndUpper() {
    // Arrange
    when(mockPool.getCurrentTick()).thenReturn(5);
    when(mockPosition.getTickLowerIndex()).thenReturn(1);
    when(mockPosition.getTickUpperIndex()).thenReturn(10);
    when(mockPosition.getAmount0Initial()).thenReturn(amount0);
    when(mockPosition.getAmount1Initial()).thenReturn(amount1);

    // Act
    BigDecimal result = LiquidityUtils.calculateLiquidity(mockPool, mockPosition);

    // Assert
    assertNotNull(result);
    assertEquals(result.compareTo(BigDecimal.ZERO), 1); // liquidity > 0
  }

  @Test
  @DisplayName("Test calculateLiquidity khi xảy ra ngoại lệ")
  public void testCalculateLiquidity_Exception() {
    // Arrange
    when(mockPool.getCurrentTick()).thenReturn(5);
    when(mockPosition.getTickLowerIndex()).thenThrow(new RuntimeException("Test exception"));

    // Act
    BigDecimal result = LiquidityUtils.calculateLiquidity(mockPool, mockPosition);

    // Assert
    assertNotNull(result);
    assertEquals(0, BigDecimal.ZERO.compareTo(result)); // Khi có exception, return BigDecimal.ZERO
  }

  @Test
  @DisplayName("Test calculateLiquidityForAmounts khi tick hiện tại nhỏ hơn tick dưới")
  public void testCalculateLiquidityForAmounts_CurrentBelowLower() {
    // Arrange
    sqrtRatioCurrentTick = BigDecimal.valueOf(0.5); // < sqrtRatioLowerTick

    // Act
    BigDecimal result = LiquidityUtils.calculateLiquidityForAmounts(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        amount0,
        amount1);

    // Assert
    assertNotNull(result);
    // Khi tick hiện tại < tick dưới, thanh khoản dựa vào amount0
    BigDecimal expected = amount0
        .multiply(sqrtRatioLowerTick)
        .multiply(sqrtRatioUpperTick)
        .divide(sqrtRatioUpperTick.subtract(sqrtRatioLowerTick), AmmPoolConfig.DECIMAL_SCALE,
            AmmPoolConfig.ROUNDING_MODE);
    assertEquals(0, expected.compareTo(result));
  }

  @Test
  @DisplayName("Test calculateLiquidityForAmounts khi tick hiện tại lớn hơn tick trên")
  public void testCalculateLiquidityForAmounts_CurrentAboveUpper() {
    // Arrange
    sqrtRatioCurrentTick = BigDecimal.valueOf(2.5); // > sqrtRatioUpperTick

    // Act
    BigDecimal result = LiquidityUtils.calculateLiquidityForAmounts(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        amount0,
        amount1);

    // Assert
    assertNotNull(result);
    // Khi tick hiện tại > tick trên, thanh khoản dựa vào amount1
    BigDecimal expected = amount1
        .divide(sqrtRatioUpperTick.subtract(sqrtRatioLowerTick), AmmPoolConfig.DECIMAL_SCALE,
            AmmPoolConfig.ROUNDING_MODE);
    assertEquals(0, expected.compareTo(result));
  }

  @Test
  @DisplayName("Test calculateLiquidityForAmounts khi tick hiện tại nằm giữa tick dưới và trên")
  public void testCalculateLiquidityForAmounts_CurrentBetweenLowerAndUpper() {
    // Act
    BigDecimal result = LiquidityUtils.calculateLiquidityForAmounts(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        amount0,
        amount1);

    // Assert
    assertNotNull(result);

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

    // Lấy giá trị nhỏ hơn
    BigDecimal expected = liquidity0.min(liquidity1);
    assertEquals(0, expected.compareTo(result));
  }

  @Test
  @DisplayName("Test calculateLiquidityForAmounts khi xảy ra ngoại lệ")
  public void testCalculateLiquidityForAmounts_Exception() {
    // Arrange - gây ra lỗi chia cho 0
    sqrtRatioLowerTick = sqrtRatioUpperTick;

    // Act
    BigDecimal result = LiquidityUtils.calculateLiquidityForAmounts(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        amount0,
        amount1);

    // Assert
    assertNotNull(result);
    assertEquals(0, BigDecimal.ZERO.compareTo(result));
  }

  @Test
  @DisplayName("Test getAmountsForLiquidity khi tick hiện tại nhỏ hơn tick dưới")
  public void testGetAmountsForLiquidity_CurrentBelowLower() {
    // Arrange
    sqrtRatioCurrentTick = BigDecimal.valueOf(0.5); // < sqrtRatioLowerTick

    // Act
    BigDecimal[] result = LiquidityUtils.getAmountsForLiquidity(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        liquidity);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);

    // Khi tick hiện tại <= tick dưới, chỉ tính token0
    BigDecimal expectedAmount0 = liquidity
        .multiply(sqrtRatioUpperTick.subtract(sqrtRatioLowerTick))
        .divide(sqrtRatioLowerTick.multiply(sqrtRatioUpperTick), AmmPoolConfig.DECIMAL_SCALE,
            AmmPoolConfig.ROUNDING_MODE);
    BigDecimal expectedAmount1 = BigDecimal.ZERO;

    assertEquals(0, expectedAmount0.compareTo(result[0]));
    assertEquals(0, expectedAmount1.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test getAmountsForLiquidity khi tick hiện tại lớn hơn tick trên")
  public void testGetAmountsForLiquidity_CurrentAboveUpper() {
    // Arrange
    sqrtRatioCurrentTick = BigDecimal.valueOf(2.5); // > sqrtRatioUpperTick

    // Act
    BigDecimal[] result = LiquidityUtils.getAmountsForLiquidity(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        liquidity);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);

    // Khi tick hiện tại >= tick trên, chỉ tính token1
    BigDecimal expectedAmount0 = BigDecimal.ZERO;
    BigDecimal expectedAmount1 = liquidity
        .multiply(sqrtRatioUpperTick.subtract(sqrtRatioLowerTick));

    assertEquals(0, expectedAmount0.compareTo(result[0]));
    assertEquals(0, expectedAmount1.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test getAmountsForLiquidity khi tick hiện tại nằm giữa tick dưới và trên")
  public void testGetAmountsForLiquidity_CurrentBetweenLowerAndUpper() {
    // Act
    BigDecimal[] result = LiquidityUtils.getAmountsForLiquidity(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        liquidity);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);

    // Tính token0
    BigDecimal expectedAmount0 = liquidity
        .multiply(sqrtRatioUpperTick.subtract(sqrtRatioCurrentTick))
        .divide(sqrtRatioCurrentTick.multiply(sqrtRatioUpperTick), AmmPoolConfig.DECIMAL_SCALE,
            AmmPoolConfig.ROUNDING_MODE);

    // Tính token1
    BigDecimal expectedAmount1 = liquidity
        .multiply(sqrtRatioCurrentTick.subtract(sqrtRatioLowerTick));

    assertEquals(0, expectedAmount0.compareTo(result[0]));
    assertEquals(0, expectedAmount1.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test getAmountsForLiquidity khi xảy ra ngoại lệ do chia cho 0")
  public void testGetAmountsForLiquidity_Exception() {
    // Arrange - gây ra lỗi chia cho 0
    sqrtRatioLowerTick = sqrtRatioUpperTick;

    // Act
    BigDecimal[] result = LiquidityUtils.getAmountsForLiquidity(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        liquidity);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0]));
    assertEquals(0, BigDecimal.ZERO.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test getAmountsForLiquidity khi xảy ra NullPointerException")
  public void testGetAmountsForLiquidity_NullPointerException() {
    // Act
    BigDecimal[] result = LiquidityUtils.getAmountsForLiquidity(
        null, // null để kích hoạt NullPointerException
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        liquidity);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0]));
    assertEquals(0, BigDecimal.ZERO.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test getAmountsForLiquidity với các tham số đặc biệt")
  public void testGetAmountsForLiquidity_SpecialCases() {
    // Arrange - liquidity là 0
    BigDecimal zeroLiquidity = BigDecimal.ZERO;

    // Act
    BigDecimal[] result = LiquidityUtils.getAmountsForLiquidity(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        zeroLiquidity);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0]));
    assertEquals(0, BigDecimal.ZERO.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test getAmountsForLiquidity với giá trị âm")
  public void testGetAmountsForLiquidity_NegativeValues() {
    // Arrange - tạo tình huống đặc biệt có thể gây ra giá trị âm
    BigDecimal negativeLiquidity = BigDecimal.valueOf(-50);

    // Act
    BigDecimal[] result = LiquidityUtils.getAmountsForLiquidity(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        negativeLiquidity);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);

    // Kiểm tra kết quả có thể âm (tuỳ vào logic của phương thức)
    // Nếu liquidity âm thì kết quả cũng sẽ âm theo tỷ lệ
    BigDecimal expectedAmount0 = negativeLiquidity
        .multiply(sqrtRatioUpperTick.subtract(sqrtRatioCurrentTick))
        .divide(sqrtRatioCurrentTick.multiply(sqrtRatioUpperTick), AmmPoolConfig.DECIMAL_SCALE,
            AmmPoolConfig.ROUNDING_MODE);

    BigDecimal expectedAmount1 = negativeLiquidity
        .multiply(sqrtRatioCurrentTick.subtract(sqrtRatioLowerTick));

    assertEquals(0, expectedAmount0.compareTo(result[0]));
    assertEquals(0, expectedAmount1.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test calculateFees")
  public void testCalculateFees() {
    // Arrange
    BigDecimal liquidity = BigDecimal.valueOf(1000);
    double feePercentage = 0.003; // 0.3%
    long timePeriod = 86400000; // 1 day in ms

    when(mockPosition.getLiquidity()).thenReturn(liquidity);
    when(mockPool.getFeePercentage()).thenReturn(feePercentage);

    // Act
    BigDecimal[] result = LiquidityUtils.calculateFees(mockPosition, mockPool, timePeriod);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(result[0].compareTo(BigDecimal.ZERO), 1); // fee0 > 0
    assertEquals(result[1].compareTo(BigDecimal.ZERO), 1); // fee1 > 0
  }

  @Test
  @DisplayName("Test calculateFees khi xảy ra ngoại lệ")
  public void testCalculateFees_Exception() {
    // Arrange
    when(mockPosition.getLiquidity()).thenThrow(new RuntimeException("Test exception"));

    // Act
    BigDecimal[] result = LiquidityUtils.calculateFees(mockPosition, mockPool, 86400000);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0]));
    assertEquals(0, BigDecimal.ZERO.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test getFeeGrowthInside khi tick hiện tại nằm giữa lower và upper")
  public void testGetFeeGrowthInside_CurrentBetweenLowerAndUpper() {
    // Arrange
    int currentTick = 5;
    BigDecimal feeGrowthGlobal0 = BigDecimal.valueOf(100);
    BigDecimal feeGrowthGlobal1 = BigDecimal.valueOf(200);

    when(mockLowerTick.getTickIndex()).thenReturn(1);
    when(mockUpperTick.getTickIndex()).thenReturn(10);
    when(mockLowerTick.getFeeGrowthOutside0()).thenReturn(BigDecimal.valueOf(20));
    when(mockLowerTick.getFeeGrowthOutside1()).thenReturn(BigDecimal.valueOf(30));
    when(mockUpperTick.getFeeGrowthOutside0()).thenReturn(BigDecimal.valueOf(40));
    when(mockUpperTick.getFeeGrowthOutside1()).thenReturn(BigDecimal.valueOf(50));

    // Act
    BigDecimal[] result = LiquidityUtils.getFeeGrowthInside(
        mockLowerTick,
        mockUpperTick,
        currentTick,
        feeGrowthGlobal0,
        feeGrowthGlobal1);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);

    // currentTick >= lowerTick.getTickIndex() - lấy feeGrowthOutside cực bên dưới
    BigDecimal feeGrowthBelow0 = BigDecimal.valueOf(20);
    BigDecimal feeGrowthBelow1 = BigDecimal.valueOf(30);

    // currentTick < upperTick.getTickIndex() - lấy feeGrowthOutside cực bên trên
    BigDecimal feeGrowthAbove0 = BigDecimal.valueOf(40);
    BigDecimal feeGrowthAbove1 = BigDecimal.valueOf(50);

    BigDecimal expectedFeeGrowthInside0 = feeGrowthGlobal0.subtract(feeGrowthBelow0).subtract(feeGrowthAbove0);
    BigDecimal expectedFeeGrowthInside1 = feeGrowthGlobal1.subtract(feeGrowthBelow1).subtract(feeGrowthAbove1);

    assertArrayEquals(new BigDecimal[] { expectedFeeGrowthInside0, expectedFeeGrowthInside1 }, result);
  }

  @Test
  @DisplayName("Test getFeeGrowthInside khi tick hiện tại nhỏ hơn lower tick")
  public void testGetFeeGrowthInside_CurrentBelowLower() {
    // Arrange
    int currentTick = 0;
    BigDecimal feeGrowthGlobal0 = BigDecimal.valueOf(100);
    BigDecimal feeGrowthGlobal1 = BigDecimal.valueOf(200);

    when(mockLowerTick.getTickIndex()).thenReturn(1);
    when(mockUpperTick.getTickIndex()).thenReturn(10);
    when(mockLowerTick.getFeeGrowthOutside0()).thenReturn(BigDecimal.valueOf(20));
    when(mockLowerTick.getFeeGrowthOutside1()).thenReturn(BigDecimal.valueOf(30));
    when(mockUpperTick.getFeeGrowthOutside0()).thenReturn(BigDecimal.valueOf(40));
    when(mockUpperTick.getFeeGrowthOutside1()).thenReturn(BigDecimal.valueOf(50));

    // Act
    BigDecimal[] result = LiquidityUtils.getFeeGrowthInside(
        mockLowerTick,
        mockUpperTick,
        currentTick,
        feeGrowthGlobal0,
        feeGrowthGlobal1);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);

    // currentTick < lowerTick.getTickIndex() - tính feeGrowthBelow từ
    // feeGrowthGlobal
    BigDecimal feeGrowthBelow0 = feeGrowthGlobal0.subtract(BigDecimal.valueOf(20));
    BigDecimal feeGrowthBelow1 = feeGrowthGlobal1.subtract(BigDecimal.valueOf(30));

    // currentTick < upperTick.getTickIndex() - lấy feeGrowthOutside cực bên trên
    BigDecimal feeGrowthAbove0 = BigDecimal.valueOf(40);
    BigDecimal feeGrowthAbove1 = BigDecimal.valueOf(50);

    // Tính giá trị trung gian
    BigDecimal intermediateFeeGrowthInside0 = feeGrowthGlobal0.subtract(feeGrowthBelow0).subtract(feeGrowthAbove0);
    BigDecimal intermediateFeeGrowthInside1 = feeGrowthGlobal1.subtract(feeGrowthBelow1).subtract(feeGrowthAbove1);

    // Đối với Uniswap V3, giá trị feeGrowth không được âm
    BigDecimal expectedFeeGrowthInside0 = intermediateFeeGrowthInside0.max(BigDecimal.ZERO);
    BigDecimal expectedFeeGrowthInside1 = intermediateFeeGrowthInside1.max(BigDecimal.ZERO);

    assertArrayEquals(new BigDecimal[] { expectedFeeGrowthInside0, expectedFeeGrowthInside1 }, result);
  }

  @Test
  @DisplayName("Test getFeeGrowthInside khi tick hiện tại lớn hơn upper tick")
  public void testGetFeeGrowthInside_CurrentAboveUpper() {
    // Arrange
    int currentTick = 15;
    BigDecimal feeGrowthGlobal0 = BigDecimal.valueOf(100);
    BigDecimal feeGrowthGlobal1 = BigDecimal.valueOf(200);

    when(mockLowerTick.getTickIndex()).thenReturn(1);
    when(mockUpperTick.getTickIndex()).thenReturn(10);
    when(mockLowerTick.getFeeGrowthOutside0()).thenReturn(BigDecimal.valueOf(20));
    when(mockLowerTick.getFeeGrowthOutside1()).thenReturn(BigDecimal.valueOf(30));
    when(mockUpperTick.getFeeGrowthOutside0()).thenReturn(BigDecimal.valueOf(40));
    when(mockUpperTick.getFeeGrowthOutside1()).thenReturn(BigDecimal.valueOf(50));

    // Act
    BigDecimal[] result = LiquidityUtils.getFeeGrowthInside(
        mockLowerTick,
        mockUpperTick,
        currentTick,
        feeGrowthGlobal0,
        feeGrowthGlobal1);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);

    // currentTick >= lowerTick.getTickIndex() - lấy feeGrowthOutside cực bên dưới
    BigDecimal feeGrowthBelow0 = BigDecimal.valueOf(20);
    BigDecimal feeGrowthBelow1 = BigDecimal.valueOf(30);

    // currentTick >= upperTick.getTickIndex() - tính feeGrowthAbove từ
    // feeGrowthGlobal
    BigDecimal feeGrowthAbove0 = feeGrowthGlobal0.subtract(BigDecimal.valueOf(40));
    BigDecimal feeGrowthAbove1 = feeGrowthGlobal1.subtract(BigDecimal.valueOf(50));

    BigDecimal expectedFeeGrowthInside0 = feeGrowthGlobal0.subtract(feeGrowthBelow0).subtract(feeGrowthAbove0);
    BigDecimal expectedFeeGrowthInside1 = feeGrowthGlobal1.subtract(feeGrowthBelow1).subtract(feeGrowthAbove1);

    assertArrayEquals(new BigDecimal[] { expectedFeeGrowthInside0, expectedFeeGrowthInside1 }, result);
  }

  @Test
  @DisplayName("Test getFeeGrowthInside khi xảy ra ngoại lệ")
  public void testGetFeeGrowthInside_Exception() {
    // Arrange
    when(mockLowerTick.getTickIndex()).thenThrow(new RuntimeException("Test exception"));

    // Act
    BigDecimal[] result = LiquidityUtils.getFeeGrowthInside(
        mockLowerTick,
        mockUpperTick,
        5,
        BigDecimal.valueOf(100),
        BigDecimal.valueOf(200));

    // Assert
    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(0, BigDecimal.ZERO.compareTo(result[0]));
    assertEquals(0, BigDecimal.ZERO.compareTo(result[1]));
  }

  @Test
  @DisplayName("Test calculateFeesOwed")
  public void testCalculateFeesOwed() {
    // Arrange
    BigDecimal liquidity = BigDecimal.valueOf(1000);
    BigDecimal feeGrowthInside = BigDecimal.valueOf(50);
    BigDecimal feeGrowthInsideLast = BigDecimal.valueOf(20);

    // Act
    BigDecimal result = LiquidityUtils.calculateFeesOwed(
        liquidity,
        feeGrowthInside,
        feeGrowthInsideLast);

    // Assert
    assertNotNull(result);
    BigDecimal expectedFeesOwed = liquidity.multiply(feeGrowthInside.subtract(feeGrowthInsideLast))
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    assertEquals(0, expectedFeesOwed.compareTo(result));
  }

  @Test
  @DisplayName("Test calculateFeesOwed khi không có phí phát sinh")
  public void testCalculateFeesOwed_NoFeeAccrued() {
    // Arrange
    BigDecimal liquidity = BigDecimal.valueOf(1000);
    BigDecimal feeGrowthInside = BigDecimal.valueOf(20);
    BigDecimal feeGrowthInsideLast = BigDecimal.valueOf(20);

    // Act
    BigDecimal result = LiquidityUtils.calculateFeesOwed(
        liquidity,
        feeGrowthInside,
        feeGrowthInsideLast);

    // Assert
    assertNotNull(result);
    assertEquals(0, BigDecimal.ZERO.compareTo(result));
  }

  @Test
  @DisplayName("Test calculateFeesOwed khi feeGrowthInside nhỏ hơn feeGrowthInsideLast")
  public void testCalculateFeesOwed_NegativeFeeGrowth() {
    // Arrange
    BigDecimal liquidity = BigDecimal.valueOf(1000);
    BigDecimal feeGrowthInside = BigDecimal.valueOf(10); // Nhỏ hơn feeGrowthInsideLast
    BigDecimal feeGrowthInsideLast = BigDecimal.valueOf(20);

    // Act
    BigDecimal result = LiquidityUtils.calculateFeesOwed(
        liquidity,
        feeGrowthInside,
        feeGrowthInsideLast);

    // Assert
    assertNotNull(result);
    assertEquals(0, BigDecimal.ZERO.compareTo(result)); // Khi delta âm, return BigDecimal.ZERO
  }

  @Test
  @DisplayName("Test calculateFeesOwed khi xảy ra ngoại lệ")
  public void testCalculateFeesOwed_Exception() {
    // Arrange
    BigDecimal liquidity = null; // Gây ra NullPointerException

    // Act
    BigDecimal result = LiquidityUtils.calculateFeesOwed(
        liquidity,
        BigDecimal.valueOf(50),
        BigDecimal.valueOf(20));

    // Assert
    assertNotNull(result);
    assertEquals(0, BigDecimal.ZERO.compareTo(result));
  }
}
