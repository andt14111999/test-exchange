"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Checkbox } from "@/components/ui/checkbox";
import { Shield, AlertTriangle } from "lucide-react";
import {
  setDeviceTrustPreference,
  getDeviceTrustPreference,
} from "@/lib/utils/device-trust-preference";

interface TwoFactorAuthInputProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (code: string) => Promise<void> | void;
  title?: string;
  description?: string;
  submitText?: string;
  isLoading?: boolean;
  error?: string | null;
  onError?: (error: string | null) => void;
}

export function TwoFactorAuthInput({
  open,
  onOpenChange,
  onSubmit,
  title,
  description,
  submitText,
  isLoading = false,
  error,
  onError,
}: TwoFactorAuthInputProps) {
  const t = useTranslations("withdraw.twoFactorAuth");

  // Use provided props or fallback to translations
  const finalTitle = title || t("title");
  const finalDescription = description || t("description");
  const finalSubmitText = submitText || t("submitText");
  const [code, setCode] = useState<string>("");
  const [localError, setLocalError] = useState<string | null>(null);
  const [trustDevice, setTrustDevice] = useState<boolean>(
    getDeviceTrustPreference(),
  );

  // Use external error if provided, otherwise use local error
  const displayError = error ?? localError;

  const handleSubmit = async () => {
    if (!code || code.length !== 6) {
      const errorMsg = t("invalidCodeLength");
      if (onError) {
        onError(errorMsg);
      } else {
        setLocalError(errorMsg);
      }
      return;
    }

    try {
      // Clear errors before submitting
      if (onError) {
        onError(null);
      } else {
        setLocalError(null);
      }

      // Save device trust preference before submitting
      setDeviceTrustPreference(trustDevice);

      await onSubmit(code);

      // Clear form on success
      setCode("");
    } catch (error) {
      const errorMsg =
        error instanceof Error ? error.message : t("genericError");
      if (onError) {
        onError(errorMsg);
      } else {
        setLocalError(errorMsg);
      }
    }
  };

  const handleCancel = () => {
    setCode("");
    if (onError) {
      onError(null);
    } else {
      setLocalError(null);
    }
    onOpenChange(false);
  };

  const handleCodeChange = (value: string) => {
    // Only allow numbers and limit to 6 digits
    const cleanValue = value.replace(/\D/g, "").slice(0, 6);
    setCode(cleanValue);

    // Clear errors when user starts typing
    if (onError) {
      onError(null);
    } else {
      setLocalError(null);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Shield className="h-5 w-5" />
            {finalTitle}
          </DialogTitle>
          <DialogDescription>{finalDescription}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <Alert>
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>{t("alertDescription")}</AlertDescription>
          </Alert>

          <div className="space-y-2">
            <Label htmlFor="two-factor-code">{t("codeLabel")}</Label>
            <Input
              id="two-factor-code"
              value={code}
              onChange={(e) => handleCodeChange(e.target.value)}
              placeholder={t("codePlaceholder")}
              maxLength={6}
              className="text-center text-lg tracking-widest font-mono"
              autoComplete="one-time-code"
              autoFocus
            />
            <div className="text-xs text-muted-foreground text-center">
              {t("codeHelper")}
            </div>
          </div>

          {displayError && (
            <Alert variant="destructive">
              <AlertDescription>{displayError}</AlertDescription>
            </Alert>
          )}

          {/* Trust Device Checkbox */}
          <div className="flex items-center space-x-2">
            <Checkbox
              id="trust-device"
              checked={trustDevice}
              onCheckedChange={(checked) => setTrustDevice(Boolean(checked))}
            />
            <Label
              htmlFor="trust-device"
              className="text-sm font-normal cursor-pointer"
            >
              {t("trustDevice")}
            </Label>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleCancel} disabled={isLoading}>
            {t("cancel")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={isLoading || code.length !== 6}
          >
            {isLoading ? t("authenticating") : finalSubmitText}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
