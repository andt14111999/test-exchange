package com.exchangeengine.service.engine.offer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exchangeengine.model.Offer;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.OfferEvent;
import com.exchangeengine.storage.cache.OfferCache;

@ExtendWith(MockitoExtension.class)
public class OfferProcessorTest {

    @Mock(lenient = true)
    private DisruptorEvent mockEvent;
    
    @Mock(lenient = true)
    private OfferEvent mockOfferEvent;
    
    @Mock(lenient = true)
    private Offer mockOffer;
    
    @Mock(lenient = true)
    private OfferCache mockOfferCache;
    
    private OfferProcessor processor;
    
    @BeforeEach
    public void setUp() {
        // Cấu hình mock cơ bản
        when(mockEvent.getOfferEvent()).thenReturn(mockOfferEvent);
        when(mockEvent.isSuccess()).thenReturn(true);
        
        processor = new OfferProcessor(mockEvent);
    }
    
    @Test
    @DisplayName("process với OfferEvent là null")
    public void testProcessWithNullOfferEvent() {
        // Given
        when(mockEvent.getOfferEvent()).thenReturn(null);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage("Offer event is null");
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("process với loại OperationType không được hỗ trợ")
    public void testProcessWithUnsupportedOperationType() {
        // Given
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.BALANCE_QUERY); // Loại không hỗ trợ
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage(contains("Unsupported operation type"));
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("process xử lý ngoại lệ")
    public void testProcessHandlesException() {
        // Given
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_CREATE);
        when(mockOfferEvent.toOffer(anyBoolean())).thenThrow(new RuntimeException("Test exception"));
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage("Test exception");
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processCreateOperation với offer đã tồn tại")
    public void testProcessCreateWithExistingOffer() {
        // Given
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_CREATE);
        when(mockOfferEvent.getIdentifier()).thenReturn("offer-123");
        when(mockOfferEvent.fetchOffer(false)).thenReturn(Optional.of(mockOffer));
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage(contains("Offer already exists"));
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processCreateOperation thành công")
    public void testProcessCreateSuccess() {
        // Given
        String identifier = "offer-123";
        BigDecimal totalAmount = BigDecimal.TEN;
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_CREATE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.getTotalAmount()).thenReturn(totalAmount);
        when(mockOfferEvent.fetchOffer(false)).thenReturn(Optional.empty());
        when(mockOfferEvent.toOffer(false)).thenReturn(mockOffer);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockOffer).setDisabled(false);
        verify(mockOffer).setDeleted(false);
        verify(mockOffer).setAvailableAmount(totalAmount);
        verify(mockOfferEvent).updateOffer(mockOffer);
        verify(mockEvent).successes();
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processCreateOperation bắt ngoại lệ và tạo offer thất bại")
    public void testProcessCreateExceptionAndCreateFailedOffer() {
        // Given
        String identifier = "offer-123";
        String userId = "user-123";
        BigDecimal totalAmount = BigDecimal.TEN;
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_CREATE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.getUserId()).thenReturn(userId);
        when(mockOfferEvent.getOfferType()).thenReturn("BUY");
        when(mockOfferEvent.getCoinCurrency()).thenReturn("btc");
        when(mockOfferEvent.getCurrency()).thenReturn("usd");
        when(mockOfferEvent.getTotalAmount()).thenReturn(totalAmount);
        when(mockOfferEvent.fetchOffer(false)).thenReturn(Optional.empty());
        when(mockOfferEvent.toOffer(false)).thenReturn(mockOffer);
        
