package com.exchangeengine.factory;

import com.exchangeengine.model.Coin;
import org.instancio.Instancio;
import org.instancio.Model;

import static org.instancio.Select.field;

/**
 * Factory class for creating Coin instances for tests
 */
public class CoinFactory {

  /**
   * Creates a model for Coin với tên và các thông số tương ứng
   *
   * @param name    Tên của coin
   * @param decimal Số thập phân
   * @return Model<Coin> that can be used to create Coin instances
   */
  private static Model<Coin> model(String name, int decimal) {
    return Instancio.of(Coin.class)
        .set(field(Coin::getName), name.toUpperCase())
        .set(field(Coin::getDecimal), decimal)
        .set(field(Coin::getErrorMessage), "")
        .toModel();
  }

  /**
   * Returns a USDT coin
   *
   * @return A Coin instance representing USDT
   */
  public static Coin usdt() {
    return Instancio.create(model("USDT", 6));
  }

  /**
   * Returns a VND coin
   *
   * @return A Coin instance representing VND
   */
  public static Coin vnd() {
    return Instancio.create(model("VND", 0));
  }

  /**
   * Returns a PHP coin
   *
   * @return A Coin instance representing PHP
   */
  public static Coin php() {
    return Instancio.create(model("PHP", 2));
  }

  /**
   * Returns a NGN coin
   *
   * @return A Coin instance representing NGN
   */
  public static Coin ngn() {
    return Instancio.create(model("NGN", 2));
  }
}
