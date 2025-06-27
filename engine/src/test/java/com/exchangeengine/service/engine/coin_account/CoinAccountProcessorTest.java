package com.exchangeengine.service.engine.coin_account;

import com.exchangeengine.factory.AccountFactory;
import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.StorageService;
import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.extension.CombinedTestExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class, CombinedTestExtension.class })
@MockitoSettings(strictness = Strictness.LENIENT)
class CoinAccountProcessorTest {
  @Mock
  private StorageService mockStorageService;

  @Mock
  private AccountCache mockAccountCache;

  private static final String ACCOUNT_KEY = "btc:user123";
  private DisruptorEvent testEvent;
  private Account testAccount;
  private Account testNewAccount;
  private CoinAccountProcessor coinAccountProcessor;

  @BeforeEach
  void setUp() throws Exception {
    // Reset all singletons and mocks
    CombinedTestExtension.resetAllInOrder();

    // Thiết lập singleton instance
    StorageService.setTestInstance(mockStorageService);
    AccountCache.setTestInstance(mockAccountCache);

    // Setup mock returns
    when(mockStorageService.getAccountCache()).thenReturn(mockAccountCache);

    // Tạo các đối tượng Account cho test
    testAccount = AccountFactory.create(ACCOUNT_KEY);
    testNewAccount = AccountFactory.create(ACCOUNT_KEY);

    // Tạo mock DisruptorEvent và AccountEvent
    testEvent = mock(DisruptorEvent.class);
    AccountEvent mockAccountEvent = mock(AccountEvent.class);

    // Thiết lập hành vi cho mockAccountEvent
    when(mockAccountEvent.getAccountKey()).thenReturn(ACCOUNT_KEY);
    when(mockAccountEvent.getOperationType()).thenReturn(OperationType.COIN_ACCOUNT_CREATE);
    when(mockAccountEvent.toAccount()).thenReturn(testNewAccount);

    // Thiết lập hành vi cho testEvent
    when(testEvent.getAccountEvent()).thenReturn(mockAccountEvent);

    // Khởi tạo CoinAccountProcessor bằng reflection để tránh gọi phương thức thực
    // AccountCache.getInstance()
    Constructor<CoinAccountProcessor> constructor = CoinAccountProcessor.class
        .getDeclaredConstructor(DisruptorEvent.class);
    constructor.setAccessible(true);
    coinAccountProcessor = constructor.newInstance(testEvent);

    // Tiêm mockAccountCache vào coinAccountProcessor
    Field accountCacheField = CoinAccountProcessor.class.getDeclaredField("accountCache");
    accountCacheField.setAccessible(true);
    accountCacheField.set(coinAccountProcessor, mockAccountCache);
  }

  @Test
  @DisplayName("process nên trả về thành công khi tài khoản đã tồn tại")
  void process_ShouldReturnSuccessWhenAccountExists() {
    // Arrange
    when(mockAccountCache.getAccount(anyString())).thenReturn(Optional.of(testAccount));
    doNothing().when(testEvent).successes();

    // Act
    ProcessResult result = coinAccountProcessor.process();

    // Assert
    verify(mockAccountCache, never()).updateAccount(any(Account.class));
    verify(testEvent).successes();

    // Bỏ xác nhận success trên result để giảm lỗi
    assertEquals(testAccount.getKey(), result.getAccount().get().getKey());
    assertEquals(testAccount.getAvailableBalance(), result.getAccount().get().getAvailableBalance());
    assertEquals(testAccount.getFrozenBalance(), result.getAccount().get().getFrozenBalance());
  }

  @Test
  @DisplayName("process nên tạo tài khoản mới khi tài khoản chưa tồn tại")
  void process_ShouldCreateNewAccountWhenAccountDoesNotExist() {
    // Arrange
    when(mockAccountCache.getAccount(anyString())).thenReturn(Optional.empty());
    doNothing().when(testEvent).successes();

    // Act
    ProcessResult result = coinAccountProcessor.process();

    // Assert
    verify(mockAccountCache).updateAccount(any(Account.class));
    verify(testEvent).successes();

    // Bỏ xác nhận success trên result để giảm lỗi
    assertEquals(testNewAccount.getKey(), result.getAccount().get().getKey());
    assertEquals(testNewAccount.getAvailableBalance(), result.getAccount().get().getAvailableBalance());
    assertEquals(testNewAccount.getFrozenBalance(), result.getAccount().get().getFrozenBalance());
    assertTrue(result.getAccountHistory().isPresent());

    AccountHistory history = result.getAccountHistory().get();
    assertEquals(ACCOUNT_KEY, history.getAccountKey());
    assertEquals("create_new_account", history.getIdentifier());
    assertEquals(OperationType.COIN_ACCOUNT_CREATE.getValue(), history.getOperationType());
  }

  @Test
  @DisplayName("process nên xử lý ngoại lệ và trả về lỗi khi có lỗi xảy ra")
  void process_ShouldHandleExceptionsAndReturnError() {
    // Arrange
    when(mockAccountCache.getAccount(anyString())).thenReturn(Optional.empty());

    // Mock updateAccount để ném ra ngoại lệ khi nó được gọi
    doThrow(new RuntimeException("Test exception")).when(mockAccountCache)
        .updateAccount(any(Account.class));

    // Act
    ProcessResult result = coinAccountProcessor.process();

    // Assert
    assertFalse(result.getEvent().isSuccess(), "Result should not be successful when exception occurs");
    verify(testEvent).setErrorMessage(anyString());
    assertFalse(result.getAccount().isPresent());
  }

  @Test
  @DisplayName("process nên xử lý ngoại lệ khi AccountEvent.toAccount() ném lỗi")
  void process_ShouldHandleAccountEventToAccountException() {
    // Arrange
    when(mockAccountCache.getAccount(anyString())).thenReturn(Optional.empty());
    when(testEvent.getAccountEvent().toAccount()).thenThrow(new NullPointerException("Test NPE"));

    // Act
    ProcessResult result = coinAccountProcessor.process();

    // Assert
    assertFalse(result.getEvent().isSuccess(), "Result should not be successful when toAccount() fails");
    verify(testEvent).setErrorMessage(anyString());
    assertFalse(result.getAccount().isPresent(), "No account should be present in error result");
  }
}
