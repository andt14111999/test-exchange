package com.exchangeengine.factory;

import com.exchangeengine.model.ActionType;
import com.exchangeengine.model.CoinDeposit;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class for creating CoinDeposit instances for tests
 */
public class CoinDepositFactory {

  private static final int DEFAULT_SCALE = 16;

  /**
   * Creates a model for CoinDeposit with default values
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the deposit
   * @param amount     The deposit amount
   * @return Model<CoinDeposit> that can be used to create CoinDeposit instances
   */
  public static Model<CoinDeposit> model(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.of(CoinDeposit.class)
        .set(field(CoinDeposit::getAccountKey), accountKey)
        .set(field(CoinDeposit::getIdentifier), identifier)
        .set(field(CoinDeposit::getAmount), amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(CoinDeposit::getStatus), "pending")
        .set(field(CoinDeposit::getCoin), "USDT")
        .set(field(CoinDeposit::getActionType), ActionType.COIN_TRANSACTION)
        .set(field(CoinDeposit::getActionId), "act-" + System.currentTimeMillis())
        .set(field(CoinDeposit::getTxHash), "0x" + System.currentTimeMillis())
        .set(field(CoinDeposit::getLayer), "L1")
        .set(field(CoinDeposit::getDepositAddress), "deposit-address-" + System.currentTimeMillis())
        .set(field(CoinDeposit::getCreatedAt), Instant.now().toEpochMilli())
        .set(field(CoinDeposit::getUpdatedAt), Instant.now().toEpochMilli())
        .toModel();
  }

  /**
   * Creates a CoinDeposit with default values
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the deposit
   * @param amount     The deposit amount
   * @return A CoinDeposit instance
   */
  public static CoinDeposit create(String accountKey, String identifier, BigDecimal amount) {
    return Instancio.create(model(accountKey, identifier, amount));
  }

  /**
   * Creates a CoinDeposit with custom status
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the deposit
   * @param amount     The deposit amount
   * @param status     The status of the deposit
   * @return A CoinDeposit instance
   */
  public static CoinDeposit createWithStatus(String accountKey, String identifier, BigDecimal amount, String status) {
    CoinDeposit deposit = create(accountKey, identifier, amount);
    deposit.setStatus(status);
    return deposit;
  }

  /**
   * Creates a CoinDeposit with null deposit address
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the deposit
   * @param amount     The deposit amount
   * @return A CoinDeposit instance with null deposit address
   */
  public static CoinDeposit createWithNullDepositAddress(String accountKey, String identifier, BigDecimal amount) {
    CoinDeposit deposit = create(accountKey, identifier, amount);
    deposit.setDepositAddress(null);
    return deposit;
  }

  /**
   * Creates a CoinDeposit with empty deposit address
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the deposit
   * @param amount     The deposit amount
   * @return A CoinDeposit instance with empty deposit address
   */
  public static CoinDeposit createWithEmptyDepositAddress(String accountKey, String identifier, BigDecimal amount) {
    CoinDeposit deposit = create(accountKey, identifier, amount);
    deposit.setDepositAddress("  ");
    return deposit;
  }

  /**
   * Creates a CoinDeposit with a custom status that is neither pending nor
   * processed
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the deposit
   * @param amount     The deposit amount
   * @param status     A custom status that is neither pending nor processed
   * @return A CoinDeposit instance with a custom status
   */
  public static CoinDeposit createWithCustomStatus(String accountKey, String identifier, BigDecimal amount,
      String status) {
    if (status.equals("pending") || status.equals("processed")) {
      throw new IllegalArgumentException("Status should not be pending or processed for this factory method");
    }
    return createWithStatus(accountKey, identifier, amount, status);
  }

  /**
   * Creates a CoinDeposit with custom coin type
   *
   * @param accountKey The account key
   * @param identifier Unique identifier for the deposit
   * @param amount     The deposit amount
   * @param coin       The coin name for the deposit
   * @return A CoinDeposit instance
   */
  public static CoinDeposit createWithCoin(String accountKey, String identifier, BigDecimal amount, String coin) {
    CoinDeposit deposit = create(accountKey, identifier, amount);
    deposit.setCoin(coin);
    return deposit;
  }

  public static CoinDeposit createDefaultCoinDeposit() {
    String identifier = UUID.randomUUID().toString();
    String accountKey = "account-" + identifier;
    return create(accountKey, identifier, BigDecimal.ONE);
  }
}
