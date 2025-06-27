"use client";

import { ReactNode } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ProtectedLayout } from "@/components/protected-layout";

interface FiatTransactionLayoutProps {
  title: string;
  children: ReactNode;
  loadingFallback?: ReactNode;
}

export function FiatTransactionLayout({
  title,
  children,
  loadingFallback,
}: FiatTransactionLayoutProps) {
  return (
    <ProtectedLayout loadingFallback={loadingFallback}>
      <div className="container max-w-lg py-6 mx-auto">
        <Card>
          <CardHeader>
            <CardTitle className="text-2xl font-bold text-center">
              {title}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">{children}</CardContent>
        </Card>
      </div>
    </ProtectedLayout>
  );
}
