import { Tick } from "../api/amm-ticks";

/**
 * Tính toán giá trị của một tick
 * @param tickIndex Chỉ số tick
 * @returns Giá trị của tick (token1/token0)
 */
function tickToPrice(tickIndex: number): number {
  // Trong Uniswap V3, giá = 1.0001^tick_index
  // Ví dụ: Nếu tick_index = 100600, giá = 1.0001^100600 ≈ 26000 VND/USDT
  return Math.pow(1.0001, tickIndex);
}

/**
 * Tính toán sqrt price từ tick index
 * @param tickIndex Chỉ số tick
 * @returns Giá trị căn bậc hai của giá
 */
function tickToSqrtPrice(tickIndex: number): number {
  return Math.sqrt(tickToPrice(tickIndex));
}

/**
 * Ước tính số lượng token nhận được khi swap (từ token0 sang token1)
 * @param {Object} poolData - Dữ liệu của pool từ API
 * @param {number} amountIn - Số lượng token đầu vào
 * @param {Tick[]} ticks - Dữ liệu ticks từ API
 * @return {Object} Kết quả ước tính
 */
export function estimateSwap0to1(
  poolData: {
    sqrt_price: string;
    fee_percentage: string;
    tick_spacing: number;
    tvl_in_token0: string;
    tvl_in_token1: string;
    price: string;
    current_tick_index?: number;
  },
  amountIn: number,
  ticks: Tick[] = []
): {
  amountOut: number;
  amountIn: number;
  fee: number;
  priceAfter: number;
  priceImpact: number;
  originalPrice: number;
  error?: string;
} {
  console.log("estimateSwap0to1", poolData, amountIn, ticks);
  // Trích xuất dữ liệu pool
  const sqrtPrice = parseFloat(poolData.sqrt_price);
  const feePercentage = parseFloat(poolData.fee_percentage);
  const price = parseFloat(poolData.price);

  // Đã trừ phí
  const amountInAfterFee = amountIn * (1 - feePercentage);

  // Tính liquidity từ TVL (đơn giản hóa)
  const tvl0 = parseFloat(poolData.tvl_in_token0);
  const tvl1 = parseFloat(poolData.tvl_in_token1);

  // Check for insufficient liquidity
  if (tvl0 === 0 || tvl1 === 0) {
    return {
      amountOut: 0,
      amountIn: amountIn,
      fee: amountIn * feePercentage,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Insufficient liquidity in the pool",
    };
  }

  // Tính liquidity chính xác hơn từ cả hai token
  let liquidity = Math.min(
    tvl0 * sqrtPrice, // L = tvl0 * √P
    tvl1 / sqrtPrice // L = tvl1 / √P
  );

  // Check if calculated liquidity is too low or invalid
  if (liquidity <= 0 || !isFinite(liquidity)) {
    return {
      amountOut: 0,
      amountIn: amountIn,
      fee: amountIn * feePercentage,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Calculated liquidity is too low",
    };
  }

  // Tính toán amountOut dựa trên giá thực tế (price = token1/token0)
  // Ví dụ: Nếu swap 100 USDT với giá 26000 VND/USDT, amountOut = 100 * 26000 = 2,600,000 VND
  // Đặt giá trị mặc định cho amountOut
  let amountOut = 0;

  // Biến để theo dõi số lượng token còn lại và giá hiện tại
  let remainingAmount = amountInAfterFee;
  let currentSqrtPrice = sqrtPrice;

  if (ticks.length > 0 && remainingAmount > 0) {
    // Sắp xếp ticks theo thứ tự tăng dần (từ thấp lên cao)
    const sortedTicks = [...ticks].sort((a, b) => a.tick_index - b.tick_index);

    // Lấy tick hiện tại từ pool data
    const currentTickIndex =
      poolData.current_tick_index ||
      Math.floor(Math.log(price) / Math.log(1.0001));

    console.log("Current tick index in estimateSwap0to1:", currentTickIndex);
    console.log(
      "Available ticks:",
      sortedTicks.map((t) => t.tick_index)
    );

    // Tìm các ticks nhỏ hơn tick hiện tại (khi swap 0->1, giá giảm, nên chúng ta cần các tick nhỏ hơn)
    const relevantTicks = sortedTicks
      .filter((tick) => tick.tick_index < currentTickIndex)
      .sort((a, b) => b.tick_index - a.tick_index); // Sort high to low

    console.log(
      "Relevant ticks:",
      relevantTicks.map((t) => t.tick_index)
    );

    console.log("Direction: USDT → VND (0→1)");
    console.log("Current tick:", currentTickIndex, "Price:", price);
    console.log(
      "Filtered ticks:",
      relevantTicks.map(
        (t) => `${t.tick_index} (liquidity: ${t.liquidity_net})`
      )
    );
    console.log(
      "Initial amountIn:",
      amountInAfterFee,
      "Initial liquidity:",
      liquidity
    );
    // debugger;

    // Nếu không có ticks phù hợp hoặc ticks không đúng, sử dụng Uniswap V3 math chính xác
    if (relevantTicks.length === 0) {
      console.log("No relevant ticks found, using exact Uniswap V3 math");

      // Sử dụng công thức Uniswap V3 chính xác với constant liquidity:
      // Δx = L * (1/√P_next - 1/√P_current)
      // => √P_next = 1 / (1/√P_current + Δx/L)
      const sqrtPriceAfter = 1 / (1 / sqrtPrice + amountInAfterFee / liquidity);

      // Δy = L * (√P_current - √P_next)
      amountOut = liquidity * (sqrtPrice - sqrtPriceAfter);
      currentSqrtPrice = sqrtPriceAfter;

      console.log("Exact calculation: amountOut =", amountOut);
    } else {
      // Xử lý từng tick một
      for (const tick of relevantTicks) {
        const nextSqrtPrice = tickToSqrtPrice(tick.tick_index);
        const liquidityDelta = parseFloat(tick.liquidity_net);

        // Công thức chính xác Uniswap V3 cho token0 input:
        // Δx = L * (1/√P_next - 1/√P_current)
        const amountNeeded =
          liquidity * (1 / nextSqrtPrice - 1 / currentSqrtPrice);

        console.log(`Processing tick ${tick.tick_index}:`, {
          currentSqrtPrice,
          nextSqrtPrice,
          amountNeeded,
          remainingAmount,
          liquidity,
        });

        if (remainingAmount <= amountNeeded) {
          // Không đủ token để đạt đến tick tiếp theo
          // Giải phương trình để tìm √P_next:
          // Δx = L * (1/√P_next - 1/√P_current)
          // => 1/√P_next = 1/√P_current + Δx/L
          // => √P_next = 1 / (1/√P_current + Δx/L)
          const sqrtPriceAfter =
            1 / (1 / currentSqrtPrice + remainingAmount / liquidity);

          // Tính lượng token1 nhận được:
          // Δy = L * (√P_current - √P_next)
          amountOut += liquidity * (currentSqrtPrice - sqrtPriceAfter);
          currentSqrtPrice = sqrtPriceAfter;
          remainingAmount = 0;
          break;
        } else {
          // Đủ token để đạt đến tick tiếp theo
          // Tính lượng token1 nhận được cho segment này:
          // Δy = L * (√P_current - √P_next)
          amountOut += liquidity * (currentSqrtPrice - nextSqrtPrice);

          // Trừ lượng token0 đã dùng
          remainingAmount -= amountNeeded;
          currentSqrtPrice = nextSqrtPrice;

          // CRITICALLY IMPORTANT: When crossing tick boundaries going DOWN in price
          // We SUBTRACT liquidity when the liquidityNet is positive
          liquidity -= liquidityDelta;

          console.log(`After processing tick ${tick.tick_index}:`, {
            currentSqrtPrice,
            newLiquidity: liquidity,
            remainingAmount,
            amountOutSoFar: amountOut,
          });
        }
      }

      // Xử lý số token còn lại nếu đã vượt qua tất cả các ticks
      if (remainingAmount > 0) {
        // Giải phương trình đúng cho Uniswap V3:
        // Δx = L * (1/√P_next - 1/√P_current)
        // => √P_next = 1 / (1/√P_current + Δx/L)
        const sqrtPriceAfter =
          1 / (1 / currentSqrtPrice + remainingAmount / liquidity);

        // Tính lượng token1 nhận được:
        // Δy = L * (√P_current - √P_next)
        amountOut += liquidity * (currentSqrtPrice - sqrtPriceAfter);
        currentSqrtPrice = sqrtPriceAfter;
      }
    }
  } else {
    // Nếu không có dữ liệu ticks, sử dụng Uniswap V3 math chính xác
    console.log("No ticks data, using exact Uniswap V3 math");

    // Sử dụng công thức Uniswap V3 chính xác với constant liquidity:
    // Δx = L * (1/√P_next - 1/√P_current)
    // => √P_next = 1 / (1/√P_current + Δx/L)
    const sqrtPriceAfter = 1 / (1 / sqrtPrice + amountInAfterFee / liquidity);

    // Δy = L * (√P_current - √P_next)
    amountOut = liquidity * (sqrtPrice - sqrtPriceAfter);
    currentSqrtPrice = sqrtPriceAfter;

    console.log("Exact calculation: amountOut =", amountOut);
  }

  // Tính giá sau khi swap
  const priceAfter = currentSqrtPrice * currentSqrtPrice;

  // Tính price impact
  const priceImpact = Math.abs((priceAfter - price) / price);
  console.log({ priceImpact, priceAfter, price, amountOut });

  // Phí thực tế
  const fee = amountIn * feePercentage;

  // Validate output amount
  if (amountOut <= 0 || !isFinite(amountOut)) {
    return {
      amountOut: 0,
      amountIn: amountIn,
      fee: fee,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Cannot calculate valid output amount",
    };
  }

  return {
    amountOut: amountOut,
    amountIn: amountIn,
    fee: fee,
    priceAfter: priceAfter,
    priceImpact: priceImpact,
    originalPrice: price,
  };
}

