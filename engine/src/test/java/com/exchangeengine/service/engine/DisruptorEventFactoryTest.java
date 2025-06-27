package com.exchangeengine.service.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.exchangeengine.factory.event.DisruptorEventFactory;
import com.exchangeengine.model.event.DisruptorEvent;
import com.lmax.disruptor.EventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DisruptorEventFactoryTest {

  private com.exchangeengine.service.engine.DisruptorEventFactory factory;

  @BeforeEach
  void setUp() {
    factory = new com.exchangeengine.service.engine.DisruptorEventFactory();
  }

  @Test
  void newInstance_ShouldCreateNewEventWithDefaultValues() {
    // When
    DisruptorEvent event = factory.newInstance();

    // Then
    assertNotNull(event, "Event should not be null");
    assertTrue(event.isSuccess(), "Event should be success by default");
    assertNull(event.getAccountEvent(), "AccountEvent should be null by default");
    assertNull(event.getCoinDepositEvent(), "CoinDepositEvent should be null by default");
    assertNull(event.getCoinWithdrawalEvent(), "CoinWithdrawalEvent should be null by default");
    assertNull(event.getAmmPoolEvent(), "AmmPoolEvent should be null by default");
    assertNull(event.getErrorMessage(), "ErrorMessage should be null by default");
    assertTrue(event.getTimestamp() > 0, "Timestamp should be set");
  }

  @Test
  void implementsEventFactory_ShouldImplementCorrectInterface() {
    // Then
    assertTrue(factory instanceof EventFactory, "Factory should implement EventFactory interface");
  }

  @Test
  void multipleInstances_ShouldCreateUniqueObjects() {
    // When
    DisruptorEvent event1 = factory.newInstance();
    DisruptorEvent event2 = factory.newInstance();

    // Then
    assertNotSame(event1, event2, "Factory should create unique instances");

    // Set some data on first event
    event1.setSuccess(false);
    event1.setErrorMessage("test-error");

    // Verify second event is unaffected
    assertNull(event2.getErrorMessage(), "Second event's errorMessage should remain null");
    assertTrue(event2.isSuccess(), "Second event's success flag should remain true");
  }

  @Test
  void factoryShouldCreateConsistentWithTestFactory() {
    // When using the service factory
    DisruptorEvent serviceEvent = factory.newInstance();

    // When using the test factory
    DisruptorEvent testEvent = DisruptorEventFactory.create();

    // Then
    assertEquals(serviceEvent.isSuccess(), testEvent.isSuccess(), "Success flag should match");
    assertNull(serviceEvent.getErrorMessage(), "ErrorMessage should be null in both factories");
    assertNull(testEvent.getErrorMessage(), "ErrorMessage should be null in both factories");
  }
}
