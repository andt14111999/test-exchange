package com.exchangeengine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.exchangeengine.storage.cache.AccountCache;
import com.exchangeengine.storage.cache.AmmPoolCache;
import com.exchangeengine.storage.cache.TickBitmapCache;
import com.exchangeengine.storage.cache.TickCache;
import com.exchangeengine.util.JsonSerializer;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import com.exchangeengine.util.ammPool.LiquidityUtils;
import com.exchangeengine.util.ammPool.TickMath;
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
 * Automated Market Maker (AMM) Position Model
 * Represents a liquidity position in an AMM pool
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmmPosition {

  @NotBlank(message = "Position identifier is required")
  private String identifier;

  @NotBlank(message = "Pool pair is required")
  private String poolPair;

  @NotBlank(message = "Owner account key 0 is required")
  private String ownerAccountKey0;

  @NotBlank(message = "Owner account key 1 is required")
  private String ownerAccountKey1;

  @NotBlank(message = "Status is required")
  @Pattern(regexp = "pending|open|closed|error", message = "Status must be one of: pending, open, closed, error")
  private String status = "pending";

  private String errorMessage = "";

  private int tickLowerIndex;
  private int tickUpperIndex;

  @NotNull(message = "Liquidity cannot be null")
  @PositiveOrZero(message = "Liquidity must be greater than or equal to 0")
  private BigDecimal liquidity = BigDecimal.ZERO;

  @NotNull(message = "Slippage cannot be null")
  @PositiveOrZero(message = "Slippage must be greater than or equal to 0")
  private BigDecimal slippage = AmmPoolConfig.MAX_SLIPPAGE; // slippage của người dùng input vào mặc định là 100% (auto)
                                                            // min sẽ là 0.01%

  @NotNull(message = "Amount 0 cannot be null")
  @PositiveOrZero(message = "Amount 0 must be greater than or equal to 0")
  private BigDecimal amount0 = BigDecimal.ZERO; // giá trị tính toán min để tạo position

  @NotNull(message = "Amount 1 cannot be null")
  @PositiveOrZero(message = "Amount 1 must be greater than or equal to 0")
  private BigDecimal amount1 = BigDecimal.ZERO; // giá trị tính toán min để tạo position

  @NotNull(message = "Initial amount 0 cannot be null")
  @PositiveOrZero(message = "Initial amount 0 must be greater than or equal to 0")
  private BigDecimal amount0Initial = BigDecimal.ZERO; // giá trị người dùng input

  @NotNull(message = "Initial amount 1 cannot be null")
  @PositiveOrZero(message = "Initial amount 1 must be greater than or equal to 0")
  private BigDecimal amount1Initial = BigDecimal.ZERO; // giá trị người dùng input

  @NotNull(message = "Fee growth inside 0 last cannot be null")
  @PositiveOrZero(message = "Fee growth inside 0 last must be greater than or equal to 0")
  private BigDecimal feeGrowthInside0Last = BigDecimal.ZERO;

  @NotNull(message = "Fee growth inside 1 last cannot be null")
  @PositiveOrZero(message = "Fee growth inside 1 last must be greater than or equal to 0")
  private BigDecimal feeGrowthInside1Last = BigDecimal.ZERO;

  @NotNull(message = "Tokens owed 0 cannot be null")
  @PositiveOrZero(message = "Tokens owed 0 must be greater than or equal to 0")
  private BigDecimal tokensOwed0 = BigDecimal.ZERO;

  @NotNull(message = "Tokens owed 1 cannot be null")
  @PositiveOrZero(message = "Tokens owed 1 must be greater than or equal to 0")
  private BigDecimal tokensOwed1 = BigDecimal.ZERO;

  @NotNull(message = "Fee collected 0 cannot be null")
  @PositiveOrZero(message = "Fee collected 0 must be greater than or equal to 0")
  private BigDecimal feeCollected0 = BigDecimal.ZERO;

  @NotNull(message = "Fee collected 1 cannot be null")
  @PositiveOrZero(message = "Fee collected 1 must be greater than or equal to 0")
  private BigDecimal feeCollected1 = BigDecimal.ZERO;

  @NotNull(message = "Amount 0 withdrawal cannot be null")
  @PositiveOrZero(message = "Amount 0 withdrawal must be greater than or equal to 0")
  private BigDecimal amount0Withdrawal = BigDecimal.ZERO; // Lượng token0 được rút khi đóng position

  @NotNull(message = "Amount 1 withdrawal cannot be null")
  @PositiveOrZero(message = "Amount 1 withdrawal must be greater than or equal to 0")
  private BigDecimal amount1Withdrawal = BigDecimal.ZERO; // Lượng token1 được rút khi đóng position

  private long createdAt = Instant.now().toEpochMilli();
  private long updatedAt = Instant.now().toEpochMilli();
  private long stoppedAt;

  // Status constants
  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_OPEN = "open";
  public static final String STATUS_CLOSED = "closed";
  public static final String STATUS_ERROR = "error";

  protected AccountCache getAccountCache() {
    return AccountCache.getInstance();
  }

  protected TickCache getTickCache() {
    return TickCache.getInstance();
  }

  protected AmmPoolCache getAmmPoolCache() {
    return AmmPoolCache.getInstance();
  }

  protected TickBitmapCache getTickBitmapCache() {
    return TickBitmapCache.getInstance();
  }

  /**
   * Tạo position với identifier và pool
   *
   * @param identifier Position identifier
   * @param poolPair   Pool pair name
   */
  public AmmPosition(String identifier, String poolPair) {
    this.identifier = identifier;
    this.poolPair = poolPair;
    this.status = STATUS_PENDING;
    this.createdAt = Instant.now().toEpochMilli();
    this.updatedAt = this.createdAt;
  }

  /**
   * Constructor với đầy đủ thông tin ban đầu
   *
   * @param identifier       Position identifier
   * @param poolPair         Pool pair
   * @param ownerAccountKey0 Owner account key 0
   * @param tickLowerIndex   Tick lower index
   * @param tickUpperIndex   Tick upper index
   * @param ownerAccountKey1 Owner account key 1
   * @param slippage         Slippage amount
   * @param amount0Initial   Initial amount 0
   * @param amount1Initial   Initial amount 1
   */
  public AmmPosition(
      String identifier,
      String poolPair,
      String ownerAccountKey0,
      String ownerAccountKey1,
      int tickLowerIndex,
      int tickUpperIndex,
      BigDecimal slippage,
      BigDecimal amount0Initial,
      BigDecimal amount1Initial) {
    this(identifier, poolPair);
    this.ownerAccountKey0 = ownerAccountKey0;
    this.ownerAccountKey1 = ownerAccountKey1;
    this.tickLowerIndex = tickLowerIndex;
    this.tickUpperIndex = tickUpperIndex;
    this.slippage = slippage;
    this.amount0Initial = amount0Initial;
    this.amount1Initial = amount1Initial;
  }

  public List<String> validateRequiredFields() {
    List<String> errors = new ArrayList<>();

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<AmmPosition>> violations = validator.validate(this);

    errors.addAll(violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList()));

    String tickLowerValidation = AmmPoolConfig.validateTick(tickLowerIndex);
    if (!tickLowerValidation.isEmpty()) {
      errors.add("Lower tick: " + tickLowerValidation);
    }

    String tickUpperValidation = AmmPoolConfig.validateTick(tickUpperIndex);
    if (!tickUpperValidation.isEmpty()) {
      errors.add("Upper tick: " + tickUpperValidation);
    }

    if (tickUpperIndex <= tickLowerIndex) {
      errors.add("Upper tick must be greater than lower tick");
    }

    if (slippage.compareTo(AmmPoolConfig.MIN_SLIPPAGE) < 0) {
      errors.add("Slippage must be at least 0.01% (value: " + AmmPoolConfig.MIN_SLIPPAGE + ")");
    }

    if (liquidity.compareTo(AmmPoolConfig.MIN_LIQUIDITY) < 0 && !STATUS_PENDING.equals(this.status)) {
      errors.add("Liquidity must be at least " + AmmPoolConfig.MIN_LIQUIDITY + " to avoid dust positions");
    }

    if (STATUS_PENDING.equals(this.status) &&
        amount0Initial.compareTo(BigDecimal.ZERO) > 0 &&
        amount1Initial.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal estimatedLiquidity = calculateEstimatedLiquidity();
      if (estimatedLiquidity.compareTo(AmmPoolConfig.MIN_LIQUIDITY) < 0) {
        errors.add("Estimated liquidity (" + estimatedLiquidity + ") is less than minimum required (" +
            AmmPoolConfig.MIN_LIQUIDITY + "). Please increase the amount of tokens.");
      }
    }

    validateTickSpacing(errors);

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

    if (!getTickBitmap().isPresent()) {
      errors.add("Required tick bitmap does not exist for pool: " + poolPair);
    }

    return errors;
  }

  private void validateTickSpacing(List<String> errors) {
    Optional<AmmPool> poolOpt = getPool();
    if (!poolOpt.isPresent()) {
      return;
    }

    AmmPool pool = poolOpt.get();
    int tickSpacing = pool.getTickSpacing();

    if (tickLowerIndex % tickSpacing != 0) {
      errors.add(String.format("Lower tick index (%d) must be a multiple of tick spacing (%d)",
          tickLowerIndex, tickSpacing));
    }

    if (tickUpperIndex % tickSpacing != 0) {
      errors.add(String.format("Upper tick index (%d) must be a multiple of tick spacing (%d)",
          tickUpperIndex, tickSpacing));
    }
  }

  /**
   * Update thông tin position sau khi tạo
   * Chỉ được update khi status là pending
   *
   * @param tickLower            Tick lower
   * @param tickUpper            Tick upper
   * @param liquidity            Liquidity
   * @param amount0              Amount 0
   * @param amount1              Amount 1
   * @param feeGrowthInside0Last Fee growth inside 0 last
   * @param feeGrowthInside1Last Fee growth inside 1 last
   * @return true nếu update thành công, false nếu không
   */
  public boolean updateAfterCreate(int tickLowerIndex, int tickUpperIndex, BigDecimal liquidity,
      BigDecimal amount0, BigDecimal amount1,
      BigDecimal feeGrowthInside0Last, BigDecimal feeGrowthInside1Last) {
    if (!isPending()) {
      return false;
    }

    this.tickLowerIndex = tickLowerIndex;
    this.tickUpperIndex = tickUpperIndex;
    this.liquidity = liquidity;
    this.amount0 = amount0;
    this.amount1 = amount1;
    this.feeGrowthInside0Last = feeGrowthInside0Last;
    this.feeGrowthInside1Last = feeGrowthInside1Last;
    this.updatedAt = Instant.now().toEpochMilli();

    return true;
  }

  /**
   * Update position khi chuyển sang trạng thái open
   *
   * @return true nếu update thành công, false nếu không
   */
  public boolean openPosition() {
    if (!isPending()) {
      return false;
    }

    this.status = STATUS_OPEN;
    this.updatedAt = Instant.now().toEpochMilli();

    return true;
  }

  /**
   * Update position khi collect fee với cập nhật feeGrowthInside
   *
   * @param tokensOwed0      Số lượng token0 đã thu
   * @param tokensOwed1      Số lượng token1 đã thu
   * @param feeGrowthInside0 Giá trị fee growth inside 0 mới
   * @param feeGrowthInside1 Giá trị fee growth inside 1 mới
   * @return true nếu update thành công, false nếu không
   */
  public boolean updateAfterCollectFee(
      BigDecimal tokensOwed0,
      BigDecimal tokensOwed1,
      BigDecimal feeGrowthInside0,
      BigDecimal feeGrowthInside1) {
    if (!isOpen()) {
      return false;
    }

    // Cập nhật feeCollected - tổng phí đã thu
    this.feeCollected0 = this.feeCollected0.add(tokensOwed0)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
    this.feeCollected1 = this.feeCollected1.add(tokensOwed1)
        .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);

    // Reset tokensOwed về 0 sau khi đã thu phí
    this.tokensOwed0 = BigDecimal.ZERO;
    this.tokensOwed1 = BigDecimal.ZERO;

    // Cập nhật feeGrowthInside để theo dõi cho lần thu phí tiếp theo
    this.feeGrowthInside0Last = feeGrowthInside0;
    this.feeGrowthInside1Last = feeGrowthInside1;

    this.updatedAt = Instant.now().toEpochMilli();

    return true;
  }

  /**
   * Đóng position và cập nhật thông tin rút token và fee growth
   *
   * @param amount0Withdrawal    Số lượng token0 được rút khi đóng position
   * @param amount1Withdrawal    Số lượng token1 được rút khi đóng position
   * @param feeGrowthInside0Last Giá trị fee growth inside 0 cuối cùng
   * @param feeGrowthInside1Last Giá trị fee growth inside 1 cuối cùng
   * @return true nếu đóng thành công, false nếu không
   */
  public boolean closePosition(BigDecimal amount0Withdrawal, BigDecimal amount1Withdrawal,
      BigDecimal feeGrowthInside0Last, BigDecimal feeGrowthInside1Last) {
    if (!isOpen()) {
      return false;
    }

    this.status = STATUS_CLOSED;
    this.amount0Withdrawal = amount0Withdrawal;
    this.amount1Withdrawal = amount1Withdrawal;
    this.feeGrowthInside0Last = feeGrowthInside0Last;
    this.feeGrowthInside1Last = feeGrowthInside1Last;
    this.liquidity = BigDecimal.ZERO;
    this.stoppedAt = Instant.now().toEpochMilli();
    this.updatedAt = this.stoppedAt;

    return true;
  }

  public boolean markError(String errorMessage) {
    if (isError()) {
      return false;
    }

    this.status = STATUS_ERROR;
    this.errorMessage = errorMessage;
    this.updatedAt = Instant.now().toEpochMilli();

    return true;
  }

  public boolean isPending() {
    return STATUS_PENDING.equals(this.status);
  }

  public boolean isOpen() {
    return STATUS_OPEN.equals(this.status);
  }

  public boolean isClosed() {
    return STATUS_CLOSED.equals(this.status);
  }

  public boolean isError() {
    return STATUS_ERROR.equals(this.status);
  }

  public String getTickLowerKey() {
    return poolPair + "-" + tickLowerIndex;
  }

  public String getTickUpperKey() {
    return poolPair + "-" + tickUpperIndex;
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

  public Optional<Tick> getTickLower() {
    return getTickCache().getTick(getTickLowerKey());
  }

  public Optional<Tick> getTickUpper() {
    return getTickCache().getTick(getTickUpperKey());
  }

  /**
   * Lấy thông tin pool từ cache
   */
  public Optional<AmmPool> getPool() {
    return getAmmPoolCache().getAmmPool(poolPair);
  }

  /**
   * Lấy tick bitmap từ cache
   */
  public Optional<TickBitmap> getTickBitmap() {
    return getTickBitmapCache().getTickBitmap(poolPair);
  }

  public Map<String, Object> toMessageJson() {
    return JsonSerializer.toMap(this);
  }

  /**
   * Tính toán estimated liquidity dựa trên amount0Initial và amount1Initial,
   * tickLowerIndex, tickUpperIndex
   * và thông tin từ pool
   *
   * @return BigDecimal Estimated liquidity được tính toán hoặc BigDecimal.ZERO
   *         nếu pool không tồn tại
   */
  public BigDecimal calculateEstimatedLiquidity() {
    // Lấy thông tin pool
    Optional<AmmPool> poolOpt = getPool();

    if (poolOpt.isEmpty()) {
      return BigDecimal.ZERO;
    }

    AmmPool pool = poolOpt.get();

    // Lấy các giá trị sqrt price cho các tick
    BigDecimal sqrtRatioCurrentTick = TickMath.getSqrtRatioAtTick(pool.getCurrentTick());
    BigDecimal sqrtRatioLowerTick = TickMath.getSqrtRatioAtTick(tickLowerIndex);
    BigDecimal sqrtRatioUpperTick = TickMath.getSqrtRatioAtTick(tickUpperIndex);

    // Tính toán liquidity dựa trên các giá trị
    return LiquidityUtils.calculateLiquidityForAmounts(
        sqrtRatioCurrentTick,
        sqrtRatioLowerTick,
        sqrtRatioUpperTick,
        amount0Initial,
        amount1Initial);
  }
}
