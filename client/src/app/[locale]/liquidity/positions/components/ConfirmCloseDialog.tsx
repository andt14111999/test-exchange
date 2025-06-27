import { memo } from "react";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { AmmPosition } from "@/lib/api/positions";

interface ConfirmCloseDialogProps {
  isOpen: boolean;
  position: AmmPosition | null;
  onClose: () => void;
  onConfirm: () => void;
}

const ConfirmCloseDialog = memo(
  ({ isOpen, position, onClose, onConfirm }: ConfirmCloseDialogProps) => {
    const t = useTranslations("liquidity");

    return (
      <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("confirmClose")}</DialogTitle>
            <DialogDescription>
              {position &&
                t("confirmCloseDescription", { pool: position.pool_pair })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="flex gap-2 sm:justify-end">
            <Button variant="outline" onClick={onClose}>
              {t("cancel")}
            </Button>
            <Button onClick={onConfirm}>{t("close")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  },
);

ConfirmCloseDialog.displayName = "ConfirmCloseDialog";

export default ConfirmCloseDialog;
