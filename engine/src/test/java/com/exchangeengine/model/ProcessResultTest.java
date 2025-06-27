package com.exchangeengine.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.exchangeengine.factory.AccountFactory;
import com.exchangeengine.factory.AccountHistoryFactory;
import com.exchangeengine.factory.AmmPoolFactory;
import com.exchangeengine.factory.CoinDepositFactory;
import com.exchangeengine.factory.CoinWithdrawalFactory;
import com.exchangeengine.factory.MerchantEscrowFactory;
import com.exchangeengine.factory.AmmPositionFactory;
import com.exchangeengine.factory.AmmOrderFactory;
import com.exchangeengine.factory.OfferFactory;
import com.exchangeengine.factory.TradeFactory;
import com.exchangeengine.factory.ProcessResultFactory;
import com.exchangeengine.factory.TickFactory;
import com.exchangeengine.factory.event.DisruptorEventFactory;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.factory.BalanceLockFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;

class ProcessResultTest {
  private ProcessResult emptyResult;
  private AmmPool ammPool;
  private MerchantEscrow merchantEscrow;
  private AmmPosition ammPosition;
  private AmmOrder ammOrder;
  private Offer offer;
  private Trade trade;
  private Tick tick;
  private DisruptorEvent event;
  private Account account;
  private Account fiatAccount;
  private Account coinAccount;
  private AccountHistory accountHistory;
  private AccountHistory fiatAccountHistory;
  private AccountHistory coinAccountHistory;
  private CoinDeposit deposit;
  private CoinWithdrawal withdrawal;
  private BalanceLock balanceLock;

  @BeforeEach
  void setUp() {
    // Initialize event and empty result
    event = DisruptorEventFactory.withAccountEvent();
    emptyResult = ProcessResultFactory.createEmptyResult();

    // Initialize all components needed for tests
    String accountKey = event.getAccountEvent().getAccountKey();
    String eventId = event.getAccountEvent().getEventId();

    // Create test objects
    account = AccountFactory.create(accountKey);
    fiatAccount = AccountFactory.create(accountKey + "_fiat");
    coinAccount = AccountFactory.create(accountKey + "_coin");
    deposit = CoinDepositFactory.create(accountKey, eventId, new BigDecimal("1.0"));
    withdrawal = CoinWithdrawalFactory.create(accountKey, eventId, new BigDecimal("1.0"));
    accountHistory = AccountHistoryFactory.create(
        accountKey, eventId, OperationType.COIN_DEPOSIT_CREATE.getValue());
    fiatAccountHistory = AccountHistoryFactory.create(
        accountKey + "_fiat", eventId, OperationType.COIN_DEPOSIT_CREATE.getValue());
    coinAccountHistory = AccountHistoryFactory.create(
        accountKey + "_coin", eventId, OperationType.COIN_DEPOSIT_CREATE.getValue());
    ammPool = AmmPoolFactory.createDefaultAmmPool();
    merchantEscrow = MerchantEscrowFactory.createDefault();
    ammPosition = AmmPositionFactory.createDefaultAmmPosition();
    ammOrder = AmmOrderFactory.create();
    offer = OfferFactory.create();
    trade = TradeFactory.create();
    tick = new TickFactory().createTick("BTC-USDT", 1000);
    balanceLock = BalanceLockFactory.create();
  }

