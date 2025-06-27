package com.exchangeengine.service.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.AmmPoolEvent;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.MerchantEscrowEvent;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.model.event.AmmOrderEvent;
import com.exchangeengine.model.event.TradeEvent;
import com.exchangeengine.model.event.OfferEvent;
import com.exchangeengine.model.event.BalancesLockEvent;

public class EngineHandler {
  private static final Logger logger = LoggerFactory.getLogger(EngineHandler.class);

  // Singleton instance
  private static volatile EngineHandler instance;

  private final EngineDisruptorService disruptorService;

  /**
   * Lấy instance của EngineHandler.
   *
   * @return Instance của EngineHandler
   */
  public static synchronized EngineHandler getInstance() {
    if (instance == null) {
      instance = new EngineHandler();
    }
    return instance;
  }

  /**
   * Thiết lập instance kiểm thử (chỉ sử dụng cho testing)
   *
   * @param testInstance Instance kiểm thử cần thiết lập
   */
  public static void setTestInstance(EngineHandler testInstance) {
    instance = testInstance;
  }

  /**
   * Constructor mặc định sử dụng Singleton Pattern.
   * Private để đảm bảo Singleton pattern.
   */
  private EngineHandler() {
    this.disruptorService = EngineDisruptorService.getInstance();
    logger.info("EngineHandler initialized");
  }

  /**
   * Constructor với EngineDisruptorService.
   * Giữ lại để tương thích ngược và dễ kiểm thử.
   * Private để đảm bảo Singleton pattern.
   */
  private EngineHandler(EngineDisruptorService disruptorService) {
    this.disruptorService = disruptorService;
  }

  public void deposit(CoinDepositEvent coinDepositEvent) {
    logger.info("Processing deposit: {}", coinDepositEvent);
    disruptorService.deposit(coinDepositEvent);
  }

  public void withdraw(CoinWithdrawalEvent coinWithdrawalEvent) {
    logger.info("Processing withdrawal: {}", coinWithdrawalEvent);
    disruptorService.withdraw(coinWithdrawalEvent);
  }

  public void createCoinAccount(AccountEvent accountEvent) {
    logger.info("Processing create coin account: {}", accountEvent);
    disruptorService.createCoinAccount(accountEvent);
  }

  public void ammPool(AmmPoolEvent ammPoolEvent) {
    logger.info("Processing amm pool: {}", ammPoolEvent);
    disruptorService.ammPool(ammPoolEvent);
  }

  public void merchantEscrow(MerchantEscrowEvent merchantEscrowEvent) {
    logger.info("Processing merchant escrow operation: {}", merchantEscrowEvent);
    disruptorService.merchantEscrow(merchantEscrowEvent);
  }

  public void ammPosition(AmmPositionEvent ammPositionEvent) {
    logger.info("Processing amm position: {}", ammPositionEvent);
    disruptorService.ammPosition(ammPositionEvent);
  }

  public void ammOrder(AmmOrderEvent ammOrderEvent) {
    logger.info("Processing amm order: {}", ammOrderEvent);
    disruptorService.ammOrder(ammOrderEvent);
  }

  public void processTrade(TradeEvent tradeEvent) {
    logger.info("Processing trade operation: {}", tradeEvent);
    disruptorService.trade(tradeEvent);
  }

  public void processOffer(OfferEvent offerEvent) {
    logger.info("Processing offer operation: {}", offerEvent);
    disruptorService.offer(offerEvent);
  }
  
  public void balancesLock(BalancesLockEvent balancesLockEvent) {
    logger.info("Processing balances lock operation: {}", balancesLockEvent);
    disruptorService.balancesLock(balancesLockEvent);
  }
}
