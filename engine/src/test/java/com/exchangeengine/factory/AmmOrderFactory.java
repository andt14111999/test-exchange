package com.exchangeengine.factory;

import com.exchangeengine.model.AmmOrder;
import com.exchangeengine.util.TestModelFactory;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Factory tạo mẫu AmmOrder cho thử nghiệm
 */
public class AmmOrderFactory {

  /**
   * Tạo một model AmmOrder với các giá trị mặc định
   *
   * @return Model<AmmOrder> sử dụng để tạo instance AmmOrder
   */
  public static Model<AmmOrder> ammOrderModel() {
    String randomId = UUID.randomUUID().toString();

    return Instancio.of(AmmOrder.class)
        .set(Select.field(AmmOrder::getIdentifier), randomId)
        .set(Select.field(AmmOrder::getPoolPair), "USDT-VND")
        .set(Select.field(AmmOrder::getOwnerAccountKey0), "account1")
        .set(Select.field(AmmOrder::getOwnerAccountKey1), "account1")
        .set(Select.field(AmmOrder::getStatus), AmmOrder.STATUS_PROCESSING)
        .set(Select.field(AmmOrder::getZeroForOne), true)
        .set(Select.field(AmmOrder::getAmountSpecified), new BigDecimal("1000"))
        .set(Select.field(AmmOrder::getAmountEstimated), new BigDecimal("900"))
        .set(Select.field(AmmOrder::getAmountReceived), BigDecimal.ZERO)
        .set(Select.field(AmmOrder::getAmountActual), BigDecimal.ZERO)
        .set(Select.field(AmmOrder::getBeforeTickIndex), 0)
        .set(Select.field(AmmOrder::getAfterTickIndex), 0)
        .set(Select.field(AmmOrder::getFees), new HashMap<>())
        .set(Select.field(AmmOrder::getSlippage), AmmPoolConfig.DEFAULT_SLIPPAGE)
        .set(Select.field(AmmOrder::getCreatedAt), Instant.now().toEpochMilli())
        .set(Select.field(AmmOrder::getUpdatedAt), Instant.now().toEpochMilli())
        .toModel();
  }

  /**
   * Tạo một instance AmmOrder với các giá trị mặc định
   *
   * @return Instance AmmOrder
   */
  public static AmmOrder create() {
    return Instancio.create(ammOrderModel());
  }

  public static AmmOrder createCustomAmmOrder(Map<String, Object> customFields) {
    AmmOrder order = create();
    return TestModelFactory.customize(order, customFields);
  }
}
