package com.exchangeengine.service.engine.withdrawal;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exchangeengine.model.Account;
import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.CoinWithdrawal;
import com.exchangeengine.model.event.CoinWithdrawalEvent;
import com.exchangeengine.model.event.DisruptorEvent;
import com.exchangeengine.model.OperationType;
import com.exchangeengine.model.ProcessResult;
import com.exchangeengine.storage.StorageService;

/**
 * Processor for withdrawal events
 */
public class WithdrawalProcessor {
  private static final Logger logger = LoggerFactory.getLogger(WithdrawalProcessor.class);
  private final StorageService storageService;
  private final DisruptorEvent event;

  /**
   * Constructor với DisruptorEvent.
   *
   * @param event Sự kiện cần xử lý
   */
  public WithdrawalProcessor(DisruptorEvent event) {
    this.storageService = StorageService.getInstance();
    this.event = event;
  }

  /**
   * Process withdrawal event
   *
   * @return ProcessResult chứa kết quả xử lý
   */
  public ProcessResult process() {
    ProcessResult result = new ProcessResult(event);
    CoinWithdrawalEvent coinWithdrawalEvent = event.getCoinWithdrawalEvent();
    CoinWithdrawal coinWithdrawal = coinWithdrawalEvent.toCoinWithdrawal(false);
    Optional<Account> coinAccount = storageService.getAccountCache().getAccount(coinWithdrawal.getAccountKey());

    try {
      if (!coinAccount.isPresent()) {
        throw new IllegalStateException("Account not found accountKey: " + coinWithdrawal.getAccountKey());
      }
      Account currentCoinAccount = coinAccount.get();
      logger.debug("current account: {}", currentCoinAccount);

      // Tạo AccountHistory ngay từ đầu
      AccountHistory accountHistory = new AccountHistory(
          currentCoinAccount.getKey(),
          coinWithdrawal.getIdentifier(),
          coinWithdrawalEvent.getOperationType().getValue());

      // Lưu giá trị ban đầu
      BigDecimal prevAvailableBalance = currentCoinAccount.getAvailableBalance();
      BigDecimal prevFrozenBalance = currentCoinAccount.getFrozenBalance();

      // Create recipient account history if recipient account key is present
      AccountHistory recipientAccountHistory = null;
      Account recipientAccount = null;
      BigDecimal recipientPrevAvailableBalance = null;
      BigDecimal recipientPrevFrozenBalance = null;

      // Check if there's a recipient account key and it's a releasing operation
      if (coinWithdrawal.hasRecipientAccount() && 
          OperationType.COIN_WITHDRAWAL_RELEASING.equals(coinWithdrawalEvent.getOperationType())) {
        Optional<Account> recipientAccountOpt = storageService.getAccountCache().getAccount(coinWithdrawal.getRecipientAccountKey());
        if (!recipientAccountOpt.isPresent()) {
          throw new IllegalStateException("Recipient account not found: " + coinWithdrawal.getRecipientAccountKey());
        }
        
        recipientAccount = recipientAccountOpt.get();
        recipientPrevAvailableBalance = recipientAccount.getAvailableBalance();
        recipientPrevFrozenBalance = recipientAccount.getFrozenBalance();
        
        // Create recipient account history
        recipientAccountHistory = new AccountHistory(
            recipientAccount.getKey(),
            coinWithdrawal.getIdentifier(),
            "recipient_" + coinWithdrawalEvent.getOperationType().getValue());
      }

      boolean isAccountUpdated = false;
      if (OperationType.COIN_WITHDRAWAL_CREATE.equals(coinWithdrawalEvent.getOperationType())) {
        isAccountUpdated = freezeBalance(currentCoinAccount, coinWithdrawal);
      } else if (OperationType.COIN_WITHDRAWAL_RELEASING.equals(coinWithdrawalEvent.getOperationType())) {
        isAccountUpdated = releaseBalanceSuccess(currentCoinAccount, recipientAccount, coinWithdrawal);
      } else if (OperationType.COIN_WITHDRAWAL_FAILED.equals(coinWithdrawalEvent.getOperationType())) {
        isAccountUpdated = transitionToFailed(coinWithdrawal);
      } else if (OperationType.COIN_WITHDRAWAL_CANCELLED.equals(coinWithdrawalEvent.getOperationType())) {
        isAccountUpdated = releaseBalanceCancelled(currentCoinAccount, coinWithdrawal);
      }

      // Chỉ cập nhật account khi có sự thay đổi
      event.successes();
      if (isAccountUpdated) {
        // Cập nhật giá trị mới cho AccountHistory
        accountHistory.setBalanceValues(
            prevAvailableBalance,
            currentCoinAccount.getAvailableBalance(),
            prevFrozenBalance,
            currentCoinAccount.getFrozenBalance());

        storageService.getAccountCache().updateAccount(currentCoinAccount);
        result.setAccount(currentCoinAccount).setAccountHistory(accountHistory);
        
        // Update recipient account if needed
        if (recipientAccount != null) {
          recipientAccountHistory.setBalanceValues(
              recipientPrevAvailableBalance,
              recipientAccount.getAvailableBalance(),
              recipientPrevFrozenBalance,
              recipientAccount.getFrozenBalance());
          
          storageService.getAccountCache().updateAccount(recipientAccount);
          result.setRecipientAccount(recipientAccount).setRecipientAccountHistory(recipientAccountHistory);
        }
      }

      logger.debug("Withdrawal processed successfully: {}\n{}\n{}", currentCoinAccount, coinWithdrawal, accountHistory);
    } catch (Exception e) {
      coinWithdrawal.setStatusExplanation(e.getMessage());
      event.setErrorMessage(e.getMessage());

      logger.error("Error processing withdrawal event: {}", e.getMessage(), e);
    } finally {
      storageService.getWithdrawalCache().updateCoinWithdrawal(coinWithdrawal);
      result.setWithdrawal(coinWithdrawal);
    }

    return result;
  }

