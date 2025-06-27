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
 * Processor xử lý việc tạo vị thế AMM mới (Uniswap V3)
 */
public class AmmPositionCreateProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AmmPositionCreateProcessor.class);

  // Input event và output result
  private final AmmPositionEvent event;
  private final DisruptorEvent disruptorEvent;
  private final ProcessResult result;

  // Các cache service
  private final AmmPoolCache ammPoolCache;
  private final AmmPositionCache ammPositionCache;
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
  private AmmPool backupPool;
  private Account backupAccount0;
  private Account backupAccount1;
  private Tick backupLowerTick;
  private Tick backupUpperTick;
  private TickBitmap backupTickBitmap;

  // Dữ liệu tính toán
  private BigDecimal liquidity;

  /**
   * Khởi tạo processor với AmmPositionEvent
   */
  public AmmPositionCreateProcessor(DisruptorEvent disruptorEvent) {
    this.disruptorEvent = disruptorEvent;
    this.event = disruptorEvent.getAmmPositionEvent();
    this.result = new ProcessResult(disruptorEvent);

    // Khởi tạo các cache service
    this.ammPoolCache = AmmPoolCache.getInstance();
    this.ammPositionCache = AmmPositionCache.getInstance();
    this.accountCache = AccountCache.getInstance();
    this.tickCache = TickCache.getInstance();
    this.tickBitmapCache = TickBitmapCache.getInstance();
    this.accountHistoryCache = AccountHistoryCache.getInstance();
  }

  /**
   * Xử lý tạo vị thế mới
   */
  public ProcessResult process() {
    try {
      logger.info("Processing AMM position creation: {}", event.getIdentifier());

      // 1. Validate và lấy dữ liệu
      fetchData();

      List<String> validationErrors = calculateActualAmountsAndValidate();
      if (!validationErrors.isEmpty()) {
        String errorMsg = String.join("; ", validationErrors);
        position.markError(errorMsg);
        disruptorEvent.setErrorMessage(errorMsg);
        return result;
      }

      // 2. Backup dữ liệu
      backupData();

      // 3. Tính toán và thêm position
      if (!addPositionToPool()) {
        return result;
      }

      // 4. Lưu thay đổi vào cache
      saveToCache();

      // 5. Hoàn thành
      logger.info("Successfully created AMM position: {}", position.getIdentifier());
      disruptorEvent.successes();
      return result;

    } catch (Exception e) {
      logger.error("Error processing AMM position creation: {}", e.getMessage(), e);
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
   * Lưu ý: Phần lớn việc validate nằm trong AmmPositionEvent
   */
  private void fetchData() {
    position = event.toAmmPosition(false);
    result.setAmmPosition(position);

    pool = position.getPool().get();
    account0 = position.getAccount0();
    account1 = position.getAccount1();
    tickBitmap = position.getTickBitmap().get();

    upperTick = position.getTickUpper().orElseGet(() -> new Tick(position.getPoolPair(), position.getTickUpperIndex()));
    lowerTick = position.getTickLower().orElseGet(() -> new Tick(position.getPoolPair(), position.getTickLowerIndex()));
  }

  /**
   * Tính toán và kiểm tra slippage cho các giá trị thanh khoản và số lượng token
   * thực tế.
   * Quá trình này gồm các bước:
   * 1. Tính toán thanh khoản dự kiến dựa trên số lượng token ban đầu người dùng
   * cung cấp
   * 2. Tính toán số lượng token thực tế cần dùng dựa trên thanh khoản
   * 3. Kiểm tra slippage - sự chênh lệch giữa số lượng token dự kiến và thực tế
   * 4. Kiểm tra số dư tài khoản có đủ không
   *
   * Phương thức này không chỉ validate mà còn tính toán các giá trị thực tế cho
   * position
   */
  private List<String> calculateActualAmountsAndValidate() {
    List<String> errors = new ArrayList<>();

    // Tính thanh khoản dự kiến
    BigDecimal sqrtRatioCurrentTick = TickMath.getSqrtRatioAtTick(pool.getCurrentTick());
    BigDecimal sqrtRatioLowerTick = TickMath.getSqrtRatioAtTick(position.getTickLowerIndex());
    BigDecimal sqrtRatioUpperTick = TickMath.getSqrtRatioAtTick(position.getTickUpperIndex());

    // Tính thanh khoản dự kiến
    BigDecimal estimatedLiquidity = LiquidityUtils.calculateLiquidityForAmounts(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        position.getAmount0Initial(),
        position.getAmount1Initial());

    // Làm tròn đến DECIMAL_SCALE
    estimatedLiquidity = estimatedLiquidity.setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    // Việc kiểm tra giá trị liquidity tối thiểu đã được thực hiện trong
    // validateRequiredFields của AmmPosition
    // nên không cần kiểm tra lại ở đây

    // Lưu giá trị liquidity để sử dụng sau
    this.liquidity = estimatedLiquidity;

    // Tính số lượng token thực tế cần thiết bằng cách tái sử dụng
    // calculateActualAmounts
    BigDecimal[] actualAmounts = calculateActualAmounts(estimatedLiquidity);
    BigDecimal actualAmount0 = actualAmounts[0];
    BigDecimal actualAmount1 = actualAmounts[1];

    // Kiểm tra slippage
    BigDecimal slippage = position.getSlippage();
    boolean slippageExceeded = false;
    BigDecimal slippageAmount0 = BigDecimal.ZERO;
    BigDecimal slippageAmount1 = BigDecimal.ZERO;

    if (position.getAmount0Initial().compareTo(BigDecimal.ZERO) > 0) {
      slippageAmount0 = position.getAmount0Initial().subtract(actualAmount0)
          .divide(position.getAmount0Initial(), AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE)
          .abs();
      if (slippageAmount0.compareTo(slippage) > 0) {
        slippageExceeded = true;
      }
    }

    if (position.getAmount1Initial().compareTo(BigDecimal.ZERO) > 0) {
      slippageAmount1 = position.getAmount1Initial().subtract(actualAmount1)
          .divide(position.getAmount1Initial(), AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE)
          .abs();
      if (slippageAmount1.compareTo(slippage) > 0) {
        slippageExceeded = true;
      }
    }

    if (slippageExceeded) {
      errors.add(String.format(
          "Slippage tolerance exceeded: actual slippage token0: %s, token1: %s, max allowed: %s",
          slippageAmount0, slippageAmount1, slippage));
    }

    // Kiểm tra số dư dựa trên số lượng token thực tế sau khi tính slippage
    if (account0.getAvailableBalance().compareTo(actualAmount0) < 0) {
      errors.add(String.format("Insufficient balance in account 0: %s < %s",
          account0.getAvailableBalance(), actualAmount0));
    }

    if (account1.getAvailableBalance().compareTo(actualAmount1) < 0) {
      errors.add(String.format("Insufficient balance in account 1: %s < %s",
          account1.getAvailableBalance(), actualAmount1));
    }

    // Nếu không có lỗi, cập nhật giá trị thực tế vào position
    if (errors.isEmpty()) {
      position.setAmount0(actualAmount0);
      position.setAmount1(actualAmount1);
    }

    return errors;
  }

  /**
   * Backup dữ liệu để rollback nếu cần
   */
  private void backupData() {
    backupPool = ObjectCloner.duplicate(pool, AmmPool.class);
    backupAccount0 = ObjectCloner.duplicate(account0, Account.class);
    backupAccount1 = ObjectCloner.duplicate(account1, Account.class);
    backupLowerTick = ObjectCloner.duplicate(lowerTick, Tick.class);
    backupUpperTick = ObjectCloner.duplicate(upperTick, Tick.class);
    backupTickBitmap = ObjectCloner.duplicate(tickBitmap, TickBitmap.class);

    logger.debug("Data backed up successfully");
  }

  /**
   * Thêm position vào pool theo quy trình Uniswap V3
   */
  private boolean addPositionToPool() {
    try {
      // Tính toán số lượng token thực tế
      BigDecimal[] actualAmounts = calculateActualAmounts(liquidity);
      BigDecimal actualAmount0 = actualAmounts[0];
      BigDecimal actualAmount1 = actualAmounts[1];

      // Kiểm tra xem position có nằm trong range hiện tại không
      boolean isInRange = position.getTickLowerIndex() <= pool.getCurrentTick()
          && position.getTickUpperIndex() > pool.getCurrentTick();

      // Cập nhật pool - sử dụng phương thức mới updateForAddPosition
      pool.updateForAddPosition(liquidity, isInRange, actualAmount0, actualAmount1);

      // Cập nhật tick data (Uniswap V3)
      updateTicksForLiquidity(liquidity);

      // Cập nhật tickBitmap
      updateTickBitmaps();

      // Tính toán feeGrowthInside - sử dụng phương thức từ LiquidityUtils
      BigDecimal[] feeGrowthInside = LiquidityUtils.getFeeGrowthInside(
          lowerTick,
          upperTick,
          pool.getCurrentTick(),
          pool.getFeeGrowthGlobal0(),
          pool.getFeeGrowthGlobal1());

      BigDecimal feeGrowthInside0Last = feeGrowthInside[0];
      BigDecimal feeGrowthInside1Last = feeGrowthInside[1];

      // Cập nhật vị thế
      position.updateAfterCreate(
          position.getTickLowerIndex(),
          position.getTickUpperIndex(),
          liquidity,
          actualAmount0,
          actualAmount1,
          feeGrowthInside0Last,
          feeGrowthInside1Last);

      // Cập nhật số dư tài khoản
      updateAccountBalances(actualAmount0, actualAmount1);

      // Tạo lịch sử giao dịch
      createAccountHistories();

      // Mở vị thế
      if (!position.openPosition()) {
        position.markError("Failed to open position");
        disruptorEvent.setErrorMessage("Failed to open position");
        return false;
      }

      return true;
    } catch (Exception e) {
      logger.error("Error adding position to pool: {}", e.getMessage(), e);
      position.markError("Error: " + e.getMessage());
      disruptorEvent.setErrorMessage("Error: " + e.getMessage());
      return false;
    }
  }

  /**
   * Tính toán số lượng token thực tế dựa trên thanh khoản
   */
  private BigDecimal[] calculateActualAmounts(BigDecimal liquidity) {
    BigDecimal sqrtRatioCurrentTick = TickMath.getSqrtRatioAtTick(pool.getCurrentTick());
    BigDecimal sqrtRatioLowerTick = TickMath.getSqrtRatioAtTick(position.getTickLowerIndex());
    BigDecimal sqrtRatioUpperTick = TickMath.getSqrtRatioAtTick(position.getTickUpperIndex());

    BigDecimal[] amounts = LiquidityUtils.getAmountsForLiquidity(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        liquidity);

    // Làm tròn đến DECIMAL_SCALE
    amounts[0] = amounts[0].setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    amounts[1] = amounts[1].setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    logger.info("Calculated actual amounts - token0: {}, token1: {}", amounts[0], amounts[1]);
    return amounts;
  }

  /**
   * Cập nhật tick data khi thêm thanh khoản (Uniswap V3)
   */
  private void updateTicksForLiquidity(BigDecimal liquidity) {
    // Sử dụng BigDecimal trực tiếp thay vì chuyển đổi sang BigInteger
    BigDecimal maxLiquidity = AmmPoolConfig.MAX_LIQUIDITY_PER_TICK;

    // Kiểm tra giới hạn liquidityGross
    BigDecimal lowerTickLiquidityAfter = lowerTick.getLiquidityGross().add(liquidity);
    BigDecimal upperTickLiquidityAfter = upperTick.getLiquidityGross().add(liquidity);

    if (lowerTickLiquidityAfter.compareTo(maxLiquidity) > 0) {
      throw new IllegalArgumentException(String.format(
          "Lower tick %d liquidityGross exceeds maximum: %s > %s",
          lowerTick.getTickIndex(), lowerTickLiquidityAfter, maxLiquidity));
    }

    if (upperTickLiquidityAfter.compareTo(maxLiquidity) > 0) {
      throw new IllegalArgumentException(String.format(
          "Upper tick %d liquidityGross exceeds maximum: %s > %s",
          upperTick.getTickIndex(), upperTickLiquidityAfter, maxLiquidity));
    }

    // Lấy giá trị tick và fee growth hiện tại từ pool
    int currentTick = pool.getCurrentTick();
    BigDecimal feeGrowthGlobal0 = pool.getFeeGrowthGlobal0();
    BigDecimal feeGrowthGlobal1 = pool.getFeeGrowthGlobal1();

    // Đánh dấu tick là initialized nếu chưa được khởi tạo
    if (!lowerTick.isInitialized()) {
      lowerTick.setInitialized(true);
      lowerTick.setTickInitializedTimestamp(System.currentTimeMillis());
    }

    if (!upperTick.isInitialized()) {
      upperTick.setInitialized(true);
      upperTick.setTickInitializedTimestamp(System.currentTimeMillis());
    }

    // Cập nhật lower tick - không phải tick trên nên upper = false
    lowerTick.update(liquidity, false, maxLiquidity, currentTick, feeGrowthGlobal0, feeGrowthGlobal1);

    // Cập nhật upper tick - tick trên nên upper = true
    upperTick.update(liquidity, true, maxLiquidity, currentTick, feeGrowthGlobal0, feeGrowthGlobal1);

    logger.info("Updated ticks with liquidity: lower: {}, upper: {}",
        lowerTick.getTickIndex(), upperTick.getTickIndex());
  }

  /**
   * Cập nhật tickBitmap
   */
  private void updateTickBitmaps() {
    // Tính position trong bitmap và set bit
    int lowerBitPos = position.getTickLowerIndex();
    int upperBitPos = position.getTickUpperIndex();

    tickBitmap.setBit(lowerBitPos);
    tickBitmap.setBit(upperBitPos);

    logger.info("Updated tick bitmap for pool: {}, ticks: [{}, {}]",
        position.getPoolPair(), lowerBitPos, upperBitPos);
  }

  /**
   * Cập nhật số dư tài khoản
   */
  private void updateAccountBalances(BigDecimal amount0, BigDecimal amount1) {
    // Sử dụng phương thức có sẵn trong Account để giảm số dư khả dụng
    // Các phương thức này đã xử lý việc làm tròn đến DEFAULT_SCALE
    account0.decreaseAvailableBalance(amount0);
    account1.decreaseAvailableBalance(amount1);

    logger.info("Updated account balances - account0: {}, account1: {}",
        account0.getAvailableBalance(), account1.getAvailableBalance());
  }

  /**
   * Tạo lịch sử giao dịch cho các tài khoản
   */
  private void createAccountHistories() {
    // Tạo lịch sử cho tài khoản token0
    AccountHistory history0 = new AccountHistory(
        account0.getKey(),
        position.getIdentifier(),
        OperationType.AMM_POSITION_CREATE.getValue());
    history0.setBalanceValues(
        backupAccount0.getAvailableBalance(),
        account0.getAvailableBalance(),
        backupAccount0.getFrozenBalance(),
        account0.getFrozenBalance());

    // Tạo lịch sử cho tài khoản token1
    AccountHistory history1 = new AccountHistory(
        account1.getKey(),
        position.getIdentifier(),
        OperationType.AMM_POSITION_CREATE.getValue());
    history1.setBalanceValues(
        backupAccount1.getAvailableBalance(),
        account1.getAvailableBalance(),
        backupAccount1.getFrozenBalance(),
        account1.getFrozenBalance());

    // Lưu lịch sử vào cache
    accountHistoryCache.updateAccountHistory(history0);
    accountHistoryCache.updateAccountHistory(history1);

    // Thêm lịch sử vào result
    result.setAccountHistory(history0); // Đặt history chính
    result.addAccountHistory(history1); // Thêm history thứ hai

    logger.info("Created account histories for transaction");
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

      // 6. Cập nhật dữ liệu vào kết quả
      result.setAmmPool(pool);
      result.setAmmPosition(position);

      // 7. Thêm các tài khoản vào result
      result.addAccount(account0); // Sử dụng addAccount thay vì setAccount
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
      if (backupAccount0 != null) {
        accountCache.updateAccount(backupAccount0);
      }

      if (backupAccount1 != null) {
        accountCache.updateAccount(backupAccount1);
      }

      if (backupPool != null) {
        ammPoolCache.updateAmmPool(backupPool);
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
