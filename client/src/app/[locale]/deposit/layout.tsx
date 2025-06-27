"use client";

import { ProtectedLayout } from "@/components/protected-layout";

export default function DepositLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ProtectedLayout>
      <div className="min-h-[calc(100vh-theme(spacing.14))] bg-gradient-to-b from-background to-muted mx-auto max-w-screen-lg">
        {children}
      </div>
    </ProtectedLayout>
  );
}
