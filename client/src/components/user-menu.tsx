"use client";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "@/navigation";
import { LogIn, User } from "lucide-react";
import { useTranslations } from "next-intl";

export function UserMenu() {
  const t = useTranslations();
  const router = useRouter();
  const { user, logout } = useUserStore();

  const handleLogout = () => {
    localStorage.removeItem("token");
    logout();
    router.push("/login");
  };

  if (!user) {
    return (
      <Button
        variant="ghost"
        size="icon"
        className="h-10 w-10"
        onClick={() => router.push("/login")}
      >
        <LogIn className="h-5 w-5" data-testid="login-icon" />
      </Button>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="h-10 w-10">
          <User className="h-5 w-5" data-testid="user-icon" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={() => router.push("/profile")}>
          {t("common.profile")}
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={handleLogout}>
          {t("common.logout")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
