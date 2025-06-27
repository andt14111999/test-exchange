import { BigNumber } from "bignumber.js";

// Constants
const FEE_TIER = 0.003; // 0.3% fee tier
const MIN_TICK = -887272;
const MAX_TICK = 887272;

// Export internal functions for testing
export function priceToSqrtPriceX96(
  price: BigNumber,
  decimals0: number,
  decimals1: number,
): BigNumber {
  const adjustedPrice = price
    .times(new BigNumber(2).pow(192))
    .times(new BigNumber(10).pow(decimals1 - decimals0));
  return adjustedPrice.sqrt();
}

export function getTickAtSqrtRatio(sqrtPriceX96: BigNumber): number {
  const tick = Math.floor(
    Math.log(sqrtPriceX96.toNumber()) / Math.log(Math.sqrt(1.0001)),
  );
  return Math.max(MIN_TICK, Math.min(tick, MAX_TICK));
}

// Core AMM functions
export interface Pool {
  token0: string;
  token1: string;
  fee: number;
  sqrtPriceX96: BigNumber;
  liquidity: BigNumber;
  tick: number;
}

export interface Position {
  pool: Pool;
  tickLower: number;
  tickUpper: number;
  liquidity: BigNumber;
}

export function calculateSwapOutput(
  pool: Pool,
  amountIn: BigNumber,
  zeroForOne: boolean,
): { amountOut: BigNumber; newSqrtPrice: BigNumber } {
  const liquidityX96 = pool.liquidity.times(new BigNumber(2).pow(96));

  if (zeroForOne) {
    // Swap token0 for token1
    const newSqrtPrice = pool.sqrtPriceX96.minus(
      amountIn.times(new BigNumber(2).pow(96)).div(liquidityX96),
    );
    const amountOut = liquidityX96
      .times(pool.sqrtPriceX96.minus(newSqrtPrice))
      .div(new BigNumber(2).pow(96));

    return {
      amountOut: amountOut.times(1 - FEE_TIER), // Apply fee
      newSqrtPrice,
    };
  } else {
    // Swap token1 for token0
    const newSqrtPrice = pool.sqrtPriceX96.plus(
      amountIn.times(new BigNumber(2).pow(96)).div(liquidityX96),
    );
    const amountOut = liquidityX96
      .times(newSqrtPrice.minus(pool.sqrtPriceX96))
      .div(new BigNumber(2).pow(96));

    return {
      amountOut: amountOut.times(1 - FEE_TIER), // Apply fee
      newSqrtPrice,
    };
  }
}

export function calculateOptimalAmounts(
  pool: Pool,
  amount0Desired: BigNumber,
  amount1Desired: BigNumber,
  tickLower: number,
  tickUpper: number,
): { amount0: BigNumber; amount1: BigNumber } {
  const sqrtRatioAX96 = priceToSqrtPriceX96(
    new BigNumber(1.0001).pow(tickLower),
    18,
    18,
  );
  const sqrtRatioBX96 = priceToSqrtPriceX96(
    new BigNumber(1.0001).pow(tickUpper),
    18,
    18,
  );

  // Calculate liquidity from token amounts
  const liquidity = calculateLiquidity(
    pool.sqrtPriceX96,
    sqrtRatioAX96,
    sqrtRatioBX96,
    amount0Desired,
    amount1Desired,
  );

  // Calculate optimal amounts based on current price
  const amount0 = calculateAmount0Delta(
    pool.sqrtPriceX96,
    sqrtRatioBX96,
    liquidity,
  );
  const amount1 = calculateAmount1Delta(
    sqrtRatioAX96,
    pool.sqrtPriceX96,
    liquidity,
  );

  return { amount0, amount1 };
}

export function calculateLiquidity(
  sqrtPrice: BigNumber,
  sqrtRatioA: BigNumber,
  sqrtRatioB: BigNumber,
  amount0: BigNumber,
  amount1: BigNumber,
): BigNumber {
  if (sqrtRatioA.gt(sqrtRatioB)) {
    [sqrtRatioA, sqrtRatioB] = [sqrtRatioB, sqrtRatioA];
  }

  if (sqrtPrice.lte(sqrtRatioA)) {
    return amount0
      .times(sqrtRatioA)
      .times(sqrtRatioB)
      .div(sqrtRatioB.minus(sqrtRatioA));
  } else if (sqrtPrice.lt(sqrtRatioB)) {
    const liquidity0 = amount0
      .times(sqrtRatioA)
      .times(sqrtRatioB)
      .div(sqrtRatioB.minus(sqrtRatioA));
    const liquidity1 = amount1.div(sqrtRatioB.minus(sqrtRatioA));
    return BigNumber.min(liquidity0, liquidity1);
  } else {
    return amount1.div(sqrtRatioB.minus(sqrtRatioA));
  }
}

export function calculateAmount0Delta(
  sqrtRatioA: BigNumber,
  sqrtRatioB: BigNumber,
  liquidity: BigNumber,
): BigNumber {
  if (sqrtRatioA.gt(sqrtRatioB)) {
    [sqrtRatioA, sqrtRatioB] = [sqrtRatioB, sqrtRatioA];
  }

  return liquidity
    .times(sqrtRatioB.minus(sqrtRatioA))
    .div(sqrtRatioA.times(sqrtRatioB));
}

export function calculateAmount1Delta(
  sqrtRatioA: BigNumber,
  sqrtRatioB: BigNumber,
  liquidity: BigNumber,
): BigNumber {
  if (sqrtRatioA.gt(sqrtRatioB)) {
    [sqrtRatioA, sqrtRatioB] = [sqrtRatioB, sqrtRatioA];
  }

  return liquidity.times(sqrtRatioB.minus(sqrtRatioA));
}

// Pool state management
export function createPool(
  token0: string,
  token1: string,
  initialPrice: BigNumber,
  initialLiquidity: BigNumber,
): Pool {
  const sqrtPriceX96 = priceToSqrtPriceX96(initialPrice, 18, 18);
  const tick = getTickAtSqrtRatio(sqrtPriceX96);

  return {
    token0,
    token1,
    fee: FEE_TIER,
    sqrtPriceX96,
    liquidity: initialLiquidity,
    tick,
  };
}

export function addLiquidity(
  pool: Pool,
  tickLower: number,
  tickUpper: number,
  amount0Desired: BigNumber,
  amount1Desired: BigNumber,
): Position {
  const { amount0, amount1 } = calculateOptimalAmounts(
    pool,
    amount0Desired,
    amount1Desired,
    tickLower,
    tickUpper,
  );

  const liquidity = calculateLiquidity(
    pool.sqrtPriceX96,
    priceToSqrtPriceX96(new BigNumber(1.0001).pow(tickLower), 18, 18),
    priceToSqrtPriceX96(new BigNumber(1.0001).pow(tickUpper), 18, 18),
    amount0,
    amount1,
  );

  return {
    pool: {
      ...pool,
      liquidity: pool.liquidity.plus(liquidity),
    },
    tickLower,
    tickUpper,
    liquidity,
  };
}
