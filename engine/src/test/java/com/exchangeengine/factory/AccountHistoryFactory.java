package com.exchangeengine.factory;

import com.exchangeengine.model.AccountHistory;
import com.exchangeengine.model.OperationType;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;

import static org.instancio.Select.field;

/**
 * Factory class for creating AccountHistory instances for tests
 */
public class AccountHistoryFactory {

  /**
   * Creates a model for AccountHistory with default values
   *
   * @param accountKey    The account key
   * @param identifier    Unique identifier for the history record
   * @param operationType The operation type for the history record
   * @return Model<AccountHistory> that can be used to create AccountHistory
   *         instances
   */
  public static Model<AccountHistory> model(String accountKey, String identifier, String operationType) {
    AccountHistory history = new AccountHistory(accountKey, identifier, operationType);
    history.setBalanceInitAccount(); // Initialize with zero balances

    return Instancio.of(AccountHistory.class)
        .set(field(AccountHistory::getKey), history.getKey())
        .set(field(AccountHistory::getAccountKey), accountKey)
        .set(field(AccountHistory::getIdentifier), identifier)
        .set(field(AccountHistory::getOperationType), operationType)
        .set(field(AccountHistory::getAvailableBalance), history.getAvailableBalance())
        .set(field(AccountHistory::getFrozenBalance), history.getFrozenBalance())
        .set(field(AccountHistory::getTimestamp), history.getTimestamp())
        .toModel();
  }

  /**
   * Creates an AccountHistory with default values
   *
   * @param accountKey    The account key
   * @param identifier    Unique identifier for the history record
   * @param operationType The operation type for the history record
   * @return An AccountHistory instance
   */
  public static AccountHistory create(String accountKey, String identifier, String operationType) {
    return Instancio.create(model(accountKey, identifier, operationType));
  }

  /**
   * Creates an AccountHistory for deposit operation
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the history record
   * @return An AccountHistory instance for deposit operation
   */
  public static AccountHistory createForDeposit(String accountKey, String identifier) {
    return create(accountKey, identifier, OperationType.COIN_DEPOSIT_CREATE.getValue());
  }

  /**
   * Creates an AccountHistory for deposit operation with balance changes
   *
   * @param accountKey    The account key
   * @param identifier    Unique identifier for the history record
   * @param prevAvailable Previous available balance
   * @param newAvailable  New available balance after deposit
   * @param prevFrozen    Previous frozen balance
   * @param newFrozen     New frozen balance after deposit
   * @return An AccountHistory instance for deposit operation with balance changes
   */
  public static AccountHistory createForDepositWithBalanceChanges(
      String accountKey, String identifier,
      BigDecimal prevAvailable, BigDecimal newAvailable,
      BigDecimal prevFrozen, BigDecimal newFrozen) {

    AccountHistory history = createForDeposit(accountKey, identifier);
    history.setBalanceValues(prevAvailable, newAvailable, prevFrozen, newFrozen);

    return history;
  }

  /**
   * Creates an AccountHistory for withdrawal operation
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the history record
   * @return An AccountHistory instance for withdrawal operation
   */
  public static AccountHistory createForWithdrawal(String accountKey, String identifier) {
    return create(accountKey, identifier, OperationType.COIN_WITHDRAWAL_CREATE.getValue());
  }

  /**
   * Creates an AccountHistory for withdrawal operation with balance changes
   *
   * @param accountKey    The account key
   * @param identifier    Unique identifier for the history record
   * @param prevAvailable Previous available balance
   * @param newAvailable  New available balance after withdrawal
   * @param prevFrozen    Previous frozen balance
   * @param newFrozen     New frozen balance after withdrawal
   * @return An AccountHistory instance for withdrawal operation with balance
   *         changes
   */
  public static AccountHistory createForWithdrawalWithBalanceChanges(
      String accountKey, String identifier,
      BigDecimal prevAvailable, BigDecimal newAvailable,
      BigDecimal prevFrozen, BigDecimal newFrozen) {

    AccountHistory history = createForWithdrawal(accountKey, identifier);
    history.setBalanceValues(prevAvailable, newAvailable, prevFrozen, newFrozen);

    return history;
  }
}