  @Test
  @DisplayName("Constructor with event should create valid empty result")
  void constructor_WithEvent_ShouldCreateValidResult() {
    // Test constructor với chỉ event
    assertNotNull(emptyResult);
    assertNotNull(emptyResult.getEvent());
    assertTrue(emptyResult.getEvent().isSuccess());
    assertNull(emptyResult.getEvent().getErrorMessage());

    // Các field optional phải trống
    assertTrue(emptyResult.getAccount().isEmpty());
    assertTrue(emptyResult.getDeposit().isEmpty());
    assertTrue(emptyResult.getWithdrawal().isEmpty());
    assertTrue(emptyResult.getAccountHistory().isEmpty());
    assertTrue(emptyResult.getAmmPool().isEmpty());
    assertTrue(emptyResult.getFiatAccount().isEmpty());
    assertTrue(emptyResult.getCoinAccount().isEmpty());
    assertTrue(emptyResult.getFiatAccountHistory().isEmpty());
    assertTrue(emptyResult.getCoinAccountHistory().isEmpty());
    assertTrue(emptyResult.getMerchantEscrow().isEmpty());
    assertTrue(emptyResult.getAccounts().isEmpty());
    assertTrue(emptyResult.getAccountHistories().isEmpty());
    assertTrue(emptyResult.getTicks().isEmpty());
    assertNotNull(emptyResult.getAccounts()); // Map không null, nhưng rỗng
    assertNotNull(emptyResult.getAccountHistories()); // List không null, nhưng rỗng
    assertNotNull(emptyResult.getTicks()); // List không null, nhưng rỗng

    // Test constructor đầy đủ (từ factory)
    ProcessResult fullResult = ProcessResultFactory.createFullResult();
    assertNotNull(fullResult);
    assertNotNull(fullResult.getEvent());
    assertTrue(fullResult.getAccount().isPresent());
    assertTrue(fullResult.getFiatAccount().isPresent());
    assertTrue(fullResult.getCoinAccount().isPresent());
    assertTrue(fullResult.getDeposit().isPresent());
    assertTrue(fullResult.getWithdrawal().isPresent());
    assertTrue(fullResult.getAccountHistory().isPresent());
    assertTrue(fullResult.getFiatAccountHistory().isPresent());
    assertTrue(fullResult.getCoinAccountHistory().isPresent());
    assertTrue(fullResult.getAmmPool().isPresent());
    assertTrue(fullResult.getMerchantEscrow().isPresent());
    assertTrue(fullResult.getEvent().isSuccess());
    assertNotNull(fullResult.getAccounts());
    assertNotNull(fullResult.getAccountHistories());

    // Test constructor error (từ factory)
    ProcessResult errorResult = ProcessResultFactory.createErrorResult();
    assertFalse(errorResult.getEvent().isSuccess());
    assertEquals("Test error message", errorResult.getEvent().getErrorMessage());
  }

  @Test
  @DisplayName("Static factory methods should create correct results")
  void staticFactoryMethods_ShouldCreateCorrectResults() {
    // Test success static method
    DisruptorEvent event = emptyResult.getEvent();
    ProcessResult successResult = ProcessResult.success(event);
    assertNotNull(successResult);
    assertSame(event, successResult.getEvent());
    assertTrue(successResult.getEvent().isSuccess());

    // Test error static method
    String errorMessage = "Test error message";
    ProcessResult errorResult = ProcessResult.error(event, errorMessage);
    assertNotNull(errorResult);
    assertSame(event, errorResult.getEvent());
    assertFalse(errorResult.getEvent().isSuccess());
    assertEquals(errorMessage, errorResult.getEvent().getErrorMessage());
  }

