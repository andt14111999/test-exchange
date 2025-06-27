"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ArrowUpDown, HistoryIcon } from "lucide-react";
import { useCallback, useEffect, useState, useMemo } from "react";
import { BigNumber } from "bignumber.js";
import { AmountInput } from "./amount-input";
import { SwapSummary } from "./swap-summary";
import { useTranslations } from "next-intl";
import { useQuery, useMutation } from "@tanstack/react-query";
import { fetchActivePools, fetchPoolByPair } from "@/lib/api/pools";
import { getUniqueTokens, findPoolPair } from "@/lib/amm/pool-utils";
import { executeSwap } from "@/lib/api/amm-orders";
import { fetchTicks } from "@/lib/api/amm-ticks";
import { useWallet } from "@/hooks/use-wallet";
import { formatCurrency } from "@/lib/utils/format";
import { LiquidityCalculator } from "@/lib/amm/liquidity_calculator";
import { estimateSwapV3 } from "@/lib/amm/amm-math";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useToast } from "@/components/ui/use-toast";
import { fetchCoinSettings } from "@/lib/api/coins";
import type { CoinSetting } from "@/lib/api/coins";

// Cấu hình để sử dụng BigNumber chính xác hơn
BigNumber.config({
  EXPONENTIAL_AT: [-15, 20],
  DECIMAL_PLACES: 8,
  ROUNDING_MODE: BigNumber.ROUND_DOWN,
});

interface SwapState {
  inputToken: string;
  outputToken: string;
  inputAmount: string;
  outputAmount: string;
  poolPair: string | null;
  error: string | null;
  zeroForOne: boolean;
}

