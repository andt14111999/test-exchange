import { useState, useEffect, useCallback } from "react";
import { useRouter } from "@/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { API_ENDPOINTS } from "@/lib/api/config";
import { useUserStore } from "@/lib/store/user-store";
import { apiClient } from "@/lib/api/client";

interface LoginResponse {
  token: string;
}

interface GoogleCredentialResponse {
  credential: string;
  select_by: string;
  g_csrf_token: string;
}

interface GoogleAccounts {
  accounts: {
    id: {
      initialize: (config: {
        client_id: string;
        callback: (response: GoogleCredentialResponse) => void;
      }) => void;
      renderButton: (
        element: HTMLElement | null,
        options: {
          theme?: "outline" | "filled";
          size?: "large" | "medium" | "small";
          text?: "sign_in_with" | "signup_with" | "continue_with";
          shape?: "rectangular" | "pill" | "circle" | "square";
          logo_alignment?: "left" | "center";
        },
      ) => void;
    };
  };
}

declare global {
  interface Window {
    google: GoogleAccounts;
    handleCredentialResponse: (response: GoogleCredentialResponse) => void;
  }
}

export function useGoogleAuth() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const { setUser } = useUserStore();

  const loginMutation = useMutation({
    mutationFn: async (idToken: string) => {
      const response = await apiClient.post<LoginResponse>(
        API_ENDPOINTS.auth.google,
        {
          id_token: idToken,
          account_type: "user",
        },
      );
      return response.data;
    },
    onSuccess: async (data) => {
      try {
        localStorage.setItem("token", data.token);
        try {
          const userResponse = await apiClient.get(API_ENDPOINTS.users.me);

          // Đảm bảo truyền đúng trường username vào userData
          const userData = {
            id: userResponse.data.id.toString(),
            email: userResponse.data.email,
            name: userResponse.data.display_name || userResponse.data.email,
            role: userResponse.data.role,
            avatar: userResponse.data.avatar_url,
            username: userResponse.data.username,
            status: userResponse.data.status,
            kycLevel: userResponse.data.kyc_level,
            phoneVerified: userResponse.data.phone_verified,
            documentVerified: userResponse.data.document_verified,
            authenticatorEnabled: userResponse.data.authenticator_enabled,
          };

          setUser(userData);

          queryClient.invalidateQueries({ queryKey: ["user"] });

          // Redirect to username update page if username is not set
          if (!userResponse.data.username) {
            setTimeout(() => {
              router.replace("/profile/update");
            }, 100); // Add small delay to ensure state is updated
          } else {
            router.replace("/");
          }
        } catch (fetchError) {
          console.error("Error fetching user data:", fetchError);
        }
      } catch (error) {
        console.error("Login error:", error);
        setError("Failed to login");
      }
    },
    onError: (error) => {
      console.error("Login error:", error);
      setError("Login failed");
    },
  });

  const handleCredentialResponse = useCallback(
    async (response: GoogleCredentialResponse) => {
      setError(null);
      loginMutation.mutate(response.credential);
    },
    [loginMutation],
  );

  useEffect(() => {
    window.handleCredentialResponse = handleCredentialResponse;

    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    document.head.appendChild(script);

    script.onload = () => {
      window.google.accounts.id.initialize({
        client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || "",
        callback: handleCredentialResponse,
      });

      window.google.accounts.id.renderButton(
        document.getElementById("googleSignInButton"),
        {
          theme: "outline",
          size: "large",
          text: "sign_in_with",
          shape: "rectangular",
          logo_alignment: "left",
        },
      );
    };

    return () => {
      window.handleCredentialResponse = () => {};
      const scriptElement = document.querySelector(
        `script[src="${script.src}"]`,
      );
      if (scriptElement) document.head.removeChild(scriptElement);
    };
  }, [handleCredentialResponse]);

  return { error, isPending: loginMutation.isPending };
}
