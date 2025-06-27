import { useState, useCallback } from "react";

type ToastVariant = "default" | "destructive";

export interface ToastProps {
  title: string;
  description?: string;
  variant?: ToastVariant;
}

interface UseToastReturn {
  toast: (props: ToastProps) => void;
  dismissToast: () => void;
  toasts: ToastProps[];
}

export function useToast(): UseToastReturn {
  const [toasts, setToasts] = useState<ToastProps[]>([]);

  const toast = useCallback((props: ToastProps) => {
    const newToast = {
      ...props,
      variant: props.variant || "default",
    };

    setToasts((prev) => [...prev, newToast]);

    // Automatically dismiss after 5 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t !== newToast));
    }, 5000);

    // In a real implementation, this would have more features like custom durations,
    // toast IDs, and more control over the toast lifecycle
  }, []);

  const dismissToast = useCallback(() => {
    setToasts([]);
  }, []);

  return { toast, dismissToast, toasts };
}
