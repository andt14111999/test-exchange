package com.exchangeengine.model.event;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.AmmPool;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.Tick;
import com.exchangeengine.model.TickBitmap;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.TickBitmapCache;
import com.exchangeengine.storage.cache.TickCache;

import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for Tick events
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickEvent extends BaseEvent {
  private String poolPair;

  protected AmmPoolCache getAmmPoolCache() {
    return AmmPoolCache.getInstance();
  }

  protected TickCache getTickCache() {
    return TickCache.getInstance();
  }

  protected TickBitmapCache getTickBitmapCache() {
    return TickBitmapCache.getInstance();
  }

  @Override
  public String getProducerEventId() {
    return this.poolPair;
  }

  /**
   * Get all ticks from bitmap for pool pair
   *
   * @return List of ticks
   */
  public List<Tick> fetchTicksFromBitmap() {
    List<Tick> ticks = new ArrayList<>();

    // Check if pool pair exists
    if (poolPair == null || poolPair.isEmpty()) {
      return ticks;
    }

    // Get bitmap for pool pair
    var tickBitmapOpt = getTickBitmapCache().getTickBitmap(poolPair);
    if (!tickBitmapOpt.isPresent()) {
      return ticks;
    }

    TickBitmap tickBitmap = tickBitmapOpt.get();

    // Get all initialized ticks from bitmap using getSetBits
    List<Integer> setBits = tickBitmap.getSetBits();
    for (Integer tickIndex : setBits) {
      String tickKey = poolPair + "-" + tickIndex;
      getTickCache().getTick(tickKey).ifPresent(ticks::add);
    }

    return ticks;
  }

  /**
   * Parse data from JsonNode
   *
   * @param messageJson JsonNode containing the data
   * @return Parsed TickEvent
   */
  public TickEvent parserData(JsonNode messageJson) {
    String eventId = messageJson.path("eventId").asText();
    ActionType tmpActionType = ActionType.fromValue(messageJson.path("actionType").asText());
    String tmpActionId = messageJson.path("actionId").asText();
    OperationType tmpOperationType = OperationType.fromValue(messageJson.path("operationType").asText());
    String tmpPoolPair = messageJson.path("poolPair").asText();

    setEventId(eventId);
    setActionType(tmpActionType);
    setActionId(tmpActionId);
    setOperationType(tmpOperationType);
    setPoolPair(tmpPoolPair);

    return this;
  }

  /**
   * Validate the required fields for this event
   *
   * @throws IllegalArgumentException if validation fails
   */
  public void validate() {
    List<String> objectErrors = super.validateRequiredFields();

    if (poolPair == null || poolPair.isEmpty()) {
      objectErrors.add("Pool pair is required");
    } else {
      // Check if pool pair exists only if poolPair is not empty
      AmmPoolCache ammPoolCache = getAmmPoolCache();
      Optional<AmmPool> poolOpt = ammPoolCache.getAmmPool(poolPair);
      if (!poolOpt.isPresent()) {
        objectErrors.add("AMM Pool does not exist: " + poolPair);
      } else if (!poolOpt.get().isActive()) {
        objectErrors.add("AMM Pool is not active: " + poolPair);
      }
    }

    if (objectErrors.size() > 0) {
      throw new IllegalArgumentException("validate TickEvent: " + String.join(", ", objectErrors));
    }
  }
}
