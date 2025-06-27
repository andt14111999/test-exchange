"use client";

import { BankAccountSelector } from "@/components/bank-account-selector";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { useWallet } from "@/hooks/use-wallet";
import { FiatAccount } from "@/lib/api/balance";
import { BankAccount } from "@/lib/api/bank-accounts";
import { usePaymentMethods } from "@/lib/api/hooks/use-payment-methods";
import { createOffer, getOffer, updateOffer } from "@/lib/api/merchant";
import { PaymentMethod } from "@/lib/api/payment-methods";
import {
  FIAT_CURRENCIES,
  MAX_AMOUNT_PER_TRANSACTION,
  P2P_AMOUNT_LIMITS,
} from "@/lib/constants";
import { FiatCurrency } from "@/lib/types";
import { handleApiError } from "@/lib/utils/error-handler";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  AlertCircle,
  ArrowDown,
  ArrowUp,
  Banknote,
  Clock,
  Coins,
  DollarSign,
  Wallet,
} from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import * as z from "zod";
import { NumberInputWithCommas } from "@/components/ui/number-input-with-commas";

// Custom ErrorMessage component for consistent error styling
const ErrorMessage = ({ message }: { message?: string }) => {
  const t = useTranslations("merchant");

  if (!message) return null;

  // Check if this is a translation key (no spaces, short text)
  const translatedMessage = message.includes(" ")
    ? message
    : t(`validation.${message}`);

  return (
    <div className="relative w-full rounded-lg border border-destructive/50 p-4 mt-2 text-destructive dark:border-destructive">
      <AlertCircle className="h-4 w-4 absolute left-4 top-4" />
      <div className="text-sm pl-7">{translatedMessage}</div>
    </div>
  );
};

// Form Errors component that displays all errors in the form
const FormErrors = ({
  errors,
  isSubmitted,
}: {
  errors: Record<string, { message?: string }>;
  isSubmitted: boolean;
}) => {
  const t = useTranslations("merchant");

  if (Object.keys(errors).length === 0 || !isSubmitted) return null;

  // Map field keys to more readable names with translations
  const fieldNames: Record<string, string> = {
    amount: t("fields.amount"),
    minAmount: t("fields.minAmount"),
    maxAmount: t("fields.maxAmount"),
    bankAccountId: t("fields.bankAccountId"),
    paymentMethodId: t("fields.paymentMethodId"),
    paymentTime: t("fields.paymentTime"),
    paymentDetails: t("fields.paymentDetails"),
    type: t("fields.type"),
    fiatCurrency: t("fields.fiatCurrency"),
    countryCode: t("fields.countryCode"),
  };

  return (
    <div className="relative w-full rounded-lg border border-destructive/50 p-4 my-6 text-destructive dark:border-destructive">
      <AlertCircle className="h-5 w-5 absolute left-4 top-4" />
      <div className="text-sm pl-8">
        <p className="font-medium mb-3 text-base">
          {t("formErrors.checkFields")}
        </p>
        <ul className="list-disc pl-5 space-y-2">
          {Object.entries(errors).map(([field, error]) => (
            <li key={field} className="text-sm">
              <span className="font-medium">{fieldNames[field] || field}:</span>{" "}
              {error?.message || t("formErrors.invalidInfo")}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

const formSchema = z
  .object({
    type: z.enum(["buy", "sell"] as const),
    fiatCurrency: z
      .string()
      .transform((val) => val.toUpperCase() as FiatCurrency),
    amount: z.number().min(P2P_AMOUNT_LIMITS.MIN).max(P2P_AMOUNT_LIMITS.MAX),
    price: z.number().default(1), // Price is always 1
    minAmount: z.number().min(P2P_AMOUNT_LIMITS.MIN).max(P2P_AMOUNT_LIMITS.MAX),
    maxAmount: z.number().min(P2P_AMOUNT_LIMITS.MIN).max(P2P_AMOUNT_LIMITS.MAX),
    bankAccountId: z.union([z.string(), z.number()]).optional(),
    paymentMethodId: z.string().min(1, "paymentMethodRequired"),
    paymentTime: z.number().min(5).max(180), // Payment time in minutes
    paymentDetails: z.string().optional(), // Optional for buy offers
    countryCode: z.string().length(2), // Country code (e.g., VN, US)
    availableBalance: z.number().optional(), // Available balance from wallet, used for validation
  })
  .superRefine((data, ctx) => {
    if (
      data.type === "sell" &&
      (!data.bankAccountId || data.bankAccountId === "")
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "bankAccountRequiredForSell",
        path: ["bankAccountId"],
      });
    }
    if (data.minAmount > data.maxAmount) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "minAmountLessThanMax",
        path: ["minAmount"],
      });
    }
    if (
      data.type === "sell" &&
      data.availableBalance !== undefined &&
      data.amount > data.availableBalance
    ) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "amountExceedsBalance",
        path: ["amount"],
      });
    }
  });

