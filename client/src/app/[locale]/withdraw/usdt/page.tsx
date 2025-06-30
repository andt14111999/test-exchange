"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { createWithdrawal, checkReceiver } from "@/lib/api/withdrawals";
import { getWithdrawalFees } from "@/lib/api/settings";
import { formatNumber } from "@/lib/utils/index";
import { useCoinNetworks } from "@/hooks/use-coin-networks";
import { useWallet } from "@/hooks/use-wallet";
import { useUserStore } from "@/lib/store/user-store";
import { TwoFactorAuthInput } from "@/components/two-factor-auth-input";
import { useTranslations } from "next-intl";
import { useDeviceTrust } from "@/hooks/use-device-trust";

// Import new components
import { WithdrawTabs } from "./components/withdraw-tabs";
import { ConfirmationDialog } from "./components/confirmation-dialog";
import {
  Network,
  CreateWithdrawalRequest,
  addressPatterns,
} from "./components/types";

export default function WithdrawUSDTPage() {
  const router = useRouter();
  const { user } = useUserStore();
  const t = useTranslations("withdraw.usdt");
  const tWithdraw = useTranslations("withdraw");

  // Load wallet data to get USDT balance
  const { data: walletData, isLoading: isLoadingWallet } = useWallet();

  // Get USDT balance from wallet data
  const usdtBalance =
    walletData?.coin_accounts.find(
      (account) => account.coin_currency.toLowerCase() === "usdt",
    )?.balance || 0;

  const [withdrawalType, setWithdrawalType] = useState<"external" | "internal">(
    "external",
  );

  // Get networks from API
  const { networks: rawNetworks, isLoading: isLoadingNetworks } =
    useCoinNetworks("usdt");
  const [selectedNetwork, setSelectedNetwork] = useState<Network | null>(null);
  const [networkFees, setNetworkFees] = useState<Record<string, number>>({});

  // Transform raw networks to include fees - make this reactive to networkFees
  const networks: Network[] = useMemo(() => {
    return rawNetworks.map((network) => ({
      ...network,
      fee: networkFees[`usdt_${network.id}`] || 0,
    }));
  }, [rawNetworks, networkFees]);

  // Initialize when both networks and fees are available
  useEffect(() => {
    // Wait for both networks and fees to be available
    if (rawNetworks.length > 0 && Object.keys(networkFees).length > 0 && !selectedNetwork) {
      // First try to find ERC20 (Ethereum)
      let initialNetwork = rawNetworks.find((n) => n.id === "erc20" && n.enabled);
      
      // If ERC20 not available, fall back to first enabled
      if (!initialNetwork) {
        initialNetwork = rawNetworks.find((n) => n.enabled);
      }
      
      if (initialNetwork) {
        const feeKey = `usdt_${initialNetwork.id}`;
        const networkFee = networkFees[feeKey] || 0;
        
        setSelectedNetwork({
          ...initialNetwork,
          fee: networkFee,
        });
      }
    }
  }, [rawNetworks, networkFees, selectedNetwork]);

  // Load withdrawal fees
  useEffect(() => {
    if (rawNetworks.length > 0 && Object.keys(networkFees).length === 0) {
      const loadFees = async () => {
        try {
          const fees = await getWithdrawalFees();
          setNetworkFees(fees);
        } catch (error) {
          console.error("Error fetching withdrawal fees:", error);
          toast.error("Failed to load withdrawal fees. Using default values.");
        }
      };
      
      loadFees();
    }
  }, [rawNetworks, networkFees]);

  const [amount, setAmount] = useState("");
  const [address, setAddress] = useState("");
  const [username, setUsername] = useState("");
  const [addressError, setAddressError] = useState<string | null>(null);
  const [usernameError, setUsernameError] = useState<string | null>(null);
  const [amountError, setAmountError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isValidatingUsername, setIsValidatingUsername] = useState(false);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);

  // 2FA states
  const [show2FADialog, setShow2FADialog] = useState(false);
  const [twoFactorError, setTwoFactorError] = useState<string | null>(null);
  const [pendingWithdrawalData, setPendingWithdrawalData] =
    useState<CreateWithdrawalRequest | null>(null);

  // Device trust state using custom hook
  const { isDeviceTrusted, isCheckingDevice } = useDeviceTrust();

  const validateAmount = useCallback(
    (value: string) => {
      const parsedAmount = parseFloat(value) || 0;

      if (!value || value === "") {
        setAmountError(null);
        return false;
      }

      if (parsedAmount <= 0) {
        setAmountError("Amount must be greater than 0");
        return false;
      }

      if (parsedAmount < 0.01) {
        setAmountError("Minimum amount is 0.01 USDT");
        return false;
      }

      if (parsedAmount > 100000) {
        setAmountError("Maximum amount is 100,000 USDT");
        return false;
      }

      // Check if amount exceeds available balance
      if (parsedAmount > usdtBalance) {
        setAmountError(
          `Insufficient balance. Available: ${formatNumber(usdtBalance)} USDT`,
        );
        return false;
      }

      setAmountError(null);
      return true;
    },
    [usdtBalance],
  );

  const validateAddress = useCallback(
    (value: string) => {
      if (!value) {
        setAddressError(null);
        return false;
      }

      if (!selectedNetwork) {
        setAddressError("Please select a network first");
        return false;
      }

      const pattern =
        addressPatterns[selectedNetwork.id as keyof typeof addressPatterns];
      if (!pattern.test(value)) {
        setAddressError(`Invalid ${selectedNetwork.name} address format`);
        return false;
      }

      setAddressError(null);
      return true;
    },
    [selectedNetwork],
  );

  const validateUsername = useCallback(async (value: string) => {
    if (!value) {
      setUsernameError(null);
      return false;
    }

    // Basic username validation: alphanumeric, underscore, hyphen, 3-30 characters
    const usernamePattern = /^[a-zA-Z0-9_-]{3,30}$/;
    if (!usernamePattern.test(value)) {
      setUsernameError(
        "Username must be 3-30 characters (letters, numbers, _, -)",
      );
      return false;
    }

    // Check if username exists via API
    try {
      setIsValidatingUsername(true);
      const isValid = await checkReceiver(value);

      if (!isValid) {
        setUsernameError("User is invalid");
        return false;
      }

      setUsernameError(null);
      return true;
    } catch (error) {
      console.error("Error validating username:", error);
      setUsernameError("Unable to validate username. Please try again.");
      return false;
    } finally {
      setIsValidatingUsername(false);
    }
  }, []);

  // Handle network change with address reset
  const handleNetworkChange = useCallback(
    (network: Network | null) => {
      if (network) {
        const feeKey = `usdt_${network.id}`;
        const networkFee = networkFees[feeKey] || 0;
        
        setSelectedNetwork({
          ...network,
          fee: networkFee,
        });
        setAddress("");
        setAddressError(null);
      }
    },
    [networkFees],
  );

  // Validate amount when it changes
  useEffect(() => {
    validateAmount(amount);
  }, [amount, validateAmount]);

  // Validate address when it changes or network changes
  useEffect(() => {
    if (withdrawalType === "external") {
      validateAddress(address);
    }
  }, [address, selectedNetwork, withdrawalType, validateAddress]);

  // Validate username for internal transfers with debounce
  useEffect(() => {
    if (withdrawalType === "internal" && username) {
      const debounceTimer = setTimeout(() => {
        validateUsername(username);
      }, 2000);

      return () => clearTimeout(debounceTimer);
    } else if (withdrawalType === "internal" && !username) {
      setUsernameError(null);
    }
  }, [username, withdrawalType, validateUsername]);

  // Create withdrawal data object
  const createWithdrawalData = useCallback((): CreateWithdrawalRequest => {
    const parsedAmount = parseFloat(amount) || 0;

    const params: CreateWithdrawalRequest = {
      coin_amount: parsedAmount,
      coin_currency: "USDT",
      coin_layer:
        withdrawalType === "external"
          ? selectedNetwork?.id || "trc20"
          : "internal",
    };

    if (withdrawalType === "external") {
      params.coin_address = address.trim();
    } else {
      params.receiver_username = username.trim();
    }

    return params;
  }, [amount, withdrawalType, selectedNetwork, address, username]);

  // Handle withdrawal submission
  const handleWithdrawSubmission = async (twoFactorCode?: string) => {
    const withdrawalData = createWithdrawalData();

    // If user has 2FA enabled and no code provided yet, include it in first attempt
    if (user?.authenticatorEnabled && twoFactorCode) {
      withdrawalData.two_factor_code = twoFactorCode;
    }

    setIsSubmitting(true);
    setAmountError(null);
    setTwoFactorError(null);

    try {
      const response = await createWithdrawal(withdrawalData);

      toast.success(
        withdrawalType === "external"
          ? t("withdrawalRequestSuccess")
          : t("internalTransferSuccess"),
      );

      // Close dialogs and redirect to withdrawal details page
      setShowConfirmDialog(false);
      setShow2FADialog(false);
      setPendingWithdrawalData(null);

      // Navigate to the withdrawal details page using the returned ID
      router.push(`/withdraw/usdt/${response.id}`);
    } catch (error: unknown) {
      console.error("Withdrawal failed:", error);

      // Check if 2FA is required
      const errorData = error as {
        response?: {
          status?: number;
          data?: {
            requires_2fa?: boolean;
            message?: string;
            status?: string;
          };
        };
      };

      if (
        errorData.response?.status === 400 &&
        errorData.response.data?.requires_2fa === true
      ) {
        setShowConfirmDialog(false);
        setPendingWithdrawalData(withdrawalData);
        setShow2FADialog(true);
        return;
      }

      // Check if 2FA code is invalid
      if (
        errorData.response?.status === 400 &&
        (errorData.response.data?.message === "Invalid 2FA code" ||
          errorData.response.data?.message?.includes("Invalid") ||
          errorData.response.data?.message?.includes("2FA"))
      ) {
        setTwoFactorError(tWithdraw("twoFactorInvalidCode"));
        return;
      }

      // Handle other errors
      const errorMessage =
        error &&
        typeof error === "object" &&
        "response" in error &&
        error.response &&
        typeof error.response === "object" &&
        "data" in error.response &&
        error.response.data &&
        typeof error.response.data === "object" &&
        "message" in error.response.data &&
        typeof error.response.data.message === "string"
          ? error.response.data.message
          : "Failed to submit withdrawal request";

      setAmountError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleWithdraw = async () => {
    const parsedAmount = parseFloat(amount) || 0;

    if (parsedAmount <= 0) {
      return;
    }

    // Check balance before proceeding
    if (parsedAmount > usdtBalance) {
      setAmountError(
        `Insufficient balance. Available: ${formatNumber(usdtBalance)} USDT`,
      );
      return;
    }

    if (withdrawalType === "external" && (!address || addressError)) {
      return;
    }

    if (
      withdrawalType === "internal" &&
      (!username || usernameError || isValidatingUsername)
    ) {
      return;
    }

    if (amountError) {
      return;
    }

    // If user has 2FA enabled AND device is not trusted, show 2FA dialog first
    if (user?.authenticatorEnabled && !isDeviceTrusted) {
      const withdrawalData = createWithdrawalData();
      setPendingWithdrawalData(withdrawalData);
      setShowConfirmDialog(false);
      setShow2FADialog(true);
    } else {
      // If no 2FA required or device is trusted, submit directly
      await handleWithdrawSubmission();
    }
  };

  // Handle 2FA code submission
  const handle2FASubmit = async (twoFactorCode: string) => {
    if (!pendingWithdrawalData) {
      setTwoFactorError(tWithdraw("twoFactorNoData"));
      return;
    }

    await handleWithdrawSubmission(twoFactorCode);
  };

  const parsedAmount = parseFloat(amount) || 0;
  const withdrawalFee =
    withdrawalType === "external"
      ? parseFloat(selectedNetwork?.fee.toString() || "0")
      : 0;
  const totalAmount = parsedAmount + withdrawalFee;

  const isFormValid = () => {
    if (parsedAmount <= 0 || amountError) return false;

    // Check balance for both external and internal withdrawals
    if (parsedAmount > usdtBalance) return false;

    if (withdrawalType === "external") {
      return address && !addressError;
    } else {
      return username && !usernameError && !isValidatingUsername;
    }
  };

  return (
    <div className="container py-6 max-w-2xl space-y-6 mx-auto">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold tracking-tight">{t("title")}</h1>
        <p className="text-muted-foreground">{t("description")}</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{t("withdrawalDetails")}</CardTitle>
          <CardDescription>{t("chooseTypeAndDestination")}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <WithdrawTabs
            withdrawalType={withdrawalType}
            onWithdrawalTypeChange={setWithdrawalType}
            networks={networks}
            selectedNetwork={selectedNetwork}
            onNetworkChange={handleNetworkChange}
            address={address}
            onAddressChange={setAddress}
            addressError={addressError}
            username={username}
            onUsernameChange={setUsername}
            usernameError={usernameError}
            isValidatingUsername={isValidatingUsername}
            amount={amount}
            onAmountChange={setAmount}
            amountError={amountError}
            usdtBalance={usdtBalance}
            isLoadingWallet={isLoadingWallet}
            isLoadingNetworks={isLoadingNetworks}
            userHas2FA={!!user?.authenticatorEnabled}
            isDeviceTrusted={isDeviceTrusted}
            isCheckingDevice={isCheckingDevice}
          />
        </CardContent>
        <CardFooter className="flex flex-col gap-3">
          {!user?.authenticatorEnabled ? (
            <>
              <Button
                onClick={() => setShowConfirmDialog(true)}
                disabled={true}
                className="w-full"
                variant="secondary"
              >
                {`${withdrawalType === "external" ? "Withdraw" : "Transfer"} ${parsedAmount > 0 ? formatNumber(totalAmount) : ""} USDT`}{" "}
                - 2FA Required
              </Button>
              <Button
                onClick={() => router.push("/profile")}
                className="w-full"
                variant="outline"
              >
                Enable 2FA to Withdraw
              </Button>
            </>
          ) : (
            <Button
              onClick={() => setShowConfirmDialog(true)}
              disabled={!isFormValid()}
              className="w-full"
            >
              {isSubmitting
                ? "Processing..."
                : `${withdrawalType === "external" ? "Withdraw" : "Transfer"} ${parsedAmount > 0 ? formatNumber(totalAmount) : ""} USDT`}
            </Button>
          )}
        </CardFooter>
      </Card>

      {/* Confirmation Dialog */}
      <ConfirmationDialog
        open={showConfirmDialog}
        onOpenChange={setShowConfirmDialog}
        withdrawalType={withdrawalType}
        amount={parsedAmount}
        address={address}
        username={username}
        selectedNetwork={selectedNetwork}
        withdrawalFee={withdrawalFee}
        totalAmount={totalAmount}
        isSubmitting={isSubmitting}
        onConfirm={handleWithdraw}
      />

      {/* 2FA Dialog */}
      <TwoFactorAuthInput
        open={show2FADialog}
        onOpenChange={(open) => {
          setShow2FADialog(open);
          if (!open) {
            setPendingWithdrawalData(null);
            setTwoFactorError(null);
          }
        }}
        onSubmit={handle2FASubmit}
        title={t("twoFactorDialog.title")}
        description={t("twoFactorDialog.description")}
        submitText={t("twoFactorDialog.submitText")}
        isLoading={isSubmitting}
        error={twoFactorError}
        onError={setTwoFactorError}
      />
    </div>
  );
}
