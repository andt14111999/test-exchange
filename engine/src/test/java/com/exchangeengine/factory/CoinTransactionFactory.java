package com.exchangeengine.factory;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.CoinTransaction;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import static org.instancio.Select.field;

/**
 * Factory class for creating CoinTransaction instances for tests
 * Since CoinTransaction is abstract, we use a concrete implementation for
 * testing
 */
public class CoinTransactionFactory {

  private static final int DEFAULT_SCALE = 16;

  /**
   * Creates a model for CoinTransaction with default values
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the transaction
   * @param amount     The transaction amount
   * @return Model<CoinTransaction> that can be used to create CoinTransaction
   *         instances
   */
  public static Model<TestCoinTransaction> model(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(TestCoinTransaction.class)
        .set(field(CoinTransaction::getAccountKey), accountKey)
        .set(field(CoinTransaction::getIdentifier), identifier)
        .set(field(CoinTransaction::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinTransaction::getStatus), "pending")
        .set(field(CoinTransaction::getCoin), "USDT")
        .set(field(CoinTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinTransaction::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinTransaction::getTxHash), "0x" + System.currentTimeMillis())
        .set(field(CoinTransaction::getLayer), "L1")
        .set(field(CoinTransaction::getCreatedAt), Instant.now().toEpochMilli())
        .set(field(CoinTransaction::getUpdatedAt), Instant.now().toEpochMilli())
        .toModel();
  }

  /**
   * Creates a CoinTransaction with default values
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the transaction
   * @param amount     The transaction amount
   * @return A CoinTransaction instance
   */
  public static TestCoinTransaction create(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.create(model(accountKey, identifier, amount));
  }

  /**
   * Creates a CoinTransaction with null amount
   */
  public static TestCoinTransaction createWithNullAmount(String accountKey, String identifier) {
    return Instancio.of(TestCoinTransaction.class)
        .set(field(CoinTransaction::getAccountKey), accountKey)
        .set(field(CoinTransaction::getIdentifier), identifier)
        .set(field(CoinTransaction::getAmount), null)
        .set(field(CoinTransaction::getStatus), "pending")
        .set(field(CoinTransaction::getCoin), "USDT")
        .set(field(CoinTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinTransaction::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinTransaction::getTxHash), "0x" + System.currentTimeMillis())
        .set(field(CoinTransaction::getLayer), "L1")
        .create();
  }

