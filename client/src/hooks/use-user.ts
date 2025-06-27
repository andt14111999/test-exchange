import { useQuery } from "@tanstack/react-query";
import { fetchUserData } from "@/lib/api/user";
import { useEffect, useState } from "react";

export function useUser() {
  const [hasToken, setHasToken] = useState(false);

  // Check for token after component mounts and when localStorage changes
  useEffect(() => {
    const checkToken = () => {
      if (typeof window !== "undefined") {
        const token = localStorage.getItem("token");
        setHasToken(!!token);
      }
    };

    // Initial check
    checkToken();

    // Listen for storage events
    window.addEventListener("storage", checkToken);

    return () => {
      window.removeEventListener("storage", checkToken);
    };
  }, []);

  const query = useQuery({
    queryKey: ["user"],
    queryFn: async () => {
      try {
        return await fetchUserData();
      } catch (error) {
        // Ensure error is always an Error instance with consistent message
        if (error instanceof Error) {
          throw new Error(error.message);
        }
        throw new Error("Failed to fetch user data");
      }
    },
    enabled: hasToken, // Only run query if token exists
    retry: false, // Don't retry on error for testing purposes
    staleTime: 1000 * 60 * 5, // Consider data fresh for 5 minutes
  });

  return query;
}
