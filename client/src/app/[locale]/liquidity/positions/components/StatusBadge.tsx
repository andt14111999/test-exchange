import { memo } from "react";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";

interface StatusBadgeProps {
  status: string;
}

const StatusBadge = memo(({ status }: StatusBadgeProps) => {
  const t = useTranslations("liquidity");

  switch (status) {
    case "open":
      return <Badge className="bg-green-500">{t("statusOpen")}</Badge>;
    case "pending":
      return <Badge className="bg-yellow-500">{t("statusPending")}</Badge>;
    case "closed":
      return <Badge className="bg-gray-500">{t("statusClosed")}</Badge>;
    case "error":
      return <Badge className="bg-red-500">{t("statusError")}</Badge>;
    default:
      return <Badge>{status}</Badge>;
  }
});

StatusBadge.displayName = "StatusBadge";

export default StatusBadge;
