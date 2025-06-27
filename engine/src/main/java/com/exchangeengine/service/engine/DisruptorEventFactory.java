package com.exchangeengine.service.engine;

import com.exchangeengine.model.event.DisruptorEvent;
import com.lmax.disruptor.EventFactory;

public class DisruptorEventFactory implements EventFactory<DisruptorEvent> {
  @Override
  public DisruptorEvent newInstance() {
    return new DisruptorEvent();
  }
}
