package com.exchangeengine.integration;

import com.exchangeengine.extension.CombinedTestExtension;
import com.exchangeengine.factory.AccountFactory;
import com.exchangeengine.factory.CoinWithdrawalFactory;
import com.exchangeengine.messaging.producer.KafkaProducerService;
import com.exchangeengine.model.*;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.service.engine.OutputProcessor;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.WithdrawalCache;
import com.exchangeengine.storage.rocksdb.RocksDBService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CombinedTestExtension.class})
public class WithdrawalIntegrationTest {

    @Mock
    private KafkaProducerService mockKafkaProducerService;
    
    @Mock
    private StorageService mockStorageService;
    
    @Mock
    private AccountCache mockAccountCache;
    
    @Mock
    private WithdrawalCache mockWithdrawalCache;
    
    @Mock
    private RocksDBService mockRocksDBService;
    
    private MockedStatic<RocksDBService> mockedRocksDBService;
    private MockedStatic<StorageService> mockedStorageService;
    private MockedStatic<KafkaProducerService> mockedKafkaProducerService;
    
    private static final String SENDER_ACCOUNT = "btc:sender:123";
    private static final String RECIPIENT_ACCOUNT = "btc:recipient:456";
    private static final String TRANSACTION_ID = "tx-" + System.currentTimeMillis();
    private static final BigDecimal AMOUNT = new BigDecimal("1.0");
    private static final BigDecimal FEE = new BigDecimal("0.1");
    
    private OutputProcessor outputProcessor;
    
    @BeforeEach
    void setUp() throws Exception {
        // Setup mocks for static methods
        mockedRocksDBService = mockStatic(RocksDBService.class);
        mockedRocksDBService.when(RocksDBService::getInstance).thenReturn(mockRocksDBService);
        
        mockedStorageService = mockStatic(StorageService.class);
        mockedStorageService.when(StorageService::getInstance).thenReturn(mockStorageService);
        
        mockedKafkaProducerService = mockStatic(KafkaProducerService.class);
        mockedKafkaProducerService.when(KafkaProducerService::getInstance).thenReturn(mockKafkaProducerService);
        
        // Setup mock storage service - use lenient() to avoid UnnecessaryStubbingException
        lenient().when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);
        lenient().when(mockStorageService.getWithdrawalCache()).thenReturn(mockWithdrawalCache);
        
        // Create test accounts 
        Account senderAccount = AccountFactory.create(SENDER_ACCOUNT);
        senderAccount.setAvailableBalance(new BigDecimal("10.0"));
        
        Account recipientAccount = AccountFactory.create(RECIPIENT_ACCOUNT);
        recipientAccount.setAvailableBalance(new BigDecimal("5.0"));
        
        // Setup mock account cache behavior - use lenient() to avoid UnnecessaryStubbingException
        lenient().when(mockAccountCache.getAccount(SENDER_ACCOUNT)).thenReturn(Optional.of(senderAccount));
        lenient().when(mockAccountCache.getAccount(RECIPIENT_ACCOUNT)).thenReturn(Optional.of(recipientAccount));
        
        // Create OutputProcessor with mocks through reflection
        Constructor<OutputProcessor> constructor = OutputProcessor.class.getDeclaredConstructor(KafkaProducerService.class);
        constructor.setAccessible(true);
        outputProcessor = constructor.newInstance(mockKafkaProducerService);
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
    
    @Test
    @DisplayName("Verify recipient account update events are sent when withdrawal is released")
    void testRecipientAccountUpdateEventSentOnWithdrawalRelease() {
        // GIVEN: A withdrawal with a recipient account
        CoinWithdrawal withdrawal = CoinWithdrawalFactory.create(SENDER_ACCOUNT, TRANSACTION_ID, AMOUNT);
        withdrawal.setFee(FEE);
        withdrawal.setRecipientAccountKey(RECIPIENT_ACCOUNT);
        withdrawal.setStatus("processing");
        
        // Use lenient() to avoid UnnecessaryStubbingException
        lenient().when(mockWithdrawalCache.getWithdrawal(TRANSACTION_ID)).thenReturn(Optional.of(withdrawal));
        
        // Create accounts for the result
        Account senderAccount = AccountFactory.create(SENDER_ACCOUNT);
        senderAccount.setAvailableBalance(new BigDecimal("10.0").subtract(AMOUNT).subtract(FEE));
        
        Account recipientAccount = AccountFactory.create(RECIPIENT_ACCOUNT);
        recipientAccount.setAvailableBalance(new BigDecimal("5.0").add(AMOUNT));
        
        // WHEN: Create a ProcessResult with account and recipient account
        DisruptorEvent releaseEvent = new DisruptorEvent();
        CoinWithdrawalEvent releaseWithdrawalEvent = new CoinWithdrawalEvent();
        releaseWithdrawalEvent.setOperationType(OperationType.COIN_WITHDRAWAL_RELEASING);
        releaseWithdrawalEvent.setActionType(ActionType.COIN_TRANSACTION);
        releaseWithdrawalEvent.setIdentifier(TRANSACTION_ID);
        releaseWithdrawalEvent.setAccountKey(SENDER_ACCOUNT);
        releaseWithdrawalEvent.setRecipientAccountKey(RECIPIENT_ACCOUNT);
        releaseWithdrawalEvent.setEventId("event-release-" + System.currentTimeMillis());
        releaseEvent.setCoinWithdrawalEvent(releaseWithdrawalEvent);
        
        ProcessResult releaseResult = ProcessResult.success(releaseEvent);
        releaseResult.setAccount(senderAccount);
        releaseResult.setRecipientAccount(recipientAccount);
        releaseResult.setWithdrawal(withdrawal);
        
        AccountHistory senderHistory = new AccountHistory(
            SENDER_ACCOUNT, TRANSACTION_ID, "coin_withdrawal_releasing");
        AccountHistory recipientHistory = new AccountHistory(
            RECIPIENT_ACCOUNT, TRANSACTION_ID, "recipient_coin_withdrawal_releasing");
            
        releaseResult.setAccountHistory(senderHistory);
        releaseResult.setRecipientAccountHistory(recipientHistory);
        
        // Process the result in OutputProcessor
        outputProcessor.processOutput(releaseResult, true);
        
        // THEN: Verify that both sender and recipient account updates were sent to Kafka
        verify(mockKafkaProducerService).sendCoinAccountUpdate(anyString(), eq(senderAccount));
        verify(mockKafkaProducerService).sendCoinAccountUpdate(anyString(), eq(recipientAccount));
        verify(mockKafkaProducerService).sendTransactionResult(releaseEvent);
    }
}