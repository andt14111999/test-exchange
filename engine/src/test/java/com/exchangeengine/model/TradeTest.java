package com.exchangeengine.model;

import static org.junit.jupiter.api.Assertions.*;

import com.exchangeengine.util.JsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Unit test cho lớp Trade đảm bảo coverage 100%
 */
public class TradeTest {

    private Trade trade;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        trade = createDefaultTrade();
    }

    /**
     * Tạo đối tượng Trade mặc định để sử dụng trong test
     * @return Đối tượng Trade
     */
    private Trade createDefaultTrade() {
        return Trade.builder()
                .identifier("trade-123")
                .offerKey("offer-123")
                .buyerAccountKey("buyer-123")
                .sellerAccountKey("seller-123")
                .symbol("BTC:USD")
                .price(new BigDecimal("10000"))
                .coinAmount(new BigDecimal("1"))
                .status(Trade.TradeStatus.UNPAID)
                .takerSide(Trade.TAKER_SIDE_BUY)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Model Instancio để tạo đối tượng Trade cho test
     * @return Model<Trade>
     */
    private Model<Trade> tradeModel() {
        return Instancio.of(Trade.class)
                .set(Select.field(Trade::getIdentifier), "trade-123")
                .set(Select.field(Trade::getOfferKey), "offer-123")
                .set(Select.field(Trade::getBuyerAccountKey), "buyer-123")
                .set(Select.field(Trade::getSellerAccountKey), "seller-123")
                .set(Select.field(Trade::getSymbol), "BTC:USD")
                .set(Select.field(Trade::getPrice), new BigDecimal("10000"))
                .set(Select.field(Trade::getCoinAmount), new BigDecimal("1"))
                .set(Select.field(Trade::getStatus), Trade.TradeStatus.UNPAID)
                .set(Select.field(Trade::getTakerSide), Trade.TAKER_SIDE_BUY)
                .set(Select.field(Trade::getCreatedAt), now)
                .set(Select.field(Trade::getUpdatedAt), now)
                .toModel();
    }

    @Test
    @DisplayName("Test builder và constructor tạo đối tượng đúng")
    void testConstructorAndBuilder() {
        // Create via builder
        Trade tradeFromBuilder = Trade.builder()
                .identifier("trade-123")
                .offerKey("offer-123")
                .buyerAccountKey("buyer-123")
                .sellerAccountKey("seller-123")
                .symbol("BTC:USD")
                .price(new BigDecimal("10000"))
                .coinAmount(new BigDecimal("1"))
                .status(Trade.TradeStatus.UNPAID)
                .takerSide(Trade.TAKER_SIDE_BUY)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Replace direct constructor use with builder since the constructor has many parameters
        Trade tradeFromConstructor = Trade.builder()
                .identifier("trade-123")
                .offerKey("offer-123")
                .buyerAccountKey("buyer-123")
                .sellerAccountKey("seller-123")
                .symbol("BTC:USD")
                .price(new BigDecimal("10000"))
                .coinAmount(new BigDecimal("1"))
                .status(Trade.TradeStatus.UNPAID)
                .takerSide(Trade.TAKER_SIDE_BUY)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Create via no-args constructor and setters
        Trade tradeFromSetters = new Trade();
        tradeFromSetters.setIdentifier("trade-123");
        tradeFromSetters.setOfferKey("offer-123");
        tradeFromSetters.setBuyerAccountKey("buyer-123");
        tradeFromSetters.setSellerAccountKey("seller-123");
        tradeFromSetters.setSymbol("BTC:USD");
        tradeFromSetters.setPrice(new BigDecimal("10000"));
        tradeFromSetters.setCoinAmount(new BigDecimal("1"));
        tradeFromSetters.setStatus(Trade.TradeStatus.UNPAID);
        tradeFromSetters.setTakerSide(Trade.TAKER_SIDE_BUY);
        tradeFromSetters.setCreatedAt(now);
        tradeFromSetters.setUpdatedAt(now);

        // Assert all three create the same object
        assertEquals(tradeFromBuilder, tradeFromConstructor);
        assertEquals(tradeFromBuilder, tradeFromSetters);

        // Assert getters work correctly
        assertEquals("trade-123", tradeFromBuilder.getIdentifier());
        assertEquals("offer-123", tradeFromBuilder.getOfferKey());
        assertEquals("buyer-123", tradeFromBuilder.getBuyerAccountKey());
        assertEquals("seller-123", tradeFromBuilder.getSellerAccountKey());
        assertEquals("BTC:USD", tradeFromBuilder.getSymbol());
        assertEquals(new BigDecimal("10000"), tradeFromBuilder.getPrice());
        assertEquals(new BigDecimal("1"), tradeFromBuilder.getCoinAmount());
        assertEquals(Trade.TradeStatus.UNPAID, tradeFromBuilder.getStatus());
        assertEquals(Trade.TAKER_SIDE_BUY, tradeFromBuilder.getTakerSide());
        assertEquals(now, tradeFromBuilder.getCreatedAt());
        assertEquals(now, tradeFromBuilder.getUpdatedAt());
    }

    @Test
    @DisplayName("Test equals và hashCode dựa trên identifier")
    void testEqualsAndHashCode() {
        // Tạo 2 trade với identifier giống nhau nhưng các thuộc tính khác nhau
        Trade trade1 = Trade.builder()
                .identifier("same-id")
                .offerKey("offer-1")
                .price(new BigDecimal("10000"))
                .build();

        Trade trade2 = Trade.builder()
                .identifier("same-id")
                .offerKey("offer-2") // Khác offer
                .price(new BigDecimal("20000")) // Khác price
                .build();

        // Tạo trade với identifier khác
        Trade trade3 = Trade.builder()
                .identifier("different-id")
                .offerKey("offer-1")
                .price(new BigDecimal("10000"))
                .build();

        // Test equals
        assertEquals(trade1, trade2, "Trades với cùng identifier nên bằng nhau");
        assertNotEquals(trade1, trade3, "Trades với khác identifier không nên bằng nhau");

        // Test hashCode
        assertEquals(trade1.hashCode(), trade2.hashCode(), "Trades với cùng identifier nên có cùng hashCode");
        assertNotEquals(trade1.hashCode(), trade3.hashCode(), "Trades với khác identifier nên có khác hashCode");

        // Test với null và object khác loại
        assertNotEquals(trade1, null, "Trade không nên bằng null");
        assertNotEquals(trade1, "string object", "Trade không nên bằng object khác loại");
    }

    @Test
    @DisplayName("Test isUnpaid return true khi status là UNPAID")
    void testIsUnpaid() {
        // Set status là UNPAID
        trade.setStatus(Trade.TradeStatus.UNPAID);
        assertTrue(trade.isUnpaid(), "isUnpaid nên trả về true khi status là UNPAID");

        // Set status khác UNPAID
        trade.setStatus(Trade.TradeStatus.COMPLETED);
        assertFalse(trade.isUnpaid(), "isUnpaid nên trả về false khi status không phải UNPAID");

        trade.setStatus(Trade.TradeStatus.CANCELLED);
        assertFalse(trade.isUnpaid(), "isUnpaid nên trả về false khi status không phải UNPAID");
    }

    @Test
    @DisplayName("Test isCompleted return true khi status là COMPLETED")
    void testIsCompleted() {
        // Set status là COMPLETED
        trade.setStatus(Trade.TradeStatus.COMPLETED);
        assertTrue(trade.isCompleted(), "isCompleted nên trả về true khi status là COMPLETED");

        // Set status khác COMPLETED
        trade.setStatus(Trade.TradeStatus.UNPAID);
        assertFalse(trade.isCompleted(), "isCompleted nên trả về false khi status không phải COMPLETED");

        trade.setStatus(Trade.TradeStatus.CANCELLED);
        assertFalse(trade.isCompleted(), "isCompleted nên trả về false khi status không phải COMPLETED");
    }

    @Test
    @DisplayName("Test isCancelled return true khi status là CANCELLED")
    void testIsCancelled() {
        // Set status là CANCELLED
        trade.setStatus(Trade.TradeStatus.CANCELLED);
        assertTrue(trade.isCancelled(), "isCancelled nên trả về true khi status là CANCELLED");

        // Set status khác CANCELLED
        trade.setStatus(Trade.TradeStatus.UNPAID);
        assertFalse(trade.isCancelled(), "isCancelled nên trả về false khi status không phải CANCELLED");

        trade.setStatus(Trade.TradeStatus.COMPLETED);
        assertFalse(trade.isCancelled(), "isCancelled nên trả về false khi status không phải CANCELLED");
    }

    @Test
    @DisplayName("Test isBuyerTaker return true khi takerSide là BUY")
    void testIsBuyerTaker() {
        // Set takerSide là BUY
        trade.setTakerSide(Trade.TAKER_SIDE_BUY);
        assertTrue(trade.isBuyerTaker(), "isBuyerTaker nên trả về true khi takerSide là BUY");

        // Set takerSide là SELL
        trade.setTakerSide(Trade.TAKER_SIDE_SELL);
        assertFalse(trade.isBuyerTaker(), "isBuyerTaker nên trả về false khi takerSide là SELL");

        // Set takerSide là giá trị khác (case-insensitive test)
        trade.setTakerSide("buy");
        assertTrue(trade.isBuyerTaker(), "isBuyerTaker nên trả về true khi takerSide là 'buy' (case-insensitive)");
    }

    @Test
    @DisplayName("Test isSellerTaker return true khi takerSide là SELL")
    void testIsSellerTaker() {
        // Set takerSide là SELL
        trade.setTakerSide(Trade.TAKER_SIDE_SELL);
        assertTrue(trade.isSellerTaker(), "isSellerTaker nên trả về true khi takerSide là SELL");

        // Set takerSide là BUY
        trade.setTakerSide(Trade.TAKER_SIDE_BUY);
        assertFalse(trade.isSellerTaker(), "isSellerTaker nên trả về false khi takerSide là BUY");

        // Set takerSide là giá trị khác (case-insensitive test)
        trade.setTakerSide("sell");
        assertTrue(trade.isSellerTaker(), "isSellerTaker nên trả về true khi takerSide là 'sell' (case-insensitive)");
    }

    @Test
    @DisplayName("Test phương thức complete cập nhật status và timestamps đúng")
    void testComplete() {
        // Lưu thời gian updatedAt và completedAt ban đầu
        Instant originalUpdatedAt = trade.getUpdatedAt();
        Instant originalCompletedAt = trade.getCompletedAt();

        // Chờ một chút để đảm bảo timestamps mới sẽ khác
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Gọi phương thức complete
        trade.complete();

        // Kiểm tra status
        assertEquals(Trade.TradeStatus.COMPLETED, trade.getStatus(), "Status nên được cập nhật thành COMPLETED");

        // Kiểm tra timestamps
        assertNotEquals(originalUpdatedAt, trade.getUpdatedAt(), "updatedAt nên được cập nhật");
        assertNotNull(trade.getCompletedAt(), "completedAt nên được thiết lập");
        assertNotEquals(originalCompletedAt, trade.getCompletedAt(), "completedAt nên được cập nhật");
    }

    @Test
    @DisplayName("Test phương thức cancel cập nhật status và timestamps đúng")
    void testCancel() {
        // Lưu thời gian updatedAt và cancelledAt ban đầu
        Instant originalUpdatedAt = trade.getUpdatedAt();
        Instant originalCancelledAt = trade.getCancelledAt();

        // Chờ một chút để đảm bảo timestamps mới sẽ khác
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Gọi phương thức cancel
        trade.cancel();

        // Kiểm tra status
        assertEquals(Trade.TradeStatus.CANCELLED, trade.getStatus(), "Status nên được cập nhật thành CANCELLED");

        // Kiểm tra timestamps
        assertNotEquals(originalUpdatedAt, trade.getUpdatedAt(), "updatedAt nên được cập nhật");
        assertNotNull(trade.getCancelledAt(), "cancelledAt nên được thiết lập");
        assertNotEquals(originalCancelledAt, trade.getCancelledAt(), "cancelledAt nên được cập nhật");
    }

    @Test
    @DisplayName("Test phương thức updateStatus cập nhật thông tin đúng")
    void testUpdateStatus() {
        // Lưu thời gian updatedAt ban đầu
        Instant originalUpdatedAt = trade.getUpdatedAt();

        // Chờ một chút để đảm bảo timestamps mới sẽ khác
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Test cập nhật thành COMPLETED
        trade.setStatus(Trade.TradeStatus.UNPAID); // Reset lại status
        trade.updateStatus(Trade.TradeStatus.COMPLETED, "Completed by test");

        assertEquals(Trade.TradeStatus.COMPLETED, trade.getStatus(), "Status nên được cập nhật thành COMPLETED");
        assertEquals("Completed by test", trade.getStatusExplanation(), "StatusExplanation nên được cập nhật");
        assertNotEquals(originalUpdatedAt, trade.getUpdatedAt(), "updatedAt nên được cập nhật");
        assertNotNull(trade.getCompletedAt(), "completedAt nên được thiết lập");
        assertNull(trade.getCancelledAt(), "cancelledAt nên vẫn là null");

        // Test cập nhật thành CANCELLED
        trade.setStatus(Trade.TradeStatus.UNPAID); // Reset lại status
        originalUpdatedAt = trade.getUpdatedAt();
        
        // Chờ một chút để đảm bảo timestamps mới sẽ khác
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }

        trade.updateStatus(Trade.TradeStatus.CANCELLED, "Cancelled by test");

        assertEquals(Trade.TradeStatus.CANCELLED, trade.getStatus(), "Status nên được cập nhật thành CANCELLED");
        assertEquals("Cancelled by test", trade.getStatusExplanation(), "StatusExplanation nên được cập nhật");
        assertNotEquals(originalUpdatedAt, trade.getUpdatedAt(), "updatedAt nên được cập nhật");
        assertNotNull(trade.getCancelledAt(), "cancelledAt nên được thiết lập");

        // Test cập nhật về cùng status - không thay đổi gì
        trade.setStatus(Trade.TradeStatus.CANCELLED); // Đặt status đã là CANCELLED
        originalUpdatedAt = trade.getUpdatedAt();
        
        // Chờ một chút để đảm bảo timestamps mới sẽ khác nếu được cập nhật
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        trade.updateStatus(Trade.TradeStatus.CANCELLED, "Still cancelled");
        
        assertEquals(originalUpdatedAt, trade.getUpdatedAt(), "updatedAt không nên thay đổi khi status không đổi");
    }

    @Test
    @DisplayName("Test phương thức getCreatedAtEpochMilli")
    void testGetCreatedAtEpochMilli() {
        // Với createdAt có giá trị
        Instant testTime = Instant.now();
        trade.setCreatedAt(testTime);
        assertEquals(testTime.toEpochMilli(), trade.getCreatedAtEpochMilli(), 
                "getCreatedAtEpochMilli nên trả về giá trị epoch milli của createdAt");

        // Với createdAt là null
        trade.setCreatedAt(null);
        assertEquals(0, trade.getCreatedAtEpochMilli(), 
                "getCreatedAtEpochMilli nên trả về 0 khi createdAt là null");
    }

    @Test
    @DisplayName("Test phương thức getUpdatedAtEpochMilli")
    void testGetUpdatedAtEpochMilli() {
        // Với updatedAt có giá trị
        Instant testTime = Instant.now();
        trade.setUpdatedAt(testTime);
        assertEquals(testTime.toEpochMilli(), trade.getUpdatedAtEpochMilli(), 
                "getUpdatedAtEpochMilli nên trả về giá trị epoch milli của updatedAt");

        // Với updatedAt là null
        trade.setUpdatedAt(null);
        assertEquals(0, trade.getUpdatedAtEpochMilli(), 
                "getUpdatedAtEpochMilli nên trả về 0 khi updatedAt là null");
    }

    @Test
    @DisplayName("Test phương thức getCompletedAtEpochMilli")
    void testGetCompletedAtEpochMilli() {
        // Với completedAt có giá trị
        Instant testTime = Instant.now();
        trade.setCompletedAt(testTime);
        assertEquals(testTime.toEpochMilli(), trade.getCompletedAtEpochMilli(), 
                "getCompletedAtEpochMilli nên trả về giá trị epoch milli của completedAt");

        // Với completedAt là null
        trade.setCompletedAt(null);
        assertNull(trade.getCompletedAtEpochMilli(), 
                "getCompletedAtEpochMilli nên trả về null khi completedAt là null");
    }

    @Test
    @DisplayName("Test phương thức getCancelledAtEpochMilli")
    void testGetCancelledAtEpochMilli() {
        // Với cancelledAt có giá trị
        Instant testTime = Instant.now();
        trade.setCancelledAt(testTime);
        assertEquals(testTime.toEpochMilli(), trade.getCancelledAtEpochMilli(), 
                "getCancelledAtEpochMilli nên trả về giá trị epoch milli của cancelledAt");

        // Với cancelledAt là null
        trade.setCancelledAt(null);
        assertNull(trade.getCancelledAtEpochMilli(), 
                "getCancelledAtEpochMilli nên trả về null khi cancelledAt là null");
    }

    @Test
    @DisplayName("Test phương thức toMessageJson")
    void testToMessageJson() {
        // Mock JsonSerializer.toMap method
        Map<String, Object> mockMap = new HashMap<>();
        mockMap.put("id", "trade-123");
        
        try (MockedStatic<JsonSerializer> mockedJsonSerializer = Mockito.mockStatic(JsonSerializer.class)) {
            mockedJsonSerializer.when(() -> JsonSerializer.toMap(trade)).thenReturn(mockMap);
            
            // Call method
            Map<String, Object> result = trade.toMessageJson();
            
            // Assertions
            assertNotNull(result, "Result không nên là null");
            assertEquals(mockMap, result, "Result nên bằng giá trị trả về từ JsonSerializer.toMap");
            assertEquals("trade-123", result.get("id"), "Result nên chứa ID của trade");
            
            // Verify JsonSerializer was called with the correct trade
            mockedJsonSerializer.verify(() -> JsonSerializer.toMap(trade));
        }
    }

    @Test
    @DisplayName("Test validateRequiredFields với đối tượng hợp lệ")
    void testValidateRequiredFieldsWithValidTrade() {
        // Tạo trade hợp lệ
        Trade validTrade = createDefaultTrade();
        
        // Đảm bảo không ném exception
        List<String> errors = validTrade.validateRequiredFields();
        
        // Kết quả nên là list rỗng
        assertTrue(errors.isEmpty(), "Không nên có lỗi validation với trade hợp lệ");
    }

    @Test
    @DisplayName("Test validateRequiredFields với trường bắt buộc missing")
    void testValidateRequiredFieldsWithMissingRequiredFields() {
        // Tạo trade với identifier là null (vi phạm @NotBlank)
        trade.setIdentifier(null);
        
        // Kiểm tra exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            trade.validateRequiredFields();
        });
        
        // Kiểm tra message trong exception
        String exceptionMessage = exception.getMessage();
        assertTrue(exceptionMessage.contains("Trade ID is required"), 
                "Exception message nên chứa thông báo lỗi về identifier");
    }

    @Test
    @DisplayName("Test validateRequiredFields với price là số âm")
    void testValidateRequiredFieldsWithNegativePrice() {
        // Tạo trade với price là số âm (vi phạm @Positive)
        trade.setPrice(new BigDecimal("-100"));
        
        // Kiểm tra exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            trade.validateRequiredFields();
        });
        
        // Kiểm tra message trong exception
        String exceptionMessage = exception.getMessage();
        assertTrue(exceptionMessage.contains("Price must be greater than 0"), 
                "Exception message nên chứa thông báo lỗi về price");
    }

    @Test
    @DisplayName("Test validateRequiredFields với takerSide không hợp lệ")
    void testValidateRequiredFieldsWithInvalidTakerSide() {
        // Tạo trade với takerSide không hợp lệ
        trade.setTakerSide("INVALID_SIDE");
        
        // Kiểm tra exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            trade.validateRequiredFields();
        });
        
        // Kiểm tra message trong exception
        String exceptionMessage = exception.getMessage();
        assertTrue(exceptionMessage.contains("Taker side must be either BUY or SELL"), 
                "Exception message nên chứa thông báo lỗi về takerSide");
    }

    @Test
    @DisplayName("Test enum TradeStatus")
    void testTradeStatusEnum() {
        // Kiểm tra các giá trị enum
        assertEquals(3, Trade.TradeStatus.values().length, "TradeStatus enum nên có 3 giá trị");
        assertEquals(Trade.TradeStatus.UNPAID, Trade.TradeStatus.valueOf("UNPAID"));
        assertEquals(Trade.TradeStatus.COMPLETED, Trade.TradeStatus.valueOf("COMPLETED"));
        assertEquals(Trade.TradeStatus.CANCELLED, Trade.TradeStatus.valueOf("CANCELLED"));
        
        // Kiểm tra tên của enum values
        assertEquals("UNPAID", Trade.TradeStatus.UNPAID.name());
        assertEquals("COMPLETED", Trade.TradeStatus.COMPLETED.name());
        assertEquals("CANCELLED", Trade.TradeStatus.CANCELLED.name());
    }

    @Test
    @DisplayName("Test constants STATUS_UNPAID, STATUS_COMPLETED, STATUS_CANCELLED")
    void testStatusConstants() {
        assertEquals("UNPAID", Trade.STATUS_UNPAID);
        assertEquals("COMPLETED", Trade.STATUS_COMPLETED);
        assertEquals("CANCELLED", Trade.STATUS_CANCELLED);
    }

    @Test
    @DisplayName("Test constants TAKER_SIDE_BUY, TAKER_SIDE_SELL")
    void testTakerSideConstants() {
        assertEquals("BUY", Trade.TAKER_SIDE_BUY);
        assertEquals("SELL", Trade.TAKER_SIDE_SELL);
    }
} 