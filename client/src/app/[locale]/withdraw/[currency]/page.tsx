"use client";

import { FiatTransactionLayout } from "@/components/fiat-transaction-layout";
import { BankAccountSelector } from "@/components/bank-account-selector";
import { Button } from "@/components/ui/button";
import { Form, FormControl, FormField, FormItem } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/components/ui/use-toast";
import { useWallet } from "@/hooks/use-wallet";
import { BankAccount } from "@/lib/api/bank-accounts";
import { useBankAccounts } from "@/lib/api/hooks/use-bank-accounts";
import { getOffers, Offer } from "@/lib/api/merchant";
import { createTrade } from "@/lib/api/trades";
import { useUserStore } from "@/lib/store/user-store";
import { MerchantOffer } from "@/types/fiat-deposits";
import { zodResolver } from "@hookform/resolvers/zod";
import axios from "axios";
import { AlertCircle, ArrowRight, Wallet } from "lucide-react";
import { useTranslations } from "next-intl";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import * as z from "zod";

// Fee constants - should move to constants file later
const WITHDRAW_FEES = {
  PERCENTAGE: 0.1, // 0.1%
  FIXED: {
    VND: 5000,
    PHP: 10,
    NGN: 200,
  },
};

const P2P_AMOUNT_LIMITS = {
  MIN: 10000,
};

// Form schema for validation
const formSchema = z.object({
  bankAccountId: z.string().min(1, "Bank account is required"),
});

type FormValues = z.infer<typeof formSchema>;

