package com.exchangeengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Base model for coin transactions (deposit and withdrawal)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CoinTransaction extends BaseTransaction {
  // Coin transaction specific fields
  private BigDecimal amount; // Transaction amount
  private String coin; // Coin type (BTC, ETH, etc.)
  private String txHash; // Blockchain transaction hash
  private String layer; // Blockchain layer (Layer 1, Layer 2, etc.)

  // Constructor
  public CoinTransaction() {
    super();
    setActionType(ActionType.COIN_TRANSACTION);
  }

  // Getters and setters
  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount.setScale(COIN_DEFAULT_SCALE, RoundingMode.HALF_UP);
  }

  public String getCoin() {
    return coin;
  }

  public void setCoin(String coin) {
    this.coin = coin.toLowerCase().trim();
  }

  public String getTxHash() {
    return txHash;
  }

  public void setTxHash(String txHash) {
    this.txHash = txHash;
  }

  public String getLayer() {
    return layer;
  }

  public void setLayer(String layer) {
    this.layer = layer;
  }

  /**
   * Validate required fields
   *
   * @return List of errors
   */
  @Override
  public List<String> validateRequiredFields() {
    List<String> errors = super.validateRequiredFields();

    if (amount == null) {
      errors.add("Amount is required");
    }

    if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Amount must be greater than 0");
    }

    String coinValidation = Coin.validateCoin(coin);
    if (!coinValidation.isEmpty()) {
      errors.add(coinValidation);
    }

    if (txHash == null || txHash.trim().isEmpty()) {
      errors.add("Transaction hash is required");
    }

    if (layer == null || layer.trim().isEmpty()) {
      errors.add("Layer is required");
    }

    return errors;
  }

  // override toString
  @Override
  public String toString() {
    return "CoinTransaction{" +
        ", amount=" + amount +
        ", coin='" + coin + '\'' +
        ", txHash='" + txHash + '\'' +
        ", layer='" + layer + '\'' +
        ", " + super.toString() +
        '}';
  }
}
