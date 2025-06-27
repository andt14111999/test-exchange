package com.exchangeengine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.util.JsonSerializer;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Automated Market Maker (AMM) Order Model
 * Đại diện cho một lệnh giao dịch trong AMM pool
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmmOrder {

  @NotBlank(message = "Order identifier is required")
  private String identifier;

  @NotBlank(message = "Pool pair is required")
  private String poolPair;

  @NotBlank(message = "Owner account key 0 is required")
  private String ownerAccountKey0;

  @NotBlank(message = "Owner account key 1 is required")
  private String ownerAccountKey1;

  @NotNull(message = "ZeroForOne direction is required")
  private Boolean zeroForOne;

  @NotNull(message = "Amount specified cannot be null")
  private BigDecimal amountSpecified = BigDecimal.ZERO;

  @NotNull(message = "Amount estimated cannot be null")
  @PositiveOrZero(message = "Amount estimated must be greater than or equal to 0")
  private BigDecimal amountEstimated = BigDecimal.ZERO;

  @NotNull(message = "Amount actual cannot be null")
  @PositiveOrZero(message = "Amount actual must be greater than or equal to 0")
  private BigDecimal amountActual = BigDecimal.ZERO;

  @NotNull(message = "Amount received cannot be null")
  @PositiveOrZero(message = "Amount received must be greater than or equal to 0")
  private BigDecimal amountReceived = BigDecimal.ZERO;

  private int beforeTickIndex;
  private int afterTickIndex;

  @NotNull(message = "Fees map cannot be null")
  private Map<String, BigDecimal> fees = new HashMap<>();

  @NotBlank(message = "Status is required")
  @Pattern(regexp = "processing|success|error", message = "Status must be one of: processing, success, error")
  private String status;

  private String errorMessage = "";

  @NotNull(message = "Slippage cannot be null")
  @PositiveOrZero(message = "Slippage must be greater than or equal to 0")
  private BigDecimal slippage = AmmPoolConfig.DEFAULT_SLIPPAGE;

  private long createdAt = Instant.now().toEpochMilli();
  private long updatedAt = Instant.now().toEpochMilli();
  private long completedAt;

  // Status constants
  public static final String STATUS_PROCESSING = "processing";
  public static final String STATUS_SUCCESS = "success";
  public static final String STATUS_ERROR = "error";

  protected AccountCache getAccountCache() {
    return AccountCache.getInstance();
  }

  protected AmmPoolCache getAmmPoolCache() {
    return AmmPoolCache.getInstance();
  }

  /**
   * Tạo order với identifier và pool
   *
   * @param identifier Order identifier
   * @param poolPair   Pool pair name
   */
  public AmmOrder(String identifier, String poolPair) {
    this.identifier = identifier;
    this.poolPair = poolPair;
    this.createdAt = Instant.now().toEpochMilli();
    this.updatedAt = this.createdAt;
  }

  /**
   * Constructor với đầy đủ thông tin ban đầu
   */
  public AmmOrder(
      String identifier,
      String poolPair,
      String ownerAccountKey0,
      String ownerAccountKey1,
      Boolean zeroForOne,
      BigDecimal amountSpecified,
      BigDecimal slippage,
      String status) {
    this(identifier, poolPair);
    this.ownerAccountKey0 = ownerAccountKey0;
    this.ownerAccountKey1 = ownerAccountKey1;
    this.zeroForOne = zeroForOne;
    this.amountSpecified = amountSpecified;
    this.slippage = slippage;
    this.status = status;
  }

  public List<String> validateRequiredFields() {
    List<String> errors = new ArrayList<>();

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<AmmOrder>> violations = validator.validate(this);

    errors.addAll(violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList()));

    if (slippage.compareTo(AmmPoolConfig.MIN_SLIPPAGE) < 0) {
      errors.add("Slippage must be at least 0.01% (value: " + AmmPoolConfig.MIN_SLIPPAGE + ")");
    }

    if (amountSpecified.compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Amount specified must be greater than 0");
    }

    return errors;
  }

  public List<String> validateResourcesExist() {
    List<String> errors = new ArrayList<>();

    Optional<AmmPool> poolOpt = getPool();
    if (!poolOpt.isPresent()) {
      errors.add("Pool not found: " + poolPair);
      return errors;
    }

    AmmPool pool = poolOpt.get();
    if (!pool.isActive()) {
      errors.add("Pool is not active: " + poolPair);
    }

    if (!getAccountCache().getAccount(ownerAccountKey0).isPresent()) {
      errors.add("Account not found: " + ownerAccountKey0);
    }

    if (!getAccountCache().getAccount(ownerAccountKey1).isPresent()) {
      errors.add("Account not found: " + ownerAccountKey1);
    }

    return errors;
  }

  /**
   * Update thông tin order sau khi thực hiện
   *
   * @param amountActual    Số lượng thực tế
   * @param amountReceived  Số lượng token thực tế nhận được
   * @param beforeTickIndex Tick index trước khi thực hiện
   * @param afterTickIndex  Tick index sau khi thực hiện
   * @param fees            Phí giao dịch
   * @return true nếu update thành công, false nếu không
   */
  public boolean updateAfterExecution(
      BigDecimal amountActual,
      BigDecimal amountReceived,
      int beforeTickIndex,
      int afterTickIndex,
      Map<String, BigDecimal> fees) {
    if (!isProcessing()) {
      return false;
    }

    this.amountActual = amountActual;
    this.amountReceived = amountReceived;
    this.beforeTickIndex = beforeTickIndex;
    this.afterTickIndex = afterTickIndex;
    this.fees = new HashMap<>(fees);
    this.updatedAt = Instant.now().toEpochMilli();

    return true;
  }

  /**
   * Đánh dấu order thành công
   *
   * @return true nếu update thành công, false nếu không
   */
  public boolean markSuccess() {
    if (!isProcessing()) {
      return false;
    }

    this.status = STATUS_SUCCESS;
    this.completedAt = Instant.now().toEpochMilli();
    this.updatedAt = this.completedAt;

    return true;
  }

  /**
   * Đánh dấu order lỗi
   *
   * @param errorMessage Thông báo lỗi
   * @return true nếu update thành công, false nếu không
   */
  public boolean markError(String errorMessage) {
    if (!isProcessing()) {
      return false;
    }

    this.status = STATUS_ERROR;
    this.errorMessage = errorMessage;
    this.completedAt = Instant.now().toEpochMilli();
    this.updatedAt = this.completedAt;

    return true;
  }

  public boolean isProcessing() {
    return STATUS_PROCESSING.equals(this.status);
  }

  public boolean isSuccess() {
    return STATUS_SUCCESS.equals(this.status);
  }

  public boolean isError() {
    return STATUS_ERROR.equals(this.status);
  }

  public Account getAccount0() {
    Optional<Account> account = getAccountCache().getAccount(ownerAccountKey0);
    if (account.isEmpty()) {
      throw new IllegalStateException("Account not found: " + ownerAccountKey0);
    }

    return account.get();
  }

  public Account getAccount1() {
    Optional<Account> account = getAccountCache().getAccount(ownerAccountKey1);
    if (account.isEmpty()) {
      throw new IllegalStateException("Account not found: " + ownerAccountKey1);
    }

    return account.get();
  }

  /**
   * Lấy thông tin pool từ cache
   */
  public Optional<AmmPool> getPool() {
    return getAmmPoolCache().getAmmPool(poolPair);
  }

  public Map<String, Object> toMessageJson() {
    return JsonSerializer.toMap(this);
  }
}
