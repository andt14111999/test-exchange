package com.exchangeengine.service.engine;

import static org.mockito.Mockito.*;

import com.exchangeengine.factory.AccountFactory;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.rocksdb.RocksDBService;
import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.storage.cache.AccountCache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
public class OutputProcessorDirectTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private StorageService storageService;
    
    @Mock
    private RocksDBService rocksDBService;
    
    @Mock
    private AccountCache accountCache;

    private OutputProcessor outputProcessor;
    private MockedStatic<RocksDBService> mockedRocksDBService;
    private MockedStatic<StorageService> mockedStorageService;
    private MockedStatic<KafkaProducerService> mockedKafkaProducerService;
    
    @BeforeEach
    void setUp() throws Exception {
        // Setup mocks for static methods
        mockedRocksDBService = mockStatic(RocksDBService.class);
        mockedRocksDBService.when(RocksDBService::getInstance).thenReturn(rocksDBService);
        
        mockedStorageService = mockStatic(StorageService.class);
        mockedStorageService.when(StorageService::getInstance).thenReturn(storageService);
        
        mockedKafkaProducerService = mockStatic(KafkaProducerService.class);
        mockedKafkaProducerService.when(KafkaProducerService::getInstance).thenReturn(kafkaProducerService);
        
        // Setup account cache - use lenient() to avoid UnnecessaryStubbingException
        lenient().when(storageService.getAccountCache()).thenReturn(accountCache);
        
        // Access private constructor using reflection
        Constructor<OutputProcessor> constructor = OutputProcessor.class.getDeclaredConstructor(KafkaProducerService.class);
        constructor.setAccessible(true);
        outputProcessor = constructor.newInstance(kafkaProducerService);
    }
    
    @AfterEach
    void tearDown() {
        if (mockedRocksDBService != null) {
            mockedRocksDBService.close();
        }
        if (mockedStorageService != null) {
            mockedStorageService.close();
        }
        if (mockedKafkaProducerService != null) {
            mockedKafkaProducerService.close();
        }
    }

    /**
     * Helper method to create a test event with event ID set
     */
    private DisruptorEvent createTestEvent(String eventId) {
        DisruptorEvent event = new DisruptorEvent();
        AccountEvent accountEvent = new AccountEvent();
        accountEvent.setEventId(eventId);
        event.setAccountEvent(accountEvent);
        return event;
    }

    @Test
    void sendEventToKafka_ShouldSendCoinAccountUpdateForBothAccounts_WhenRecipientAccountIsPresent() throws Exception {
        // Given
        DisruptorEvent event = createTestEvent("test-event-id");
        
        // Create sender and recipient accounts
        Account senderAccount = AccountFactory.create("btc:sender:123");
        Account recipientAccount = AccountFactory.create("btc:recipient:456");
        
        ProcessResult result = ProcessResult.success(event);
        result.setAccount(senderAccount);
        result.setRecipientAccount(recipientAccount);
        
        // Make sendEventToKafka method accessible
        Method sendEventToKafkaMethod = OutputProcessor.class.getDeclaredMethod("sendEventToKafka", ProcessResult.class);
        sendEventToKafkaMethod.setAccessible(true);
        
        // When
        sendEventToKafkaMethod.invoke(outputProcessor, result);
        
        // Then
        verify(kafkaProducerService).sendCoinAccountUpdate("test-event-id", senderAccount);
        verify(kafkaProducerService).sendCoinAccountUpdate("test-event-id", recipientAccount);
        verify(kafkaProducerService).sendTransactionResult(event);
    }
} 