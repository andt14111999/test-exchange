package com.exchangeengine.model;

import com.exchangeengine.util.JsonSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Model for coin deposits
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinDeposit extends CoinTransaction {
  // Deposit specific fields
  private String depositAddress; // Deposit address

  private static final String STATUS_PENDING = "pending"; // input event
  private static final String STATUS_PROCESSED = "processed"; // action pending success status
  private static final String STATUS_FAILED = "failed"; // action pending nothing change

  // Constructor
  public CoinDeposit() {
    super();
  }

  /**
   * Constructor with essential parameters
   *
   * @param eventId        Event ID
   * @param actionType     Action type
   * @param actionId       Action ID
   * @param identifier     External reference ID
   * @param status         Status
   * @param accountKey     Account key
   * @param amount         Deposit amount
   * @param coin           Coin type
   * @param txHash         Transaction hash
   * @param layer          Layer
   * @param depositAddress Deposit address
   */
  public CoinDeposit(
      ActionType actionType,
      String actionId,
      String identifier,
      String status,
      String accountKey,
      BigDecimal amount,
      String coin,
      String txHash,
      String layer,
      String depositAddress) {
    this();
    setActionType(actionType);
    setActionId(actionId);
    setIdentifier(identifier);
    setStatus(status);
    setAccountKey(accountKey);
    setAmount(amount);
    setCoin(coin);
    setTxHash(txHash);
    setLayer(layer);
    setDepositAddress(depositAddress);
  }

  // Getters and setters
  public String getDepositAddress() {
    return depositAddress;
  }

  public void setDepositAddress(String depositAddress) {
    this.depositAddress = depositAddress;
  }

  /**
   * Process the deposit
   * Changes status from DEPOSITED to PROCESSING
   *
   * @throws IllegalStateException if deposit is not in DEPOSITED status
   */
  public void completeDeposit() {
    String currentStatus = getStatus();
    if (!STATUS_PENDING.equals(currentStatus)) {
      throw new IllegalStateException(
          "Cannot process deposit: current status is " + currentStatus + ", expected " + STATUS_PENDING);
    }
    setStatus(STATUS_PROCESSED);
  }

  public boolean isPending() {
    return STATUS_PENDING.equals(getStatus());
  }

  public boolean isProcessed() {
    return STATUS_PROCESSED.equals(getStatus());
  }

  public boolean isFailed() {
    return STATUS_FAILED.equals(getStatus());
  }

  /**
   * Set status to processed
   */
  public void transitionToProcessed() {
    if (isProcessed() || !isPending()) {
      throw new IllegalStateException("Cannot transition to processed from processed or not pending status");
    }
    setUpdatedAt(System.currentTimeMillis());
    setStatus(STATUS_PROCESSED);
  }

  /**
   * Transition to failed
   *
   * @param reason Reason for failure
   */
  public void transitionToFailed(String reason) {
    if (isProcessed()) {
      throw new IllegalStateException("Cannot transition to failed from processed status");
    }
    setStatusExplanation(reason);
    setStatus(STATUS_FAILED);
  }

  /**
   * Convert to message json
   *
   * @return message json
   */
  public Map<String, Object> toMessageJson() {
    return JsonSerializer.toMap(this);
  }

  /**
   * Validate required fields for deposits
   *
   * @return List of errors
   */
  public List<String> coinDepositValidates(String eventStatus) {
    List<String> errors = super.validateRequiredFields();

    if (depositAddress == null || depositAddress.trim().isEmpty()) {
      errors.add("Deposit address is required");
    }

    if (!STATUS_PENDING.equals(eventStatus)) {
      errors.add("The input status must be " + STATUS_PENDING);
    }

    if (!ActionType.COIN_TRANSACTION.isEqualTo(getActionType())) {
      errors.add("Action type not matched: expected coin transaction, got " + getActionType());
    }

    return errors;
  }

  // override toString
  @Override
  public String toString() {
    return "CoinDeposit{" +
        "depositAddress='" + depositAddress + '\'' +
        ", " + super.toString() +
        '}';
  }
}
