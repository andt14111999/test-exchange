package com.exchangeengine.factory.event;

import com.exchangeengine.model.event.DisruptorEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.instancio.Instancio;
import org.instancio.Model;

import java.time.Instant;

import static org.instancio.Select.field;

/**
 * Factory class to create DisruptorEvent objects for testing purposes
 */
public class DisruptorEventFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create a model for DisruptorEvent with default values
   *
   * @return a model for DisruptorEvent
   */
  public static Model<DisruptorEvent> model() {
    return Instancio.of(DisruptorEvent.class)
        .set(field(DisruptorEvent::isSuccess), true)
        .set(field(DisruptorEvent::getErrorMessage), null)
        .set(field(DisruptorEvent::getTimestamp), Instant.now().toEpochMilli())
        .set(field(DisruptorEvent::getAccountEvent), null)
        .set(field(DisruptorEvent::getCoinDepositEvent), null)
        .set(field(DisruptorEvent::getCoinWithdrawalEvent), null)
        .set(field(DisruptorEvent::getAmmPoolEvent), null)
        .set(field(DisruptorEvent::getMerchantEscrowEvent), null)
        .set(field(DisruptorEvent::getAmmPositionEvent), null)
        .set(field(DisruptorEvent::getAmmOrderEvent), null)
        .set(field(DisruptorEvent::getTradeEvent), null)
        .set(field(DisruptorEvent::getOfferEvent), null)
        .set(field(DisruptorEvent::getBalancesLockEvent), null)
        .toModel();
  }

  /**
   * Create a valid DisruptorEvent with default values and no specific event
   *
   * @return a valid DisruptorEvent
   */
  public static DisruptorEvent create() {
    return Instancio.create(model());
  }

  /**
   * Create a DisruptorEvent containing an AccountEvent
   *
   * @return DisruptorEvent with AccountEvent
   */
  public static DisruptorEvent withAccountEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getAccountEvent), AccountEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing a CoinDepositEvent
   *
   * @return DisruptorEvent with CoinDepositEvent
   */
  public static DisruptorEvent withCoinDepositEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getCoinDepositEvent), CoinDepositEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing a CoinWithdrawalEvent
   *
   * @return DisruptorEvent with CoinWithdrawalEvent
   */
  public static DisruptorEvent withCoinWithdrawalEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getCoinWithdrawalEvent), CoinWithdrawalEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing an AmmPoolEvent
   *
   * @return DisruptorEvent with AmmPoolEvent
   */
  public static DisruptorEvent withAmmPoolEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getAmmPoolEvent), AmmPoolEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing a MerchantEscrowEvent
   *
   * @return DisruptorEvent with MerchantEscrowEvent
   */
  public static DisruptorEvent withMerchantEscrowEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getMerchantEscrowEvent), MerchantEscrowEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing an AmmPositionEvent
   *
   * @return DisruptorEvent with AmmPositionEvent
   */
  public static DisruptorEvent withAmmPositionEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getAmmPositionEvent), AmmPositionEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing an AmmOrderEvent
   *
   * @return DisruptorEvent with AmmOrderEvent
   */
  public static DisruptorEvent withAmmOrderEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getAmmOrderEvent), AmmOrderEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing a TradeEvent
   *
   * @return DisruptorEvent with TradeEvent
   */
  public static DisruptorEvent withTradeEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getTradeEvent), TradeEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing an OfferEvent
   *
   * @return DisruptorEvent with OfferEvent
   */
  public static DisruptorEvent withOfferEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getOfferEvent), OfferEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent containing a BalancesLockEvent
   *
   * @return DisruptorEvent with BalancesLockEvent
   */
  public static DisruptorEvent withBalancesLockEvent() {
    return Instancio.of(model())
        .set(field(DisruptorEvent::getBalancesLockEvent), BalancesLockEventFactory.create())
        .create();
  }

  /**
   * Create a DisruptorEvent with error
   *
   * @param errorMessage error message
   * @return DisruptorEvent with error
   */
  public static DisruptorEvent withError(String errorMessage) {
    return Instancio.of(model())
        .set(field(DisruptorEvent::isSuccess), false)
        .set(field(DisruptorEvent::getErrorMessage), errorMessage)
        .create();
  }

  /**
   * Convert a DisruptorEvent to JsonNode
   *
   * @param event DisruptorEvent to convert
   * @return JsonNode representation
   */
  public static JsonNode toJsonNode(DisruptorEvent event) {
    ObjectNode jsonNode = objectMapper.createObjectNode();
    jsonNode.put("isSuccess", event.isSuccess());
    jsonNode.put("errorMessage", event.getErrorMessage());
    jsonNode.put("timestamp", event.getTimestamp());

    if (event.getAccountEvent() != null) {
      jsonNode.set("accountEvent", AccountEventFactory.toJsonNode(event.getAccountEvent()));
    }

    return jsonNode;
  }

  public static JsonNode createJsonNode() {
    return toJsonNode(create());
  }
}
