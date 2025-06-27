/**
 * Interface cho việc thêm thanh khoản (position)
 */
export interface AMMPosition {
  /** Cặp token của pool (ví dụ: "usdt_vnd") */
  pool_pair: string;

  /** Tick index thấp nhất của range */
  tick_lower_index: number;

  /** Tick index cao nhất của range */
  tick_upper_index: number;

  /** Số lượng token0 ban đầu */
  amount0_initial: string;

  /** Số lượng token1 ban đầu */
  amount1_initial: string;

  /** Slippage tolerance (basis points, default: 100 = 1%) */
  slippage: number;
}

/**
 * Interface cho phạm vi giá (price range)
 */
export interface PriceRange {
  /** Giá tối thiểu */
  min: string;

  /** Giá tối đa */
  max: string;
}

/**
 * Interface cho phạm vi tick (tick range)
 */
export interface TickRange {
  /** Tick thấp nhất */
  lower: number;

  /** Tick cao nhất */
  upper: number;
}

/**
 * Interface cho số lượng token
 */
export interface TokenAmounts {
  /** Số lượng token0 */
  token0: string;

  /** Số lượng token1 */
  token1: string;
}

/**
 * Interface cho số lượng token đã format
 */
export interface FormattedTokenAmounts {
  /** Số lượng token0 đã format (có dấu phân cách hàng nghìn) */
  token0: string;

  /** Số lượng token1 đã format (có dấu phân cách hàng nghìn) */
  token1: string;
}

/**
 * Interface cho lỗi nhập liệu
 */
export interface ValidationErrors {
  /** Lỗi khi nhập số lượng token0 */
  token0?: string;

  /** Lỗi khi nhập số lượng token1 */
  token1?: string;

  /** Lỗi khi thiết lập phạm vi tick */
  tickRange?: string;
}
