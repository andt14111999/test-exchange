import { BigNumber } from "bignumber.js";

export interface Token {
  id: string;
  symbol: string;
  name: string;
  decimals: number;
}

export interface Pool {
  id: string;
  name: string;
  token0: Token;
  token1: Token;
  fee: number;
  sqrtPriceX96: BigNumber;
  liquidity: BigNumber;
  tickSpacing: number;
  maxLiquidityPerTick: BigNumber;
}

export const VND: Token = {
  id: "vnd",
  symbol: "VND",
  name: "Vietnamese Dong",
  decimals: 0,
};

export const USDT: Token = {
  id: "usdt",
  symbol: "USDT",
  name: "USD Tether",
  decimals: 6,
};

export const PHP: Token = {
  id: "php",
  symbol: "PHP",
  name: "Philippine Peso",
  decimals: 2,
};

export const NGN: Token = {
  id: "ngn",
  symbol: "NGN",
  name: "Nigerian Naira",
  decimals: 2,
};

export const POOLS: Pool[] = [
  {
    id: "vnd-usdt",
    name: "VND/USDT",
    token0: VND,
    token1: USDT,
    fee: 0.002,
    sqrtPriceX96: new BigNumber(25000),
    liquidity: new BigNumber(1000000),
    tickSpacing: 10,
    maxLiquidityPerTick: new BigNumber(10000000),
  },
  {
    id: "php-usdt",
    name: "PHP/USDT",
    token0: PHP,
    token1: USDT,
    fee: 0.002,
    sqrtPriceX96: new BigNumber(55), // Initial price of 55 PHP/USDT
    liquidity: new BigNumber(1000000),
    tickSpacing: 10,
    maxLiquidityPerTick: new BigNumber(10000000),
  },
  {
    id: "ngn-usdt",
    name: "NGN/USDT",
    token0: NGN,
    token1: USDT,
    fee: 0.002,
    sqrtPriceX96: new BigNumber(1500), // Initial price of 1500 NGN/USDT
    liquidity: new BigNumber(1000000),
    tickSpacing: 10,
    maxLiquidityPerTick: new BigNumber(10000000),
  },
];
