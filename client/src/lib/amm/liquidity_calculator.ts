import Decimal from "decimal.js";

const BigDecimal = Decimal;

// Thiết lập mặc định scale và làm tròn
BigDecimal.set({
  precision: 40,
  rounding: BigDecimal.ROUND_HALF_UP,
});

export class LiquidityCalculator {
  static tickToSqrtPrice(tick: number): Decimal {
    const ratio = new BigDecimal("1.0001");
    return ratio.pow(tick / 2);
  }

  static getDecimalScale(symbol: string): number {
    const SUPPORTED_COINS: Record<string, number> = {
      USDT: 6,
      VND: 2,
      PHP: 2,
      NGN: 2,
    };
    return SUPPORTED_COINS[symbol.toUpperCase()] ?? 6;
  }

  static calculateAmounts(params: {
    tickLower: number;
    tickUpper: number;
    currentTick: number;
    amount0: number | null;
    amount1: number | null;
    token0Symbol: string;
    token1Symbol: string;
  }): { amount0: string; amount1: string; liquidity: string } {
    const {
      tickLower,
      tickUpper,
      currentTick,
      amount0 = null,
      amount1 = null,
      token0Symbol,
      token1Symbol,
    } = params;

    const token0Decimals = this.getDecimalScale(token0Symbol);
    const token1Decimals = this.getDecimalScale(token1Symbol);

    if (tickLower >= tickUpper) {
      console.error(
        "Invalid tick range: tickLower must be less than tickUpper",
      );
      return { amount0: "0", amount1: "0", liquidity: "0" };
    }

    if (
      (amount0 === null || amount0 <= 0) &&
      (amount1 === null || amount1 <= 0)
    ) {
      return { amount0: "0", amount1: "0", liquidity: "0" };
    }

    const sqrtPriceLower = this.tickToSqrtPrice(tickLower);
    const sqrtPriceUpper = this.tickToSqrtPrice(tickUpper);
    const sqrtPriceCurrent = this.tickToSqrtPrice(currentTick);

    let liquidity = new BigDecimal("0");
    let calculatedAmount0 = new BigDecimal("0");
    let calculatedAmount1 = new BigDecimal("0");

    if (currentTick < tickLower) {
      // Chỉ dùng token0
      if (amount0 !== null && amount0 > 0) {
        const amt = new BigDecimal(amount0.toString());
        liquidity = amt
          .mul(sqrtPriceLower)
          .mul(sqrtPriceUpper)
          .div(sqrtPriceUpper.minus(sqrtPriceLower));
      } else {
        return { amount0: "0", amount1: "0", liquidity: "0" };
      }
    } else if (currentTick > tickUpper) {
      // Chỉ dùng token1
      if (amount1 !== null && amount1 > 0) {
        const amt = new BigDecimal(amount1.toString());
        liquidity = amt.div(sqrtPriceUpper.minus(sqrtPriceLower));
      } else {
        return { amount0: "0", amount1: "0", liquidity: "0" };
      }
    } else {
      // Trong khoảng, cần cả 2 token
      if (amount0 !== null && amount0 > 0) {
        const amt0 = new BigDecimal(amount0.toString());
        liquidity = amt0
          .mul(sqrtPriceCurrent)
          .mul(sqrtPriceUpper)
          .div(sqrtPriceUpper.minus(sqrtPriceCurrent));
        calculatedAmount1 = liquidity.mul(
          sqrtPriceCurrent.minus(sqrtPriceLower),
        );
      } else if (amount1 !== null && amount1 > 0) {
        const amt1 = new BigDecimal(amount1.toString());
        liquidity = amt1.div(sqrtPriceCurrent.minus(sqrtPriceLower));
        calculatedAmount0 = liquidity
          .mul(sqrtPriceUpper.minus(sqrtPriceCurrent))
          .div(sqrtPriceUpper.mul(sqrtPriceCurrent));
      }
    }

    const result = {
      amount0:
        amount0 !== null && amount0 > 0
          ? amount0.toString()
          : calculatedAmount0.toFixed(token0Decimals),
      amount1:
        amount1 !== null && amount1 > 0
          ? amount1.toString()
          : calculatedAmount1.toFixed(token1Decimals),
      liquidity: liquidity.toFixed(0),
    };

    return result;
  }

  static calculateAmountsTest() {
    const params = {
      tickLower: 101660,
      tickUpper: 101680,
      currentTick: 101670,
      amount0: 100,
      amount1: null as number | null,
      token0Symbol: "USDT",
      token1Symbol: "VND",
    };

    const actualPrice = new BigDecimal("1.0001").pow(params.currentTick);

    console.log("🚀 Input:", {
      ...params,
      actualPrice: actualPrice.toFixed(10),
    });

    const result = this.calculateAmounts(params);
    console.log("✅ Result:", result);
  }
}
