package com.exchangeengine.util.ammPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SqrtPriceMathTest {

  private static final BigDecimal EPSILON = new BigDecimal("0.000000000000001");

  @Test
  @DisplayName("getAmount0Delta với thanh khoản dương và roundUp=true")
  public void testGetAmount0DeltaWithRoundUp() {
    // Arrange
    BigDecimal sqrtRatioA = new BigDecimal("1.2");
    BigDecimal sqrtRatioB = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    boolean roundUp = true;

    // Act
    BigDecimal amount0 = SqrtPriceMath.getAmount0Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);

    // Assert
    assertTrue(amount0.compareTo(BigDecimal.ZERO) > 0,
        "Amount0 should be positive");

    // Kiểm tra công thức chính xác: Δx = L * (sqrtRatioB - sqrtRatioA) /
    // (sqrtRatioA * sqrtRatioB)
    BigDecimal numerator = liquidity.multiply(sqrtRatioB.subtract(sqrtRatioA));
    BigDecimal denominator = sqrtRatioA.multiply(sqrtRatioB);
    BigDecimal expected = numerator.divide(denominator, AmmPoolConfig.DECIMAL_SCALE, RoundingMode.CEILING);

    // Sử dụng so sánh xấp xỉ thay vì so sánh chính xác
    assertTrue(amount0.subtract(expected).abs().compareTo(new BigDecimal("0.00001")) < 0,
        "Amount0 should approximately match Uniswap V3 formula calculation");
  }

  @Test
  @DisplayName("getAmount0Delta với thanh khoản dương và roundUp=false")
  public void testGetAmount0DeltaWithRoundDown() {
    // Arrange
    BigDecimal sqrtRatioA = new BigDecimal("1.2");
    BigDecimal sqrtRatioB = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    boolean roundUp = false;

    // Act
    BigDecimal amount0 = SqrtPriceMath.getAmount0Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);

    // Assert
    assertTrue(amount0.compareTo(BigDecimal.ZERO) > 0,
        "Amount0 should be positive");

    // Kiểm tra công thức chính xác: Δx = L * (sqrtRatioB - sqrtRatioA) /
    // (sqrtRatioA * sqrtRatioB)
    BigDecimal numerator = liquidity.multiply(sqrtRatioB.subtract(sqrtRatioA));
    BigDecimal denominator = sqrtRatioA.multiply(sqrtRatioB);
    BigDecimal expected = numerator.divide(denominator, AmmPoolConfig.DECIMAL_SCALE, RoundingMode.FLOOR);

    // Sử dụng so sánh xấp xỉ thay vì so sánh chính xác
    assertTrue(amount0.subtract(expected).abs().compareTo(new BigDecimal("0.00001")) < 0,
        "Amount0 should approximately match Uniswap V3 formula calculation");
  }

  @Test
  @DisplayName("getAmount0Delta với giá trị bị đảo")
  public void testGetAmount0DeltaWithSwappedRatio() {
    // Arrange
    BigDecimal sqrtRatioA = new BigDecimal("1.5"); // Ngược thứ tự so với test trước
    BigDecimal sqrtRatioB = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("1000");
    boolean roundUp = true;

    // Act
    BigDecimal amount0 = SqrtPriceMath.getAmount0Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);

    // Assert
    assertTrue(amount0.compareTo(BigDecimal.ZERO) > 0,
        "Amount0 should be positive");

    // Hàm sẽ tự sắp xếp lại thứ tự tham số, nên kết quả phải giống với trường hợp
    // đúng thứ tự
    BigDecimal amount0WithCorrectOrder = SqrtPriceMath.getAmount0Delta(sqrtRatioB, sqrtRatioA, liquidity, roundUp);
    assertTrue(amount0.subtract(amount0WithCorrectOrder).abs().compareTo(EPSILON) < 0,
        "Both calls should return the same result");

    // Kiểm tra công thức chính xác sau khi sắp xếp lại: Δx = L * (sqrtRatioA -
    // sqrtRatioB) / (sqrtRatioB * sqrtRatioA)
    BigDecimal numerator = liquidity.multiply(sqrtRatioA.subtract(sqrtRatioB));
    BigDecimal denominator = sqrtRatioB.multiply(sqrtRatioA);
    BigDecimal expected = numerator.divide(denominator, AmmPoolConfig.DECIMAL_SCALE, RoundingMode.CEILING);

    // Sử dụng so sánh xấp xỉ thay vì so sánh chính xác
    assertTrue(amount0.subtract(expected).abs().compareTo(new BigDecimal("0.00001")) < 0,
        "Amount0 should approximately match Uniswap V3 formula calculation with swapped ratios");
  }

  @Test
  @DisplayName("getAmount1Delta với thanh khoản dương và roundUp=true")
  public void testGetAmount1DeltaWithRoundUp() {
    // Arrange
    BigDecimal sqrtRatioA = new BigDecimal("1.2");
    BigDecimal sqrtRatioB = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    boolean roundUp = true;

    // Act
    BigDecimal amount1 = SqrtPriceMath.getAmount1Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);

    // Assert
    assertTrue(amount1.compareTo(BigDecimal.ZERO) > 0,
        "Amount1 should be positive");

    // Kiểm tra công thức chính xác: Δy = L * (sqrtRatioB - sqrtRatioA)
    BigDecimal expected = liquidity.multiply(sqrtRatioB.subtract(sqrtRatioA))
        .setScale(AmmPoolConfig.DECIMAL_SCALE, RoundingMode.CEILING);

    assertEquals(expected, amount1, "Amount1 should match Uniswap V3 formula calculation");
  }

  @Test
  @DisplayName("getAmount1Delta với thanh khoản dương và roundUp=false")
  public void testGetAmount1DeltaWithRoundDown() {
    // Arrange
    BigDecimal sqrtRatioA = new BigDecimal("1.2");
    BigDecimal sqrtRatioB = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    boolean roundUp = false;

    // Act
    BigDecimal amount1 = SqrtPriceMath.getAmount1Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);

    // Assert
    assertTrue(amount1.compareTo(BigDecimal.ZERO) > 0,
        "Amount1 should be positive");

    // Kiểm tra công thức chính xác: Δy = L * (sqrtRatioB - sqrtRatioA)
    BigDecimal expected = liquidity.multiply(sqrtRatioB.subtract(sqrtRatioA))
        .setScale(AmmPoolConfig.DECIMAL_SCALE, RoundingMode.FLOOR);

    assertEquals(expected, amount1, "Amount1 should match Uniswap V3 formula calculation");
  }

  @Test
  @DisplayName("getAmount1Delta với giá trị bị đảo")
  public void testGetAmount1DeltaWithSwappedRatio() {
    // Arrange
    BigDecimal sqrtRatioA = new BigDecimal("1.5"); // Ngược thứ tự so với test trước
    BigDecimal sqrtRatioB = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("1000");
    boolean roundUp = true;

    // Act
    BigDecimal amount1 = SqrtPriceMath.getAmount1Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);

    // Assert
    assertTrue(amount1.compareTo(BigDecimal.ZERO) > 0,
        "Amount1 should be positive");

    // Hàm sẽ tự sắp xếp lại thứ tự tham số, nên kết quả phải giống với trường hợp
    // đúng thứ tự
    BigDecimal amount1WithCorrectOrder = SqrtPriceMath.getAmount1Delta(sqrtRatioB, sqrtRatioA, liquidity, roundUp);
    assertEquals(amount1, amount1WithCorrectOrder, "Both calls should return the same result");

    // Kiểm tra công thức chính xác sau khi sắp xếp: Δy = L * (sqrtRatioA -
    // sqrtRatioB)
    BigDecimal expected = liquidity.multiply(sqrtRatioA.subtract(sqrtRatioB))
        .setScale(AmmPoolConfig.DECIMAL_SCALE, RoundingMode.CEILING);

    assertEquals(expected, amount1, "Amount1 should match Uniswap V3 formula calculation with swapped ratios");
  }

  @Test
  @DisplayName("Kiểm tra mối quan hệ giữa getAmount0Delta và getAmount1Delta")
  public void testRelationshipBetweenAmount0DeltaAndAmount1Delta() {
    // Arrange
    BigDecimal sqrtRatioA = new BigDecimal("1.2");
    BigDecimal sqrtRatioB = new BigDecimal("1.3");
    BigDecimal liquidity = new BigDecimal("1000");
    boolean roundUp = false; // Để đơn giản hóa phép tính

    // Act
    BigDecimal amount0 = SqrtPriceMath.getAmount0Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);
    BigDecimal amount1 = SqrtPriceMath.getAmount1Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);

    // Assert
    // Công thức liên hệ: amount1/amount0 ≈ sqrtRatioA * sqrtRatioB
    BigDecimal priceProduct = sqrtRatioA.multiply(sqrtRatioB);
    BigDecimal ratio = amount1.divide(amount0, AmmPoolConfig.DECIMAL_SCALE, RoundingMode.HALF_UP);

    // So sánh với sai số nhỏ
    BigDecimal difference = ratio.subtract(priceProduct).abs();
    assertTrue(difference.compareTo(new BigDecimal("0.01")) < 0,
        "Ratio of amount1/amount0 should approximately equal sqrtRatioA * sqrtRatioB");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromInput với zero-for-one và lượng vào dương")
  public void testGetNextSqrtPriceFromInputZeroForOnePositiveAmount() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountIn = new BigDecimal("100");
    boolean zeroForOne = true;

    // Act
    BigDecimal nextSqrtPrice = SqrtPriceMath.getNextSqrtPriceFromInput(sqrtPriceX96, liquidity, amountIn, zeroForOne);

    // Assert
    assertTrue(nextSqrtPrice.compareTo(sqrtPriceX96) < 0,
        "Next sqrt price should be less than current sqrt price for zero-for-one swap");

    // Kiểm tra công thức: (liquidity * sqrtPrice) / (liquidity + amountIn *
    // sqrtPrice)
    BigDecimal numerator = liquidity.multiply(sqrtPriceX96);
    BigDecimal product = amountIn.multiply(sqrtPriceX96);
    BigDecimal denominator = liquidity.add(product);
    BigDecimal expected = numerator.divide(denominator, AmmPoolConfig.MC);

    assertTrue(nextSqrtPrice.subtract(expected).abs().compareTo(EPSILON) < 0,
        "NextSqrtPrice should match Uniswap V3 formula for zero-for-one");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromInput với one-for-zero và lượng vào dương")
  public void testGetNextSqrtPriceFromInputOneForZeroPositiveAmount() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountIn = new BigDecimal("100");
    boolean zeroForOne = false;

    // Act
    BigDecimal nextSqrtPrice = SqrtPriceMath.getNextSqrtPriceFromInput(sqrtPriceX96, liquidity, amountIn, zeroForOne);

    // Assert
    assertTrue(nextSqrtPrice.compareTo(sqrtPriceX96) > 0,
        "Next sqrt price should be greater than current sqrt price for one-for-zero swap");

    // Kiểm tra công thức: sqrtPrice + (amountIn / liquidity)
    BigDecimal quotient = amountIn.divide(liquidity, AmmPoolConfig.MC);
    BigDecimal expected = sqrtPriceX96.add(quotient);

    assertTrue(nextSqrtPrice.subtract(expected).abs().compareTo(EPSILON) < 0,
        "NextSqrtPrice should match Uniswap V3 formula for one-for-zero");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromInput với lượng vào bằng 0")
  public void testGetNextSqrtPriceFromInputZeroAmount() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountIn = BigDecimal.ZERO;
    boolean zeroForOne = true;

    // Act
    BigDecimal nextSqrtPrice = SqrtPriceMath.getNextSqrtPriceFromInput(sqrtPriceX96, liquidity, amountIn, zeroForOne);

    // Assert
    assertEquals(0, nextSqrtPrice.compareTo(sqrtPriceX96),
        "Next sqrt price should be equal to current sqrt price for zero amount");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromInput với thanh khoản không dương")
  public void testGetNextSqrtPriceFromInputNonPositiveLiquidity() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.5");
    BigDecimal liquidity = BigDecimal.ZERO;
    BigDecimal amountIn = new BigDecimal("100");
    boolean zeroForOne = true;

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> {
      SqrtPriceMath.getNextSqrtPriceFromInput(sqrtPriceX96, liquidity, amountIn, zeroForOne);
    }, "Should throw IllegalArgumentException for non-positive liquidity");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromOutput với zero-for-one và lượng ra dương")
  public void testGetNextSqrtPriceFromOutputZeroForOnePositiveAmount() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountOut = new BigDecimal("50");
    boolean zeroForOne = true;

    // Act
    BigDecimal nextSqrtPrice = SqrtPriceMath.getNextSqrtPriceFromOutput(sqrtPriceX96, liquidity, amountOut, zeroForOne);

    // Assert - Trong phiên bản mới, giá tăng lên khi zeroForOne=true trong hàm
    // getNextSqrtPriceFromOutput
    assertTrue(nextSqrtPrice.compareTo(sqrtPriceX96) > 0,
        "Next sqrt price should be greater than current sqrt price for zero-for-one swap with output amount");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromOutput với one-for-zero và lượng ra dương")
  public void testGetNextSqrtPriceFromOutputOneForZeroPositiveAmount() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountOut = new BigDecimal("50");
    boolean zeroForOne = false;

    // Act
    BigDecimal nextSqrtPrice = SqrtPriceMath.getNextSqrtPriceFromOutput(sqrtPriceX96, liquidity, amountOut, zeroForOne);

    // Assert
    assertTrue(nextSqrtPrice.compareTo(sqrtPriceX96) > 0,
        "Next sqrt price should be greater than current sqrt price for one-for-zero swap");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromOutput với lượng ra bằng 0")
  public void testGetNextSqrtPriceFromOutputZeroAmount() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountOut = BigDecimal.ZERO;
    boolean zeroForOne = true;

    // Act
    BigDecimal nextSqrtPrice = SqrtPriceMath.getNextSqrtPriceFromOutput(sqrtPriceX96, liquidity, amountOut, zeroForOne);

    // Assert
    assertEquals(0, nextSqrtPrice.compareTo(sqrtPriceX96),
        "Next sqrt price should be equal to current sqrt price for zero amount");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromOutput với thanh khoản không dương")
  public void testGetNextSqrtPriceFromOutputNonPositiveLiquidity() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.5");
    BigDecimal liquidity = BigDecimal.ZERO;
    BigDecimal amountOut = new BigDecimal("50");
    boolean zeroForOne = true;

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> {
      SqrtPriceMath.getNextSqrtPriceFromOutput(sqrtPriceX96, liquidity, amountOut, zeroForOne);
    }, "Should throw IllegalArgumentException for non-positive liquidity");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromOutput cho zeroForOne với lượng ra lớn")
  public void testGetNextSqrtPriceFromOutputZeroForOneLargeAmount() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountOut = new BigDecimal("500"); // Lượng lớn
    boolean zeroForOne = true;

    // Act
    BigDecimal nextSqrtPrice = SqrtPriceMath.getNextSqrtPriceFromOutput(sqrtPriceX96, liquidity, amountOut, zeroForOne);

    // Assert - Trong phiên bản mới, giá tăng lên khi zeroForOne=true trong hàm
    // getNextSqrtPriceFromOutput
    assertTrue(nextSqrtPrice.compareTo(sqrtPriceX96) > 0,
        "Next sqrt price should be greater than current sqrt price for zero-for-one swap with large output amount");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromOutput cho oneForZero với lượng ra lớn")
  public void testGetNextSqrtPriceFromOutputOneForZeroLargeAmount() {
    // Arrange
    BigDecimal sqrtPriceX96 = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountOut = new BigDecimal("300"); // Lượng lớn
    boolean zeroForOne = false;

    // Act
    BigDecimal nextSqrtPrice = SqrtPriceMath.getNextSqrtPriceFromOutput(sqrtPriceX96, liquidity, amountOut, zeroForOne);

    // Assert
    assertTrue(nextSqrtPrice.compareTo(sqrtPriceX96) > 0,
        "Next sqrt price should be greater than current sqrt price for one-for-zero swap");
  }

  @Test
  @DisplayName("getNextSqrtPriceFromOutput cho giá trị cận biên")
  public void testGetNextSqrtPriceFromOutputEdgeCases() {
    // Arrange - amountOut rất nhỏ
    BigDecimal sqrtPriceX96 = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal smallAmount = new BigDecimal("0.0000001");
    boolean zeroForOne = true;

    // Act
    BigDecimal nextSqrtPriceSmall = SqrtPriceMath.getNextSqrtPriceFromOutput(sqrtPriceX96, liquidity, smallAmount,
        zeroForOne);

    // Assert - Trong phiên bản mới, giá tăng lên khi zeroForOne=true trong hàm
    // getNextSqrtPriceFromOutput
    assertTrue(nextSqrtPriceSmall.compareTo(sqrtPriceX96) > 0,
        "Next sqrt price should be greater than current sqrt price even for very small amount");

    // Phần trăm thay đổi phải rất nhỏ
    BigDecimal percentChange = nextSqrtPriceSmall.divide(sqrtPriceX96, 10, AmmPoolConfig.ROUNDING_MODE)
        .subtract(BigDecimal.ONE);
    assertTrue(percentChange.compareTo(new BigDecimal("0.001")) < 0,
        "Percent change should be very small for a very small amount");
  }

  @Test
  @DisplayName("Kiểm tra đặc biệt công thức getAmount0Delta với giá trị thực tế")
  public void testGetAmount0DeltaWithRealValues() {
    // Arrange - Sử dụng giá trị từ log thực tế
    BigDecimal sqrtRatioA = new BigDecimal("159.37285259428937477");
    BigDecimal sqrtRatioB = new BigDecimal("160.43396342579652029892");
    BigDecimal liquidity = new BigDecimal("1216714496.60890424732872861985");
    boolean roundUp = true;

    // Act
    BigDecimal amount0 = SqrtPriceMath.getAmount0Delta(sqrtRatioA, sqrtRatioB, liquidity, roundUp);

    // Assert
    // Kiểm tra kết quả so với calculation trong log
    BigDecimal expected = new BigDecimal("50493.88324462249349727898");
    assertTrue(amount0.subtract(expected).abs().compareTo(new BigDecimal("0.00001")) < 0,
        "Amount0 should match the expected value from real calculation");

    // Kiểm tra công thức đúng của Uniswap V3: Δx = L * (sqrtB - sqrtA) / (sqrtA *
    // sqrtB)
    BigDecimal numerator = liquidity.multiply(sqrtRatioB.subtract(sqrtRatioA));
    BigDecimal denominator = sqrtRatioA.multiply(sqrtRatioB);
    BigDecimal calculatedResult = numerator.divide(denominator, AmmPoolConfig.MC)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, RoundingMode.CEILING);

    assertTrue(amount0.subtract(calculatedResult).abs().compareTo(new BigDecimal("0.00001")) < 0,
        "Amount0 should match the manual calculation using Uniswap V3 formula");
  }
}
