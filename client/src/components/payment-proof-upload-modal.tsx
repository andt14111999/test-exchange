"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/components/ui/use-toast";
import { useTranslations } from "next-intl";
import { useState } from "react";

interface PaymentProofUploadModalProps {
  onClose: () => void;
  onSuccess?: (paymentProof: { file: File; description: string }) => void;
}

export function PaymentProofUploadModal({
  onClose,
  onSuccess,
}: PaymentProofUploadModalProps) {
  const t = useTranslations("merchant.transactions");
  const { toast } = useToast();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [description, setDescription] = useState("");
  const [isUploading, setIsUploading] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];

      // Validate file size (10MB max)
      const maxSize = 10 * 1024 * 1024;
      if (file.size > maxSize) {
        toast({
          title: t("errorFileTooLarge"),
          description: `File size must be less than ${maxSize / (1024 * 1024)}MB`,
          variant: "destructive",
        });
        setSelectedFile(null);
        return;
      }

      // Validate file type
      const allowedTypes = [
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp",
      ];
      if (!allowedTypes.includes(file.type)) {
        toast({
          title: t("errorInvalidFileType"),
          description: `Please select an image file (${allowedTypes.join(", ")})`,
          variant: "destructive",
        });
        setSelectedFile(null);
        return;
      }

      setSelectedFile(file);
    } else {
      setSelectedFile(null);
    }
  };

  const handleSubmit = async () => {
    try {
      if (!selectedFile) {
        toast({
          title: t("errorMissingPaymentProof"),
          description: t("pleaseUploadPaymentProof"),
          variant: "destructive",
        });
        return;
      }

      setIsUploading(true);

      // Call onSuccess with payment proof
      await onSuccess?.({
        file: selectedFile,
        description,
      });
      onClose();
    } catch (error) {
      let errorMessage = t("errorUploadingProof");
      if (error instanceof Error) {
        if (error.message.includes("Backend error:")) {
          errorMessage = error.message;
        } else if (error.message.includes("File upload failed")) {
          errorMessage =
            "File upload failed. Please try a different image or contact support.";
        }
      }

      toast({
        title: errorMessage,
        variant: "destructive",
      });
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <Dialog open={true} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t("uploadPaymentProof")}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="grid w-full items-center gap-1.5">
            <Label htmlFor="payment-proof">{t("paymentReceipt")}</Label>
            <Input
              id="payment-proof"
              type="file"
              accept="image/*"
              onChange={handleFileChange}
            />
            <p className="text-sm text-muted-foreground">
              {t("uploadPaymentProofHint")}
            </p>
          </div>

          <div className="grid w-full items-center gap-1.5">
            <Label htmlFor="payment-description">{t("additionalInfo")}</Label>
            <Textarea
              id="payment-description"
              placeholder={t("enterAdditionalInfo")}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
            />
          </div>
        </div>

        <DialogFooter className="sm:justify-end">
          <DialogClose asChild>
            <Button variant="outline" disabled={isUploading}>
              {t("cancel")}
            </Button>
          </DialogClose>
          <Button onClick={handleSubmit} disabled={isUploading}>
            {isUploading ? t("submitting") : t("confirmAndSendMoney")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