export function SwapContainer() {
  const t = useTranslations("swap");
  const { data: walletData } = useWallet();
  const router = useRouter();
  const { toast } = useToast();

  // State initialization
  const [state, setState] = useState<SwapState>({
    inputToken: "",
    outputToken: "",
    inputAmount: "",
    outputAmount: "",
    poolPair: null,
    error: null,
    zeroForOne: true,
  });

  const [exchangeRate, setExchangeRate] = useState<BigNumber | null>(null);
  const [priceImpact, setPriceImpact] = useState<number>(0);

  // Fetch active pools
  const {
    data: activePools,
    isLoading: isLoadingPools,
    error: poolsError,
  } = useQuery({
    queryKey: ["active-pools"],
    queryFn: fetchActivePools,
  });

  // Fetch pool details
  const {
    data: poolDetail,
    isLoading: isLoadingPoolDetail,
    error: poolDetailError,
  } = useQuery({
    queryKey: ["pool", state.poolPair],
    queryFn: () => (state.poolPair ? fetchPoolByPair(state.poolPair) : null),
    enabled: !!state.poolPair,
  });

  // Fetch ticks
  const { data: ticks } = useQuery({
    queryKey: ["ticks", state.poolPair],
    queryFn: () => (state.poolPair ? fetchTicks(state.poolPair) : []),
    enabled: !!state.poolPair,
  });

  // Fetch coin settings
  const { data: coinSettings } = useQuery<CoinSetting[]>({
    queryKey: ["coin-settings"],
    queryFn: fetchCoinSettings,
  });

  // Check if swap is enabled for both tokens
  const isSwapEnabled = useMemo(() => {
    if (!coinSettings) return true; // Default to enabled if settings not loaded
    if (!state.inputToken || !state.outputToken) return true;

    const inputSetting = coinSettings.find(
      (s) => s.currency.toLowerCase() === state.inputToken.toLowerCase(),
    );
    const outputSetting = coinSettings.find(
      (s) => s.currency.toLowerCase() === state.outputToken.toLowerCase(),
    );

    // If either token is not found in settings, default to enabled
    if (!inputSetting || !outputSetting) return true;

    // Check if swap is enabled for both tokens
    return inputSetting.swap_enabled && outputSetting.swap_enabled;
  }, [coinSettings, state.inputToken, state.outputToken]);

  // Hàm xử lý khi bấm nút đảo hướng
  const handleSwapDirection = useCallback(() => {
    // Đảo chiều hướng, đổi token và giá trị
    setState((prev) => {
      // Lưu giá trị cho việc đổi
      const tempInputToken = prev.inputToken;
      const tempOutputToken = prev.outputToken;
      const tempInputAmount = prev.inputAmount;
      const tempOutputAmount = prev.outputAmount;

      // Tìm pool mới nếu cần
      let newPoolPair = prev.poolPair;
      if (activePools && tempOutputToken && tempInputToken) {
        const newPair = findPoolPair(
          activePools,
          tempOutputToken,
          tempInputToken,
        );
        if (newPair) {
          newPoolPair = newPair.pair;
        }
      }

      return {
        ...prev,
        inputToken: tempOutputToken, // Đổi token vào/ra
        outputToken: tempInputToken,
        inputAmount: tempOutputAmount, // Đổi giá trị vào/ra
        outputAmount: tempInputAmount,
        zeroForOne: !prev.zeroForOne, // Đảo chiều zero for one
        poolPair: newPoolPair,
        error: null,
      };
    });

    // Đảo ngược tỷ giá nếu có
    if (exchangeRate && !exchangeRate.isZero()) {
      setExchangeRate(new BigNumber(1).div(exchangeRate));
    }
  }, [exchangeRate, activePools]);

  // Set default pool when active pools are loaded
  useEffect(() => {
    if (!activePools?.length || state.poolPair) return;

    const usdtPool = activePools.find(
      (pool) =>
        pool.token0.toLowerCase() === "usdt" ||
        pool.token1.toLowerCase() === "usdt",
    );

    if (usdtPool) {
      setState((prev) => ({
        ...prev,
        inputToken:
          usdtPool.token0.toLowerCase() === "usdt"
            ? usdtPool.token0.toUpperCase()
            : usdtPool.token1.toUpperCase(),
        outputToken:
          usdtPool.token0.toLowerCase() === "usdt"
            ? usdtPool.token1.toUpperCase()
            : usdtPool.token0.toUpperCase(),
        poolPair: usdtPool.pair,
        zeroForOne: usdtPool.token0.toLowerCase() === "usdt",
      }));
    } else {
      const defaultPool = activePools[0];
      setState((prev) => ({
        ...prev,
        inputToken: defaultPool.token0.toUpperCase(),
        outputToken: defaultPool.token1.toUpperCase(),
        poolPair: defaultPool.pair,
        zeroForOne: true,
      }));
    }
  }, [activePools, state.poolPair]);

  // Update exchange rate when pool detail changes
  useEffect(() => {
    if (!poolDetail || isLoadingPoolDetail) return;

    const newExchangeRate = state.zeroForOne
      ? new BigNumber(poolDetail.price)
      : new BigNumber(1).div(poolDetail.price);

    setExchangeRate(newExchangeRate);
  }, [poolDetail, isLoadingPoolDetail, state.zeroForOne]);

  // Mutation cho việc thực hiện swap
  const { mutate: performSwap, isPending: isSwapping } = useMutation({
    mutationFn: executeSwap,
    onSuccess: () => {
      // Hiển thị toast thành công
      toast({
        title: t("swapSuccess"),
        variant: "default",
      });

      // Chuyển hướng đến trang lịch sử sau khi swap thành công
      router.push("/swap/history");
    },
    onError: (error: Error & { response?: { data?: { error?: string } } }) => {
      console.error("Swap error:", error);

      // Hiển thị toast lỗi
      toast({
        title: t("swapError"),
        description: error?.response?.data?.error || t("unknownError"),
        variant: "destructive",
      });
    },
  });

  // Hàm để lấy số dư của user cho token
  const getTokenBalance = useCallback(
    (token: string): string => {
      if (!token || !walletData) return "0";

      try {
        // Kiểm tra trong coin_accounts
        const coinAccount = walletData.coin_accounts.find(
          (account) =>
            account.coin_currency.toLowerCase() === token.toLowerCase(),
        );
        if (coinAccount) return coinAccount.balance.toString();

        // Kiểm tra trong fiat_accounts
        const fiatAccount = walletData.fiat_accounts.find(
          (account) => account.currency.toLowerCase() === token.toLowerCase(),
        );
        if (fiatAccount) return fiatAccount.balance.toString();

        return "0";
      } catch (error) {
        console.error(`Error getting balance for ${token}:`, error);
        return "0";
      }
    },
    [walletData],
  );

  // Hàm để format số dư hiển thị, sử dụng formatCurrency từ utils
  const formatBalance = useCallback(
    (balance: string, token: string): string => {
      if (!balance || !token) return "0";

      try {
        const numBalance = new BigNumber(balance);
        if (numBalance.isNaN()) return "0";

        // Sử dụng getDecimalScale từ LiquidityCalculator
        const decimals = LiquidityCalculator.getDecimalScale(token);

        // Sử dụng formatCurrency từ utils
        return formatCurrency(numBalance, token.toUpperCase(), {
          decimals: decimals,
          showSymbol: false,
        });
      } catch (error) {
        console.error(`Error formatting balance for ${token}:`, error);
        return "0";
      }
    },
    [],
  );

  // Hàm xử lý khi thay đổi token đầu vào
  const handleInputTokenChange = useCallback(
    async (token: string) => {
      // Nếu token không thay đổi, bỏ qua
      if (token.toLowerCase() === state.inputToken.toLowerCase()) {
        return;
      }

      // Kiểm tra nếu token giống với token đầu ra, thì swap vị trí
      if (token.toLowerCase() === state.outputToken.toLowerCase()) {
        handleSwapDirection();
        return;
      }

      // Tìm pool phù hợp với cặp token mới
      if (!activePools || !token || !state.outputToken) return;

      const newPair = findPoolPair(activePools, token, state.outputToken);

      if (!newPair) {
        setState((prev) => ({
          ...prev,
          inputToken: token,
          inputAmount: "",
          outputAmount: "",
          error: "Không tìm thấy pool phù hợp cho cặp token này",
          poolPair: null,
        }));
        return;
      }

      // Xác định phương hướng swap (token0 -> token1 hoặc token1 -> token0)
      const zeroForOne = token.toLowerCase() === newPair.token0.toLowerCase();

      setState((prev) => ({
        ...prev,
        inputToken: token,
        inputAmount: "",
        outputAmount: "",
        error: null,
        poolPair: newPair.pair,
        zeroForOne,
      }));
    },
    [activePools, state.outputToken, state.inputToken, handleSwapDirection],
  );

  // Hàm xử lý khi thay đổi token đầu ra
  const handleOutputTokenChange = useCallback(
    async (token: string) => {
      // Nếu token không thay đổi, bỏ qua
      if (token.toLowerCase() === state.outputToken.toLowerCase()) {
        return;
      }

      // Kiểm tra nếu token giống với token đầu vào, thì swap vị trí
      if (token.toLowerCase() === state.inputToken.toLowerCase()) {
        handleSwapDirection();
        return;
      }

      // Tìm pool phù hợp với cặp token mới
      if (!activePools || !token || !state.inputToken) return;

      const newPair = findPoolPair(activePools, state.inputToken, token);

      if (!newPair) {
        setState((prev) => ({
          ...prev,
          outputToken: token,
          inputAmount: "",
          outputAmount: "",
          error: "Không tìm thấy pool phù hợp cho cặp token này",
          poolPair: null,
        }));
        return;
      }

      // Xác định phương hướng swap (token0 -> token1 hoặc token1 -> token0)
      const zeroForOne =
        state.inputToken.toLowerCase() === newPair.token0.toLowerCase();

      setState((prev) => ({
        ...prev,
        outputToken: token,
        inputAmount: "",
        outputAmount: "",
        error: null,
        poolPair: newPair.pair,
        zeroForOne,
      }));
    },
    [activePools, state.inputToken, state.outputToken, handleSwapDirection],
  );

  // Hàm xử lý khi thay đổi input amount
  const handleInputChange = useCallback(
    (amount: string) => {
      setState((prev) => ({ ...prev, inputAmount: amount }));

      if (
        !amount ||
        isNaN(Number(amount)) ||
        Number(amount) <= 0 ||
        !state.poolPair ||
        !poolDetail
      ) {
        setState((prev) => ({ ...prev, outputAmount: "" }));
        setPriceImpact(0);
        return;
      }

      try {
        // Lấy số thập phân cho token đầu ra
        const outputDecimals = LiquidityCalculator.getDecimalScale(
          state.outputToken,
        );

        // Điều chỉnh input amount theo decimal
        const inputAmountAdjusted = new BigNumber(amount);

        // Sử dụng current_tick từ pool data nếu có
        const currentTickIndex =
          poolDetail.currentTick ||
          Math.floor(Math.log(poolDetail.price.toNumber()) / Math.log(1.0001));

        // Tính giá thực tế từ tick index
        const actualPrice = Math.pow(1.0001, currentTickIndex);

        const result = estimateSwapV3(
          {
            sqrt_price: poolDetail.sqrtPriceX96.toString(),
            fee_percentage: poolDetail.fee.toString(),
            tick_spacing: poolDetail.tickSpacing,
            tvl_in_token0: poolDetail.liquidity.toString(),
            tvl_in_token1: poolDetail.price
              .multipliedBy(poolDetail.liquidity)
              .toString(),
            // Sử dụng giá thực tế từ tick index
            price: actualPrice.toString(),
            current_tick_index: currentTickIndex,
          },
          inputAmountAdjusted.toNumber(),
          state.zeroForOne,
          ticks, // Truyền dữ liệu ticks vào hàm estimateSwapV3
        );

        // Làm tròn dựa trên decimal của token output
        const formattedOutput = result.amountOut.toFixed(outputDecimals);

        setState((prev) => ({
          ...prev,
          outputAmount: formattedOutput,
          error: null,
        }));

        // Cập nhật price impact từ kết quả
        setPriceImpact(result.priceImpact * 100);

        // Cập nhật tỷ giá
        if (inputAmountAdjusted.toNumber() > 0 && result.amountOut > 0) {
          // Tỷ giá thực tế là số lượng token nhận được chia cho số lượng token đầu vào
          // Đây là tỷ giá thực tế sau khi tính toán với dữ liệu ticks
          setExchangeRate(
            new BigNumber(result.amountOut).div(inputAmountAdjusted),
          );
        }
      } catch (error) {
        console.error("Error estimating swap:", error);
        setState((prev) => ({
          ...prev,
          outputAmount: "",
          error: "Lỗi khi ước tính kết quả swap",
        }));
        setPriceImpact(0);
      }
    },
    [state.poolPair, state.zeroForOne, state.outputToken, poolDetail, ticks],
  );

  // Hàm xử lý khi thay đổi output amount
  const handleOutputChange = useCallback(
    (amount: string) => {
      setState((prev) => ({ ...prev, outputAmount: amount }));

      if (
        !amount ||
        isNaN(Number(amount)) ||
        Number(amount) <= 0 ||
        !state.poolPair ||
        !poolDetail
      ) {
        setState((prev) => ({ ...prev, inputAmount: "" }));
        setPriceImpact(0);
        return;
      }

      try {
        // Lấy số thập phân cho token đầu vào
        const inputDecimals = LiquidityCalculator.getDecimalScale(
          state.inputToken,
        );

        // Khi người dùng nhập output amount, chúng ta cần ước tính input amount
        // Tuy nhiên, estimateSwapV3 chỉ có thể tính từ input -> output
        // Vì vậy, chúng ta sẽ sử dụng cách tiếp cận đơn giản với giá hiện tại

        // Sử dụng current_tick từ pool data nếu có
        const currentTickIndex =
          poolDetail.currentTick ||
          Math.floor(Math.log(poolDetail.price.toNumber()) / Math.log(1.0001));

        // Tính giá thực tế từ tick index
        const actualPrice = Math.pow(1.0001, currentTickIndex);

        // Lấy giá hiện tại từ pool detail (sử dụng giá thực tế từ tick)
        const currentPrice = state.zeroForOne
          ? new BigNumber(actualPrice)
          : new BigNumber(1).div(actualPrice);

        let inputAmount;
        // Tính toán input amount dựa trên giá và loại swap
        if (state.zeroForOne) {
          // Token0 -> Token1: amount0 = amount1 / price
          inputAmount = new BigNumber(amount).div(currentPrice);
        } else {
          // Token1 -> Token0: amount1 = amount0 * price
          inputAmount = new BigNumber(amount).multipliedBy(currentPrice);
        }

        // Cộng thêm phí giao dịch vào input
        inputAmount = inputAmount.div(1 - poolDetail.fee);

        // Làm tròn dựa trên decimal của token input
        const formattedInput = inputAmount.toFixed(inputDecimals);

        setState((prev) => ({
          ...prev,
          inputAmount: formattedInput,
          error: null,
        }));

        // Ước tính price impact (giả định 1%)
        setPriceImpact(1);

        // Cập nhật tỷ giá
        if (new BigNumber(amount).gt(0) && inputAmount.gt(0)) {
          setExchangeRate(new BigNumber(amount).div(inputAmount));
        }
      } catch (error) {
        console.error("Error calculating input from output:", error);
        setState((prev) => ({
          ...prev,
          inputAmount: "",
          error: "Lỗi khi tính toán đầu vào",
        }));
      }
    },
    [state.poolPair, state.zeroForOne, state.inputToken, poolDetail],
  );

  // Hàm xử lý khi bấm nút swap
  const handleSwap = useCallback(async () => {
    if (!isSwapEnabled) {
      toast({
        title: t("swapDisabled"),
        description: t("swapDisabledDescription"),
        variant: "destructive",
      });
      return;
    }

    if (!state.poolPair || !state.inputAmount || !state.outputAmount) {
      return;
    }

    performSwap({
      poolPair: state.poolPair,
      zeroForOne: state.zeroForOne,
      amountSpecified: state.inputAmount,
      amountEstimated: state.outputAmount,
      slippage: 0.05, // 5% slippage mặc định
    });
  }, [
    isSwapEnabled,
    toast,
    t,
    state.poolPair,
    state.inputAmount,
    state.outputAmount,
    state.zeroForOne,
    performSwap,
  ]);

  // Kiểm tra xem user có đủ balance cho token input không
  const isInsufficientBalance = useCallback(() => {
    if (!state.inputAmount || !state.inputToken) return false;

    const balance = getTokenBalance(state.inputToken);
    return new BigNumber(state.inputAmount).isGreaterThan(balance);
  }, [state.inputAmount, state.inputToken, getTokenBalance]);

  // Hiển thị loading state
  const isLoading = isLoadingPools || isLoadingPoolDetail;
  if (isLoading) {
    return (
      <div className="container max-w-xl mx-auto py-8">
        <Card>
          <CardHeader>
            <CardTitle>{t("title")}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
            <p className="text-center text-muted-foreground">{t("loading")}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  // Hiển thị error state
  const error = poolsError || poolDetailError;
  if (error) {
    return (
      <div className="container max-w-xl mx-auto py-8">
        <Card>
          <CardHeader>
            <CardTitle>{t("error")}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="text-center py-4">
              <p className="text-red-500 mb-4">
                {typeof error === "string" ? error : t("error")}
              </p>
              <Button onClick={() => window.location.reload()}>
                {t("retry")}
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  const { token0List, token1List } = getUniqueTokens(activePools || []);

  // Thông báo lỗi nếu không đủ balance
  const errorMessage =
    state.error ||
    (isInsufficientBalance() ? t("insufficientBalance") : undefined);

  return (
    <div className="container max-w-xl mx-auto py-8">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>{t("title")}</CardTitle>
          <Link href="/swap/history">
            <Button variant="outline" size="sm">
              <HistoryIcon className="h-4 w-4 mr-2" />
              {t("viewHistory")}
            </Button>
          </Link>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-4">
            <AmountInput
              label={t("inputAmount")}
              value={state.inputAmount}
              onChange={handleInputChange}
              token={state.inputToken}
              onTokenChange={handleInputTokenChange}
              tokens={token0List.concat(token1List)}
              disabled={isSwapping || !isSwapEnabled}
              tokenBalance={formatBalance(
                getTokenBalance(state.inputToken),
                state.inputToken,
              )}
            />

            <div className="flex justify-center">
              <Button
                variant="outline"
                size="icon"
                className="rounded-full"
                onClick={handleSwapDirection}
                disabled={isSwapping || !isSwapEnabled}
              >
                <ArrowUpDown className="h-4 w-4" />
              </Button>
            </div>

            <AmountInput
              label={t("outputAmount")}
              value={state.outputAmount}
              onChange={handleOutputChange}
              token={state.outputToken}
              onTokenChange={handleOutputTokenChange}
              tokens={token0List.concat(token1List)}
              disabled={isSwapping || !isSwapEnabled}
              tokenBalance={formatBalance(
                getTokenBalance(state.outputToken),
                state.outputToken,
              )}
            />

            <SwapSummary
              exchangeRate={exchangeRate}
              outputToken={state.outputToken}
              inputToken={state.inputToken}
              disabled={
                !state.inputAmount ||
                !state.outputAmount ||
                isSwapping ||
                isInsufficientBalance() ||
                !isSwapEnabled
              }
              onSwap={handleSwap}
              buttonText={t("title")}
              errorMessage={errorMessage}
              priceImpact={priceImpact}
            />

            {!isSwapEnabled && (
              <div className="text-sm text-destructive text-center">
                {t("swapDisabled")}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
