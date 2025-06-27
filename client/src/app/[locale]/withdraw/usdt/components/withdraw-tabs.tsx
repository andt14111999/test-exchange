"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ExternalWithdrawTab } from "./external-withdraw-tab";
import { InternalWithdrawTab } from "./internal-withdraw-tab";
import { useTranslations } from "next-intl";
import { Network } from "./types";

interface WithdrawTabsProps {
  withdrawalType: "external" | "internal";
  onWithdrawalTypeChange: (type: "external" | "internal") => void;

  // External withdraw props
  networks: Network[];
  selectedNetwork: Network | null;
  onNetworkChange: (network: Network | null) => void;
  address: string;
  onAddressChange: (address: string) => void;
  addressError: string | null;

  // Internal withdraw props
  username: string;
  onUsernameChange: (username: string) => void;
  usernameError: string | null;
  isValidatingUsername: boolean;

  // Common props
  amount: string;
  onAmountChange: (amount: string) => void;
  amountError: string | null;
  usdtBalance: number;
  isLoadingWallet: boolean;
  isLoadingNetworks: boolean;
  userHas2FA: boolean;
  isDeviceTrusted: boolean;
  isCheckingDevice: boolean;
}

export function WithdrawTabs({
  withdrawalType,
  onWithdrawalTypeChange,
  networks,
  selectedNetwork,
  onNetworkChange,
  address,
  onAddressChange,
  addressError,
  username,
  onUsernameChange,
  usernameError,
  isValidatingUsername,
  amount,
  onAmountChange,
  amountError,
  usdtBalance,
  isLoadingWallet,
  isLoadingNetworks,
  userHas2FA,
  isDeviceTrusted,
  isCheckingDevice,
}: WithdrawTabsProps) {
  const t = useTranslations("withdraw.usdt");

  return (
    <Tabs
      defaultValue="external"
      value={withdrawalType}
      onValueChange={(value) =>
        onWithdrawalTypeChange(value as "external" | "internal")
      }
    >
      <TabsList className="grid w-full grid-cols-2">
        <TabsTrigger value="external">{t("onChainWithdrawal")}</TabsTrigger>
        <TabsTrigger value="internal">{t("internalTransfer")}</TabsTrigger>
      </TabsList>

      <div className="mt-6 space-y-6">
        <TabsContent value="external" className="space-y-6 mt-0">
          <ExternalWithdrawTab
            networks={networks}
            selectedNetwork={selectedNetwork}
            onNetworkChange={onNetworkChange}
            address={address}
            onAddressChange={onAddressChange}
            addressError={addressError}
            amount={amount}
            onAmountChange={onAmountChange}
            amountError={amountError}
            usdtBalance={usdtBalance}
            isLoadingWallet={isLoadingWallet}
            isLoadingNetworks={isLoadingNetworks}
            userHas2FA={userHas2FA}
            isDeviceTrusted={isDeviceTrusted}
            isCheckingDevice={isCheckingDevice}
          />
        </TabsContent>

        <TabsContent value="internal" className="space-y-6 mt-0">
          <InternalWithdrawTab
            username={username}
            onUsernameChange={onUsernameChange}
            usernameError={usernameError}
            isValidatingUsername={isValidatingUsername}
            amount={amount}
            onAmountChange={onAmountChange}
            amountError={amountError}
            usdtBalance={usdtBalance}
            isLoadingWallet={isLoadingWallet}
            userHas2FA={userHas2FA}
            isDeviceTrusted={isDeviceTrusted}
            isCheckingDevice={isCheckingDevice}
          />
        </TabsContent>
      </div>
    </Tabs>
  );
}
