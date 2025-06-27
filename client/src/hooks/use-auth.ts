import { useEffect } from "react";
import { useRouter } from "@/navigation";
import { useUserStore } from "@/lib/store/user-store";
import { fetchUserData } from "@/lib/api/user";
import { useState } from "react";
import { UserData } from "@/types/user";

export function useAuth() {
  const router = useRouter();
  const { setUser } = useUserStore();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [userData, setUserData] = useState<UserData | null>(null);

  // Handle unauthorized error
  useEffect(() => {
    if (!isLoading && error && error.message === "Unauthorized") {
      console.log("useAuth: Unauthorized error detected");
      localStorage.removeItem("token");
      // Only redirect if not already on login page
      if (!window.location.pathname.includes("/login")) {
        console.log("useAuth: Redirecting to login");
        router.push("/login");
      }
    }
  }, [isLoading, error, router]);

  // Check for token and try to load user on mount
  useEffect(() => {
    let mounted = true;

    const checkToken = async () => {
      try {
        if (typeof window === "undefined") return;

        const token = localStorage.getItem("token");
        if (!token) {
          if (mounted) setIsLoading(false);
          return;
        }

        const userData = await fetchUserData();
        if (!mounted) return;

        setUserData(userData);

        setUser({
          id: userData.id.toString(),
          email: userData.email,
          name: userData.display_name || userData.email,
          role: userData.role,
          avatar: userData.avatar_url,
          username: userData.username,
          status: userData.status,
          kycLevel: userData.kyc_level,
          phoneVerified: userData.phone_verified,
          documentVerified: userData.document_verified,
          authenticatorEnabled: userData.authenticator_enabled,
        });
      } catch (error) {
        if (!mounted) return;
        setError(
          error instanceof Error
            ? error
            : new Error("Failed to load user data"),
        );
      } finally {
        if (mounted) setIsLoading(false);
      }
    };

    checkToken();

    return () => {
      mounted = false;
    };
  }, [setUser, router]); // Only run on mount

  return {
    isAuthenticated: !!userData,
    isLoading,
    user: userData,
  };
}
