import { render, screen } from "@testing-library/react";
import DepositETHPage from "@/app/[locale]/deposit/eth/page";
import { useTranslations } from "next-intl";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
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

describe("DepositETHPage", () => {
  const mockTranslations: Record<string, string> = {
    "eth.title": "Deposit ETH",
    "eth.description": "Deposit ETH to your account",
  };

  beforeEach(() => {
    (useTranslations as jest.Mock).mockReturnValue(
      (key: string) => mockTranslations[key],
    );
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("renders the component correctly", () => {
    render(<DepositETHPage />);

    // Check if CoinDeposit component is rendered
    expect(screen.getByTestId("coin-deposit")).toBeInTheDocument();

    // Verify coin prop
    expect(screen.getByTestId("coin")).toHaveTextContent("eth");

    // Verify networks prop
    const networks = JSON.parse(
      screen.getByTestId("networks").textContent || "[]",
    );
    expect(networks).toEqual([
      { id: "erc20", name: "Ethereum (ERC20)", enabled: true },
    ]);

    // Verify translations
    expect(screen.getByTestId("title")).toHaveTextContent("Deposit ETH");
    expect(screen.getByTestId("description")).toHaveTextContent(
      "Deposit ETH to your account",
    );
  });

  it("uses correct translation namespace", () => {
    render(<DepositETHPage />);
    expect(useTranslations).toHaveBeenCalledWith("deposit");
  });
});