  @Test
  @DisplayName("All setters and getters should work correctly")
  void allSettersAndGetters_ShouldWorkCorrectly() {
    // Verify init state - all fields should be empty
    assertTrue(emptyResult.getAccount().isEmpty());
    assertTrue(emptyResult.getFiatAccount().isEmpty());
    assertTrue(emptyResult.getCoinAccount().isEmpty());
    assertTrue(emptyResult.getDeposit().isEmpty());
    assertTrue(emptyResult.getWithdrawal().isEmpty());
    assertTrue(emptyResult.getAccountHistory().isEmpty());
    assertTrue(emptyResult.getFiatAccountHistory().isEmpty());
    assertTrue(emptyResult.getCoinAccountHistory().isEmpty());
    assertTrue(emptyResult.getAmmPool().isEmpty());
    assertTrue(emptyResult.getMerchantEscrow().isEmpty());
    assertTrue(emptyResult.getAccounts().isEmpty());
    assertTrue(emptyResult.getAccountHistories().isEmpty());

    // Test all setters with method chaining
    ProcessResult result = emptyResult
        .setAccount(account)
        .setFiatAccount(fiatAccount)
        .setCoinAccount(coinAccount)
        .setDeposit(deposit)
        .setWithdrawal(withdrawal)
        .setAccountHistory(accountHistory)
        .setFiatAccountHistory(fiatAccountHistory)
        .setCoinAccountHistory(coinAccountHistory)
        .setAmmPool(ammPool)
        .setMerchantEscrow(merchantEscrow);

    // Verify same instance returns (method chaining)
    assertSame(emptyResult, result);

    // Verify all fields were set correctly
    assertTrue(result.getAccount().isPresent());
    assertTrue(result.getFiatAccount().isPresent());
    assertTrue(result.getCoinAccount().isPresent());
    assertTrue(result.getDeposit().isPresent());
    assertTrue(result.getWithdrawal().isPresent());
    assertTrue(result.getAccountHistory().isPresent());
    assertTrue(result.getFiatAccountHistory().isPresent());
    assertTrue(result.getCoinAccountHistory().isPresent());
    assertTrue(result.getAmmPool().isPresent());
    assertTrue(result.getMerchantEscrow().isPresent());

    assertEquals(account, result.getAccount().get());
    assertEquals(fiatAccount, result.getFiatAccount().get());
    assertEquals(coinAccount, result.getCoinAccount().get());
    assertEquals(deposit, result.getDeposit().get());
    assertEquals(withdrawal, result.getWithdrawal().get());
    assertEquals(accountHistory, result.getAccountHistory().get());
    assertEquals(fiatAccountHistory, result.getFiatAccountHistory().get());
    assertEquals(coinAccountHistory, result.getCoinAccountHistory().get());
    assertEquals(ammPool, result.getAmmPool().get());
    assertEquals(merchantEscrow, result.getMerchantEscrow().get());
  }

  @Test
  @DisplayName("toAmmPoolObjectMessageJson should convert correctly")
  void toAmmPoolObjectMessageJson_ShouldConvertCorrectly() {
    // Set up a result with AmmPool
    ProcessResult result = emptyResult.setAmmPool(ammPool);

    // Get the message JSON
    Map<String, Object> messageJson = result.toAmmPoolObjectMessageJson();

    // Verify structure and content
    assertNotNull(messageJson);
    assertTrue(messageJson.containsKey("object"));

    // The object key should contain the AmmPool's message JSON
    Object objectJson = messageJson.get("object");
    assertNotNull(objectJson);
    assertTrue(objectJson instanceof Map);

    // Basic checks on the content
    @SuppressWarnings("unchecked")
    Map<String, Object> ammPoolJson = (Map<String, Object>) objectJson;

    // Check some properties from AmmPool's toMessageJson
    assertNotNull(ammPoolJson.get("pair"));
    assertEquals(ammPool.getPair(), ammPoolJson.get("pair"));
  }

  @Test
  @DisplayName("toMerchantEscrowObjectMessageJson should convert correctly")
  void toMerchantEscrowObjectMessageJson_ShouldConvertCorrectly() {
    // Set up a result with MerchantEscrow
    ProcessResult result = emptyResult.setMerchantEscrow(merchantEscrow);

    // Get the message JSON
    Map<String, Object> messageJson = result.toMerchantEscrowObjectMessageJson();

    // Verify structure and content
    assertNotNull(messageJson);
    assertTrue(messageJson.containsKey("object"));

    // The object key should contain the MerchantEscrow's message JSON
    Object objectJson = messageJson.get("object");
    assertNotNull(objectJson);
    assertTrue(objectJson instanceof Map);

    // Basic checks on the content
    @SuppressWarnings("unchecked")
    Map<String, Object> merchantEscrowJson = (Map<String, Object>) objectJson;

    // Check some properties from MerchantEscrow's toMessageJson
    assertNotNull(merchantEscrowJson.get("identifier"));
    assertEquals(merchantEscrow.getIdentifier(), merchantEscrowJson.get("identifier"));
  }