/**
 * Ước tính số lượng token nhận được khi swap (từ token1 sang token0)
 * @param {Object} poolData - Dữ liệu của pool từ API
 * @param {number} amountIn - Số lượng token đầu vào
 * @param {Tick[]} ticks - Dữ liệu ticks từ API
 * @return {Object} Kết quả ước tính
 */
export function estimateSwap1to0(
  poolData: {
    sqrt_price: string;
    fee_percentage: string;
    tick_spacing: number;
    tvl_in_token0: string;
    tvl_in_token1: string;
    price: string;
    current_tick_index?: number;
  },
  amountIn: number,
  ticks: Tick[] = []
): {
  amountOut: number;
  amountIn: number;
  fee: number;
  priceAfter: number;
  priceImpact: number;
  originalPrice: number;
  error?: string;
} {
  console.log("estimateSwap1to0", poolData, amountIn, ticks);
  // Trích xuất dữ liệu pool
  const sqrtPrice = parseFloat(poolData.sqrt_price);
  const feePercentage = parseFloat(poolData.fee_percentage);
  const price = parseFloat(poolData.price);

  // Đã trừ phí
  const amountInAfterFee = amountIn * (1 - feePercentage);

  // Tính liquidity từ TVL (đơn giản hóa)
  const tvl0 = parseFloat(poolData.tvl_in_token0);
  const tvl1 = parseFloat(poolData.tvl_in_token1);

  // Check for insufficient liquidity
  if (tvl0 === 0 || tvl1 === 0) {
    return {
      amountOut: 0,
      amountIn: amountIn,
      fee: amountIn * feePercentage,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Insufficient liquidity in the pool",
    };
  }

  // Tính liquidity chính xác hơn từ cả hai token
  let liquidity = Math.min(
    tvl0 * sqrtPrice, // L = tvl0 * √P
    tvl1 / sqrtPrice // L = tvl1 / √P
  );

  // Check if calculated liquidity is too low or invalid
  if (liquidity <= 0 || !isFinite(liquidity)) {
    return {
      amountOut: 0,
      amountIn: amountIn,
      fee: amountIn * feePercentage,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Calculated liquidity is too low",
    };
  }

  // Tính toán amountOut dựa trên giá thực tế (price = token1/token0)
  // Ví dụ: Nếu swap 2,600,000 VND với giá 26000 VND/USDT, amountOut = 2,600,000 / 26000 = 100 USDT
  // Đặt giá trị mặc định cho amountOut
  let amountOut = 0;

  // Biến để theo dõi số lượng token còn lại và giá hiện tại
  let remainingAmount = amountInAfterFee;
  let currentSqrtPrice = sqrtPrice;

  if (ticks.length > 0 && remainingAmount > 0) {
    // Sắp xếp ticks theo thứ tự tăng dần (từ thấp lên cao)
    const sortedTicks = [...ticks].sort((a, b) => a.tick_index - b.tick_index);

    // Lấy tick hiện tại từ pool data
    const currentTickIndex =
      poolData.current_tick_index ||
      Math.floor(Math.log(price) / Math.log(1.0001));

    console.log("Current tick index in estimateSwap1to0:", currentTickIndex);
    console.log(
      "Available ticks:",
      sortedTicks.map((t) => t.tick_index)
    );

    // In estimateSwap1to0 (VND→USDT - price increases)
    // We need ticks ABOVE current tick (ascending in order we'll hit them)
    const relevantTicks = sortedTicks
      .filter((tick) => tick.tick_index > currentTickIndex)
      .sort((a, b) => a.tick_index - b.tick_index); // Sort low to high

    console.log(
      "Relevant ticks:",
      relevantTicks.map((t) => t.tick_index)
    );

    console.log("Direction: VND → USDT (1→0)");
    console.log("Current tick:", currentTickIndex, "Price:", price);
    console.log(
      "Filtered ticks:",
      relevantTicks.map(
        (t) => `${t.tick_index} (liquidity: ${t.liquidity_net})`
      )
    );
    console.log(
      "Initial amountIn:",
      amountInAfterFee,
      "Initial liquidity:",
      liquidity
    );

    // Nếu không có ticks phù hợp hoặc ticks không đúng, sử dụng Uniswap V3 math chính xác
    if (relevantTicks.length === 0) {
      console.log("No relevant ticks found, using exact Uniswap V3 math");

      // Sử dụng công thức Uniswap V3 chính xác với constant liquidity:
      // Δy = L * (√P_next - √P_current)
      // => √P_next = √P_current + Δy/L
      const sqrtPriceAfter = sqrtPrice + amountInAfterFee / liquidity;

      // Δx = L * (1/√P_current - 1/√P_next)
      amountOut = liquidity * (1 / sqrtPrice - 1 / sqrtPriceAfter);
      currentSqrtPrice = sqrtPriceAfter;

      console.log("Exact calculation: amountOut =", amountOut);
    } else {
      // Xử lý từng tick một
      for (const tick of relevantTicks) {
        const nextSqrtPrice = tickToSqrtPrice(tick.tick_index);
        const liquidityDelta = parseFloat(tick.liquidity_net);

        // Công thức chính xác Uniswap V3 cho token1 input:
        // Δy = L * (√P_next - √P_current)
        const amountNeeded = liquidity * (nextSqrtPrice - currentSqrtPrice);

        console.log(`Processing tick ${tick.tick_index}:`, {
          currentSqrtPrice,
          nextSqrtPrice,
          amountNeeded,
          remainingAmount,
          liquidity,
        });

        if (remainingAmount <= amountNeeded) {
          // Không đủ token để đạt đến tick tiếp theo
          // Tính toán sqrt price mới dựa trên số lượng token còn lại
          // Δy = L * (√P_next - √P_current)
          // => √P_next = √P_current + Δy/L
          const sqrtPriceAfter = currentSqrtPrice + remainingAmount / liquidity;

          // Tính lượng token0 nhận được:
          // Δx = L * (1/√P_current - 1/√P_next)
          amountOut += liquidity * (1 / currentSqrtPrice - 1 / sqrtPriceAfter);
          currentSqrtPrice = sqrtPriceAfter;
          remainingAmount = 0;
          break;
        } else {
          // Đủ token để đạt đến tick tiếp theo
          // Tính lượng token0 nhận được:
          // Δx = L * (1/√P_current - 1/√P_next)
          amountOut += liquidity * (1 / currentSqrtPrice - 1 / nextSqrtPrice);

          // Trừ lượng token1 đã dùng
          remainingAmount -= amountNeeded;
          currentSqrtPrice = nextSqrtPrice;

          // Khi đi lên trong giá (VND->USDT), ta cộng liquidityNet
          liquidity += liquidityDelta;

          console.log(`After processing tick ${tick.tick_index}:`, {
            currentSqrtPrice,
            newLiquidity: liquidity,
            remainingAmount,
            amountOutSoFar: amountOut,
          });
        }
      }

      // Xử lý số token còn lại nếu đã vượt qua tất cả các ticks
      if (remainingAmount > 0) {
        // Tính toán sqrt price mới
        // Δy = L * (√P_next - √P_current)
        // => √P_next = √P_current + Δy/L
        const sqrtPriceAfter = currentSqrtPrice + remainingAmount / liquidity;

        // Tính lượng token0 nhận được:
        // Δx = L * (1/√P_current - 1/√P_next)
        amountOut += liquidity * (1 / currentSqrtPrice - 1 / sqrtPriceAfter);
        currentSqrtPrice = sqrtPriceAfter;
      }
    }
  } else {
    // Nếu không có dữ liệu ticks, sử dụng Uniswap V3 math chính xác
    console.log("No ticks data, using exact Uniswap V3 math");

    // Sử dụng công thức Uniswap V3 chính xác với constant liquidity:
    // Δy = L * (√P_next - √P_current)
    // => √P_next = √P_current + Δy/L
    const sqrtPriceAfter = sqrtPrice + amountInAfterFee / liquidity;

    // Δx = L * (1/√P_current - 1/√P_next)
    amountOut = liquidity * (1 / sqrtPrice - 1 / sqrtPriceAfter);
    currentSqrtPrice = sqrtPriceAfter;

    console.log("Exact calculation: amountOut =", amountOut);
  }

  // Tính giá sau khi swap
  const priceAfter = currentSqrtPrice * currentSqrtPrice;

  // Tính price impact
  const priceImpact = Math.abs((priceAfter - price) / price);

  // Phí thực tế
  const fee = amountIn * feePercentage;

  // Validate output amount
  if (amountOut <= 0 || !isFinite(amountOut)) {
    return {
      amountOut: 0,
      amountIn: amountIn,
      fee: fee,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Cannot calculate valid output amount",
    };
  }

  return {
    amountOut: amountOut,
    amountIn: amountIn,
    fee: fee,
    priceAfter: priceAfter,
    priceImpact: priceImpact,
    originalPrice: price,
  };
}

