package com.exchangeengine.util.ammPool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.exchangeengine.extension.CombinedTestExtension;

@ExtendWith(CombinedTestExtension.class)
public class SwapMathTest {

  private static final BigDecimal EPSILON = new BigDecimal("0.001"); // Tăng độ sai số lên

  @Test
  @DisplayName("Kiểm tra computeSwapStep cho zero-to-one exactIn")
  public void testComputeSwapStepZeroToOneExactIn() {
    // Arrange
    BigDecimal sqrtRatioCurrent = new BigDecimal("1.5");
    BigDecimal sqrtRatioTarget = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountRemaining = new BigDecimal("1000"); // Tăng lượng token lên để đủ đạt giá mục tiêu
    double feePercentage = 0.003; // 0.3%

    // Act
    BigDecimal[] result = SwapMath.computeSwapStep(
        sqrtRatioCurrent,
        sqrtRatioTarget,
        liquidity,
        amountRemaining,
        feePercentage);

    // Assert
    BigDecimal sqrtRatioNext = result[0];
    BigDecimal amountIn = result[1];
    BigDecimal amountOut = result[2];
    BigDecimal feeAmount = result[3];

    // Kiểm tra hướng thay đổi giá (giá giảm khi zero-for-one)
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioCurrent) < 0,
        "SqrtRatioNext should be less than sqrtRatioCurrent for zero-for-one");

    // Kiểm tra kết quả có lợi nhuận (amountOut > 0)
    assertTrue(amountOut.compareTo(BigDecimal.ZERO) > 0,
        "AmountOut should be positive");

    // Kiểm tra feeAmount > 0
    assertTrue(feeAmount.compareTo(BigDecimal.ZERO) > 0,
        "FeeAmount should be positive");

    // Kiểm tra tổng token vào (amountIn + feeAmount) <= amountRemaining
    assertTrue(amountIn.add(feeAmount).compareTo(amountRemaining) <= 0,
        "AmountIn + feeAmount should be <= amountRemaining");
  }

  @Test
  @DisplayName("Kiểm tra computeSwapStep cho one-to-zero exactIn")
  public void testComputeSwapStepOneToZeroExactIn() {
    // Arrange
    BigDecimal sqrtRatioCurrent = new BigDecimal("1.2");
    BigDecimal sqrtRatioTarget = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountRemaining = new BigDecimal("1000"); // Tăng lượng token lên để đủ đạt giá mục tiêu
    double feePercentage = 0.003; // 0.3%

    // Act
    BigDecimal[] result = SwapMath.computeSwapStep(
        sqrtRatioCurrent,
        sqrtRatioTarget,
        liquidity,
        amountRemaining,
        feePercentage);

    // Assert
    BigDecimal sqrtRatioNext = result[0];
    BigDecimal amountIn = result[1];
    BigDecimal amountOut = result[2];
    BigDecimal feeAmount = result[3];

    // Kiểm tra hướng thay đổi giá (giá tăng khi one-for-zero)
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioCurrent) > 0,
        "SqrtRatioNext should be greater than sqrtRatioCurrent for one-for-zero");

    // Kiểm tra kết quả có lợi nhuận (amountOut > 0)
    assertTrue(amountOut.compareTo(BigDecimal.ZERO) > 0,
        "AmountOut should be positive");

    // Kiểm tra feeAmount > 0
    assertTrue(feeAmount.compareTo(BigDecimal.ZERO) > 0,
        "FeeAmount should be positive");
  }

  @Test
  @DisplayName("Kiểm tra computeSwapStep cho zero-to-one exactOut")
  public void testComputeSwapStepZeroToOneExactOut() {
    // Arrange
    BigDecimal sqrtRatioCurrent = new BigDecimal("1.5");
    BigDecimal sqrtRatioTarget = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountRemaining = new BigDecimal("-500"); // Âm cho exactOut, tăng lượng token
    double feePercentage = 0.003; // 0.3%

    // Act
    BigDecimal[] result = SwapMath.computeSwapStep(
        sqrtRatioCurrent,
        sqrtRatioTarget,
        liquidity,
        amountRemaining,
        feePercentage);

    // Assert
    BigDecimal sqrtRatioNext = result[0];
    BigDecimal amountIn = result[1];
    BigDecimal amountOut = result[2];
    BigDecimal feeAmount = result[3];

    // Kiểm tra hướng thay đổi giá (giá giảm khi zero-for-one)
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioCurrent) < 0,
        "SqrtRatioNext should be less than sqrtRatioCurrent for zero-for-one");

    // Kiểm tra amountIn > 0
    assertTrue(amountIn.compareTo(BigDecimal.ZERO) > 0,
        "AmountIn should be positive");

    // Kiểm tra amountOut > 0
    assertTrue(amountOut.compareTo(BigDecimal.ZERO) > 0,
        "AmountOut should be positive");

    // Kiểm tra feeAmount > 0
    assertTrue(feeAmount.compareTo(BigDecimal.ZERO) > 0,
        "FeeAmount should be positive");
  }

  @Test
  @DisplayName("Kiểm tra computeSwapStep cho one-to-zero exactOut")
  public void testComputeSwapStepOneToZeroExactOut() {
    // Arrange
    BigDecimal sqrtRatioCurrent = new BigDecimal("1.2");
    BigDecimal sqrtRatioTarget = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountRemaining = new BigDecimal("-500"); // Âm cho exactOut, tăng lượng token
    double feePercentage = 0.003; // 0.3%

    // Act
    BigDecimal[] result = SwapMath.computeSwapStep(
        sqrtRatioCurrent,
        sqrtRatioTarget,
        liquidity,
        amountRemaining,
        feePercentage);

    // Assert
    BigDecimal sqrtRatioNext = result[0];
    BigDecimal amountIn = result[1];
    BigDecimal amountOut = result[2];
    BigDecimal feeAmount = result[3];

    // Kiểm tra hướng thay đổi giá (giá tăng khi one-for-zero)
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioCurrent) > 0,
        "SqrtRatioNext should be greater than sqrtRatioCurrent for one-for-zero");

    // Kiểm tra amountIn > 0
    assertTrue(amountIn.compareTo(BigDecimal.ZERO) > 0,
        "AmountIn should be positive");

    // Kiểm tra amountOut > 0
    assertTrue(amountOut.compareTo(BigDecimal.ZERO) > 0,
        "AmountOut should be positive");

    // Kiểm tra feeAmount > 0
    assertTrue(feeAmount.compareTo(BigDecimal.ZERO) > 0,
        "FeeAmount should be positive");
  }

  @Test
  @DisplayName("Kiểm tra computeSwapStep với lượng token không đủ để đạt giá mục tiêu")
  public void testComputeSwapStepInsufficientAmount() {
    // Arrange
    BigDecimal sqrtRatioCurrent = new BigDecimal("1.5");
    BigDecimal sqrtRatioTarget = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("5000");
    BigDecimal amountRemaining = new BigDecimal("10"); // Không đủ để đạt giá mục tiêu
    double feePercentage = 0.003; // 0.3%

    // Act
    BigDecimal[] result = SwapMath.computeSwapStep(
        sqrtRatioCurrent,
        sqrtRatioTarget,
        liquidity,
        amountRemaining,
        feePercentage);

    // Assert
    BigDecimal sqrtRatioNext = result[0];
    BigDecimal amountIn = result[1];
    BigDecimal amountOut = result[2];
    BigDecimal feeAmount = result[3];

    // Kiểm tra sqrtRatioNext khác sqrtRatioTarget vì không đủ token
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioTarget) > 0,
        "SqrtRatioNext should be greater than sqrtRatioTarget");

    // Kiểm tra sqrtRatioNext < sqrtRatioCurrent (giá giảm khi zero-for-one)
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioCurrent) < 0,
        "SqrtRatioNext should be less than sqrtRatioCurrent");

    // Kiểm tra amountIn + feeAmount ≈ amountRemaining
    assertTrue(amountIn.add(feeAmount).subtract(amountRemaining).abs().compareTo(EPSILON) < 0,
        "AmountIn + feeAmount should be approximately equal to amountRemaining");
  }

  @Test
  @DisplayName("Kiểm tra computeSwapStep cho exactOutput khi amountRemaining nhỏ hơn amountOut")
  public void testComputeSwapStepExactOutputInsufficientRemaining() {
    // Arrange
    BigDecimal sqrtRatioCurrent = new BigDecimal("1.2");
    BigDecimal sqrtRatioTarget = new BigDecimal("1.5");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountRemaining = new BigDecimal("-50"); // Âm cho exactOut, nhỏ hơn amountOut tính được
    double feePercentage = 0.003; // 0.3%

    // Act
    BigDecimal[] result = SwapMath.computeSwapStep(
        sqrtRatioCurrent,
        sqrtRatioTarget,
        liquidity,
        amountRemaining,
        feePercentage);

    // Assert
    BigDecimal sqrtRatioNext = result[0];
    BigDecimal amountIn = result[1];
    BigDecimal amountOut = result[2];
    BigDecimal feeAmount = result[3];

    // Kiểm tra sqrtRatioNext nằm giữa sqrtRatioCurrent và sqrtRatioTarget
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioCurrent) > 0,
        "SqrtRatioNext should be greater than sqrtRatioCurrent");
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioTarget) < 0,
        "SqrtRatioNext should be less than sqrtRatioTarget");

    // Kiểm tra amountOut xấp xỉ với giá trị mong đợi
    BigDecimal expectedAmountOut = new BigDecimal("41.66666666666667");
    assertTrue(amountOut.subtract(expectedAmountOut).abs().compareTo(EPSILON) < 0,
        "AmountOut should be approximately equal to expected value");

    // Kiểm tra amountIn > 0
    assertTrue(amountIn.compareTo(BigDecimal.ZERO) > 0,
        "AmountIn should be positive");

    // Kiểm tra feeAmount > 0
    assertTrue(feeAmount.compareTo(BigDecimal.ZERO) > 0,
        "FeeAmount should be positive");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với slippage = null")
  public void testCheckSlippageWithNullSlippage() {
    // Arrange
    BigDecimal amount0 = new BigDecimal("100");
    BigDecimal amount1 = new BigDecimal("90");
    BigDecimal estimateAmount = new BigDecimal("95");
    boolean zeroForOne = true;
    boolean exactInput = true;
    BigDecimal slippage = null;

    // Act
    boolean result = SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert
    assertTrue(result, "Khi slippage = null, phương thức nên luôn trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = true, zeroForOne = true, output > minOutput")
  public void testCheckSlippageExactInputZeroForOneSuccessCase() {
    // Arrange
    BigDecimal amount0 = new BigDecimal("100"); // input
    BigDecimal amount1 = new BigDecimal("99"); // output
    BigDecimal estimateAmount = new BigDecimal("98"); // dự kiến
    boolean zeroForOne = true; // swap token0 -> token1
    boolean exactInput = true; // exact input
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    // minOutput = 98 * (1 - 0.01) = 97.02

    // Act
    boolean result = SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert
    assertTrue(result, "Với output (99) > minOutput (97.02), phương thức nên trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = true, zeroForOne = true, output = minOutput (edge case)")
  public void testCheckSlippageExactInputZeroForOneEdgeCase() {
    // Arrange
    BigDecimal amount0 = new BigDecimal("100"); // input
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    boolean zeroForOne = true; // swap token0 -> token1
    boolean exactInput = true; // exact input

    // Tính estimateAmount
    BigDecimal estimateAmount = new BigDecimal("100");

    // Tính minOutput chính xác: estimateAmount * (1 - slippage)
    BigDecimal minOutput = estimateAmount.multiply(
        BigDecimal.ONE.subtract(slippage))
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    // Act
    boolean result = SwapMath.checkSlippage(amount0, minOutput, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert - Khi output chính xác bằng minOutput thì phải pass
    assertTrue(result, "Với output = minOutput, phương thức nên trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = true, zeroForOne = true, output < minOutput")
  public void testCheckSlippageExactInputZeroForOneFailCase() {
    // Arrange
    BigDecimal amount0 = new BigDecimal("100"); // input
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    boolean zeroForOne = true; // swap token0 -> token1
    boolean exactInput = true; // exact input

    // Tính estimateAmount
    BigDecimal estimateAmount = new BigDecimal("100");

    // Tính minOutput: estimateAmount * (1 - slippage) = 100 * 0.99 = 99
    BigDecimal minOutput = estimateAmount.multiply(
        BigDecimal.ONE.subtract(slippage))
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    // Output nhỏ hơn minOutput
    BigDecimal actualOutput = minOutput.subtract(new BigDecimal("0.1")); // 98.9

    // Act
    boolean result = SwapMath.checkSlippage(amount0, actualOutput, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert - Khi output < minOutput thì phải fail
    assertFalse(result, "Với output < minOutput, phương thức nên trả về false");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = true, zeroForOne = false, output > minOutput")
  public void testCheckSlippageExactInputOneForZeroSuccessCase() {
    // Arrange
    BigDecimal amount0 = new BigDecimal("99"); // output (token0)
    BigDecimal amount1 = new BigDecimal("100"); // input (token1)
    BigDecimal estimateAmount = new BigDecimal("98"); // dự kiến
    boolean zeroForOne = false; // swap token1 -> token0
    boolean exactInput = true; // exact input
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    // minOutput = 98 * (1 - 0.01) = 97.02

    // Act
    boolean result = SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert
    assertTrue(result, "Với output (99) > minOutput (97.02), phương thức nên trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = true, zeroForOne = false, output < minOutput")
  public void testCheckSlippageExactInputOneForZeroFailCase() {
    // Arrange
    BigDecimal amount1 = new BigDecimal("100"); // input
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    boolean zeroForOne = false; // swap token1 -> token0
    boolean exactInput = true; // exact input

    // Tính estimateAmount
    BigDecimal estimateAmount = new BigDecimal("100");

    // Tính minOutput: estimateAmount * (1 - slippage) = 100 * 0.99 = 99
    BigDecimal minOutput = estimateAmount.multiply(
        BigDecimal.ONE.subtract(slippage))
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    // Output nhỏ hơn minOutput
    BigDecimal actualOutput = minOutput.subtract(new BigDecimal("0.1")); // 98.9

    // Act
    boolean result = SwapMath.checkSlippage(actualOutput, amount1, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert - Khi output < minOutput thì phải fail
    assertFalse(result, "Với output < minOutput, phương thức nên trả về false");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = false, zeroForOne = true, input < maxInput")
  public void testCheckSlippageExactOutputZeroForOneSuccessCase() {
    // Arrange
    BigDecimal amount0 = new BigDecimal("100"); // input (token0)
    BigDecimal amount1 = new BigDecimal("99"); // output (token1)
    BigDecimal estimateAmount = new BigDecimal("101"); // dự kiến
    boolean zeroForOne = true; // swap token0 -> token1
    boolean exactInput = false; // exact output
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    // maxInput = 101 * (1 + 0.01) = 102.01

    // Act
    boolean result = SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert
    assertTrue(result, "Với input (100) < maxInput (102.01), phương thức nên trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = false, zeroForOne = true, input > maxInput")
  public void testCheckSlippageExactOutputZeroForOneFailCase() {
    // Arrange
    BigDecimal output = new BigDecimal("100"); // output
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    boolean zeroForOne = true; // swap token0 -> token1
    boolean exactInput = false; // exact output

    // Tính estimateAmount
    BigDecimal estimateAmount = new BigDecimal("100");

    // Tính maxInput: estimateAmount / (1 - slippage) = 100 / 0.99 = 101.01
    BigDecimal maxInput = estimateAmount.divide(
        BigDecimal.ONE.subtract(slippage),
        AmmPoolConfig.DECIMAL_SCALE,
        AmmPoolConfig.ROUNDING_MODE);

    // Input lớn hơn maxInput
    BigDecimal actualInput = maxInput.add(new BigDecimal("0.1")); // 101.11

    // Act
    boolean result = SwapMath.checkSlippage(actualInput, output, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert - Khi input > maxInput thì phải fail
    assertFalse(result, "Với input > maxInput, phương thức nên trả về false");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = false, zeroForOne = false, input < maxInput")
  public void testCheckSlippageExactOutputOneForZeroSuccessCase() {
    // Arrange
    BigDecimal amount0 = new BigDecimal("99"); // output (token0)
    BigDecimal amount1 = new BigDecimal("100"); // input (token1)
    BigDecimal estimateAmount = new BigDecimal("101"); // dự kiến
    boolean zeroForOne = false; // swap token1 -> token0
    boolean exactInput = false; // exact output
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    // maxInput = 101 * (1 + 0.01) = 102.01

    // Act
    boolean result = SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert
    assertTrue(result, "Với input (100) < maxInput (102.01), phương thức nên trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = false, zeroForOne = false, input > maxInput")
  public void testCheckSlippageExactOutputOneForZeroFailCase() {
    // Arrange
    BigDecimal output = new BigDecimal("100"); // output
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage
    boolean zeroForOne = false; // swap token1 -> token0
    boolean exactInput = false; // exact output

    // Tính estimateAmount
    BigDecimal estimateAmount = new BigDecimal("100");

    // Tính maxInput: estimateAmount / (1 - slippage) = 100 / 0.99 = 101.01
    BigDecimal maxInput = estimateAmount.divide(
        BigDecimal.ONE.subtract(slippage),
        AmmPoolConfig.DECIMAL_SCALE,
        AmmPoolConfig.ROUNDING_MODE);

    // Input lớn hơn maxInput
    BigDecimal actualInput = maxInput.add(new BigDecimal("0.1")); // 101.11

    // Act
    boolean result = SwapMath.checkSlippage(output, actualInput, estimateAmount, zeroForOne, exactInput, slippage);

    // Assert - Khi input > maxInput thì phải fail
    assertFalse(result, "Với input > maxInput, phương thức nên trả về false");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage trong các trường hợp thực tế dựa trên test case trước đó")
  public void testCheckSlippageRealCases() {
    // Test case 1: exactInput = false, zeroForOne = true, input vượt quá giới hạn
    // slippage
    BigDecimal amount0 = new BigDecimal("120"); // input (token0)
    BigDecimal amount1 = new BigDecimal("100"); // output (token1)
    BigDecimal estimateAmount = new BigDecimal("100"); // dự kiến
    boolean zeroForOne = true;
    boolean exactInput = false;
    BigDecimal slippage = new BigDecimal("0.01"); // 1% slippage

    boolean result = SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, slippage);
    // amountInMaximum = estimateAmount / (1 - slippage) = 100 / 0.99 ≈ 101.01
    // 120 > 101.01, nên vượt quá slippage -> false
    assertFalse(result, "Với input (120) và output (100), slippage 1%, phương thức nên trả về false vì 120 > 100/0.99");

    // Test case 2: exactInput = true, zeroForOne = true, output nằm trong giới hạn
    // slippage
    amount0 = new BigDecimal("100"); // input (token0)
    amount1 = new BigDecimal("97"); // output (token1)
    estimateAmount = new BigDecimal("100"); // dự kiến
    zeroForOne = true;
    exactInput = true;

    result = SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, slippage);
    // amountOutMinimum = estimateAmount * (1 - slippage) = 100 * 0.99 = 99
    // 97 < 99, nên vượt quá slippage -> false
    assertFalse(result, "Với input (100) và output (97), slippage 1%, phương thức nên trả về false vì 97 < 100*0.99");

    // Test case 3: exactInput = true, zeroForOne = true, output nằm trong giới hạn
    // slippage
    amount0 = new BigDecimal("100"); // input (token0)
    amount1 = new BigDecimal("99.5"); // output (token1)
    estimateAmount = new BigDecimal("100"); // dự kiến
    zeroForOne = true;
    exactInput = true;

    result = SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, slippage);
    // amountOutMinimum = estimateAmount * (1 - slippage) = 100 * 0.99 = 99
    // 99.5 > 99, nên nằm trong slippage -> true
    assertTrue(result, "Với input (100) và output (99.5), slippage 1%, phương thức nên trả về true vì 99.5 > 100*0.99");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactInput = true và vượt quá slippage")
  public void testCheckSlippageExactInputExceedSlippage() {
    // Arrange - trường hợp vượt quá slippage
    BigDecimal slippage = new BigDecimal("0.01"); // 1%
    BigDecimal inputAmount = new BigDecimal("100");
    BigDecimal estimateAmount = new BigDecimal("100"); // dự kiến
    // Output thấp hơn giới hạn slippage: 100 * 0.99 - 1 = 98
    BigDecimal outputAmount = estimateAmount.multiply(BigDecimal.ONE.subtract(slippage)).subtract(new BigDecimal("1"));
    boolean zeroForOne = true;
    boolean exactInput = true;

    // Act
    boolean result = SwapMath.checkSlippage(inputAmount, outputAmount, estimateAmount, zeroForOne, exactInput,
        slippage);

    // Assert
    assertFalse(result, "Phải trả về false khi output thấp hơn giới hạn slippage");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage với exactOutput = true và vượt quá slippage")
  public void testCheckSlippageExactOutputExceedSlippage() {
    // Arrange - trường hợp vượt quá slippage
    BigDecimal slippage = new BigDecimal("0.01"); // 1%
    BigDecimal outputAmount = new BigDecimal("100");
    BigDecimal estimateAmount = new BigDecimal("100"); // dự kiến
    // Input cao hơn giới hạn slippage: 100 / 0.99 + 1 = 102.01
    BigDecimal inputAmount = estimateAmount.divide(BigDecimal.ONE.subtract(slippage), AmmPoolConfig.DECIMAL_SCALE,
        AmmPoolConfig.ROUNDING_MODE).add(new BigDecimal("1"));
    boolean zeroForOne = true;
    boolean exactInput = false;

    // Act
    boolean result = SwapMath.checkSlippage(inputAmount, outputAmount, estimateAmount, zeroForOne, exactInput,
        slippage);

    // Assert
    assertFalse(result, "Phải trả về false khi input cao hơn giới hạn slippage");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage đơn giản hóa với slippage = null luôn trả về true")
  public void testSimplifiedCheckSlippageWithNullSlippage() {
    boolean result = SwapMath.checkSlippage(
        new BigDecimal("100"),
        new BigDecimal("90"),
        new BigDecimal("95"),
        true,
        true,
        null);

    assertTrue(result, "Khi slippage = null, phương thức nên luôn trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage đơn giản hóa với exactInput = true và output > minOutput")
  public void testSimplifiedCheckSlippageExactInputSuccess() {
    BigDecimal input = new BigDecimal("100");
    BigDecimal slippage = new BigDecimal("0.01"); // 1%
    BigDecimal estimateAmount = new BigDecimal("100"); // dự kiến
    BigDecimal minOutput = estimateAmount.multiply(BigDecimal.ONE.subtract(slippage)); // 99
    BigDecimal output = minOutput.add(new BigDecimal("0.1")); // 99.1

    boolean result = SwapMath.checkSlippage(
        input, // amount0 (input)
        output, // amount1 (output)
        estimateAmount, // giá trị dự kiến
        true, // zeroForOne
        true, // exactInput
        slippage);

    assertTrue(result, "Với output > minOutput, phương thức nên trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage đơn giản hóa với exactInput = true và output < minOutput")
  public void testSimplifiedCheckSlippageExactInputFail() {
    BigDecimal input = new BigDecimal("100");
    BigDecimal slippage = new BigDecimal("0.01"); // 1%
    BigDecimal estimateAmount = new BigDecimal("100"); // dự kiến
    BigDecimal minOutput = estimateAmount.multiply(BigDecimal.ONE.subtract(slippage)); // 99
    BigDecimal output = minOutput.subtract(new BigDecimal("0.1")); // 98.9

    boolean result = SwapMath.checkSlippage(
        input, // amount0 (input)
        output, // amount1 (output)
        estimateAmount, // giá trị dự kiến
        true, // zeroForOne
        true, // exactInput
        slippage);

    assertFalse(result, "Với output < minOutput, phương thức nên trả về false");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage đơn giản hóa với exactInput = false và input < maxInput")
  public void testSimplifiedCheckSlippageExactOutputSuccess() {
    BigDecimal output = new BigDecimal("100");
    BigDecimal slippage = new BigDecimal("0.01"); // 1%
    BigDecimal estimateAmount = new BigDecimal("100"); // dự kiến
    BigDecimal maxInput = estimateAmount.divide(BigDecimal.ONE.subtract(slippage), AmmPoolConfig.DECIMAL_SCALE,
        AmmPoolConfig.ROUNDING_MODE); // 101.01
    BigDecimal input = maxInput.subtract(new BigDecimal("0.1")); // 100.91

    boolean result = SwapMath.checkSlippage(
        input, // amount0 (input)
        output, // amount1 (output)
        estimateAmount, // giá trị dự kiến
        true, // zeroForOne
        false, // exactOutput
        slippage);

    assertTrue(result, "Với input < maxInput, phương thức nên trả về true");
  }

  @Test
  @DisplayName("Kiểm tra checkSlippage đơn giản hóa với exactInput = false và input > maxInput")
  public void testSimplifiedCheckSlippageExactOutputFail() {
    BigDecimal output = new BigDecimal("100");
    BigDecimal slippage = new BigDecimal("0.01"); // 1%
    BigDecimal estimateAmount = new BigDecimal("100"); // dự kiến
    BigDecimal maxInput = estimateAmount.divide(BigDecimal.ONE.subtract(slippage), AmmPoolConfig.DECIMAL_SCALE,
        AmmPoolConfig.ROUNDING_MODE); // 101.01
    BigDecimal input = maxInput.add(new BigDecimal("0.1")); // 101.11

    boolean result = SwapMath.checkSlippage(
        input, // amount0 (input)
        output, // amount1 (output)
        estimateAmount, // giá trị dự kiến
        true, // zeroForOne
        false, // exactOutput
        slippage);

    assertFalse(result, "Với input > maxInput, phương thức nên trả về false");
  }

  @Test
  @DisplayName("Kiểm tra calculateEstimateAmount cho zero-to-one (token0 -> token1) với exactInput = true")
  public void testCalculateEstimateAmountZeroToOneExactInput() {
    // Arrange
    BigDecimal amountSpecified = new BigDecimal("100");
    BigDecimal initialSqrtPrice = new BigDecimal("1.5");
    double feePercentage = 0.003; // 0.3%
    boolean zeroForOne = true;
    boolean exactInput = true;

    // Act
    BigDecimal result = SwapMath.calculateEstimateAmount(
        amountSpecified,
        initialSqrtPrice,
        feePercentage,
        zeroForOne,
        exactInput);

    // Assert
    assertNotNull(result);

    // Tính toán kết quả mong đợi: amountSpecified * initialPrice * (1-fee)
    BigDecimal initialPrice = initialSqrtPrice.multiply(initialSqrtPrice)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    BigDecimal oneMinusFee = BigDecimal.ONE.subtract(BigDecimal.valueOf(feePercentage));
    BigDecimal expected = amountSpecified.multiply(initialPrice).multiply(oneMinusFee)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    assertEquals(0, expected.compareTo(result),
        "Estimate amount cho zero-to-one exactInput không chính xác");
  }

  @Test
  @DisplayName("Kiểm tra calculateEstimateAmount cho one-to-zero (token1 -> token0) với exactInput = true")
  public void testCalculateEstimateAmountOneToZeroExactInput() {
    // Arrange
    BigDecimal amountSpecified = new BigDecimal("100");
    BigDecimal initialSqrtPrice = new BigDecimal("1.5");
    double feePercentage = 0.003; // 0.3%
    boolean zeroForOne = false;
    boolean exactInput = true;

    // Act
    BigDecimal result = SwapMath.calculateEstimateAmount(
        amountSpecified,
        initialSqrtPrice,
        feePercentage,
        zeroForOne,
        exactInput);

    // Assert
    assertNotNull(result);

    // Tính toán kết quả mong đợi: amountSpecified / initialPrice * (1-fee)
    BigDecimal initialPrice = initialSqrtPrice.multiply(initialSqrtPrice)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    BigDecimal oneMinusFee = BigDecimal.ONE.subtract(BigDecimal.valueOf(feePercentage));
    BigDecimal expected = amountSpecified.divide(initialPrice, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE)
        .multiply(oneMinusFee).setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    assertEquals(0, expected.compareTo(result),
        "Estimate amount cho one-to-zero exactInput không chính xác");
  }

  @Test
  @DisplayName("Kiểm tra calculateEstimateAmount cho zero-to-one (token0 -> token1) với exactInput = false")
  public void testCalculateEstimateAmountZeroToOneExactOutput() {
    // Arrange
    BigDecimal amountSpecified = new BigDecimal("100");
    BigDecimal initialSqrtPrice = new BigDecimal("1.5");
    double feePercentage = 0.003; // 0.3%
    boolean zeroForOne = true;
    boolean exactInput = false;

    // Act
    BigDecimal result = SwapMath.calculateEstimateAmount(
        amountSpecified,
        initialSqrtPrice,
        feePercentage,
        zeroForOne,
        exactInput);

    // Assert
    assertNotNull(result);

    // Tính toán kết quả mong đợi: amountSpecified / initialPrice / (1-fee)
    BigDecimal initialPrice = initialSqrtPrice.multiply(initialSqrtPrice)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    BigDecimal oneMinusFee = BigDecimal.ONE.subtract(BigDecimal.valueOf(feePercentage));
    BigDecimal expected = amountSpecified.divide(initialPrice, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE)
        .divide(oneMinusFee, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    assertEquals(0, expected.compareTo(result),
        "Estimate amount cho zero-to-one exactOutput không chính xác");
  }

  @Test
  @DisplayName("Kiểm tra calculateEstimateAmount cho one-to-zero (token1 -> token0) với exactInput = false")
  public void testCalculateEstimateAmountOneToZeroExactOutput() {
    // Arrange
    BigDecimal amountSpecified = new BigDecimal("100");
    BigDecimal initialSqrtPrice = new BigDecimal("1.5");
    double feePercentage = 0.003; // 0.3%
    boolean zeroForOne = false;
    boolean exactInput = false;

    // Act
    BigDecimal result = SwapMath.calculateEstimateAmount(
        amountSpecified,
        initialSqrtPrice,
        feePercentage,
        zeroForOne,
        exactInput);

    // Assert
    assertNotNull(result);

    // Tính toán kết quả mong đợi: amountSpecified * initialPrice / (1-fee)
    BigDecimal initialPrice = initialSqrtPrice.multiply(initialSqrtPrice)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    BigDecimal oneMinusFee = BigDecimal.ONE.subtract(BigDecimal.valueOf(feePercentage));
    BigDecimal expected = amountSpecified.multiply(initialPrice)
        .divide(oneMinusFee, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    assertEquals(0, expected.compareTo(result),
        "Estimate amount cho one-to-zero exactOutput không chính xác");
  }

  @Test
  @DisplayName("Kiểm tra calculateEstimateAmount với trường hợp cực đoan: giá gần bằng 0")
  public void testCalculateEstimateAmountWithLowPrice() {
    // Arrange
    BigDecimal amountSpecified = new BigDecimal("100");
    BigDecimal initialSqrtPrice = new BigDecimal("0.0001"); // Giá rất thấp
    double feePercentage = 0.003; // 0.3%
    boolean zeroForOne = true;
    boolean exactInput = true;

    // Act
    BigDecimal result = SwapMath.calculateEstimateAmount(
        amountSpecified,
        initialSqrtPrice,
        feePercentage,
        zeroForOne,
        exactInput);

    // Assert
    assertNotNull(result);
    // Kết quả phải dương khi có input dương
    assertTrue(result.compareTo(BigDecimal.ZERO) > 0,
        "Estimate amount phải dương khi amountSpecified dương");
  }

  @Test
  @DisplayName("Kiểm tra calculateEstimateAmount với mức phí cao")
  public void testCalculateEstimateAmountWithHighFee() {
    // Arrange
    BigDecimal amountSpecified = new BigDecimal("100");
    BigDecimal initialSqrtPrice = new BigDecimal("1.5");
    double feePercentage = 0.1; // 10% - phí cao
    boolean zeroForOne = true;
    boolean exactInput = true;

    // Act
    BigDecimal result = SwapMath.calculateEstimateAmount(
        amountSpecified,
        initialSqrtPrice,
        feePercentage,
        zeroForOne,
        exactInput);

    // Assert
    assertNotNull(result);
    BigDecimal initialPrice = initialSqrtPrice.multiply(initialSqrtPrice)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    BigDecimal oneMinusFee = BigDecimal.ONE.subtract(BigDecimal.valueOf(feePercentage));
    BigDecimal expected = amountSpecified.multiply(initialPrice).multiply(oneMinusFee)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    assertEquals(0, expected.compareTo(result),
        "Estimate amount với phí cao không chính xác");
  }

  @Test
  @DisplayName("Kiểm tra computeSwapStep với exactOutput và amountOut >= amountRemaining.abs()")
  public void testComputeSwapStepExactOutputAmountOutGreaterThanRemaining() {
    // Arrange
    BigDecimal sqrtRatioCurrent = new BigDecimal("1.5");
    BigDecimal sqrtRatioTarget = new BigDecimal("1.2");
    BigDecimal liquidity = new BigDecimal("1000");
    BigDecimal amountRemaining = new BigDecimal("-50"); // Âm cho exactOut
    double feePercentage = 0.003; // 0.3%

    // Act
    BigDecimal[] result = SwapMath.computeSwapStep(
        sqrtRatioCurrent,
        sqrtRatioTarget,
        liquidity,
        amountRemaining,
        feePercentage);

    // Assert
    BigDecimal amountOut = result[2];

    // Kiểm tra amountOut đúng bằng amountRemaining.abs()
    assertEquals(amountRemaining.abs(), amountOut,
        "AmountOut phải đúng bằng amountRemaining.abs() khi !exactIn && amountOut >= amountRemaining.abs()");
  }

  @Test
  @DisplayName("Kiểm tra computeSwapStep khi exactInput và không đạt đến giá mục tiêu")
  public void testComputeSwapStepExactInputNotReachingTarget() {
    // Arrange
    BigDecimal sqrtRatioCurrent = new BigDecimal("1.5");
    BigDecimal sqrtRatioTarget = new BigDecimal("1.0"); // Giá mục tiêu khó đạt hơn
    BigDecimal liquidity = new BigDecimal("1000"); // Tăng liquidity
    double feePercentage = 0.003; // 0.3%

    // Act - Tính toán lượng token tối đa có thể đưa vào trước khi test
    BigDecimal maxAmount = SqrtPriceMath.getAmount0Delta(
        sqrtRatioTarget,
        sqrtRatioCurrent,
        liquidity,
        true);

    // Chọn một lượng token nhỏ hơn 1/10 maxAmount để đảm bảo không đạt target
    BigDecimal amountRemaining = maxAmount.divide(BigDecimal.TEN, AmmPoolConfig.DECIMAL_SCALE,
        AmmPoolConfig.ROUNDING_MODE);

    // Đảm bảo amount không đủ để đạt đến target
    assertTrue(amountRemaining.compareTo(maxAmount) < 0,
        "Lượng token đưa vào phải nhỏ hơn lượng token cần thiết để đạt target");

    BigDecimal[] result = SwapMath.computeSwapStep(
        sqrtRatioCurrent,
        sqrtRatioTarget,
        liquidity,
        amountRemaining,
        feePercentage);

    // Assert
    BigDecimal sqrtRatioNext = result[0];
    BigDecimal amountIn = result[1];
    BigDecimal feeAmount = result[3];

    // Kiểm tra sqrtRatioNext nằm giữa sqrtRatioCurrent và sqrtRatioTarget
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioCurrent) < 0,
        "SqrtRatioNext phải nhỏ hơn sqrtRatioCurrent (giá giảm khi zero-for-one)");
    assertTrue(sqrtRatioNext.compareTo(sqrtRatioTarget) > 0,
        "SqrtRatioNext phải lớn hơn sqrtRatioTarget khi không đủ liquidity");

    // Kiểm tra công thức tính phí khi không đạt đến giá mục tiêu: feeAmount =
    // amountRemaining - amountIn
    BigDecimal expectedFee = amountRemaining.subtract(amountIn);
    assertEquals(0, expectedFee.compareTo(feeAmount),
        "Phí phải được tính bằng amountRemaining - amountIn khi không đạt đến giá mục tiêu");
  }

  @Test
  @DisplayName("Kiểm tra các trường hợp đặc biệt của checkSlippage")
  public void testCheckSlippageSpecialCases() {
    BigDecimal amount0 = new BigDecimal("100");
    BigDecimal amount1 = new BigDecimal("100");
    BigDecimal estimateAmount = new BigDecimal("100");
    boolean zeroForOne = true;
    boolean exactInput = true;

    // Test case 1: slippage = null
    assertTrue(SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, null),
        "Khi slippage = null, phương thức nên luôn trả về true");

    // Test case 2: slippage = 0
    assertTrue(SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, BigDecimal.ZERO),
        "Khi slippage = 0, phương thức nên luôn trả về true");

    // Test case 3: slippage = 1
    assertTrue(SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, BigDecimal.ONE),
        "Khi slippage = 1, phương thức nên luôn trả về true");

    // Test case 4: amount0 <= 0
    assertFalse(SwapMath.checkSlippage(
        BigDecimal.ZERO, amount1, estimateAmount, zeroForOne, exactInput, new BigDecimal("0.01")),
        "Khi amount0 = 0, phương thức nên trả về false");

    // Test case 5: amount1 <= 0
    assertFalse(SwapMath.checkSlippage(
        amount0, BigDecimal.ZERO, estimateAmount, zeroForOne, exactInput, new BigDecimal("0.01")),
        "Khi amount1 = 0, phương thức nên trả về false");

    // Test case 6: estimateAmount <= 0
    assertFalse(SwapMath.checkSlippage(
        amount0, amount1, BigDecimal.ZERO, zeroForOne, exactInput, new BigDecimal("0.01")),
        "Khi estimateAmount = 0, phương thức nên trả về false");

    // Test case 7: Các giá trị âm
    assertFalse(SwapMath.checkSlippage(
        new BigDecimal("-1"), amount1, estimateAmount, zeroForOne, exactInput, new BigDecimal("0.01")),
        "Khi amount0 âm, phương thức nên trả về false");

    assertFalse(SwapMath.checkSlippage(
        amount0, new BigDecimal("-1"), estimateAmount, zeroForOne, exactInput, new BigDecimal("0.01")),
        "Khi amount1 âm, phương thức nên trả về false");

    assertFalse(SwapMath.checkSlippage(
        amount0, amount1, new BigDecimal("-1"), zeroForOne, exactInput, new BigDecimal("0.01")),
        "Khi estimateAmount âm, phương thức nên trả về false");
  }
}
