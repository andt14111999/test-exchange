package com.exchangeengine.factory;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.BaseTransaction;
import org.instancio.Instancio;
import org.instancio.Model;

import java.time.Instant;

import static org.instancio.Select.field;

/**
 * Factory class for creating BaseTransaction instances for tests
 * Since BaseTransaction is abstract, we use a concrete implementation for
 * testing
 */
public class BaseTransactionFactory {

  /**
   * Creates a model for a concrete implementation of BaseTransaction with default
   * values
   *
   * @param actionId   The action ID
   * @param identifier Unique identifier for the transaction
   * @param accountKey The account key
   * @return Model<BaseTransaction> that can be used to create transaction
   *         instances
   */
  public static Model<TestBaseTransaction> model(String actionId, String identifier, String accountKey) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), actionId)
        .set(field(BaseTransaction::getIdentifier), identifier)
        .set(field(BaseTransaction::getAccountKey), accountKey)
        .set(field(BaseTransaction::getStatus), "pending")
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(BaseTransaction::getCreatedAt), Instant.now().toEpochMilli())
        .set(field(BaseTransaction::getUpdatedAt), Instant.now().toEpochMilli())
        .toModel();
  }

  /**
   * Creates a BaseTransaction with default values
   *
   * @param actionId   The action ID
   * @param identifier Unique identifier for the transaction
   * @param accountKey The account key
   * @return A BaseTransaction instance
   */
  public static TestBaseTransaction create(String actionId, String identifier, String accountKey) {
    return Instancio.create(model(actionId, identifier, accountKey));
  }

  /**
   * Creates a transaction with null identifier
   */
  public static TestBaseTransaction createWithNullIdentifier(String actionId, String accountKey) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), actionId)
        .set(field(BaseTransaction::getIdentifier), null)
        .set(field(BaseTransaction::getAccountKey), accountKey)
        .set(field(BaseTransaction::getStatus), "pending")
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .create();
  }

  /**
   * Creates a transaction with empty identifier
   */
  public static TestBaseTransaction createWithEmptyIdentifier(String actionId, String accountKey) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), actionId)
        .set(field(BaseTransaction::getIdentifier), "  ")
        .set(field(BaseTransaction::getAccountKey), accountKey)
        .set(field(BaseTransaction::getStatus), "pending")
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .create();
  }

  /**
   * Creates a transaction with null action type
   */
  public static TestBaseTransaction createWithNullActionType(String actionId, String identifier, String accountKey) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), actionId)
        .set(field(BaseTransaction::getIdentifier), identifier)
        .set(field(BaseTransaction::getAccountKey), accountKey)
        .set(field(BaseTransaction::getStatus), "pending")
        .set(field(BaseTransaction::getActionType), null)
        .create();
  }

  /**
   * Creates a transaction with null action ID
   */
  public static TestBaseTransaction createWithNullActionId(String identifier, String accountKey) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), null)
        .set(field(BaseTransaction::getIdentifier), identifier)
        .set(field(BaseTransaction::getAccountKey), accountKey)
        .set(field(BaseTransaction::getStatus), "pending")
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .create();
  }

  /**
   * Creates a transaction with empty action ID
   */
  public static TestBaseTransaction createWithEmptyActionId(String identifier, String accountKey) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), "  ")
        .set(field(BaseTransaction::getIdentifier), identifier)
        .set(field(BaseTransaction::getAccountKey), accountKey)
        .set(field(BaseTransaction::getStatus), "pending")
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .create();
  }

  /**
   * Creates a transaction with null account key
   */
  public static TestBaseTransaction createWithNullAccountKey(String actionId, String identifier) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), actionId)
        .set(field(BaseTransaction::getIdentifier), identifier)
        .set(field(BaseTransaction::getAccountKey), null)
        .set(field(BaseTransaction::getStatus), "pending")
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .create();
  }

  /**
   * Creates a transaction with empty account key
   */
  public static TestBaseTransaction createWithEmptyAccountKey(String actionId, String identifier) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), actionId)
        .set(field(BaseTransaction::getIdentifier), identifier)
        .set(field(BaseTransaction::getAccountKey), "  ")
        .set(field(BaseTransaction::getStatus), "pending")
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .create();
  }

  /**
   * Creates a transaction with null status
   */
  public static TestBaseTransaction createWithNullStatus(String actionId, String identifier, String accountKey) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), actionId)
        .set(field(BaseTransaction::getIdentifier), identifier)
        .set(field(BaseTransaction::getAccountKey), accountKey)
        .set(field(BaseTransaction::getStatus), null)
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .create();
  }

  /**
   * Creates a transaction with empty status
   */
  public static TestBaseTransaction createWithEmptyStatus(String actionId, String identifier, String accountKey) {
    return Instancio.of(TestBaseTransaction.class)
        .set(field(BaseTransaction::getActionId), actionId)
        .set(field(BaseTransaction::getIdentifier), identifier)
        .set(field(BaseTransaction::getAccountKey), accountKey)
        .set(field(BaseTransaction::getStatus), "  ")
        .set(field(BaseTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .create();
  }

  /**
   * Concrete implementation of BaseTransaction for testing
   */
  public static class TestBaseTransaction extends BaseTransaction {
    // No additional implementation needed, just a concrete class for testing
  }
}
