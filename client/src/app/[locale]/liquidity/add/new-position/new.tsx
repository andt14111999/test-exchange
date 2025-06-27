"use client";

import { useState, useCallback, useEffect } from "react";
import { FormattedPool } from "@/lib/api/pools";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import {
  calculateTickRange,
  MIN_TICK,
  MAX_TICK,
} from "@/lib/amm/position-utils";
import { createPosition, fetchPositions } from "@/lib/api/positions";
import { AxiosError } from "axios";
import { useQueryClient } from "@tanstack/react-query";

// Import các component con
import { Header } from "./header";
import { PoolInfo } from "./pool-info";
import { SelectPriceRange } from "./select-price-range";
import { TokensInput } from "./token-input";
import { SlippageSelector } from "./slippage-selector";

// Định nghĩa Position
interface Position {
  pool_pair: string;
  tick_lower_index: number;
  tick_upper_index: number;
  amount0_initial: string;
  amount1_initial: string;
  slippage: number;
}

interface NewPositionProps {
  pool: FormattedPool;
  getTokenBalance: (token: string) => number;
}

export function NewPosition({ pool, getTokenBalance }: NewPositionProps) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();

  // State của position
  const [position, setPosition] = useState<Position>({
    pool_pair: pool.pair,
    tick_lower_index: 0,
    tick_upper_index: 0,
    amount0_initial: "0",
    amount1_initial: "0",
    slippage: 100,
  });

  const [isSubmitting, setIsSubmitting] = useState(false);

  // Khởi tạo giá trị mặc định khi component mount
  useEffect(() => {
    try {
      // Thiết lập ±10% từ tick hiện tại
      if (pool.currentTick && pool.tickSpacing) {
        const { tickLower, tickUpper } = calculateTickRange(
          pool.currentTick,
          pool.tickSpacing,
          10,
        );

        // Cập nhật tick range
        setPosition((prev) => ({
          ...prev,
          tick_lower_index: tickLower,
          tick_upper_index: tickUpper,
        }));
      }
    } catch (error) {
      console.error("Error setting default tick range:", error);
    }
  }, [pool]);

  // Xử lý khi tick range thay đổi
  const handleTickRangeChange = useCallback(
    (lowerTick: number, upperTick: number) => {
      setPosition((prev) => ({
        ...prev,
        tick_lower_index: lowerTick,
        tick_upper_index: upperTick,
      }));
    },
    [],
  );

  // Xử lý khi token values thay đổi
  const handleTokenValuesChange = useCallback(
    (amount0: string, amount1: string) => {
      setPosition((prev) => ({
        ...prev,
        amount0_initial: amount0,
        amount1_initial: amount1,
      }));
    },
    [],
  );

  // Cập nhật slippage
  const handleSlippageChange = useCallback((newSlippage: number) => {
    setPosition((prev) => ({ ...prev, slippage: newSlippage }));
  }, []);

  // Xử lý submit form
  const handleSubmit = useCallback(async () => {
    if (!pool) return;

    // Kiểm tra số dư
    const balance0 = getTokenBalance(pool.token0);
    const balance1 = getTokenBalance(pool.token1);

    const amount0 = parseFloat(position.amount0_initial);
    const amount1 = parseFloat(position.amount1_initial);

    if (amount0 <= 0 || amount1 <= 0) {
      toast.error(t("liquidity.invalidAmount"));
      return;
    }

    if (amount0 > balance0) {
      toast.error(
        t("common.errors.insufficientBalance", {
          balance: balance0,
          token: pool.token0.toUpperCase(),
        }),
      );
      return;
    }

    if (amount1 > balance1) {
      toast.error(
        t("common.errors.insufficientBalance", {
          balance: balance1,
          token: pool.token1.toUpperCase(),
        }),
      );
      return;
    }

    // Kiểm tra tick range
    if (
      position.tick_lower_index >= position.tick_upper_index ||
      position.tick_lower_index < MIN_TICK ||
      position.tick_upper_index > MAX_TICK
    ) {
      toast.error(t("liquidity.invalidPriceRange"));
      return;
    }

    try {
      setIsSubmitting(true);

      // Gửi dữ liệu position lên API
      const positionData = {
        pool_pair: position.pool_pair,
        tick_lower_index: position.tick_lower_index,
        tick_upper_index: position.tick_upper_index,
        amount0_initial: position.amount0_initial,
        amount1_initial: position.amount1_initial,
        slippage: position.slippage,
      };

      await createPosition(positionData);

      // Invalidate positions query cache để force refresh data
      queryClient.invalidateQueries({ queryKey: ["positions"] });

      // Prefetch positions data to ensure it's ready when user arrives
      await queryClient.prefetchQuery({
        queryKey: ["positions", "all"],
        queryFn: () => fetchPositions("all"),
      });

      // Hiển thị thông báo thành công
      toast.success(t("liquidity.positionCreated"));

      // Chuyển hướng đến trang positions
      router.push("/liquidity/positions");
    } catch (error) {
      console.error("Error creating position:", error);
      // Xử lý lỗi API
      let errorMessage = t("common.errors.somethingWentWrong");

      if (error instanceof AxiosError && error.response?.data?.error) {
        errorMessage = error.response.data.error;
      }

      toast.error(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  }, [pool, position, getTokenBalance, router, t, queryClient]);

  // Kiểm tra nút Submit có bị vô hiệu hóa hay không
  const isSubmitDisabled = () => {
    const amount0 = parseFloat(position.amount0_initial);
    const amount1 = parseFloat(position.amount1_initial);
    const balance0 = getTokenBalance(pool.token0);
    const balance1 = getTokenBalance(pool.token1);

    return (
      isSubmitting ||
      amount0 <= 0 ||
      amount1 <= 0 ||
      amount0 > balance0 ||
      amount1 > balance1 ||
      position.tick_lower_index >= position.tick_upper_index ||
      position.tick_lower_index < MIN_TICK ||
      position.tick_upper_index > MAX_TICK
    );
  };

  return (
    <div className="w-1/2 mx-auto">
      <Header />

      <Card className="border-0 shadow-none">
        <CardContent className="space-y-6 pt-6">
          <div className="text-2xl font-bold mb-4">{pool.pair}</div>

          <PoolInfo pool={pool} />

          <SelectPriceRange
            pool={pool}
            initialLowerTick={position.tick_lower_index}
            initialUpperTick={position.tick_upper_index}
            onTicksChange={handleTickRangeChange}
          />

          <TokensInput
            pool={pool}
            tickLowerIndex={position.tick_lower_index}
            tickUpperIndex={position.tick_upper_index}
            token0Balance={getTokenBalance(pool.token0).toString()}
            token1Balance={getTokenBalance(pool.token1).toString()}
            onAmountsChange={handleTokenValuesChange}
          />

          <SlippageSelector
            initialValue={position.slippage}
            onChange={handleSlippageChange}
          />
        </CardContent>

        <CardFooter className="pb-6">
          <Button
            className="w-full py-6 text-base font-medium rounded-md"
            onClick={handleSubmit}
            disabled={isSubmitDisabled()}
          >
            {isSubmitting
              ? t("common.submitting")
              : t("liquidity.createPosition")}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
