"use client";

import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";

export function Header() {
  const router = useRouter();
  const t = useTranslations();

  return (
    <div className="flex items-center space-x-2 p-4 border-b">
      <button
        onClick={() => router.back()}
        className="text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft size={20} />
      </button>
      <h1 className="text-xl font-semibold">{t("liquidity.addLiquidity")}</h1>
    </div>
  );
}
