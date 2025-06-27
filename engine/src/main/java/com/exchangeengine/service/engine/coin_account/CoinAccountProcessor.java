package com.exchangeengine.service.engine.coin_account;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.event.AccountEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.cache.AccountCache;

/**
 * Processor for deposit events
 */
public class CoinAccountProcessor {
  private static final Logger logger = LoggerFactory.getLogger(CoinAccountProcessor.class);
  private final AccountCache accountCache;
  private final DisruptorEvent event;

  /**
   * Constructor với DisruptorEvent.
   *
   * @param event Sự kiện cần xử lý
   */
  public CoinAccountProcessor(DisruptorEvent event) {
    this.event = event;
    this.accountCache = AccountCache.getInstance();
  }

  /**
   * Process deposit event
   *
   * @return ProcessResult chứa kết quả xử lý
   */
  public ProcessResult process() {
    ProcessResult result = new ProcessResult(event);
    AccountEvent accountEvent = event.getAccountEvent();
    Optional<Account> coinAccount = accountCache.getAccount(accountEvent.getAccountKey());

    try {
      if (coinAccount.isPresent()) {
        event.successes();
        return ProcessResult.success(event)
            .setAccount(coinAccount.get());
      }

      Account newAccount = accountEvent.toAccount();
      AccountHistory accountHistory = new AccountHistory(newAccount.getKey(), "create_new_account",
          accountEvent.getOperationType().getValue());
      accountHistory.setBalanceInitAccount();

      accountCache.updateAccount(newAccount);
      event.successes();
      result.setAccount(newAccount).setAccountHistory(accountHistory);

      logger.debug("Coin account created successfully: {}\n{}", newAccount, accountHistory);
    } catch (Exception e) {
      event.setErrorMessage(e.getMessage());

      logger.error("Error processing coin account event: {}", e.getMessage(), e);
    }

    return result;
  }
}
