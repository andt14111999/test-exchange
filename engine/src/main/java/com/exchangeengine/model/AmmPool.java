package com.exchangeengine.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.exchangeengine.storage.cache.TickBitmapCache;
import com.exchangeengine.storage.cache.TickCache;
import com.exchangeengine.util.JsonSerializer;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import com.exchangeengine.util.ammPool.TickMath;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Automated Market Maker (AMM) Pool Model
 * Represents a liquidity pool for a trading pair in the AMM system
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmmPool {
  @NotBlank(message = "Trading pair is required")
  private String pair;

  private boolean isActive = false;

  @NotBlank(message = "Token0 is required")
  private String token0;

  @NotBlank(message = "Token1 is required")
  private String token1;

  @Positive(message = "Tick spacing must be greater than 0")
  private int tickSpacing; // Tick spacing (10, 60, 200...)

  @PositiveOrZero(message = "Fee percentage must be greater than or equal to 0")
  private double feePercentage; // Mức phí Ví dụ: 0.0005 = 0.05%, 0.003 = 0.3%, 0.01 = 1%

  @PositiveOrZero(message = "Protocol fee percentage must be greater than or equal to 0")
  private double feeProtocolPercentage = 0.0; // Mức phí giao thức Ví dụ: 0.05 = 5%

  @Positive(message = "initPrice must be positive")
  private BigDecimal initPrice;

  private int currentTick;

  @NotNull
  @PositiveOrZero(message = "Square root price must be greater than or equal to 0")
  private BigDecimal sqrtPrice = BigDecimal.ZERO;

  @NotNull
  @PositiveOrZero(message = "Price must be greater than or equal to 0")
  private BigDecimal price = BigDecimal.ZERO; // Giá thực tế, bằng sqrtPrice^2

  @NotNull
  @PositiveOrZero(message = "Liquidity must be greater than or equal to 0")
  private BigDecimal liquidity = BigDecimal.ZERO; // Thanh khoản hiện tại

  // Bộ tích lũy phí
  @NotNull
  @PositiveOrZero(message = "Fee growth global 0 must be greater than or equal to 0")
  private BigDecimal feeGrowthGlobal0 = BigDecimal.ZERO; // Phí token0 tích lũy

  @NotNull
  @PositiveOrZero(message = "Fee growth global 1 must be greater than or equal to 0")
  private BigDecimal feeGrowthGlobal1 = BigDecimal.ZERO; // Phí token1 tích lũy

  // Protocol fee
  @NotNull
  @PositiveOrZero(message = "Protocol fees 0 must be greater than or equal to 0")
  private BigDecimal protocolFees0 = BigDecimal.ZERO;

  @NotNull
  @PositiveOrZero(message = "Protocol fees 1 must be greater than or equal to 0")
  private BigDecimal protocolFees1 = BigDecimal.ZERO;

  // Thống kê giao dịch
  @NotNull
  @PositiveOrZero(message = "Volume token 0 must be greater than or equal to 0")
  private BigDecimal volumeToken0 = BigDecimal.ZERO; // Tổng khối lượng token0 đã giao dịch

  @NotNull
  @PositiveOrZero(message = "Volume token 1 must be greater than or equal to 0")
  private BigDecimal volumeToken1 = BigDecimal.ZERO; // Tổng khối lượng token1 đã giao dịch

  @NotNull
  @PositiveOrZero(message = "Volume USD must be greater than or equal to 0")
  private BigDecimal volumeUSD = BigDecimal.ZERO; // Tổng khối lượng USD đã giao dịch

  @PositiveOrZero(message = "Transaction count must be greater than or equal to 0")
  private int txCount; // Số lượng giao dịch đã thực hiện

  // Token reserves (trạng thái thực tế)
  @NotNull
  @PositiveOrZero(message = "Total value locked token 0 must be greater than or equal to 0")
  private BigDecimal totalValueLockedToken0 = BigDecimal.ZERO; // Tổng lượng token0 trong pool

  @NotNull
  @PositiveOrZero(message = "Total value locked token 1 must be greater than or equal to 0")
  private BigDecimal totalValueLockedToken1 = BigDecimal.ZERO; // Tổng lượng token1 trong pool

  private long createdAt = Instant.now().toEpochMilli();
  private long updatedAt = Instant.now().toEpochMilli();

  private String statusExplanation = "";

  public AmmPool(String pair) {
    this.pair = pair;
    this.createdAt = Instant.now().toEpochMilli();
    this.updatedAt = this.createdAt;
  }

  public List<String> validateRequiredFields() {
    List<String> errors = new ArrayList<>();

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<AmmPool>> violations = validator.validate(this);

    errors.addAll(violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList()));

    // Thêm các quy tắc validation tùy chỉnh không thể thực hiện bằng annotation
    // Kiểm tra token0 và token1 phải khác nhau
    if (token0 != null && token1 != null && token0.equals(token1)) {
      errors.add("Token0 and Token1 must be different");
    }
    // Kiểm tra mã token hợp lệ
    if (token0 != null && !token0.isEmpty()) {
      String validationToken0Msg = Coin.validateCoin(token0);
      if (!validationToken0Msg.isEmpty()) {
        errors.add("Token0: " + validationToken0Msg);
      }
    }

    if (token1 != null && !token1.isEmpty()) {
      String validationToken1Msg = Coin.validateCoin(token1);
      if (!validationToken1Msg.isEmpty()) {
        errors.add("Token1: " + validationToken1Msg);
      }
    }
    // Kiểm tra tick nằm trong phạm vi hợp lệ
    String tickValidation = AmmPoolConfig.validateTick(currentTick);
    if (!tickValidation.isEmpty()) {
      errors.add(tickValidation);
    }

    return errors;
  }

  public int priceToTick(BigDecimal price) {
    return TickMath.priceToTick(price);
  }

  public BigDecimal tickToPrice(int tick) {
    return TickMath.tickToPrice(tick);
  }

  public boolean hasUpdateField(boolean isActive, double feePercentage, double feeProtocolPercentage,
      BigDecimal initPrice) {
    boolean hasBasicFieldUpdate = this.isActive != isActive
        || (this.feePercentage != feePercentage && feePercentage >= 0)
        || (this.feeProtocolPercentage != feeProtocolPercentage && feeProtocolPercentage >= 0);

    boolean hasInitPriceUpdate = initPrice != null
        && (this.initPrice == null || !this.initPrice.equals(initPrice))
        && initPrice.compareTo(BigDecimal.ZERO) > 0;

    return hasBasicFieldUpdate || hasInitPriceUpdate;
  }

  public boolean update(boolean newActive, Double newFeePercentage, Double newFeeProtocolPercentage,
      BigDecimal newInitPrice) {
    boolean isChanged = false;

    if (this.isActive != newActive) {
      this.isActive = newActive;
      isChanged = true;
    }

    if (newFeePercentage != null && this.feePercentage != newFeePercentage && newFeePercentage >= 0) {
      this.feePercentage = newFeePercentage;
      isChanged = true;
    }

    if (newFeeProtocolPercentage != null && this.feeProtocolPercentage != newFeeProtocolPercentage
        && newFeeProtocolPercentage >= 0) {
      this.feeProtocolPercentage = newFeeProtocolPercentage;
      isChanged = true;
    }

    if (newInitPrice != null && this.initPrice != newInitPrice && newInitPrice.compareTo(BigDecimal.ZERO) > 0) {
      this.initPrice = newInitPrice;
      calculateInitialPriceAndTick();
      isChanged = true;
    }

    if (isChanged) {
      this.updatedAt = Instant.now().toEpochMilli();
    }

    return isChanged;
  }

  public void calculateInitialPriceAndTick() {
    if (totalValueLockedToken0.compareTo(BigDecimal.ZERO) == 0 &&
        totalValueLockedToken1.compareTo(BigDecimal.ZERO) == 0 &&
        initPrice != null &&
        initPrice.compareTo(BigDecimal.ZERO) > 0) {

      this.price = initPrice;
      this.currentTick = priceToTick(initPrice);
      this.sqrtPrice = initPrice.sqrt(AmmPoolConfig.MC);
    }
  }

  public Map<String, Object> toMessageJson() {
    return JsonSerializer.toMap(this);
  }

  /**
   * Cập nhật trạng thái pool khi thêm vị thế mới
   *
   * @param liquidity Liquidity cần thêm vào pool
   * @param isInRange Có nằm trong phạm vi active không (tickLower <= currentTick
   *                  < tickUpper)
   * @param amount0   Số lượng token0 được thêm vào pool
   * @param amount1   Số lượng token1 được thêm vào pool
   * @return true nếu có bất kỳ thay đổi nào, false nếu không
   */
  public boolean updateForAddPosition(BigDecimal liquidity, boolean isInRange,
      BigDecimal amount0, BigDecimal amount1) {
    boolean isChanged = false;

    // Chỉ cập nhật liquidity của pool khi position nằm trong phạm vi active
    if (isInRange && liquidity.compareTo(BigDecimal.ZERO) > 0) {
      this.liquidity = this.liquidity.add(liquidity);
      isChanged = true;
    }

    // Cập nhật tổng giá trị khóa cho token0 và token1
    if (amount0.compareTo(BigDecimal.ZERO) > 0) {
      this.totalValueLockedToken0 = this.totalValueLockedToken0.add(amount0);
      isChanged = true;
    }

    if (amount1.compareTo(BigDecimal.ZERO) > 0) {
      this.totalValueLockedToken1 = this.totalValueLockedToken1.add(amount1);
      isChanged = true;
    }

    // Tăng số giao dịch
    this.txCount++;

    // Cập nhật timestamp
    this.updatedAt = Instant.now().toEpochMilli();

    return isChanged;
  }

  /**
   * Cập nhật trạng thái pool khi một vị thế trong range được đóng
   * Phương thức này đơn giản hóa từ Uniswap V3, chỉ cập nhật các trường cần thiết
   *
   * @param removedLiquidity Lượng thanh khoản bị loại bỏ
   * @param amount0          Lượng token0 được rút ra
   * @param amount1          Lượng token1 được rút ra
   * @return true nếu cập nhật thành công, false nếu không
   */
  public boolean updateForClosePosition(BigDecimal removedLiquidity, BigDecimal amount0, BigDecimal amount1) {
    boolean isChanged = false;

    try {
      // 1. Giảm thanh khoản tổng
      if (removedLiquidity.compareTo(BigDecimal.ZERO) > 0) {
        this.liquidity = this.liquidity.subtract(removedLiquidity)
            .max(BigDecimal.ZERO) // Đảm bảo không âm
            .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
        isChanged = true;
      }

      // 2. Giảm tổng giá trị khóa cho token0 và token1
      if (amount0.compareTo(BigDecimal.ZERO) > 0) {
        this.totalValueLockedToken0 = this.totalValueLockedToken0.subtract(amount0)
            .max(BigDecimal.ZERO) // Đảm bảo không âm
            .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
        isChanged = true;
      }

      if (amount1.compareTo(BigDecimal.ZERO) > 0) {
        this.totalValueLockedToken1 = this.totalValueLockedToken1.subtract(amount1)
            .max(BigDecimal.ZERO) // Đảm bảo không âm
            .setScale(AmmPoolConfig.DECIMAL_SCALE, AmmPoolConfig.ROUNDING_MODE);
        isChanged = true;
      }

      // 3. Tăng số giao dịch
      this.txCount++;

      // 4. Cập nhật timestamp
      this.updatedAt = Instant.now().toEpochMilli();

      return isChanged;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Lấy TickBitmap cho pool từ cache
   *
   * @return TickBitmap của pool
   * @throws IllegalStateException nếu không tìm thấy TickBitmap
   */
  public TickBitmap getTickBitmap() {
    TickBitmapCache tickBitmapCache = TickBitmapCache.getInstance();
    return tickBitmapCache.getTickBitmap(getPair())
        .orElseThrow(() -> new IllegalStateException("TickBitmap not found for pool: " + getPair()));
  }

  /**
   * Lấy Tick theo index từ cache
   *
   * @param tickIndex Index của tick cần lấy
   * @return Tick tương ứng với index
   * @throws IllegalStateException nếu không tìm thấy Tick
   */
  public Tick getTick(int tickIndex) {
    String tickKey = getPair() + "-" + tickIndex;
    TickCache tickCache = TickCache.getInstance();
    return tickCache.getTick(tickKey)
        .orElseThrow(() -> new IllegalStateException("Tick not found for key: " + tickKey));
  }

  /**
   * Cập nhật trạng thái pool sau khi swap
   *
   * @param tick                   Tick hiện tại sau swap
   * @param sqrtPrice              Giá sqrt hiện tại sau swap
   * @param liquidity              Thanh khoản mới sau swap
   * @param feeGrowthGlobal0       Phí tăng trưởng toàn cục cho token0
   * @param feeGrowthGlobal1       Phí tăng trưởng toàn cục cho token1
   * @param totalValueLockedToken0 Giá trị token0 bị khóa sau swap
   * @param totalValueLockedToken1 Giá trị token1 bị khóa sau swap
   * @param volumeToken0           Khối lượng token0 tích lũy
   * @param volumeToken1           Khối lượng token1 tích lũy
   */
  public void updatePoolAfterSwap(
      int tick,
      BigDecimal sqrtPrice,
      BigDecimal liquidity,
      BigDecimal feeGrowthGlobal0,
      BigDecimal feeGrowthGlobal1,
      BigDecimal totalValueLockedToken0,
      BigDecimal totalValueLockedToken1,
      BigDecimal volumeToken0,
      BigDecimal volumeToken1) {

    // Kiểm tra tham số null
    if (sqrtPrice == null || liquidity == null ||
        feeGrowthGlobal0 == null || feeGrowthGlobal1 == null ||
        totalValueLockedToken0 == null || totalValueLockedToken1 == null ||
        volumeToken0 == null || volumeToken1 == null) {
      return;
    }

    // 1. Cập nhật giá và tick
    this.currentTick = tick;
    this.sqrtPrice = sqrtPrice;
    // Tính price từ sqrtPrice để đảm bảo tính nhất quán
    this.price = sqrtPrice.pow(2, AmmPoolConfig.MC).setScale(AmmPoolConfig.DISPLAY_SCALE, AmmPoolConfig.ROUNDING_MODE);

    // 2. Cập nhật thanh khoản nếu thay đổi
    if (this.liquidity.compareTo(liquidity) != 0) {
      this.liquidity = liquidity;
    }

    // 3. Cập nhật phí
    this.feeGrowthGlobal0 = feeGrowthGlobal0;
    this.feeGrowthGlobal1 = feeGrowthGlobal1;

    // 4. Cập nhật TVL
    this.totalValueLockedToken0 = totalValueLockedToken0;
    this.totalValueLockedToken1 = totalValueLockedToken1;

    // 5. Cập nhật volume
    this.volumeToken0 = volumeToken0;
    this.volumeToken1 = volumeToken1;

    // 6. Tăng txCount và cập nhật thời gian
    this.txCount++;
    this.updatedAt = System.currentTimeMillis();
  }
}
