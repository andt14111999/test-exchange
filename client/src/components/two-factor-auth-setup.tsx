"use client";

import { useState, useEffect } from "react";
import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { Checkbox } from "@/components/ui/checkbox";
import { enableTwoFactorAuth, verifyTwoFactorAuth } from "@/lib/api/user";
import { useRouter } from "@/navigation";
import { QrCode, Shield, CheckCircle, Copy, ArrowLeft } from "lucide-react";
import QRCode from "react-qr-code";
import { useToast } from "@/components/ui/use-toast";
import {
  setDeviceTrustPreference,
  getDeviceTrustPreference,
} from "@/lib/utils/device-trust-preference";

type SetupStep = "qr" | "verify" | "success";

interface TwoFactorAuthSetupProps {
  onComplete?: () => void;
}

export function TwoFactorAuthSetup({ onComplete }: TwoFactorAuthSetupProps) {
  const router = useRouter();
  const { toast } = useToast();
  const t = useTranslations("withdraw.twoFactorAuth.setup");
  const tTwoFA = useTranslations("withdraw.twoFactorAuth");

  const [step, setStep] = useState<SetupStep>("qr");
  const [qrCodeUri, setQrCodeUri] = useState<string>("");
  const [secretKey, setSecretKey] = useState<string>("");
  const [verificationCode, setVerificationCode] = useState<string>("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const [trustDevice, setTrustDevice] = useState<boolean>(
    getDeviceTrustPreference(),
  );

  // Initialize 2FA setup
  useEffect(() => {
    const initializeSetup = async () => {
      try {
        setIsInitializing(true);
        const response = await enableTwoFactorAuth();
        setQrCodeUri(response.qr_code_uri);

        // Extract secret key from QR code URI
        const urlParams = new URLSearchParams(
          response.qr_code_uri.split("?")[1],
        );
        const secret = urlParams.get("secret");
        if (secret) {
          setSecretKey(secret);
        }
      } catch (error) {
        setError(
          error instanceof Error ? error.message : t("initializationError"),
        );
      } finally {
        setIsInitializing(false);
      }
    };

    initializeSetup();
  }, []);

  const handleVerifyCode = async () => {
    if (!verificationCode || verificationCode.length !== 6) {
      setError(t("invalidCodeLength"));
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      // Save device trust preference before verification
      setDeviceTrustPreference(trustDevice);

      await verifyTwoFactorAuth(verificationCode, trustDevice);
      setStep("success");
    } catch (error) {
      setError(error instanceof Error ? error.message : t("verificationError"));
    } finally {
      setIsLoading(false);
    }
  };

  const copySecretKey = () => {
    if (secretKey) {
      navigator.clipboard.writeText(secretKey);
      toast({
        title: t("copied"),
        description: t("copiedDescription"),
      });
    }
  };

  const handleBackToProfile = () => {
    if (onComplete) {
      onComplete();
    } else {
      router.push("/profile");
    }
  };

  if (isInitializing) {
    return (
      <div className="container max-w-md mx-auto py-8">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              <Skeleton className="h-6 w-32" />
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-48 w-full" />
            <Skeleton className="h-10 w-full" />
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container max-w-md mx-auto py-8">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Shield className="h-5 w-5" />
            {t("title")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Step 1: QR Code */}
          {step === "qr" && (
            <>
              <div className="text-center space-y-4">
                <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
                  <QrCode className="h-4 w-4" />
                  {t("step1Title")}
                </div>

                <p className="text-sm">{t("step1Description")}</p>

                {qrCodeUri && (
                  <div className="flex justify-center p-4 bg-white rounded-lg">
                    <QRCode
                      value={qrCodeUri}
                      size={200}
                      role="img"
                      aria-label="QR Code for 2FA setup"
                    />
                  </div>
                )}

                {secretKey && (
                  <div className="space-y-2">
                    <Label className="text-sm font-medium">
                      {t("step1ManualEntry")}
                    </Label>
                    <div className="flex gap-2">
                      <Input
                        value={secretKey}
                        readOnly
                        className="font-mono text-xs"
                      />
                      <Button
                        type="button"
                        variant="outline"
                        size="icon"
                        onClick={copySecretKey}
                      >
                        <Copy className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                )}
              </div>

              <div className="flex gap-2">
                <Button
                  variant="outline"
                  onClick={handleBackToProfile}
                  className="flex-1"
                >
                  {t("cancel")}
                </Button>
                <Button
                  onClick={() => setStep("verify")}
                  className="flex-1"
                  disabled={!qrCodeUri}
                >
                  {t("step1Continue")}
                </Button>
              </div>
            </>
          )}

          {/* Step 2: Verify Code */}
          {step === "verify" && (
            <>
              <div className="text-center space-y-4">
                <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground">
                  <Shield className="h-4 w-4" />
                  {t("step2Title")}
                </div>

                <p className="text-sm">{t("step2Description")}</p>

                <div className="space-y-4">
                  <div>
                    <Label htmlFor="verification-code">{t("codeLabel")}</Label>
                    <Input
                      id="verification-code"
                      value={verificationCode}
                      onChange={(e) => {
                        const value = e.target.value
                          .replace(/\D/g, "")
                          .slice(0, 6);
                        setVerificationCode(value);
                        setError(null);
                      }}
                      placeholder={t("codePlaceholder")}
                      maxLength={6}
                      className="text-center text-lg tracking-widest"
                    />
                  </div>

                  {error && (
                    <Alert variant="destructive">
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}

                  {/* Trust Device Checkbox */}
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="trust-device-setup"
                      checked={trustDevice}
                      onCheckedChange={(checked) =>
                        setTrustDevice(Boolean(checked))
                      }
                    />
                    <Label
                      htmlFor="trust-device-setup"
                      className="text-sm font-normal cursor-pointer"
                    >
                      {tTwoFA("trustDevice")}
                    </Label>
                  </div>
                </div>
              </div>

              <div className="flex gap-2">
                <Button
                  variant="outline"
                  onClick={() => setStep("qr")}
                  className="flex-1"
                >
                  <ArrowLeft className="h-4 w-4 mr-2" />
                  {t("backButton")}
                </Button>
                <Button
                  onClick={handleVerifyCode}
                  disabled={isLoading || verificationCode.length !== 6}
                  className="flex-1"
                >
                  {isLoading ? t("verifying") : t("verifyButton")}
                </Button>
              </div>
            </>
          )}

          {/* Step 3: Success */}
          {step === "success" && (
            <>
              <div className="text-center space-y-4">
                <CheckCircle className="h-16 w-16 text-green-500 mx-auto" />
                <div>
                  <h3 className="text-lg font-semibold">{t("successTitle")}</h3>
                  <p className="text-sm text-muted-foreground">
                    {t("successDescription")}
                  </p>
                </div>
              </div>

              <Button onClick={handleBackToProfile} className="w-full">
                {t("backToProfile")}
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
