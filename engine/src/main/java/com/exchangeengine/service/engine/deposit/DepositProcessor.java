package com.exchangeengine.service.engine.deposit;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.CoinDeposit;
import com.exchangeengine.model.event.CoinDepositEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.StorageService;

/**
 * Processor for deposit events
 */
public class DepositProcessor {
  private static final Logger logger = LoggerFactory.getLogger(DepositProcessor.class);
  private final StorageService storageService;
  private final DisruptorEvent event;

  /**
   * Constructor với DisruptorEvent.
   *
   * @param event Sự kiện cần xử lý
   */
  public DepositProcessor(DisruptorEvent event) {
    this.storageService = StorageService.getInstance();
    this.event = event;
  }

  /**
   * Process deposit event
   *
   * @return ProcessResult chứa kết quả xử lý
   */
  public ProcessResult process() {
    ProcessResult result = new ProcessResult(event);
    CoinDepositEvent coinDepositEvent = event.getCoinDepositEvent();
    CoinDeposit coinDeposit = coinDepositEvent.toCoinDeposit(false);
    Optional<Account> coinAccount = storageService.getAccountCache().getAccount(coinDeposit.getAccountKey());

    try {
      if (!coinAccount.isPresent()) {
        throw new IllegalStateException("Account not found accountKey: " + coinDeposit.getAccountKey());
      }

      Account currentCoinAccount = coinAccount.get();
      BigDecimal prevAvailableBalance = currentCoinAccount.getAvailableBalance();
      BigDecimal prevFrozenBalance = currentCoinAccount.getFrozenBalance();

      currentCoinAccount.increaseAvailableBalance(coinDeposit.getAmount());
      coinDeposit.transitionToProcessed();

      AccountHistory accountHistory = new AccountHistory(
          currentCoinAccount.getKey(),
          coinDeposit.getIdentifier(),
          coinDepositEvent.getOperationType().getValue());
      accountHistory.setBalanceValues(
          prevAvailableBalance,
          currentCoinAccount.getAvailableBalance(),
          prevFrozenBalance,
          currentCoinAccount.getFrozenBalance());

      storageService.getAccountCache().updateAccount(currentCoinAccount);
      event.successes();
      result.setAccount(currentCoinAccount).setDeposit(coinDeposit).setAccountHistory(accountHistory);

      logger.debug("Deposit processed successfully: {}\n{}", currentCoinAccount, accountHistory);
    } catch (Exception e) {
      coinDeposit.setStatusExplanation(e.getMessage());
      event.setErrorMessage(e.getMessage());
      result.setDeposit(coinDeposit);

      logger.error("Error processing deposit event: {}", e.getMessage(), e);
    } finally {
      storageService.getDepositCache().updateCoinDeposit(coinDeposit);
    }

    return result;
  }
}
