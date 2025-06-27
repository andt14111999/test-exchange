"use client";

import { ProtectedLayout } from "@/components/protected-layout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  cancelFiatMint,
  createFiatMint,
  FiatMint,
  getFiatMints,
} from "@/lib/api/merchant";
import { ExchangeRates, getExchangeRates } from "@/lib/api/settings";
import { format } from "date-fns";
import { AlertCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import { useCallback, useEffect, useState, useMemo } from "react";
import { toast } from "sonner";
import { useWallet } from "@/hooks/use-wallet";
import { getTokenBalance } from "@/lib/api/coins";
import { useQuery } from "@tanstack/react-query";
import { fetchCoins } from "@/lib/api/coins";
import { useFiatMintChannel } from "@/hooks/use-fiat-mint-channel";
import { NumberInputWithCommas } from "@/components/ui/number-input-with-commas";

// Component to handle individual fiat mint subscription
function FiatMintSubscription({
  fiatMint,
  onUpdate,
}: {
  fiatMint: FiatMint;
  onUpdate: (fiatMint: FiatMint) => void;
}) {
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [pollCount, setPollCount] = useState(0);
  const MAX_POLL_ATTEMPTS = 15; // 1.5 seconds maximum polling (100ms * 15)

  // Memoize the fiat mint ID to prevent unnecessary re-renders
  const fiatMintId = useMemo(() => fiatMint.id.toString(), [fiatMint.id]);

  // Memoize the update callback to prevent unnecessary re-renders
  const handleFiatMintUpdate = useCallback(
    (updatedFiatMint: FiatMint) => {
      if (!updatedFiatMint || !updatedFiatMint.id) {
        console.error("Invalid fiat mint data received");
        return;
      }

      const mergedFiatMint = {
        ...fiatMint,
        status: updatedFiatMint.status,
      };

      onUpdate(mergedFiatMint);
    },
    [fiatMint, onUpdate],
  );

  // Only subscribe once when component mounts
  useEffect(() => {
    if (!isSubscribed) {
      setIsSubscribed(true);
    }
  }, [isSubscribed]);

  // Use the memoized values in the websocket hook
  useFiatMintChannel({
    fiatMintId,
    onFiatMintUpdated: handleFiatMintUpdate,
  });

  // Poll for fiat mint status updates if still pending
  useEffect(() => {
    if (fiatMint.status === "pending" && pollCount < MAX_POLL_ATTEMPTS) {
      const pollInterval = setInterval(async () => {
        try {
          const response = await getFiatMints();
          const fiatMintsData = Array.isArray(response)
            ? response
            : Array.isArray(response.data)
              ? response.data
              : [response.data];

          const updatedFiatMint = fiatMintsData.find(
            (e: FiatMint | undefined) => e?.id === fiatMint.id,
          );

          if (updatedFiatMint && updatedFiatMint.status !== "pending") {
            onUpdate(updatedFiatMint);
            clearInterval(pollInterval);
          } else {
            setPollCount((prev) => prev + 1);
          }
        } catch (error) {
          console.error("Failed to poll fiat mint status:", error);
          setPollCount((prev) => prev + 1);
        }
      }, 100); // Poll every 100ms

      return () => clearInterval(pollInterval);
    }
  }, [fiatMint.id, fiatMint.status, onUpdate, pollCount]);

  return null;
}

export default function MerchantMintFiat() {
  const t = useTranslations("merchant.mintFiat");
  const [fiatMints, setFiatMints] = useState<FiatMint[]>([]);
  const [loading, setLoading] = useState(true);
  const [usdtAmount, setUsdtAmount] = useState("");
  const [fiatCurrency, setFiatCurrency] = useState("VND");
  const [creating, setCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<React.ReactNode | null>(
    null,
  );
  const [exchangeRates, setExchangeRates] = useState<ExchangeRates | null>(
    null,
  );
  const [estimatedFiatAmount, setEstimatedFiatAmount] = useState<string>("");
  const { data: walletData } = useWallet();
  const { data: coinsData } = useQuery({
    queryKey: ["coins"],
    queryFn: fetchCoins,
  });
  const [usdtBalance, setUsdtBalance] = useState("0");
  const [isBalanceInsufficient, setIsBalanceInsufficient] = useState(false);

  // Fetch exchange rates
  useEffect(() => {
    const fetchExchangeRates = async () => {
      try {
        const rates = await getExchangeRates();
        setExchangeRates(rates);
      } catch (error) {
        console.error("Failed to fetch exchange rates:", error);
      }
    };

    fetchExchangeRates();
  }, []);

  // Update USDT balance when wallet data changes
  useEffect(() => {
    if (walletData && coinsData) {
      const balance = getTokenBalance("usdt", walletData, coinsData);
      setUsdtBalance(balance);
    }
  }, [walletData, coinsData]);

  // Calculate estimated fiat amount and validate against balance
  useEffect(() => {
    if (!exchangeRates || !usdtAmount || !fiatCurrency) {
      setEstimatedFiatAmount("");
      setErrorMessage(null);
      setIsBalanceInsufficient(false);
      return;
    }

    // Check USDT balance
    if (walletData && coinsData) {
      const usdtBalance = getTokenBalance("usdt", walletData, coinsData);
      const inputAmount = parseFloat(usdtAmount);

      if (inputAmount > parseFloat(usdtBalance)) {
        setErrorMessage(t("insufficientBalance"));
        setIsBalanceInsufficient(true);
      } else {
        setErrorMessage(null);
        setIsBalanceInsufficient(false);
      }
    }

    let rate = 0;
    switch (fiatCurrency) {
      case "VND":
        rate = exchangeRates.usdt_to_vnd;
        break;
      case "PHP":
        rate = exchangeRates.usdt_to_php;
        break;
      case "NGN":
        rate = exchangeRates.usdt_to_ngn;
        break;
      default:
        rate = 0;
    }

    const amount = parseFloat(usdtAmount) * rate;
    setEstimatedFiatAmount(amount.toLocaleString());
  }, [usdtAmount, fiatCurrency, exchangeRates, walletData, coinsData, t]);

  const fetchFiatMints = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getFiatMints();

      if (Array.isArray(response)) {
        setFiatMints(response);
      } else if (response.data) {
        const fiatMintsData = Array.isArray(response.data)
          ? response.data
          : [response.data];
        setFiatMints(fiatMintsData);
      } else {
        setFiatMints([]);
      }
    } catch (error) {
      toast.error(t("fetchError") + ": " + error);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    fetchFiatMints();
  }, [fetchFiatMints]);

  const handleFiatMintUpdate = useCallback((updatedFiatMint: FiatMint) => {
    setFiatMints((currentFiatMints) =>
      currentFiatMints.map((e) =>
        e.id === updatedFiatMint.id ? updatedFiatMint : e,
      ),
    );
  }, []);

  async function handleCreateFiatMint(e: React.FormEvent) {
    e.preventDefault();

    if (!usdtAmount || !fiatCurrency) {
      toast.error(t("validationError"));
      return;
    }

    if (walletData && coinsData) {
      const usdtBalance = getTokenBalance("usdt", walletData, coinsData);
      const inputAmount = parseFloat(usdtAmount);

      if (inputAmount > parseFloat(usdtBalance)) {
        toast.error(t("insufficientBalance"));
        return;
      }
    }

    setCreating(true);
    setErrorMessage(null);
    try {
      const newFiatMint = await createFiatMint(usdtAmount, fiatCurrency);
      if (
        "data" in newFiatMint &&
        newFiatMint.data &&
        !Array.isArray(newFiatMint.data)
      ) {
        setFiatMints((prev) => [newFiatMint.data as FiatMint, ...prev]);
        toast.success(t("creationSuccess"));
        setUsdtAmount("");
        setFiatCurrency("VND");
      } else if ("message" in newFiatMint) {
        setErrorMessage(newFiatMint.message as string);
        toast.error(`${t("creationError")} ${newFiatMint.message}`);
      } else {
        setErrorMessage(t("creationError"));
        toast.error(t("creationError"));
      }
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : t("creationError");
      setErrorMessage(errorMessage);
      toast.error(`${t("creationError")} ${errorMessage}`);
    } finally {
      setCreating(false);
    }
  }

  async function handleCancelFiatMint(id: number) {
    try {
      await cancelFiatMint(id);
      setFiatMints((prev) =>
        prev.map((fiatMint) =>
          fiatMint.id === id ? { ...fiatMint, status: "cancelled" } : fiatMint,
        ),
      );
      toast.success(t("cancellationSuccess"));
    } catch {
      toast.error(t("cancellationError"));
    }
  }

  function getStatusBadge(status: string) {
    switch (status) {
      case "active":
        return <Badge variant="default">{t("status.active")}</Badge>;
      case "cancelled":
        return <Badge variant="destructive">{t("status.cancelled")}</Badge>;
      default:
        return <Badge variant="secondary">{status}</Badge>;
    }
  }

  return (
    <ProtectedLayout>
      <div className="container mx-auto p-4">
        <h1 className="mb-4 text-2xl font-bold">{t("title")}</h1>

        <Card className="mb-8">
          <CardHeader>
            <CardTitle>{t("createTitle")}</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreateFiatMint} className="space-y-4">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                  <label
                    htmlFor="usdt-amount"
                    className="mb-2 block text-sm font-medium"
                  >
                    {t("usdtAmount")}
                  </label>
                  <NumberInputWithCommas
                    value={usdtAmount}
                    onChange={setUsdtAmount}
                    placeholder={t("usdtAmountPlaceholder")}
                    className="w-full"
                    disabled={creating}
                  />
                  {usdtBalance && (
                    <div className="mt-2 text-sm text-gray-500">
                      {t("currentBalance")}: {usdtBalance} USDT
                    </div>
                  )}
                </div>
                <div>
                  <label
                    htmlFor="fiat-currency"
                    className="mb-2 block text-sm font-medium"
                  >
                    {t("fiatCurrency")}
                  </label>
                  <Select
                    value={fiatCurrency}
                    onValueChange={setFiatCurrency}
                    disabled={creating}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={t("selectCurrency")} />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="VND">VND</SelectItem>
                      <SelectItem value="PHP">PHP</SelectItem>
                      <SelectItem value="NGN">NGN</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {estimatedFiatAmount && (
                <div className="text-sm text-gray-500">
                  {t("estimatedFiatAmount")}: {estimatedFiatAmount}
                </div>
              )}

              {errorMessage && (
                <div className="flex items-center text-red-500">
                  <AlertCircle className="mr-2 h-4 w-4" />
                  {errorMessage}
                </div>
              )}

              <Button
                type="submit"
                disabled={creating || isBalanceInsufficient}
              >
                {creating ? t("creating") : t("create")}
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t("listTitle")}</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <p>{t("loading")}</p>
            ) : fiatMints.length === 0 ? (
              <p>{t("noMints")}</p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t("id")}</TableHead>
                    <TableHead>{t("usdtAmount")}</TableHead>
                    <TableHead>{t("fiatAmountLabel")}</TableHead>
                    <TableHead>{t("fiatCurrency")}</TableHead>
                    <TableHead>{t("statusHeader")}</TableHead>
                    <TableHead>{t("createdAt")}</TableHead>
                    <TableHead>{t("actions")}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {fiatMints.map((fiatMint) => (
                    <TableRow key={fiatMint.id}>
                      <TableCell>{fiatMint.id}</TableCell>
                      <TableCell>{fiatMint.usdt_amount}</TableCell>
                      <TableCell>{fiatMint.fiat_amount}</TableCell>
                      <TableCell>{fiatMint.fiat_currency}</TableCell>
                      <TableCell>{getStatusBadge(fiatMint.status)}</TableCell>
                      <TableCell>
                        {format(new Date(fiatMint.created_at), "PPP p")}
                      </TableCell>
                      <TableCell>
                        {fiatMint.status === "active" && (
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleCancelFiatMint(fiatMint.id)}
                          >
                            {t("cancel")}
                          </Button>
                        )}
                      </TableCell>
                      <FiatMintSubscription
                        fiatMint={fiatMint}
                        onUpdate={handleFiatMintUpdate}
                      />
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>
    </ProtectedLayout>
  );
}