  @Test
  @DisplayName("Setting individual fields should work correctly")
  void settingIndividualFields_ShouldWorkCorrectly() {
    // Test setting account
    ProcessResult accountResult = emptyResult.setAccount(account);
    assertTrue(accountResult.getAccount().isPresent());
    assertSame(account, accountResult.getAccount().get());

    // Test setting fiatAccount
    ProcessResult fiatAccountResult = emptyResult.setFiatAccount(fiatAccount);
    assertTrue(fiatAccountResult.getFiatAccount().isPresent());
    assertSame(fiatAccount, fiatAccountResult.getFiatAccount().get());

    // Test setting coinAccount
    ProcessResult coinAccountResult = emptyResult.setCoinAccount(coinAccount);
    assertTrue(coinAccountResult.getCoinAccount().isPresent());
    assertSame(coinAccount, coinAccountResult.getCoinAccount().get());

    // Test setting accountHistory
    ProcessResult accountHistoryResult = emptyResult.setAccountHistory(accountHistory);
    assertTrue(accountHistoryResult.getAccountHistory().isPresent());
    assertSame(accountHistory, accountHistoryResult.getAccountHistory().get());

    // Test setting fiatAccountHistory
    ProcessResult fiatAccountHistoryResult = emptyResult.setFiatAccountHistory(fiatAccountHistory);
    assertTrue(fiatAccountHistoryResult.getFiatAccountHistory().isPresent());
    assertSame(fiatAccountHistory, fiatAccountHistoryResult.getFiatAccountHistory().get());

    // Test setting coinAccountHistory
    ProcessResult coinAccountHistoryResult = emptyResult.setCoinAccountHistory(coinAccountHistory);
    assertTrue(coinAccountHistoryResult.getCoinAccountHistory().isPresent());
    assertSame(coinAccountHistory, coinAccountHistoryResult.getCoinAccountHistory().get());

    // Test setting deposit
    ProcessResult depositResult = emptyResult.setDeposit(deposit);
    assertTrue(depositResult.getDeposit().isPresent());
    assertSame(deposit, depositResult.getDeposit().get());

    // Test setting withdrawal
    ProcessResult withdrawalResult = emptyResult.setWithdrawal(withdrawal);
    assertTrue(withdrawalResult.getWithdrawal().isPresent());
    assertSame(withdrawal, withdrawalResult.getWithdrawal().get());

    // Test setting ammPool
    ProcessResult ammPoolResult = emptyResult.setAmmPool(ammPool);
    assertTrue(ammPoolResult.getAmmPool().isPresent());
    assertSame(ammPool, ammPoolResult.getAmmPool().get());

    // Test setting merchantEscrow
    ProcessResult merchantEscrowResult = emptyResult.setMerchantEscrow(merchantEscrow);
    assertTrue(merchantEscrowResult.getMerchantEscrow().isPresent());
    assertSame(merchantEscrow, merchantEscrowResult.getMerchantEscrow().get());
  }

  @Test
  @DisplayName("addAccount should add accounts to the map correctly")
  void addAccount_ShouldAddAccountsToMap() {
    // Setup
    Account account1 = AccountFactory.create("account1");
    Account account2 = AccountFactory.create("account2");

    // Verify initial state
    assertTrue(emptyResult.getAccounts().isEmpty());

    // Action: Add first account
    emptyResult.addAccount(account1);

    // Verify one account added correctly
    assertEquals(1, emptyResult.getAccounts().size());
    assertTrue(emptyResult.getAccount("account1").isPresent());
    assertEquals(account1, emptyResult.getAccount("account1").get());

    // Action: Add second account
    emptyResult.addAccount(account2);

    // Verify second account added correctly
    assertEquals(2, emptyResult.getAccounts().size());
    assertTrue(emptyResult.getAccount("account2").isPresent());
    assertEquals(account2, emptyResult.getAccount("account2").get());

    // Verify method chaining works
    ProcessResult result = emptyResult.addAccount(AccountFactory.create("account3"));
    assertSame(emptyResult, result);
    assertEquals(3, emptyResult.getAccounts().size());
  }

