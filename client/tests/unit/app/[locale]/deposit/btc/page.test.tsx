import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DepositBTCPage from "@/app/[locale]/deposit/btc/page";
import { useTranslations } from "next-intl";

// Create a test query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

// Wrapper component with QueryClientProvider
const TestWrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock the fetchCoinSettings API
jest.mock("@/lib/api/coins", () => ({
  fetchCoinSettings: jest.fn(() =>
    Promise.resolve([
      {
        currency: "btc",
        layers: [
          {
            layer: "bitcoin",
            deposit_enabled: true,
          },
          {
            layer: "bep20",
            deposit_enabled: true,
          },
        ],
      },
    ]),
  ),
}));

interface Network {
  id: string;
  name: string;
}

// Mock CoinDeposit component
jest.mock("@/components/deposit/coin-deposit", () => ({
  CoinDeposit: jest.fn(({ coin, networks, title, description }) => (
    <div data-testid="coin-deposit">
      <div data-testid="coin">{coin}</div>
      <div data-testid="networks">
        {networks.map((network: Network) => (
          <div key={network.id} data-testid={`network-${network.id}`}>
            {network.name}
          </div>
        ))}
      </div>
      <div data-testid="title">{title}</div>
      <div data-testid="description">{description}</div>
    </div>
  )),
}));

describe("DepositBTCPage", () => {
  const mockTranslations: Record<string, string> = {
    "btc.title": "Bitcoin Deposit",
    "btc.description": "Deposit your Bitcoin",
  };

  beforeEach(() => {
    (useTranslations as jest.Mock).mockReturnValue(
      (key: string) => mockTranslations[key],
    );
  });

  it("renders the component with correct props", async () => {
    render(
      <TestWrapper>
        <DepositBTCPage />
      </TestWrapper>,
    );

    // Wait for loading to complete
    await waitFor(() => {
      expect(screen.getByTestId("coin-deposit")).toBeInTheDocument();
    });

    // Check if CoinDeposit component is rendered
    expect(screen.getByTestId("coin-deposit")).toBeInTheDocument();

    // Check coin prop
    expect(screen.getByTestId("coin")).toHaveTextContent("btc");

    // Check networks
    expect(screen.getByTestId("network-bitcoin")).toHaveTextContent(
      "Bitcoin (BTC)",
    );
    expect(screen.getByTestId("network-bep20")).toHaveTextContent(
      "BNB Smart Chain (BEP20)",
    );

    // Check translated content
    expect(screen.getByTestId("title")).toHaveTextContent("Bitcoin Deposit");
    expect(screen.getByTestId("description")).toHaveTextContent(
      "Deposit your Bitcoin",
    );
  });

  it("uses correct translation namespace", () => {
    render(
      <TestWrapper>
        <DepositBTCPage />
      </TestWrapper>,
    );
    expect(useTranslations).toHaveBeenCalledWith("deposit");
  });
});