        // Gây ra ngoại lệ khi cập nhật offer
        doThrow(new RuntimeException("Update failed")).when(mockOfferEvent).updateOffer(mockOffer);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage("Update failed");
        assertNotNull(result);
        assertTrue(result.getOffer().isPresent());
        Offer failedOffer = result.getOffer().get();
        assertTrue(failedOffer.getDisabled());
        assertEquals(userId, failedOffer.getUserId());
        assertEquals(Offer.OfferStatus.PENDING, failedOffer.getStatus());
    }
    
    @Test
    @DisplayName("processUpdateOperation với offer bị disabled hoặc deleted")
    public void testProcessUpdateWithDisabledOrDeletedOffer() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_UPDATE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDisabled()).thenReturn(true);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage(contains("Cannot update: Offer is disabled or deleted"));
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processUpdateOperation thành công")
    public void testProcessUpdateSuccess() {
        // Given
        String identifier = "offer-123";
        BigDecimal totalAmount = BigDecimal.TEN;
        BigDecimal availableAmount = BigDecimal.ONE;
        BigDecimal price = new BigDecimal("100.0");
        BigDecimal minAmount = new BigDecimal("0.01");
        BigDecimal maxAmount = new BigDecimal("1.0");
        BigDecimal margin = new BigDecimal("1.5");
        String paymentMethodId = "pm-123";
        Integer paymentTime = 30;
        String countryCode = "US";
        Boolean automatic = true;
        Boolean online = true;
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_UPDATE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.getTotalAmount()).thenReturn(totalAmount);
        when(mockOfferEvent.getAvailableAmount()).thenReturn(availableAmount);
        when(mockOfferEvent.getPrice()).thenReturn(price);
        when(mockOfferEvent.getMinAmount()).thenReturn(minAmount);
        when(mockOfferEvent.getMaxAmount()).thenReturn(maxAmount);
        when(mockOfferEvent.getPaymentMethodId()).thenReturn(paymentMethodId);
        when(mockOfferEvent.getPaymentTime()).thenReturn(paymentTime);
        when(mockOfferEvent.getCountryCode()).thenReturn(countryCode);
        when(mockOfferEvent.getAutomatic()).thenReturn(automatic);
        when(mockOfferEvent.getOnline()).thenReturn(online);
        when(mockOfferEvent.getMargin()).thenReturn(margin);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDisabled()).thenReturn(false);
        when(mockOffer.getDeleted()).thenReturn(false);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockOffer).setPrice(price);
        verify(mockOffer).setMinAmount(minAmount);
        verify(mockOffer).setMaxAmount(maxAmount);
        verify(mockOffer).setTotalAmount(totalAmount);
        verify(mockOffer).setAvailableAmount(availableAmount);
        verify(mockOffer).setPaymentMethodId(paymentMethodId);
        verify(mockOffer).setPaymentTime(paymentTime);
        verify(mockOffer).setCountryCode(countryCode);
        verify(mockOffer).setAutomatic(automatic);
        verify(mockOffer).setOnline(online);
        verify(mockOffer).setMargin(margin);
        verify(mockOfferEvent).updateOffer(mockOffer);
        verify(mockEvent).successes();
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processUpdateOperation bắt ngoại lệ")
    public void testProcessUpdateException() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_UPDATE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDisabled()).thenReturn(false);
        when(mockOffer.getDeleted()).thenReturn(false);
        
        // Gây ra ngoại lệ khi cập nhật offer
        doThrow(new RuntimeException("Update failed")).when(mockOfferEvent).updateOffer(mockOffer);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage("Update failed");
        assertNotNull(result);
        assertTrue(result.getOffer().isPresent());
    }
    
    @Test
    @DisplayName("processDisableOperation với offer đã bị disabled hoặc deleted")
    public void testProcessDisableWithAlreadyDisabledOrDeletedOffer() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DISABLE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDisabled()).thenReturn(true);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage(contains("Offer is already disabled or deleted"));
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processDisableOperation thành công")
    public void testProcessDisableSuccess() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DISABLE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDisabled()).thenReturn(false);
        when(mockOffer.getDeleted()).thenReturn(false);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockOffer).setDisabled(true);
        verify(mockOfferEvent).updateOffer(mockOffer);
        verify(mockEvent).successes();
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processDisableOperation bắt ngoại lệ")
    public void testProcessDisableException() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DISABLE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDisabled()).thenReturn(false);
        when(mockOffer.getDeleted()).thenReturn(false);
        
        // Gây ra ngoại lệ khi cập nhật offer
        doThrow(new RuntimeException("Disable failed")).when(mockOfferEvent).updateOffer(mockOffer);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage("Disable failed");
        assertNotNull(result);
        assertTrue(result.getOffer().isPresent());
    }
    
    @Test
    @DisplayName("processEnableOperation với offer đã bị deleted")
    public void testProcessEnableWithDeletedOffer() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_ENABLE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDeleted()).thenReturn(true);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage(contains("Cannot enable: Offer is deleted"));
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processEnableOperation thành công")
    public void testProcessEnableSuccess() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_ENABLE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDeleted()).thenReturn(false);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockOffer).setDisabled(false);
        verify(mockOfferEvent).updateOffer(mockOffer);
        verify(mockEvent).successes();
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processEnableOperation bắt ngoại lệ")
    public void testProcessEnableException() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_ENABLE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDeleted()).thenReturn(false);
        
        // Gây ra ngoại lệ khi cập nhật offer
        doThrow(new RuntimeException("Enable failed")).when(mockOfferEvent).updateOffer(mockOffer);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage("Enable failed");
        assertNotNull(result);
        assertTrue(result.getOffer().isPresent());
    }
    
    @Test
    @DisplayName("processDeleteOperation với offer đã bị deleted")
    public void testProcessDeleteWithAlreadyDeletedOffer() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DELETE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDeleted()).thenReturn(true);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage(contains("Offer is already deleted"));
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processDeleteOperation thành công")
    public void testProcessDeleteSuccess() {
        // Given
        String identifier = "offer-123";
        String offerType = "BUY";
        BigDecimal totalAmount = BigDecimal.TEN;
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DELETE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.getOfferType()).thenReturn(offerType);
        when(mockOfferEvent.getTotalAmount()).thenReturn(totalAmount);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDeleted()).thenReturn(false);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockOffer).setDisabled(true);
        verify(mockOffer).setDeleted(true);
        verify(mockOffer).setAvailableAmount(BigDecimal.ZERO);
        verify(mockOfferEvent).updateOffer(mockOffer);
        verify(mockEvent).successes();
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processDeleteOperation bắt ngoại lệ")
    public void testProcessDeleteException() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DELETE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDeleted()).thenReturn(false);
        
        // Gây ra ngoại lệ khi cập nhật offer
        doThrow(new RuntimeException("Delete failed")).when(mockOfferEvent).updateOffer(mockOffer);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage("Delete failed");
        assertNotNull(result);
        assertTrue(result.getOffer().isPresent());
    }
    
    @Test
    @DisplayName("processUpdateOperation với offer bị deleted")
    public void testProcessUpdateWithDeletedOffer() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_UPDATE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDisabled()).thenReturn(false);
        when(mockOffer.getDeleted()).thenReturn(true);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage(contains("Cannot update: Offer is disabled or deleted"));
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("processDisableOperation với offer bị deleted")
    public void testProcessDisableWithDeletedOffer() {
        // Given
        String identifier = "offer-123";
        
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DISABLE);
        when(mockOfferEvent.getIdentifier()).thenReturn(identifier);
        when(mockOfferEvent.toOffer(true)).thenReturn(mockOffer);
        when(mockOffer.getDisabled()).thenReturn(false);
        when(mockOffer.getDeleted()).thenReturn(true);
        
        // When
        ProcessResult result = processor.process();
        
        // Then
        verify(mockEvent).setErrorMessage(contains("Offer is already disabled or deleted"));
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("process với tất cả các loại OperationType hợp lệ")
    public void testProcessWithAllValidOperationTypes() {
        // Tạo các mock offer thành công cho mỗi operation
        when(mockOffer.getDisabled()).thenReturn(false);
        when(mockOffer.getDeleted()).thenReturn(false);
        when(mockOfferEvent.fetchOffer(false)).thenReturn(Optional.empty());
        when(mockOfferEvent.toOffer(anyBoolean())).thenReturn(mockOffer);
        
        // Test OFFER_CREATE
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_CREATE);
        processor.process();
        verify(mockEvent, atLeastOnce()).successes();
        
        // Test OFFER_UPDATE
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_UPDATE);
        processor.process();
        verify(mockEvent, atLeast(2)).successes();
        
        // Test OFFER_DISABLE
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DISABLE);
        processor.process();
        verify(mockEvent, atLeast(3)).successes();
        
        // Test OFFER_ENABLE
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_ENABLE);
        processor.process();
        verify(mockEvent, atLeast(4)).successes();
        
        // Test OFFER_DELETE
        when(mockOfferEvent.getOperationType()).thenReturn(OperationType.OFFER_DELETE);
        processor.process();
        verify(mockEvent, atLeast(5)).successes();
    }
}