  /**
   * Xử lý đóng băng số dư khi tạo lệnh rút tiền
   *
   * @param account    Tài khoản cần xử lý
   * @param withdrawal Thông tin giao dịch rút tiền
   * @return true nếu tài khoản đã được cập nhật
   */
  private boolean freezeBalance(Account account, CoinWithdrawal withdrawal) {
    withdrawal.transitionToProcessing();
    account.increaseFrozenBalance(withdrawal.getAmountWithFee(), withdrawal.getIdentifier());

    logger.debug("Freezed balance for account: {}", account);
    return true;
  }

  /**
   * Xử lý hoàn tất lệnh rút tiền thành công
   *
   * @param account          Tài khoản cần xử lý
   * @param recipientAccount Tài khoản người nhận (có thể null)
   * @param withdrawal       Thông tin giao dịch rút tiền
   * @return true nếu tài khoản đã được cập nhật
   */
  private boolean releaseBalanceSuccess(Account account, Account recipientAccount, CoinWithdrawal withdrawal) {
    withdrawal.transitionToCompleted();
    account.decreaseFrozenBalance(withdrawal.getAmountWithFee());
    
    // If recipient account is specified, increase its available balance by the withdrawal amount (without fee)
    logger.debug("recipientAccount: {}", recipientAccount);
    if (recipientAccount != null) {
      recipientAccount.increaseAvailableBalance(withdrawal.getAmount());
      logger.debug("Increased balance for recipient account: {}", recipientAccount);
    }

    logger.debug("Released frozen balance for successful account: {}", account);
    return true;
  }

  /**
   * Xử lý hoàn trả số dư khi lệnh rút tiền thất bại
   *
   * @param account    Tài khoản cần xử lý
   * @param withdrawal Thông tin giao dịch rút tiền
   * @return true nếu tài khoản đã được cập nhật
   */
  private boolean transitionToFailed(CoinWithdrawal withdrawal) {
    withdrawal.transitionToFailed();

    logger.debug("Transitioned to failed status for withdrawal: {}", withdrawal);
    return true;
  }

  private boolean releaseBalanceCancelled(Account account, CoinWithdrawal withdrawal) {
    withdrawal.transitionToCancelled();
    account.decreaseFrozenIncreaseAvailableBalance(withdrawal.getAmountWithFee());

    logger.debug("Released frozen balance and restored available balance for cancelled account: {}", account);
    return true;
  }
}
