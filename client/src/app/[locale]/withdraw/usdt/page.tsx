"use client";

import { useState, useEffect, useCallback, useRef } from "react";
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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Label } from "@/components/ui/label";

import {
  createWithdrawal,
  CreateWithdrawalRequest,
  checkReceiver,
} from "@/lib/api/withdrawals";
import { getWithdrawalFees } from "@/lib/api/settings";
import { formatNumber } from "@/lib/utils/index";
import { useCoinNetworks } from "@/hooks/use-coin-networks";
import { useWallet } from "@/hooks/use-wallet";

// Define network type
interface Network {
  id: string;
  name: string;
  fee: number;
  enabled: boolean;
}

const addressPatterns = {
  // BEP20 addresses are Ethereum-like, starting with 0x followed by 40 hex characters
  bep20: /^0x[a-fA-F0-9]{40}$/,
  // TRC20 addresses start with T and are typically 34 characters long
  trc20: /^T[a-zA-Z0-9]{33}$/,
  // ERC20 uses the same pattern as BEP20
  erc20: /^0x[a-fA-F0-9]{40}$/,
  // Solana addresses are 32-44 characters, base58 encoded
  solana: /^[1-9A-HJ-NP-Za-km-z]{32,44}$/,
};

export default function WithdrawUSDTPage() {
  const router = useRouter();

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
  const { networks, isLoading: isLoadingNetworks } = useCoinNetworks("usdt");
  const [selectedNetwork, setSelectedNetwork] = useState<Network | null>(null);
  const [networkFees, setNetworkFees] = useState<Record<string, number>>({});
  const hasInitializedRef = useRef(false);

  // Fetch withdrawal fees and set initial network only once
  useEffect(() => {
    let isMounted = true;

    const initialize = async () => {
      // Only proceed if we have networks and haven't initialized yet
      if (!networks.length || hasInitializedRef.current) return;
      hasInitializedRef.current = true;

      try {
        const fees = await getWithdrawalFees();

        if (!isMounted) return;
        setNetworkFees(fees);

        // Set initial selected network if none is selected
        if (!selectedNetwork) {
          const firstEnabled = networks.find((n) => n.enabled);
          if (firstEnabled) {
            setSelectedNetwork({
              ...firstEnabled,
              fee: fees[`usdt_${firstEnabled.id}`] || 0,
            });
          }
        }
      } catch (error) {
        console.error("Error fetching withdrawal fees:", error);
        if (isMounted) {
          toast.error("Failed to load withdrawal fees. Using default values.");
          // Still set initial network even if fees fail
          if (!selectedNetwork) {
            const firstEnabled = networks.find((n) => n.enabled);
            if (firstEnabled) {
              setSelectedNetwork({
                ...firstEnabled,
                fee: 0,
              });
            }
          }
        }
      }
    };

    initialize();

    return () => {
      isMounted = false;
    };
  }, [networks, selectedNetwork]);

  const [amount, setAmount] = useState("");
  const [address, setAddress] = useState("");
  const [username, setUsername] = useState("");
  const [addressError, setAddressError] = useState<string | null>(null);
  const [usernameError, setUsernameError] = useState<string | null>(null);
  const [amountError, setAmountError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isValidatingUsername, setIsValidatingUsername] = useState(false);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);

  // Fetch coin settings
  // Remove unused coin settings query

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
      }, 2000); // 500ms debounce

      return () => clearTimeout(debounceTimer);
    } else if (withdrawalType === "internal" && !username) {
      setUsernameError(null);
    }
  }, [username, withdrawalType, validateUsername]);

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

    setIsSubmitting(true);
    setAmountError(null);

    try {
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
      const response = await createWithdrawal(params);

      toast.success(
        withdrawalType === "external"
          ? "Withdrawal request submitted successfully"
          : "Internal transfer submitted successfully",
      );

      // Close dialog and redirect to withdrawal details page
      setShowConfirmDialog(false);

      // Navigate to the withdrawal details page using the returned ID
      router.push(`/withdraw/usdt/${response.id}`);
    } catch (error: unknown) {
      console.error("Withdrawal failed:", error);
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

  const handleOpenConfirmDialog = () => {
    setShowConfirmDialog(true);
  };

  const handleCloseConfirmDialog = () => {
    setShowConfirmDialog(false);
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
        <h1 className="text-2xl font-bold tracking-tight">Withdraw USDT</h1>
        <p className="text-muted-foreground">
          Withdraw USDT to an external wallet or transfer to another user
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Withdrawal Details</CardTitle>
          <CardDescription>
            Choose withdrawal type and destination
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <Tabs
            defaultValue="external"
            value={withdrawalType}
            onValueChange={(value) =>
              setWithdrawalType(value as "external" | "internal")
            }
          >
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="external">On-chain Withdrawal</TabsTrigger>
              <TabsTrigger value="internal">Internal Transfer</TabsTrigger>
            </TabsList>
            <div className="mt-6 space-y-6">
              <TabsContent value="external" className="space-y-6 mt-0">
                <div className="space-y-2">
                  <Label htmlFor="network" className="text-sm font-medium">
                    Network
                  </Label>
                  <Select
                    value={selectedNetwork?.id}
                    onValueChange={(value) => {
                      const network = networks.find((n) => n.id === value);
                      if (network && network.enabled) {
                        setSelectedNetwork({
                          ...network,
                          fee: networkFees[`usdt_${value}`] || 0,
                        });
                        setAddress(""); // Reset address when network changes
                        setAddressError(null);
                      }
                    }}
                    disabled={isLoadingNetworks}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select a network" />
                    </SelectTrigger>
                    <SelectContent>
                      {networks.map((network) => (
                        <SelectItem
                          key={network.id}
                          value={network.id}
                          disabled={!network.enabled}
                          className={
                            !network.enabled ? "text-muted-foreground" : ""
                          }
                        >
                          {network.name}
                          {!network.enabled && (
                            <>
                              {" "}
                              <span className="text-muted-foreground">
                                (Network disabled)
                              </span>
                            </>
                          )}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="address" className="text-sm font-medium">
                    Destination Address
                  </Label>
                  <Input
                    placeholder={
                      selectedNetwork
                        ? `Enter ${selectedNetwork.name} address`
                        : "Please select a network first"
                    }
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    className={addressError ? "border-red-500" : ""}
                  />
                  <div className="text-xs text-muted-foreground">
                    Example:
                    {selectedNetwork?.id === "bep20" &&
                      " 0x71C7656EC7ab88b098defB751B7401B5f6d8976F"}
                    {selectedNetwork?.id === "trc20" &&
                      " TKQpQkMWRvTJpQgYrGp8wKgJSHV3DqNHJ3"}
                    {selectedNetwork?.id === "erc20" &&
                      " 0x71C7656EC7ab88b098defB751B7401B5f6d8976F"}
                    {selectedNetwork?.id === "solana" &&
                      " 7UX2i7SucgLMQcfZ75s3VXmZZY4YRUyJN9X1RgfMoDUi"}
                  </div>
                  {addressError && (
                    <div className="text-sm text-red-600">{addressError}</div>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="amount" className="text-sm font-medium">
                    Amount (USDT)
                  </Label>
                  <Input
                    id="amount"
                    type="number"
                    placeholder="Enter amount"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className={amountError ? "border-red-500" : ""}
                  />
                  <div className="text-sm text-muted-foreground">
                    Available balance:{" "}
                    {isLoadingWallet
                      ? "Loading..."
                      : `${formatNumber(usdtBalance)} USDT`}
                  </div>
                  {parsedAmount > 0 && (
                    <div className="text-sm">
                      Total amount:{" "}
                      {formatNumber(parsedAmount + (selectedNetwork?.fee || 0))}{" "}
                      USDT
                    </div>
                  )}
                  {amountError && (
                    <div className="text-sm text-red-600">{amountError}</div>
                  )}
                </div>

                <div className="text-sm text-muted-foreground">
                  <p>Important:</p>
                  <ul className="list-disc list-inside space-y-1">
                    <li>Minimum withdrawal: 0.01 USDT</li>
                    <li>Network fee: {selectedNetwork?.fee || 0} USDT</li>
                    <li>
                      Only withdraw to addresses on the {selectedNetwork?.name}{" "}
                      network
                    </li>
                  </ul>
                </div>
              </TabsContent>

              <TabsContent value="internal" className="space-y-6 mt-0">
                <div className="space-y-2">
                  <Label htmlFor="username" className="text-sm font-medium">
                    Recipient Username
                  </Label>
                  <Input
                    id="username"
                    placeholder="Enter username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className={usernameError ? "border-red-500" : ""}
                  />
                  <div className="text-xs text-muted-foreground">
                    Example: johndoe123
                  </div>
                  {usernameError && (
                    <div className="text-sm text-red-600">{usernameError}</div>
                  )}
                  {isValidatingUsername && (
                    <div className="text-sm text-blue-600">
                      Validating username...
                    </div>
                  )}
                </div>

                <div className="space-y-2">
                  <Label
                    htmlFor="amount-internal"
                    className="text-sm font-medium"
                  >
                    Amount (USDT)
                  </Label>
                  <Input
                    id="amount-internal"
                    type="number"
                    placeholder="Enter amount"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className={amountError ? "border-red-500" : ""}
                  />
                  <div className="text-sm text-muted-foreground">
                    Available balance:{" "}
                    {isLoadingWallet
                      ? "Loading..."
                      : `${formatNumber(usdtBalance)} USDT`}
                  </div>
                  {parsedAmount > 0 && (
                    <div className="text-sm">
                      Total amount: {formatNumber(parsedAmount)} USDT
                    </div>
                  )}
                  {amountError && (
                    <div className="text-sm text-red-600">{amountError}</div>
                  )}
                </div>

                <div className="text-sm text-muted-foreground">
                  <p>Important:</p>
                  <ul className="list-disc list-inside space-y-1">
                    <li>Minimum transfer: 0.01 USDT</li>
                    <li>No network fees for internal transfers</li>
                    <li>Transfers are instant between platform users</li>
                  </ul>
                </div>
              </TabsContent>
            </div>
          </Tabs>
        </CardContent>
        <CardFooter>
          <Button
            onClick={handleOpenConfirmDialog}
            disabled={!isFormValid()}
            className="w-full"
          >
            {isSubmitting
              ? "Processing..."
              : `${withdrawalType === "external" ? "Withdraw" : "Transfer"} ${parsedAmount > 0 ? formatNumber(totalAmount) : ""} USDT`}
          </Button>
        </CardFooter>
      </Card>

      <Dialog open={showConfirmDialog} onOpenChange={setShowConfirmDialog}>
        <DialogContent className="sm:max-w-[525px]">
          <DialogHeader>
            <DialogTitle>
              {withdrawalType === "external"
                ? "Confirm Withdrawal"
                : "Confirm Internal Transfer"}
            </DialogTitle>
          </DialogHeader>
          <div className="py-4 space-y-3">
            <div className="flex flex-col items-center justify-center mb-4">
              <span className="text-xs text-muted-foreground mb-1">
                {withdrawalType === "external"
                  ? "Withdraw Amount"
                  : "Transfer Amount"}
              </span>
              <span className="text-3xl font-bold">
                {formatNumber(parsedAmount)} USDT
              </span>
              <span className="text-sm text-muted-foreground">
                ~ ${formatNumber(parsedAmount)}
              </span>
            </div>
            <Separator />
            <DialogDescription className="text-sm font-medium mb-2">
              Transaction Details
            </DialogDescription>
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">
                {withdrawalType === "external" ? "Address" : "Recipient"}
              </span>
              <span
                className="font-medium text-sm text-right"
                title={withdrawalType === "external" ? address : username}
              >
                {withdrawalType === "external" ? address : `@${username}`}
              </span>
            </div>
            {withdrawalType === "external" && (
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">Network</span>
                <span className="font-medium text-sm">
                  {selectedNetwork?.name}
                </span>
              </div>
            )}
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">Fee</span>
              <span className="font-medium text-sm bg-muted px-2 py-0.5 rounded">
                {formatNumber(withdrawalFee)} USDT
              </span>
            </div>
            <Separator />
            <div className="flex justify-between items-center pt-2">
              <span className="text-sm text-muted-foreground">Total</span>
              <span className="font-bold text-lg">
                {formatNumber(totalAmount)} USDT
              </span>
            </div>
          </div>
          <DialogFooter className="gap-2 sm:justify-between">
            <Button
              variant="outline"
              onClick={handleCloseConfirmDialog}
              className="w-full sm:w-auto"
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              onClick={handleWithdraw}
              className="w-full sm:w-auto text-white"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Processing..." : "Confirm"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
