package com.exchangeengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Base model for all events in the system
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseTransaction {
  protected static final int COIN_DEFAULT_SCALE = 16;

  // Common fields for all events
  private ActionType actionType; // Type of action (COIN_TRANSACTION, P2P_TRANSACTION, etc.)
  private String actionId; // External reference ID
  private String accountKey; // Account key in format "coin:accountId"
  private String identifier; // Unique identifier for the event
  private String status; // Event status
  private String statusExplanation; // Explanation for the status
  private long createdAt; // Creation timestamp
  private long updatedAt; // Last update timestamp

  // Constructor
  public BaseTransaction() {
    this.createdAt = Instant.now().toEpochMilli();
    this.updatedAt = this.createdAt;
  }

  public String getActionType() {
    return actionType != null ? actionType.getValue() : null;
  }

  public void setActionType(String actionType) {
    this.actionType = ActionType.fromValue(actionType);
  }

  public void setActionType(ActionType actionType) {
    this.actionType = actionType;
  }

  public String getActionId() {
    return actionId;
  }

  public void setActionId(String actionId) {
    this.actionId = actionId;
  }

  public String getAccountKey() {
    return accountKey;
  }

  public void setAccountKey(String accountKey) {
    this.accountKey = accountKey;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
    this.updatedAt = Instant.now().toEpochMilli();
  }

  public String getStatusExplanation() {
    return statusExplanation;
  }

  public void setStatusExplanation(String statusExplanation) {
    this.statusExplanation = statusExplanation;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getIdentifier() {
    return identifier != null ? identifier.toLowerCase() : null;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier != null ? identifier.toLowerCase() : null;
  }

  /**
   * Update the event status
   *
   * @param newStatus The new status to set
   */
  public void updateStatus(String newStatus) {
    this.status = newStatus;
    this.updatedAt = Instant.now().toEpochMilli();
  }

  /**
   * Validate required fields
   *
   * @return List of errors
   */
  public List<String> validateRequiredFields() {
    List<String> errors = new ArrayList<>();
    if (identifier == null || identifier.trim().isEmpty()) {
      errors.add("Identifier is required");
    }

    if (actionType == null) {
      errors.add("Action type is required");
    }

    if (actionId == null || actionId.trim().isEmpty()) {
      errors.add("Action ID is required");
    }

    if (accountKey == null || accountKey.trim().isEmpty()) {
      errors.add("Account key is required");
    }

    if (status == null || status.trim().isEmpty()) {
      errors.add("Status is required");
    }

    return errors;
  }

  // override toString
  @Override
  public String toString() {
    return "BaseCoinTransaction{" +
        "actionType=" + actionType +
        ", actionId='" + actionId + '\'' +
        ", accountKey='" + accountKey + '\'' +
        ", identifier='" + identifier + '\'' +
        ", status='" + status + '\'' +
        ", statusExplanation='" + statusExplanation + '\'' +
        ", createdAt=" + createdAt +
        ", updatedAt=" + updatedAt +
        '}';
  }
}