  @Test
  @DisplayName("addAccountHistory should add histories to the list correctly")
  void addAccountHistory_ShouldAddHistoriesToList() {
    // Setup
    AccountHistory history1 = AccountHistoryFactory.create("account1", "tx1", "DEPOSIT");
    AccountHistory history2 = AccountHistoryFactory.create("account2", "tx2", "WITHDRAWAL");

    // Verify initial state
    assertTrue(emptyResult.getAccountHistories().isEmpty());

    // Action: Add first history
    emptyResult.addAccountHistory(history1);

    // Verify one history added correctly
    assertEquals(1, emptyResult.getAccountHistories().size());
    assertTrue(emptyResult.getAccountHistories().contains(history1));

    // Action: Add second history
    emptyResult.addAccountHistory(history2);

    // Verify second history added correctly
    assertEquals(2, emptyResult.getAccountHistories().size());
    assertTrue(emptyResult.getAccountHistories().contains(history2));

    // Verify method chaining works
    ProcessResult result = emptyResult.addAccountHistory(
        AccountHistoryFactory.create("account3", "tx3", "DEPOSIT"));
    assertSame(emptyResult, result);
    assertEquals(3, emptyResult.getAccountHistories().size());
  }

  @Test
  @DisplayName("setAccount and addAccount should work independently")
  void setAccountAndAddAccount_ShouldWorkIndependently() {
    // Setup
    Account account1 = AccountFactory.create("account1");
    Account account2 = AccountFactory.create("account2");

    // Action: Set account using setAccount
    emptyResult.setAccount(account1);

    // Verify account is set but not in accounts map
    assertTrue(emptyResult.getAccount().isPresent());
    assertEquals(account1, emptyResult.getAccount().get());
    assertTrue(emptyResult.getAccounts().isEmpty()); // Accounts map should remain empty

    // Action: Add account using addAccount
    emptyResult.addAccount(account2);

    // Verify account is added to map but primary account is unchanged
    assertEquals(1, emptyResult.getAccounts().size());
    assertTrue(emptyResult.getAccount("account2").isPresent());
    assertEquals(account1, emptyResult.getAccount().get()); // Primary account still account1
  }

  @Test
  @DisplayName("toAmmPositionObjectMessageJson should include event and position data")
  void toAmmPositionObjectMessageJson_ShouldIncludeEventAndPositionData() {
    // Arrange
    ProcessResult result = ProcessResultFactory.createEmptyResult();
    AmmPosition ammPosition = AmmPositionFactory.createDefaultAmmPosition();
    ammPosition.setIdentifier("test-position-id");
    result.setAmmPosition(ammPosition);

    // Act
    Map<String, Object> json = result.toAmmPositionObjectMessageJson();

    // Assert
    assertNotNull(json);
    assertTrue(json.containsKey("eventId"));
    assertTrue(json.containsKey("isSuccess"));
    assertTrue(json.containsKey("timestamp"));
    assertTrue(json.containsKey("object"));

    // Check that object contains position data
    @SuppressWarnings("unchecked")
    Map<String, Object> objectJson = (Map<String, Object>) json.get("object");
    assertNotNull(objectJson);
    assertEquals("test-position-id", objectJson.get("identifier"));
  }

  @Test
  @DisplayName("toAmmPoolObjectMessageJson should include event and pool data")
  void toAmmPoolObjectMessageJson_ShouldIncludeEventAndPoolData() {
    // Arrange
    ProcessResult result = ProcessResultFactory.createEmptyResult();
    AmmPool ammPool = AmmPoolFactory.createDefaultAmmPool();
    ammPool.setPair("test-pool-id");
    result.setAmmPool(ammPool);

    // Act
    Map<String, Object> json = result.toAmmPoolObjectMessageJson();

    // Assert
    assertNotNull(json);
    assertTrue(json.containsKey("eventId"));
    assertTrue(json.containsKey("isSuccess"));
    assertTrue(json.containsKey("timestamp"));
    assertTrue(json.containsKey("object"));

    // Check that object contains pool data
    @SuppressWarnings("unchecked")
    Map<String, Object> objectJson = (Map<String, Object>) json.get("object");
    assertNotNull(objectJson);
    assertEquals("test-pool-id", objectJson.get("pair"));
  }