  /**
   * Creates a CoinTransaction with zero or negative amount
   */
  public static TestCoinTransaction createWithNonPositiveAmount(String accountKey, String identifier,
      BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) > 0) {
      throw new IllegalArgumentException("Amount must be zero or negative for this test factory method");
    }
    return Instancio.of(TestCoinTransaction.class)
        .set(field(CoinTransaction::getAccountKey), accountKey)
        .set(field(CoinTransaction::getIdentifier), identifier)
        .set(field(CoinTransaction::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinTransaction::getStatus), "pending")
        .set(field(CoinTransaction::getCoin), "USDT")
        .set(field(CoinTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinTransaction::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinTransaction::getTxHash), "0x" + System.currentTimeMillis())
        .set(field(CoinTransaction::getLayer), "L1")
        .create();
  }

  /**
   * Creates a CoinTransaction with null coin
   */
  public static TestCoinTransaction createWithNullCoin(String accountKey, String identifier, BigDecimal amount) {
    TestCoinTransaction transaction = new TestCoinTransaction();
    transaction.setAccountKey(accountKey);
    transaction.setIdentifier(identifier);
    transaction.setAmount(amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP));
    transaction.setStatus("pending");
    transaction.setActionId("act-" + System.currentTimeMillis());
    transaction.setTxHash("0x" + System.currentTimeMillis());
    transaction.setLayer("L1");
    // Không gọi setCoin để coin vẫn là null
    return transaction;
  }

  /**
   * Creates a CoinTransaction with empty coin
   */
  public static TestCoinTransaction createWithEmptyCoin(String accountKey, String identifier, BigDecimal amount) {
    TestCoinTransaction transaction = new TestCoinTransaction();
    transaction.setAccountKey(accountKey);
    transaction.setIdentifier(identifier);
    transaction.setAmount(amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP));
    transaction.setStatus("pending");
    transaction.setActionId("act-" + System.currentTimeMillis());
    transaction.setTxHash("0x" + System.currentTimeMillis());
    transaction.setLayer("L1");
    // Thiết lập trực tiếp vào field để tránh xử lý trong setCoin
    try {
      java.lang.reflect.Field field = CoinTransaction.class.getDeclaredField("coin");
      field.setAccessible(true);
      field.set(transaction, "");
    } catch (Exception e) {
      throw new RuntimeException("Failed to set empty coin", e);
    }
    return transaction;
  }

  /**
   * Creates a CoinTransaction with invalid coin type
   */
  public static TestCoinTransaction createWithInvalidCoin(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(TestCoinTransaction.class)
        .set(field(CoinTransaction::getAccountKey), accountKey)
        .set(field(CoinTransaction::getIdentifier), identifier)
        .set(field(CoinTransaction::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinTransaction::getStatus), "pending")
        .set(field(CoinTransaction::getCoin), "INVALID_COIN")
        .set(field(CoinTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinTransaction::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinTransaction::getTxHash), "0x" + System.currentTimeMillis())
        .set(field(CoinTransaction::getLayer), "L1")
        .create();
  }

  /**
   * Creates a CoinTransaction with null txHash
   */
  public static TestCoinTransaction createWithNullTxHash(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(TestCoinTransaction.class)
        .set(field(CoinTransaction::getAccountKey), accountKey)
        .set(field(CoinTransaction::getIdentifier), identifier)
        .set(field(CoinTransaction::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinTransaction::getStatus), "pending")
        .set(field(CoinTransaction::getCoin), "USDT")
        .set(field(CoinTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinTransaction::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinTransaction::getTxHash), null)
        .set(field(CoinTransaction::getLayer), "L1")
        .create();
  }

  /**
   * Creates a CoinTransaction with empty txHash
   */
  public static TestCoinTransaction createWithEmptyTxHash(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(TestCoinTransaction.class)
        .set(field(CoinTransaction::getAccountKey), accountKey)
        .set(field(CoinTransaction::getIdentifier), identifier)
        .set(field(CoinTransaction::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinTransaction::getStatus), "pending")
        .set(field(CoinTransaction::getCoin), "USDT")
        .set(field(CoinTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinTransaction::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinTransaction::getTxHash), "  ")
        .set(field(CoinTransaction::getLayer), "L1")
        .create();
  }

  /**
   * Creates a CoinTransaction with null layer
   */
  public static TestCoinTransaction createWithNullLayer(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(TestCoinTransaction.class)
        .set(field(CoinTransaction::getAccountKey), accountKey)
        .set(field(CoinTransaction::getIdentifier), identifier)
        .set(field(CoinTransaction::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinTransaction::getStatus), "pending")
        .set(field(CoinTransaction::getCoin), "USDT")
        .set(field(CoinTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinTransaction::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinTransaction::getTxHash), "0x" + System.currentTimeMillis())
        .set(field(CoinTransaction::getLayer), null)
        .create();
  }

  /**
   * Creates a CoinTransaction with empty layer
   */
  public static TestCoinTransaction createWithEmptyLayer(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(TestCoinTransaction.class)
        .set(field(CoinTransaction::getAccountKey), accountKey)
        .set(field(CoinTransaction::getIdentifier), identifier)
        .set(field(CoinTransaction::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinTransaction::getStatus), "pending")
        .set(field(CoinTransaction::getCoin), "USDT")
        .set(field(CoinTransaction::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinTransaction::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinTransaction::getTxHash), "0x" + System.currentTimeMillis())
        .set(field(CoinTransaction::getLayer), "  ")
        .create();
  }

  // Concrete implementation of CoinTransaction for testing
  public static class TestCoinTransaction extends CoinTransaction {
    @Override
    public String toString() {
      return "TestCoinTransaction{" +
          "amount=" + getAmount() +
          ", coin='" + getCoin() + '\'' +
          ", txHash='" + getTxHash() + '\'' +
          ", layer='" + getLayer() + '\'' +
          '}';
    }
  }
}
