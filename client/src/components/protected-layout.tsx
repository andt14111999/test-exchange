"use client";

import { Skeleton } from "@/components/ui/skeleton";
import { useProtectedRoute } from "@/hooks/use-protected-route";
import { useEffect, useState } from "react";

interface ProtectedLayoutProps {
  children: React.ReactNode;
  loadingFallback?: React.ReactNode;
}

export function ProtectedLayout({
  children,
  loadingFallback,
}: ProtectedLayoutProps) {
  const { isLoading } = useProtectedRoute();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Show loading state when checking authentication or not mounted
  if (!mounted || isLoading) {
    return (
      loadingFallback || (
        <div className="container py-8" data-testid="skeleton-container">
          <div className="space-y-4">
            <Skeleton className="h-8 w-48" />
            <div className="text-sm text-muted-foreground">
              <Skeleton className="h-4 w-96" />
            </div>
            <div className="space-y-2">
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
              <Skeleton className="h-12 w-full" />
            </div>
          </div>
        </div>
      )
    );
  }

  // Only render children when authenticated
  return <>{children}</>;
}
