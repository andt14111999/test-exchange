package com.exchangeengine.model;

import com.exchangeengine.factory.OfferFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OfferTest {

    @Test
    @DisplayName("Kiểm tra khởi tạo offer hợp lệ sử dụng builder")
    void testOfferConstructionWithBuilder() {
        // Given
        String identifier = "offer-123";
        String userId = "user-123";
        String symbol = "BTC:USD";
        Offer.OfferType type = Offer.OfferType.BUY;
        BigDecimal price = new BigDecimal("10000");
        BigDecimal totalAmount = new BigDecimal("1");
        BigDecimal availableAmount = new BigDecimal("1");
        Instant now = Instant.now();

        // When
        Offer offer = Offer.builder()
                .identifier(identifier)
                .userId(userId)
                .symbol(symbol)
                .type(type)
                .price(price)
                .totalAmount(totalAmount)
                .availableAmount(availableAmount)
                .status(Offer.OfferStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertEquals(identifier, offer.getIdentifier());
        assertEquals(userId, offer.getUserId());
        assertEquals(symbol, offer.getSymbol());
        assertEquals(type, offer.getType());
        assertEquals(price, offer.getPrice());
        assertEquals(totalAmount, offer.getTotalAmount());
        assertEquals(availableAmount, offer.getAvailableAmount());
        assertEquals(Offer.OfferStatus.PENDING, offer.getStatus());
        assertEquals(now, offer.getCreatedAt());
        assertEquals(now, offer.getUpdatedAt());
    }

    @Test
    @DisplayName("Kiểm tra khởi tạo offer hợp lệ sử dụng constructor")
    void testOfferConstructionWithConstructor() {
        // Given
        String identifier = "offer-123";
        String userId = "user-123";
        String offerType = "BUY";
        String coinCurrency = "BTC";
        String fiatCurrency = "USD";
        BigDecimal price = new BigDecimal("10000");
        BigDecimal minAmount = new BigDecimal("0.01");
        BigDecimal maxAmount = new BigDecimal("1");
        BigDecimal totalAmount = new BigDecimal("1");
        BigDecimal availableAmount = new BigDecimal("1");
        String paymentMethodId = "pm-123";
        Integer paymentTime = 30;
        String countryCode = "US";
        Boolean disabled = false;
        Boolean deleted = false;
        Boolean automatic = true;
        Boolean online = true;
        BigDecimal margin = new BigDecimal("1.5");
        Instant now = Instant.now();

        // When
        Offer offer = new Offer(
                identifier, userId, offerType, coinCurrency, fiatCurrency,
                price, minAmount, maxAmount, totalAmount, availableAmount,
                paymentMethodId, paymentTime, countryCode, disabled, deleted,
                automatic, online, margin, now, now
        );

        // Then
        assertEquals(identifier, offer.getIdentifier());
        assertEquals(userId, offer.getUserId());
        assertEquals(coinCurrency + ":" + fiatCurrency, offer.getSymbol());
        assertEquals(Offer.OfferType.BUY, offer.getType());
        assertEquals(price, offer.getPrice());
        assertEquals(minAmount, offer.getMinAmount());
        assertEquals(maxAmount, offer.getMaxAmount());
        assertEquals(totalAmount, offer.getTotalAmount());
        assertEquals(availableAmount, offer.getAvailableAmount());
        assertEquals(paymentMethodId, offer.getPaymentMethodId());
        assertEquals(paymentTime, offer.getPaymentTime());
        assertEquals(countryCode, offer.getCountryCode());
        assertEquals(disabled, offer.getDisabled());
        assertEquals(deleted, offer.getDeleted());
        assertEquals(automatic, offer.getAutomatic());
        assertEquals(online, offer.getOnline());
        assertEquals(margin, offer.getMargin());
        assertEquals(now, offer.getCreatedAt());
        assertEquals(now, offer.getUpdatedAt());
        assertEquals(Offer.OfferStatus.PENDING, offer.getStatus());
    }

    @Test
    @DisplayName("Kiểm tra constructor sử dụng Instant.now() khi truyền timestamp null")
    void testConstructorWithNullTimestamps() {
        // Given
        String identifier = "offer-123";
        String userId = "user-123";
        String offerType = "BUY";
        String coinCurrency = "BTC";
        String fiatCurrency = "USD";
        BigDecimal price = new BigDecimal("10000");
        BigDecimal minAmount = new BigDecimal("0.01");
        BigDecimal maxAmount = new BigDecimal("1");
        BigDecimal totalAmount = new BigDecimal("1");
        BigDecimal availableAmount = new BigDecimal("1");
        String paymentMethodId = "pm-123";
        Integer paymentTime = 30;
        String countryCode = "US";
        Boolean disabled = false;
        Boolean deleted = false;
        Boolean automatic = true;
        Boolean online = true;
        BigDecimal margin = new BigDecimal("1.5");

        // When
        Offer offer = new Offer(
                identifier, userId, offerType, coinCurrency, fiatCurrency,
                price, minAmount, maxAmount, totalAmount, availableAmount,
                paymentMethodId, paymentTime, countryCode, disabled, deleted,
                automatic, online, margin, null, null
        );

        // Then
        assertNotNull(offer.getCreatedAt());
        assertNotNull(offer.getUpdatedAt());
    }

    @Test
    @DisplayName("Kiểm tra phương thức isActive trả về true cho các trạng thái active")
    void testIsActiveReturnsTrue() {
        // Given
        Offer pendingOffer = OfferFactory.withStatus(Offer.OfferStatus.PENDING);
        Offer partiallyFilledOffer = OfferFactory.withStatus(Offer.OfferStatus.PARTIALLY_FILLED);

        // When & Then
        assertTrue(pendingOffer.isActive());
        assertTrue(partiallyFilledOffer.isActive());
    }

    @Test
    @DisplayName("Kiểm tra phương thức isActive trả về false cho các trạng thái inactive")
    void testIsActiveReturnsFalse() {
        // Given
        Offer filledOffer = OfferFactory.withStatus(Offer.OfferStatus.FILLED);
        Offer cancelledOffer = OfferFactory.withStatus(Offer.OfferStatus.CANCELLED);

        // When & Then
        assertFalse(filledOffer.isActive());
        assertFalse(cancelledOffer.isActive());
    }

    @Test
    @DisplayName("Kiểm tra phương thức isFullyFilled trả về đúng")
    void testIsFullyFilled() {
        // Given
        Offer filledOffer = OfferFactory.withStatus(Offer.OfferStatus.FILLED);
        Offer pendingOffer = OfferFactory.withStatus(Offer.OfferStatus.PENDING);

        // When & Then
        assertTrue(filledOffer.isFullyFilled());
        assertFalse(pendingOffer.isFullyFilled());
    }

    @Test
    @DisplayName("Kiểm tra phương thức canBeFilled trả về đúng")
    void testCanBeFilled() {
        // Given
        Offer offer = OfferFactory.withAvailableAmount(new BigDecimal("1.0"));

        // When & Then
        assertTrue(offer.canBeFilled(new BigDecimal("0.5")));
        assertTrue(offer.canBeFilled(new BigDecimal("1.0")));
        assertFalse(offer.canBeFilled(new BigDecimal("1.1")));
    }

    @Test
    @DisplayName("Kiểm tra phương thức fill cập nhật trạng thái và số lượng")
    void testFill() {
        // Given
        Offer offer = OfferFactory.create();
        Instant beforeUpdate = offer.getUpdatedAt();

        // When
        offer.fill();

        // Then
        assertEquals(Offer.OfferStatus.FILLED, offer.getStatus());
        assertEquals(BigDecimal.ZERO, offer.getAvailableAmount());
        assertTrue(offer.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    @DisplayName("Kiểm tra phương thức partiallyFill cập nhật trạng thái và số lượng")
    void testPartiallyFill() {
        // Given
        Offer offer = OfferFactory.withAvailableAmount(new BigDecimal("2.0"));
        Instant beforeUpdate = offer.getUpdatedAt();
        BigDecimal filledQuantity = new BigDecimal("1.5");

        // When
        offer.partiallyFill(filledQuantity);

        // Then
        assertEquals(Offer.OfferStatus.PARTIALLY_FILLED, offer.getStatus());
        assertEquals(new BigDecimal("0.5"), offer.getAvailableAmount());
        assertTrue(offer.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    @DisplayName("Kiểm tra phương thức cancel cập nhật trạng thái")
    void testCancel() {
        // Given
        Offer offer = OfferFactory.create();
        Instant beforeUpdate = offer.getUpdatedAt();

        // When
        offer.cancel();

        // Then
        assertEquals(Offer.OfferStatus.CANCELLED, offer.getStatus());
        assertTrue(offer.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    @DisplayName("Kiểm tra phương thức updateStatus cập nhật trạng thái và mô tả")
    void testUpdateStatus() {
        // Given
        Offer offer = OfferFactory.create();
        Instant beforeUpdate = offer.getUpdatedAt();
        Offer.OfferStatus newStatus = Offer.OfferStatus.CANCELLED;
        String description = "Cancelled by user";

        // When
        offer.updateStatus(newStatus, description);

        // Then
        assertEquals(newStatus, offer.getStatus());
        assertEquals(description, offer.getStatusExplanation());
        assertTrue(offer.getUpdatedAt().isAfter(beforeUpdate));
    }

    @Test
    @DisplayName("Kiểm tra phương thức updateStatus không cập nhật timestamp khi trạng thái không thay đổi")
    void testUpdateStatusWithSameStatus() {
        // Given
        Offer offer = OfferFactory.create();
        Offer.OfferStatus sameStatus = offer.getStatus();
        Instant beforeUpdate = offer.getUpdatedAt();
        String description = "Same status";

        // When
        offer.updateStatus(sameStatus, description);

        // Then
        assertEquals(beforeUpdate, offer.getUpdatedAt());
    }

    @Test
    @DisplayName("Kiểm tra phương thức getCreatedAtEpochMilli")
    void testGetCreatedAtEpochMilli() {
        // Given
        Instant now = Instant.now();
        Offer offer = OfferFactory.create();
        offer.setCreatedAt(now);

        // When
        long epochMilli = offer.getCreatedAtEpochMilli();

        // Then
        assertEquals(now.toEpochMilli(), epochMilli);
    }

    @Test
    @DisplayName("Kiểm tra phương thức getCreatedAtEpochMilli với giá trị null")
    void testGetCreatedAtEpochMilliWithNull() {
        // Given
        Offer offer = OfferFactory.create();
        offer.setCreatedAt(null);

        // When
        long epochMilli = offer.getCreatedAtEpochMilli();

        // Then
        assertEquals(0, epochMilli);
    }

    @Test
    @DisplayName("Kiểm tra phương thức getUpdatedAtEpochMilli")
    void testGetUpdatedAtEpochMilli() {
        // Given
        Instant now = Instant.now();
        Offer offer = OfferFactory.create();
        offer.setUpdatedAt(now);

        // When
        long epochMilli = offer.getUpdatedAtEpochMilli();

        // Then
        assertEquals(now.toEpochMilli(), epochMilli);
    }

    @Test
    @DisplayName("Kiểm tra phương thức getUpdatedAtEpochMilli với giá trị null")
    void testGetUpdatedAtEpochMilliWithNull() {
        // Given
        Offer offer = OfferFactory.create();
        offer.setUpdatedAt(null);

        // When
        long epochMilli = offer.getUpdatedAtEpochMilli();

        // Then
        assertEquals(0, epochMilli);
    }

    @Test
    @DisplayName("Kiểm tra phương thức toMessageJson")
    void testToMessageJson() {
        // Given
        Offer offer = OfferFactory.create();

        // When
        Map<String, Object> result = offer.toMessageJson();

        // Then
        assertNotNull(result);
        assertEquals(offer.getIdentifier(), result.get("identifier"));
        assertEquals(offer.getUserId(), result.get("userId"));
        assertEquals(offer.getSymbol(), result.get("symbol"));
    }

    @Test
    @DisplayName("Kiểm tra phương thức validateRequiredFields với offer hợp lệ")
    void testValidateRequiredFieldsValid() {
        // Given
        Offer offer = OfferFactory.create();

        // When
        List<String> errors = offer.validateRequiredFields();

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("Kiểm tra phương thức validateRequiredFields khi availableAmount > totalAmount")
    void testValidateRequiredFieldsAvailableGreaterThanTotal() {
        // Given
        Offer offer = OfferFactory.create();
        offer.setTotalAmount(new BigDecimal("1.0"));
        offer.setAvailableAmount(new BigDecimal("1.5"));

        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> offer.validateRequiredFields());
        assertTrue(exception.getMessage().contains("Available amount cannot be greater than total amount"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidOffers")
    @DisplayName("Kiểm tra phương thức validateRequiredFields với các trường bắt buộc thiếu")
    void testValidateRequiredFieldsMissingRequiredFields(Offer offer, String expectedErrorMessage) {
        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> offer.validateRequiredFields());
        assertTrue(exception.getMessage().contains(expectedErrorMessage));
    }

    private static Stream<Arguments> provideInvalidOffers() {
        return Stream.of(
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("identifier", null)), "Offer ID is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("identifier", "")), "Offer ID is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("userId", null)), "User ID is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("userId", "  ")), "User ID is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("symbol", null)), "Symbol is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("symbol", "")), "Symbol is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("type", null)), "Offer type is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("price", null)), "Price is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("price", new BigDecimal("-1"))), "Price must be greater than 0"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("totalAmount", null)), "Total amount is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("totalAmount", new BigDecimal("0"))), "Total amount must be greater than 0"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("status", null)), "Status is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("createdAt", null)), "Created at timestamp is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("updatedAt", null)), "Updated at timestamp is required"),
                Arguments.of(OfferFactory.createCustomOffer(Collections.singletonMap("availableAmount", null)), "Available amount is required")
        );
    }

    @Test
    @DisplayName("Kiểm tra phương thức equals và hashCode")
    void testEqualsAndHashCode() {
        // Given
        Offer offer1 = OfferFactory.create();
        Offer offer2 = OfferFactory.create();
        offer2.setIdentifier(offer1.getIdentifier());
        Offer offer3 = OfferFactory.create();

        // When & Then
        assertEquals(offer1, offer2);
        assertNotEquals(offer1, offer3);
        assertEquals(offer1.hashCode(), offer2.hashCode());
        assertNotEquals(offer1.hashCode(), offer3.hashCode());
    }

    @Test
    @DisplayName("Kiểm tra phương thức equals với các trường hợp đặc biệt")
    void testEqualsSpecialCases() {
        // Given
        Offer offer = OfferFactory.create();

        // When & Then
        assertEquals(offer, offer); // Same object
        assertNotEquals(offer, null); // Null
        assertNotEquals(offer, new Object()); // Different class
    }

    @Test
    @DisplayName("Kiểm tra các hằng số trạng thái")
    void testStatusConstants() {
        // When & Then
        assertEquals("PENDING", Offer.STATUS_PENDING);
        assertEquals("PARTIALLY_FILLED", Offer.STATUS_PARTIALLY_FILLED);
        assertEquals("FILLED", Offer.STATUS_FILLED);
        assertEquals("CANCELLED", Offer.STATUS_CANCELLED);
    }
    
    @Test
    @DisplayName("Kiểm tra xử lý ngoại lệ khi offerType không hợp lệ")
    void testInvalidOfferType() {
        // Given
        String identifier = "offer-123";
        String userId = "user-123";
        String invalidOfferType = "INVALID";
        String coinCurrency = "BTC";
        String fiatCurrency = "USD";
        BigDecimal price = new BigDecimal("10000");
        BigDecimal minAmount = new BigDecimal("0.01");
        BigDecimal maxAmount = new BigDecimal("1");
        BigDecimal totalAmount = new BigDecimal("1");
        BigDecimal availableAmount = new BigDecimal("1");
        String paymentMethodId = "pm-123";
        Integer paymentTime = 30;
        String countryCode = "US";
        Boolean disabled = false;
        Boolean deleted = false;
        Boolean automatic = true;
        Boolean online = true;
        BigDecimal margin = new BigDecimal("1.5");
        Instant now = Instant.now();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> new Offer(
                identifier, userId, invalidOfferType, coinCurrency, fiatCurrency,
                price, minAmount, maxAmount, totalAmount, availableAmount,
                paymentMethodId, paymentTime, countryCode, disabled, deleted,
                automatic, online, margin, now, now
        ));
    }
    
    @Test
    @DisplayName("Kiểm tra phương thức partiallyFill với số lượng bằng availableAmount")
    void testPartiallyFillWithEqualAmount() {
        // Given
        BigDecimal amount = new BigDecimal("1.0");
        Offer offer = OfferFactory.withAvailableAmount(amount);
        Instant beforeUpdate = offer.getUpdatedAt();

        // When
        offer.partiallyFill(amount);

        // Then
        assertEquals(Offer.OfferStatus.PARTIALLY_FILLED, offer.getStatus());
        assertEquals(new BigDecimal("0.0"), offer.getAvailableAmount());
        assertTrue(offer.getUpdatedAt().isAfter(beforeUpdate));
    }
    
    @Test
    @DisplayName("Kiểm tra phương thức partiallyFill với số lượng không hợp lệ")
    void testPartiallyFillWithInvalidAmount() {
        // Given
        Offer offer = OfferFactory.withAvailableAmount(new BigDecimal("1.0"));
        BigDecimal invalidAmount = new BigDecimal("1.1");

        // When
        offer.partiallyFill(invalidAmount);

        // Then
        // Phương thức partiallyFill của Offer không kiểm tra tính hợp lệ của số lượng
        // nên kết quả sẽ là số lượng âm
        assertEquals(new BigDecimal("-0.1"), offer.getAvailableAmount());
        assertEquals(Offer.OfferStatus.PARTIALLY_FILLED, offer.getStatus());
    }
    
    @Test
    @DisplayName("Kiểm tra phương thức partiallyFill với số lượng âm")
    void testPartiallyFillWithNegativeAmount() {
        // Given
        Offer offer = OfferFactory.withAvailableAmount(new BigDecimal("1.0"));
        BigDecimal negativeAmount = new BigDecimal("-0.5");

        // When
        offer.partiallyFill(negativeAmount);

        // Then
        // Phương thức partiallyFill của Offer không kiểm tra số lượng âm
        // nên kết quả sẽ là số lượng tăng thêm
        assertEquals(new BigDecimal("1.5"), offer.getAvailableAmount());
        assertEquals(Offer.OfferStatus.PARTIALLY_FILLED, offer.getStatus());
    }
    
    @Test
    @DisplayName("Kiểm tra phương thức all setters và getters")
    void testSettersAndGetters() {
        // Given
        Offer offer = new Offer();
        String identifier = "offer-123";
        String userId = "user-123";
        String symbol = "BTC:USD";
        Offer.OfferType type = Offer.OfferType.SELL;
        BigDecimal price = new BigDecimal("10000");
        BigDecimal totalAmount = new BigDecimal("1");
        BigDecimal availableAmount = new BigDecimal("1");
        Offer.OfferStatus status = Offer.OfferStatus.PENDING;
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();
        Boolean disabled = true;
        Boolean deleted = true;
        Boolean automatic = false;
        Boolean online = false;
        BigDecimal margin = new BigDecimal("2.5");
        String paymentMethodId = "pm-123";
        Integer paymentTime = 30;
        String countryCode = "VN";
        BigDecimal minAmount = new BigDecimal("0.01");
        BigDecimal maxAmount = new BigDecimal("5");
        String statusExplanation = "Test status";

        // When
        offer.setIdentifier(identifier);
        offer.setUserId(userId);
        offer.setSymbol(symbol);
        offer.setType(type);
        offer.setPrice(price);
        offer.setTotalAmount(totalAmount);
        offer.setAvailableAmount(availableAmount);
        offer.setStatus(status);
        offer.setCreatedAt(createdAt);
        offer.setUpdatedAt(updatedAt);
        offer.setDisabled(disabled);
        offer.setDeleted(deleted);
        offer.setAutomatic(automatic);
        offer.setOnline(online);
        offer.setMargin(margin);
        offer.setPaymentMethodId(paymentMethodId);
        offer.setPaymentTime(paymentTime);
        offer.setCountryCode(countryCode);
        offer.setMinAmount(minAmount);
        offer.setMaxAmount(maxAmount);
        offer.setStatusExplanation(statusExplanation);

        // Then
        assertEquals(identifier, offer.getIdentifier());
        assertEquals(userId, offer.getUserId());
        assertEquals(symbol, offer.getSymbol());
        assertEquals(type, offer.getType());
        assertEquals(price, offer.getPrice());
        assertEquals(totalAmount, offer.getTotalAmount());
        assertEquals(availableAmount, offer.getAvailableAmount());
        assertEquals(status, offer.getStatus());
        assertEquals(createdAt, offer.getCreatedAt());
        assertEquals(updatedAt, offer.getUpdatedAt());
        assertEquals(disabled, offer.getDisabled());
        assertEquals(deleted, offer.getDeleted());
        assertEquals(automatic, offer.getAutomatic());
        assertEquals(online, offer.getOnline());
        assertEquals(margin, offer.getMargin());
        assertEquals(paymentMethodId, offer.getPaymentMethodId());
        assertEquals(paymentTime, offer.getPaymentTime());
        assertEquals(countryCode, offer.getCountryCode());
        assertEquals(minAmount, offer.getMinAmount());
        assertEquals(maxAmount, offer.getMaxAmount());
        assertEquals(statusExplanation, offer.getStatusExplanation());
    }
    
    @Test
    @DisplayName("Kiểm tra toString method")
    void testToString() {
        // Given
        Offer offer = OfferFactory.create();
        
        // When
        String result = offer.toString();
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains(offer.getIdentifier()));
        assertTrue(result.contains(offer.getUserId()));
        assertTrue(result.contains(offer.getSymbol()));
    }
} 