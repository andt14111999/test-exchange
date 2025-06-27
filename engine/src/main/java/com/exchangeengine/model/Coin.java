package com.exchangeengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Class đại diện cho các loại tiền tệ được hỗ trợ
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Coin {
  private static final Map<String, Integer> SUPPORTED_COINS = new HashMap<>();

  static {
    // Khởi tạo danh sách các đồng tiền được hỗ trợ và số thập phân tương ứng
    SUPPORTED_COINS.put("USDT", 6);
    SUPPORTED_COINS.put("VND", 0);
    SUPPORTED_COINS.put("PHP", 2);
    SUPPORTED_COINS.put("NGN", 2);
  }

  private String name;
  private int decimal;
  private String errorMessage;

  /**
   * Constructor không tham số cho Jackson deserialization
   */
  public Coin() {
    this.name = "";
    this.decimal = 0;
    this.errorMessage = "";
  }

  /**
   * Constructor với tham số name, decimal sẽ được tính toán dựa vào name
   */
  public Coin(String name) {
    this();

    if (name == null || name.isEmpty()) {
      this.errorMessage = "Coin is required";
      return;
    }

    this.name = name.toUpperCase();
    Integer decimal = SUPPORTED_COINS.get(this.name);
    if (decimal != null) {
      this.decimal = decimal;
      this.errorMessage = "";
    } else {
      this.errorMessage = "Unsupported coin: " + name;
    }
  }

  /**
   * Validate tên đồng tiền
   *
   * @param coinName Tên đồng tiền
   * @return Chuỗi rỗng nếu hợp lệ, thông báo lỗi nếu không hợp lệ
   */
  public static String validateCoin(String coinName) {
    Coin coin = new Coin(coinName);
    return coin.getErrorMessage();
  }

  /**
   * Kiểm tra coin có hợp lệ không
   *
   * @return true nếu hợp lệ, false nếu không hợp lệ
   */
  public boolean isValid() {
    return errorMessage.isEmpty();
  }

  // Getters & Setters
  public String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  public int getDecimal() {
    return decimal;
  }

  void setDecimal(int decimal) {
    this.decimal = decimal;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public String toString() {
    return "Coin{" +
        "name='" + name + '\'' +
        ", decimal=" + decimal +
        ", errorMessage='" + errorMessage + '\'' +
        '}';
  }
}
