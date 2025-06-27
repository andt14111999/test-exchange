"use client";

import { Alert, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/components/ui/use-toast";
import { useCoinAddress } from "@/hooks/use-coin-address";
import type { Network } from "@/hooks/use-coin-networks";
import type { CoinSetting } from "@/lib/api/coins";
import { fetchCoinSettings } from "@/lib/api/coins";
import { isEthereumLikeNetwork, toChecksumAddress } from "@/lib/utils/ethereum";
import { useQuery } from "@tanstack/react-query";
import { AlertCircle, CopyIcon } from "lucide-react";
import { useTranslations } from "next-intl";
import { useCallback, useEffect, useMemo, useState } from "react";
import QRCode from "react-qr-code";

interface CoinDepositProps {
  coin: string;
  networks: Network[];
  title: string;
  description: string;
}

const LoadingSkeleton = () => (
  <div className="space-y-4">
    <Skeleton className="h-8 w-full" role="status" aria-label="Loading..." />
    <Skeleton className="h-32 w-full" role="status" aria-label="Loading..." />
  </div>
);

const ErrorState = ({
  t,
  onRetry,
}: {
  t: ReturnType<typeof useTranslations>;
  onRetry: () => void;
}) => (
  <div className="text-center space-y-4">
    <p className="text-destructive">{t("error.message")}</p>
    <Button onClick={onRetry}>{t("error.retry")}</Button>
  </div>
);

const NetworkSelect = ({
  networks,
  selectedNetwork,
  onNetworkChange,
  disabled,
}: {
  networks: Network[];
  selectedNetwork: Network;
  onNetworkChange: (value: string) => void;
  disabled?: boolean;
}) => {
  return (
    <Select
      value={selectedNetwork.id}
      onValueChange={(value) => {
        const network = networks.find((n) => n.id === value);
        if (network && network.enabled) {
          onNetworkChange(value);
        }
      }}
      disabled={disabled}
    >
      <SelectTrigger>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {networks.map((network) => (
          <SelectItem
            key={network.id}
            value={network.id}
            disabled={!network.enabled}
            className={!network.enabled ? "text-muted-foreground" : ""}
          >
            {network.name}
            {!network.enabled && (
              <>
                {" "}
                <span className="text-muted-foreground">(Mạng đã tắt)</span>
              </>
            )}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
};

const DepositAddress = ({
  address,
  t,
  onCopy,
}: {
  address: string;
  t: ReturnType<typeof useTranslations>;
  onCopy: () => void;
}) => (
  <div className="space-y-2">
    <div className="text-sm font-medium">{t("depositAddress")}</div>
    <div className="flex items-center gap-2">
      <code className="flex-1 p-2 bg-muted rounded-md text-sm break-all">
        {address}
      </code>
      <Button
        variant="outline"
        size="icon"
        onClick={onCopy}
        className="shrink-0"
        aria-label="Copy address"
      >
        <CopyIcon className="h-4 w-4" />
      </Button>
    </div>
  </div>
);

const ImportantNotes = ({
  t,
  networkName,
  coin,
}: {
  t: ReturnType<typeof useTranslations>;
  networkName: string;
  coin: string;
}) => (
  <div className="text-sm text-muted-foreground">
    <p>{t("important.title")}</p>
    <ul className="list-disc list-inside space-y-1">
      <li>{t("important.network", { network: networkName })}</li>
      <li>{t("important.minimum", { coin })}</li>
      <li>{t("important.confirmation")}</li>
    </ul>
  </div>
);

const GenerateAddress = ({
  t,
  onGenerate,
  isLoading,
  disabled,
  error,
}: {
  t: ReturnType<typeof useTranslations>;
  onGenerate: () => void;
  isLoading: boolean;
  disabled?: boolean;
  error?: string;
}) => (
  <div className="space-y-4 text-center">
    <p className="text-muted-foreground">{t("generateAddress.message")}</p>

    {error && (
      <div className="flex items-center justify-center w-full my-2">
        <Alert
          variant="destructive"
          className="flex items-center justify-center gap-2 max-w-[400px]"
        >
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          <AlertTitle>{error}</AlertTitle>
        </Alert>
      </div>
    )}

    <Button
      onClick={onGenerate}
      disabled={disabled || isLoading}
      className="min-w-32"
    >
      {isLoading
        ? t("generateAddress.generating")
        : t("generateAddress.button")}
    </Button>
  </div>
);

export function CoinDeposit({
  coin,
  networks,
  title,
  description,
}: CoinDepositProps) {
  const t = useTranslations("deposit");
  const { toast } = useToast();
  const [selectedNetwork, setSelectedNetwork] = useState(() => {
    // Find first enabled network
    const firstEnabled = networks.find((network) => network.enabled);
    if (firstEnabled) {
      return firstEnabled;
    }
    // If no enabled networks found, return the first network with a warning
    if (networks.length > 0) {
      return networks[0];
    }
    // Fallback to a default network if no networks provided
    return {
      id: "",
      name: "",
      enabled: false,
    };
  });
  const [displayAddress, setDisplayAddress] = useState<string>("");

  // Fetch coin settings
  const { data: coinSettings, isLoading: isLoadingSettings } = useQuery<
    CoinSetting[]
  >({
    queryKey: ["coin-settings"],
    queryFn: fetchCoinSettings,
  });

  const {
    data,
    isLoading,
    error,
    generateAddress,
    isGenerating,
    generateError,
  } = useCoinAddress({
    coinCurrency: coin,
    layer: selectedNetwork.id,
  });

  // Check if deposit is enabled for the current coin and network
  const isDepositEnabled = useMemo(() => {
    if (!coinSettings) return true; // Default to enabled if settings not loaded

    const setting = coinSettings.find(
      (s) => s.currency.toLowerCase() === coin.toLowerCase(),
    );

    if (!setting) return true; // Default to enabled if coin not found in settings

    // Check coin-level settings
    if (!setting.deposit_enabled) return false;

    // Check network-level settings if layers exist
    const layer = setting.layers.find(
      (l) => l.layer.toLowerCase() === selectedNetwork.id.toLowerCase(),
    );

    if (layer) {
      return layer.deposit_enabled;
    }

    return true; // Default to enabled if no layer settings found
  }, [coinSettings, coin, selectedNetwork.id]);

  const handleNetworkChange = useCallback(
    (value: string) => {
      const network = networks.find((n) => n.id === value);
      if (network && network.enabled) {
        setSelectedNetwork(network);
      }
    },
    [networks],
  );

  const handleCopy = useCallback(async () => {
    if (!displayAddress) return;
    try {
      await navigator.clipboard.writeText(displayAddress);
      toast({
        title: t("addressCopied"),
        description: t("addressCopiedDescription"),
      });
    } catch (err) {
      console.error("Failed to copy address:", err);
      toast({
        title: t("error.copyFailed"),
        description: t("error.copyFailedDescription"),
        variant: "destructive",
      });
    }
  }, [displayAddress, t, toast]);

  const handleRetry = useCallback(() => {
    window.location.reload();
  }, []);

  const handleGenerateAddress = useCallback(() => {
    if (!isDepositEnabled) return;
    if (!selectedNetwork.id) return;
    generateAddress();
  }, [generateAddress, isDepositEnabled, selectedNetwork.id]);

  // Get error message for address generation
  const getGenerateErrorMessage = useCallback(() => {
    if (!generateError) return undefined;

    // Handle API error responses (e.g., from Axios)
    if (typeof generateError === "object" && generateError !== null) {
      const apiError = generateError as {
        response?: {
          data?:
            | {
                message?: string;
              }
            | string;
        };
        message?: string;
      };

      // Prioritize message from API response body
      if (
        typeof apiError.response?.data === "object" &&
        apiError.response.data.message
      ) {
        return apiError.response.data.message;
      }

      // Handle case where response.data is just a string
      if (
        typeof apiError.response?.data === "string" &&
        apiError.response.data
      ) {
        return apiError.response.data;
      }
    }

    // Fallback to a generic message if a specific one isn't found
    return t("error.generateAddressFailed");
  }, [generateError, t]);

  const depositAddress = useMemo(
    () => data?.data.address || "",
    [data?.data.address],
  );

  // Apply checksum formatting for Ethereum-like networks
  useEffect(() => {
    const formatAddress = async () => {
      if (!depositAddress) {
        setDisplayAddress("");
        return;
      }

      if (isEthereumLikeNetwork(selectedNetwork.id)) {
        try {
          const checksummedAddress = await toChecksumAddress(depositAddress);
          setDisplayAddress(checksummedAddress);
        } catch (error) {
          console.warn("Failed to checksum address:", error);
          setDisplayAddress(depositAddress);
        }
      } else {
        setDisplayAddress(depositAddress);
      }
    };

    formatAddress();
  }, [depositAddress, selectedNetwork.id]);

  if (isLoading || isLoadingSettings) {
    return <LoadingSkeleton />;
  }

  if (error) {
    return <ErrorState t={t} onRetry={handleRetry} />;
  }

  return (
    <div className="container py-6 max-w-2xl space-y-6 mx-auto">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        <p className="text-muted-foreground">{description}</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{t("networkSelection.title")}</CardTitle>
          <CardDescription>{t("networkSelection.description")}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <NetworkSelect
            networks={networks}
            selectedNetwork={selectedNetwork}
            onNetworkChange={handleNetworkChange}
            disabled={isLoading || isGenerating}
          />

          <div className="space-y-4">
            {displayAddress ? (
              <>
                <div className="flex justify-center p-4 bg-white rounded-lg">
                  <QRCode
                    value={displayAddress}
                    role="img"
                    aria-label="QR code for deposit address"
                  />
                </div>

                <DepositAddress
                  address={displayAddress}
                  t={t}
                  onCopy={handleCopy}
                />
              </>
            ) : depositAddress ? (
              // Show loading state while formatting address
              <div className="space-y-2">
                <div className="text-sm font-medium">{t("depositAddress")}</div>
                <div className="flex items-center gap-2">
                  <code className="flex-1 p-2 bg-muted rounded-md text-sm break-all text-muted-foreground">
                    Formatting address...
                  </code>
                </div>
              </div>
            ) : (
              <GenerateAddress
                t={t}
                onGenerate={handleGenerateAddress}
                isLoading={isGenerating}
                disabled={!isDepositEnabled}
                error={getGenerateErrorMessage()}
              />
            )}

            {!isDepositEnabled && (
              <div className="text-sm text-destructive text-center">
                {t("depositDisabled")}
              </div>
            )}

            <ImportantNotes
              t={t}
              networkName={selectedNetwork.name}
              coin={coin}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
