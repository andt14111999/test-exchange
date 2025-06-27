package com.exchangeengine.model;

import com.exchangeengine.util.JsonSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model for coin withdrawals
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinWithdrawal extends CoinTransaction {
  private static final String STATUS_PENDING = "pending";
  private static final String STATUS_VERIFIED = "verified";
  private static final String STATUS_CANCELLED = "cancelled";
  private static final String STATUS_PROCESSING = "processing";
  private static final String STATUS_COMPLETED = "completed";
  private static final String STATUS_FAILED = "failed";

  // Withdrawal specific fields
  private String destinationAddress; // Recipient address
  private BigDecimal fee; // Withdrawal fee
  private String recipientAccountKey; // Recipient account key for internal transfers

  // Constructor
  public CoinWithdrawal() {
    super();
  }

  /**
   * Constructor with essential parameters
   *
   * @param eventId            Event ID
   * @param actionType         Action type
   * @param actionId           Action ID
   * @param identifier         External reference ID
   * @param status             Status
   * @param accountKey         Account key
   * @param amount             Withdrawal amount
   * @param coin               Coin type
   * @param txHash             Transaction hash
   * @param layer              Layer
   * @param destinationAddress Destination address
   * @param fee                Withdrawal fee
   * @param recipientAccountKey Recipient account key for internal transfers
   */
  public CoinWithdrawal(
      ActionType actionType,
      String actionId,
      String identifier,
      String status,
      String accountKey,
      BigDecimal amount,
      String coin,
      String txHash,
      String layer,
      String destinationAddress,
      BigDecimal fee,
      String recipientAccountKey) {
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
    setDestinationAddress(destinationAddress);
    setFee(fee);
    setRecipientAccountKey(recipientAccountKey);
  }

  /**
   * Constructor with essential parameters without recipient account key
   *
   * @param eventId            Event ID
   * @param actionType         Action type
   * @param actionId           Action ID
   * @param identifier         External reference ID
   * @param status             Status
   * @param accountKey         Account key
   * @param amount             Withdrawal amount
   * @param coin               Coin type
   * @param txHash             Transaction hash
   * @param layer              Layer
   * @param destinationAddress Destination address
   * @param fee                Withdrawal fee
   */
  public CoinWithdrawal(
      ActionType actionType,
      String actionId,
      String identifier,
      String status,
      String accountKey,
      BigDecimal amount,
      String coin,
      String txHash,
      String layer,
      String destinationAddress,
      BigDecimal fee) {
    this(actionType, actionId, identifier, status, accountKey, amount, coin, txHash, layer, destinationAddress, fee, null);
  }

  // Getters and setters
  public String getDestinationAddress() {
    return destinationAddress;
  }

  public void setDestinationAddress(String destinationAddress) {
    this.destinationAddress = destinationAddress;
  }

  public BigDecimal getFee() {
    return fee;
  }

  public void setFee(BigDecimal fee) {
    this.fee = fee;
  }

  public String getRecipientAccountKey() {
    return recipientAccountKey;
  }

  public void setRecipientAccountKey(String recipientAccountKey) {
    this.recipientAccountKey = recipientAccountKey;
  }

  public boolean hasRecipientAccount() {
    return recipientAccountKey != null && !recipientAccountKey.trim().isEmpty();
  }

  public BigDecimal getAmountWithFee() {
    return getAmount().add(getFee());
  }

  public boolean isPending() {
    return STATUS_PENDING.equals(getStatus());
  }

  public boolean isVerified() {
    return STATUS_VERIFIED.equals(getStatus());
  }

  public boolean isCancelled() {
    return STATUS_CANCELLED.equals(getStatus());
  }

  public boolean isProcessing() {
    return STATUS_PROCESSING.equals(getStatus());
  }

  public boolean isCompleted() {
    return STATUS_COMPLETED.equals(getStatus());
  }

  public boolean isFailed() {
    return STATUS_FAILED.equals(getStatus());
  }

  public void transitionToProcessing() {
    if (!isVerified() && !isPending()) {
      throw new IllegalStateException("Cannot transition to processing from " + getStatus() + " status");
    }
    setUpdatedAt(System.currentTimeMillis());
    setStatusExplanation("");
    setStatus(STATUS_PROCESSING);
  }

  public void transitionToCompleted() {
    if (!isProcessing()) {
      throw new IllegalStateException("Cannot transition to completed from " + getStatus() + " status");
    }
    setUpdatedAt(System.currentTimeMillis());
    setStatus(STATUS_COMPLETED);
  }

  public void transitionToFailed() {
    if (!isProcessing()) {
      throw new IllegalStateException("Cannot transition to failed from " + getStatus() + " status");
    }
    setUpdatedAt(System.currentTimeMillis());
    setStatus(STATUS_FAILED);
  }

  public void transitionToCancelled() {
    if (!isProcessing() && !isPending()) {
      throw new IllegalStateException("Cannot transition to cancelled from " + getStatus() + " status");
    }
    setUpdatedAt(System.currentTimeMillis());
    setStatus(STATUS_CANCELLED);
  }

  /**
   * Validate required fields for withdrawals
   *
   * @return List of errors
   */
  public List<String> coinWithdrawalValidates(String eventStatus) {
    List<String> errors = super.validateRequiredFields();
    if (!isCompleted() && !isCancelled() && !isFailed() && !isVerified()) {
      errors.add("The input status must be " + STATUS_COMPLETED + ", " + STATUS_CANCELLED + ", " + STATUS_VERIFIED + ", or " + STATUS_FAILED);
    }

    if (!ActionType.COIN_TRANSACTION.isEqualTo(getActionType())) {
      errors.add("Action type not matched: expected coin transaction, got " + getActionType());
    }

    if (destinationAddress == null || destinationAddress.isEmpty()) {
      errors.add("Destination address is required");
    }

    if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
      errors.add("Fee is required and must be greater than or equal to 0");
    }

    return errors;
  }

  public List<String> coinWithdrawalReleasingValidates(String operationType) {
    List<String> errors = new ArrayList<>();
    if (!STATUS_PROCESSING.equals(getStatus())) {
      errors.add("The input status must equal processing");
    }

    if (!OperationType.COIN_WITHDRAWAL_RELEASING.isEqualTo(operationType)) {
      errors.add("Action type not matched: expected coin withdrawal releasing, got " + operationType);
    }

    return errors;
  }

  public List<String> coinWithdrawalFailedValidates(String operationType) {
    List<String> errors = new ArrayList<>();
    if (!STATUS_PROCESSING.equals(getStatus())) {
      errors.add("The input status must equal processing");
    }

    if (!OperationType.COIN_WITHDRAWAL_FAILED.isEqualTo(operationType)) {
      errors.add("Action type not matched: expected coin withdrawal failed, got " + operationType);
    }

    return errors;
  }

  public List<String> coinWithdrawalCancelledValidates(String operationType) {
    List<String> errors = new ArrayList<>();
    if (!STATUS_PROCESSING.equals(getStatus()) && !STATUS_PENDING.equals(getStatus()) && !STATUS_FAILED.equals(getStatus())) {
      errors.add("The input status must equal processing or pending or failed");
    }

    if (!OperationType.COIN_WITHDRAWAL_CANCELLED.isEqualTo(operationType)) {
      errors.add("Action type not matched: expected coin withdrawal cancelled, got " + operationType);
    }

    return errors;
  }

  /**
   * Convert to message json
   *
   * @return message json
   */
  public Map<String, Object> toMessageJson() {
    return JsonSerializer.toMap(this);
  }

  // override toString
  @Override
  public String toString() {
    return "CoinWithdrawal{" +
        "destinationAddress='" + destinationAddress + '\'' +
        ", fee=" + fee +
        ", " + super.toString() +
        '}';
  }
}