/**
 * Ước tính số lượng token nhận được khi swap (hỗ trợ cả exact input và exact output)
 * @param {Object} poolData - Dữ liệu của pool từ API
 * @param {number} amount - Số lượng token (input nếu exactOutput=false, output nếu exactOutput=true)
 * @param {boolean} zeroForOne - Hướng swap (true: USDT->VND, false: VND->USDT)
 * @param {Tick[]} ticks - Dữ liệu ticks từ API (tùy chọn)
 * @param {boolean} exactOutput - true: amount là output mong muốn, false: amount là input
 * @return {Object} Kết quả ước tính
 */
export function estimateSwapV3(
  poolData: {
    sqrt_price: string;
    fee_percentage: string;
    tick_spacing: number;
    tvl_in_token0: string;
    tvl_in_token1: string;
    price: string;
    current_tick_index?: number;
  },
  amount: number,
  zeroForOne: boolean,
  ticks: Tick[] = [],
  exactOutput: boolean = false
): {
  amountOut: number;
  amountIn: number;
  fee: number;
  priceAfter: number;
  priceImpact: number;
  originalPrice: number;
  error?: string;
} {
  console.log(
    "estimateSwapV3",
    poolData,
    amount,
    zeroForOne,
    ticks,
    exactOutput
  );

  if (!exactOutput) {
    // Exact Input - logic hiện tại
    if (zeroForOne) {
      return estimateSwap0to1(poolData, amount, ticks);
    } else {
      return estimateSwap1to0(poolData, amount, ticks);
    }
  } else {
    // Exact Output - logic mới
    if (zeroForOne) {
      return estimateSwap0to1ExactOutput(poolData, amount, ticks);
    } else {
      return estimateSwap1to0ExactOutput(poolData, amount, ticks);
    }
  }
}

