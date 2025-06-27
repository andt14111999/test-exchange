import { TickMath } from "./tick-math";
import { FormattedPool } from "../api/pools";
import { AMMPosition, TokenAmounts, ValidationErrors } from "./types";
import { LiquidityCalculator } from "./liquidity_calculator";

export const MIN_TICK = -887272;
export const MAX_TICK = 887272;

/**
 * Tính toán tick range dựa trên giá hiện tại và phần trăm
 */
export function calculateTickRange(
  currentTick: number,
  tickSpacing: number,
  percentage: number = 5,
): { tickLower: number; tickUpper: number } {
  // Tính toán price range dựa trên phần trăm
  const currentPrice = TickMath.tickToPrice(currentTick);
  const lowerPrice = currentPrice * (1 - percentage / 100);
  const upperPrice = currentPrice * (1 + percentage / 100);

  // Chuyển đổi giá thành tick
  let tickLower = TickMath.priceToTick(lowerPrice);
  let tickUpper = TickMath.priceToTick(upperPrice);

  // Làm tròn theo tick spacing
  tickLower = TickMath.roundToTickSpacing(tickLower, tickSpacing);
  tickUpper = TickMath.roundToTickSpacing(tickUpper, tickSpacing, true);

  // Đảm bảo tickLower < tickUpper
  if (tickLower >= tickUpper) {
    tickUpper = tickLower + tickSpacing;
  }

  return { tickLower, tickUpper };
}

/**
 * Tính toán số lượng token dựa trên tick range và số lượng token đầu vào
 */
export function calculateTokenAmounts(
  pool: FormattedPool,
  tickLower: number,
  tickUpper: number,
  inputToken: "token0" | "token1",
  inputAmount: number,
): TokenAmounts {
  const result = LiquidityCalculator.calculateAmounts({
    tickLower,
    tickUpper,
    currentTick: pool.currentTick,
    amount0: inputToken === "token0" ? inputAmount : null,
    amount1: inputToken === "token1" ? inputAmount : null,
    token0Symbol: pool.token0,
    token1Symbol: pool.token1,
  });

  return {
    token0: result.amount0.toString(),
    token1: result.amount1.toString(),
  };
}

/**
 * Tạo đối tượng AMMPosition từ dữ liệu đầu vào
 */
export function createAMMPosition(
  pool: FormattedPool,
  tickLower: number,
  tickUpper: number,
  amount0: string,
  amount1: string,
  slippage: number = 100,
): AMMPosition {
  return {
    pool_pair: pool.pair,
    tick_lower_index: tickLower,
    tick_upper_index: tickUpper,
    amount0_initial: amount0,
    amount1_initial: amount1,
    slippage,
  };
}

/**
 * Kiểm tra xem position có hợp lệ không
 */
export function validatePosition(
  pool: FormattedPool | null,
  tickLower: number,
  tickUpper: number,
  amount0: string,
  amount1: string,
): { isValid: boolean; errors: ValidationErrors } {
  const errors: ValidationErrors = {};

  if (!pool) {
    return { isValid: false, errors: { tickRange: "Pool không tồn tại" } };
  }

  // Kiểm tra tick range
  if (tickLower >= tickUpper) {
    errors.tickRange = "Giá thấp nhất phải nhỏ hơn giá cao nhất";
  }

  // Kiểm tra số lượng token
  const numAmount0 = parseFloat(amount0);
  const numAmount1 = parseFloat(amount1);

  if (isNaN(numAmount0) || numAmount0 <= 0) {
    errors.token0 = "Số lượng token không hợp lệ";
  }

  if (isNaN(numAmount1) || numAmount1 <= 0) {
    errors.token1 = "Số lượng token không hợp lệ";
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
}

/**
 * Format số với dấu phân cách hàng nghìn
 */
export function formatNumberWithCommas(value: string): string {
  // Loại bỏ tất cả dấu phân cách hiện có
  const plainNumber = value.replace(/,/g, "");

  // Nếu là số thập phân, định dạng phần nguyên
  if (plainNumber.includes(".")) {
    const [integer, decimal] = plainNumber.split(".");
    return `${integer.replace(/\B(?=(\d{3})+(?!\d))/g, ",")}.${decimal}`;
  }

  // Nếu là số nguyên
  return plainNumber.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

/**
 * Chuyển từ chuỗi đã định dạng sang số
 */
export function parseFormattedNumber(formattedValue: string): number {
  // Loại bỏ tất cả dấu phân cách
  const plainNumber = formattedValue.replace(/,/g, "");
  return parseFloat(plainNumber) || 0;
}

/**
 * Định dạng giá hiển thị
 */
export function formatDisplayPrice(
  price: number,
  isLargeNumber: boolean = false,
  decimals: number = 2,
): string {
  if (price === 0) return "0";

  // Nếu số lớn hơn 1, làm tròn đến số chữ số thập phân được chỉ định
  // Nếu số nhỏ hơn 1, giữ đến 6 chữ số thập phân hoặc số được chỉ định
  return isLargeNumber || price >= 1
    ? price.toLocaleString("en-US", {
        maximumFractionDigits: decimals,
        useGrouping: true,
      })
    : price.toLocaleString("en-US", {
        maximumFractionDigits: Math.max(6, decimals),
        minimumFractionDigits: 2,
        useGrouping: true,
      });
}

/**
 * Định dạng số hiển thị với độ chính xác hợp lý
 * Giới hạn số chữ số thập phân để hiển thị gọn hơn
 */
export function formatDisplayNumberWithPrecision(
  value: string | number,
  maxDecimals: number = 4,
): string {
  // Chuyển đổi giá trị thành số
  let numValue: number;
  if (typeof value === "string") {
    // Loại bỏ dấu phẩy
    numValue = parseFloat(value.replace(/,/g, ""));
  } else {
    numValue = value;
  }

  // Nếu không phải số hợp lệ
  if (isNaN(numValue) || !isFinite(numValue)) {
    return "0";
  }

  // Số nguyên không cần xử lý thập phân
  if (Number.isInteger(numValue)) {
    return numValue.toLocaleString();
  }

  // Xử lý số thập phân
  const options = {
    minimumFractionDigits: 0,
    maximumFractionDigits: maxDecimals,
    useGrouping: true,
  };

  return numValue.toLocaleString(undefined, options);
}
