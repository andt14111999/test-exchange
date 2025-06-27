import { render, screen, fireEvent } from "@testing-library/react";
import { useTranslations } from "next-intl";
import PositionsList from "@/app/[locale]/liquidity/positions/components/PositionsList";
import { AmmPosition } from "@/lib/api/positions";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

describe("PositionsList", () => {
  const mockT = (key: string) => key;
  const mockPosition: AmmPosition = {
    id: 1,
    identifier: "ETH-USDT-1",
    pool_pair: "ETH/USDT",
    tick_lower_index: -100,
    tick_upper_index: 100,
    status: "open",
    error_message: null,
    liquidity: "1000",
    amount0: "1.0",
    amount1: "2000.0",
    amount0_initial: "1.0",
    amount1_initial: "2000.0",
    slippage: "0.5",
    fee_growth_inside0_last: "0",
    fee_growth_inside1_last: "0",
    tokens_owed0: "1",
    tokens_owed1: "0",
    fee_collected0: "0",
    fee_collected1: "0",
    // New fields
    apr: "3.51",
    estimate_fee_token0: "0.2",
    estimate_fee_token1: "40",
    total_estimate_fee_in_token0: "2.5",
    created_at: Date.now(),
    updated_at: Date.now(),
  };

  const mockOnClaim = jest.fn();
  const mockOnClose = jest.fn();
  const mockOnViewDetail = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useTranslations as jest.Mock).mockReturnValue(mockT);
  });

  it("renders empty state when no positions", () => {
    render(
      <PositionsList
        positions={[]}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );
    expect(screen.getByText("noPositionsFound")).toBeInTheDocument();
  });

  it("renders positions list with header and items", () => {
    render(
      <PositionsList
        positions={[mockPosition]}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    // Check header elements (at least one occurrence)
    expect(screen.getAllByText("pair").length).toBeGreaterThan(0);
    expect(screen.getAllByText("range").length).toBeGreaterThan(0);
    expect(screen.getAllByText("token0").length).toBeGreaterThan(0);
    expect(screen.getAllByText("token1").length).toBeGreaterThan(0);
    expect(screen.getAllByText("status.title").length).toBeGreaterThan(0);
    expect(screen.getAllByText("actions").length).toBeGreaterThan(0);

    // Check position item is rendered
    expect(screen.getAllByText(mockPosition.pool_pair).length).toBeGreaterThan(
      0,
    );
  });

  it("renders multiple positions", () => {
    const positions = [
      mockPosition,
      {
        ...mockPosition,
        id: 2,
        identifier: "ETH-USDT-2",
      },
    ];

    render(
      <PositionsList
        positions={positions}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    // There will be multiple occurrences due to mobile/desktop, just check >= 2
    expect(
      screen.getAllByText(mockPosition.pool_pair).length,
    ).toBeGreaterThanOrEqual(2);
  });

  it("calls onClaim when claim button is clicked", () => {
    render(
      <PositionsList
        positions={[mockPosition]}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    const claimButtons = screen.getAllByRole("button", { name: /getFee/i });
    fireEvent.click(claimButtons[0]); // Click the first claim button
    expect(mockOnClaim).toHaveBeenCalledWith(mockPosition.id);
  });

  it("calls onClose when close button is clicked and confirmed", () => {
    render(
      <PositionsList
        positions={[mockPosition]}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    const closeButtons = screen.getAllByRole("button", { name: /close/i });
    fireEvent.click(closeButtons[0]); // Open dialog
    // Confirm in dialog (first 'close' button is confirm)
    const confirmButtons = screen.getAllByRole("button", { name: /^close$/i });
    fireEvent.click(confirmButtons[0]);
    expect(mockOnClose).toHaveBeenCalledWith(mockPosition);
  });

  it("calls onViewDetail when position is clicked", () => {
    render(
      <PositionsList
        positions={[mockPosition]}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    // Find all pool_pair elements, click the first one
    const positionItems = screen.getAllByText(mockPosition.pool_pair);
    fireEvent.click(positionItems[0]);
    expect(mockOnViewDetail).toHaveBeenCalledWith(mockPosition);
  });

  it("renders header container", () => {
    render(
      <PositionsList
        positions={[mockPosition]}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );
    // Just check header exists
    expect(screen.getAllByText("pair").length).toBeGreaterThan(0);
  });

  it("renders list container with correct spacing", () => {
    render(
      <PositionsList
        positions={[mockPosition]}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );
    // The outermost container has class space-y-4, check by class
    const containers = document.querySelectorAll(".space-y-4");
    expect(containers.length).toBeGreaterThan(0);
  });
});
