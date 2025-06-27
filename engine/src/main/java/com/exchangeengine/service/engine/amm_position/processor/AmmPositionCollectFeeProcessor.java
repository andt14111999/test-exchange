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
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AccountHistoryCache;
import com.exchangeengine.storage.cache.AmmPositionCache;
import com.exchangeengine.util.ObjectCloner;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import com.exchangeengine.util.ammPool.LiquidityUtils;

/**
 * Processor xử lý việc thu phí từ vị thế AMM (Uniswap V3)
 */
public class AmmPositionCollectFeeProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AmmPositionCollectFeeProcessor.class);

  // Input event và output result
  private final AmmPositionEvent event;
  private final DisruptorEvent disruptorEvent;
  private final ProcessResult result;

  // Các cache service
  private final AmmPositionCache ammPositionCache;
  private final AccountCache accountCache;
  private final AccountHistoryCache accountHistoryCache;

  // Dữ liệu chính
  private AmmPosition position;
  private AmmPool pool;
  private Account account0;
  private Account account1;
  private Tick lowerTick;
  private Tick upperTick;

  // Dữ liệu backup
  private AmmPosition backupPosition;
  private Account backupAccount0;
  private Account backupAccount1;

  /**
   * Khởi tạo processor với DisruptorEvent trực tiếp
   */
  public AmmPositionCollectFeeProcessor(DisruptorEvent disruptorEvent) {
    this.disruptorEvent = disruptorEvent;
    this.event = disruptorEvent.getAmmPositionEvent();
    this.result = new ProcessResult(disruptorEvent);

    // Khởi tạo các cache service
    this.ammPositionCache = AmmPositionCache.getInstance();
    this.accountCache = AccountCache.getInstance();
    this.accountHistoryCache = AccountHistoryCache.getInstance();
  }

  /**
   * Xử lý thu phí từ vị thế
   */
  public ProcessResult process() {
    try {
      logger.info("Processing AMM position fee collection: {}", event.getIdentifier());

      // 1. Validate và lấy dữ liệu
      fetchData();

      List<String> validationErrors = validatePosition();
      if (!validationErrors.isEmpty()) {
        String errorMsg = String.join("; ", validationErrors);
        position.markError(errorMsg);
        disruptorEvent.setErrorMessage(errorMsg);
        return result;
      }

      // 2. Backup dữ liệu
      backupData();

      // 3. Tính toán và thu phí
      if (!collectFees()) {
        return result;
      }

      // 4. Lưu thay đổi vào cache
      saveToCache();

      // 5. Hoàn thành
      logger.info("Successfully collected fees from AMM position: {}", position.getIdentifier());
      disruptorEvent.successes();
      return result;

    } catch (Exception e) {
      logger.error("Error processing AMM position fee collection: {}", e.getMessage(), e);
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
    lowerTick = position.getTickLower().orElse(null);
    upperTick = position.getTickUpper().orElse(null);
  }

  /**
   * Kiểm tra vị thế có hợp lệ để thu phí không
   */
  private List<String> validatePosition() {
    List<String> errors = new ArrayList<>();

    // Kiểm tra trạng thái vị thế
    if (!position.isOpen()) {
      errors.add("Position is not open: " + position.getIdentifier());
    }

    // Kiểm tra thanh khoản
    if (position.getLiquidity().compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Position has no liquidity: " + position.getIdentifier());
    }

    return errors;
  }

  /**
   * Backup dữ liệu để rollback nếu cần
   */
  private void backupData() {
    backupPosition = ObjectCloner.duplicate(position, AmmPosition.class);
    backupAccount0 = ObjectCloner.duplicate(account0, Account.class);
    backupAccount1 = ObjectCloner.duplicate(account1, Account.class);

    logger.debug("Data backed up successfully");
  }

  /**
   * Tính toán và thu phí từ vị thế
   */
  private boolean collectFees() {
    try {
      // Tính toán phí bên trong range (tương tự Uniswap V3)
      BigDecimal[] feeGrowthInside = LiquidityUtils.getFeeGrowthInside(
          lowerTick,
          upperTick,
          pool.getCurrentTick(),
          pool.getFeeGrowthGlobal0(),
          pool.getFeeGrowthGlobal1());

      BigDecimal feeGrowthInside0 = feeGrowthInside[0];
      BigDecimal feeGrowthInside1 = feeGrowthInside[1];

      // Tính toán phí đã tích lũy
      BigDecimal tokensOwed0 = calculateFeesOwed(
          position.getLiquidity(),
          feeGrowthInside0,
          position.getFeeGrowthInside0Last());

      BigDecimal tokensOwed1 = calculateFeesOwed(
          position.getLiquidity(),
          feeGrowthInside1,
          position.getFeeGrowthInside1Last());

      // Làm tròn đến DECIMAL_SCALE
      tokensOwed0 = tokensOwed0.setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
      tokensOwed1 = tokensOwed1.setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

      // Kiểm tra xem có phí để thu không
      if (tokensOwed0.compareTo(BigDecimal.ZERO) <= 0 && tokensOwed1.compareTo(BigDecimal.ZERO) <= 0) {
        logger.info("No fees to collect for position: {}", position.getIdentifier());
        return true;
      }

      // Cập nhật vị thế - lưu phí đã thu và cập nhật feeGrowthInside
      boolean updated = position.updateAfterCollectFee(
          tokensOwed0,
          tokensOwed1,
          feeGrowthInside0,
          feeGrowthInside1);

      if (!updated) {
        position.markError("Failed to update position with collected fees");
        disruptorEvent.setErrorMessage("Failed to update position with collected fees");
        return false;
      }

      // Cập nhật số dư tài khoản
      updateAccountBalances(tokensOwed0, tokensOwed1);

      // Tạo lịch sử giao dịch
      createAccountHistories(tokensOwed0, tokensOwed1);

      logger.info("Collected fees - token0: {}, token1: {}", tokensOwed0, tokensOwed1);
      return true;
    } catch (Exception e) {
      logger.error("Error collecting fees: {}", e.getMessage(), e);
      position.markError("Error: " + e.getMessage());
      disruptorEvent.setErrorMessage("Error: " + e.getMessage());
      return false;
    }
  }

  /**
   * Tính toán phí đã tích lũy dựa trên feeGrowth
   */
  private BigDecimal calculateFeesOwed(BigDecimal liquidity, BigDecimal feeGrowthInside,
      BigDecimal feeGrowthInsideLast) {
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
  }

  /**
   * Cập nhật số dư tài khoản
   */
  private void updateAccountBalances(BigDecimal amount0, BigDecimal amount1) {
    // Cập nhật số dư khả dụng
    if (amount0.compareTo(BigDecimal.ZERO) > 0) {
      account0.increaseAvailableBalance(amount0);
    }

    if (amount1.compareTo(BigDecimal.ZERO) > 0) {
      account1.increaseAvailableBalance(amount1);
    }

    logger.info("Updated account balances - account0: {}, account1: {}",
        account0.getAvailableBalance(), account1.getAvailableBalance());
  }

  /**
   * Tạo lịch sử giao dịch cho các tài khoản
   */
  private void createAccountHistories(BigDecimal amount0, BigDecimal amount1) {
    // Tạo lịch sử cho tài khoản token0 nếu có phí
    if (amount0.compareTo(BigDecimal.ZERO) > 0) {
      AccountHistory history0 = new AccountHistory(
          account0.getKey(),
          position.getIdentifier(),
          OperationType.AMM_POSITION_COLLECT_FEE.getValue());
      history0.setBalanceValues(
          backupAccount0.getAvailableBalance(),
          account0.getAvailableBalance(),
          backupAccount0.getFrozenBalance(),
          account0.getFrozenBalance());

      accountHistoryCache.updateAccountHistory(history0);
      result.setAccountHistory(history0);
    }

    // Tạo lịch sử cho tài khoản token1 nếu có phí
    if (amount1.compareTo(BigDecimal.ZERO) > 0) {
      AccountHistory history1 = new AccountHistory(
          account1.getKey(),
          position.getIdentifier(),
          OperationType.AMM_POSITION_COLLECT_FEE.getValue());
      history1.setBalanceValues(
          backupAccount1.getAvailableBalance(),
          account1.getAvailableBalance(),
          backupAccount1.getFrozenBalance(),
          account1.getFrozenBalance());

      accountHistoryCache.updateAccountHistory(history1);
      result.addAccountHistory(history1);
    }

    logger.info("Created account histories for transaction");
  }

  /**
   * Lưu tất cả thay đổi vào cache
   */
  private void saveToCache() {
    try {
      // 1. Lưu position vào cache
      ammPositionCache.updateAmmPosition(position);

      // 2. Lưu accounts vào cache
      accountCache.updateAccount(account0);
      accountCache.updateAccount(account1);

      // 3. Cập nhật dữ liệu vào kết quả
      result.setAmmPosition(position);

      // Thêm ticks vào result nếu có
      if (lowerTick != null) {
        result.addTick(lowerTick);
      }
      if (upperTick != null) {
        result.addTick(upperTick);
      }

      // 4. Thêm các tài khoản vào result
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

      if (backupAccount0 != null) {
        accountCache.updateAccount(backupAccount0);
      }

      if (backupAccount1 != null) {
        accountCache.updateAccount(backupAccount1);
      }

      logger.info("Successfully rolled back changes");
    } catch (Exception e) {
      logger.error("Error during rollback: {}", e.getMessage(), e);
    }
  }
}
