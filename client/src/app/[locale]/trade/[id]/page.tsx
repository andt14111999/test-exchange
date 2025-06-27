"use client";

import { PaymentProofUploadModal } from "@/components/payment-proof-upload-modal";
import { useToast } from "@/components/ui/use-toast";
import {
  useDisputeTrade,
  useMarkTradePaid,
  useReleaseTrade,
  useTrade,
  useCancelTrade,
} from "@/lib/api/hooks/use-trades";
import { useUserStore } from "@/lib/store/user-store";
import { formatFiatAmount } from "@/lib/utils/index";
import { useQueryClient } from "@tanstack/react-query";
import { CheckIcon, InfoIcon, TimerIcon, Copy as CopyIcon } from "lucide-react";
import { useTranslations } from "next-intl";
import { useParams, useRouter } from "next/navigation";
import React, { useState, useEffect } from "react";
import { VietQR } from "@/components/viet-qr";
import { CopyableField } from "@/components/copyable-field";
import { ImageViewer } from "@/components/ui/image-viewer";

// Components
import { FiatTransactionLayout } from "@/components/fiat-transaction-layout";
import { ProtectedLayout } from "@/components/protected-layout";
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
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { ApiTrade } from "@/lib/api/trades";
import { useTradeChannel } from "@/hooks/use-trade-channel";

// =============================================================================
// Type Definitions
// =============================================================================

interface User {
  id: string;
  role: string;
}

type TranslationFunction = (key: string) => string;

interface CardProps {
  t: TranslationFunction;
  router?: ReturnType<typeof useRouter>;
  trade?: ApiTrade;
}

interface CountdownProps {
  trade: ApiTrade;
  t: TranslationFunction;
  onComplete?: () => void;
}

interface CancelTradeModalProps {
  trade: ApiTrade;
  onClose: () => void;
}

interface DisputeTradeModalProps {
  trade: ApiTrade;
  onClose: () => void;
}

interface TradeDetailContentProps {
  trade: ApiTrade;
  t: TranslationFunction;
  router: ReturnType<typeof useRouter>;
  user: User;
}

// =============================================================================
// Utility Functions
// =============================================================================

/**
 * Determines the trade type (BUY/SELL) based on the trade and user info
 */
const getTradeType = (trade: ApiTrade, user: User): "BUY" | "SELL" => {
  // If user is merchant, check seller.id
  if (user?.role === "merchant") {
    return Number(trade.seller.id) === Number(user?.id) ? "BUY" : "SELL";
  }

  // Otherwise use taker_side
  return trade.taker_side === "buy" ? "BUY" : "SELL";
};

/**
 * Maps API status to display status
 */
const mapStatus = (status: string): string => {
  const statusMap: Record<string, string> = {
    completed: "COMPLETED",
    cancelled: "CANCELLED",
    awaiting: "PENDING",
    paid: "PAID",
    disputed: "DISPUTED",
  };
  return statusMap[status] || status.toUpperCase();
};

/**
 * Returns the appropriate CSS class for a status badge
 */
const getStatusBadgeColor = (status: string): string => {
  const colorMap: Record<string, string> = {
    released: "bg-green-100 text-green-800 border border-green-200",
    cancelled: "bg-gray-100 text-gray-800 border border-gray-200",
    unpaid: "bg-yellow-100 text-yellow-800 border border-yellow-200",
    paid: "bg-blue-100 text-blue-800 border border-blue-200",
    disputed: "bg-red-100 text-red-800 border border-red-200",
  };
  return colorMap[status] || "bg-gray-100 text-gray-800 border border-gray-200";
};

/**
 * Formats a time value in seconds to a MM:SS string
 */
