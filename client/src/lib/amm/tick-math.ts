/**
 * Lớp tiện ích cho việc tính toán giữa tick và price trong AMM
 */
export class TickMath {
  /** Tick nhỏ nhất có thể */
  static readonly MIN_TICK = -887272;

  /** Tick lớn nhất có thể */
  static readonly MAX_TICK = 887272;

  /** Hệ số nhân giữa các tick (1.0001) */
  static readonly TICK_MULTIPLIER = 1.0001;

  /**
   * Chuyển đổi price thành tick
   * @param price Giá cần chuyển đổi
   * @returns Giá trị tick
   */
  static priceToTick(price: number): number {
    if (price <= 0) {
      console.warn("Invalid price for priceToTick:", price);
      return 0;
    }

    const tick = Math.floor(Math.log(price) / Math.log(this.TICK_MULTIPLIER));
    return Math.max(this.MIN_TICK, Math.min(tick, this.MAX_TICK));
  }

  /**
   * Chuyển đổi tick thành price
   * @param tick Giá trị tick cần chuyển đổi
   * @returns Giá
   */
  static tickToPrice(tick: number): number {
    try {
      const boundedTick = Math.max(
        this.MIN_TICK,
        Math.min(tick, this.MAX_TICK),
      );
      return Math.pow(this.TICK_MULTIPLIER, boundedTick);
    } catch (error) {
      console.error("Error in tickToPrice:", error);
      return 0;
    }
  }

  /**
   * Làm tròn tick đến bội số gần nhất của tickSpacing
   * @param tick Tick cần làm tròn
   * @param tickSpacing Khoảng cách tick
   * @param roundUp Làm tròn lên hay xuống
   * @returns Tick đã làm tròn
   */
  static roundToTickSpacing(
    tick: number,
    tickSpacing: number,
    roundUp: boolean = false,
  ): number {
    if (tickSpacing <= 0) {
      console.warn("Invalid tickSpacing:", tickSpacing);
      return tick;
    }

    // Nếu roundUp được chỉ định, làm tròn lên
    if (roundUp) {
      return Math.ceil(tick / tickSpacing) * tickSpacing;
    }

    // Mặc định, làm tròn đến giá trị gần nhất thay vì luôn làm tròn xuống
    const remainder = tick % tickSpacing;
    if (remainder === 0) return tick;

    // Nếu phần dư lớn hơn một nửa tickSpacing, làm tròn lên
    if (Math.abs(remainder) > tickSpacing / 2) {
      return (
        Math.sign(tick) * Math.ceil(Math.abs(tick) / tickSpacing) * tickSpacing
      );
    }
    // Nếu không, làm tròn xuống
    return (
      Math.sign(tick) * Math.floor(Math.abs(tick) / tickSpacing) * tickSpacing
    );
  }

  /**
   * Tính toán phạm vi tick dựa trên phần trăm so với giá hiện tại
   * @param currentTick Tick hiện tại
   * @param percentage Phần trăm (ví dụ: 5 cho 5%)
   * @param tickSpacing Khoảng cách tick
   * @returns Phạm vi tick (tickLower và tickUpper)
   */
  static calculateTickRange(
    currentTick: number,
    percentage: number,
    tickSpacing: number,
  ): { tickLower: number; tickUpper: number } {
    // Tính toán phạm vi giá dựa trên phần trăm
    const currentPrice = this.tickToPrice(currentTick);

    if (currentPrice <= 0) {
      console.warn("Invalid currentPrice in calculateTickRange:", currentPrice);
      return { tickLower: 0, tickUpper: tickSpacing };
    }

    const lowerPrice = currentPrice * (1 - percentage / 100);
    const upperPrice = currentPrice * (1 + percentage / 100);

    // Chuyển đổi giá thành tick
    let tickLower = this.priceToTick(lowerPrice);
    let tickUpper = this.priceToTick(upperPrice);

    // Làm tròn đến tick spacing hợp lệ
    tickLower = this.roundToTickSpacing(tickLower, tickSpacing);
    tickUpper = this.roundToTickSpacing(tickUpper, tickSpacing, true);

    // Đảm bảo tickLower < tickUpper
    if (tickLower >= tickUpper) {
      tickUpper = tickLower + tickSpacing;
    }

    return { tickLower, tickUpper };
  }

  /**
   * Kiểm tra xem tickLower có lớn hơn tickUpper không
   * @param tickLower Tick thấp
   * @param tickUpper Tick cao
   * @returns true nếu tickLower > tickUpper, false nếu không
   */
  static isInvalidTickRange(tickLower: number, tickUpper: number): boolean {
    return tickLower >= tickUpper;
  }

  /**
   * Sửa lại phạm vi tick không hợp lệ
   * @param tickLower Tick thấp
   * @param tickUpper Tick cao
   * @param tickSpacing Khoảng cách tick
   * @returns Phạm vi tick đã sửa
   */
  static fixInvalidTickRange(
    tickLower: number,
    tickUpper: number,
    tickSpacing: number,
  ): { tickLower: number; tickUpper: number } {
    if (tickLower >= tickUpper) {
      return { tickLower, tickUpper: tickLower + tickSpacing };
    }
    return { tickLower, tickUpper };
  }

  /**
   * Tính toán số lượng token1 tương ứng với số lượng token0 và giá
   * @param token0Amount Số lượng token0
   * @param price Giá (token1/token0)
   * @returns Số lượng token1 tương ứng
   */
  static calculateToken1Amount(token0Amount: number, price: number): number {
    if (
      isNaN(token0Amount) ||
      token0Amount <= 0 ||
      isNaN(price) ||
      price <= 0
    ) {
      return 0;
    }

    // Công thức: token1 = token0 * giá
    return token0Amount * price;
  }

  /**
   * Tính toán số lượng token0 tương ứng với số lượng token1 và giá
   * @param token1Amount Số lượng token1
   * @param price Giá (token1/token0)
   * @returns Số lượng token0 tương ứng
   */
  static calculateToken0Amount(token1Amount: number, price: number): number {
    if (
      isNaN(token1Amount) ||
      token1Amount <= 0 ||
      isNaN(price) ||
      price <= 0
    ) {
      return 0;
    }

    // Công thức: token0 = token1 / giá
    return token1Amount / price;
  }

  /**
   * Format giá để hiển thị
   * @param price Giá cần format
   * @returns Chuỗi giá đã được format
   */
  static formatPrice(price: number): string {
    try {
      if (!isFinite(price) || price < 0) {
        return "0";
      }
      return new Intl.NumberFormat().format(Math.round(price));
    } catch (error) {
      console.error("Error in formatPrice:", error);
      return "0";
    }
  }
}
