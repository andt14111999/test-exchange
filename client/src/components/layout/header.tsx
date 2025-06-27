"use client";

import { LanguageSelector } from "@/components/language-selector";
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  navigationMenuTriggerStyle,
} from "@/components/layout/navigation-menu";
import { NotificationBell } from "@/components/notification-bell";
import { UserMenu } from "@/components/user-menu";
import { useUserStore } from "@/lib/store/user-store";
import { cn } from "@/lib/utils";
import { Link, usePathname } from "@/navigation";
import { useTranslations } from "next-intl";
import { Menu } from "lucide-react";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";

export function Header() {
  const pathname = usePathname();
  const t = useTranslations();
  const { user } = useUserStore();
  const [mounted, setMounted] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const merchantLinks = [
    {
      title: t("merchant.createOffer"),
      href: "/merchant/create-offer",
    },
    {
      title: t("merchant.manageOffers"),
      href: "/merchant/manage-offers",
    },
    {
      title: t("merchant.tradingHistory"),
      href: "/transactions",
    },
    {
      title: t("common.wallet"),
      href: "/wallet",
    },
    {
      title: t("common.swap"),
      href: "/swap",
    },
    {
      title: t("common.liquidity"),
      href: "/liquidity/pools",
    },
    {
      title: t("merchant.escrows.title"),
      href: "/merchant/escrows",
    },
  ];

  const customerLinks = [
    {
      title: t("customer.tradingHistory"),
      href: "/transactions",
    },
    {
      title: t("common.wallet"),
      href: "/wallet",
    },
    {
      title: t("common.swap"),
      href: "/swap",
    },
    {
      title: t("common.liquidity"),
      href: "/liquidity/pools",
    },
    {
      title: t("merchant.register.title"),
      href: "/merchant/register",
    },
  ];

  const renderLogo = () => (
    <Link href="/" className="flex items-center">
      <div className="flex items-center gap-2">
        <div className="relative flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="h-4 w-4 text-primary"
          >
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
          </svg>
        </div>
        <span className="text-base font-semibold text-foreground/90">
          SnowFox
        </span>
      </div>
    </Link>
  );

  const links =
    user?.role?.toLowerCase() === "merchant"
      ? merchantLinks
      : customerLinks.filter(Boolean);

  const renderMobileMenu = () => {
    if (!user) return null;

    return (
      <>
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
        >
          <Menu className="h-5 w-5" />
          <span className="sr-only">Toggle menu</span>
        </Button>

        {mobileMenuOpen && (
          <div className="absolute left-0 top-14 z-50 w-full bg-background border-b border-border/40 md:hidden">
            <div className="container p-4">
              <nav className="flex flex-col space-y-2">
                {links.map((link) => (
                  <Link
                    key={link.href}
                    href={link.href}
                    className={cn(
                      "rounded-md px-3 py-2 text-sm font-medium transition-colors",
                      pathname === link.href
                        ? "bg-primary/10 text-primary"
                        : "text-foreground/70 hover:bg-accent hover:text-foreground",
                    )}
                    onClick={() => setMobileMenuOpen(false)}
                  >
                    {link.title}
                  </Link>
                ))}
              </nav>
            </div>
          </div>
        )}
      </>
    );
  };

  // Render basic header during SSR
  if (!mounted) {
    return (
      <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-14 items-center justify-between px-4">
          {renderLogo()}
          <div className="flex items-center gap-3">
            <LanguageSelector />
            <UserMenu />
          </div>
        </div>
      </header>
    );
  }

  // Nếu chưa login hoặc không có user, không hiển thị navigation
  if (!user) {
    return (
      <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 shadow-sm backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-14 items-center justify-between px-4">
          {renderLogo()}
          <div className="flex items-center gap-3">
            <LanguageSelector />
            <UserMenu />
          </div>
        </div>
      </header>
    );
  }

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 shadow-sm backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-14 items-center px-4">
        <div className="flex items-center gap-0 md:gap-4">
          {renderMobileMenu()}
          {renderLogo()}
        </div>

        <div className="hidden md:block pl-4 flex-1">
          <NavigationMenu className="max-w-none">
            <NavigationMenuList className="flex-wrap justify-start space-x-1 px-0">
              {links.map((link) => (
                <NavigationMenuItem key={link.href}>
                  <Link href={link.href} legacyBehavior passHref>
                    <NavigationMenuLink
                      className={cn(
                        navigationMenuTriggerStyle(),
                        "text-sm font-medium transition-all",
                        pathname === link.href
                          ? "bg-primary/10 text-primary"
                          : "text-foreground/70 hover:text-foreground",
                      )}
                    >
                      {link.title}
                    </NavigationMenuLink>
                  </Link>
                </NavigationMenuItem>
              ))}
            </NavigationMenuList>
          </NavigationMenu>
        </div>

        <div className="ml-auto flex items-center gap-2 md:gap-3">
          <NotificationBell />
          <div className="hidden h-6 w-px bg-border/60 md:block"></div>
          <LanguageSelector />
          <UserMenu />
        </div>
      </div>
    </header>
  );
}