const formatTime = (seconds: number): string => {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}:${remainingSeconds < 10 ? "0" : ""}${remainingSeconds}`;
};

/**
 * Calculates the correct payment amount based on trade type
 * For sell trades (merchant to user), show amount_after_fee
 * For buy trades (user to merchant), show the full fiat_amount
 */
const getPaymentAmount = (trade: ApiTrade): string => {
  return trade.taker_side === "sell" && trade.amount_after_fee
    ? trade.amount_after_fee
    : trade.fiat_amount;
};

/**
 * Determines if a user is the buyer in a trade
 */
const isBuyerInTrade = (trade: ApiTrade, user: User | null): boolean => {
  return user ? Number(trade.buyer.id) === Number(user.id) : false;
};

/**
 * Determines if a user is the seller in a trade
 */
const isSellerInTrade = (trade: ApiTrade, user: User | null): boolean => {
  return user ? Number(trade.seller.id) === Number(user.id) : false;
};

// =============================================================================
// Loading Skeleton
// =============================================================================

/**
 * Skeleton loader for trade details
 */
const TradeDetailSkeleton = ({ t }: { t: TranslationFunction }) => (
  <FiatTransactionLayout title={t("tradeDetail")}>
    <div className="space-y-8">
      <Card className="overflow-hidden">
        <CardHeader className="pb-4">
          <div className="space-y-3">
            <Skeleton className="h-7 w-64" />
            <Skeleton className="h-5 w-48" />
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <Skeleton className="h-5 w-24" />
              <Skeleton className="h-6 w-full" />
              <Skeleton className="h-5 w-24" />
              <Skeleton className="h-6 w-full" />
            </div>
            <div className="space-y-4">
              <Skeleton className="h-5 w-24" />
              <Skeleton className="h-6 w-full" />
              <Skeleton className="h-5 w-24" />
              <Skeleton className="h-6 w-full" />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  </FiatTransactionLayout>
);

/**
 * Modal for cancelling a trade
 */
const CancelTradeModal = ({ trade, onClose }: CancelTradeModalProps) => {
  const t = useTranslations("merchant.transactions") as TranslationFunction;
  const { toast } = useToast();
  const [cancelReason, setCancelReason] = useState("");
  const { mutate: cancelTrade, isPending: isCancelling } = useCancelTrade();
  const queryClient = useQueryClient();

  const handleSubmit = () => {
    // Validate reason before submitting
    if (!cancelReason.trim()) {
      toast({
        title: t("cancelReasonRequired"),
        variant: "destructive",
      });
      return;
    }

    // Call the cancel trade API
    cancelTrade(
      { id: trade.id.toString(), params: { cancel_reason: cancelReason } },
      {
        onSuccess: () => {
          toast({ title: t("tradeCancelled") });
          // Refresh trade data
          queryClient.invalidateQueries({
            queryKey: ["trade", trade.id.toString()],
          });
          queryClient.invalidateQueries({ queryKey: ["trades"] });
          onClose();
        },
        onError: () => {
          toast({
            title: t("failedToCancelTrade"),
            variant: "destructive",
          });
        },
      },
    );
  };

  return (
    <Dialog open={true} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t("cancelTrade")}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="grid w-full items-center gap-1.5">
            <div className="bg-red-50 p-3 rounded-lg border border-red-200 mb-3">
              <p className="text-sm text-red-700 font-medium">
                {t("cancelWarningTitle")}
              </p>
              <p className="text-sm text-red-700 mt-1">
                {t("cancelWarningMessage")}
              </p>
            </div>
            <Textarea
              placeholder={t("enterCancelReason")}
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              rows={4}
            />
            <p className="text-sm text-muted-foreground">
              {t("cancelReasonDescription")}
            </p>
          </div>
        </div>

        <DialogFooter className="sm:justify-end">
          <DialogClose asChild>
            <Button variant="outline" disabled={isCancelling}>
              {t("back")}
            </Button>
          </DialogClose>
          <Button
            onClick={handleSubmit}
            disabled={!cancelReason.trim() || isCancelling}
            variant="destructive"
          >
            {isCancelling ? t("cancelling") : t("confirmCancel")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

/**
 * Modal for disputing a trade
 */
const DisputeTradeModal = ({ trade, onClose }: DisputeTradeModalProps) => {
  const t = useTranslations("merchant.transactions") as TranslationFunction;
  const { toast } = useToast();
  const [disputeReason, setDisputeReason] = useState("");
  const { mutate: disputeTrade, isPending: isDisputing } = useDisputeTrade();

  const handleSubmit = () => {
    // Validate reason before submitting
    if (!disputeReason.trim()) {
      toast({
        title: t("disputeReasonRequired"),
        variant: "destructive",
      });
      return;
    }

    // Call the dispute trade API
    disputeTrade(
      { id: trade.id.toString(), params: { dispute_reason: disputeReason } },
      {
        onSuccess: () => {
          toast({ title: t("tradeDisputed") });
          onClose();
        },
        onError: () => {
          toast({
            title: t("failedToDisputeTrade"),
            variant: "destructive",
          });
        },
      },
    );
  };

  return (
    <Dialog open={true} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t("disputeTrade")}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="grid w-full items-center gap-1.5">
            <Textarea
              placeholder={t("enterDisputeReason")}
              value={disputeReason}
              onChange={(e) => setDisputeReason(e.target.value)}
              rows={4}
            />
            <p className="text-sm text-muted-foreground">
              {t("disputeReasonDescription")}
            </p>
          </div>
        </div>

        <DialogFooter className="sm:justify-end">
          <DialogClose asChild>
            <Button variant="outline" disabled={isDisputing}>
              {t("cancel")}
            </Button>
          </DialogClose>
          <Button
            onClick={handleSubmit}
            disabled={!disputeReason.trim() || isDisputing}
            variant="destructive"
          >
            {isDisputing ? t("disputing") : t("submitDispute")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

/**
 * Card displayed when a trade is in "awaiting payment" state
 */
const AwaitingPaymentCard = ({ trade, t, router }: Required<CardProps>) => {
  const { mutate: markPaid, isPending: isMarkingPaid } = useMarkTradePaid();
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const { toast } = useToast();
  const { user } = useUserStore();
  const queryClient = useQueryClient();

  const handleMarkPaid = (paymentProof: {
    file: File;
    description: string;
  }) => {
    if (!user) return;

    markPaid(
      {
        id: trade.id.toString(),
        payment_receipt_details: {
          file: paymentProof.file,
          description: paymentProof.description,
        },
      },
      {
        onSuccess: () => {
          toast({ title: t("tradeMarkedAsPaid") });
          setShowUploadModal(false);
          queryClient.invalidateQueries({
            queryKey: ["trade", trade.id.toString()],
          });
        },
        onError: () =>
          toast({
            title: t("failedToMarkTradeAsPaid"),
            variant: "destructive",
          }),
      },
    );
  };

  // Check if the current user is the buyer in this trade
  const isBuyer = isBuyerInTrade(trade, user);

  // Buyer always needs to upload payment proof
  const shouldShowUploadButton = isBuyer;

  // Get the appropriate payment amount based on trade type
  const paymentAmount = getPaymentAmount(trade);

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("awaitingPayment")}</CardTitle>
        <CardDescription>{t("awaitingPaymentDescription")}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-6">
          <div className="bg-yellow-50 p-3 rounded-lg border border-yellow-200 flex items-start space-x-3">
            <InfoIcon className="h-5 w-5 text-yellow-500 mt-0.5 flex-shrink-0" />
            <div className="text-sm text-yellow-700">
              {t("awaitingPaymentInfo")}
            </div>
          </div>

          {/* Payment Information - Only shown to buyers */}
          {isBuyer && (
            <Card className="shadow-sm">
              <CardHeader>
                <CardTitle>{t("paymentInformation")}</CardTitle>
                <CardDescription>
                  {t("paymentInformationDescription")}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {/* Bank Information */}
                  <div className="bg-slate-50 p-4 rounded">
                    <div className="flex items-center justify-between">
                      <div className="text-gray-500 text-sm">
                        {t("bankName")}
                      </div>
                      <div className="flex items-center">
                        <div className="font-medium">
                          {trade.payment_details.bank_name}
                        </div>
                        <CopyableField
                          content={<CopyIcon className="h-4 w-4" />}
                          value={trade.payment_details.bank_name}
                          copyMessage="Bank name copied"
                        />
                      </div>
                    </div>
                  </div>

                  <div className="bg-slate-50 p-4 rounded">
                    <div className="flex items-center justify-between">
                      <div className="text-gray-500 text-sm">
                        {t("accountName")}
                      </div>
                      <div className="flex items-center">
                        <div className="font-medium">
                          {trade.payment_details.bank_account_name}
                        </div>
                        <CopyableField
                          content={<CopyIcon className="h-4 w-4" />}
                          value={trade.payment_details.bank_account_name}
                          copyMessage="Account name copied"
                        />
                      </div>
                    </div>
                  </div>

                  <div className="bg-slate-50 p-4 rounded">
                    <div className="flex items-center justify-between">
                      <div className="text-gray-500 text-sm">
                        {t("accountNumber")}
                      </div>
                      <div className="flex items-center">
                        <div className="font-medium">
                          {trade.payment_details.bank_account_number}
                        </div>
                        <CopyableField
                          content={<CopyIcon className="h-4 w-4" />}
                          value={trade.payment_details.bank_account_number}
                          copyMessage="Account number copied"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Payment Amount */}
                  <div className="bg-slate-50 p-4 rounded">
                    <div className="flex items-center justify-between">
                      <div className="text-gray-500 text-sm">{t("amount")}</div>
                      <div className="flex items-center">
                        <div className="font-medium">
                          {formatFiatAmount(
                            parseFloat(paymentAmount),
                            trade.fiat_currency,
                          )}
                        </div>
                        <CopyableField
                          content={<CopyIcon className="h-4 w-4" />}
                          value={paymentAmount}
                          copyMessage="Amount copied"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Transfer Note (if any) */}
                  {trade.trade_memo && (
                    <div className="bg-slate-50 p-4 rounded">
                      <div className="flex items-center justify-between">
                        <div className="text-gray-500 text-sm">
                          {t("transferNote")}
                        </div>
                        <div className="flex items-center">
                          <div className="font-medium">{trade.trade_memo}</div>
                          <CopyableField
                            content={<CopyIcon className="h-4 w-4" />}
                            value={trade.trade_memo}
                            copyMessage="Transfer note copied"
                          />
                        </div>
                      </div>
                    </div>
                  )}

                  {/* VietQR Component */}
                  <VietQR
                    bankName={trade.payment_details.bank_name || ""}
                    accountName={trade.payment_details.bank_account_name || ""}
                    accountNumber={
                      trade.payment_details.bank_account_number || ""
                    }
                    amount={paymentAmount || "0"}
                    content={trade.trade_memo || `Trade ${trade.ref}`}
                    currency={trade.fiat_currency}
                    copyButtonText={t("copyPaymentInfo")}
                    scanQRText={t("scanQRCode")}
                    qrSize={220}
                    useImageAPI={true}
                  />
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </CardContent>
      <CardFooter className="flex flex-col space-y-3 pt-2">
        {shouldShowUploadButton && (
          <Button
            className="w-full"
            onClick={() => setShowUploadModal(true)}
            disabled={isMarkingPaid}
          >
            {t("uploadPaymentProof")}
          </Button>
        )}
        {isBuyer && (
          <Button
            variant="outline"
            className="w-full text-red-500 hover:bg-red-50 hover:text-red-600"
            onClick={() => setShowCancelModal(true)}
            disabled={isMarkingPaid}
          >
            {t("cancelTrade")}
          </Button>
        )}
        <Button
          variant="outline"
          className="w-full"
          onClick={() => router.push("/transactions")}
          disabled={isMarkingPaid}
        >
          {t("backToTransactions")}
        </Button>
      </CardFooter>

      {/* Modals */}
      {showUploadModal && (
        <PaymentProofUploadModal
          onClose={() => setShowUploadModal(false)}
          onSuccess={handleMarkPaid}
        />
      )}

      {showCancelModal && (
        <CancelTradeModal
          trade={trade}
          onClose={() => setShowCancelModal(false)}
        />
      )}
    </Card>
  );
};

/**
 * Card displayed when a trade is in "paid" state
 */
const PaidCard = ({ t, trade }: Omit<Required<CardProps>, "router">) => {
  const { user } = useUserStore();
  const { mutate: releaseTrade, isPending: isReleasing } = useReleaseTrade();
  const [showDisputeModal, setShowDisputeModal] = useState(false);
  const [showReleaseConfirmModal, setShowReleaseConfirmModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const isSeller = isSellerInTrade(trade, user);
  const isBuyer = isBuyerInTrade(trade, user);

  // Get the appropriate payment amount based on trade type
  const paymentAmount = getPaymentAmount(trade);

  const handleRelease = async () => {
    try {
      await releaseTrade(trade.id.toString(), {
        onSuccess: () => {
          toast({ title: t("tradeReleased") });
          // Invalidate related queries to refresh data
          queryClient.invalidateQueries({ queryKey: ["trade", trade.id] });
          queryClient.invalidateQueries({ queryKey: ["trades"] });
          queryClient.invalidateQueries({ queryKey: ["transactions"] });
          queryClient.invalidateQueries({ queryKey: ["balances"] });
          setShowReleaseConfirmModal(false);
        },
        onError: () =>
          toast({
            title: t("failedToReleaseTrade"),
            variant: "destructive",
          }),
      });
    } catch (error) {
      console.error("Error releasing trade:", error);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("paymentReceived")}</CardTitle>
        <CardDescription>{t("paymentReceivedDescription")}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="bg-blue-50 p-3 rounded-lg border border-blue-200 flex items-start space-x-3">
            <InfoIcon className="h-5 w-5 text-blue-500 mt-0.5 flex-shrink-0" />
            <div className="text-sm text-blue-700">
              {t("paymentReceivedInfo")}
            </div>
          </div>
        </div>
      </CardContent>

      {/* Payment Information - Only shown to buyers */}
      {isBuyer && (
        <Card className="shadow-sm mt-4">
          <CardHeader>
            <CardTitle>{t("paymentInformation")}</CardTitle>
            <CardDescription>
              {t("paymentInformationDescription")}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {/* Bank Information */}
              <div className="bg-slate-50 p-4 rounded">
                <div className="flex items-center justify-between">
                  <div className="text-gray-500 text-sm">{t("bankName")}</div>
                  <div className="flex items-center">
                    <div className="font-medium">
                      {trade.payment_details.bank_name}
                    </div>
                    <CopyableField
                      content={<CopyIcon className="h-4 w-4" />}
                      value={trade.payment_details.bank_name}
                      copyMessage="Bank name copied"
                    />
                  </div>
                </div>
              </div>

              <div className="bg-slate-50 p-4 rounded">
                <div className="flex items-center justify-between">
                  <div className="text-gray-500 text-sm">
                    {t("accountName")}
                  </div>
                  <div className="flex items-center">
                    <div className="font-medium">
                      {trade.payment_details.bank_account_name}
                    </div>
                    <CopyableField
                      content={<CopyIcon className="h-4 w-4" />}
                      value={trade.payment_details.bank_account_name}
                      copyMessage="Account name copied"
                    />
                  </div>
                </div>
              </div>

              <div className="bg-slate-50 p-4 rounded">
                <div className="flex items-center justify-between">
                  <div className="text-gray-500 text-sm">
                    {t("accountNumber")}
                  </div>
                  <div className="flex items-center">
                    <div className="font-medium">
                      {trade.payment_details.bank_account_number}
                    </div>
                    <CopyableField
                      content={<CopyIcon className="h-4 w-4" />}
                      value={trade.payment_details.bank_account_number}
                      copyMessage="Account number copied"
                    />
                  </div>
                </div>
              </div>

              {/* Payment Amount */}
              <div className="bg-slate-50 p-4 rounded">
                <div className="flex items-center justify-between">
                  <div className="text-gray-500 text-sm">{t("amount")}</div>
                  <div className="flex items-center">
                    <div className="font-medium">
                      {formatFiatAmount(
                        parseFloat(paymentAmount),
                        trade.fiat_currency,
                      )}
                    </div>
                    <CopyableField
                      content={<CopyIcon className="h-4 w-4" />}
                      value={paymentAmount}
                      copyMessage="Amount copied"
                    />
                  </div>
                </div>
              </div>

              {/* Transfer Note (if any) */}
              {trade.trade_memo && (
                <div className="bg-slate-50 p-4 rounded">
                  <div className="flex items-center justify-between">
                    <div className="text-gray-500 text-sm">
                      {t("transferNote")}
                    </div>
                    <div className="flex items-center">
                      <div className="font-medium">{trade.trade_memo}</div>
                      <CopyableField
                        content={<CopyIcon className="h-4 w-4" />}
                        value={trade.trade_memo}
                        copyMessage="Transfer note copied"
                      />
                    </div>
                  </div>
                </div>
              )}

              {/* VietQR Component */}
              <VietQR
                bankName={trade.payment_details.bank_name || ""}
                accountName={trade.payment_details.bank_account_name || ""}
                accountNumber={trade.payment_details.bank_account_number || ""}
                amount={paymentAmount || "0"}
                content={trade.trade_memo || `Trade ${trade.ref}`}
                currency={trade.fiat_currency}
                copyButtonText={t("copyPaymentInfo")}
                scanQRText={t("scanQRCode")}
                qrSize={220}
                useImageAPI={true}
              />
            </div>
          </CardContent>
        </Card>
      )}

      {/* Action buttons - Only shown to sellers or buyers */}
      <CardFooter className="flex flex-col space-y-3 pt-2">
        {/* Seller actions */}
        {isSeller && (
          <>
            <Button
              className="w-full"
              onClick={() => setShowReleaseConfirmModal(true)}
              disabled={isReleasing}
            >
              {isReleasing ? t("releasing") : t("releaseFunds")}
            </Button>
            <Button
              variant="outline"
              className="w-full text-red-500 hover:bg-red-50 hover:text-red-600"
              onClick={() => setShowDisputeModal(true)}
              disabled={isReleasing}
            >
              {t("disputeTrade")}
            </Button>
          </>
        )}

        {/* Buyer actions */}
        {isBuyer && (
          <Button
            variant="outline"
            className="w-full text-red-500 hover:bg-red-50 hover:text-red-600"
            onClick={() => setShowCancelModal(true)}
            disabled={isReleasing}
          >
            {t("cancelTrade")}
          </Button>
        )}
      </CardFooter>

      {/* Release Confirmation Modal */}
      {showReleaseConfirmModal && (
        <Dialog open={true} onOpenChange={setShowReleaseConfirmModal}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>{t("confirmReleaseFunds")}</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="bg-amber-50 p-3 rounded-lg border border-amber-200">
                <p className="text-sm text-amber-800 font-medium">
                  {t("releaseConfirmationWarning")}
                </p>
              </div>
              <div className="space-y-4">
                <div className="bg-slate-50 p-3 rounded-lg">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {t("recipient")}
                    </span>
                    <span className="font-medium">
                      {trade.buyer.display_name ||
                        trade.buyer.email ||
                        `ID: ${trade.buyer.id}`}
                    </span>
                  </div>
                </div>
                <div className="bg-slate-50 p-3 rounded-lg">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {t("tradeAmount")}
                    </span>
                    <span className="font-medium">
                      {formatFiatAmount(
                        parseFloat(trade.fiat_amount),
                        trade.fiat_currency,
                      )}
                    </span>
                  </div>
                </div>
                <div className="bg-slate-50 p-3 rounded-lg">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {t("tradeReference")}
                    </span>
                    <span className="font-medium">{trade.ref}</span>
                  </div>
                </div>
              </div>
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setShowReleaseConfirmModal(false)}
                disabled={isReleasing}
              >
                {t("cancel")}
              </Button>
              <Button onClick={handleRelease} disabled={isReleasing}>
                {isReleasing ? t("releasing") : t("confirmRelease")}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}

      {/* Dispute Modal */}
      {showDisputeModal && (
        <DisputeTradeModal
          trade={trade}
          onClose={() => setShowDisputeModal(false)}
        />
      )}

      {/* Cancel Modal */}
      {showCancelModal && (
        <CancelTradeModal
          trade={trade}
          onClose={() => setShowCancelModal(false)}
        />
      )}
    </Card>
  );
};

/**
 * Card displayed when a trade is in "completed" state
 */
const CompletedCard = ({ t, router }: Required<Omit<CardProps, "trade">>) => (
  <Card>
    <CardHeader>
      <CardTitle>{t("tradeCompleted")}</CardTitle>
      <CardDescription>{t("tradeCompletedDescription")}</CardDescription>
    </CardHeader>
    <CardContent>
      <div className="bg-green-50 p-3 rounded-lg border border-green-200 flex items-start space-x-3">
        <CheckIcon className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
        <div className="text-sm text-green-700">{t("tradeCompletedInfo")}</div>
      </div>
    </CardContent>
    <CardFooter>
      <Button className="w-full" onClick={() => router.push("/transactions")}>
        {t("backToTransactions")}
      </Button>
    </CardFooter>
  </Card>
);

/**
 * Card displayed when a trade is in "cancelled" state
 */
const CancelledCard = ({ t, router }: Required<Omit<CardProps, "trade">>) => (
  <Card>
    <CardHeader>
      <CardTitle>{t("tradeCancelled")}</CardTitle>
      <CardDescription>{t("tradeCancelledDescription")}</CardDescription>
    </CardHeader>
    <CardContent>
      <div className="bg-red-50 p-3 rounded-lg border border-red-200 flex items-start space-x-3">
        <InfoIcon className="h-5 w-5 text-red-500 mt-0.5 flex-shrink-0" />
        <div className="text-sm text-red-700">{t("tradeCancelledInfo")}</div>
      </div>
    </CardContent>
    <CardFooter>
      <Button className="w-full" onClick={() => router.push("/transactions")}>
        {t("backToTransactions")}
      </Button>
    </CardFooter>
  </Card>
);

/**
 * Card displayed when a trade is in "disputed" state
 */
const DisputedCard = ({ t, router, trade }: Required<CardProps>) => {
  const { user } = useUserStore();
  const { mutate: releaseTrade, isPending: isReleasing } = useReleaseTrade();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [showReleaseConfirmModal, setShowReleaseConfirmModal] = useState(false);

  const isSeller = isSellerInTrade(trade, user);

  const handleRelease = async () => {
    try {
      await releaseTrade(trade.id.toString(), {
        onSuccess: () => {
          toast({ title: t("tradeReleased") });
          // Refresh related data
          queryClient.invalidateQueries({ queryKey: ["trade", trade.id] });
          queryClient.invalidateQueries({ queryKey: ["trades"] });
          queryClient.invalidateQueries({ queryKey: ["transactions"] });
          queryClient.invalidateQueries({ queryKey: ["balances"] });
          setShowReleaseConfirmModal(false);
        },
        onError: () =>
          toast({
            title: t("failedToReleaseTrade"),
            variant: "destructive",
          }),
      });
    } catch (error) {
      console.error("Error releasing trade:", error);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("tradeDisputed")}</CardTitle>
        <CardDescription>{t("tradeDisputedDescription")}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="bg-orange-50 p-3 rounded-lg border border-orange-200 flex items-start space-x-3">
          <InfoIcon className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
          <div className="text-sm text-orange-700">
            {t("tradeDisputedInfo")}
          </div>
        </div>

        {/* Dispute Reason (if available) */}
        {trade.dispute_reason && (
          <div className="mt-4 p-3 bg-slate-50 rounded-lg">
            <div className="text-sm text-gray-500 mb-1">
              {t("disputeReason")}:
            </div>
            <div className="text-sm">{trade.dispute_reason}</div>
          </div>
        )}

        {/* Dispute Resolution (if available) */}
        {trade.dispute_resolution && (
          <div className="mt-4 p-3 bg-slate-50 rounded-lg">
            <div className="text-sm text-gray-500 mb-1">
              {t("disputeResolution")}:
            </div>
            <div className="text-sm">{trade.dispute_resolution}</div>
          </div>
        )}
      </CardContent>
      <CardFooter className="flex flex-col space-y-3">
        {/* Release Button - Only shown to sellers */}
        {isSeller && (
          <Button
            className="w-full"
            onClick={() => setShowReleaseConfirmModal(true)}
            disabled={isReleasing}
          >
            {isReleasing ? t("releasing") : t("releaseFunds")}
          </Button>
        )}
        <Button
          className="w-full"
          variant="outline"
          onClick={() => router.push("/transactions")}
        >
          {t("backToTransactions")}
        </Button>
      </CardFooter>

      {/* Release Confirmation Modal */}
      {showReleaseConfirmModal && (
        <Dialog open={true} onOpenChange={setShowReleaseConfirmModal}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>{t("confirmReleaseFunds")}</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="bg-amber-50 p-3 rounded-lg border border-amber-200">
                <p className="text-sm text-amber-800 font-medium">
                  {t("releaseConfirmationWarning")}
                </p>
              </div>
              <div className="space-y-4">
                <div className="bg-slate-50 p-3 rounded-lg">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {t("recipient")}
                    </span>
                    <span className="font-medium">
                      {trade.buyer.display_name ||
                        trade.buyer.email ||
                        `ID: ${trade.buyer.id}`}
                    </span>
                  </div>
                </div>
                <div className="bg-slate-50 p-3 rounded-lg">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {t("tradeAmount")}
                    </span>
                    <span className="font-medium">
                      {formatFiatAmount(
                        parseFloat(trade.fiat_amount),
                        trade.fiat_currency,
                      )}
                    </span>
                  </div>
                </div>
                <div className="bg-slate-50 p-3 rounded-lg">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      {t("tradeReference")}
                    </span>
                    <span className="font-medium">{trade.ref}</span>
                  </div>
                </div>
              </div>
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setShowReleaseConfirmModal(false)}
                disabled={isReleasing}
              >
                {t("cancel")}
              </Button>
              <Button onClick={handleRelease} disabled={isReleasing}>
                {isReleasing ? t("releasing") : t("confirmRelease")}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </Card>
  );
};

// =============================================================================
// Helper Components
// =============================================================================

/**
 * Displays a countdown timer for trade timeouts (unpaid or paid)
 */
const TradeCountdown = ({ trade, t, onComplete }: CountdownProps) => {
  // Calculate time left based on timeout dates
  const calculateTimeLeft = () => {
    const isUnpaidCountdown = trade.countdown_status === "unpaid_countdown";
    const isPaidCountdown = trade.countdown_status === "paid_countdown";

    // For unpaid countdown, use unpaid_timeout_at
    if (isUnpaidCountdown && trade.unpaid_timeout_at) {
      const timeoutDate = new Date(trade.unpaid_timeout_at);
      const now = new Date();
      const diffSeconds = Math.floor(
        (timeoutDate.getTime() - now.getTime()) / 1000,
      );
      return Math.max(0, diffSeconds);
    }

    // For paid countdown, use paid_timeout_at
    if (isPaidCountdown && trade.paid_timeout_at) {
      const timeoutDate = new Date(trade.paid_timeout_at);
      const now = new Date();
      const diffSeconds = Math.floor(
        (timeoutDate.getTime() - now.getTime()) / 1000,
      );
      return Math.max(0, diffSeconds);
    }

    // Fallback to countdown_seconds or 0
    return trade.countdown_seconds || 0;
  };

  const [timeLeft, setTimeLeft] = useState<number>(calculateTimeLeft());

  // Setup timer effect
  useEffect(() => {
    // If no time left, call onComplete and exit
    if (timeLeft <= 0) {
      if (onComplete) onComplete();
      return;
    }

    // Create interval to update timeLeft
    const timer = setInterval(() => {
      setTimeLeft((prevTime) => {
        const newTime = prevTime - 1;
        if (newTime <= 0 && onComplete) {
          clearInterval(timer);
          onComplete();
        }
        return newTime > 0 ? newTime : 0;
      });
    }, 1000);

    // Cleanup timer on unmount
    return () => clearInterval(timer);
  }, [timeLeft, onComplete]);

  const isUnpaidCountdown = trade.countdown_status === "unpaid_countdown";
  const isPaidCountdown = trade.countdown_status === "paid_countdown";

  // Only render if there's an active countdown
  if (!isUnpaidCountdown && !isPaidCountdown) return null;

  // Style variables based on countdown type
  const containerClass = isUnpaidCountdown
    ? "bg-yellow-50 border border-yellow-200"
    : "bg-blue-50 border border-blue-200";

  const iconClass = isUnpaidCountdown ? "text-yellow-500" : "text-blue-500";

  const timerClass =
    timeLeft < 60
      ? "text-red-600"
      : isUnpaidCountdown
        ? "text-yellow-600"
        : "text-blue-600";

  return (
    <div
      className={`flex items-center rounded-lg p-3 space-x-2 ${containerClass}`}
    >
      <TimerIcon className={`h-5 w-5 ${iconClass}`} />
      <div className="flex-1">
        <h4 className="font-medium text-sm">
          {isUnpaidCountdown ? t("autoCancel") : t("autoDispute")}
        </h4>
        <p className="text-sm">
          {isUnpaidCountdown
            ? t("autoCancelDescription")
            : t("autoDisputeDescription")}
        </p>
      </div>
      <div className={`text-lg font-bold px-3 py-1 rounded ${timerClass}`}>
        {formatTime(timeLeft)}
      </div>
    </div>
  );
};

/**
 * Main content for the trade detail page
 */
const TradeDetailContent = ({
  trade,
  t,
  router,
  user,
}: TradeDetailContentProps): React.ReactNode => {
  const queryClient = useQueryClient();

  const handleCountdownComplete = () => {
    // Refresh data when countdown completes
    queryClient.invalidateQueries({ queryKey: ["trade", trade.id.toString()] });
  };

  return (
    <FiatTransactionLayout title={t("tradeDetail")}>
      <div className="space-y-4">
        {/* Countdown Timer (if applicable) */}
        {(trade.countdown_status === "unpaid_countdown" ||
          trade.countdown_status === "paid_countdown") && (
          <TradeCountdown
            trade={trade}
            t={t}
            onComplete={handleCountdownComplete}
          />
        )}

        {/* Trade Status and Date */}
        <div className="space-y-4">
          <div className="bg-slate-50 p-4 rounded-lg">
            <div className="flex items-center justify-between">
              <div className="text-gray-500 text-sm">{t("tradeStatus")}</div>
              <div
                className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${getStatusBadgeColor(trade.status)}`}
              >
                {mapStatus(trade.status)}
              </div>
            </div>
          </div>
          <div className="bg-slate-50 p-4 rounded-lg">
            <div className="flex items-center justify-between">
              <div className="text-gray-500 text-sm">{t("tradeDate")}</div>
              <div className="font-medium">
                {new Date(trade.created_at).toLocaleDateString()}
              </div>
            </div>
          </div>
        </div>

        {/* Trade Amount Information */}
        <div className="bg-slate-50 p-4 rounded-lg space-y-3">
          <div className="flex items-center justify-between">
            <div className="text-gray-500 text-sm">{t("tradeAmount")}</div>
            <div className="text-xl font-bold">
              {formatFiatAmount(
                parseFloat(trade.fiat_amount),
                trade.fiat_currency,
              )}
            </div>
          </div>

          {trade.total_fee && (
            <div className="flex items-center justify-between">
              <div className="text-gray-500 text-sm">{t("totalFee")}</div>
              <div>
                {trade.total_fee} {trade.fiat_currency}
              </div>
            </div>
          )}

          {trade.amount_after_fee && (
            <div className="flex items-center justify-between">
              <div className="text-gray-500 text-sm">{t("amountAfterFee")}</div>
              <div className="font-medium">
                {trade.amount_after_fee} {trade.fiat_currency}
              </div>
            </div>
          )}
        </div>

        {/* Trade Details Card */}
        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle>{t("tradeInformation")}</CardTitle>
            <CardDescription>
              {t("tradeInformationDescription")}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {/* Trade Reference */}
              <div className="bg-slate-50 p-4 rounded">
                <div className="flex items-center justify-between">
                  <div className="text-gray-500 text-sm">{t("reference")}</div>
                  <div className="font-medium">{trade.ref}</div>
                </div>
              </div>

              {/* Trade Type */}
              <div className="bg-slate-50 p-4 rounded">
                <div className="flex items-center justify-between">
                  <div className="text-gray-500 text-sm">{t("type")}</div>
                  <div
                    className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      getTradeType(trade, user) === "SELL"
                        ? "bg-red-100 text-red-800"
                        : "bg-green-100 text-green-800"
                    }`}
                  >
                    {getTradeType(trade, user) === "SELL"
                      ? t("sell")
                      : t("buy")}
                  </div>
                </div>
              </div>

              {/* Payment Method */}
              <div className="bg-slate-50 p-4 rounded">
                <div className="flex items-center justify-between">
                  <div className="text-gray-500 text-sm">
                    {t("paymentType")}
                  </div>
                  <div className="font-medium">{trade.payment_method}</div>
                </div>
              </div>

              {/* Payment Receipt (if available and trade is not unpaid) */}
              {trade.payment_receipt_details && trade.status !== "unpaid" && (
                <div className="bg-slate-50 p-4 rounded">
                  <div className="space-y-4">
                    <div className="text-gray-500 text-sm">
                      {t("paymentProof")}
                    </div>
                    {trade.payment_receipt_details.file_url ? (
                      <div className="flex justify-center items-center w-full">
                        <div className="w-full max-w-md flex justify-center">
                          <ImageViewer
                            src={trade.payment_receipt_details.file_url}
                            alt="Payment proof"
                            className="mx-auto"
                            maxHeight={300}
                          />
                        </div>
                      </div>
                    ) : "file" in trade.payment_receipt_details ? (
                      <div className="p-3 bg-yellow-50 rounded-lg border border-yellow-200">
                        <p className="text-sm text-yellow-700">
                          {t("paymentProofUploading")}
                        </p>
                      </div>
                    ) : (
                      <div className="p-3 bg-gray-50 rounded-lg border border-gray-200">
                        <p className="text-sm text-gray-700">
                          {t("noPaymentProof")}
                        </p>
                      </div>
                    )}
                    {trade.payment_receipt_details.description && (
                      <div className="text-sm text-gray-600">
                        {trade.payment_receipt_details.description}
                      </div>
                    )}
                    {trade.payment_receipt_details.uploaded_at && (
                      <div className="text-sm text-gray-500">
                        {t("uploadedAt")}:{" "}
                        {new Date(
                          trade.payment_receipt_details.uploaded_at,
                        ).toLocaleString()}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Status-specific action cards */}
        {trade.status === "unpaid" && (
          <AwaitingPaymentCard trade={trade} t={t} router={router} />
        )}
        {trade.status === "paid" && <PaidCard t={t} trade={trade} />}
        {trade.status === "completed" && (
          <CompletedCard t={t} router={router} />
        )}
        {trade.status === "cancelled" && (
          <CancelledCard t={t} router={router} />
        )}
        {trade.status === "disputed" && (
          <DisputedCard t={t} router={router} trade={trade} />
        )}
      </div>
    </FiatTransactionLayout>
  );
};

/**
 * Main page component for trade details
 */
export default function TradeDetailPage() {
  const params = useParams();
  const router = useRouter();
  const t = useTranslations("merchant.transactions") as TranslationFunction;
  const { user } = useUserStore();
  const tradeId = params.id as string;
  const queryClient = useQueryClient();

  // Fetch trade data
  const { data: trade, isLoading, error } = useTrade(tradeId);

  // Setup websocket connection for real-time updates
  useTradeChannel({
    tradeId,
    onTradeUpdated: (updatedTrade) => {
      // Validate trade data before updating
      if (!updatedTrade || !updatedTrade.id) {
        return;
      }

      // Get current trade data from cache
      const currentTrade = queryClient.getQueryData([
        "trade",
        tradeId,
      ]) as ApiTrade;

      // Merge all fields from websocket update
      const mergedTrade = {
        ...currentTrade,
        status: updatedTrade.status,
        countdown_status: updatedTrade.countdown_status,
        countdown_seconds: updatedTrade.countdown_seconds,
        paid_at: updatedTrade.paid_at,
        released_at: updatedTrade.released_at,
        cancelled_at: updatedTrade.cancelled_at,
        disputed_at: updatedTrade.disputed_at,
        expired_at: updatedTrade.expired_at,
        payment_receipt_details:
          updatedTrade.payment_receipt_details ||
          currentTrade.payment_receipt_details,
        updated_at: updatedTrade.updated_at,
      };

      // Update the trade data in the cache
      queryClient.setQueryData(["trade", tradeId], mergedTrade);
    },
  });

  // Show loading state
  if (isLoading) {
    return (
      <ProtectedLayout loadingFallback={<TradeDetailSkeleton t={t} />}>
        <TradeDetailSkeleton t={t} />
      </ProtectedLayout>
    );
  }

  // Show error state
  if (error || !trade || !user) {
    return (
      <ProtectedLayout>
        <div className="text-center py-8 text-red-500">{t("failedToLoad")}</div>
      </ProtectedLayout>
    );
  }

  // Show trade details
  return (
    <ProtectedLayout>
      <TradeDetailContent trade={trade} t={t} router={router} user={user} />
    </ProtectedLayout>
  );
}
