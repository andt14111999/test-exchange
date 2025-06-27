import { render, screen } from "@testing-library/react";
import { Header } from "@/components/layout/header";
import { useUserStore } from "@/lib/store/user-store";

// Mocking dependencies
jest.mock("@/lib/store/user-store", () => ({
  useUserStore: jest.fn(),
}));

jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));

jest.mock("@/navigation", () => ({
  Link: ({ children, href, ...props }: any) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
  usePathname: () => "/",
}));
jest.mock("@/components/language-selector", () => ({
  LanguageSelector: () => <div>LanguageSelector</div>,
}));
jest.mock("@/components/notification-bell", () => ({
  NotificationBell: () => <div>NotificationBell</div>,
}));
jest.mock("@/components/user-menu", () => ({
  UserMenu: () => <div>UserMenu</div>,
}));

describe("Header", () => {
  beforeEach(() => {
    (useUserStore as unknown as jest.Mock).mockReturnValue({ user: null });
  });

  it("renders the logo", () => {
    render(<Header />);
    expect(screen.getByText("SnowFox")).toBeInTheDocument();
  });

  describe("for customers", () => {
    beforeEach(() => {
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: { id: 1, email: "customer@example.com", role: "customer" },
      });
    });

    it("renders customer navigation links", () => {
      render(<Header />);
      expect(screen.getByText("customer.tradingHistory")).toBeInTheDocument();
      expect(screen.getByText("common.wallet")).toBeInTheDocument();
      expect(screen.getByText("common.swap")).toBeInTheDocument();
      expect(screen.queryByText("merchant.mintFiat")).not.toBeInTheDocument();
    });
  });

  describe("for merchants", () => {
    beforeEach(() => {
      (useUserStore as unknown as jest.Mock).mockReturnValue({
        user: { id: 2, email: "merchant@example.com", role: "merchant" },
      });
    });

    it("renders merchant navigation links including Mint Fiat", () => {
      render(<Header />);
      expect(screen.getByText("merchant.createOffer")).toBeInTheDocument();
      expect(screen.getByText("merchant.manageOffers")).toBeInTheDocument();
      expect(screen.getByText("merchant.tradingHistory")).toBeInTheDocument();
      expect(screen.getByText("common.wallet")).toBeInTheDocument();
      expect(screen.getByText("common.swap")).toBeInTheDocument();
      expect(screen.getByText("merchant.mintFiat")).toBeInTheDocument();
    });
  });
});
