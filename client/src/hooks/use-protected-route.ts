import { useEffect, useState } from "react";
import { useRouter } from "@/navigation";
import { useUserStore } from "@/lib/store/user-store";
import { useAuth } from "./use-auth";

export function useProtectedRoute() {
  const router = useRouter();
  const { user } = useUserStore();
  const { isLoading } = useAuth();
  const [mounted, setMounted] = useState(false);

  // Handle authentication check
  useEffect(() => {
    // Skip during SSR
    if (typeof window === "undefined") return;

    setMounted(true);

    // If no user and not loading, redirect to login
    if (!isLoading && !user && !window.location.pathname.includes("/login")) {
      console.log("No user found, redirecting to login");
      router.push("/login");
    }
  }, [user, isLoading, router]);

  return {
    isAuthenticated: !!user,
    isLoading: isLoading || !mounted,
    user,
  };
}
