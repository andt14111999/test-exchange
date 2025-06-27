import { render, screen, fireEvent } from "@testing-library/react";
import PositionsSummary from "@/app/[locale]/liquidity/positions/components/PositionsSummary";
import { useTranslations } from "next-intl";
import { AmmPosition } from "@/lib/api/positions";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock format functions
jest.mock("@/lib/utils/format", () => ({
  formatCurrency: jest.fn((value) => `$${value}`),
  formatLiquidity: jest.fn((value) => `${value} LP`),
}));

describe("PositionsSummary", () => {
  const mockPositions: AmmPosition[] = [
    {
      id: 1,
      identifier: "pos-1",
      pool_pair: "USDT/VND",
      tick_lower_index: -100,
      tick_upper_index: 100,
      status: "open",
      error_message: null,
      liquidity: "100",
      amount0: "50",
      amount1: "50",
      amount0_initial: "50",
      amount1_initial: "50",
      slippage: "100",
      fee_growth_inside0_last: "0",
      fee_growth_inside1_last: "0",
      tokens_owed0: "10",
      tokens_owed1: "20",
      fee_collected0: "10",
      fee_collected1: "20",
      created_at: 1234567890,
      updated_at: 1234567890,
    },
    {
      id: 2,
      identifier: "pos-2",
      pool_pair: "USDT/VND",
      tick_lower_index: -200,
      tick_upper_index: 200,
      status: "open",
      error_message: null,
      liquidity: "200",
      amount0: "100",
      amount1: "100",
      amount0_initial: "100",
      amount1_initial: "100",
      slippage: "100",
      fee_growth_inside0_last: "0",
      fee_growth_inside1_last: "0",
      tokens_owed0: "30",
      tokens_owed1: "40",
      fee_collected0: "30",
      fee_collected1: "40",
      created_at: 1234567890,
      updated_at: 1234567890,
    },
  ];

  const mockOnClaimAll = jest.fn();

  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();

    // Mock translations
    (useTranslations as jest.Mock).mockReturnValue((key: string) => {
      const translations: Record<string, string> = {
        totalPositions: "Total Positions",
        claimableFees: "Claimable Fees",
        claimAll: "Claim All",
      };
      return translations[key] || key;
    });
  });

  it("renders correctly with positions", () => {
    render(
      <PositionsSummary
        positions={mockPositions}
        onClaimAll={mockOnClaimAll}
      />,
    );

    // Check if total positions is displayed correctly
    expect(screen.getByText("Total Positions")).toBeInTheDocument();
    expect(screen.getByText("300 LP")).toBeInTheDocument();

    // Check if claimable fees is displayed correctly
    expect(screen.getByText("Claimable Fees")).toBeInTheDocument();
    expect(screen.getByText("$100")).toBeInTheDocument();

    // Check if claim all button is enabled and clickable
    const claimButton = screen.getByText("Claim All");
    expect(claimButton).toBeInTheDocument();
    expect(claimButton).not.toBeDisabled();

    // Test button click
    fireEvent.click(claimButton);
    expect(mockOnClaimAll).toHaveBeenCalledTimes(1);
  });

  it("renders correctly with empty positions", () => {
    render(<PositionsSummary positions={[]} onClaimAll={mockOnClaimAll} />);

    // Check if total positions shows 0
    expect(screen.getByText("0 LP")).toBeInTheDocument();

    // Check if claimable fees shows 0
    expect(screen.getByText("$0")).toBeInTheDocument();

    // Check if claim all button is disabled
    const claimButton = screen.getByText("Claim All");
    expect(claimButton).toBeDisabled();
  });

  it("renders correctly with zero fees", () => {
    const positionsWithZeroFees: AmmPosition[] = [
      {
        id: 3,
        identifier: "pos-3",
        pool_pair: "USDT/VND",
        tick_lower_index: -100,
        tick_upper_index: 100,
        status: "open",
        error_message: null,
        liquidity: "100",
        amount0: "50",
        amount1: "50",
        amount0_initial: "50",
        amount1_initial: "50",
        slippage: "100",
        fee_growth_inside0_last: "0",
        fee_growth_inside1_last: "0",
        tokens_owed0: "0",
        tokens_owed1: "0",
        fee_collected0: "0",
        fee_collected1: "0",
        created_at: 1234567890,
        updated_at: 1234567890,
      },
    ];

    render(
      <PositionsSummary
        positions={positionsWithZeroFees}
        onClaimAll={mockOnClaimAll}
      />,
    );

    // Check if claim all button is disabled when fees are 0
    const claimButton = screen.getByText("Claim All");
    expect(claimButton).toBeDisabled();
  });
});
