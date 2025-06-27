"use client";

import { useState, useMemo, useEffect, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";
import { useTranslations } from "next-intl";
import { FormattedPool } from "@/lib/api/pools";
import {
  calculateTokenAmounts,
  formatNumberWithCommas,
} from "@/lib/amm/position-utils";

interface TokensInputProps {
  pool: FormattedPool;
  tickLowerIndex: number;
  tickUpperIndex: number;
  token0Balance: string;
  token1Balance: string;
  onAmountsChange: (amount0: string, amount1: string) => void;
}

export function TokensInput({
  pool,
  tickLowerIndex,
  tickUpperIndex,
  token0Balance,
  token1Balance,
  onAmountsChange,
}: TokensInputProps) {
  const t = useTranslations();

  // Sử dụng useRef để theo dõi price range trước đó
  const prevTickLowerRef = useRef(tickLowerIndex);
  const prevTickUpperRef = useRef(tickUpperIndex);

  // Giá trị decimal trong state
  const [token0Value, setToken0Value] = useState("");
  const [token1Value, setToken1Value] = useState("");

  // Giá trị hiển thị có định dạng
  const [token0Display, setToken0Display] = useState("");
  const [token1Display, setToken1Display] = useState("");

  const [token0Error, setToken0Error] = useState("");
  const [token1Error, setToken1Error] = useState("");

  // Format displayed balance
  const displayToken0Balance = useMemo(() => {
    return formatNumberWithCommas(token0Balance);
  }, [token0Balance]);

  const displayToken1Balance = useMemo(() => {
    return formatNumberWithCommas(token1Balance);
  }, [token1Balance]);

  // Check if user has balance
  const hasToken0Balance = useMemo(() => {
    return parseFloat(token0Balance) > 0;
  }, [token0Balance]);

  const hasToken1Balance = useMemo(() => {
    return parseFloat(token1Balance) > 0;
  }, [token1Balance]);

  // Kiểm tra và cập nhật giá trị
  useEffect(() => {
    // Giá trị đã là decimal
    const amount0 = token0Value || "0";
    const amount1 = token1Value || "0";

    // Kiểm tra lỗi số dư
    setToken0Error(
      parseFloat(amount0) > parseFloat(token0Balance)
        ? t("common.errors.insufficientBalance", {
            balance: token0Balance,
            token: pool.token0.toUpperCase(),
          })
        : "",
    );

    setToken1Error(
      parseFloat(amount1) > parseFloat(token1Balance)
        ? t("common.errors.insufficientBalance", {
            balance: token1Balance,
            token: pool.token1.toUpperCase(),
          })
        : "",
    );

    // Thông báo thay đổi giá trị cho component cha (giá trị decimal)
    onAmountsChange(amount0, amount1);
  }, [
    onAmountsChange,
    pool.token0,
    pool.token1,
    t,
    token0Balance,
    token0Value,
    token1Balance,
    token1Value,
  ]);

  // Tính lại token1 khi price range thay đổi
  useEffect(() => {
    // Kiểm tra xem price range có thay đổi không
    if (
      tickLowerIndex !== prevTickLowerRef.current ||
      tickUpperIndex !== prevTickUpperRef.current
    ) {
      // Cập nhật refs
      prevTickLowerRef.current = tickLowerIndex;
      prevTickUpperRef.current = tickUpperIndex;

      // Nếu token0 có giá trị, tính lại token1
      if (token0Value && parseFloat(token0Value) > 0) {
        const numericValue = parseFloat(token0Value);
        try {
          const result = calculateTokenAmounts(
            pool,
            tickLowerIndex,
            tickUpperIndex,
            "token0",
            numericValue,
          );
          // Cập nhật state decimal và hiển thị
          setToken1Value(result.token1);
          setToken1Display(formatNumberWithCommas(result.token1));
        } catch (error) {
          console.error("Error recalculating token amounts:", error);
        }
      }
    }
  }, [tickLowerIndex, tickUpperIndex, token0Value, pool]);

  const handleToken0Max = () => {
    if (!hasToken0Balance) return;

    // Set giá trị decimal
    setToken0Value(token0Balance);
    // Set giá trị hiển thị
    setToken0Display(formatNumberWithCommas(token0Balance));

    // Tính giá trị token1 tương ứng
    const numericValue = parseFloat(token0Balance);
    if (numericValue > 0 && tickLowerIndex && tickUpperIndex) {
      const result = calculateTokenAmounts(
        pool,
        tickLowerIndex,
        tickUpperIndex,
        "token0",
        numericValue,
      );
      // Set giá trị decimal
      setToken1Value(result.token1);
      // Set giá trị hiển thị
      setToken1Display(formatNumberWithCommas(result.token1));
    }
  };

  const handleToken1Max = () => {
    if (!hasToken1Balance) return;

    // Set giá trị decimal
    setToken1Value(token1Balance);
    // Set giá trị hiển thị
    setToken1Display(formatNumberWithCommas(token1Balance));

    // Tính giá trị token0 tương ứng
    const numericValue = parseFloat(token1Balance);
    if (numericValue > 0 && tickLowerIndex && tickUpperIndex) {
      const result = calculateTokenAmounts(
        pool,
        tickLowerIndex,
        tickUpperIndex,
        "token1",
        numericValue,
      );
      // Set giá trị decimal
      setToken0Value(result.token0);
      // Set giá trị hiển thị
      setToken0Display(formatNumberWithCommas(result.token0));
    }
  };

  // Hàm format số khi nhập
  const formatInput = (value: string): string => {
    // Loại bỏ tất cả dấu phẩy
    const rawValue = value.replace(/,/g, "");

    // Nếu rỗng hoặc không phải số, trả về nguyên giá trị
    if (!rawValue || isNaN(Number(rawValue))) {
      return value;
    }

    // Format số với dấu phẩy
    return formatNumberWithCommas(rawValue);
  };

  const handleToken0Change = (value: string) => {
    // Cập nhật giá trị hiển thị (có định dạng)
    const formattedValue = formatInput(value);
    setToken0Display(formattedValue);

    // Lưu giá trị decimal (không có định dạng)
    const decimalValue = formattedValue.replace(/,/g, "");
    setToken0Value(decimalValue);

    // Reset token1 if input is empty
    if (!value) {
      setToken1Display("");
      setToken1Value("");
      return;
    }

    // Tính token1 dựa trên token0
    const numericValue = parseFloat(decimalValue) || 0;
    if (numericValue > 0 && tickLowerIndex && tickUpperIndex) {
      try {
        const result = calculateTokenAmounts(
          pool,
          tickLowerIndex,
          tickUpperIndex,
          "token0",
          numericValue,
        );

        // Cập nhật state và hiển thị
        setToken1Value(result.token1);
        setToken1Display(formatNumberWithCommas(result.token1));
      } catch (error) {
        console.error("Error calculating token1 amount:", error);
      }
    }
  };

  const handleToken1Change = (value: string) => {
    // Cập nhật giá trị hiển thị (có định dạng)
    const formattedValue = formatInput(value);
    setToken1Display(formattedValue);

    // Lưu giá trị decimal (không có định dạng)
    const decimalValue = formattedValue.replace(/,/g, "");
    setToken1Value(decimalValue);

    // Reset token0 if input is empty
    if (!value) {
      setToken0Display("");
      setToken0Value("");
      return;
    }

    // Tính token0 dựa trên token1
    const numericValue = parseFloat(decimalValue) || 0;
    if (numericValue > 0 && tickLowerIndex && tickUpperIndex) {
      try {
        const result = calculateTokenAmounts(
          pool,
          tickLowerIndex,
          tickUpperIndex,
          "token1",
          numericValue,
        );

        // Cập nhật state và hiển thị
        setToken0Value(result.token0);
        setToken0Display(formatNumberWithCommas(result.token0));
      } catch (error) {
        console.error("Error calculating token0 amount:", error);
      }
    }
  };

  return (
    <div className="mx-auto">
      {/* First token */}
      <div className="w-full space-y-1">
        <div className="flex items-center justify-between mb-2">
          <div className="text-base font-medium">
            {t("liquidity.tokenAmount")} {pool.token0}
          </div>
          <div className="text-sm text-muted-foreground">
            {t("liquidity.balance")}: {displayToken0Balance}
          </div>
        </div>

        <div className="flex gap-2">
          <div className="w-full">
            <input
              id="token0-input"
              data-testid="token0-input"
              className="w-full h-14 px-4 py-2 text-xl text-left bg-transparent border rounded-lg border-input focus:outline-none focus:ring-2 focus:ring-primary"
              value={token0Display}
              placeholder="0.0"
              onChange={(e) => handleToken0Change(e.target.value)}
            />
            {token0Error && (
              <p className="mt-1 text-sm text-destructive">{token0Error}</p>
            )}
          </div>

          <Button
            variant="outline"
            size="lg"
            className="px-4 whitespace-nowrap"
            onClick={handleToken0Max}
            disabled={!hasToken0Balance}
          >
            {t("liquidity.max")}
          </Button>
        </div>
      </div>

      {/* Plus sign in between */}
      <div className="flex justify-center items-center">
        <div className="bg-muted rounded-full p-3">
          <Plus className="h-6 w-6" />
        </div>
      </div>

      {/* Second token */}
      <div className="w-full space-y-1">
        <div className="flex items-center justify-between mb-2">
          <div className="text-base font-medium">
            {t("liquidity.tokenAmount")} {pool.token1}
          </div>
          <div className="text-sm text-muted-foreground">
            {t("liquidity.balance")}: {displayToken1Balance}
          </div>
        </div>

        <div className="flex gap-2">
          <div className="w-full">
            <input
              id="token1-input"
              data-testid="token1-input"
              className="w-full h-14 px-4 py-2 text-xl text-left bg-transparent border rounded-lg border-input focus:outline-none focus:ring-2 focus:ring-primary"
              value={token1Display}
              placeholder="0.0"
              onChange={(e) => handleToken1Change(e.target.value)}
            />
            {token1Error && (
              <p className="mt-1 text-sm text-destructive">{token1Error}</p>
            )}
          </div>

          <Button
            variant="outline"
            size="lg"
            className="px-4 whitespace-nowrap"
            onClick={handleToken1Max}
            disabled={!hasToken1Balance}
          >
            {t("liquidity.max")}
          </Button>
        </div>
      </div>
    </div>
  );
}
