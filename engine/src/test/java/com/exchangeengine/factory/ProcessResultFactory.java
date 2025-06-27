package com.exchangeengine.factory;

import com.exchangeengine.model.*;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.factory.event.DisruptorEventFactory;

import java.math.BigDecimal;

/**
 * Factory để tạo ProcessResult cho testing
 */
public class ProcessResultFactory {

  /**
   * Tạo một ProcessResult trống chỉ với DisruptorEvent
   *
   * @return ProcessResult instance
   */
  public static ProcessResult createEmptyResult() {
    DisruptorEvent event = DisruptorEventFactory.withAccountEvent();
    return new ProcessResult(event);
  }

  /**
   * Tạo một ProcessResult đầy đủ các thành phần
   *
   * @return ProcessResult đầy đủ
   */
  public static ProcessResult createFullResult() {
    // Sử dụng DisruptorEventFactory để tạo event
    DisruptorEvent event = DisruptorEventFactory.withAccountEvent();
    AccountEvent accountEvent = event.getAccountEvent();

    String accountKey = accountEvent.getAccountKey();
    String eventId = accountEvent.getEventId();

    // Sử dụng factory có sẵn cho các thành phần
    Account account = AccountFactory.create(accountKey);
    Account fiatAccount = AccountFactory.create(accountKey + "_fiat");
    Account coinAccount = AccountFactory.create(accountKey + "_coin");
    CoinDeposit deposit = CoinDepositFactory.create(accountKey, eventId, new BigDecimal("1.0"));
    CoinWithdrawal withdrawal = CoinWithdrawalFactory.create(accountKey, eventId, new BigDecimal("1.0"));
    AccountHistory accountHistory = AccountHistoryFactory.create(
        accountKey,
        eventId,
        OperationType.COIN_DEPOSIT_CREATE.getValue());
    AccountHistory fiatAccountHistory = AccountHistoryFactory.create(
        accountKey + "_fiat",
        eventId,
        OperationType.COIN_DEPOSIT_CREATE.getValue());
    AccountHistory coinAccountHistory = AccountHistoryFactory.create(
        accountKey + "_coin",
        eventId,
        OperationType.COIN_DEPOSIT_CREATE.getValue());
    AmmPool ammPool = AmmPoolFactory.createDefaultAmmPool();
    MerchantEscrow merchantEscrow = MerchantEscrowFactory.createDefault();
    AmmPosition ammPosition = AmmPositionFactory.createDefaultAmmPosition();

    // Tạo ProcessResult với các thành phần đã tạo, sử dụng setters thay vì
    // constructor đầy đủ
    ProcessResult result = new ProcessResult(event);
    result.setAccount(account)
        .setFiatAccount(fiatAccount)
        .setCoinAccount(coinAccount)
        .setDeposit(deposit)
        .setWithdrawal(withdrawal)
        .setAccountHistory(accountHistory)
        .setFiatAccountHistory(fiatAccountHistory)
        .setCoinAccountHistory(coinAccountHistory)
        .setAmmPool(ammPool)
        .setMerchantEscrow(merchantEscrow)
        .setAmmPosition(ammPosition);

    // Thêm account vào accounts map
    result.addAccount(account);
    result.addAccount(fiatAccount);
    result.addAccount(coinAccount);

    // Thêm accountHistory vào list
    result.addAccountHistory(accountHistory);
    result.addAccountHistory(fiatAccountHistory);
    result.addAccountHistory(coinAccountHistory);

    return result;
  }

  /**
   * Tạo một ProcessResult với trạng thái lỗi
   *
   * @return ProcessResult lỗi
   */
  public static ProcessResult createErrorResult() {
    ProcessResult result = createFullResult();
    result.getEvent().setErrorMessage("Test error message");
    return result;
  }

  /**
   * Tạo một AmmPool instance
   *
   * @return AmmPool instance
   */
  public static AmmPool createAmmPool() {
    // Sử dụng đồng tiền được hỗ trợ trong Coin.java: USDT, VND, PHP, NGN
    AmmPool pool = AmmPoolFactory.createDefaultAmmPool();
    // Giữ nguyên cặp mặc định USDT/VND từ AmmPoolFactory
    return pool;
  }
}
