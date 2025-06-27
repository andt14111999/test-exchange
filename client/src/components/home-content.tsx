"use client";

import { useTranslations } from "next-intl";
import { useUserStore } from "@/lib/store/user-store";
import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/use-auth";

export function HomeContent() {
  const t = useTranslations("home");
  const { user } = useUserStore();
  const { isLoading } = useAuth();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted || isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)]">
        <h1 className="text-4xl font-bold mb-4">{t("title")}</h1>
        <p className="text-lg text-muted-foreground">{t("welcome")}</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)]">
      <h1 className="text-4xl font-bold mb-4">{t("title")}</h1>
      <p className="text-lg text-muted-foreground">
        {user ? `Welcome back, ${user.name || "User"}!` : t("welcome")}
      </p>
      {user && <p className="text-sm mt-2">Your role: {user.role}</p>}
    </div>
  );
}
