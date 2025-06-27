"use client";
import { useTranslations } from "next-intl";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { useGoogleAuth } from "@/hooks/use-google-auth";
import { useUserStore } from "@/lib/store/user-store";
import { useEffect, useState } from "react";
import { useRouter } from "@/navigation";

export default function Login() {
  const t = useTranslations("login");
  const { error, isPending } = useGoogleAuth();
  const { user } = useUserStore();
  const router = useRouter();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);

    if (user) {
      router.push("/");
      return;
    }

    if (typeof window !== "undefined" && !user) {
      const token = localStorage.getItem("token");
      if (token) {
        localStorage.removeItem("token");
      }
    }
  }, [user, router]);

  if (!mounted) {
    return (
      <div className="flex min-h-[calc(100vh-theme(spacing.14))] items-center justify-center bg-gradient-to-b from-background to-muted/50 p-4">
        <div className="w-full max-w-md p-8 text-center">
          <p>Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-[calc(100vh-theme(spacing.14))] items-center justify-center bg-gradient-to-b from-background to-muted/50 p-4">
      <Card className="card w-full max-w-md border-t-4 border-primary shadow-lg">
        <CardHeader className="card-header space-y-1">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="h-6 w-6 text-primary"
            >
              <path d="M2 20a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8l-10-6L2 8v12z" />
              <path d="M12 10a2 2 0 0 0-2 2c0 1.1.9 2 2 2 1.1 0 2-.9 2-2a2 2 0 0 0-2-2z" />
            </svg>
          </div>
          <CardTitle className="card-title text-center text-2xl font-bold">
            {t("title")}
          </CardTitle>
          <CardDescription className="card-description text-center">
            {t("description", {
              fallback: "Sign in to your account to continue",
            })}
          </CardDescription>
        </CardHeader>
        <CardContent className="card-content grid gap-6">
          {error && (
            <div className="rounded-md bg-destructive/10 p-3 text-destructive">
              <div className="flex items-center gap-2">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="h-4 w-4"
                >
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{error}</span>
              </div>
            </div>
          )}

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t"></span>
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-background px-2 text-muted-foreground">
                {t("continueWith", { fallback: "Continue with" })}
              </span>
            </div>
          </div>

          <div className="flex justify-center">
            <div
              id="googleSignInButton"
              data-testid="googleSignInButton"
              className={isPending ? "opacity-50 pointer-events-none" : ""}
            />
          </div>

          <p className="mt-2 text-center text-xs text-muted-foreground">
            {t("termsAndPrivacy", {
              fallback:
                "By signing in, you agree to our Terms of Service and Privacy Policy",
            })}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
