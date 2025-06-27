package com.exchangeengine.factory;

import com.exchangeengine.model.Account;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import static org.instancio.Select.field;

/**
 * Factory class for creating Account instances for tests
 */
public class AccountFactory {

  private static final int DEFAULT_SCALE = 16;

  /**
   * Creates a model for Account with default values
   *
   * @param accountKey The key for the account
   * @return Model<Account> that can be used to create Account instances
   */
  public static Model<Account> model(String accountKey) {
    return Instancio.of(Account.class)
        .set(field(Account::getKey), accountKey)
        .set(field(Account::getAvailableBalance), new BigDecimal("10.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(Account::getFrozenBalance), new BigDecimal("1.0").setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(Account::getCreatedAt), Instant.now().toEpochMilli())
        .set(field(Account::getUpdatedAt), Instant.now().toEpochMilli())
        .toModel();
  }

  /**
   * Creates an Account with default values
   *
   * @param accountKey The key for the account
   * @return An Account instance
   */
  public static Account create(String accountKey) {
    return Instancio.create(model(accountKey));
  }

  /**
   * Creates an Account with custom available and frozen balances
   *
   * @param accountKey       The key for the account
   * @param availableBalance The available balance
   * @param frozenBalance    The frozen balance
   * @return An Account instance
   */
  public static Account createWithBalances(String accountKey, BigDecimal availableBalance, BigDecimal frozenBalance) {
    return Instancio.of(Account.class)
        .set(field(Account::getKey), accountKey)
        .set(field(Account::getAvailableBalance), availableBalance.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(Account::getFrozenBalance), frozenBalance.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP))
        .set(field(Account::getCreatedAt), Instant.now().toEpochMilli())
        .set(field(Account::getUpdatedAt), Instant.now().toEpochMilli())
        .create();
  }
}