export default function WithdrawPage() {
  const t = useTranslations("withdraw");
  const router = useRouter();
  const { toast } = useToast();
  const { currency } = useParams() as { currency: string };
  const [amount, setAmount] = useState("");
  const [selectedOfferId, setSelectedOfferId] = useState<string>();
  const [showOffers, setShowOffers] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [offers, setOffers] = useState<MerchantOffer[]>([]);
  const [selectedBankAccount, setSelectedBankAccount] =
    useState<BankAccount | null>(null);
  const { user } = useUserStore();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Get the country code based on currency
  const countryCode =
    currency.toUpperCase() === "VND"
      ? "VN"
      : currency.toUpperCase() === "PHP"
        ? "PH"
        : currency.toUpperCase() === "NGN"
          ? "NG"
          : "";

  // Form for bank account selection
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      bankAccountId: "",
    },
  });

  // Fetch bank accounts
  const { isLoading: isLoadingBankAccounts } = useBankAccounts({
    country_code: countryCode,
  });

  // Use the wallet hook to get real-time balance data
  const { data: walletData } = useWallet();

  // Get balance for the current currency directly from wallet data
  const currentBalance =
    walletData?.fiat_accounts.find(
      (account) => account.currency.toLowerCase() === currency.toLowerCase(),
    )?.balance || 0;

  const handleShowOffers = async () => {
    // Check if bank account is selected
    const { bankAccountId } = form.getValues();
    if (!bankAccountId) {
      toast({
        title: t("missingBankAccount"),
        description: t("pleaseSelectBankAccount"),
        variant: "destructive",
      });
      return;
    }

    if (!amount) {
      toast({
        title: t("missingAmount"),
        description: t("pleaseEnterAmount"),
        variant: "destructive",
      });
      return;
    }

    const numAmount = Number(amount);
    if (isNaN(numAmount) || numAmount < P2P_AMOUNT_LIMITS.MIN) {
      setErrorMessage(
        t("amountTooLow", {
          min: new Intl.NumberFormat("en-US").format(P2P_AMOUNT_LIMITS.MIN),
          currency: currency.toUpperCase(),
        }),
      );
      return;
    }

    // Check if the user has sufficient balance
    if (numAmount > currentBalance) {
      setErrorMessage(
        t("insufficientBalanceDescription", {
          available: new Intl.NumberFormat("en-US").format(currentBalance),
          currency: currency.toUpperCase(),
        }),
      );
      return;
    }

    // Clear any error message
    setErrorMessage(null);

    setIsLoading(true);
    try {
      if (!countryCode) {
        throw new Error("Unsupported currency");
      }

      // Call API to get offers list
      const offersResponse = await getOffers();

      // Convert response to array if needed
      const offersData = Array.isArray(offersResponse)
        ? offersResponse
        : (offersResponse.data as Offer[]);

      // Filter offers by currency, country code, and amount range
      const numAmount = Number(amount);
      const filteredOffers = offersData.filter((offer) => {
        const isMatchingCurrency =
          offer.currency.toLowerCase() === currency.toLowerCase();
        const isMatchingCountry = offer.country_code === countryCode;
        const isBuyOffer = offer.offer_type === "buy";
        const isActive = offer.is_active !== false;
        const isOnline = offer.online !== false;
        const isNotOwnOffer = Number(offer.user_id) !== Number(user?.id);

        // Check if the withdraw amount is within the offer's min/max range
        const minAmount = parseFloat(String(offer.min_amount));
        const maxAmount = parseFloat(String(offer.max_amount));
        const isAmountInRange =
          numAmount >= minAmount && numAmount <= maxAmount;

        return (
          isMatchingCurrency &&
          isMatchingCountry &&
          isBuyOffer &&
          isActive &&
          isOnline &&
          isNotOwnOffer &&
          isAmountInRange
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

      // Convert API offers to MerchantOffer format for UI
      const merchantOffers: MerchantOffer[] = filteredOffers.map((offer) => {
        const paymentDetails =
          typeof offer.payment_details === "string"
            ? JSON.parse(offer.payment_details)
            : offer.payment_details;

        // Ensure min_amount and max_amount are numbers
        const minAmount = parseFloat(String(offer.min_amount));
        const maxAmount = parseFloat(String(offer.max_amount));

        return {
          id: offer.id.toString(),
          merchantName: offer.merchant_display_name || `Merchant #${offer.id}`,
          rate: parseFloat(String(offer.price)),
          minAmount,
          maxAmount,
          accountInfo: {
            bank_name: paymentDetails?.bank_name || "",
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

  const handleConfirmWithdraw = async () => {
    if (!amount || !selectedOfferId) return;

    // Get the bank account ID from the form
    const { bankAccountId } = form.getValues();
    if (!bankAccountId) {
      toast({
        title: t("missingBankAccount"),
        description: t("pleaseSelectBankAccount"),
        variant: "destructive",
      });
      return;
    }

    if (!selectedBankAccount) {
      toast({
        title: t("missingBankAccount"),
        description: t("pleaseSelectBankAccount"),
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const withdrawAmount = Number(amount);
      const selectedOffer = offers.find(
        (offer) => offer.id === selectedOfferId,
      );

      if (!selectedOffer) {
        throw new Error("Selected offer not found");
      }

      // Create trade with additional bank information
      const tradeResponse = await createTrade({
        offer_id: selectedOfferId,
        coin_amount: withdrawAmount,
        bank_name: selectedBankAccount.bank_name,
        bank_account_name: selectedBankAccount.account_name,
        bank_account_number: selectedBankAccount.account_number,
        bank_branch: selectedBankAccount.branch,
      });

      // Redirect to trade detail page
      router.push(`/trade/${tradeResponse.id}`);
    } catch (error) {
      toast({
        title: t("errorCreatingWithdrawal"),
        description: axios.isAxiosError(error)
          ? error.response?.data?.message
          : t("somethingWentWrong"),
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  // Handle bank account selection
  const handleBankAccountSelect = (account: BankAccount) => {
    form.setValue("bankAccountId", account.id.toString());
    setSelectedBankAccount(account);
    setErrorMessage(null);
  };

  return (
    <FiatTransactionLayout
      title={t("title", { currency: currency.toUpperCase() })}
    >
      <Form {...form}>
        <div className="max-w-lg mx-auto space-y-6">
          {errorMessage && (
            <div className="relative w-full rounded-lg border border-destructive/50 p-4 text-destructive dark:border-destructive mb-6">
              <AlertCircle className="h-4 w-4 absolute left-4 top-4" />
              <div className="text-sm pl-7">{errorMessage}</div>
            </div>
          )}

          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <Wallet className="h-5 w-5 mr-2" />
              <span className="font-medium">{t("balance")}</span>
            </div>
            <div className="text-sm text-muted-foreground">
              {t("balance")}:{" "}
              {new Intl.NumberFormat("en-US").format(currentBalance)}{" "}
              {currency.toUpperCase()}
            </div>
          </div>

          {/* Amount Input */}
          <div className="space-y-1">
            <Label className="text-sm text-muted-foreground">
              {t("amountToWithdraw")}
            </Label>
            <div className="relative">
              <Input
                type="text"
                inputMode="numeric"
                placeholder={`0 ${currency.toUpperCase()}`}
                value={
                  amount
                    ? new Intl.NumberFormat("en-US").format(Number(amount))
                    : ""
                }
                onChange={(e) => {
                  // Remove all non-numeric characters and store as raw number
                  const rawValue = e.target.value.replace(/[^0-9.]/g, "");
                  setAmount(rawValue);
                  setShowOffers(false);
                  setErrorMessage(null);
                }}
                className="w-full pr-16 h-11"
              />
              <div className="absolute right-3 top-2.5 text-sm text-muted-foreground">
                {currency.toUpperCase()}
              </div>
            </div>
            <div className="text-xs text-muted-foreground mt-1">
              {t.rich("feeDisplay", {
                percentage: WITHDRAW_FEES.PERCENTAGE,
                fixed:
                  WITHDRAW_FEES.FIXED[
                    currency.toUpperCase() as keyof typeof WITHDRAW_FEES.FIXED
                  ] || 0,
                currency: currency.toUpperCase(),
              })}
            </div>
          </div>

          {/* Bank Account Selection */}
          <div className="space-y-2">
            <Label className="text-sm text-muted-foreground">
              {t("withdrawalAccount")}
            </Label>

            <FormField
              control={form.control}
              name="bankAccountId"
              render={() => (
                <FormItem>
                  <FormControl>
                    <BankAccountSelector
                      control={form.control}
                      name="bankAccountId"
                      onAccountSelect={handleBankAccountSelect}
                    />
                  </FormControl>
                </FormItem>
              )}
            />
          </div>

          {/* Continue button */}
          <Button
            className="w-full bg-black hover:bg-black/90 text-white h-11"
            onClick={handleShowOffers}
            disabled={isLoading || isLoadingBankAccounts}
          >
            {isLoading ? (
              <div className="flex items-center gap-2">
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"></div>
                <span>{t("loading")}</span>
              </div>
            ) : (
              <div className="flex items-center justify-center gap-2">
                <span>{t("continue")}</span>
                <ArrowRight className="h-4 w-4" />
              </div>
            )}
          </Button>

          {/* Merchant Selection (only shows after amount and bank selection) */}
          {showOffers && (
            <div className="space-y-4">
              <div className="font-medium">{t("selectMerchant")}</div>

              {offers.length > 0 ? (
                <>
                  <div className="space-y-3">
                    {offers.map((offer) => (
                      <div
                        key={offer.id}
                        className={`p-3 rounded-md border cursor-pointer ${
                          selectedOfferId === offer.id
                            ? "border-primary bg-accent/30"
                            : "border-border hover:bg-accent/10"
                        }`}
                        onClick={() => setSelectedOfferId(offer.id)}
                      >
                        <div className="flex justify-between items-center">
                          <div className="font-medium">
                            {offer.merchantName}
                          </div>
                          <div className="text-sm text-muted-foreground">
                            {t("limits")}:{" "}
                            {new Intl.NumberFormat("en-US").format(
                              offer.minAmount,
                            )}{" "}
                            -
                            {new Intl.NumberFormat("en-US").format(
                              offer.maxAmount,
                            )}{" "}
                            {currency.toUpperCase()}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="flex gap-3 pt-2">
                    <Button
                      variant="outline"
                      onClick={() => setShowOffers(false)}
                      className="flex-1"
                      disabled={isLoading}
                    >
                      {t("back")}
                    </Button>

                    <Button
                      className="flex-1 bg-black hover:bg-black/90 text-white"
                      onClick={handleConfirmWithdraw}
                      disabled={!selectedOfferId || isLoading}
                    >
                      {isLoading ? (
                        <div className="flex items-center gap-2">
                          <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"></div>
                          <span>{t("loading")}</span>
                        </div>
                      ) : (
                        t("withdraw")
                      )}
                    </Button>
                  </div>
                </>
              ) : (
                <div className="py-6 text-center">
                  <p className="text-sm text-muted-foreground">
                    {t("noOffersAvailable")}
                  </p>
                  <Button
                    className="mt-4"
                    onClick={() => setShowOffers(false)}
                    variant="outline"
                  >
                    {t("back")}
                  </Button>
                </div>
              )}
            </div>
          )}
        </div>
      </Form>
    </FiatTransactionLayout>
  );
}
