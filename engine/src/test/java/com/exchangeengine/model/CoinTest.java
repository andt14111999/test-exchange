package com.exchangeengine.model;

import com.exchangeengine.factory.CoinFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoinTest {

  @Test
  @DisplayName("Factory should create Coin with correct values")
  void factory_ShouldCreateCoinWithCorrectValues() {
    Coin usdtCoin = CoinFactory.usdt();

    assertEquals("USDT", usdtCoin.getName());
    assertEquals(6, usdtCoin.getDecimal());
    assertTrue(usdtCoin.isValid());
    assertEquals("", usdtCoin.getErrorMessage());
  }

  @Test
  @DisplayName("Constructor should create valid coin for supported coins")
  void constructor_WithSupportedCoin_ShouldCreateValidCoin() {
    Coin usdtCoin = new Coin("USDT");

    assertEquals("USDT", usdtCoin.getName());
    assertEquals(6, usdtCoin.getDecimal());
    assertTrue(usdtCoin.isValid());
    assertEquals("", usdtCoin.getErrorMessage());

    Coin vndCoin = new Coin("vnd"); // Kiểm tra case insensitive
    assertEquals("VND", vndCoin.getName());
    assertEquals(0, vndCoin.getDecimal());
    assertTrue(vndCoin.isValid());
    assertEquals("", vndCoin.getErrorMessage());
  }

  @Test
  @DisplayName("Constructor should create invalid coin for unsupported coins")
  void constructor_WithUnsupportedCoin_ShouldCreateInvalidCoin() {
    Coin btcCoin = new Coin("BTC");

    assertEquals("BTC", btcCoin.getName());
    assertEquals(0, btcCoin.getDecimal());
    assertFalse(btcCoin.isValid());
    assertEquals("Unsupported coin: BTC", btcCoin.getErrorMessage());
  }

  @Test
  @DisplayName("Constructor should handle null or empty input")
  void constructor_WithNullOrEmptyInput_ShouldHandleGracefully() {
    Coin nullCoin = new Coin(null);

    assertEquals("", nullCoin.getName());
    assertEquals(0, nullCoin.getDecimal());
    assertFalse(nullCoin.isValid());
    assertEquals("Coin is required", nullCoin.getErrorMessage());

    Coin emptyCoin = new Coin("");
    assertEquals("", emptyCoin.getName());
    assertEquals(0, emptyCoin.getDecimal());
    assertFalse(emptyCoin.isValid());
    assertEquals("Coin is required", emptyCoin.getErrorMessage());
  }

  @Test
  @DisplayName("Constructor should be case insensitive")
  void constructor_IsCaseInsensitive() {
    Coin usdtLowerCase = new Coin("usdt");
    Coin usdtMixedCase = new Coin("Usdt");
    Coin usdtUpperCase = new Coin("USDT");

    assertEquals("USDT", usdtLowerCase.getName());
    assertEquals("USDT", usdtMixedCase.getName());
    assertEquals("USDT", usdtUpperCase.getName());

    assertTrue(usdtLowerCase.isValid());
    assertTrue(usdtMixedCase.isValid());
    assertTrue(usdtUpperCase.isValid());
  }

  @Test
  @DisplayName("validateCoin() should return empty string for valid coins")
  void validateCoin_WithValidCoin_ReturnsEmptyString() {
    assertEquals("", Coin.validateCoin("USDT"));
    assertEquals("", Coin.validateCoin("VND"));
    assertEquals("", Coin.validateCoin("PHP"));
    assertEquals("", Coin.validateCoin("NGN"));
    assertEquals("", Coin.validateCoin("usdt"));
    assertEquals("", Coin.validateCoin("vnd"));
  }

  @Test
  @DisplayName("validateCoin() should return error message for null coin")
  void validateCoin_WithNullCoin_ReturnsErrorMessage() {
    assertEquals("Coin is required", Coin.validateCoin(null));
  }

  @Test
  @DisplayName("validateCoin() should return error message for empty coin")
  void validateCoin_WithEmptyCoin_ReturnsErrorMessage() {
    assertEquals("Coin is required", Coin.validateCoin(""));
  }

  @Test
  @DisplayName("validateCoin() should return error message for unsupported coins")
  void validateCoin_WithUnsupportedCoin_ReturnsErrorMessage() {
    assertEquals("Unsupported coin: BTC", Coin.validateCoin("BTC"));
    assertEquals("Unsupported coin: ETH", Coin.validateCoin("ETH"));
    assertEquals("Unsupported coin: XXX", Coin.validateCoin("XXX"));
  }

  @Test
  @DisplayName("isValid() should return correct validation state")
  void isValid_ShouldReturnCorrectState() {
    assertTrue(new Coin("USDT").isValid());
    assertTrue(new Coin("VND").isValid());
    assertTrue(new Coin("PHP").isValid());
    assertTrue(new Coin("NGN").isValid());

    assertFalse(new Coin("BTC").isValid());
    assertFalse(new Coin("").isValid());
    assertFalse(new Coin(null).isValid());
  }

  @Test
  @DisplayName("All setters should correctly update the Coin properties")
  void allSetters_ShouldUpdatePropertiesCorrectly() throws Exception {
    // Create a default Coin
    Coin coin = new Coin();

    // Use reflection to access the setters since they have package-private access
    java.lang.reflect.Method setNameMethod = Coin.class.getDeclaredMethod("setName", String.class);
    java.lang.reflect.Method setDecimalMethod = Coin.class.getDeclaredMethod("setDecimal", int.class);
    java.lang.reflect.Method setErrorMessageMethod = Coin.class.getDeclaredMethod("setErrorMessage", String.class);

    // Make the methods accessible
    setNameMethod.setAccessible(true);
    setDecimalMethod.setAccessible(true);
    setErrorMessageMethod.setAccessible(true);

    // Set new values
    String newName = "PHP";
    int newDecimal = 4;
    String newErrorMessage = "Test error message";

    // Call the setters
    setNameMethod.invoke(coin, newName);
    setDecimalMethod.invoke(coin, newDecimal);
    setErrorMessageMethod.invoke(coin, newErrorMessage);

    // Verify the values were updated correctly
    assertEquals(newName, coin.getName(), "Name should be updated");
    assertEquals(newDecimal, coin.getDecimal(), "Decimal should be updated");
    assertEquals(newErrorMessage, coin.getErrorMessage(), "Error message should be updated");

    // Verify toString includes all fields
    String toString = coin.toString();
    assertTrue(toString.contains("name='" + newName + "'"), "toString should include name");
    assertTrue(toString.contains("decimal=" + newDecimal), "toString should include decimal");
    assertTrue(toString.contains("errorMessage='" + newErrorMessage + "'"), "toString should include errorMessage");
  }
}
