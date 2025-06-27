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
  cancelEscrow,
  createEscrow,
  Escrow,
  getEscrows,
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
import { useEscrowChannel } from "@/hooks/use-escrow-channel";
import { NumberInputWithCommas } from "@/components/ui/number-input-with-commas";

// Component to handle individual escrow subscription
function EscrowSubscription({
  escrow,
  onUpdate,
}: {
  escrow: Escrow;
  onUpdate: (escrow: Escrow) => void;
}) {
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [pollCount, setPollCount] = useState(0);
  const MAX_POLL_ATTEMPTS = 15; // 1.5 seconds maximum polling (100ms * 15)

  // Memoize the escrow ID to prevent unnecessary re-renders
  const escrowId = useMemo(() => escrow.id.toString(), [escrow.id]);

  // Memoize the update callback to prevent unnecessary re-renders
  const handleEscrowUpdate = useCallback(
    (updatedEscrow: Escrow) => {
      if (!updatedEscrow || !updatedEscrow.id) {
        console.error("Invalid escrow data received");
        return;
      }

      const mergedEscrow = {
        ...escrow,
        status: updatedEscrow.status,
      };

      onUpdate(mergedEscrow);
    },
    [escrow, onUpdate],
  );

  // Only subscribe once when component mounts
  useEffect(() => {
    if (!isSubscribed) {
      setIsSubscribed(true);
    }
  }, [isSubscribed]);

  // Use the memoized values in the websocket hook
  useEscrowChannel({
    escrowId,
    onEscrowUpdated: handleEscrowUpdate,
  });

  // Poll for escrow status updates if still pending
  useEffect(() => {
    if (escrow.status === "pending" && pollCount < MAX_POLL_ATTEMPTS) {
      const pollInterval = setInterval(async () => {
        try {
          const response = await getEscrows();
          const escrowsData = Array.isArray(response)
            ? response
            : Array.isArray(response.data)
              ? response.data
              : [response.data];

          const updatedEscrow = escrowsData.find(
            (e: Escrow | undefined) => e?.id === escrow.id,
          );

          if (updatedEscrow && updatedEscrow.status !== "pending") {
            onUpdate(updatedEscrow);
            clearInterval(pollInterval);
          } else {
            setPollCount((prev) => prev + 1);
          }
        } catch (error) {
          console.error("Failed to poll escrow status:", error);
          setPollCount((prev) => prev + 1);
        }
      }, 100); // Poll every 100ms

      return () => clearInterval(pollInterval);
    }
  }, [escrow.id, escrow.status, onUpdate, pollCount]);

  return null;
}

