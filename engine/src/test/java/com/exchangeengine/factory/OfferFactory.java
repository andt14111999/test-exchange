package com.exchangeengine.factory;

import com.exchangeengine.model.Offer;
import com.exchangeengine.util.TestModelFactory;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Factory tạo mẫu Offer cho thử nghiệm
 */
public class OfferFactory {

    /**
     * Tạo một model Offer với các giá trị mặc định
     *
     * @return Model<Offer> sử dụng để tạo instance Offer
     */
    public static Model<Offer> offerModel() {
        String randomId = UUID.randomUUID().toString();
        String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);

        return Instancio.of(Offer.class)
                .set(Select.field(Offer::getIdentifier), "offer-" + randomId.substring(0, 8))
                .set(Select.field(Offer::getUserId), userId)
                .set(Select.field(Offer::getSymbol), "BTC:USD")
                .set(Select.field(Offer::getType), Offer.OfferType.BUY)
                .set(Select.field(Offer::getPrice), new BigDecimal("10000"))
                .set(Select.field(Offer::getTotalAmount), new BigDecimal("1"))
                .set(Select.field(Offer::getAvailableAmount), new BigDecimal("1"))
                .set(Select.field(Offer::getStatus), Offer.OfferStatus.PENDING)
                .set(Select.field(Offer::getCreatedAt), Instant.now())
                .set(Select.field(Offer::getUpdatedAt), Instant.now())
                .set(Select.field(Offer::getDisabled), false)
                .set(Select.field(Offer::getDeleted), false)
                .set(Select.field(Offer::getAutomatic), true)
                .set(Select.field(Offer::getOnline), true)
                .set(Select.field(Offer::getMargin), new BigDecimal("1.5"))
                .set(Select.field(Offer::getPaymentMethodId), "pm-" + UUID.randomUUID().toString().substring(0, 8))
                .set(Select.field(Offer::getPaymentTime), 30)
                .set(Select.field(Offer::getCountryCode), "US")
                .set(Select.field(Offer::getMinAmount), new BigDecimal("0.01"))
                .set(Select.field(Offer::getMaxAmount), new BigDecimal("1"))
                .set(Select.field(Offer::getStatusExplanation), "")
                .toModel();
    }

    /**
     * Tạo một instance Offer với các giá trị mặc định
     *
     * @return Instance Offer
     */
    public static Offer create() {
        return Instancio.create(offerModel());
    }

    /**
     * Tạo một instance Offer với loại giao dịch cụ thể
     *
     * @param type Loại giao dịch (BUY/SELL)
     * @return Instance Offer với loại giao dịch được chỉ định
     */
    public static Offer withType(Offer.OfferType type) {
        return Instancio.of(offerModel())
                .set(Select.field(Offer::getType), type)
                .create();
    }

    /**
     * Tạo một instance Offer với trạng thái cụ thể
     *
     * @param status Trạng thái offer
     * @return Instance Offer với trạng thái được chỉ định
     */
    public static Offer withStatus(Offer.OfferStatus status) {
        return Instancio.of(offerModel())
                .set(Select.field(Offer::getStatus), status)
                .create();
    }

    /**
     * Tạo một instance Offer với số lượng có sẵn cụ thể
     *
     * @param availableAmount Số lượng có sẵn
     * @return Instance Offer với số lượng có sẵn được chỉ định
     */
    public static Offer withAvailableAmount(BigDecimal availableAmount) {
        return Instancio.of(offerModel())
                .set(Select.field(Offer::getAvailableAmount), availableAmount)
                .create();
    }

    /**
     * Tạo một Offer với các trường tùy chỉnh
     *
     * @param customFields Map các trường và giá trị tương ứng
     * @return Offer đã được tùy chỉnh
     */
    public static Offer createCustomOffer(Map<String, Object> customFields) {
        Offer offer = create();
        return TestModelFactory.customize(offer, customFields);
    }
}
