"use client";

import { useState } from "react";
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
import { disableTwoFactorAuth } from "@/lib/api/user";
import { Shield, AlertTriangle } from "lucide-react";
import { useToast } from "@/components/ui/use-toast";

interface TwoFactorAuthDisableDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

export function TwoFactorAuthDisableDialog({
  open,
  onOpenChange,
  onSuccess,
}: TwoFactorAuthDisableDialogProps) {
  const { toast } = useToast();
  const [verificationCode, setVerificationCode] = useState<string>("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDisable = async () => {
    if (!verificationCode || verificationCode.length !== 6) {
      setError("Vui lòng nhập mã 6 số");
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      await disableTwoFactorAuth(verificationCode);

      toast({
        title: "Thành công",
        description: "Xác thực 2 bước đã được tắt",
      });

      if (onSuccess) {
        onSuccess();
      }

      // Reset form
      setVerificationCode("");
      onOpenChange(false);
    } catch (error) {
      setError(
        error instanceof Error ? error.message : "Mã xác thực không đúng",
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setVerificationCode("");
    setError(null);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Shield className="h-5 w-5" />
            Tắt xác thực 2 bước
          </DialogTitle>
          <DialogDescription>
            Việc tắt xác thực 2 bước sẽ làm giảm tính bảo mật của tài khoản của
            bạn.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <Alert>
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              Để tắt xác thực 2 bước, vui lòng nhập mã 6 số từ ứng dụng xác thực
              của bạn.
            </AlertDescription>
          </Alert>

          <div className="space-y-2">
            <Label htmlFor="disable-verification-code">Mã xác thực</Label>
            <Input
              id="disable-verification-code"
              value={verificationCode}
              onChange={(e) => {
                const value = e.target.value.replace(/\D/g, "").slice(0, 6);
                setVerificationCode(value);
                setError(null);
              }}
              placeholder="123456"
              maxLength={6}
              className="text-center text-lg tracking-widest"
            />
          </div>

          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleCancel} disabled={isLoading}>
            Hủy
          </Button>
          <Button
            variant="destructive"
            onClick={handleDisable}
            disabled={isLoading || verificationCode.length !== 6}
          >
            {isLoading ? "Đang tắt..." : "Tắt 2FA"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
