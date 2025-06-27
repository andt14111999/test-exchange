import { render, screen } from "@testing-library/react";
import DepositUSDTPage from "@/app/[locale]/deposit/usdt/page";
import { useTranslations } from "next-intl";
import { useCoinNetworks } from "@/hooks/use-coin-networks";
import { mockNetworksByToken } from "@/lib/constants/mock-data";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock useCoinNetworks
jest.mock("@/hooks/use-coin-networks", () => ({
  useCoinNetworks: jest.fn(),
}));

// Mock CoinDeposit component
jest.mock("@/components/deposit/coin-deposit", () => ({
  CoinDeposit: jest.fn(({ coin, networks, title, description }) => (
    <div data-testid="coin-deposit">
      <div data-testid="coin">{coin}</div>
      <div data-testid="networks">{JSON.stringify(networks)}</div>
      <div data-testid="title">{title}</div>
      <div data-testid="description">{description}</div>
    </div>
  )),
}));

describe("DepositUSDTPage", () => {
  const mockTranslations = {
    "usdt.title": "USDT Deposit",
    "usdt.description": "Deposit USDT to your account",
  } as const;

  type TranslationKey = keyof typeof mockTranslations;

  beforeEach(() => {
    (useTranslations as jest.Mock).mockImplementation(
      () => (key: TranslationKey) => mockTranslations[key],
    );
    (useCoinNetworks as jest.Mock).mockReturnValue({
      networks: mockNetworksByToken.usdt,
      isLoading: false,
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders the CoinDeposit component with correct props", () => {
    render(<DepositUSDTPage />);

    // Check if CoinDeposit component is rendered
    const coinDeposit = screen.getByTestId("coin-deposit");
    expect(coinDeposit).toBeInTheDocument();

    // Check coin prop
    const coin = screen.getByTestId("coin");
    expect(coin).toHaveTextContent("usdt");

    // Check networks prop
    const networks = screen.getByTestId("networks");
    expect(JSON.parse(networks.textContent || "[]")).toEqual(
      mockNetworksByToken.usdt,
    );

    // Check title prop
    const title = screen.getByTestId("title");
    expect(title).toHaveTextContent(mockTranslations["usdt.title"]);

    // Check description prop
    const description = screen.getByTestId("description");
    expect(description).toHaveTextContent(mockTranslations["usdt.description"]);
  });

  it("uses the correct translation namespace", () => {
    render(<DepositUSDTPage />);
    expect(useTranslations).toHaveBeenCalledWith("deposit");
  });

  it("shows nothing while loading networks", () => {
    (useCoinNetworks as jest.Mock).mockReturnValue({
      networks: [],
      isLoading: true,
    });
    const { container } = render(<DepositUSDTPage />);
    expect(container).toBeEmptyDOMElement();
  });
});
