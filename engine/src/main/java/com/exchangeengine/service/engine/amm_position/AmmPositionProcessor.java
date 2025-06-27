package com.exchangeengine.service.engine.amm_position;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.model.event.AmmPositionEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.service.engine.amm_position.processor.AmmPositionCreateProcessor;
import com.exchangeengine.service.engine.amm_position.processor.AmmPositionCollectFeeProcessor;
import com.exchangeengine.service.engine.amm_position.processor.AmmPositionCloseProcessor;

public class AmmPositionProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AmmPositionProcessor.class);
  private ProcessResult result;
  private final DisruptorEvent event;

  public AmmPositionProcessor(DisruptorEvent event) {
    this.result = new ProcessResult(event);
    this.event = event;
  }

  public ProcessResult process() {
    try {
      AmmPositionEvent ammPositionEvent = event.getAmmPositionEvent();
      if (ammPositionEvent == null) {
        throw new IllegalArgumentException("AmmPositionEvent is null");
      }

      OperationType operationType = ammPositionEvent.getOperationType();
      if (operationType == null) {
        throw new IllegalArgumentException("Operation type is null");
      }

      if (OperationType.AMM_POSITION_CREATE.equals(operationType)) {
        result = new AmmPositionCreateProcessor(event).process();
      } else if (OperationType.AMM_POSITION_COLLECT_FEE.equals(operationType)) {
        result = new AmmPositionCollectFeeProcessor(event).process();
      } else if (OperationType.AMM_POSITION_CLOSE.equals(operationType)) {
        result = new AmmPositionCloseProcessor(event).process();
      } else {
        throw new IllegalArgumentException("Unsupported operation type: " + operationType);
      }

    } catch (Exception e) {
      event.setErrorMessage(e.getMessage());
      logger.error("Error processing AmmPosition event: {}", e.getMessage(), e);
    } finally {
      if (result.getAmmPosition().isEmpty()) {
        result.setAmmPosition(new AmmPosition("error_amm_position", "error_pool"));
      }
    }

    return result;
  }
}