  @Test
  @DisplayName("toAmmOrderObjectMessageJson should include event and order data")
  void toAmmOrderObjectMessageJson_ShouldIncludeEventAndOrderData() {
    // Arrange
    DisruptorEvent mockEvent = Mockito.mock(DisruptorEvent.class);
    Map<String, Object> mockMessageJson = new HashMap<>();
    mockMessageJson.put("eventId", "test-event-id");
    mockMessageJson.put("isSuccess", true);
    mockMessageJson.put("timestamp", 123456789L);
    Mockito.when(mockEvent.toDisruptorMessageJson()).thenReturn(mockMessageJson);

    ProcessResult result = new ProcessResult(mockEvent);
    AmmOrder ammOrder = AmmOrderFactory.create();
    ammOrder.setIdentifier("test-order-id");
    result.setAmmOrder(ammOrder);

    // Act
    Map<String, Object> json = result.toAmmOrderObjectMessageJson();

    // Assert
    assertNotNull(json);
    assertTrue(json.containsKey("eventId"));
    assertTrue(json.containsKey("isSuccess"));
    assertTrue(json.containsKey("timestamp"));
    assertTrue(json.containsKey("object"));

    // Check that object contains order data
    @SuppressWarnings("unchecked")
    Map<String, Object> objectJson = (Map<String, Object>) json.get("object");
    assertNotNull(objectJson);
    assertEquals("test-order-id", objectJson.get("identifier"));
  }

  @Test
  @DisplayName("toOfferObjectMessageJson should include event and offer data")
  void toOfferObjectMessageJson_ShouldIncludeEventAndOfferData() {
    // Arrange
    ProcessResult result = ProcessResultFactory.createEmptyResult();
    Offer offer = OfferFactory.create();
    offer.setIdentifier("test-offer-id");
    result.setOffer(offer);

    // Act
    Map<String, Object> json = result.toOfferObjectMessageJson();

    // Assert
    assertNotNull(json);
    assertTrue(json.containsKey("eventId"));
    assertTrue(json.containsKey("isSuccess"));
    assertTrue(json.containsKey("timestamp"));
    assertTrue(json.containsKey("object"));

    // Check that object contains offer data
    @SuppressWarnings("unchecked")
    Map<String, Object> objectJson = (Map<String, Object>) json.get("object");
    assertNotNull(objectJson);
    assertEquals("test-offer-id", objectJson.get("identifier"));
  }

  @Test
  @DisplayName("toTradeObjectMessageJson should include event and trade data")
  void toTradeObjectMessageJson_ShouldIncludeEventAndTradeData() {
    // Arrange
    ProcessResult result = ProcessResultFactory.createEmptyResult();
    Trade trade = TradeFactory.create();
    trade.setIdentifier("test-trade-id");
    result.setTrade(trade);

    // Act
    Map<String, Object> json = result.toTradeObjectMessageJson();

    // Assert
    assertNotNull(json);
    assertTrue(json.containsKey("eventId"));
    assertTrue(json.containsKey("isSuccess"));
    assertTrue(json.containsKey("timestamp"));
    assertTrue(json.containsKey("object"));

    // Check that object contains trade data
    @SuppressWarnings("unchecked")
    Map<String, Object> objectJson = (Map<String, Object>) json.get("object");
    assertNotNull(objectJson);
    assertEquals("test-trade-id", objectJson.get("identifier"));
  }

