package com.exchangeengine.factory;

import com.exchangeengine.model.Trade;
import com.exchangeengine.util.TestModelFactory;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Factory tạo mẫu Trade cho thử nghiệm
 */
public class TradeFactory {

    /**
     * Tạo một model Trade với các giá trị mặc định
     *
     * @return Model<Trade> sử dụng để tạo instance Trade
     */
    public static Model<Trade> tradeModel() {
        String randomId = UUID.randomUUID().toString();

        return Instancio.of(Trade.class)
                .set(Select.field(Trade::getIdentifier), "trade-" + randomId.substring(0, 8))
                .set(Select.field(Trade::getOfferKey), "offer-" + UUID.randomUUID().toString().substring(0, 8))
                .set(Select.field(Trade::getBuyerAccountKey), "buyer-" + UUID.randomUUID().toString().substring(0, 8))
                .set(Select.field(Trade::getSellerAccountKey), "seller-" + UUID.randomUUID().toString().substring(0, 8))
                .set(Select.field(Trade::getSymbol), "BTC:USD")
                .set(Select.field(Trade::getPrice), new BigDecimal("10000"))
                .set(Select.field(Trade::getCoinAmount), new BigDecimal("1"))
                .set(Select.field(Trade::getStatus), Trade.TradeStatus.UNPAID)
                .set(Select.field(Trade::getTakerSide), Trade.TAKER_SIDE_BUY)
                .set(Select.field(Trade::getCreatedAt), Instant.now())
                .set(Select.field(Trade::getUpdatedAt), Instant.now())
                .set(Select.field(Trade::getStatusExplanation), "")
                .toModel();
    }

    /**
     * Tạo một instance Trade với các giá trị mặc định
     *
     * @return Instance Trade
     */
    public static Trade create() {
        return Instancio.create(tradeModel());
    }

    /**
     * Tạo một instance Trade với trạng thái cụ thể
     *
     * @param status Trạng thái trade
     * @return Instance Trade với trạng thái được chỉ định
     */
    public static Trade withStatus(Trade.TradeStatus status) {
        return Instancio.of(tradeModel())
                .set(Select.field(Trade::getStatus), status)
                .create();
    }

    /**
     * Tạo một instance Trade với taker side cụ thể
     *
     * @param takerSide Taker side (BUY/SELL)
     * @return Instance Trade với taker side được chỉ định
     */
    public static Trade withTakerSide(String takerSide) {
        return Instancio.of(tradeModel())
                .set(Select.field(Trade::getTakerSide), takerSide)
                .create();
    }

    /**
     * Tạo một instance Trade với số lượng coin cụ thể
     *
     * @param coinAmount Số lượng coin
     * @return Instance Trade với số lượng coin được chỉ định
     */
    public static Trade withCoinAmount(BigDecimal coinAmount) {
        return Instancio.of(tradeModel())
                .set(Select.field(Trade::getCoinAmount), coinAmount)
                .create();
    }

    /**
     * Tạo một Trade với các trường tùy chỉnh
     *
     * @param customFields Map các trường và giá trị tương ứng
     * @return Trade đã được tùy chỉnh
     */
    public static Trade createCustomTrade(Map<String, Object> customFields) {
        Trade trade = create();
        return TestModelFactory.customize(trade, customFields);
    }
}