export default function MerchantEscrows() {
  const t = useTranslations("merchant.escrows");
  const [escrows, setEscrows] = useState<Escrow[]>([]);
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

  const fetchEscrows = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getEscrows();

      if (Array.isArray(response)) {
        setEscrows(response);
      } else if (response.data) {
        const escrowsData = Array.isArray(response.data)
          ? response.data
          : [response.data];
        setEscrows(escrowsData);
      } else {
        setEscrows([]);
      }
    } catch (error) {
      toast.error(t("fetchError") + ": " + error);
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    fetchEscrows();
  }, [fetchEscrows]);

  const handleEscrowUpdate = useCallback((updatedEscrow: Escrow) => {
    setEscrows((currentEscrows) =>
      currentEscrows.map((e) =>
        e.id === updatedEscrow.id ? updatedEscrow : e,
      ),
    );
  }, []);

  async function handleCreateEscrow(e: React.FormEvent) {
    e.preventDefault();

    if (!usdtAmount || !fiatCurrency) {
      toast.error(t("validationError"));
      return;
    }

    if (walletData && coinsData) {
      const usdtBalance = getTokenBalance("usdt", walletData, coinsData);
      const inputAmount = parseFloat(usdtAmount);

      if (inputAmount > parseFloat(usdtBalance)) {
        setErrorMessage(t("insufficientBalance"));
        setIsBalanceInsufficient(true);
        return;
      }
    }

    try {
      setCreating(true);
      setErrorMessage(null);
      await createEscrow(usdtAmount, fiatCurrency);
      toast.success(t("createSuccess"));
      setUsdtAmount("");
      fetchEscrows();
    } catch (error: unknown) {
      console.error("Failed to create escrow:", error);

      const axiosError = error as {
        response?: { data?: { errors?: string[] } };
      };
      if (axiosError.response?.data?.errors) {
        const errorMessages = axiosError.response.data.errors;
        if (
          errorMessages.includes(
            "Usdt amount USDT amount exceeds available balance",
          )
        ) {
          setErrorMessage(t("merchant.escrows.insufficientBalance"));
        } else {
          setErrorMessage(errorMessages.join(", "));
        }
      } else {
        setErrorMessage(t("createError"));
      }
    } finally {
      setCreating(false);
    }
  }

  async function handleCancelEscrow(id: number) {
    try {
      const escrow = escrows.find((e) => e.id === id);
      if (!escrow) {
        toast.error(t("escrowNotFound"));
        return;
      }

      if (walletData && coinsData) {
        const fiatBalance = getTokenBalance(
          escrow.fiat_currency.toLowerCase(),
          walletData,
          coinsData,
        );
        const requiredFiatAmount = parseFloat(escrow.fiat_amount);

        if (parseFloat(fiatBalance) < requiredFiatAmount) {
          toast.error(
            <div className="flex flex-col gap-1">
              <div className="font-medium">{t("insufficientFiatBalance")}</div>
              <div className="text-sm text-muted-foreground">
                {t("common.errors.insufficientBalance", {
                  balance: parseFloat(fiatBalance).toLocaleString(),
                  token: escrow.fiat_currency,
                })}
              </div>
            </div>,
          );
          return;
        }
      }

      await cancelEscrow(id);
    } catch (error) {
      console.error(`Failed to cancel escrow ${id}:`, error);
      toast.error(t("cancelError"));
    }
  }

  function getStatusBadge(status: string) {
    switch (status) {
      case "active":
        return <Badge className="bg-green-500">{t("status.active")}</Badge>;
      case "cancelled":
        return <Badge className="bg-red-500">{t("status.cancelled")}</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  }

  return (
    <ProtectedLayout>
      <div className="container mx-auto py-8">
        {/* Add escrow subscriptions only for non-cancelled escrows */}
        {escrows
          .filter((escrow) => escrow.status !== "cancelled")
          .map((escrow) => (
            <EscrowSubscription
              key={escrow.id}
              escrow={escrow}
              onUpdate={handleEscrowUpdate}
            />
          ))}

        <h1 className="text-2xl font-bold mb-6">{t("title")}</h1>

        <Card className="mb-8">
          <CardHeader>
            <CardTitle>{t("createTitle")}</CardTitle>
          </CardHeader>
          <CardContent>
            {errorMessage && (
              <div className="relative w-full rounded-lg border border-destructive/50 p-4 text-destructive dark:border-destructive mb-6">
                <AlertCircle className="h-4 w-4 absolute left-4 top-4" />
                <div className="text-sm pl-7">{errorMessage}</div>
              </div>
            )}

            {exchangeRates && (
              <div className="mb-4 p-3 bg-muted rounded-md">
                <h3 className="text-sm font-medium mb-2">
                  {t("currentRates")}
                </h3>
                <div className="grid grid-cols-3 gap-2 text-xs">
                  <div>
                    1 USDT = {exchangeRates.usdt_to_vnd.toLocaleString()} VND
                  </div>
                  <div>
                    1 USDT = {exchangeRates.usdt_to_php.toLocaleString()} PHP
                  </div>
                  <div>
                    1 USDT = {exchangeRates.usdt_to_ngn.toLocaleString()} NGN
                  </div>
                </div>
              </div>
            )}

            <div className="mb-4 p-3 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 rounded-md">
              <p className="text-sm">
                <span className="font-medium">{t("currentBalance")}: </span>
                <span className="font-bold">
                  {parseFloat(usdtBalance).toLocaleString()}
                </span>{" "}
                USDT
              </p>
            </div>

            <form onSubmit={handleCreateEscrow} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    {t("usdtAmount")}
                  </label>
                  <NumberInputWithCommas
                    data-testid="balance-input"
                    value={usdtAmount}
                    onChange={setUsdtAmount}
                    placeholder={t("usdtAmountPlaceholder")}
                    min={0}
                    step={0.01}
                    inputMode="decimal"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    {t("fiatCurrency")}
                  </label>
                  <Select value={fiatCurrency} onValueChange={setFiatCurrency}>
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
                <div className="p-3 bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 rounded-md">
                  <p className="text-sm">
                    {t("estimatedFiatAmount")}:{" "}
                    <strong>{estimatedFiatAmount}</strong> {fiatCurrency}
                  </p>
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
              <div className="text-center py-4">{t("loading")}</div>
            ) : escrows.length === 0 ? (
              <div className="text-center py-4">{t("noEscrows")}</div>
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
                  {escrows.map((escrow) => (
                    <TableRow key={escrow.id}>
                      <TableCell>{escrow.id}</TableCell>
                      <TableCell>{escrow.usdt_amount}</TableCell>
                      <TableCell>{escrow.fiat_amount}</TableCell>
                      <TableCell>{escrow.fiat_currency}</TableCell>
                      <TableCell>{getStatusBadge(escrow.status)}</TableCell>
                      <TableCell>
                        {format(
                          new Date(escrow.created_at),
                          "MMM d, yyyy HH:mm",
                        )}
                      </TableCell>
                      <TableCell>
                        {escrow.status === "active" && (
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleCancelEscrow(escrow.id)}
                          >
                            {t("cancel")}
                          </Button>
                        )}
                      </TableCell>
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
