import { render, screen, waitFor } from "@testing-library/react";
import { Header } from "@/components/layout/header";
import { useTranslations } from "next-intl";
import { useUserStore } from "@/lib/store/user-store";
import { usePathname } from "@/navigation";
import type { ComponentProps } from "react";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock useUserStore
jest.mock("@/lib/store/user-store");

// Mock usePathname
jest.mock("@/navigation", () => ({
  usePathname: jest.fn(),
  Link: ({ href, children, ...props }: ComponentProps<"a">) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

// Mock NotificationBell component
jest.mock("@/components/notification-bell", () => ({
  NotificationBell: () => <div data-testid="notification-bell">Bell</div>,
}));

// Mock LanguageSelector component
jest.mock("@/components/language-selector", () => ({
  LanguageSelector: () => <div data-testid="language-selector">Language</div>,
}));

// Mock UserMenu component
jest.mock("@/components/user-menu", () => ({
  UserMenu: () => <div data-testid="user-menu">User Menu</div>,
}));

// Mock NavigationMenu components
jest.mock("@/components/layout/navigation-menu", () => ({
  NavigationMenu: ({ children }: { children: React.ReactNode }) => (
    <nav role="navigation">{children}</nav>
  ),
  NavigationMenuList: ({ children }: { children: React.ReactNode }) => (
    <ul>{children}</ul>
  ),
  NavigationMenuItem: ({ children }: { children: React.ReactNode }) => (
    <li>{children}</li>
  ),
  NavigationMenuLink: ({
    className,
    children,
    ...props
  }: ComponentProps<"a"> & { className?: string }) => (
    <a className={className} {...props}>
      {children}
    </a>
  ),
  navigationMenuTriggerStyle: () => "base-style",
}));

describe("Header", () => {
  const mockTranslations: Record<string, string> = {
    "merchant.createOffer": "Create Offer",
    "merchant.manageOffers": "Manage Offers",
    "merchant.tradingHistory": "Trading History",
    "common.wallet": "Wallet",
    "common.swap": "Swap",
    "common.liquidity": "Liquidity",
    "merchant.escrows.title": "Escrows",
    "customer.tradingHistory": "Trading History",
    "merchant.register.title": "Register as Merchant",
  };

  beforeEach(() => {
    // Reset all mocks
    jest.clearAllMocks();

    // Setup default mock implementations
    (useTranslations as jest.Mock).mockReturnValue(
      (key: string) => mockTranslations[key] || key,
    );
    (usePathname as jest.Mock).mockReturnValue("/");
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: null,
    });
  });

  it("renders basic header during SSR (not mounted)", async () => {
    render(<Header />);

    expect(screen.getByTestId("language-selector")).toBeInTheDocument();
    expect(screen.getByTestId("user-menu")).toBeInTheDocument();
    expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
  });

  it("renders basic header when user is not logged in", async () => {
    render(<Header />);

    // Wait for component to mount
    await waitFor(() => {
      expect(screen.getByTestId("language-selector")).toBeInTheDocument();
      expect(screen.getByTestId("user-menu")).toBeInTheDocument();
      expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
    });
  });

  it("renders merchant navigation when user is a merchant", async () => {
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: {
        id: "1",
        email: "test@test.com",
        name: "Test User",
        role: "merchant",
      },
    });

    render(<Header />);

    await waitFor(() => {
      expect(screen.getByText("Create Offer")).toBeInTheDocument();
      expect(screen.getByText("Manage Offers")).toBeInTheDocument();
      expect(screen.getByText("Trading History")).toBeInTheDocument();
      expect(screen.getByText("Wallet")).toBeInTheDocument();
      expect(screen.getByText("Swap")).toBeInTheDocument();
      expect(screen.getByText("Liquidity")).toBeInTheDocument();
      expect(screen.getByText("Escrows")).toBeInTheDocument();
    });
  });

  it("renders customer navigation when user is a customer", async () => {
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: {
        id: "1",
        email: "test@test.com",
        name: "Test User",
        role: "customer",
      },
    });

    render(<Header />);

    await waitFor(() => {
      expect(screen.getByText("Trading History")).toBeInTheDocument();
      expect(screen.getByText("Wallet")).toBeInTheDocument();
      expect(screen.getByText("Swap")).toBeInTheDocument();
      expect(screen.getByText("Liquidity")).toBeInTheDocument();
      expect(screen.getByText("Register as Merchant")).toBeInTheDocument();
    });
  });

  it("highlights current navigation item based on pathname", async () => {
    (usePathname as jest.Mock).mockReturnValue("/wallet");
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: {
        id: "1",
        email: "test@test.com",
        name: "Test User",
        role: "customer",
      },
    });

    render(<Header />);

    await waitFor(() => {
      const walletLink = screen.getByText("Wallet").closest("a");
      expect(walletLink?.className).toContain("bg-primary/10");
    });
  });

  it("renders notification bell when user is logged in", async () => {
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: {
        id: "1",
        email: "test@test.com",
        name: "Test User",
        role: "customer",
      },
    });

    render(<Header />);

    await waitFor(() => {
      expect(screen.getByTestId("notification-bell")).toBeInTheDocument();
    });
  });

  it("does not render notification bell when user is not logged in", async () => {
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: null,
    });

    render(<Header />);

    await waitFor(() => {
      expect(screen.queryByTestId("notification-bell")).not.toBeInTheDocument();
    });
  });
});
