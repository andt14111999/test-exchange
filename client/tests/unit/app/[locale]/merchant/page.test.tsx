import { render, screen } from "@testing-library/react";
import MerchantDashboard from "@/app/[locale]/merchant/page";
import { MOCK_OFFERS, MOCK_TRADES } from "@/lib/constants/mock-data";

// Mock next/link
jest.mock("next/link", () => {
  const MockLink = ({
    children,
    href,
  }: {
    children: React.ReactNode;
    href: string;
  }) => <a href={href}>{children}</a>;
  MockLink.displayName = "Link";
  return MockLink;
});

describe("MerchantDashboard", () => {
  it("renders the dashboard title", () => {
    render(<MerchantDashboard />);
    expect(screen.getByText("Merchant Dashboard")).toBeInTheDocument();
  });

  it("displays correct number of active offers", () => {
    render(<MerchantDashboard />);
    const activeOffersCount = MOCK_OFFERS.filter(
      (offer) => offer.isActive,
    ).length;
    expect(screen.getByText(activeOffersCount.toString())).toBeInTheDocument();
  });

  it("displays correct total transactions count", () => {
    render(<MerchantDashboard />);
    expect(screen.getByText(MOCK_TRADES.length.toString())).toBeInTheDocument();
  });

  it("displays correct total volume", () => {
    render(<MerchantDashboard />);
    const totalVolume = MOCK_TRADES.reduce(
      (acc, trade) => acc + trade.amount,
      0,
    );
    expect(screen.getByText(`${totalVolume} USDT`)).toBeInTheDocument();
  });

  it("renders active offers section with correct data", () => {
    render(<MerchantDashboard />);
    const activeOffers = MOCK_OFFERS.filter((offer) => offer.isActive);

    // Check section title (using the h3 element)
    expect(
      screen.getByRole("heading", { name: "Active Offers" }),
    ).toBeInTheDocument();

    // Check create offer button
    expect(screen.getByText("Create Offer")).toBeInTheDocument();
    expect(screen.getByText("Create Offer").closest("a")).toHaveAttribute(
      "href",
      "/merchant/create-offer",
    );

    // Check each offer
    activeOffers.forEach((offer) => {
      expect(
        screen.getByText(`${offer.type} ${offer.fiatCurrency}`),
      ).toBeInTheDocument();
      expect(
        screen.getAllByText(
          `Amount: ${offer.amount.toLocaleString()} ${offer.fiatCurrency}`,
        ),
      ).toHaveLength(
        MOCK_OFFERS.filter(
          (o) =>
            o.amount === offer.amount && o.fiatCurrency === offer.fiatCurrency,
        ).length,
      );
      expect(
        screen.getAllByText(
          `Limit: ${offer.minAmount.toLocaleString()} - ${offer.maxAmount.toLocaleString()} ${offer.fiatCurrency}`,
        ),
      ).toHaveLength(
        MOCK_OFFERS.filter(
          (o) =>
            o.minAmount === offer.minAmount &&
            o.maxAmount === offer.maxAmount &&
            o.fiatCurrency === offer.fiatCurrency,
        ).length,
      );
    });
  });

  it("renders recent transactions section with correct data", () => {
    render(<MerchantDashboard />);
    const recentTrades = MOCK_TRADES.slice(0, 5);

    // Check section title
    expect(
      screen.getByRole("heading", { name: "Recent Transactions" }),
    ).toBeInTheDocument();

    // Check view all button
    expect(screen.getByText("View All")).toBeInTheDocument();
    expect(screen.getByText("View All").closest("a")).toHaveAttribute(
      "href",
      "/merchant/transactions",
    );

    // Check each trade
    recentTrades.forEach((trade) => {
      expect(
        screen.getByText(
          `${trade.amount.toLocaleString()} ${trade.fiatCurrency}`,
        ),
      ).toBeInTheDocument();
      // Status is rendered in a span with specific classes
      const statusElements = screen.getAllByText(trade.status);
      const statusElement = statusElements.find(
        (el) => el.tagName.toLowerCase() === "span",
      );
      expect(statusElement).toBeTruthy();

      if (statusElement) {
        const statusClass = statusElement.className;

        switch (trade.status.toLowerCase()) {
          case "completed":
            expect(statusClass).toContain("bg-green-100");
            expect(statusClass).toContain("text-green-800");
            break;
          case "pending":
            expect(statusClass).toContain("bg-yellow-100");
            expect(statusClass).toContain("text-yellow-800");
            break;
          case "failed":
            expect(statusClass).toContain("bg-red-100");
            expect(statusClass).toContain("text-red-800");
            break;
          case "cancelled":
            expect(statusClass).toContain("bg-gray-100");
            expect(statusClass).toContain("text-gray-800");
            break;
        }
      }
    });
  });

  it("applies correct status colors", () => {
    render(<MerchantDashboard />);
    const recentTrades = MOCK_TRADES.slice(0, 5);

    recentTrades.forEach((trade) => {
      const statusElements = screen.getAllByText(trade.status);
      const statusElement = statusElements.find(
        (el) => el.tagName.toLowerCase() === "span",
      );
      expect(statusElement).toBeTruthy();

      if (statusElement) {
        const statusClass = statusElement.className;

        switch (trade.status.toLowerCase()) {
          case "completed":
            expect(statusClass).toContain("bg-green-100");
            expect(statusClass).toContain("text-green-800");
            break;
          case "pending":
            expect(statusClass).toContain("bg-yellow-100");
            expect(statusClass).toContain("text-yellow-800");
            break;
          case "failed":
            expect(statusClass).toContain("bg-red-100");
            expect(statusClass).toContain("text-red-800");
            break;
          case "cancelled":
            expect(statusClass).toContain("bg-gray-100");
            expect(statusClass).toContain("text-gray-800");
            break;
        }
      }
    });
  });

  it("applies correct type colors for offers", () => {
    render(<MerchantDashboard />);
    const activeOffers = MOCK_OFFERS.filter((offer) => offer.isActive);

    activeOffers.forEach((offer) => {
      const typeElement = screen.getByText(
        `${offer.type} ${offer.fiatCurrency}`,
      );
      const typeClass = typeElement.className;

      switch (offer.type.toLowerCase()) {
        case "buy":
          expect(typeClass).toContain("bg-green-100");
          expect(typeClass).toContain("text-green-800");
          break;
        case "sell":
          expect(typeClass).toContain("bg-red-100");
          expect(typeClass).toContain("text-red-800");
          break;
      }
    });
  });
});
