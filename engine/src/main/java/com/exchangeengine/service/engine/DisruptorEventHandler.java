package com.exchangeengine.service.engine;

import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.EventHandlerAction;
import com.exchangeengine.service.engine.coin_account.CoinAccountProcessor;
import com.exchangeengine.service.engine.deposit.DepositProcessor;
import com.exchangeengine.service.engine.withdrawal.WithdrawalProcessor;
import com.exchangeengine.service.engine.amm_pool.AmmPoolProcessor;
import com.exchangeengine.service.engine.merchant_escrow.MerchantEscrowProcessor;
import com.exchangeengine.service.engine.amm_position.AmmPositionProcessor;
import com.exchangeengine.service.engine.amm_order.AmmOrderProcessor;
import com.exchangeengine.service.engine.trade.TradeProcessor;
import com.exchangeengine.service.engine.offer.OfferProcessor;
import com.exchangeengine.service.engine.balances_lock.BalancesLockProcessor;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.StorageService;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for processing events from Disruptor.
 * Focus on Business Logic Processor - only perform calculations.
 */
public class DisruptorEventHandler implements EventHandler<DisruptorEvent> {
  private static final Logger logger = LoggerFactory.getLogger(DisruptorEventHandler.class);

  private final StorageService storageService;
  private final OutputProcessor outputProcessor;

  /**
   * Constructor mặc định, lấy instance tự động.
   */
  public DisruptorEventHandler() {
    this.storageService = StorageService.getInstance();
    this.outputProcessor = OutputProcessor.getInstance();
  }

  @Override
  public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) {
    ProcessResult result = null;

    try {
      if (storageService.getEventCache().isEventProcessed(event.getEventId())) {
        // Nếu event đã được xử lý, tạo ProcessResult thành công
        result = ProcessResult.success(event);
        return;
      }

      String eventHandler = event.getEvent().getEventHandler();

      switch (eventHandler) {
        case EventHandlerAction.ACCOUNT_EVENT:
          result = new CoinAccountProcessor(event).process();
          break;

        case EventHandlerAction.DEPOSIT_EVENT:
          result = new DepositProcessor(event).process();
          break;

        case EventHandlerAction.WITHDRAWAL_EVENT:
          result = new WithdrawalProcessor(event).process();
          break;

        case EventHandlerAction.AMM_POOL_EVENT:
          result = new AmmPoolProcessor(event).process();
          break;

        case EventHandlerAction.MERCHANT_ESCROW_EVENT:
          result = new MerchantEscrowProcessor(event).process();
          break;

        case EventHandlerAction.AMM_POSITION_EVENT:
          result = new AmmPositionProcessor(event).process();
          break;

        case EventHandlerAction.TRADE_EVENT:
          result = new TradeProcessor(event).process();
          break;

        case EventHandlerAction.OFFER_EVENT:
          result = new OfferProcessor(event).process();
          break;

        case EventHandlerAction.AMM_ORDER_EVENT:
          result = new AmmOrderProcessor(event).process();
          break;
          
        case EventHandlerAction.BALANCES_LOCK_EVENT:
          result = new BalancesLockProcessor(event).process();
          break;

        default:
          throw new IllegalArgumentException("Unknown event type: " + eventHandler);
      }

      logger.info("Event processed successfully: eventId={}, eventHandler={}",
          event.getEventId(), eventHandler);
    } catch (Exception e) {
      result = ProcessResult.error(event, e.getMessage());
      logger.error("Error processing event: {}", e.getMessage(), e);
    } finally {
      storageService.getEventCache().updateEvent(event.getEventId());
      outputProcessor.processOutput(result, endOfBatch);
    }
  }

  /**
   * shutdown handler and release resources
   */
  public void shutdown() {
    logger.info("Shutting down DisruptorEventHandler");
  }
}
