"use client";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { formatNumber } from "@/lib/utils/index";
import { Copy, ExternalLink, ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { getWithdrawalById, WithdrawalResponse } from "@/lib/api/withdrawals";

// Helper function to format date/time
const formatDateTime = (isoString: string) => {
  try {
    const date = new Date(isoString);
    const datePart = date.toLocaleDateString("en-GB", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
    const timePart = date.toLocaleTimeString("en-GB", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    });
    return `${datePart} - ${timePart}`;
  } catch (error) {
    console.error("Error formatting date:", error);
    return "Invalid Date";
  }
};

// Helper to truncate strings
const truncateMiddle = (str: string, start = 6, end = 6) => {
  if (!str || str.length <= start + end) return str;
  return `${str.substring(0, start)}...${str.substring(str.length - end)}`;
};

// Map API status to display status
const mapStatus = (
  status: string,
):
  | "completed"
  | "pending"
  | "failed"
  | "cancelled"
  | "processing"
  | "verified" => {
  const statusMap: Record<
    string,
    "completed" | "pending" | "failed" | "cancelled" | "processing" | "verified"
  > = {
    COMPLETED: "completed",
    PENDING: "pending",
    FAILED: "failed",
    CANCELLED: "cancelled",
    PROCESSING: "processing",
    VERIFIED: "verified",
  };
  return statusMap[status.toUpperCase()] || "pending";
};

export default function WithdrawalDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const [details, setDetails] = useState<WithdrawalResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchWithdrawalDetails = async () => {
      setLoading(true);
      setError(null);

      try {
        const data = await getWithdrawalById(id);
        setDetails(data);
      } catch (err) {
        console.error("Error fetching withdrawal details:", err);
        setError("Failed to load withdrawal details.");
      } finally {
        setLoading(false);
      }
    };

    fetchWithdrawalDetails();
  }, [id]);

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    toast.success(`${label} copied to clipboard`);
  };

  if (loading) {
    return (
      <div className="container py-6 max-w-2xl space-y-6 mx-auto text-center">
        <p>Loading withdrawal details...</p>
      </div>
    );
  }

  if (error || !details) {
    return (
      <div className="container py-6 max-w-2xl space-y-6 mx-auto text-center">
        <Link href="/withdraw/usdt">
          <Button variant="ghost" className="absolute top-4 left-4">
            <ArrowLeft className="h-4 w-4 mr-2" /> Back to Withdraw
          </Button>
        </Link>
        <p className="mt-16">{error || "Withdrawal details not found."}</p>
      </div>
    );
  }

  const displayStatus = mapStatus(details.status);
  const isInternalTransfer = details.is_internal_transfer;
  const networkName =
    details.network_name || `${details.coin_layer?.toUpperCase()} Network`;

  const getExplorerUrl = () => {
    // No explorer URL for internal transfers
    if (isInternalTransfer) return null;

    // Different explorers for different networks
    const hash = details.tx_hash;
    if (!hash) return null;

    if (details.coin_layer === "bep20") {
      return `https://bscscan.com/tx/${hash}`;
    } else if (details.coin_layer === "trc20") {
      return `https://tronscan.org/#/transaction/${hash}`;
    } else if (details.coin_layer === "erc20") {
      return `https://etherscan.io/tx/${hash}`;
    } else if (details.coin_layer === "solana") {
      return `https://solscan.io/tx/${hash}`;
    }
    return null;
  };

  const explorerUrl = getExplorerUrl();

  return (
    <div className="container py-6 max-w-2xl space-y-6 mx-auto">
      <div className="flex items-center mb-4">
        <Link href="/withdraw/usdt">
          <Button variant="ghost" size="icon" className="mr-2">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <h1 className="text-xl font-semibold">Withdrawal Details</h1>
      </div>

      <Card>
        <CardHeader className="items-center text-center pb-4">
          <Badge variant="outline" className="mb-2 text-sm">
            {isInternalTransfer ? "Internal Transfer" : "Withdraw"}{" "}
            {details.coin_currency.toUpperCase()}
          </Badge>
          <CardTitle className="text-3xl">
            {formatNumber(details.coin_amount)}{" "}
            {details.coin_currency.toUpperCase()}
          </CardTitle>
          <CardDescription>
            ~ ${formatNumber(details.coin_amount)}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex justify-between items-center">
            <span className="text-sm text-muted-foreground">Status</span>
            <Badge
              variant={"secondary"}
              className={`border-transparent hover:bg-secondary/80 ${
                displayStatus === "completed" || displayStatus === "verified"
                  ? "bg-green-100 text-green-800"
                  : displayStatus === "failed"
                    ? "bg-red-100 text-red-800"
                    : displayStatus === "processing"
                      ? "bg-yellow-50 text-yellow-700"
                      : displayStatus === "cancelled"
                        ? "bg-gray-100 text-gray-800"
                        : "bg-yellow-100 text-yellow-800"
              }`}
            >
              {displayStatus}
            </Badge>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-sm text-muted-foreground">Time</span>
            <span className="text-sm font-medium">
              {formatDateTime(details.created_at)}
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-sm text-muted-foreground">Type</span>
            <span className="text-sm font-medium">
              {isInternalTransfer ? "Internal transfer" : "External transfer"}
            </span>
          </div>
          <hr className="my-4" />
          {!isInternalTransfer && (
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">Network</span>
              <span className="text-sm font-medium">{networkName}</span>
            </div>
          )}
          <div className="flex justify-between items-center">
            <span className="text-sm text-muted-foreground">
              {isInternalTransfer ? "Recipient" : "To address"}
            </span>
            <div className="flex items-center gap-1">
              <span
                className="text-sm font-mono font-medium"
                title={
                  isInternalTransfer
                    ? details.receiver_username
                    : details.coin_address
                }
              >
                {isInternalTransfer
                  ? `@${details.receiver_username}`
                  : truncateMiddle(details.coin_address)}
              </span>
              <Button
                variant="ghost"
                size="icon"
                className="h-5 w-5"
                onClick={() =>
                  handleCopy(
                    isInternalTransfer
                      ? details.receiver_username!
                      : details.coin_address!,
                    isInternalTransfer ? "Username" : "Address",
                  )
                }
              >
                <Copy className="h-3 w-3" />
              </Button>
            </div>
          </div>

          {details.tx_hash && !isInternalTransfer && (
            <div className="flex justify-between items-center">
              <span className="text-sm text-muted-foreground">Hash</span>
              <div className="flex items-center gap-1">
                <span
                  className="text-sm font-mono font-medium"
                  title={details.tx_hash}
                >
                  {truncateMiddle(details.tx_hash)}
                </span>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-5 w-5"
                  onClick={() =>
                    handleCopy(details.tx_hash!, "Transaction hash")
                  }
                >
                  <Copy className="h-3 w-3" />
                </Button>
                {explorerUrl && (
                  <a
                    href={explorerUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-5 w-5"
                      aria-label="View transaction"
                    >
                      <ExternalLink className="h-3 w-3" />
                    </Button>
                  </a>
                )}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="text-center mt-6">
        <Link href="/wallet">
          <Button className="w-full sm:w-auto text-white">
            Back to Wallet
          </Button>
        </Link>
      </div>
    </div>
  );
}
