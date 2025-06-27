"use client";

import { useEffect } from "react";
import { useToast } from "./use-toast";
import { cn } from "@/lib/utils";

export function Toast() {
  const { toasts, dismissToast } = useToast();

  useEffect(() => {
    const timer = setTimeout(() => {
      dismissToast();
    }, 5000);

    return () => clearTimeout(timer);
  }, [toasts, dismissToast]);

  if (toasts.length === 0) return null;

  return (
    <div
      className="fixed top-4 right-4 z-50 flex flex-col gap-2"
      data-testid="toast-container"
    >
      {toasts.map((toast, index) => (
        <div
          key={index}
          data-testid={`toast-${index}`}
          className={cn(
            "rounded-md border p-4 shadow-lg",
            toast.variant === "destructive"
              ? "bg-red-50 text-red-800 border-red-200"
              : "bg-white text-gray-800 border-gray-200",
          )}
        >
          <div className="font-medium">{toast.title}</div>
          {toast.description && (
            <div
              className="text-sm mt-1"
              data-testid={`toast-description-${index}`}
            >
              {toast.description}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
