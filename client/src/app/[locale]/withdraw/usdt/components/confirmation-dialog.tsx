"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Separator } from "@/components/ui/separator";
import { formatNumber } from "@/lib/utils/index";
import { Network } from "./types";

interface ConfirmationDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  withdrawalType: "external" | "internal";
  amount: number;
  address?: string;
  username?: string;
  selectedNetwork?: Network | null;
  withdrawalFee: number;
  totalAmount: number;
  isSubmitting: boolean;
  onConfirm: () => void;
}

export function ConfirmationDialog({
  open,
  onOpenChange,
  withdrawalType,
  amount,
  address,
  username,
  selectedNetwork,
  withdrawalFee,
  totalAmount,
  isSubmitting,
  onConfirm,
}: ConfirmationDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
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
              {formatNumber(amount)} USDT
            </span>
            <span className="text-sm text-muted-foreground">
              ~ ${formatNumber(amount)}
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
            onClick={() => onOpenChange(false)}
            className="w-full sm:w-auto"
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button
            onClick={onConfirm}
            className="w-full sm:w-auto text-white"
            disabled={isSubmitting}
          >
            {isSubmitting ? "Processing..." : "Confirm"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
