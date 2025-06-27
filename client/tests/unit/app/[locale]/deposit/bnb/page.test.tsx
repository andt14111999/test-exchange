import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import DepositBNBPage from "@/app/[locale]/deposit/bnb/page";
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
        currency: "bnb",
        layers: [
          {
            layer: "bep20",
            deposit_enabled: true,
          },
          {
            layer: "erc20",
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

describe("DepositBNBPage", () => {
  beforeEach(() => {
    // Mock translations
    (useTranslations as jest.Mock).mockReturnValue((key: string) => {
      const translations: Record<string, string> = {
        "bnb.title": "BNB Deposit",
        "bnb.description": "Deposit BNB to your account",
      };
      return translations[key] || key;
    });
  });

  it("renders the CoinDeposit component with correct props", async () => {
    render(
      <TestWrapper>
        <DepositBNBPage />
      </TestWrapper>,
    );

    // Wait for loading to complete
    await waitFor(() => {
      expect(screen.getByTestId("coin-deposit")).toBeInTheDocument();
    });

    // Check if CoinDeposit is rendered
    expect(screen.getByTestId("coin-deposit")).toBeInTheDocument();

    // Check coin prop
    expect(screen.getByTestId("coin")).toHaveTextContent("bnb");

    // Check networks
    expect(screen.getByTestId("network-bep20")).toHaveTextContent(
      "BNB Smart Chain (BEP20)",
    );
    expect(screen.getByTestId("network-erc20")).toHaveTextContent(
      "Ethereum (ERC20)",
    );

    // Check translated title and description
    expect(screen.getByTestId("title")).toHaveTextContent("BNB Deposit");
    expect(screen.getByTestId("description")).toHaveTextContent(
      "Deposit BNB to your account",
    );
  });

  it("uses correct translation namespace", () => {
    render(
      <TestWrapper>
        <DepositBNBPage />
      </TestWrapper>,
    );
    expect(useTranslations).toHaveBeenCalledWith("deposit");
  });
});
