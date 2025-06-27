package com.exchangeengine.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.exchangeengine.util.JsonSerializer;
import com.exchangeengine.util.ammPool.AmmPoolConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đại diện cho một tick trong hệ thống AMM.
 * Tick là các điểm giá nơi thanh khoản có thể được thêm vào hoặc rút ra.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tick {

  @NotBlank(message = "Pool pair is required")
  private String poolPair;

  private int tickIndex;

  /**
   * Tổng thanh khoản vị thế tham chiếu đến tick này
   */
  @NotNull
  private BigDecimal liquidityGross = BigDecimal.ZERO;

  /**
   * Lượng thanh khoản ròng được thêm vào (trừ đi) khi tick được vượt qua
   * từ trái sang phải (phải sang trái)
   */
  @NotNull
  private BigDecimal liquidityNet = BigDecimal.ZERO;

  /**
   * Tăng trưởng phí trên mỗi đơn vị thanh khoản ở phía bên kia của tick này cho
   * token0
   */
  @NotNull
  private BigDecimal feeGrowthOutside0 = BigDecimal.ZERO;

  /**
   * Tăng trưởng phí trên mỗi đơn vị thanh khoản ở phía bên kia của tick này cho
   * token1
   */
  @NotNull
  private BigDecimal feeGrowthOutside1 = BigDecimal.ZERO;

  /**
   * Timestamp when the tick was initialized (milliseconds since epoch)
   */
  private long tickInitializedTimestamp;

  /**
   * True if the tick is initialized (has been used)
   */
  private boolean initialized;

  /**
   * Creation timestamp
   */
  private long createdAt;

  /**
   * Last update timestamp
   */
  private long updatedAt;

  /**
   * Constructor with required poolPair and tickIndex
   */
  public Tick(String poolPair, int tickIndex) {
    this.poolPair = poolPair;
    this.tickIndex = tickIndex;
    this.tickInitializedTimestamp = System.currentTimeMillis();
    this.createdAt = System.currentTimeMillis();
    this.updatedAt = this.createdAt;
  }

  /**
   * Lấy khóa duy nhất cho tick này dựa trên pool và index
   */
  public String getTickKey() {
    return getPoolPair() + "-" + getTickIndex();
  }

  /**
   * Xác thực rằng tick này có tất cả các trường bắt buộc và giá trị hợp lệ
   */
  public List<String> validateRequiredFields() {
    List<String> errors = new ArrayList<>();

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<Tick>> violations = validator.validate(this);

    errors.addAll(violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList()));

    // Kiểm tra xem tick index có hợp lệ không (trong khoảng MIN_TICK và MAX_TICK)
    if (tickIndex < AmmPoolConfig.MIN_TICK || tickIndex > AmmPoolConfig.MAX_TICK) {
      errors.add("Tick index must be between " + AmmPoolConfig.MIN_TICK + " and " + AmmPoolConfig.MAX_TICK);
    }

    return errors;
  }

  /**
   * Tính toán trạng thái flip giữa trạng thái khởi tạo và chưa khởi tạo
   *
   * @param liquidityGrossAfter  Giá trị liquidityGross sau khi cập nhật
   * @param liquidityGrossBefore Giá trị liquidityGross trước khi cập nhật
   * @return true nếu trạng thái khởi tạo thay đổi (flip), ngược lại là false
   */
  public static boolean isFlipped(BigDecimal liquidityGrossAfter, BigDecimal liquidityGrossBefore) {
    boolean afterIsZero = liquidityGrossAfter.compareTo(BigDecimal.ZERO) == 0;
    boolean beforeIsZero = liquidityGrossBefore.compareTo(BigDecimal.ZERO) == 0;
    return afterIsZero != beforeIsZero;
  }

  /**
   * Cập nhật thanh khoản của tick khi vượt qua
   *
   * @param liquidityDelta   sự thay đổi thanh khoản
   * @param upper            true nếu cập nhật tick trên, false cho tick dưới
   * @param maxLiquidity     thanh khoản tối đa cho phép mỗi tick
   * @param tickCurrent      tick hiện tại của pool
   * @param feeGrowthGlobal0 tăng trưởng phí toàn cục cho token0
   * @param feeGrowthGlobal1 tăng trưởng phí toàn cục cho token1
   * @return true nếu tick chuyển từ khởi tạo sang chưa khởi tạo hoặc ngược lại
   */
  public boolean update(BigDecimal liquidityDelta, boolean upper, BigDecimal maxLiquidity,
      int tickCurrent, BigDecimal feeGrowthGlobal0, BigDecimal feeGrowthGlobal1) {
    BigDecimal liquidityGrossBefore = this.liquidityGross;

    // Cập nhật thanh khoản tổng - sử dụng giá trị tuyệt đối của delta
    BigDecimal absoluteDelta = liquidityDelta.abs();
    BigDecimal liquidityGrossAfter;

    // Nếu delta âm (rút thanh khoản), trừ đi từ gross
    if (liquidityDelta.compareTo(BigDecimal.ZERO) < 0) {
      liquidityGrossAfter = this.liquidityGross.subtract(absoluteDelta);
      // Đảm bảo không âm
      if (liquidityGrossAfter.compareTo(BigDecimal.ZERO) < 0) {
        liquidityGrossAfter = BigDecimal.ZERO;
      }
    } else {
      // Nếu delta dương (thêm thanh khoản), cộng vào gross
      liquidityGrossAfter = this.liquidityGross.add(absoluteDelta);
    }

    // Đảm bảo thanh khoản không vượt quá mức tối đa
    if (liquidityGrossAfter.compareTo(maxLiquidity) > 0) {
      throw new IllegalArgumentException("Liquidity exceeds maximum allowed");
    }

    // Kiểm tra nếu trạng thái của thanh khoản đã thay đổi (flipped)
    boolean beforeIsZero = liquidityGrossBefore.compareTo(BigDecimal.ZERO) == 0;
    boolean afterIsZero = liquidityGrossAfter.compareTo(BigDecimal.ZERO) == 0;
    boolean flipped = beforeIsZero != afterIsZero;

    this.liquidityGross = liquidityGrossAfter;

    // Nếu tick được khởi tạo lần đầu (liquidityGrossBefore = 0)
    if (beforeIsZero) {
      // Theo quy ước Uniswap, chúng ta giả định rằng tất cả fee growth trước khi
      // một tick được khởi tạo xảy ra _bên dưới_ tick
      if (this.tickIndex <= tickCurrent) {
        this.feeGrowthOutside0 = feeGrowthGlobal0;
        this.feeGrowthOutside1 = feeGrowthGlobal1;
        // Các biến khác như secondsPerLiquidityOutside, tickCumulativeOutside,
        // secondsOutside có thể được thêm trong tương lai khi cần
      }
      this.initialized = true;
      this.tickInitializedTimestamp = System.currentTimeMillis();
    }

    // Cập nhật thanh khoản ròng dựa trên cờ upper/lower
    if (upper) {
      this.liquidityNet = this.liquidityNet.subtract(liquidityDelta);
    } else {
      this.liquidityNet = this.liquidityNet.add(liquidityDelta);
    }

    this.updatedAt = System.currentTimeMillis();

    return flipped;
  }

  /**
   * Cập nhật thanh khoản của tick (phiên bản không có fee growth)
   *
   * @param liquidityDelta sự thay đổi thanh khoản
   * @param upper          true nếu cập nhật tick trên, false cho tick dưới
   * @param maxLiquidity   thanh khoản tối đa cho phép mỗi tick
   * @return true nếu tick chuyển từ khởi tạo sang chưa khởi tạo hoặc ngược lại
   */
  public boolean update(BigDecimal liquidityDelta, boolean upper, BigDecimal maxLiquidity) {
    // Sử dụng phiên bản đầy đủ của hàm update với các giá trị mặc định cho fee
    // growth
    return update(liquidityDelta, upper, maxLiquidity, 0, BigDecimal.ZERO, BigDecimal.ZERO);
  }

  /**
   * Xóa tất cả dữ liệu trong tick, đặt lại về giá trị mặc định
   */
  public void clear() {
    this.liquidityGross = BigDecimal.ZERO;
    this.liquidityNet = BigDecimal.ZERO;
    this.feeGrowthOutside0 = BigDecimal.ZERO;
    this.feeGrowthOutside1 = BigDecimal.ZERO;
    this.initialized = false;
    this.updatedAt = System.currentTimeMillis();
  }

  /**
   * Chuyển đổi tick thành JSON để gửi đi
   *
   * @return Map chứa thông tin của tick
   */

  public Map<String, Object> toMessageJson() {
    return JsonSerializer.toMap(this);
  }

  /**
   * Lật các biến tăng trưởng khi vượt qua một tick
   *
   * @param feeGrowthGlobal0 tăng trưởng phí toàn cục cho token0
   * @param feeGrowthGlobal1 tăng trưởng phí toàn cục cho token1
   * @return thay đổi thanh khoản ròng khi vượt qua tick này
   */
  public BigDecimal cross(BigDecimal feeGrowthGlobal0, BigDecimal feeGrowthGlobal1) {
    // Lật các tích lũy tăng trưởng phí bên ngoài
    this.feeGrowthOutside0 = feeGrowthGlobal0.subtract(this.feeGrowthOutside0);
    this.feeGrowthOutside1 = feeGrowthGlobal1.subtract(this.feeGrowthOutside1);

    this.updatedAt = System.currentTimeMillis();

    return this.liquidityNet;
  }
}
