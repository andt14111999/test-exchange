package com.exchangeengine.service.engine.amm_order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.AmmOrder;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.Tick;
import com.exchangeengine.model.TickBitmap;
import com.exchangeengine.model.event.AmmOrderEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AccountHistoryCache;
import com.exchangeengine.storage.cache.AmmOrderCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.TickBitmapCache;
import com.exchangeengine.storage.cache.TickCache;
import com.exchangeengine.util.ObjectCloner;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import com.exchangeengine.util.ammPool.SwapMath;
import com.exchangeengine.util.ammPool.TickMath;

/**
 * Processor cho AMM Order events
 */
public class AmmOrderProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AmmOrderProcessor.class);

  private final DisruptorEvent disruptorEvent;
  private final AmmOrderEvent ammOrderEvent;
  private final ProcessResult result;

  // Cache services
  private final AmmOrderCache ammOrderCache;
  private final AmmPoolCache ammPoolCache;
  private final AccountCache accountCache;
  private final TickCache tickCache;
  private final TickBitmapCache tickBitmapCache;
  private final AccountHistoryCache accountHistoryCache;

  // Dữ liệu chính
  private AmmOrder ammOrder;
  private AmmPool pool;
  private Account account0;
  private Account account1;
  private TickBitmap tickBitmap;
  private List<Tick> crossedTicks = new ArrayList<>();

  // Dữ liệu backup
  private AmmPool backupPool;
  private Account backupAccount0;
  private Account backupAccount1;
  private TickBitmap backupTickBitmap;
  private List<Tick> backupTicks = new ArrayList<>();

  // Dữ liệu kết quả swap
  private BigDecimal amount0;
  private BigDecimal amount1;
  private Map<String, BigDecimal> fees = new HashMap<>();
  private int initialTick;

  /**
   * Khởi tạo processor với DisruptorEvent
   *
   * @param disruptorEvent Event cần xử lý
   */
  public AmmOrderProcessor(DisruptorEvent disruptorEvent) {
    this.disruptorEvent = disruptorEvent;
    this.ammOrderEvent = disruptorEvent.getAmmOrderEvent();
    this.result = new ProcessResult(disruptorEvent);

    // Khởi tạo các cache service
    this.ammOrderCache = AmmOrderCache.getInstance();
    this.ammPoolCache = AmmPoolCache.getInstance();
    this.accountCache = AccountCache.getInstance();
    this.tickCache = TickCache.getInstance();
    this.tickBitmapCache = TickBitmapCache.getInstance();
    this.accountHistoryCache = AccountHistoryCache.getInstance();
  }

  /**
   * Xử lý AMM Order event
   *
   * @return ProcessResult kết quả xử lý
   */
  public ProcessResult process() {
    try {
      logger.info("Processing AMM Order Swap: identifier={}", ammOrderEvent.getIdentifier());

      // 1. Fetch dữ liệu
      fetchData();

      // 2. Validate dữ liệu
      List<String> validationErrors = validateSwap();
      if (!validationErrors.isEmpty()) {
        String errorMsg = String.join("; ", validationErrors);
        ammOrder.markError(errorMsg);
        disruptorEvent.setErrorMessage(errorMsg);
        return result;
      }

      // 3. Backup dữ liệu
      backupData();

      // Lưu tick ban đầu
      initialTick = pool.getCurrentTick();

      // 4. Thực hiện swap
      if (!executeSwap()) {
        // Swap thất bại, lỗi đã được set trong executeSwap
        return result;
      }

      // 5. Cập nhật dữ liệu mới sau swap
      if (!updateNewData()) {
        // Nếu cập nhật dữ liệu thất bại, rollback và trả về lỗi
        rollbackChanges();
        return result;
      }

      // 6. Lưu thay đổi vào cache
      saveToCache();

      // 7. Hoàn thành
      logger.info("Successfully executed AMM Order swap: {}", ammOrder.getIdentifier());
      disruptorEvent.successes();
      return result;

    } catch (Exception e) {
      logger.error("Error processing AMM Order event: {}", e.getMessage(), e);
      rollbackChanges();
      return ProcessResult.error(disruptorEvent, e.getMessage());
    }
  }

  /**
   * Cập nhật tất cả dữ liệu mới sau khi swap thành công
   * Phương thức này thực hiện tất cả các bước cập nhật cần thiết như:
   * - Cập nhật số dư tài khoản
   * - Tạo lịch sử giao dịch
   * - Cập nhật thông tin order
   * - Đánh dấu order hoàn thành
   *
   * @return true nếu cập nhật thành công, false nếu có lỗi
   */
  private boolean updateNewData() {
    try {
      logger.info("Updating data after successful swap");

      // 1. Cập nhật số dư tài khoản
      updateAccountBalances(amount0, amount1, ammOrder.getZeroForOne());

      // 2. Tạo lịch sử giao dịch
      createAccountHistories();

      // 3. Cập nhật thông tin order với amountReceived
      BigDecimal amountActual = ammOrder.getZeroForOne() ? amount0 : amount1;
      BigDecimal amountReceived = ammOrder.getZeroForOne() ? amount1 : amount0;

      // Cập nhật thông tin order sau khi thực hiện swap
      boolean updateSuccess = ammOrder.updateAfterExecution(
          amountActual,
          amountReceived,
          initialTick,
          pool.getCurrentTick(),
          fees);

      if (!updateSuccess) {
        ammOrder.markError("Failed to update order after execution");
        disruptorEvent.setErrorMessage("Failed to update order after execution");
        return false;
      }

      // 4. Đánh dấu order hoàn thành
      if (!ammOrder.markSuccess()) {
        disruptorEvent.setErrorMessage("Failed to mark order as success");
        return false;
      }

      return true;

    } catch (Exception e) {
      logger.error("Error updating data after swap: {}", e.getMessage(), e);
      ammOrder.markError("Error updating data: " + e.getMessage());
      disruptorEvent.setErrorMessage("Error updating data: " + e.getMessage());
      return false;
    }
  }

  private void fetchData() {
    // Khởi tạo AmmOrder từ event
    ammOrder = ammOrderEvent.toAmmOrder(false);
    result.setAmmOrder(ammOrder);

    // Lấy thông tin pool và accounts
    pool = ammOrder.getPool().get();
    account0 = ammOrder.getAccount0();
    account1 = ammOrder.getAccount1();

    // Lấy thông tin TickBitmap từ pool
    tickBitmap = pool.getTickBitmap();

    logger.debug("Fetched data successfully - pool: {}, tickBitmap: {}", pool.getPair(), tickBitmap.getPoolPair());
  }

  /**
   * Kiểm tra dữ liệu trước khi swap
   */
  private List<String> validateSwap() {
    List<String> errors = new ArrayList<>();

    // Kiểm tra số dư tài khoản
    boolean exactInput = ammOrder.getAmountSpecified().compareTo(BigDecimal.ZERO) > 0;

    if (exactInput) {
      if (ammOrder.getZeroForOne()) {
        // Swap token0 -> token1, kiểm tra số dư token0
        if (account0.getAvailableBalance().compareTo(ammOrder.getAmountSpecified()) < 0) {
          errors.add(String.format("Insufficient token0 balance: %s < %s",
              account0.getAvailableBalance(), ammOrder.getAmountSpecified()));
        }
      } else {
        // Swap token1 -> token0, kiểm tra số dư token1
        if (account1.getAvailableBalance().compareTo(ammOrder.getAmountSpecified()) < 0) {
          errors.add(String.format("Insufficient token1 balance: %s < %s",
              account1.getAvailableBalance(), ammOrder.getAmountSpecified()));
        }
      }
    }

    // Kiểm tra liquidity của pool
    if (pool.getLiquidity().compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Pool has no liquidity");
    }

    if (!ammOrder.isProcessing()) {
      errors.add("Order is not processing");
    }

    return errors;
  }

  /**
   * Backup dữ liệu trước khi swap
   */
  private void backupData() {
    backupPool = ObjectCloner.duplicate(pool, AmmPool.class);
    backupAccount0 = ObjectCloner.duplicate(account0, Account.class);
    backupAccount1 = ObjectCloner.duplicate(account1, Account.class);
    backupTickBitmap = ObjectCloner.duplicate(tickBitmap, TickBitmap.class);

    logger.debug("Data backed up successfully");
  }

  /**
   * Thực hiện swap theo thuật toán Uniswap V3
   * Hàm này cập nhật các biến thành viên để lưu kết quả swap
   *
   * @return true nếu swap thành công, false nếu không
   */
  private boolean executeSwap() {
    try {
      final boolean zeroForOne = ammOrder.getZeroForOne();
      final BigDecimal amountSpecified = ammOrder.getAmountSpecified();
      final boolean exactInput = amountSpecified.compareTo(BigDecimal.ZERO) > 0;

      // Khởi tạo các trạng thái cho quá trình swap
      BigDecimal sqrtPriceLimit = getSqrtPriceLimit(zeroForOne);
      BigDecimal amountSpecifiedRemaining = getInitialAmountRemaining(amountSpecified, exactInput);
      final double feePercentage = pool.getFeePercentage();

      // Trạng thái ban đầu cho vòng lặp swap
      BigDecimal amountCalculated = BigDecimal.ZERO; // Lượng token đã tính
      BigDecimal sqrtPrice = pool.getSqrtPrice(); // Giá hiện tại
      int tick = pool.getCurrentTick(); // Tick hiện tại
      BigDecimal feeGrowthGlobal0 = pool.getFeeGrowthGlobal0(); // Phí toàn cục token0
      BigDecimal feeGrowthGlobal1 = pool.getFeeGrowthGlobal1(); // Phí toàn cục token1
      BigDecimal liquidity = pool.getLiquidity(); // Thanh khoản hiện tại

      // Vòng lặp chính thực hiện swap
      while (amountSpecifiedRemaining.compareTo(BigDecimal.ZERO) != 0 && sqrtPrice.compareTo(sqrtPriceLimit) != 0) {
        int nextInitializedTick = findNextInitializedTick(tick, zeroForOne);
        BigDecimal sqrtPriceNext = getSqrtPriceNext(nextInitializedTick, zeroForOne, sqrtPriceLimit);

        // Thực hiện xử lý một bước swap
        SwapStepResult swapStepResult = processSwapStep(
            sqrtPrice, sqrtPriceNext, liquidity, amountSpecifiedRemaining, feePercentage,
            zeroForOne, exactInput, feeGrowthGlobal0, feeGrowthGlobal1, tick, nextInitializedTick, amountCalculated);

        // Cập nhật các biến trạng thái
        sqrtPrice = swapStepResult.sqrtPrice;
        amountSpecifiedRemaining = swapStepResult.amountSpecifiedRemaining;
        amountCalculated = swapStepResult.amountCalculated;
        feeGrowthGlobal0 = swapStepResult.feeGrowthGlobal0;
        feeGrowthGlobal1 = swapStepResult.feeGrowthGlobal1;
        liquidity = swapStepResult.liquidity;
        tick = swapStepResult.tick;

        // Thêm tick đã cross vào danh sách nếu có
        addCrossedTickToList(swapStepResult);
      }

      // Tính toán số lượng token thực tế đã swap
      amount0 = calculateAmount0(amountSpecified, amountSpecifiedRemaining, amountCalculated, zeroForOne, exactInput);
      amount1 = calculateAmount1(amountSpecified, amountSpecifiedRemaining, amountCalculated, zeroForOne, exactInput);

      // Kiểm tra slippage
      if (!checkSlippage(amount0, amount1, zeroForOne, exactInput)) {
        return false;
      }

      // Cập nhật pool
      updatePool(amount0, amount1, zeroForOne, tick, sqrtPrice, feeGrowthGlobal0, feeGrowthGlobal1, liquidity);

      return true;

    } catch (Exception e) {
      logger.error("Error calculating swap: {}", e.getMessage(), e);
      ammOrder.markError("Error: " + e.getMessage());
      disruptorEvent.setErrorMessage("Error: " + e.getMessage());
      return false;
    }
  }

  /**
   * Kiểm tra slippage cho swap
   */
  private boolean checkSlippage(BigDecimal amount0, BigDecimal amount1, boolean zeroForOne, boolean exactInput) {
    // Lấy số lượng token ban đầu từ ammOrder
    BigDecimal amountSpecified = ammOrder.getAmountSpecified().abs();

    // Lấy giá ban đầu từ initialTick
    BigDecimal initialSqrtPrice = TickMath.getSqrtRatioAtTick(initialTick);

    // Tính toán số lượng token dự kiến
    BigDecimal estimateAmount = SwapMath.calculateEstimateAmount(
        amountSpecified,
        initialSqrtPrice,
        pool.getFeePercentage(),
        zeroForOne,
        exactInput);

    logger.info("Slippage check - amountSpecified: {}, amount0: {}, amount1: {}, estimateAmount: {}",
        amountSpecified, amount0, amount1, estimateAmount);

    // Gọi phương thức checkSlippage với tham số estimateAmount
    if (!SwapMath.checkSlippage(amount0, amount1, estimateAmount, zeroForOne, exactInput, ammOrder.getSlippage())) {
      logger.error("Swap failed due to slippage");
      ammOrder.markError("Swap exceeds slippage tolerance");
      disruptorEvent.setErrorMessage("Swap exceeds slippage tolerance");
      return false;
    }

    logger.info("Slippage check passed successfully");
    return true;
  }

  /**
   * Cập nhật thông tin pool sau khi swap
   *
   * @param amount0          Lượng token0 được swap
   * @param amount1          Lượng token1 được swap
   * @param zeroForOne       Hướng swap (true nếu swap từ token0 sang token1)
   * @param tick             Tick hiện tại sau swap
   * @param sqrtPrice        Giá sqrt hiện tại sau swap
   * @param feeGrowthGlobal0 Phí tăng trưởng toàn cục cho token0
   * @param feeGrowthGlobal1 Phí tăng trưởng toàn cục cho token1
   * @param liquidity        Thanh khoản mới sau swap
   */
  private void updatePool(
      BigDecimal amount0,
      BigDecimal amount1,
      boolean zeroForOne,
      int tick,
      BigDecimal sqrtPrice,
      BigDecimal feeGrowthGlobal0,
      BigDecimal feeGrowthGlobal1,
      BigDecimal liquidity) {

    // Tính toán TVL mới dựa trên hướng swap
    BigDecimal newTVL0;
    BigDecimal newTVL1;

    if (zeroForOne) {
      // Swap token0 -> token1, tăng token0 (pool nhận), giảm token1 (pool cho đi)
      newTVL0 = pool.getTotalValueLockedToken0().add(amount0);
      newTVL1 = pool.getTotalValueLockedToken1().subtract(amount1);
    } else {
      // Swap token1 -> token0, giảm token0 (pool cho đi), tăng token1 (pool nhận)
      newTVL0 = pool.getTotalValueLockedToken0().subtract(amount0);
      newTVL1 = pool.getTotalValueLockedToken1().add(amount1);
    }

    // Tính toán Volume mới
    BigDecimal newVolume0 = pool.getVolumeToken0().add(amount0);
    BigDecimal newVolume1 = pool.getVolumeToken1().add(amount1);

    // Đảm bảo feeGrowthGlobal không âm trước khi cập nhật pool
    // Trong Uniswap V3, feeGrowthGlobal là uint256 nên không thể âm
    BigDecimal safeFeeGrowthGlobal0 = feeGrowthGlobal0.max(BigDecimal.ZERO);
    BigDecimal safeFeeGrowthGlobal1 = feeGrowthGlobal1.max(BigDecimal.ZERO);

    // Gọi phương thức cập nhật pool
    pool.updatePoolAfterSwap(
        tick,
        sqrtPrice,
        liquidity,
        safeFeeGrowthGlobal0,
        safeFeeGrowthGlobal1,
        newTVL0,
        newTVL1,
        newVolume0,
        newVolume1);
  }

  /**
   * Lấy giới hạn giá sqrt dựa trên hướng swap
   */
  private BigDecimal getSqrtPriceLimit(boolean zeroForOne) {
    if (zeroForOne) {
      // Swap token0 -> token1, sử dụng giá trị gần MIN_TICK
      return TickMath.getSqrtRatioAtTick(AmmPoolConfig.MIN_TICK + 1);
    } else {
      // Swap token1 -> token0, sử dụng giá trị gần MAX_TICK
      return TickMath.getSqrtRatioAtTick(AmmPoolConfig.MAX_TICK - 1);
    }
  }

  /**
   * Lấy giá trị amountSpecifiedRemaining ban đầu
   */
  private BigDecimal getInitialAmountRemaining(BigDecimal amountSpecified, boolean exactInput) {
    if (exactInput) {
      return amountSpecified;
    } else {
      // exactOutput: amountSpecified là lượng token cần nhận, đổi dấu âm để xử lý
      return amountSpecified.negate();
    }
  }

  /**
   * Lấy giá sqrt cho tick tiếp theo
   */
  private BigDecimal getSqrtPriceNext(int nextInitializedTick, boolean zeroForOne, BigDecimal sqrtPriceLimit) {
    boolean targetReached = nextInitializedTick == AmmPoolConfig.MIN_TICK
        || nextInitializedTick == AmmPoolConfig.MAX_TICK;
    return targetReached ? sqrtPriceLimit : TickMath.getSqrtRatioAtTick(nextInitializedTick);
  }

  /**
   * Tính amount0 thực tế dựa trên kết quả swap
   */
  private BigDecimal calculateAmount0(
      BigDecimal amountSpecified,
      BigDecimal amountSpecifiedRemaining,
      BigDecimal amountCalculated,
      boolean zeroForOne,
      boolean exactInput) {

    BigDecimal result;
    if (zeroForOne == exactInput) {
      result = amountSpecified.subtract(amountSpecifiedRemaining);
    } else {
      result = amountCalculated.abs();
    }
    return result;
  }

  /**
   * Tính amount1 thực tế dựa trên kết quả swap
   */
  private BigDecimal calculateAmount1(
      BigDecimal amountSpecified,
      BigDecimal amountSpecifiedRemaining,
      BigDecimal amountCalculated,
      boolean zeroForOne,
      boolean exactInput) {

    BigDecimal result;
    if (zeroForOne == exactInput) {
      result = amountCalculated.abs();
    } else {
      result = amountSpecified.subtract(amountSpecifiedRemaining);
    }
    return result;
  }

  /**
   * Class để lưu trữ kết quả sau khi xử lý một bước trong quá trình swap
   */
  /* package */ static class SwapStepResult {
    BigDecimal sqrtPrice;
    BigDecimal amountSpecifiedRemaining;
    BigDecimal amountCalculated;
    BigDecimal feeGrowthGlobal0;
    BigDecimal feeGrowthGlobal1;
    BigDecimal liquidity;
    int tick;
    Tick crossedTick;
  }

  /**
   * Xử lý một bước của quá trình swap
   * Phương thức này thực hiện logic bên trong vòng lặp while của executeSwap
   */
  /* package */ SwapStepResult processSwapStep(
      BigDecimal sqrtPrice, BigDecimal sqrtPriceNext, BigDecimal liquidity,
      BigDecimal amountSpecifiedRemaining, double feePercentage,
      boolean zeroForOne, boolean exactInput,
      BigDecimal feeGrowthGlobal0, BigDecimal feeGrowthGlobal1,
      int currentTick, int nextInitializedTick, BigDecimal amountCalculated) {

    // Tính toán bước swap
    BigDecimal[] swapResult = SwapMath.computeSwapStep(
        sqrtPrice,
        sqrtPriceNext,
        liquidity,
        amountSpecifiedRemaining,
        feePercentage);

    BigDecimal newSqrtPrice = swapResult[0]; // Giá mới
    BigDecimal amountIn = swapResult[1]; // Lượng token vào
    BigDecimal amountOut = swapResult[2]; // Lượng token ra
    BigDecimal feeAmount = swapResult[3]; // Phí

    // Cập nhật fee cho order hiện tại
    String token = zeroForOne ? pool.getToken0() : pool.getToken1();
    updateOrderFees(token, feeAmount);

    // Cập nhật lượng token còn lại và đã tính
    SwapStepResult stepResult = new SwapStepResult();
    stepResult.sqrtPrice = newSqrtPrice;
    stepResult.feeGrowthGlobal0 = feeGrowthGlobal0;
    stepResult.feeGrowthGlobal1 = feeGrowthGlobal1;
    stepResult.liquidity = liquidity;

    if (exactInput) {
      stepResult.amountSpecifiedRemaining = amountSpecifiedRemaining.subtract(amountIn).subtract(feeAmount);
      stepResult.amountCalculated = amountCalculated.subtract(amountOut);
    } else {
      stepResult.amountSpecifiedRemaining = amountSpecifiedRemaining.add(amountOut);
      stepResult.amountCalculated = amountCalculated.add(amountIn.add(feeAmount));
    }

    // Cộng phí vào hệ thống nếu có thanh khoản - theo logic Uniswap V3
    if (liquidity.compareTo(BigDecimal.ZERO) > 0) {
      // Tính toán feeGrowthDelta = feeAmount / liquidity (tương đương với
      // mulDiv(feeAmount, Q128, liquidity) trong Uniswap)
      BigDecimal feeGrowthDelta = feeAmount
          .divide(liquidity, AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

      // Cập nhật feeGrowthGlobal dựa trên hướng swap
      if (zeroForOne) {
        stepResult.feeGrowthGlobal0 = feeGrowthGlobal0.add(feeGrowthDelta);
      } else {
        stepResult.feeGrowthGlobal1 = feeGrowthGlobal1.add(feeGrowthDelta);
      }
    }

    if (newSqrtPrice.compareTo(sqrtPriceNext) == 0) {
      // Chúng ta đã cross một tick - lấy thông tin về tick đó
      Tick tickObject = getTick(nextInitializedTick);

      // Backup tick để rollback nếu cần
      backupTicks.add(ObjectCloner.duplicate(tickObject, Tick.class));

      // Thực hiện cross tick - cập nhật thanh khoản khi vượt qua một tick
      BigDecimal liquidityNet = crossTick(tickObject, zeroForOne, stepResult.feeGrowthGlobal0,
          stepResult.feeGrowthGlobal1);

      // Cập nhật thanh khoản dựa trên hướng của swap
      if (zeroForOne) {
        liquidityNet = liquidityNet.negate();
      }

      // Chỉ thay đổi liquidity nếu liquidityNet khác 0
      if (liquidityNet.compareTo(BigDecimal.ZERO) != 0) {
        stepResult.liquidity = liquidity.add(liquidityNet);
      }

      // Lưu tick đã cross để trả về
      stepResult.crossedTick = tickObject;

      // Cập nhật tick hiện tại
      stepResult.tick = zeroForOne ? nextInitializedTick - 1 : nextInitializedTick;
    } else {
      // Tính lại tick hiện tại dựa trên giá mới nếu chưa vượt qua tick tiếp theo
      stepResult.tick = TickMath.getTickAtSqrtRatio(newSqrtPrice);
    }

    return stepResult;
  }

  /**
   * Xử lý khi vượt qua một tick (cross tick)
   * Hàm này cập nhật feeGrowth và trạng thái initialized của tick
   * Trả về liquidityNet cho biết sự thay đổi thanh khoản khi vượt qua tick này
   *
   * @param tick             Tick đang được cross qua
   * @param zeroForOne       Hướng swap (true: token0 -> token1, false: token1 ->
   *                         token0)
   * @param feeGrowthGlobal0 Phí toàn cục token0 hiện tại
   * @param feeGrowthGlobal1 Phí toàn cục token1 hiện tại
   * @return liquidityNet - Mức thay đổi thanh khoản khi vượt qua tick này
   */
  private BigDecimal crossTick(Tick tick, boolean zeroForOne, BigDecimal feeGrowthGlobal0,
      BigDecimal feeGrowthGlobal1) {
    // Cập nhật feeGrowth dựa trên hướng swap - theo logic Uniswap V3
    // Trong Uniswap V3, khi cross qua một tick, feeGrowthOutside được cập nhật
    // với giá trị feeGrowthGlobal hiện tại
    if (zeroForOne) {
      // Khi swap từ token0 sang token1 (giá giảm), cập nhật feeGrowthOutside0
      tick.setFeeGrowthOutside0(feeGrowthGlobal0);
      // Giữ nguyên feeGrowthOutside1
      tick.setFeeGrowthOutside1(tick.getFeeGrowthOutside1());
    } else {
      // Khi swap từ token1 sang token0 (giá tăng), cập nhật feeGrowthOutside1
      tick.setFeeGrowthOutside1(feeGrowthGlobal1);
      // Giữ nguyên feeGrowthOutside0
      tick.setFeeGrowthOutside0(tick.getFeeGrowthOutside0());
    }

    // Nếu tick chưa được khởi tạo, mới cần thiết lập
    boolean wasInitialized = tick.isInitialized();
    if (!wasInitialized) {
      tick.setInitialized(true);
      tick.setTickInitializedTimestamp(System.currentTimeMillis());
    }

    // Trả về liquidityNet
    return tick.getLiquidityNet();
  }

  /**
   * Tìm tick tiếp theo đã được khởi tạo
   * Sử dụng phương thức nextSetBit/previousSetBit của BitSet
   */
  private int findNextInitializedTick(int currentTick, boolean zeroForOne) {
    // Lấy TickBitmap từ pool
    TickBitmap tickBitmap = pool.getTickBitmap();

    int result;
    if (zeroForOne) {
      // Khi zeroForOne = true, tìm tick nhỏ hơn hoặc bằng tick hiện tại
      int prevSetBit = tickBitmap.previousSetBit(currentTick);

      // Nếu không tìm thấy, trả về MIN_TICK
      result = prevSetBit >= 0 ? prevSetBit : AmmPoolConfig.MIN_TICK;
    } else {
      // Khi zeroForOne = false, tìm tick lớn hơn tick hiện tại
      int nextSetBit = tickBitmap.nextSetBit(currentTick + 1);

      // Nếu không tìm thấy, trả về MAX_TICK
      result = nextSetBit >= 0 ? nextSetBit : AmmPoolConfig.MAX_TICK;
    }

    return result;
  }

  /**
   * Cập nhật số dư tài khoản sau khi swap
   */
  private void updateAccountBalances(BigDecimal amount0, BigDecimal amount1, boolean zeroForOne) {
    if (zeroForOne) {
      // Swap token0 -> token1
      account0.decreaseAvailableBalance(amount0);
      account1.increaseAvailableBalance(amount1);
    } else {
      // Swap token1 -> token0
      account0.increaseAvailableBalance(amount0);
      account1.decreaseAvailableBalance(amount1);
    }

    logger.info("Updated account balances - account0: {}, account1: {}",
        account0.getAvailableBalance(), account1.getAvailableBalance());
  }

  /**
   * Tạo lịch sử giao dịch
   */
  private void createAccountHistories() {
    // Tạo lịch sử cho tài khoản token0
    AccountHistory history0 = new AccountHistory(
        account0.getKey(),
        ammOrder.getIdentifier(),
        OperationType.AMM_ORDER_SWAP.getValue());
    history0.setBalanceValues(
        backupAccount0.getAvailableBalance(),
        account0.getAvailableBalance(),
        backupAccount0.getFrozenBalance(),
        account0.getFrozenBalance());

    // Tạo lịch sử cho tài khoản token1
    AccountHistory history1 = new AccountHistory(
        account1.getKey(),
        ammOrder.getIdentifier(),
        OperationType.AMM_ORDER_SWAP.getValue());
    history1.setBalanceValues(
        backupAccount1.getAvailableBalance(),
        account1.getAvailableBalance(),
        backupAccount1.getFrozenBalance(),
        account1.getFrozenBalance());

    // Lưu lịch sử vào cache
    accountHistoryCache.updateAccountHistory(history0);
    accountHistoryCache.updateAccountHistory(history1);

    // Thêm lịch sử vào result
    result.setAccountHistory(history0);
    result.addAccountHistory(history1);

    logger.info("Created account histories for swap transaction");
  }

  /**
   * Lưu tất cả thay đổi vào cache
   */
  private void saveToCache() {
    try {
      // 1. Lưu order vào cache
      ammOrderCache.updateAmmOrder(ammOrder.getIdentifier());

      // 2. Lưu pool vào cache
      ammPoolCache.updateAmmPool(pool);

      // 3. Lưu accounts vào cache
      accountCache.updateAccount(account0);
      accountCache.updateAccount(account1);

      // 4. Lưu ticks đã cross vào cache và thêm vào result
      for (Tick tick : crossedTicks) {
        tickCache.updateTick(tick);
        result.addTick(tick);
      }

      // 5. Lưu tick bitmap
      tickBitmapCache.updateTickBitmap(tickBitmap);

      // 6. Cập nhật dữ liệu vào kết quả
      result.setAmmPool(pool);
      result.setAmmOrder(ammOrder);
      result.addAccount(account0);
      result.addAccount(account1);

      logger.info("All changes saved to cache successfully");
    } catch (Exception e) {
      logger.error("Error saving to cache: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Rollback các thay đổi nếu có lỗi
   */
  private void rollbackChanges() {
    logger.warn("Rolling back changes due to error");

    try {
      // Khôi phục các dữ liệu đã backup
      if (backupAccount0 != null) {
        accountCache.updateAccount(backupAccount0);
      }

      if (backupAccount1 != null) {
        accountCache.updateAccount(backupAccount1);
      }

      if (backupPool != null) {
        ammPoolCache.updateAmmPool(backupPool);
      }

      // Khôi phục tick đã lưu
      for (Tick tick : backupTicks) {
        tickCache.updateTick(tick);
      }

      if (backupTickBitmap != null) {
        tickBitmapCache.updateTickBitmap(backupTickBitmap);
      }

      logger.info("Successfully rolled back changes");
    } catch (Exception e) {
      logger.error("Error during rollback: {}", e.getMessage(), e);
    }
  }

  /**
   * Cập nhật phí giao dịch cho order hiện tại
   * Phương thức này tích lũy (accumulate) phí qua các bước swap
   *
   * @param token  Token mà fee được áp dụng
   * @param amount Số tiền phí cần thêm vào
   */
  private void updateOrderFees(String token, BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal currentFee = fees.getOrDefault(token, BigDecimal.ZERO);
      fees.put(token, currentFee.add(amount));
    }
  }

  /**
   * Thêm crossed tick vào danh sách nếu không phải null
   *
   * @param swapStepResult kết quả swap step chứa crossedTick
   */
  /* package */ void addCrossedTickToList(SwapStepResult swapStepResult) {
    if (swapStepResult.crossedTick != null) {
      crossedTicks.add(swapStepResult.crossedTick);
    }
  }

  /**
   * Get tick from cache, throw exception if not found
   *
   * @param tickIndex Index of the tick to retrieve
   * @return Found tick
   * @throws IllegalStateException if tick not found, indicating pool has no
   *                               liquidity
   */
  private Tick getTick(int tickIndex) {
    // Create key to find in cache - use same format as pool.getTick
    String tickKey = pool.getPair() + "-" + tickIndex;

    // Get tick from cache
    return tickCache.getTick(tickKey)
        .orElseThrow(() -> {
          // Log detailed technical information
          logger.error("Tick not found in cache: {} for index: {}", tickKey, tickIndex);
          // Throw exception with user-friendly message
          return new IllegalStateException("Pool has no liquidity for this price range");
        });
  }
}
