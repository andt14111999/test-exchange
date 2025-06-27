"use client";

import { FiatTransactionLayout } from "@/components/fiat-transaction-layout";
import { ProtectedLayout } from "@/components/protected-layout";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/use-toast";
import { useWallet } from "@/hooks/use-wallet";
import { getOffers, Offer } from "@/lib/api/merchant";
import { createTrade } from "@/lib/api/trades";
import { MOCK_AMOUNT_LIMITS } from "@/lib/constants";
import { getTradingFees, TradingFees } from "@/lib/api/settings";
import { useUserStore } from "@/lib/store/user-store";
import { MerchantOffer } from "@/types/fiat-deposits";
import axios from "axios";
import { ArrowRight } from "lucide-react";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState, useEffect } from "react";

export default function DepositPage() {
  const t = useTranslations("deposit");
  const router = useRouter();
  const { toast } = useToast();
  const { currency } = useParams() as { currency: string };
  const [amount, setAmount] = useState("");
  const [selectedOfferId, setSelectedOfferId] = useState<string>();
  const [showOffers, setShowOffers] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [offers, setOffers] = useState<MerchantOffer[]>([]);
  const [tradingFees, setTradingFees] = useState<TradingFees | null>(null);
  const [isLoadingFees, setIsLoadingFees] = useState(true);
  const { user } = useUserStore();

  // Use the wallet hook to get real-time balance data
  const { data: walletData } = useWallet();

  // Get balance for the current currency directly from wallet data
  const currentBalance =
    walletData?.fiat_accounts.find(
      (account) => account.currency.toLowerCase() === currency.toLowerCase(),
    )?.balance || 0;

  // Fetch trading fees
  useEffect(() => {
    const fetchTradingFees = async () => {
      try {
        const fees = await getTradingFees();
        setTradingFees(fees);
      } catch (error) {
        console.error("Error fetching trading fees:", error);
        toast({
          title: "Error fetching fees",
          description: "Using default fee values",
          variant: "destructive",
        });
      } finally {
        setIsLoadingFees(false);
      }
    };

    fetchTradingFees();
  }, [toast]);

  const calculateReceiveAmount = (sendAmount: number): number => {
    if (!tradingFees) return sendAmount; // Return original amount if fees not loaded yet

    const currencyKey =
      currency.toLowerCase() as keyof typeof tradingFees.fee_ratios;

    // Use the specific currency fee ratio or default
    const feeRatio =
      tradingFees.fee_ratios[currencyKey] !== undefined
        ? tradingFees.fee_ratios[currencyKey]
        : tradingFees.fee_ratios.default;

    // Use the specific currency fixed fee or default
    const fixedFee =
      tradingFees.fixed_fees[currencyKey] !== undefined
        ? tradingFees.fixed_fees[currencyKey]
        : tradingFees.fixed_fees.default;

    // Apply the fee ratio directly as it's already in decimal form (e.g., 0.001 for 0.1%)
    const percentageFee = sendAmount * feeRatio;
    return sendAmount - percentageFee - fixedFee;
  };

  const displayAmount = amount
    ? calculateReceiveAmount(Number(amount)).toFixed(2)
    : "";

  const availableOffers = offers.filter((offer) => {
    const amountToCheck = Number(amount);

    const isValid =
      amountToCheck >= offer.minAmount && amountToCheck <= offer.maxAmount;

    return isValid;
  });

  const handleShowOffers = async () => {
    if (!amount) return;

    const numAmount = Number(amount);
    if (isNaN(numAmount) || numAmount < MOCK_AMOUNT_LIMITS.MIN) {
      toast({
        title: t("invalidAmount"),
        description: t("amountTooLow", {
          min: new Intl.NumberFormat("en-US").format(MOCK_AMOUNT_LIMITS.MIN),
          currency: currency.toUpperCase(),
        }),
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      // Lấy country code từ currency
      const countryCode =
        currency.toUpperCase() === "VND"
          ? "VN"
          : currency.toUpperCase() === "PHP"
            ? "PH"
            : "";

      if (!countryCode) {
        throw new Error("Unsupported currency");
      }

      // Gọi API để lấy danh sách offers
      const offersResponse = await getOffers();

      // Chuyển response thành array nếu cần
      const offersData = Array.isArray(offersResponse)
        ? offersResponse
        : (offersResponse.data as Offer[]);

      // Lọc offers theo currency và country code
      const filteredOffers = offersData.filter((offer) => {
        const isMatchingCurrency =
          offer.currency.toLowerCase() === currency.toLowerCase();
        const isMatchingCountry = offer.country_code === countryCode;
        const isSellOffer = offer.offer_type === "sell";
        const isActive = offer.is_active !== false;
        const isOnline = offer.online !== false;
        const isNotOwnOffer = Number(offer.user_id) !== Number(user?.id);

        return (
          isMatchingCurrency &&
          isMatchingCountry &&
          isSellOffer &&
          isActive &&
          isOnline &&
          isNotOwnOffer
        );
      });

      if (!filteredOffers || filteredOffers.length === 0) {
        toast({
          title: t("noOffersForCurrency"),
          description: t("noOffersForCurrencyDescription", {
            currency: currency.toUpperCase(),
          }),
          variant: "destructive",
        });
        setOffers([]);
        setShowOffers(true);
        return;
      }

      // Chuyển đổi API offers thành format MerchantOffer cho UI
      const merchantOffers: MerchantOffer[] = filteredOffers.map((offer) => {
        const paymentDetails =
          typeof offer.payment_details === "string"
            ? JSON.parse(offer.payment_details)
            : offer.payment_details;

        // Đảm bảo min_amount và max_amount là số
        const minAmount = parseFloat(String(offer.min_amount));
        const maxAmount = parseFloat(String(offer.max_amount));

        return {
          id: offer.id.toString(),
          merchantName: offer.merchant_display_name || `Merchant #${offer.id}`, // Use merchant_display_name if available
          rate: parseFloat(String(offer.price)),
          minAmount,
          maxAmount,
          accountInfo: {
            bank_name: paymentDetails?.bank_name || "Bank",
            account_number: paymentDetails?.account_number || "",
            account_holder_name: paymentDetails?.account_holder_name || "",
          },
        };
      });

      setOffers(merchantOffers);
      setShowOffers(true);
      setSelectedOfferId(undefined);
    } catch (error) {
      toast({
        title: t("errorFetchingOffers"),
        description: axios.isAxiosError(error)
          ? error.response?.data?.message
          : t("somethingWentWrong"),
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleConfirmDeposit = async () => {
    if (!amount || !selectedOfferId) return;

    setIsLoading(true);
    try {
      const depositAmount = Number(amount);
      const selectedOffer = offers.find(
        (offer) => offer.id === selectedOfferId,
      );

      if (!selectedOffer) {
        throw new Error("Selected offer not found");
      }

      // Chỉ cần tạo trade, fiat deposit sẽ được tạo tự động từ server
      const tradeResponse = await createTrade({
        offer_id: selectedOfferId,
        coin_amount: depositAmount,
      });

      // Redirect to trade detail page
      router.push(`/trade/${tradeResponse.id}`);
    } catch (error) {
      toast({
        title: t("errorCreatingDeposit"),
        description: axios.isAxiosError(error)
          ? error.response?.data?.message
          : t("somethingWentWrong"),
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  // Loading skeleton for authentication check
  const loadingSkeleton = (
    <FiatTransactionLayout
      title={`${t("title", { currency: currency.toUpperCase() })}`}
    >
      <div className="space-y-8">
        <div className="space-y-6">
          <div className="space-y-4">
            <div className="flex justify-end">
              <Skeleton className="h-5 w-32" />
            </div>

            <div className="space-y-2">
              <Skeleton className="h-4 w-40" />
              <Skeleton className="h-10 w-full" />
            </div>

            <Skeleton className="h-4 w-60" />
            <Skeleton className="h-5 w-48" />
            <Skeleton className="h-10 w-full" />
          </div>

          <Skeleton className="h-10 w-full" />
        </div>
      </div>
    </FiatTransactionLayout>
  );

  const getFeeDisplay = () => {
    if (isLoadingFees) {
      return <Skeleton className="h-4 w-60" />;
    }

    if (!tradingFees) {
      return (
        <div className="text-xs text-muted-foreground">
          {t("fee")}: {t("feesUnavailable")}
        </div>
      );
    }

    const currencyKey =
      currency.toLowerCase() as keyof typeof tradingFees.fee_ratios;

    // Use the specific currency fee ratio or default
    const feeRatio =
      tradingFees.fee_ratios[currencyKey] !== undefined
        ? tradingFees.fee_ratios[currencyKey]
        : tradingFees.fee_ratios.default;

    // Use the specific currency fixed fee or default
    const fixedFee =
      tradingFees.fixed_fees[currencyKey] !== undefined
        ? tradingFees.fixed_fees[currencyKey]
        : tradingFees.fixed_fees.default;

    return (
      <div className="text-xs text-muted-foreground">
        {t("fee")}: {feeRatio * 100}% +{" "}
        {new Intl.NumberFormat("en-US").format(fixedFee)}{" "}
        {currency.toUpperCase()}
      </div>
    );
  };

  const content = (
    <FiatTransactionLayout
      title={t("title", { currency: currency.toUpperCase() })}
    >
      <div className="space-y-8">
        <div className="space-y-6">
          <div className="space-y-4">
            <div className="flex justify-end">
              <p className="text-sm text-muted-foreground">
                {t("balance")}:{" "}
                {new Intl.NumberFormat("en-US").format(currentBalance)}{" "}
                {currency.toUpperCase()}
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="amount">{t("amountToDeposit")}</Label>
              <Input
                id="amount"
                type="text"
                inputMode="numeric"
                placeholder={`${new Intl.NumberFormat("en-US").format(MOCK_AMOUNT_LIMITS.MIN)} ${currency.toUpperCase()}`}
                value={
                  amount
                    ? new Intl.NumberFormat("en-US").format(Number(amount))
                    : ""
                }
                onChange={(e) => {
                  // Remove all non-numeric characters and store as raw number
                  const rawValue = e.target.value.replace(/[^0-9.]/g, "");
                  const numValue = Number(rawValue);

                  // Allow empty or valid numbers
                  if (rawValue === "" || !isNaN(numValue)) {
                    setAmount(rawValue);
                    setShowOffers(false);
                  }
                }}
                className="pl-2"
                disabled={isLoading}
              />
              <p className="text-sm text-muted-foreground">
                {t("min")}:{" "}
                {new Intl.NumberFormat("en-US").format(MOCK_AMOUNT_LIMITS.MIN)}{" "}
                {currency.toUpperCase()}
              </p>
            </div>

            {/* Fee information */}
            {getFeeDisplay()}

            {/* Calculated amount display */}
            {amount && (
              <div className="text-sm font-medium">
                {t("youWillReceive", {
                  amount: new Intl.NumberFormat("en-US").format(
                    Number(displayAmount),
                  ),
                })}
              </div>
            )}
          </div>

          {!showOffers ? (
            <Button
              className="w-full"
              onClick={handleShowOffers}
              disabled={!amount || isLoading}
            >
              {t("continue")}
            </Button>
          ) : (
            <>
              {availableOffers.length > 0 ? (
                <div className="space-y-2">
                  <Label>{t("selectMerchant")}</Label>
                  <div className="space-y-2">
                    {availableOffers.map((offer) => (
                      <div
                        key={offer.id}
                        className={`p-4 rounded-lg border cursor-pointer ${
                          selectedOfferId === offer.id
                            ? "border-primary bg-accent"
                            : "border-border"
                        }`}
                        onClick={() => setSelectedOfferId(offer.id)}
                      >
                        <div className="font-medium">{offer.merchantName}</div>
                      </div>
                    ))}
                  </div>
                  <Button
                    className="w-full"
                    onClick={handleConfirmDeposit}
                    disabled={!selectedOfferId || isLoading}
                  >
                    {t("confirmDeposit")}
                  </Button>
                </div>
              ) : (
                <div className="space-y-4">
                  <p className="text-sm text-muted-foreground">
                    {t("noMerchantAvailable")}
                  </p>
                  <Button
                    className="w-full"
                    onClick={() => setShowOffers(false)}
                    variant="outline"
                    disabled={isLoading}
                  >
                    {t("back")}
                  </Button>
                </div>
              )}
            </>
          )}
        </div>

        {/* Transaction History Card with link to wallet history */}
        <Card>
          <CardHeader>
            <CardTitle>
              {t("depositHistory", { fallback: "Deposit History" })}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              {t("viewHistoryDescription", {
                fallback:
                  "View all your deposit transactions in the wallet history section",
              })}
            </p>
          </CardContent>
          <CardFooter className="flex justify-end">
            <Button variant="outline" size="sm" asChild>
              <Link href={`/wallet/history/fiat/${currency.toLowerCase()}`}>
                {t("viewHistory", { fallback: "View History" })}
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
          </CardFooter>
        </Card>
      </div>
    </FiatTransactionLayout>
  );

  return (
    <ProtectedLayout loadingFallback={loadingSkeleton}>
      {content}
    </ProtectedLayout>
  );
}
