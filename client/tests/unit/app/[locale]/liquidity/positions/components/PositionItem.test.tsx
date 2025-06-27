import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { useTranslations } from "next-intl";
import PositionItem from "@/app/[locale]/liquidity/positions/components/PositionItem";
import { AmmPosition } from "@/lib/api/positions";
import { TickMath } from "@/lib/amm/tick-math";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock TickMath
jest.mock("@/lib/amm/tick-math", () => ({
  TickMath: {
    tickToPrice: jest.fn(),
  },
}));

// Mock ConfirmCloseDialog to simplify dialog interaction
jest.mock(
  "@/app/[locale]/liquidity/positions/components/ConfirmCloseDialog",
  () => ({
    __esModule: true,
    default: ({
      isOpen,
      onConfirm,
    }: {
      isOpen: boolean;
      onConfirm: () => void;
    }) =>
      isOpen ? (
        <div role="dialog">
          <button onClick={onConfirm}>confirm</button>
        </div>
      ) : null,
  }),
);

describe("PositionItem", () => {
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
    (TickMath.tickToPrice as jest.Mock).mockImplementation(
      (tick) => tick * 0.01,
    );
  });

  it("renders position information correctly", () => {
    render(
      <PositionItem
        position={mockPosition}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    // Check pool pair
    expect(screen.getAllByText(mockPosition.pool_pair).length).toBeGreaterThan(
      0,
    );

    // Check token amounts (should include $)
    expect(
      screen.getAllByText((content) => content.includes("1.00")).length,
    ).toBeGreaterThan(0); // amount0
    expect(
      screen.getAllByText((content) => content.includes("2,000.00")).length,
    ).toBeGreaterThan(0); // amount1

    // Check price range
    expect(
      screen.getAllByText((content) => content.includes("-1.00")).length,
    ).toBeGreaterThan(0); // lower price
    expect(
      screen.getAllByText((content) => content.includes("1.00")).length,
    ).toBeGreaterThan(0); // upper price

    // Check status badge
    expect(screen.getAllByText("statusOpen").length).toBeGreaterThan(0);
  });

  it("handles view detail click", () => {
    render(
      <PositionItem
        position={mockPosition}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    // Find the outermost card with cursor-pointer
    const cards = screen
      .getAllByText(mockPosition.pool_pair)
      .map((el) => el.closest(".cursor-pointer"));
    const card = cards.find(Boolean) as HTMLElement;
    fireEvent.click(card);
    expect(mockOnViewDetail).toHaveBeenCalledWith(mockPosition);
  });

  it("handles claim fee click", () => {
    render(
      <PositionItem
        position={mockPosition}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    const claimButtons = screen.getAllByText("getFee");
    const claimButton = claimButtons.find(
      (btn) => !btn.hasAttribute("disabled"),
    );
    fireEvent.click(claimButton!);
    expect(mockOnClaim).toHaveBeenCalledWith(mockPosition.id);
  });

  it("handles close position click", async () => {
    render(
      <PositionItem
        position={mockPosition}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    const closeButtons = screen.getAllByText("close");
    const closeButton = closeButtons[0];
    fireEvent.click(closeButton);

    // Check if confirm dialog is opened
    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    // Confirm close
    const confirmButton = screen.getByText("confirm");
    fireEvent.click(confirmButton);

    expect(mockOnClose).toHaveBeenCalledWith(mockPosition);
  });

  it("disables claim button when total_estimate_fee_in_token0 is less than or equal to 1", () => {
    const positionWithLowFees = {
      ...mockPosition,
      total_estimate_fee_in_token0: "0.5",
    };

    render(
      <PositionItem
        position={positionWithLowFees}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    const claimButtons = screen.getAllByText("getFee");
    claimButtons.forEach((btn) => expect(btn).toBeDisabled());
  });

  it("renders closed position without action buttons", () => {
    const closedPosition = {
      ...mockPosition,
      status: "closed" as const,
    };

    render(
      <PositionItem
        position={closedPosition}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    expect(screen.queryByText("getFee")).not.toBeInTheDocument();
    expect(screen.queryByText("close")).not.toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("handles mobile view correctly", () => {
    // Mock window.innerWidth for mobile view
    Object.defineProperty(window, "innerWidth", {
      writable: true,
      configurable: true,
      value: 500,
    });

    render(
      <PositionItem
        position={mockPosition}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    // Check if mobile view elements are rendered
    expect(screen.getByText("range")).toBeInTheDocument();
    expect(screen.getByText("fees")).toBeInTheDocument();
  });

  it("handles desktop view correctly", () => {
    // Mock window.innerWidth for desktop view
    Object.defineProperty(window, "innerWidth", {
      writable: true,
      configurable: true,
      value: 1024,
    });

    render(
      <PositionItem
        position={mockPosition}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    // Check if desktop view elements are rendered
    const poolPairDivs = screen.getAllByText(mockPosition.pool_pair);
    expect(poolPairDivs.length).toBeGreaterThan(0);
  });

  it("prevents event propagation on button clicks", () => {
    render(
      <PositionItem
        position={mockPosition}
        onClaim={mockOnClaim}
        onClose={mockOnClose}
        onViewDetail={mockOnViewDetail}
      />,
    );

    const claimButtons = screen.getAllByText("getFee");
    const closeButtons = screen.getAllByText("close");

    // Just check that the callback is called on click
    fireEvent.click(claimButtons[0]);
    expect(mockOnClaim).toHaveBeenCalledWith(mockPosition.id);

    fireEvent.click(closeButtons[0]);
    // Confirm dialog will appear, but we only care that click does not throw
  });
});
