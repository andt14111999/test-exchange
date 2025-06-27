"use client";

import { useEffect } from "react";
import { useAMMStore } from "@/lib/store/amm-store";

export function ClientPoolInitializer() {
  const initializePools = useAMMStore((state) => state.initializePools);

  useEffect(() => {
    initializePools();
  }, [initializePools]);

  return null;
}