/**
 * Ước tính số lượng token0 cần để nhận được amount token1 cố định (exact output 0->1)
 * @param {Object} poolData - Dữ liệu của pool từ API
 * @param {number} amountOut - Số lượng token1 muốn nhận
 * @param {Tick[]} ticks - Dữ liệu ticks từ API
 * @return {Object} Kết quả ước tính
 */
function estimateSwap0to1ExactOutput(
  poolData: {
    sqrt_price: string;
    fee_percentage: string;
    tick_spacing: number;
    tvl_in_token0: string;
    tvl_in_token1: string;
    price: string;
    current_tick_index?: number;
  },
  amountOut: number,
  ticks: Tick[] = []
): {
  amountOut: number;
  amountIn: number;
  fee: number;
  priceAfter: number;
  priceImpact: number;
  originalPrice: number;
  error?: string;
} {
  console.log("estimateSwap0to1ExactOutput", poolData, amountOut, ticks);

  const sqrtPrice = parseFloat(poolData.sqrt_price);
  const feePercentage = parseFloat(poolData.fee_percentage);
  const price = parseFloat(poolData.price);
  const tvl0 = parseFloat(poolData.tvl_in_token0);
  const tvl1 = parseFloat(poolData.tvl_in_token1);

  if (tvl0 === 0 || tvl1 === 0) {
    return {
      amountOut: amountOut,
      amountIn: 0,
      fee: 0,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Insufficient liquidity in the pool",
    };
  }

  let liquidity = Math.min(tvl0 * sqrtPrice, tvl1 / sqrtPrice);

  if (liquidity <= 0 || !isFinite(liquidity)) {
    return {
      amountOut: amountOut,
      amountIn: 0,
      fee: 0,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Calculated liquidity is too low",
    };
  }

  let amountIn = 0;
  let remainingAmount = amountOut;
  let currentSqrtPrice = sqrtPrice;

  if (ticks.length > 0 && remainingAmount > 0) {
    const sortedTicks = [...ticks].sort((a, b) => a.tick_index - b.tick_index);
    const currentTickIndex =
      poolData.current_tick_index ||
      Math.floor(Math.log(price) / Math.log(1.0001));

    // Exact output 0->1: giá giảm, cần ticks nhỏ hơn current tick
    const relevantTicks = sortedTicks
      .filter((tick) => tick.tick_index < currentTickIndex)
      .sort((a, b) => b.tick_index - a.tick_index);

    if (relevantTicks.length === 0) {
      // Sử dụng phương pháp đơn giản
      // Exact Output: amountIn = amountOut / price (trước fee)
      amountIn = amountOut / price;
      const sqrtPriceAfter = 1 / (1 / sqrtPrice + amountIn / liquidity);
      currentSqrtPrice = sqrtPriceAfter;
    } else {
      // Xử lý từng tick một
      for (const tick of relevantTicks) {
        const nextSqrtPrice = tickToSqrtPrice(tick.tick_index);
        const liquidityDelta = parseFloat(tick.liquidity_net);

        // Lượng token1 có thể nhận được trong segment này:
        // Δy = L * (√P_current - √P_next)
        const amountAvailable = liquidity * (currentSqrtPrice - nextSqrtPrice);

        if (remainingAmount <= amountAvailable) {
          // Đủ liquidity trong segment này
          // Tính √P_next từ Δy: √P_next = √P_current - Δy/L
          const sqrtPriceAfter = currentSqrtPrice - remainingAmount / liquidity;

          // Tính Δx cần thiết: Δx = L * (1/√P_next - 1/√P_current)
          amountIn += liquidity * (1 / sqrtPriceAfter - 1 / currentSqrtPrice);
          currentSqrtPrice = sqrtPriceAfter;
          remainingAmount = 0;
          break;
        } else {
          // Cần toàn bộ segment này
          // Δx = L * (1/√P_next - 1/√P_current)
          amountIn += liquidity * (1 / nextSqrtPrice - 1 / currentSqrtPrice);

          remainingAmount -= amountAvailable;
          currentSqrtPrice = nextSqrtPrice;
          liquidity -= liquidityDelta; // Trừ liquidity khi đi xuống
        }
      }

      // Xử lý phần còn lại
      if (remainingAmount > 0) {
        const sqrtPriceAfter = currentSqrtPrice - remainingAmount / liquidity;
        amountIn += liquidity * (1 / sqrtPriceAfter - 1 / currentSqrtPrice);
        currentSqrtPrice = sqrtPriceAfter;
      }
    }
  } else {
    // Phương pháp đơn giản
    amountIn = amountOut / price;
    const sqrtPriceAfter = 1 / (1 / sqrtPrice + amountIn / liquidity);
    currentSqrtPrice = sqrtPriceAfter;
  }

  // Thêm fee vào amountIn
  const amountInWithFee = amountIn / (1 - feePercentage);
  const fee = amountInWithFee * feePercentage;
  const priceAfter = currentSqrtPrice * currentSqrtPrice;
  const priceImpact = Math.abs((priceAfter - price) / price);

  if (amountIn <= 0 || !isFinite(amountIn)) {
    return {
      amountOut: amountOut,
      amountIn: 0,
      fee: 0,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Cannot calculate valid input amount",
    };
  }

  return {
    amountOut: amountOut,
    amountIn: amountInWithFee,
    fee: fee,
    priceAfter: priceAfter,
    priceImpact: priceImpact,
    originalPrice: price,
  };
}

