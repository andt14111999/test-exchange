package com.exchangeengine.service.engine.amm_position.processor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.Tick;
import com.exchangeengine.model.TickBitmap;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AccountHistoryCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.AmmPositionCache;
import com.exchangeengine.storage.cache.TickBitmapCache;
import com.exchangeengine.storage.cache.TickCache;
import com.exchangeengine.util.ObjectCloner;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import com.exchangeengine.util.ammPool.LiquidityUtils;
import com.exchangeengine.util.ammPool.TickMath;

/**
 * Processor xử lý việc đóng vị thế AMM (Uniswap V3)
 */
public class AmmPositionCloseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AmmPositionCloseProcessor.class);

  // Input event và output result
  private final AmmPositionEvent event;
  private final DisruptorEvent disruptorEvent;
  private final ProcessResult result;

  // Các cache service
  private final AmmPositionCache ammPositionCache;
  private final AmmPoolCache ammPoolCache;
  private final AccountCache accountCache;
  private final TickCache tickCache;
  private final TickBitmapCache tickBitmapCache;
  private final AccountHistoryCache accountHistoryCache;

  // Dữ liệu chính
  private AmmPosition position;
  private AmmPool pool;
  private Account account0;
  private Account account1;
  private Tick lowerTick;
  private Tick upperTick;
  private TickBitmap tickBitmap;

  // Dữ liệu backup
  private AmmPosition backupPosition;
  private AmmPool backupPool;
  private Account backupAccount0;
  private Account backupAccount1;
  private Tick backupLowerTick;
  private Tick backupUpperTick;
  private TickBitmap backupTickBitmap;

  /**
   * Khởi tạo processor với DisruptorEvent
   *
   * @param disruptorEvent DisruptorEvent chứa AmmPositionEvent
   */
  public AmmPositionCloseProcessor(DisruptorEvent disruptorEvent) {
    this.disruptorEvent = disruptorEvent;
    this.event = disruptorEvent.getAmmPositionEvent();
    this.result = new ProcessResult(disruptorEvent);

    // Khởi tạo các cache service
    this.ammPositionCache = AmmPositionCache.getInstance();
    this.ammPoolCache = AmmPoolCache.getInstance();
    this.accountCache = AccountCache.getInstance();
    this.tickCache = TickCache.getInstance();
    this.tickBitmapCache = TickBitmapCache.getInstance();
    this.accountHistoryCache = AccountHistoryCache.getInstance();
  }

  /**
   * Xử lý đóng vị thế
   *
   * @return ProcessResult chứa kết quả xử lý
   */
  public ProcessResult process() {
    try {
      logger.info("Processing AMM position closing: {}", event.getIdentifier());

      // 1. Validate và lấy dữ liệu
      fetchData();

      List<String> validationErrors = validatePosition();
      if (!validationErrors.isEmpty()) {
        String errorMsg = String.join("; ", validationErrors);
        position.markError(errorMsg);
        disruptorEvent.setErrorMessage(errorMsg);
        return result;
      }

      // 2. Backup dữ liệu để rollback nếu có lỗi
      backupData();

      // 3. Thu thập phí còn lại trước khi đóng (nếu có)
      collectRemainingFees();

      // 4. Xử lý thanh khoản, cập nhật pool và đóng vị thế
      if (!processClosePosition()) {
        rollbackChanges();
        return result;
      }

      // 6. Lưu thay đổi vào cache
      saveToCache();

      // 7. Hoàn thành
      logger.info("Successfully closed AMM position: {}", position.getIdentifier());
      disruptorEvent.successes();
      return result;

    } catch (Exception e) {
      logger.error("Error processing AMM position closing: {}", e.getMessage(), e);
      if (position != null) {
        position.markError("Error: " + e.getMessage());
      }
      disruptorEvent.setErrorMessage("Error: " + e.getMessage());
      rollbackChanges();
      return result;
    }
  }

  /**
   * Lấy tất cả dữ liệu cần thiết
   */
  private void fetchData() {
    position = event.toAmmPosition(true);
    result.setAmmPosition(position);

    pool = position.getPool().get();
    account0 = position.getAccount0();
    account1 = position.getAccount1();
    lowerTick = position.getTickLower().get();
    upperTick = position.getTickUpper().get();
    tickBitmap = position.getTickBitmap().get();
  }

  /**
   * Validate vị thế có sẵn sàng để đóng không
   */
  private List<String> validatePosition() {
    List<String> errors = new ArrayList<>();

    // Kiểm tra trạng thái vị thế
    if (!position.isOpen()) {
      errors.add("Position is not open: " + position.getIdentifier());
    }

    // Kiểm tra pool đang hoạt động
    if (!pool.isActive()) {
      errors.add("Pool is not active: " + position.getPoolPair());
    }

    return errors;
  }

  /**
   * Backup dữ liệu để rollback nếu cần
   */
  private void backupData() {
    backupPosition = ObjectCloner.duplicate(position, AmmPosition.class);
    backupPool = ObjectCloner.duplicate(pool, AmmPool.class);
    backupAccount0 = ObjectCloner.duplicate(account0, Account.class);
    backupAccount1 = ObjectCloner.duplicate(account1, Account.class);
    backupLowerTick = ObjectCloner.duplicate(lowerTick, Tick.class);
    backupUpperTick = ObjectCloner.duplicate(upperTick, Tick.class);
    backupTickBitmap = ObjectCloner.duplicate(tickBitmap, TickBitmap.class);

    logger.debug("Data backed up successfully");
  }

  /**
   * Thu thập phí còn lại trước khi đóng vị thế
   */
  private void collectRemainingFees() {
    BigDecimal[] feeGrowthInside = LiquidityUtils.getFeeGrowthInside(
        lowerTick,
        upperTick,
        pool.getCurrentTick(),
        pool.getFeeGrowthGlobal0(),
        pool.getFeeGrowthGlobal1());

    BigDecimal feeGrowthInside0 = feeGrowthInside[0];
    BigDecimal feeGrowthInside1 = feeGrowthInside[1];

    // Tính toán phí đã tích lũy kể từ lần thu phí cuối cùng
    BigDecimal tokensOwed0 = LiquidityUtils.calculateFeesOwed(
        position.getLiquidity(),
        feeGrowthInside0,
        position.getFeeGrowthInside0Last());

    BigDecimal tokensOwed1 = LiquidityUtils.calculateFeesOwed(
        position.getLiquidity(),
        feeGrowthInside1,
        position.getFeeGrowthInside1Last());

    // Kiểm tra xem có phí để thu không
    if (tokensOwed0.compareTo(BigDecimal.ZERO) > 0 || tokensOwed1.compareTo(BigDecimal.ZERO) > 0) {
      logger.info("Collecting remaining fees before closing position: {}", position.getIdentifier());

      // Cập nhật vị thế - lưu phí đã thu và cập nhật feeGrowthInside
      position.updateAfterCollectFee(
          tokensOwed0,
          tokensOwed1,
          feeGrowthInside0,
          feeGrowthInside1);

      // Cập nhật số dư tài khoản
      account0.increaseAvailableBalance(tokensOwed0);
      account1.increaseAvailableBalance(tokensOwed1);

      // Tạo lịch sử thu phí
      createFeeCollectionHistories(tokensOwed0, tokensOwed1);

      logger.info("Collected fees - token0: {}, token1: {}", tokensOwed0, tokensOwed1);
    }
  }

  /**
   * Tạo lịch sử giao dịch với thông tin chung
   */
  private AccountHistory createHistory(Account account, Account backupAccount, String operationId,
      String operationType) {
    AccountHistory history = new AccountHistory(
        account.getKey(),
        operationId,
        operationType);

    history.setBalanceValues(
        backupAccount.getAvailableBalance(),
        account.getAvailableBalance(),
        backupAccount.getFrozenBalance(),
        account.getFrozenBalance());

    return history;
  }

  /**
   * Tạo lịch sử thu phí
   */
  private void createFeeCollectionHistories(BigDecimal tokensOwed0, BigDecimal tokensOwed1) {
    List<AccountHistory> histories = new ArrayList<>();

    if (tokensOwed0.compareTo(BigDecimal.ZERO) > 0) {
      histories.add(createHistory(
          account0,
          backupAccount0,
          position.getIdentifier(),
          OperationType.AMM_POSITION_COLLECT_FEE.getValue()));
    }

    if (tokensOwed1.compareTo(BigDecimal.ZERO) > 0) {
      histories.add(createHistory(
          account1,
          backupAccount1,
          position.getIdentifier(),
          OperationType.AMM_POSITION_COLLECT_FEE.getValue()));
    }

    for (AccountHistory history : histories) {
      result.addAccountHistory(history);
    }
  }

  /**
   * Xử lý đóng vị thế và cập nhật các dữ liệu liên quan
   */
  private boolean processClosePosition() {
    try {
      // Lấy liquidity từ position
      BigDecimal liquidity = position.getLiquidity();

      // 1. Cập nhật ticks
      updateTicksForRemoveLiquidity(liquidity);

      // 2. Cập nhật tickBitmap nếu cần
      updateTickBitmaps();

      // 3. Trả lại token cho chủ sở hữu và lấy số lượng token đã trả
      BigDecimal[] tokenAmounts = transferTokensBackToOwner();
      BigDecimal amount0 = tokenAmounts[0];
      BigDecimal amount1 = tokenAmounts[1];

      // 4. Cập nhật pool - giảm liquidity tổng và TVL
      updatePoolLiquidity(liquidity, amount0, amount1);

      // 5. Cập nhật position - đóng vị thế và cập nhật thông tin
      updatePosition(amount0, amount1);

      // 6. Tạo lịch sử giao dịch cho đóng vị thế
      createClosePositionHistories();

      return true;
    } catch (Exception e) {
      logger.error("Error processing position closing: {}", e.getMessage(), e);
      position.markError("Error: " + e.getMessage());
      disruptorEvent.setErrorMessage("Error: " + e.getMessage());
      return false;
    }
  }

  /**
   * Cập nhật position - đóng vị thế và cập nhật thông tin liên quan
   *
   * @param amount0 Lượng token0 đã rút
   * @param amount1 Lượng token1 đã rút
   */
  private void updatePosition(BigDecimal amount0, BigDecimal amount1) {
    // Tính toán feeGrowthInside cuối cùng
    BigDecimal[] feeGrowthInside = LiquidityUtils.getFeeGrowthInside(
        lowerTick,
        upperTick,
        pool.getCurrentTick(),
        pool.getFeeGrowthGlobal0(),
        pool.getFeeGrowthGlobal1());

    // Đóng vị thế và cập nhật tất cả thông tin liên quan trong một lần gọi
    if (!position.closePosition(amount0, amount1, feeGrowthInside[0], feeGrowthInside[1])) {
      throw new RuntimeException("Failed to close position: " + position.getIdentifier());
    }
  }

  /**
   * Cập nhật liquidity trong pool khi đóng vị thế
   */
  private void updatePoolLiquidity(BigDecimal liquidity, BigDecimal amount0, BigDecimal amount1) {
    // Cập nhật liquidity của pool theo logic Uniswap V3
    logger.info("Pool before update: {}", pool.toString());

    // Sử dụng phương thức updateForClosePosition để cập nhật cả liquidity và TVL
    pool.updateForClosePosition(liquidity, amount0, amount1);

    // KHÔNG cần cập nhật TVL lại ở đây vì updateForClosePosition đã xử lý

    // Nếu liquidity = 0, đặt TVL về 0 theo logic Uniswap V3
    if (pool.getLiquidity().compareTo(BigDecimal.ZERO) == 0) {
      pool.setTotalValueLockedToken0(BigDecimal.ZERO);
      pool.setTotalValueLockedToken1(BigDecimal.ZERO);
      logger.info("Set TVL to zero as liquidity is zero");
    }

    // Cập nhật thời gian của pool
    updatePoolObservation();
  }

  /**
   * Cập nhật oracle observation cho pool
   * Đơn giản hóa vì không sử dụng Observation
   */
  private void updatePoolObservation() {
    try {
      // Chỉ cập nhật timestamp của pool để đánh dấu thời điểm gần nhất có thay đổi
      pool.setUpdatedAt(System.currentTimeMillis());
      logger.info("Updated pool timestamp for position close");
    } catch (Exception e) {
      logger.warn("Failed to update pool timestamp: {}", e.getMessage());
    }
  }

  /**
   * Tạo lịch sử giao dịch cho việc đóng vị thế
   */
  private void createClosePositionHistories() {
    List<AccountHistory> histories = new ArrayList<>();

    // Tạo lịch sử cho cả hai tài khoản
    histories.add(createHistory(
        account0,
        backupAccount0,
        position.getIdentifier(),
        OperationType.AMM_POSITION_CLOSE.getValue()));

    histories.add(createHistory(
        account1,
        backupAccount1,
        position.getIdentifier(),
        OperationType.AMM_POSITION_CLOSE.getValue()));

    // Thêm vào result
    result.setAccountHistory(histories.get(0)); // Đặt history chính

    // Thêm các lịch sử bổ sung
    for (int i = 1; i < histories.size(); i++) {
      result.addAccountHistory(histories.get(i));
    }

    logger.info("Created account histories for close position transaction");
  }

  /**
   * Cập nhật tick data khi gỡ bỏ thanh khoản (Uniswap V3)
   */
  private void updateTicksForRemoveLiquidity(BigDecimal liquidity) {
    // Sử dụng BigDecimal trực tiếp thay vì chuyển đổi sang BigInteger
    BigDecimal maxLiquidity = AmmPoolConfig.MAX_LIQUIDITY_PER_TICK;

    // Lấy giá trị tick và fee growth hiện tại từ pool
    int currentTick = pool.getCurrentTick();
    BigDecimal feeGrowthGlobal0 = pool.getFeeGrowthGlobal0();
    BigDecimal feeGrowthGlobal1 = pool.getFeeGrowthGlobal1();

    // Cập nhật lower tick - trừ liquidity (negate)
    // không phải tick trên nên upper = false
    BigDecimal negativeLiquidity = liquidity.negate();
    lowerTick.update(negativeLiquidity, false, maxLiquidity, currentTick, feeGrowthGlobal0, feeGrowthGlobal1);

    // Cập nhật upper tick - trừ liquidity (negate)
    // tick trên nên upper = true
    upperTick.update(negativeLiquidity, true, maxLiquidity, currentTick, feeGrowthGlobal0, feeGrowthGlobal1);

    logger.info("Updated ticks with removing liquidity: lower: {}, upper: {}",
        lowerTick.getTickIndex(), upperTick.getTickIndex());
  }

  /**
   * Cập nhật tickBitmap - xóa bit nếu tick không còn được sử dụng
   */
  private void updateTickBitmaps() {
    // Kiểm tra xem có còn vị thế nào dùng các tick này không
    // Nếu lowerTick và upperTick không còn liquidityGross, có thể xóa khỏi bitmap
    if (lowerTick.getLiquidityGross().compareTo(BigDecimal.ZERO) == 0) {
      tickBitmap.clearBit(lowerTick.getTickIndex());
      logger.info("Cleared lower tick from bitmap: {}", lowerTick.getTickIndex());
    }

    if (upperTick.getLiquidityGross().compareTo(BigDecimal.ZERO) == 0) {
      tickBitmap.clearBit(upperTick.getTickIndex());
      logger.info("Cleared upper tick from bitmap: {}", upperTick.getTickIndex());
    }
  }

  /**
   * Chuyển token trong vị thế về lại cho chủ sở hữu
   *
   * @return Mảng gồm 2 giá trị [amount0, amount1] chỉ lượng token đã trả về
   */
  BigDecimal[] transferTokensBackToOwner() {
    // Lấy số lượng token trong vị thế để hoàn trả
    // Trong Uniswap V3, số lượng token được tính lại dựa trên liquidityDelta âm
    // và phạm vi giá của vị thế

    BigDecimal liquidity = position.getLiquidity();
    BigDecimal amount0 = position.getAmount0();
    BigDecimal amount1 = position.getAmount1();

    // Lấy thông tin tick và giá hiện tại
    int currentTick = pool.getCurrentTick();
    int tickLower = position.getTickLowerIndex();
    int tickUpper = position.getTickUpperIndex();

    // Tính các giá trị căn bậc hai của giá từ tick
    BigDecimal sqrtPriceCurrent;
    BigDecimal sqrtPriceLower;
    BigDecimal sqrtPriceUpper;

    try {
      // Sử dụng TickMath để tính toán căn bậc hai của giá từ tick
      // Chú ý: Trong Uniswap V3 gốc, các giá trị này được lưu dưới dạng Q64.96
      // fixed-point
      // Trong Java chúng ta sử dụng BigDecimal với độ chính xác đầy đủ
      sqrtPriceCurrent = TickMath.getSqrtRatioAtTick(currentTick);
      sqrtPriceLower = TickMath.getSqrtRatioAtTick(tickLower);
      sqrtPriceUpper = TickMath.getSqrtRatioAtTick(tickUpper);

      // Tính lại số lượng token cần trả lại cho người dùng theo logic Uniswap V3
      if (currentTick < tickLower) {
        // Vị thế hoàn toàn ở dưới range - tất cả là token0
        logger.info("Position below current price range - all liquidity in token0");

        // Công thức Uniswap V3: amount0 = L * (sqrtUpper - sqrtLower) / (sqrtUpper *
        // sqrtLower)
        // Đây là công thức để tính lượng token0 khi thanh khoản nằm hoàn toàn dưới
        // range hiện tại
        BigDecimal numerator = sqrtPriceUpper.subtract(sqrtPriceLower);
        BigDecimal denominator = sqrtPriceUpper.multiply(sqrtPriceLower);
        BigDecimal calculatedAmount0 = liquidity.multiply(numerator).divide(denominator, AmmPoolConfig.DECIMAL_SCALE,
            AmmPoolConfig.ROUNDING_MODE);

        // Kiểm tra xem amount0 đã có sẵn có chính xác không
        if (calculatedAmount0.compareTo(amount0) != 0) {
          logger.warn(
              "Số lượng token0 tính được ({}) khác với số lượng lưu trữ trong position ({}), sử dụng giá trị tính toán",
              calculatedAmount0, amount0);
          amount0 = calculatedAmount0;
        }

        // Khi ở dưới range, token1 = 0
        amount1 = BigDecimal.ZERO;

      } else if (currentTick < tickUpper) {
        // Vị thế một phần trong range - cần tính cả token0 và token1
        logger.info("Position partially in current price range");

        // Công thức Uniswap V3 cho token0 khi vị thế nằm một phần trong range:
        // amount0 = L * (sqrtUpper - sqrtCurrent) / (sqrtUpper * sqrtCurrent)
        BigDecimal numerator0 = sqrtPriceUpper.subtract(sqrtPriceCurrent);
        BigDecimal denominator0 = sqrtPriceUpper.multiply(sqrtPriceCurrent);
        BigDecimal calculatedAmount0 = liquidity.multiply(numerator0).divide(denominator0, AmmPoolConfig.DECIMAL_SCALE,
            AmmPoolConfig.ROUNDING_MODE);

        // Công thức Uniswap V3 cho token1 khi vị thế nằm một phần trong range:
        // amount1 = L * (sqrtCurrent - sqrtLower)
        BigDecimal calculatedAmount1 = liquidity.multiply(sqrtPriceCurrent.subtract(sqrtPriceLower))
            .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

        // Cập nhật lại số lượng nếu có sự khác biệt
        if (calculatedAmount0.compareTo(amount0) != 0 || calculatedAmount1.compareTo(amount1) != 0) {
          logger.warn("Số lượng token tính toán khác với số lượng lưu trữ, sử dụng giá trị tính toán");
          amount0 = calculatedAmount0;
          amount1 = calculatedAmount1;
        }

      } else {
        // Vị thế hoàn toàn ở trên range - tất cả là token1
        logger.info("Position above current price range - all liquidity in token1");

        // Công thức Uniswap V3: amount1 = L * (sqrtUpper - sqrtLower)
        // Đây là công thức để tính lượng token1 khi thanh khoản nằm hoàn toàn trên
        // range hiện tại
        BigDecimal calculatedAmount1 = liquidity.multiply(sqrtPriceUpper.subtract(sqrtPriceLower))
            .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

        // Kiểm tra xem amount1 đã có sẵn có chính xác không
        if (calculatedAmount1.compareTo(amount1) != 0) {
          logger.warn(
              "Số lượng token1 tính được ({}) khác với số lượng lưu trữ trong position ({}), sử dụng giá trị tính toán",
              calculatedAmount1, amount1);
          amount1 = calculatedAmount1;
        }

        // Khi ở trên range, token0 = 0
        amount0 = BigDecimal.ZERO;
      }
    } catch (Exception e) {
      logger.error("Lỗi khi tính toán số lượng token: {}", e.getMessage());
      logger.info("Sử dụng giá trị mặc định từ position: amount0={}, amount1={}", amount0, amount1);
      // Sử dụng giá trị mặc định từ position nếu có lỗi
    }

    logger.info("Số lượng token sẽ trả về: amount0={}, amount1={}", amount0, amount1);

    // Lưu lại giá trị token để trả về trước khi tăng số dư tài khoản
    BigDecimal tokenAmount0 = amount0;
    BigDecimal tokenAmount1 = amount1;

    // Hoàn trả token cho người dùng
    if (amount0.compareTo(BigDecimal.ZERO) > 0) {
      account0.increaseAvailableBalance(amount0);
      logger.info("Đã trả {} token cho tài khoản {}", amount0, account0.getKey());
    }

    if (amount1.compareTo(BigDecimal.ZERO) > 0) {
      account1.increaseAvailableBalance(amount1);
      logger.info("Đã trả {} token cho tài khoản {}", amount1, account1.getKey());
    }

    // Đánh dấu rằng token đã được trả về (để đảm bảo không trả lại nhiều lần nếu
    // hàm được gọi lại)
    position.setAmount0(BigDecimal.ZERO);
    position.setAmount1(BigDecimal.ZERO);

    // Trả về số lượng token đã hoàn trả
    return new BigDecimal[] { tokenAmount0, tokenAmount1 };
  }

  /**
   * Lưu tất cả thay đổi vào cache - thực hiện một lần duy nhất
   */
  private void saveToCache() {
    try {
      // 1. Lưu position vào cache
      ammPositionCache.updateAmmPosition(position);

      // 2. Lưu pool vào cache
      ammPoolCache.updateAmmPool(pool);

      // 3. Lưu accounts vào cache
      accountCache.updateAccount(account0);
      accountCache.updateAccount(account1);

      // 4. Lưu ticks vào cache
      tickCache.updateTick(lowerTick);
      tickCache.updateTick(upperTick);

      // Thêm ticks vào result
      result.addTick(lowerTick);
      result.addTick(upperTick);

      // 5. Lưu tick bitmap
      tickBitmapCache.updateTickBitmap(tickBitmap);

      // 6. Lưu danh sách lịch sử giao dịch
      List<AccountHistory> histories = result.getAccountHistories();
      for (AccountHistory history : histories) {
        accountHistoryCache.updateAccountHistory(history);
      }

      // 7. Cập nhật dữ liệu vào kết quả
      result.setAmmPool(pool);
      result.setAmmPosition(position);

      // 8. Thêm các tài khoản vào result
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
      // Khôi phục lại dữ liệu
      if (backupPosition != null) {
        ammPositionCache.updateAmmPosition(backupPosition);
      }

      if (backupPool != null) {
        ammPoolCache.updateAmmPool(backupPool);
      }

      if (backupAccount0 != null) {
        accountCache.updateAccount(backupAccount0);
      }

      if (backupAccount1 != null) {
        accountCache.updateAccount(backupAccount1);
      }

      if (backupLowerTick != null) {
        tickCache.updateTick(backupLowerTick);
      }

      if (backupUpperTick != null) {
        tickCache.updateTick(backupUpperTick);
      }

      if (backupTickBitmap != null) {
        tickBitmapCache.updateTickBitmap(backupTickBitmap);
      }

      logger.info("Successfully rolled back changes");
    } catch (Exception e) {
      logger.error("Error during rollback: {}", e.getMessage(), e);
    }
  }
}
