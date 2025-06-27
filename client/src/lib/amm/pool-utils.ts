import { ActivePool } from "@/lib/api/pools";

/**
 * Lấy danh sách token không trùng lặp từ danh sách pools
 * Trả về hai danh sách token0 và token1
 */
export function getUniqueTokens(pools: ActivePool[]) {
  if (!pools || pools.length === 0) return { token0List: [], token1List: [] };

  const token0Set = new Set<string>();
  const token1Set = new Set<string>();

  pools.forEach((pool) => {
    token0Set.add(pool.token0.toUpperCase());
    token1Set.add(pool.token1.toUpperCase());
  });

  return {
    token0List: Array.from(token0Set),
    token1List: Array.from(token1Set),
  };
}

/**
 * Tìm pool dựa vào cặp token đầu vào
 * Không phân biệt thứ tự token0/token1
 */
export function findPoolPair(
  pools: ActivePool[],
  token0: string,
  token1: string,
) {
  if (!pools || pools.length === 0) return null;

  // Case insensitive search
  const t0 = token0.toLowerCase();
  const t1 = token1.toLowerCase();

  const foundPool = pools.find(
    (pool) =>
      (pool.token0.toLowerCase() === t0 && pool.token1.toLowerCase() === t1) ||
      (pool.token0.toLowerCase() === t1 && pool.token1.toLowerCase() === t0),
  );

  return foundPool || null;
}

/**
 * Định dạng giá theo cần thiết
 */
export function formatPoolPrice(price: number | string): string {
  try {
    const numPrice = Number(price);
    if (isNaN(numPrice)) return "0";

    return numPrice.toLocaleString("en-US", {
      minimumFractionDigits: numPrice > 1 ? 2 : 6,
      maximumFractionDigits: numPrice > 1 ? 2 : 6,
    });
  } catch {
    return "0";
  }
}
