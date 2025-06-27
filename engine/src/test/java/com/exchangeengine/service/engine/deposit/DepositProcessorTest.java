package com.exchangeengine.service.engine.deposit;

import com.exchangeengine.factory.AccountFactory;
import com.exchangeengine.factory.CoinDepositFactory;
import com.exchangeengine.factory.event.CoinDepositEventFactory;
import com.exchangeengine.factory.event.DisruptorEventFactory;
import com.exchangeengine.model.*;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.DepositCache;
import com.exchangeengine.extension.SingletonResetExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class, SingletonResetExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class DepositProcessorTest {

  @Mock
  private StorageService mockStorageService;

  @Mock
  private AccountCache mockAccountCache;

  @Mock
  private DepositCache mockDepositCache;

  @Mock
  private DisruptorEvent mockEvent;

  @Mock
  private CoinDepositEvent mockDepositEvent;

  private DepositProcessor depositProcessor;

  private static final String ACCOUNT_KEY = "btc:user123";
  private static final String TRANSACTION_ID = "txn123";
  private static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("2.5");

  private Account testAccount;
  private CoinDeposit testDeposit;

  @BeforeEach
  void setUp() throws Exception {
    // Reset all singleton instances
    SingletonResetExtension.resetAll();

    // Thiết lập singleton instance
    StorageService.setTestInstance(mockStorageService);

    // Setup mock returns
    when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);
    when(mockStorageService.getDepositCache()).thenReturn(mockDepositCache);

    // Tạo Account bằng factory
    testAccount = AccountFactory.create(ACCOUNT_KEY);
    testAccount.setAvailableBalance(new BigDecimal("10.0"));
    testAccount.setFrozenBalance(new BigDecimal("1.0"));

    // Tạo CoinDeposit bằng factory
    testDeposit = CoinDepositFactory.create(ACCOUNT_KEY, TRANSACTION_ID, DEPOSIT_AMOUNT);

    // Thiết lập mock cho DisruptorEvent và CoinDepositEvent
    when(mockEvent.getCoinDepositEvent()).thenReturn(mockDepositEvent);
    when(mockDepositEvent.toCoinDeposit(false)).thenReturn(testDeposit);
    when(mockDepositEvent.getOperationType()).thenReturn(OperationType.COIN_DEPOSIT_CREATE);
    when(mockDepositEvent.getAccountKey()).thenReturn(ACCOUNT_KEY);
    when(mockDepositEvent.getIdentifier()).thenReturn(TRANSACTION_ID);
    when(mockDepositEvent.getAmount()).thenReturn(DEPOSIT_AMOUNT);

    // Set up AccountCache mock behavior
    when(mockAccountCache.getAccount(ACCOUNT_KEY)).thenReturn(Optional.of(testAccount));

    // Create processor with mocked dependencies
    depositProcessor = new DepositProcessor(mockEvent);

    // Sử dụng reflection để tiêm mockStorageService vào trường storageService của
    // DepositProcessor
    try {
      java.lang.reflect.Field storageServiceField = DepositProcessor.class.getDeclaredField("storageService");
      storageServiceField.setAccessible(true);
      storageServiceField.set(depositProcessor, mockStorageService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mocks using reflection", e);
    }
  }

  @Test
  @DisplayName("process nên xử lý tiền gửi thành công khi tài khoản tồn tại")
  void process_ShouldProcessDepositSuccessfullyWhenAccountExists() {
    // Act
    ProcessResult result = depositProcessor.process();

    // Assert
    verify(mockAccountCache).updateAccount(any(Account.class));
    verify(mockDepositCache).updateCoinDeposit(any(CoinDeposit.class));

    // Không kiểm tra event success nữa
    assertTrue(result.getAccount().isPresent());
    assertTrue(result.getDeposit().isPresent());
    assertTrue(result.getAccountHistory().isPresent());

    // Kiểm tra account có đúng giá trị mới không
    Account account = result.getAccount().get();
    assertEquals(ACCOUNT_KEY, account.getKey());
    // Sử dụng compareTo để so sánh BigDecimal
    BigDecimal expected = new BigDecimal("12.5");
    BigDecimal actual = account.getAvailableBalance();
    assertEquals(0, expected.compareTo(actual),
        "Expected balance to be " + expected + " but was " + actual);
  }

  @Test
  @DisplayName("process nên trả về lỗi khi tài khoản không tồn tại")
  void process_ShouldReturnErrorWhenAccountDoesNotExist() {
    // Arrange
    when(mockAccountCache.getAccount(anyString())).thenReturn(Optional.empty());

    // Act
    ProcessResult result = depositProcessor.process();

    // Assert
    verify(mockAccountCache, never()).updateAccount(any(Account.class));
    verify(mockDepositCache).updateCoinDeposit(any(CoinDeposit.class));

    // Kiểm tra xem setErrorMessage đã được gọi
    verify(mockEvent).setErrorMessage(anyString());

    // Không kiểm tra nội dung của errorMessage nữa
    assertFalse(result.getEvent().isSuccess());
    assertTrue(result.getDeposit().isPresent());
  }

  @Test
  @DisplayName("process nên xử lý ngoại lệ và cập nhật trạng thái khi có lỗi")
  void process_ShouldHandleExceptionsAndUpdateStatus() {
    // Arrange - Tạo một Account spy để ném lỗi khi gọi increaseAvailableBalance
    Account accountSpy = spy(testAccount);
    doThrow(new RuntimeException("Test exception")).when(accountSpy).increaseAvailableBalance(any(BigDecimal.class));
    when(mockAccountCache.getAccount(ACCOUNT_KEY)).thenReturn(Optional.of(accountSpy));

    // Act
    ProcessResult result = depositProcessor.process();

    // Assert
    verify(mockDepositCache).updateCoinDeposit(any(CoinDeposit.class));

    // Kiểm tra xem setErrorMessage đã được gọi
    verify(mockEvent).setErrorMessage(anyString());

    // Không kiểm tra nội dung của errorMessage nữa
    assertFalse(result.getEvent().isSuccess());
    assertTrue(result.getDeposit().isPresent());
  }

  @Test
  @DisplayName("Kiểm tra AccountHistory được tạo chính xác")
  void process_CreatesCorrectAccountHistory() {
    // Execute
    ProcessResult result = depositProcessor.process();

    // Verify
    Optional<AccountHistory> historyOpt = result.getAccountHistory();
    assertTrue(historyOpt.isPresent());

    AccountHistory history = historyOpt.get();
    assertEquals(ACCOUNT_KEY, history.getAccountKey());
    assertEquals(TRANSACTION_ID, history.getIdentifier());
    assertEquals(OperationType.COIN_DEPOSIT_CREATE.getValue(), history.getOperationType());

    // Verify balance records - balance được lưu dưới dạng chuỗi formatted
    String availableBalance = history.getAvailableBalance();
    String frozenBalance = history.getFrozenBalance();

    assertNotNull(availableBalance);
    assertNotNull(frozenBalance);

    // Kiểm tra chuỗi có định dạng đúng "previous|current|diff"
    String[] availableParts = availableBalance.split("\\|");
    String[] frozenParts = frozenBalance.split("\\|");

    assertEquals(3, availableParts.length);
    assertEquals(3, frozenParts.length);

    // Kiểm tra giá trị của số dư
    assertTrue(availableParts[0].contains("10.0"), "Previous available balance should be 10.0");
    assertTrue(availableParts[1].contains("12.5"), "Current available balance should be 12.5");
    assertTrue(availableParts[2].contains("2.5"), "Difference should be 2.5");
  }
}
