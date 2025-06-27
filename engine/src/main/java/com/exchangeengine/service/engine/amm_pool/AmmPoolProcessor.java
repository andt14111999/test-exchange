package com.exchangeengine.service.engine.amm_pool;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.TickBitmap;
import com.exchangeengine.model.event.AmmPoolEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.TickBitmapCache;

/**
 * Processor for AMM Pool events
 */
public class AmmPoolProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AmmPoolProcessor.class);
  private final AmmPoolCache ammPoolCache;
  private final TickBitmapCache tickBitmapCache;
  private final ProcessResult result;
  private final DisruptorEvent event;

  /**
   * Constructor với DisruptorEvent.
   *
   * @param event Sự kiện cần xử lý
   */
  public AmmPoolProcessor(DisruptorEvent event) {
    this.ammPoolCache = AmmPoolCache.getInstance();
    this.tickBitmapCache = TickBitmapCache.getInstance();
    this.result = new ProcessResult(event);
    this.event = event;
  }

  public ProcessResult process() {
    AmmPoolEvent ammPoolEvent = event.getAmmPoolEvent();

    try {
      if (OperationType.AMM_POOL_CREATE.equals(ammPoolEvent.getOperationType())) {
        processCreateEvent(ammPoolEvent);
      } else if (OperationType.AMM_POOL_UPDATE.equals(ammPoolEvent.getOperationType())) {
        processUpdateEvent(ammPoolEvent);
      } else {
        throw new IllegalArgumentException("Unsupported operation type: " + ammPoolEvent.getOperationType());
      }

      event.successes();
    } catch (Exception e) {
      event.setErrorMessage(e.getMessage());
      logger.error("Error processing AmmPool event: {}", e.getMessage(), e);
    } finally {
      if (result.getAmmPool().isEmpty()) {
        result.setAmmPool(new AmmPool("error_amm_pool"));
      }
    }

    return result;
  }

  private void processCreateEvent(AmmPoolEvent ammPoolEvent) {
    Optional<AmmPool> existingPool = ammPoolEvent.fetchAmmPool(false);
    if (existingPool.isPresent()) {
      result.setAmmPool(existingPool.get());
      throw new IllegalArgumentException("AmmPool already exists: " + ammPoolEvent.getPair());
    } else {
      AmmPool newPool = ammPoolEvent.toAmmPool(false);

      newPool.calculateInitialPriceAndTick();
      ammPoolCache.updateAmmPool(newPool);
      createTickBitmap(newPool.getPair());
      result.setAmmPool(newPool);

      logger.info("Created new AmmPool: {}", newPool.toString());
    }
  }

  private void processUpdateEvent(AmmPoolEvent ammPoolEvent) {
    AmmPool existingPool = ammPoolEvent.toAmmPool(true);
    boolean hasChanged = false;

    hasChanged = existingPool.update(
        ammPoolEvent.isActive(),
        ammPoolEvent.getFeePercentage(),
        ammPoolEvent.getFeeProtocolPercentage(),
        ammPoolEvent.getInitPrice());

    if (hasChanged) {
      ammPoolCache.updateAmmPool(existingPool);
      createTickBitmap(existingPool.getPair());
      logger.info("Updated AmmPool: {}", existingPool.getPair());
    } else {
      event.setErrorMessage("No changes to update for AmmPool: " + existingPool.getPair());
      logger.info("No changes to update for AmmPool: {}", existingPool.getPair());
    }

    result.setAmmPool(existingPool);
  }

  private void createTickBitmap(String poolPair) {
    if (!tickBitmapCache.getTickBitmap(poolPair).isPresent()) {
      TickBitmap bitmap = new TickBitmap(poolPair);
      tickBitmapCache.updateTickBitmap(bitmap);
      logger.debug("Created empty tick bitmap for pool: {}, word index: {}", poolPair, 0);
    }
  }
}
