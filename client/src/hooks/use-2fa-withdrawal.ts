import { useState, useCallback } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import {
  createWithdrawal,
  CreateWithdrawalRequest,
} from "@/lib/api/withdrawals";
import { trustCurrentDevice } from "@/lib/api/device";
import { useUserStore } from "@/lib/store/user-store";
import { useDeviceTrust } from "./use-device-trust";

export interface Use2FAWithdrawalReturn {
  show2FADialog: boolean;
  twoFactorError: string | null;
  isSubmitting: boolean;
  pendingWithdrawalData: CreateWithdrawalRequest | null;
  handle2FASubmit: (twoFactorCode: string) => Promise<void>;
  handleClose2FADialog: () => void;
  handleWithdrawWithDevice: (
    withdrawalData: CreateWithdrawalRequest,
  ) => Promise<void>;
  shouldShow2FAWarning: boolean;
}

/**
 * Custom hook to handle 2FA withdrawal logic
 * Manages device trust status and 2FA verification flow
 */
export function use2FAWithdrawal(): Use2FAWithdrawalReturn {
  const { user } = useUserStore();
  const { isDeviceTrusted, isCheckingDevice, setIsDeviceTrusted } =
    useDeviceTrust();
  const t = useTranslations("withdraw.twoFactorAuth");

  const [show2FADialog, setShow2FADialog] = useState(false);
  const [twoFactorError, setTwoFactorError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingWithdrawalData, setPendingWithdrawalData] =
    useState<CreateWithdrawalRequest | null>(null);

  // Determine if we should show 2FA warning
  const shouldShow2FAWarning =
    !isCheckingDevice &&
    Boolean(user?.authenticatorEnabled) &&
    !isDeviceTrusted;

  // Handle withdrawal with device trust check
  const handleWithdrawWithDevice = useCallback(
    async (withdrawalData: CreateWithdrawalRequest) => {
      // If 2FA is enabled and device is not trusted, show 2FA dialog
      if (user?.authenticatorEnabled && !isDeviceTrusted) {
        setPendingWithdrawalData(withdrawalData);
        setShow2FADialog(true);
        return;
      }

      // If device is trusted or 2FA is disabled, proceed directly
      setIsSubmitting(true);
      try {
        await createWithdrawal(withdrawalData);
        toast.success("Withdrawal request submitted successfully");
      } catch (error: unknown) {
        console.error("Withdrawal error:", error);
        let errorMessage = "An unexpected error occurred";

        if (error && typeof error === "object") {
          const err = error as {
            response?: { data?: { error?: string } };
            message?: string;
          };
          errorMessage =
            err.response?.data?.error || err.message || errorMessage;
        }

        toast.error(errorMessage);
      } finally {
        setIsSubmitting(false);
      }
    },
    [user?.authenticatorEnabled, isDeviceTrusted],
  );

  // Handle 2FA submission
  const handle2FASubmit = useCallback(
    async (twoFactorCode: string) => {
      if (!pendingWithdrawalData) return;

      setIsSubmitting(true);
      setTwoFactorError(null);

      try {
        // Submit withdrawal with 2FA code
        await createWithdrawal({
          ...pendingWithdrawalData,
          two_factor_code: twoFactorCode,
        });

        // Check if device was trusted after 2FA submission
        try {
          const updatedDevice = await trustCurrentDevice();
          setIsDeviceTrusted(updatedDevice.trusted);
        } catch (deviceError) {
          console.warn("Failed to check device trust status:", deviceError);
          // Don't fail the withdrawal if device check fails
        }

        toast.success("Withdrawal request submitted successfully");

        // Close dialog and reset state
        setShow2FADialog(false);
        setPendingWithdrawalData(null);
        setTwoFactorError(null);
      } catch (error: unknown) {
        console.error("2FA Withdrawal error:", error);
        let errorMessage = "An unexpected error occurred";

        if (error && typeof error === "object") {
          const err = error as {
            response?: { data?: { error?: string } };
            message?: string;
          };
          errorMessage =
            err.response?.data?.error || err.message || errorMessage;
        }

        // Check if it's a 2FA-specific error
        if (
          errorMessage.toLowerCase().includes("invalid") &&
          errorMessage.toLowerCase().includes("code")
        ) {
          setTwoFactorError(t("invalidCode"));
        } else {
          setTwoFactorError(errorMessage);
        }
      } finally {
        setIsSubmitting(false);
      }
    },
    [pendingWithdrawalData, t, setIsDeviceTrusted],
  );

  // Handle closing 2FA dialog
  const handleClose2FADialog = useCallback(() => {
    setShow2FADialog(false);
    setPendingWithdrawalData(null);
    setTwoFactorError(null);
  }, []);

  return {
    show2FADialog,
    twoFactorError,
    isSubmitting,
    pendingWithdrawalData,
    handle2FASubmit,
    handleClose2FADialog,
    handleWithdrawWithDevice,
    shouldShow2FAWarning,
  };
}
