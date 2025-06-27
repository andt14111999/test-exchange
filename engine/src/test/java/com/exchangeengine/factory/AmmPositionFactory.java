package com.exchangeengine.factory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;

import com.exchangeengine.model.AmmPosition;
import com.exchangeengine.util.TestModelFactory;
import com.exchangeengine.util.ammPool.AmmPoolConfig;

public class AmmPositionFactory {

  public static Model<AmmPosition> ammPositionModel() {
    String randomId = UUID.randomUUID().toString();

    return Instancio.of(AmmPosition.class)
        .set(Select.field(AmmPosition::getIdentifier), randomId)
        .set(Select.field(AmmPosition::getPoolPair), "USDT-VND")
        .set(Select.field(AmmPosition::getOwnerAccountKey0), "account1")
        .set(Select.field(AmmPosition::getOwnerAccountKey1), "account1")
        .set(Select.field(AmmPosition::getStatus), AmmPosition.STATUS_PENDING)
        .set(Select.field(AmmPosition::getTickLowerIndex), -100)
        .set(Select.field(AmmPosition::getTickUpperIndex), 100)
        .set(Select.field(AmmPosition::getLiquidity), new BigDecimal("1000000"))
        .set(Select.field(AmmPosition::getSlippage), AmmPoolConfig.MAX_SLIPPAGE)
        .set(Select.field(AmmPosition::getAmount0), new BigDecimal("1000"))
        .set(Select.field(AmmPosition::getAmount1), new BigDecimal("1000"))
        .set(Select.field(AmmPosition::getAmount0Initial), new BigDecimal("1000"))
        .set(Select.field(AmmPosition::getAmount1Initial), new BigDecimal("1000"))
        .set(Select.field(AmmPosition::getFeeGrowthInside0Last), BigDecimal.ZERO)
        .set(Select.field(AmmPosition::getFeeGrowthInside1Last), BigDecimal.ZERO)
        .set(Select.field(AmmPosition::getTokensOwed0), BigDecimal.ZERO)
        .set(Select.field(AmmPosition::getTokensOwed1), BigDecimal.ZERO)
        .set(Select.field(AmmPosition::getFeeCollected0), BigDecimal.ZERO)
        .set(Select.field(AmmPosition::getFeeCollected1), BigDecimal.ZERO)
        .set(Select.field(AmmPosition::getCreatedAt), Instant.now().toEpochMilli())
        .set(Select.field(AmmPosition::getUpdatedAt), Instant.now().toEpochMilli())
        .set(Select.field(AmmPosition::getStoppedAt), 0L)
        .toModel();
  }

  public static AmmPosition createDefaultAmmPosition() {
    return Instancio.create(ammPositionModel());
  }

  public static AmmPosition createCustomAmmPosition(Map<String, Object> customFields) {
    AmmPosition position = createDefaultAmmPosition();
    return TestModelFactory.customize(position, customFields);
  }
}
