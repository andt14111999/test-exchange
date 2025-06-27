package com.exchangeengine.factory;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.CoinWithdrawal;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class for creating CoinWithdrawal instances for tests
 */
public class CoinWithdrawalFactory {

  private static final int DEFAULT_SCALE = 16;

  /**
   * Creates a model for CoinWithdrawal with default values
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the withdrawal
   * @param amount     The withdrawal amount
   * @return Model<CoinWithdrawal> that can be used to create CoinWithdrawal
   *         instances
   */
  public static Model<CoinWithdrawal> model(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(CoinWithdrawal.class)
        .set(field(CoinWithdrawal::getAccountKey), accountKey)
        .set(field(CoinWithdrawal::getIdentifier), identifier)
        .set(field(CoinWithdrawal::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinWithdrawal::getStatus), "pending")
        .set(field(CoinWithdrawal::getCoin), "USDT")
        .set(field(CoinWithdrawal::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinWithdrawal::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinWithdrawal::getTxHash), "0x" + System.currentTimeMillis())
        .set(field(CoinWithdrawal::getLayer), "L1")
        .set(field(CoinWithdrawal::getDestinationAddress), "destination-address-" + System.currentTimeMillis())
        .set(field(CoinWithdrawal::getFee), new BigDecimal("0.0001").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinWithdrawal::getRecipientAccountKey), null)
        .set(field(CoinWithdrawal::getCreatedAt), Instant.now().toEpochMilli())
        .set(field(CoinWithdrawal::getUpdatedAt), Instant.now().toEpochMilli())
        .toModel();
  }

  /**
   * Creates a model for CoinWithdrawal with recipient account key
   *
   * @param accountKey         The account key
   * @param identifier         Unique identifier for the withdrawal
   * @param amount             The withdrawal amount
   * @param recipientAccountKey The recipient account key
   * @return Model<CoinWithdrawal> that can be used to create CoinWithdrawal
   *         instances with recipient account
   */
  public static Model<CoinWithdrawal> modelWithRecipient(String accountKey, String identifier, BigDecimal amount, String recipientAccountKey) {
    return Instancio.of(model(accountKey, identifier, amount))
        .set(field(CoinWithdrawal::getRecipientAccountKey), recipientAccountKey)
        .toModel();
  }

  /**
   * Creates a CoinWithdrawal with default values
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the withdrawal
   * @param amount     The withdrawal amount
   * @return A CoinWithdrawal instance
   */
  public static CoinWithdrawal create(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.create(model(accountKey, identifier, amount));
  }

  /**
   * Creates a CoinWithdrawal with recipient account key
   *
   * @param accountKey         The account key
   * @param identifier         Unique identifier for the withdrawal
   * @param amount             The withdrawal amount
   * @param recipientAccountKey The recipient account key
   * @return A CoinWithdrawal instance with recipient account
   */
  public static CoinWithdrawal createWithRecipient(String accountKey, String identifier, BigDecimal amount, String recipientAccountKey) {
    return Instancio.create(modelWithRecipient(accountKey, identifier, amount, recipientAccountKey));
  }

  /**
   * Creates a CoinWithdrawal with specified status
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the withdrawal
   * @param amount     The withdrawal amount
   * @param status     The status of the withdrawal (pending, processing,
   *                   completed, failed)
   * @return A CoinWithdrawal instance with the specified status
   */
  public static CoinWithdrawal createWithStatus(String accountKey, String identifier, BigDecimal amount, String status) {
    return Instancio.of(model(accountKey, identifier, amount))
        .set(field(CoinWithdrawal::getStatus), status)
        .create();
  }

  /**
   * Creates a CoinWithdrawal with specified status and recipient account key
   *
   * @param accountKey         The account key
   * @param identifier         Unique identifier for the withdrawal
   * @param amount             The withdrawal amount
   * @param status             The status of the withdrawal
   * @param recipientAccountKey The recipient account key
   * @return A CoinWithdrawal instance with the specified status and recipient account
   */
  public static CoinWithdrawal createWithStatusAndRecipient(String accountKey, String identifier, BigDecimal amount, 
                                                           String status, String recipientAccountKey) {
    return Instancio.of(modelWithRecipient(accountKey, identifier, amount, recipientAccountKey))
        .set(field(CoinWithdrawal::getStatus), status)
        .create();
  }

  /**
   * Creates a CoinWithdrawal with specified coin type
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the withdrawal
   * @param amount     The withdrawal amount
   * @param coin       The coin name
   * @return A CoinWithdrawal instance with the specified coin type
   */
  public static CoinWithdrawal createWithCoin(String accountKey, String identifier, BigDecimal amount,
      String coin) {
    return Instancio.of(model(accountKey, identifier, amount))
        .set(field(CoinWithdrawal::getCoin), coin)
        .create();
  }

  /**
   * Creates a CoinWithdrawal with null destination address
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the withdrawal
   * @param amount     The withdrawal amount
   * @return A CoinWithdrawal instance with null destination address
   */
  public static CoinWithdrawal createWithNullDestinationAddress(String accountKey, String identifier,
      BigDecimal amount) {
    return Instancio.of(model(accountKey, identifier, amount))
        .set(field(CoinWithdrawal::getDestinationAddress), null)
        .create();
  }

  /**
   * Creates a CoinWithdrawal with empty destination address
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the withdrawal
   * @param amount     The withdrawal amount
   * @return A CoinWithdrawal instance with empty destination address
   */
  public static CoinWithdrawal createWithEmptyDestinationAddress(String accountKey, String identifier,
      BigDecimal amount) {
    return Instancio.of(model(accountKey, identifier, amount))
        .set(field(CoinWithdrawal::getDestinationAddress), "")
        .create();
  }

  /**
   * Creates a CoinWithdrawal with null fee
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the withdrawal
   * @param amount     The withdrawal amount
   * @return A CoinWithdrawal instance with null fee
   */
  public static CoinWithdrawal createWithNullFee(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(model(accountKey, identifier, amount))
        .set(field(CoinWithdrawal::getFee), null)
        .create();
  }

  /**
   * Creates a CoinWithdrawal with negative fee
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the withdrawal
   * @param amount     The withdrawal amount
   * @return A CoinWithdrawal instance with negative fee
   */
  public static CoinWithdrawal createWithNegativeFee(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(model(accountKey, identifier, amount))
        .set(field(CoinWithdrawal::getFee), new BigDecimal("-0.1").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .create();
  }

  /**
   * Creates a CoinWithdrawal with default values
   *
   * @return A CoinWithdrawal instance with default values
   */
  public static CoinWithdrawal createDefaultCoinWithdrawal() {
    return create("test_account", UUID.randomUUID().toString(), new BigDecimal("1.0"));
  }

  /**
   * Creates a CoinWithdrawal with multiple invalid fields for testing validation
   *
   * @return A CoinWithdrawal instance with multiple invalid fields
   */
  public static CoinWithdrawal createInvalidWithdrawal() {
    return Instancio.of(CoinWithdrawal.class)
        .set(field(CoinWithdrawal::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinWithdrawal::getDestinationAddress), "")
        .set(field(CoinWithdrawal::getFee), new BigDecimal("-0.1"))
        .create();
  }
}