  @Test
  @DisplayName("addTick should add ticks to the list correctly")
  void addTick_ShouldAddTicksToList() {
    // Setup
    Tick tick1 = new TickFactory().createTick("BTC-USDT", 1000);
    Tick tick2 = new TickFactory().createTick("ETH-USDT", 2000);

    // Verify initial state
    assertTrue(emptyResult.getTicks().isEmpty());

    // Action: Add first tick
    emptyResult.addTick(tick1);

    // Verify one tick added correctly
    assertEquals(1, emptyResult.getTicks().size());
    assertTrue(emptyResult.getTicks().contains(tick1));

    // Action: Add second tick
    emptyResult.addTick(tick2);

    // Verify second tick added correctly
    assertEquals(2, emptyResult.getTicks().size());
    assertTrue(emptyResult.getTicks().contains(tick2));

    // Verify method chaining works
    ProcessResult result = emptyResult.addTick(new TickFactory().createTick("XRP-USDT", 3000));
    assertSame(emptyResult, result);
    assertEquals(3, emptyResult.getTicks().size());
  }

  @Test
  @DisplayName("getTicks should return the list of added ticks")
  void getTicks_ShouldReturnListOfAddedTicks() {
    // Setup
    ProcessResult result = ProcessResultFactory.createEmptyResult();
    Tick tick = new TickFactory().createTick("BTC-USDT", 1000);
    result.addTick(tick);

    // Verify the tick is in the list
    List<Tick> ticks = result.getTicks();
    assertNotNull(ticks);
    assertEquals(1, ticks.size());
    assertSame(tick, ticks.get(0));

    // Verify tick properties
    Tick retrievedTick = ticks.get(0);
    assertEquals("BTC-USDT", retrievedTick.getPoolPair());
    assertEquals(1000, retrievedTick.getTickIndex());
  }

  @Test
  @DisplayName("setBalanceLock and getBalanceLock should work correctly")
  void setBalanceLockAndGetBalanceLock_ShouldWorkCorrectly() {
    // Verify init state - field should be empty
    assertTrue(emptyResult.getBalanceLock().isEmpty());
    
    // Set balance lock
    ProcessResult result = emptyResult.setBalanceLock(balanceLock);
    
    // Verify same instance returns (method chaining)
    assertSame(emptyResult, result);
    
    // Verify field was set correctly
    assertTrue(result.getBalanceLock().isPresent());
    assertEquals(balanceLock, result.getBalanceLock().get());
  }
  
  @Test
  @DisplayName("toBalanceLockObjectMessageJson should include the balance lock data")
  void toBalanceLockObjectMessageJson_ShouldIncludeBalanceLockData() {
    // Set up ProcessResult with BalanceLock
    ProcessResult result = emptyResult.setBalanceLock(balanceLock);
    
    // Set some event data
    event.setSuccess(true);
    event.setErrorMessage(null);
    
    // Convert to message JSON
    Map<String, Object> messageJson = result.toBalanceLockObjectMessageJson();
    
    // Verify the message JSON contains the expected data
    assertNotNull(messageJson);
    assertTrue((Boolean) messageJson.get("isSuccess"));
    assertNull(messageJson.get("errorMessage"));
    
    // Verify the object field exists and is a Map
    Object objectField = messageJson.get("object");
    assertNotNull(objectField);
    assertTrue(objectField instanceof Map);
    
    // Verify the object contains expected BalanceLock fields
    @SuppressWarnings("unchecked")
    Map<String, Object> objectMap = (Map<String, Object>) objectField;
    assertEquals(balanceLock.getLockId(), objectMap.get("lockId"));
    assertEquals(balanceLock.getStatus(), objectMap.get("status"));
  }

  @Test
  @DisplayName("toCoinWithdrawalObjectMessageJson should include the coin withdrawal data")
  void toCoinWithdrawalObjectMessageJson_ShouldIncludeCoinWithdrawalData() {
    // Given
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.create("btc:user123", "withdrawal-123", new BigDecimal("1.0"));
    emptyResult.setWithdrawal(withdrawal);

    // When
    Map<String, Object> messageJson = emptyResult.toCoinWithdrawalObjectMessageJson();

    // Then
    assertNotNull(messageJson);
    assertTrue(messageJson.containsKey("object"));
    assertNotNull(messageJson.get("object"));
    assertTrue(messageJson.containsKey("inputEventId"));
    assertTrue(messageJson.get("inputEventId").toString().contains(withdrawal.getIdentifier()));
  }
}
