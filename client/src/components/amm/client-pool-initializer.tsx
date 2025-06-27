"use client";

import dynamic from "next/dynamic";
import { Suspense } from "react";

const PoolInitializer = dynamic(
  () =>
    import("@/components/amm/pool-initializer").then(
      (mod) => mod.PoolInitializer,
    ),
  { ssr: false },
);

export function ClientPoolInitializer() {
  return (
    <Suspense fallback={null}>
      <PoolInitializer />
    </Suspense>
  );
}