/**
 * Ước tính số lượng token1 cần để nhận được amount token0 cố định (exact output 1->0)
 * @param {Object} poolData - Dữ liệu của pool từ API
 * @param {number} amountOut - Số lượng token0 muốn nhận
 * @param {Tick[]} ticks - Dữ liệu ticks từ API
 * @return {Object} Kết quả ước tính
 */
function estimateSwap1to0ExactOutput(
  poolData: {
    sqrt_price: string;
    fee_percentage: string;
    tick_spacing: number;
    tvl_in_token0: string;
    tvl_in_token1: string;
    price: string;
    current_tick_index?: number;
  },
  amountOut: number,
  ticks: Tick[] = []
): {
  amountOut: number;
  amountIn: number;
  fee: number;
  priceAfter: number;
  priceImpact: number;
  originalPrice: number;
  error?: string;
} {
  console.log("estimateSwap1to0ExactOutput", poolData, amountOut, ticks);

  const sqrtPrice = parseFloat(poolData.sqrt_price);
  const feePercentage = parseFloat(poolData.fee_percentage);
  const price = parseFloat(poolData.price);
  const tvl0 = parseFloat(poolData.tvl_in_token0);
  const tvl1 = parseFloat(poolData.tvl_in_token1);

  if (tvl0 === 0 || tvl1 === 0) {
    return {
      amountOut: amountOut,
      amountIn: 0,
      fee: 0,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Insufficient liquidity in the pool",
    };
  }

  let liquidity = Math.min(tvl0 * sqrtPrice, tvl1 / sqrtPrice);

  if (liquidity <= 0 || !isFinite(liquidity)) {
    return {
      amountOut: amountOut,
      amountIn: 0,
      fee: 0,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Calculated liquidity is too low",
    };
  }

  let amountIn = 0;
  let remainingAmount = amountOut;
  let currentSqrtPrice = sqrtPrice;

  if (ticks.length > 0 && remainingAmount > 0) {
    const sortedTicks = [...ticks].sort((a, b) => a.tick_index - b.tick_index);
    const currentTickIndex =
      poolData.current_tick_index ||
      Math.floor(Math.log(price) / Math.log(1.0001));

    // Exact output 1->0: giá tăng, cần ticks lớn hơn current tick
    const relevantTicks = sortedTicks
      .filter((tick) => tick.tick_index > currentTickIndex)
      .sort((a, b) => a.tick_index - b.tick_index);

    if (relevantTicks.length === 0) {
      // Sử dụng phương pháp đơn giản
      // Exact Output: amountIn = amountOut * price (trước fee)
      amountIn = amountOut * price;
      const sqrtPriceAfter = sqrtPrice + amountIn / liquidity;
      currentSqrtPrice = sqrtPriceAfter;
    } else {
      // Xử lý từng tick một
      for (const tick of relevantTicks) {
        const nextSqrtPrice = tickToSqrtPrice(tick.tick_index);
        const liquidityDelta = parseFloat(tick.liquidity_net);

        // Lượng token0 có thể nhận được trong segment này:
        // Δx = L * (1/√P_current - 1/√P_next)
        const amountAvailable =
          liquidity * (1 / currentSqrtPrice - 1 / nextSqrtPrice);

        if (remainingAmount <= amountAvailable) {
          // Đủ liquidity trong segment này
          // Tính √P_next từ Δx: 1/√P_next = 1/√P_current - Δx/L
          const sqrtPriceAfter =
            1 / (1 / currentSqrtPrice - remainingAmount / liquidity);

          // Tính Δy cần thiết: Δy = L * (√P_next - √P_current)
          amountIn += liquidity * (sqrtPriceAfter - currentSqrtPrice);
          currentSqrtPrice = sqrtPriceAfter;
          remainingAmount = 0;
          break;
        } else {
          // Cần toàn bộ segment này
          // Δy = L * (√P_next - √P_current)
          amountIn += liquidity * (nextSqrtPrice - currentSqrtPrice);

          remainingAmount -= amountAvailable;
          currentSqrtPrice = nextSqrtPrice;
          liquidity += liquidityDelta; // Cộng liquidity khi đi lên
        }
      }

      // Xử lý phần còn lại
      if (remainingAmount > 0) {
        const sqrtPriceAfter =
          1 / (1 / currentSqrtPrice - remainingAmount / liquidity);
        amountIn += liquidity * (sqrtPriceAfter - currentSqrtPrice);
        currentSqrtPrice = sqrtPriceAfter;
      }
    }
  } else {
    // Phương pháp đơn giản
    amountIn = amountOut * price;
    const sqrtPriceAfter = sqrtPrice + amountIn / liquidity;
    currentSqrtPrice = sqrtPriceAfter;
  }

  // Thêm fee vào amountIn
  const amountInWithFee = amountIn / (1 - feePercentage);
  const fee = amountInWithFee * feePercentage;
  const priceAfter = currentSqrtPrice * currentSqrtPrice;
  const priceImpact = Math.abs((priceAfter - price) / price);

  if (amountIn <= 0 || !isFinite(amountIn)) {
    return {
      amountOut: amountOut,
      amountIn: 0,
      fee: 0,
      priceAfter: 0,
      priceImpact: 0,
      originalPrice: price,
      error: "Cannot calculate valid input amount",
    };
  }

  return {
    amountOut: amountOut,
    amountIn: amountInWithFee,
    fee: fee,
    priceAfter: priceAfter,
    priceImpact: priceImpact,
    originalPrice: price,
  };
}
