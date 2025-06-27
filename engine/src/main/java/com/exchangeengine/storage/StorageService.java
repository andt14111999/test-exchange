package com.exchangeengine.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.storage.cache.*;

/**
 * Service quản lý lưu trữ dữ liệu và tương tác giữa các lớp cache.
 * Tách biệt logic lưu trữ để dễ quản lý và tái sử dụng.
 */
public class StorageService {
  private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

  private static volatile StorageService instance;

  private final AmmPoolCache ammPoolCache;
  private final AccountCache accountCache;
  private final DepositCache depositCache;
  private final WithdrawalCache withdrawalCache;
  private final AccountHistoryCache accountHistoryCache;
  private final MerchantEscrowCache merchantEscrowCache;
  private final EventCache eventCache;
  private final TickCache tickCache;
  private final TickBitmapCache tickBitmapCache;
  private final AmmPositionCache ammPositionCache;
  private final AmmOrderCache ammOrderCache;
  private final OfferCache offerCache;
  private final TradeCache tradeCache;
  private final BalanceLockCache balanceLockCache;

  /**
   * Lấy instance của StorageService.
   *
   * @return Instance của StorageService
   */
  public static synchronized StorageService getInstance() {
    if (instance == null) {
      instance = new StorageService();
      instance.initializeCache();
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(StorageService testInstance) {
    instance = testInstance;
  }

  /**
   * Reset instance về null (chỉ sử dụng cho testing)
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Khởi tạo StorageService.
   * Private để đảm bảo Singleton pattern.
   */
  private StorageService() {
    this.ammPoolCache = AmmPoolCache.getInstance();
    this.accountCache = AccountCache.getInstance();
    this.depositCache = DepositCache.getInstance();
    this.withdrawalCache = WithdrawalCache.getInstance();
    this.accountHistoryCache = AccountHistoryCache.getInstance();
    this.merchantEscrowCache = MerchantEscrowCache.getInstance();
    this.eventCache = EventCache.getInstance();
    this.tickCache = TickCache.getInstance();
    this.tickBitmapCache = TickBitmapCache.getInstance();
    this.ammPositionCache = AmmPositionCache.getInstance();
    this.ammOrderCache = AmmOrderCache.getInstance();
    this.offerCache = OfferCache.getInstance();
    this.tradeCache = TradeCache.getInstance();
    this.balanceLockCache = BalanceLockCache.getInstance();
  }

  /**
   * Khởi tạo cache từ dữ liệu trong RocksDB.
   * Cập nhật cache chỉ khi dữ liệu trong RocksDB mới hơn hoặc cache trống.
   */
  public void initializeCache() {
    logger.info("Đang khởi tạo cache từ RocksDB...");
    getAccountCache().initializeAccountCache();
    getDepositCache().initializeDepositCache();
    getWithdrawalCache().initializeWithdrawalCache();
    getAmmPoolCache().initializeAmmPoolCache();
    getTickCache().initializeTickCache();
    getTickBitmapCache().initializeTickBitmapCache();
    getAmmPositionCache().initializeAmmPositionCache();
    getMerchantEscrowCache().initializeMerchantEscrowCache();
    getAmmOrderCache().initializeAmmOrderCache();
    getOfferCache().initializeOfferCache();
    getTradeCache().initializeTradeCache();
    getBalanceLockCache().loadBalanceLocksFromRocksDB();
    logger.info("Đã khởi tạo cache thành công");
  }

  public AmmPoolCache getAmmPoolCache() {
    return ammPoolCache;
  }

  public AccountCache getAccountCache() {
    return accountCache;
  }

  public DepositCache getDepositCache() {
    return depositCache;
  }

  public WithdrawalCache getWithdrawalCache() {
    return withdrawalCache;
  }

  public EventCache getEventCache() {
    return eventCache;
  }

  public AccountHistoryCache getAccountHistoryCache() {
    return accountHistoryCache;
  }

  public MerchantEscrowCache getMerchantEscrowCache() {
    return merchantEscrowCache;
  }

  public TickCache getTickCache() {
    return tickCache;
  }

  public TickBitmapCache getTickBitmapCache() {
    return tickBitmapCache;
  }

  public AmmPositionCache getAmmPositionCache() {
    return ammPositionCache;
  }
  
  public OfferCache getOfferCache() {
    return offerCache;
  }
  
  public TradeCache getTradeCache() {
    return tradeCache;
  }

  public AmmOrderCache getAmmOrderCache() {
    return ammOrderCache;
  }
  
  public BalanceLockCache getBalanceLockCache() {
    return balanceLockCache;
  }

  public boolean shouldFlush() {
    return getAccountCache().accountCacheShouldFlush() ||
        getDepositCache().depositCacheShouldFlush() ||
        getWithdrawalCache().withdrawalCacheShouldFlush() ||
        getAccountHistoryCache().historiesCacheShouldFlush() ||
        getAmmPoolCache().ammPoolCacheShouldFlush() ||
        getTickCache().tickCacheShouldFlush() ||
        getTickBitmapCache().tickBitmapCacheShouldFlush() ||
        getAmmPositionCache().ammPositionCacheShouldFlush() ||
        getMerchantEscrowCache().merchantEscrowCacheShouldFlush() ||
        getOfferCache().offerCacheShouldFlush() ||
        getTradeCache().tradeCacheShouldFlush();
  }

  /**
   * Lưu dữ liệu vào RocksDB.
   */
  public void flushToDisk() {
    getAccountCache().flushAccountToDisk();
    getDepositCache().flushDepositToDisk();
    getWithdrawalCache().flushWithdrawalToDisk();
    getAccountHistoryCache().flushHistoryToDisk();
    getAmmPoolCache().flushAmmPoolToDisk();
    getTickCache().flushTicksToDisk();
    getTickBitmapCache().flushTickBitmapsToDisk();
    getAmmPositionCache().flushAmmPositionToDisk();
    getMerchantEscrowCache().flushMerchantEscrowToDisk();
    getOfferCache().flushOfferToDisk();
    getTradeCache().flushTradeToDisk();
    getBalanceLockCache().saveBalanceLockBatch();
  }

  /**
   * Đóng service và lưu tất cả dữ liệu còn lại.
   */
  public void shutdown() {
    logger.info("Đang đóng StorageService, lưu dữ liệu còn lại...");
    flushToDisk();
    logger.info("Đã đóng thành công");
  }
}