type FormValues = z.infer<typeof formSchema>;

export default function CreateOffer() {
  const t = useTranslations("merchant");
  const router = useRouter();
  const searchParams = useSearchParams();
  const offerId = searchParams.get("id");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isOfferLoaded, setIsOfferLoaded] = useState(false);
  const isEditMode = !!offerId;
  const [selectedBankAccount, setSelectedBankAccount] =
    useState<BankAccount | null>(null);
  const [selectedPaymentMethod, setSelectedPaymentMethod] =
    useState<PaymentMethod | null>(null);
  const [availableBalance, setAvailableBalance] = useState<number | undefined>(
    undefined,
  );

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      type: "sell",
      fiatCurrency: "VND",
      amount: P2P_AMOUNT_LIMITS.MIN,
      price: 1, // Always 1
      minAmount: P2P_AMOUNT_LIMITS.MIN,
      maxAmount: P2P_AMOUNT_LIMITS.MAX,
      bankAccountId: undefined,
      paymentMethodId: "",
      paymentTime: 15,
      paymentDetails: undefined,
      countryCode: "VN",
      availableBalance: undefined,
    },
    mode: "onSubmit",
    reValidateMode: "onBlur",
  });

  // Get wallet data
  const { data: walletData } = useWallet();

  // Update available balance when currency changes
  const updateAvailableBalance = useCallback(
    (currency: FiatCurrency | undefined) => {
      if (walletData && currency && typeof currency === "string") {
        const account = walletData.fiat_accounts.find(
          (acc: FiatAccount) =>
            acc.currency.toUpperCase() === currency.toUpperCase(),
        );

        if (account) {
          const balance = account.balance - (account.frozen_balance || 0);
          setAvailableBalance(balance);
          form.setValue("availableBalance", balance);
          return balance;
        }
      }

      setAvailableBalance(undefined);
      form.setValue("availableBalance", undefined);
      return undefined;
    },
    [walletData, form],
  );

  // Update default amount when wallet data changes
  useEffect(() => {
    if (walletData) {
      const selectedCurrency = form.getValues("fiatCurrency");
      if (selectedCurrency) {
        const balance = updateAvailableBalance(selectedCurrency);

        // Only auto-set amount for Withdraw offers (sell) in new offer mode
        if (
          balance !== undefined &&
          !isEditMode &&
          form.getValues("type") === "sell"
        ) {
          form.setValue("amount", balance);
        }
      }
    }
  }, [walletData, form, isEditMode, updateAvailableBalance]);

  // Watch currency changes to update default amount
  const selectedCurrency = form.watch("fiatCurrency");
  useEffect(() => {
    if (walletData && selectedCurrency) {
      const balance = updateAvailableBalance(selectedCurrency);

      // Only auto-set amount for Withdraw offers (sell) and not in edit mode
      if (
        balance !== undefined &&
        !isEditMode &&
        form.getValues("type") === "sell"
      ) {
        form.setValue("amount", balance);
      }
    }
  }, [selectedCurrency, walletData, form, isEditMode, updateAvailableBalance]);

  // Watch amount changes to update maxAmount based on the 300 million rule
  const amount = form.watch("amount");
  useEffect(() => {
    if (amount < MAX_AMOUNT_PER_TRANSACTION) {
      form.setValue("maxAmount", amount);
    } else {
      form.setValue("maxAmount", MAX_AMOUNT_PER_TRANSACTION);
    }
  }, [amount, form]);

  // Watch offer type changes to clear amount fields when switching from sell to buy
  const watchedOfferType = form.watch("type");
  useEffect(() => {
    // Only clear amounts when switching from sell to buy (not during initial load or edit mode)
    if (!isEditMode && watchedOfferType === "buy") {
      // Clear amount fields when switching to deposit offer (buy)
      form.setValue("amount", P2P_AMOUNT_LIMITS.MIN);
      form.setValue("minAmount", P2P_AMOUNT_LIMITS.MIN);
      form.setValue("maxAmount", P2P_AMOUNT_LIMITS.MAX);
    }
  }, [watchedOfferType, form, isEditMode]);

  // Fetch payment methods
  const { data: paymentMethodsResponse, isLoading: isLoadingPaymentMethods } =
    usePaymentMethods(undefined, true); // Only enabled payment methods

  // Use useMemo to ensure stable reference
  const paymentMethods = useMemo(() => {
    return Array.isArray(paymentMethodsResponse)
      ? paymentMethodsResponse
      : (paymentMethodsResponse?.data as PaymentMethod[]) || [];
  }, [paymentMethodsResponse]);

  // Memoize the payment methods array to keep it stable across renders
  const memoizedPaymentMethods = useMemo(
    () => paymentMethods,
    [paymentMethods],
  );

  // Define fetchOfferData with useCallback before it's used
  const fetchOfferData = useCallback(
    async (id: number) => {
      try {
        setIsLoading(true);

        try {
          const response = await getOffer(id);

          // We need to cast to handle inconsistent API response structure
          // Use a type assertion that satisfies the linter but still allows flexible handling
          const apiResponse = response as {
            data?: unknown;
            id?: number;
            [key: string]: unknown;
          };

          // Create a typed object for extracted offer data
          let offerData: {
            id?: number;
            offer_type?: string;
            total_amount?: string | number;
            price?: string | number;
            min_amount?: string | number;
            max_amount?: string | number;
            payment_method_id?: number | string;
            payment_time?: number;
            payment_details?:
              | string
              | {
                  payment_instructions?: string;
                  bank_name?: string;
                  bank_account_name?: string;
                  bank_account_number?: string;
                  bank_id?: string | number;
                  [key: string]: unknown;
                };
            country_code?: string;
            currency?: string;
            [key: string]: unknown;
          } | null = null;

          if (apiResponse && apiResponse.data) {
            offerData = Array.isArray(apiResponse.data)
              ? apiResponse.data[0]
              : apiResponse.data;
          } else if (apiResponse && apiResponse.id) {
            offerData = apiResponse;
          } else {
            toast.error(t("offerNotFound"));
            setIsLoading(false);
            setIsOfferLoaded(true);
            return;
          }

          if (!offerData) {
            toast.error(t("offerNotFound"));
            setIsLoading(false);
            setIsOfferLoaded(true);
            return;
          }

          // Helper function to safely convert to number
          const safeNumber = (value: unknown): number => {
            if (value === null || value === undefined) return 0;
            const num = Number(value);
            return isNaN(num) ? 0 : num;
          };

          // Extract payment details
          let paymentDetailsValue = "";
          if (typeof offerData.payment_details === "string") {
            paymentDetailsValue = offerData.payment_details;
          } else if (
            offerData.payment_details &&
            typeof offerData.payment_details === "object"
          ) {
            paymentDetailsValue =
              offerData.payment_details.payment_instructions || "";
          }

          // Force convert to proper number for safety
          const totalAmount = safeNumber(offerData.total_amount);

          // Extract bank account ID from payment details for sell offers
          let bankAccountId: string | undefined;
          if (offerData.offer_type === "sell" && offerData.payment_details) {
            const bankDetails =
              typeof offerData.payment_details === "object"
                ? offerData.payment_details
                : {};
            bankAccountId = bankDetails.bank_id
              ? String(bankDetails.bank_id)
              : undefined;
          }

          const formValues = {
            type: offerData.offer_type as "buy" | "sell",
            fiatCurrency: (offerData.currency?.toUpperCase() ||
              "VND") as FiatCurrency,
            amount: totalAmount,
            price: safeNumber(offerData.price),
            minAmount: safeNumber(offerData.min_amount),
            maxAmount: safeNumber(offerData.max_amount),
            bankAccountId: bankAccountId,
            paymentTime: safeNumber(offerData.payment_time),
            paymentDetails: paymentDetailsValue,
            countryCode: offerData.country_code || "VN",
            paymentMethodId: offerData.payment_method_id
              ? String(offerData.payment_method_id)
              : "",
          };

          // Set form values with retrieved data
          form.reset(formValues);

          // Force set all important values again after reset to ensure they're applied
          setTimeout(() => {
            form.setValue("amount", totalAmount);
            // Ensure currency is uppercase for enum validation
            const currency = offerData.currency?.toUpperCase() as FiatCurrency;
            form.setValue("fiatCurrency", currency || "VND");
            if (offerData.payment_method_id) {
              form.setValue(
                "paymentMethodId",
                String(offerData.payment_method_id),
              );
            }
            // Force set bank account ID if it's a sell offer
            if (bankAccountId) {
              form.setValue("bankAccountId", bankAccountId);
            }
            // Force validate form after setting values
            form.trigger();
          }, 100);

          // Also need to set the selected payment method
          if (offerData.payment_method_id) {
            const method = paymentMethods.find(
              (pm) => String(pm.id) === String(offerData.payment_method_id),
            );

            if (method) {
              setSelectedPaymentMethod(method);
            }
          }

          // Set offer as loaded
          setIsOfferLoaded(true);
        } catch {
          toast.error(t("fetchOfferFailed"));
          setIsOfferLoaded(true);
        }
      } catch (error) {
        handleApiError(error, t("fetchOfferFailed"));
        setIsOfferLoaded(true); // Mark as loaded even if error occurred
      } finally {
        setIsLoading(false);
      }
    },
    [t, paymentMethods, form],
  );

  // Update default paymentMethodId when payment methods are loaded
  useEffect(() => {
    if (paymentMethods.length > 0 && !form.getValues("paymentMethodId")) {
      const defaultId = String(paymentMethods[0].id);
      form.setValue("paymentMethodId", defaultId, {
        shouldValidate: true,
        shouldDirty: true,
        shouldTouch: true,
      });
      setSelectedPaymentMethod(paymentMethods[0]);

      // Force validate the entire form
      form.trigger();
    }
  }, [paymentMethods, form]);

  // Fetch offer data if in edit mode
  useEffect(() => {
    if (
      offerId &&
      isEditMode &&
      !isOfferLoaded &&
      memoizedPaymentMethods.length > 0 &&
      !isLoadingPaymentMethods
    ) {
      fetchOfferData(parseInt(offerId));
    }
  }, [
    offerId,
    isEditMode,
    isOfferLoaded,
    memoizedPaymentMethods,
    isLoadingPaymentMethods,
    fetchOfferData,
  ]);

  // Get current offer type to conditionally display bank account selector
  const offerType = form.watch("type");
  const isWithdrawOffer = offerType === "sell"; // Withdraw Offer
  const isDepositOffer = offerType === "buy"; // Deposit Offer

  // Memoize handlers to prevent infinite loops
  const handlePaymentMethodChange = useCallback(
    (id: string) => {
      const method = paymentMethods.find((pm) => String(pm.id) === id);
      setSelectedPaymentMethod(method || null);
      // Set the value and force form validation
      form.setValue("paymentMethodId", id, {
        shouldValidate: true,
        shouldDirty: true,
        shouldTouch: true,
      });

      // Force validate the entire form
      form.trigger();
    },
    [paymentMethods, form],
  );

  const handleBankAccountChange = useCallback(
    (bankAccount: BankAccount) => {
      setSelectedBankAccount(bankAccount);
      if (bankAccount?.id) {
        // Set the value and force form validation
        form.setValue("bankAccountId", bankAccount.id, {
          shouldValidate: true,
          shouldDirty: true,
          shouldTouch: true,
        });

        // Force validate the entire form
        form.trigger();
      }
    },
    [form],
  );

  async function onSubmit(values: FormValues) {
    try {
      setIsSubmitting(true);

      // Validate toàn bộ form
      const result = await form.trigger();
      if (!result) {
        setIsSubmitting(false);
        return; // Dừng lại nếu form không valid
      }

      // Kiểm tra bank account nếu là sell offer
      if (values.type === "sell" && !selectedBankAccount) {
        toast.error(t("bankDetailsRequired"), {
          position: "top-right",
          duration: 3000,
          className: "shadow-lg border border-gray-200 dark:border-gray-800",
        });
        setIsSubmitting(false);
        return;
      }

      let paymentDetails;

      // If it's a sell offer and we have a selected bank account, create structured payment details
      if (values.type === "sell" && selectedBankAccount) {
        paymentDetails = {
          bank_name: selectedBankAccount.bank_name,
          bank_account_number: selectedBankAccount.account_number,
          bank_account_name: selectedBankAccount.account_name,
          bank_id: selectedBankAccount.id,
        };
      } else if (values.type === "sell") {
        // If no bank details available but it's a sell offer, show error
        toast.error(t("bankDetailsRequired"), {
          position: "top-right",
          duration: 3000,
          className: "shadow-lg border border-gray-200 dark:border-gray-800",
        });
        setIsSubmitting(false);
        return;
      } else if (!values.paymentDetails) {
        // For buy offers without payment details
        paymentDetails = {
          payment_instructions: t("defaultPaymentInstructions"),
        };
      } else {
        // If there's custom payment details for buy offers
        paymentDetails = {
          payment_instructions: values.paymentDetails,
        };
      }

      // Ensure numerical values are proper numbers
      const amount = Number(values.amount);
      const minAmount = Number(values.minAmount);
      const maxAmount = Number(values.maxAmount);

      if (isEditMode && offerId) {
        // Update existing offer
        // IMPORTANT: Do NOT include offer_type when updating!
        const updateData = {
          coin_currency: values.fiatCurrency,
          currency: values.fiatCurrency,
          price: 1, // Always 1
          total_amount: amount,
          min_amount: minAmount,
          max_amount: maxAmount,
          payment_method_id: values.paymentMethodId
            ? parseInt(values.paymentMethodId)
            : selectedPaymentMethod?.id || 1,
          payment_time: values.paymentTime,
          payment_details: paymentDetails,
          country_code: values.countryCode,
          is_active: true,
          coin_amount: amount,
        };

        // Convert offerId to number
        const offerIdNumber = parseInt(offerId);
        await updateOffer(offerIdNumber, updateData);
        toast.success(t("offerUpdatedSuccess"), {
          position: "top-right",
          duration: 3000,
          className: "shadow-lg border border-gray-200 dark:border-gray-800",
        });
      } else {
        // Create new offer
        const createData = {
          offer_type: values.type, // Only include when creating
          coin_currency: values.fiatCurrency,
          currency: values.fiatCurrency,
          price: 1, // Always 1
          total_amount: amount,
          min_amount: minAmount,
          max_amount: maxAmount,
          payment_method_id: values.paymentMethodId
            ? parseInt(values.paymentMethodId)
            : selectedPaymentMethod?.id || 1,
          payment_time: values.paymentTime,
          payment_details: paymentDetails,
          country_code: values.countryCode,
          is_active: true,
          coin_amount: amount,
        };

        await createOffer(createData);
        toast.success(t("offerCreatedSuccess"), {
          position: "top-right",
          duration: 3000,
          className: "shadow-lg border border-gray-200 dark:border-gray-800",
        });
      }

      router.push("/merchant/manage-offers");
    } catch (error) {
      handleApiError(
        error,
        isEditMode ? t("offerUpdateFailed") : t("offerCreationFailed"),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div
      className="container max-w-3xl mx-auto py-12 px-4 sm:px-6"
      data-testid="create-offer-component"
    >
      <Card className="border-t-4 border-primary shadow-lg">
        <CardHeader className="space-y-6 pb-8">
          <div className="flex items-center justify-center">
            <div className="rounded-full bg-primary/10 p-4">
              <Wallet className="h-8 w-8 text-primary" />
            </div>
          </div>
          <div className="space-y-2 text-center">
            <CardTitle className="text-3xl font-bold" data-testid="card-title">
              {isEditMode ? t("editOffer") : t("createOffer")}
            </CardTitle>
            <CardDescription className="text-lg" data-testid="card-description">
              {isEditMode
                ? t("editOfferDescription")
                : t("createOfferDescription")}
            </CardDescription>
          </div>
        </CardHeader>
        <CardContent data-testid="card-content">
          {isLoading ? (
            <div className="flex justify-center items-center py-12">
              <div
                data-testid="loading-spinner"
                className="animate-spin rounded-full h-10 w-10 border-4 border-primary border-t-transparent"
              ></div>
            </div>
          ) : (
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit(onSubmit)}
                className="space-y-8"
                role="form"
                data-testid="create-offer-form"
              >
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  {/* Offer Type */}
                  <FormField
                    control={form.control}
                    name="type"
                    render={({ field, fieldState }) => (
                      <FormItem className="space-y-3">
                        <FormLabel className="flex items-center gap-2 text-base">
                          <Coins className="h-5 w-5" />
                          {t("offerType")}
                        </FormLabel>
                        <Select
                          disabled={isEditMode}
                          value={field.value}
                          onValueChange={field.onChange}
                          data-testid="offer-type-select"
                        >
                          <FormControl>
                            <SelectTrigger className="h-12">
                              <SelectValue placeholder={t("selectOfferType")} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="buy">
                              <div className="flex items-center gap-2">
                                <ArrowDown className="h-5 w-5 text-green-500" />
                                <span className="font-medium">
                                  {t("depositOffer")}
                                </span>
                              </div>
                            </SelectItem>
                            <SelectItem value="sell">
                              <div className="flex items-center gap-2">
                                <ArrowUp className="h-5 w-5 text-red-500" />
                                <span className="font-medium">
                                  {t("withdrawOffer")}
                                </span>
                              </div>
                            </SelectItem>
                          </SelectContent>
                        </Select>
                        {fieldState.error && (
                          <ErrorMessage message={fieldState.error.message} />
                        )}
                        <div className="hidden">
                          <FormMessage data-testid="form-message" />
                        </div>
                      </FormItem>
                    )}
                  />

                  {/* Currency */}
                  <FormField
                    control={form.control}
                    name="fiatCurrency"
                    render={({ field, fieldState }) => (
                      <FormItem className="space-y-3">
                        <FormLabel className="flex items-center gap-2 text-base">
                          <DollarSign className="h-5 w-5" />
                          {t("fiatCurrency")}
                        </FormLabel>
                        <Select
                          value={field.value}
                          onValueChange={field.onChange}
                          disabled={isEditMode}
                          data-testid="fiat-currency-select"
                        >
                          <FormControl>
                            <SelectTrigger className="h-12">
                              <SelectValue placeholder={t("selectCurrency")} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {FIAT_CURRENCIES.map((currency) => (
                              <SelectItem key={currency} value={currency}>
                                <span className="font-medium">{currency}</span>
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        {fieldState.error && (
                          <ErrorMessage message={fieldState.error.message} />
                        )}
                        <div className="hidden">
                          <FormMessage data-testid="form-message" />
                        </div>
                      </FormItem>
                    )}
                  />

                  {/* Country */}
                  <FormField
                    control={form.control}
                    name="countryCode"
                    render={({ field, fieldState }) => (
                      <FormItem className="space-y-3">
                        <FormLabel className="flex items-center gap-2 text-base">
                          <Banknote className="h-5 w-5" />
                          {t("country")}
                        </FormLabel>
                        <Select
                          value={field.value}
                          onValueChange={field.onChange}
                          disabled={isEditMode}
                        >
                          <FormControl>
                            <SelectTrigger className="h-12">
                              <SelectValue placeholder={t("selectCountry")} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="VN">
                              <span className="font-medium">Vietnam</span>
                            </SelectItem>
                            <SelectItem value="US">
                              <span className="font-medium">United States</span>
                            </SelectItem>
                            <SelectItem value="SG">
                              <span className="font-medium">Singapore</span>
                            </SelectItem>
                            <SelectItem value="PH">
                              <span className="font-medium">Philippines</span>
                            </SelectItem>
                          </SelectContent>
                        </Select>
                        {fieldState.error && (
                          <ErrorMessage message={fieldState.error.message} />
                        )}
                        <div className="hidden">
                          <FormMessage data-testid="form-message" />
                        </div>
                      </FormItem>
                    )}
                  />

                  {/* Payment Time */}
                  <FormField
                    control={form.control}
                    name="paymentTime"
                    render={({ field, fieldState }) => (
                      <FormItem className="space-y-3">
                        <FormLabel className="flex items-center gap-2 text-base">
                          <Clock className="h-5 w-5" />
                          {t("paymentTimeMinutes")}
                        </FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            inputMode="numeric"
                            className="h-12"
                            value={field.value === null ? "" : field.value}
                            onChange={(e) => {
                              const inputValue = e.target.value;
                              // Allow empty input field for UX but track as null
                              if (inputValue === "") {
                                field.onChange(null);
                                return;
                              }

                              const value = parseInt(inputValue);
                              if (!isNaN(value)) {
                                field.onChange(value);
                              }
                            }}
                          />
                        </FormControl>
                        <FormDescription>
                          {t("paymentTimeDescription")}
                        </FormDescription>
                        {fieldState.error && (
                          <ErrorMessage message={fieldState.error.message} />
                        )}
                        <div className="hidden">
                          <FormMessage data-testid="form-message" />
                        </div>
                      </FormItem>
                    )}
                  />
                </div>

                <Separator className="my-8" />

                <div className="space-y-6">
                  <h3 className="text-xl font-semibold flex items-center gap-2">
                    {t("amountSettings")}
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {/* Total Amount */}
                    <FormField
                      control={form.control}
                      name="amount"
                      render={({ field }) => (
                        <FormItem className="space-y-3">
                          <FormLabel className="text-base">
                            {t("totalAmount")}
                          </FormLabel>
                          <FormControl>
                            <NumberInputWithCommas
                              value={field.value?.toString() ?? ""}
                              onChange={(v) =>
                                field.onChange(v === "" ? null : parseFloat(v))
                              }
                              className="h-12"
                              step="0.01"
                              min="0"
                              data-testid="total-amount-input"
                            />
                          </FormControl>
                          {/* Only show available balance for Withdraw offers */}
                          {isWithdrawOffer &&
                            availableBalance !== undefined && (
                              <FormDescription>
                                {t("availableBalance")}: {availableBalance}{" "}
                                {selectedCurrency}
                              </FormDescription>
                            )}
                          <div className="hidden">
                            <FormMessage data-testid="form-message" />
                          </div>
                        </FormItem>
                      )}
                    />

                    {/* Min Amount */}
                    <FormField
                      control={form.control}
                      name="minAmount"
                      render={({ field }) => (
                        <FormItem className="space-y-3">
                          <FormLabel className="text-base">
                            {t("minAmount")}
                          </FormLabel>
                          <FormControl>
                            <NumberInputWithCommas
                              value={field.value?.toString() ?? ""}
                              onChange={(v) =>
                                field.onChange(v === "" ? null : parseFloat(v))
                              }
                              className="h-12"
                              step="0.01"
                              min="0"
                              data-testid="min-amount-input"
                            />
                          </FormControl>
                          <div className="hidden">
                            <FormMessage data-testid="form-message" />
                          </div>
                        </FormItem>
                      )}
                    />

                    {/* Max Amount */}
                    <FormField
                      control={form.control}
                      name="maxAmount"
                      render={({ field }) => (
                        <FormItem className="space-y-3">
                          <FormLabel className="text-base">
                            {t("maxAmount")}
                          </FormLabel>
                          <FormControl>
                            <NumberInputWithCommas
                              value={field.value?.toString() ?? ""}
                              onChange={(v) =>
                                field.onChange(v === "" ? null : parseFloat(v))
                              }
                              className="h-12"
                              step="0.01"
                              min="0"
                              data-testid="max-amount-input"
                            />
                          </FormControl>
                          <div className="hidden">
                            <FormMessage data-testid="form-message" />
                          </div>
                        </FormItem>
                      )}
                    />
                  </div>
                </div>

                <Separator className="my-8" />

                <div className="space-y-6">
                  <h3 className="text-xl font-semibold flex items-center gap-2">
                    {t("paymentSettings")}
                  </h3>

                  {/* Payment Methods */}
                  <FormField
                    control={form.control}
                    name="paymentMethodId"
                    render={({ field, fieldState }) => (
                      <FormItem className="space-y-3">
                        <FormLabel className="text-base">
                          {t("paymentMethod")}
                        </FormLabel>
                        <Select
                          value={field.value ? String(field.value) : ""}
                          onValueChange={(value) => {
                            field.onChange(value);
                            handlePaymentMethodChange(value);
                          }}
                          data-testid="payment-method-select"
                        >
                          <FormControl>
                            <SelectTrigger className="h-12">
                              <SelectValue
                                placeholder={t("selectPaymentMethod")}
                              />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {paymentMethods.map((method) => (
                              <SelectItem
                                key={method.id}
                                value={String(method.id)}
                              >
                                <span className="font-medium">
                                  {method.name}
                                </span>
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        {fieldState.error && (
                          <ErrorMessage message={fieldState.error.message} />
                        )}
                        <div className="hidden">
                          <FormMessage data-testid="form-message" />
                        </div>
                      </FormItem>
                    )}
                  />

                  {/* Bank Account Selector for Sell Offers */}
                  {isWithdrawOffer && (
                    <div className="space-y-3">
                      <FormLabel className="block text-base">
                        {t("bankAccount")}
                      </FormLabel>
                      <BankAccountSelector
                        control={form.control}
                        name="bankAccountId"
                        onAccountSelect={handleBankAccountChange}
                        data-testid="bank-account-selector"
                      />
                      {form.formState.isSubmitted &&
                        form.formState.errors.bankAccountId && (
                          <ErrorMessage
                            message={
                              form.formState.errors.bankAccountId
                                .message as string
                            }
                          />
                        )}
                    </div>
                  )}

                  {/* Payment Instructions for Buy Offers */}
                  {isDepositOffer && (
                    <FormField
                      control={form.control}
                      name="paymentDetails"
                      render={({ field }) => (
                        <FormItem className="space-y-3">
                          <FormLabel className="text-base">
                            {t("paymentInstructions")}
                          </FormLabel>
                          <FormControl>
                            <Input className="h-12" {...field} />
                          </FormControl>
                          <FormDescription>
                            {t("paymentInstructionsDescription")}
                          </FormDescription>
                          <FormMessage data-testid="form-message" />
                        </FormItem>
                      )}
                    />
                  )}
                </div>

                {/* Display form errors */}
                <FormErrors
                  errors={form.formState.errors}
                  isSubmitted={form.formState.isSubmitted}
                />

                {/* Submit Button */}
                <Button
                  type="submit"
                  className="w-full h-12 text-base font-medium"
                  disabled={isSubmitting || isLoading}
                  data-testid="submit-offer-button"
                >
                  {isSubmitting ? (
                    <div className="flex items-center justify-center gap-2">
                      <div className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></div>
                      <span>{isEditMode ? t("updating") : t("creating")}</span>
                    </div>
                  ) : isEditMode ? (
                    t("updateOffer")
                  ) : (
                    t("createOffer")
                  )}
                </Button>
              </form>
            </Form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
