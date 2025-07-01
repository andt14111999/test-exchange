import { render, screen, fireEvent } from "@testing-library/react";
import PositionDetail from "@/app/[locale]/liquidity/positions/components/PositionDetail";
import { AmmPosition } from "@/lib/api/positions";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));

// Mock formatCurrency
jest.mock("@/lib/utils/format", () => ({
  formatCurrency: jest.fn((value) => value.toString()),
}));

// Mock formatDisplayPrice
jest.mock("@/lib/amm/position-utils", () => ({
  formatDisplayPrice: jest.fn((value) => value.toString()),
}));

// Mock TickMath
jest.mock("@/lib/amm/tick-math", () => ({
  TickMath: {
    tickToPrice: jest.fn((tick) => tick * 1.0001),
  },
}));

// Mock ConfirmCloseDialog
jest.mock(
  "@/app/[locale]/liquidity/positions/components/ConfirmCloseDialog",
  () => {
    return function ConfirmCloseDialog({
      isOpen,
      onConfirm,
      onClose,
    }: {
      isOpen: boolean;
      onConfirm: () => void;
      onClose: () => void;
    }) {
      if (!isOpen) return null;
      return (
        <div data-testid="confirm-close-dialog">
          <button onClick={onConfirm}>Confirm Close</button>
          <button onClick={onClose}>Cancel</button>
        </div>
      );
    };
  },
);

describe("PositionDetail", () => {
  const mockPosition: AmmPosition = {
    id: 1,
    identifier: "AP1",
    pool_pair: "ETH/USDT",
    status: "open",
    created_at: 1646092800, // March 1, 2022
    updated_at: 1646092800,
    amount0_initial: "1.5",
    amount1_initial: "3000",
    amount0: "1.2",
    amount1: "2400",
    fee_collected0: "0.1",
    fee_collected1: "20",
    tokens_owed0: "0.05",
    tokens_owed1: "10",
    tick_lower_index: 1000,
    tick_upper_index: 2000,
    error_message: null,
    liquidity: "1000000",
    slippage: "0.5",
    fee_growth_inside0_last: "0",
    fee_growth_inside1_last: "0",
    // New fields
    apr: "3.51",
    estimate_fee_token0: "0.2",
    estimate_fee_token1: "40",
    total_estimate_fee_in_token0: "2.5",
  };

  const defaultProps = {
    position: mockPosition,
    isOpen: true,
    onClose: jest.fn(),
    onClaim: jest.fn(),
    onClosePosition: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders nothing when position is null", () => {
    const { container } = render(
      <PositionDetail {...defaultProps} position={null} />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("renders position details correctly", () => {
    render(<PositionDetail {...defaultProps} />);

    // Check title
    expect(screen.getByText("AP1")).toBeInTheDocument();

    // Check pair
    expect(screen.getByText("ETH/USDT")).toBeInTheDocument();

    // Check status badge
    expect(screen.getByText("status.active")).toBeInTheDocument();

    // Check initial amounts
    expect(screen.getByText("1.5 ETH - 3000 USDT")).toBeInTheDocument();

    // Check current amounts
    expect(screen.getByText("1.2 ETH - 2400 USDT")).toBeInTheDocument();

    // Check collected fees
    expect(screen.getByText("20.1")).toBeInTheDocument();
    expect(screen.getByText(/ETH: 0.1 & USDT: 20/)).toBeInTheDocument();

    // Check uncollected fees
    expect(screen.getByText("10.05")).toBeInTheDocument();
    expect(screen.getByText(/ETH: 0.05 & USDT: 10/)).toBeInTheDocument();
  });

  it("handles close button click", () => {
    render(<PositionDetail {...defaultProps} />);
    const closeButton = screen.getByRole("button", { name: "Close" });
    fireEvent.click(closeButton);
    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  it("handles claim button click when fees are available", () => {
    render(<PositionDetail {...defaultProps} />);
    const claimButton = screen.getByText("getFee");
    fireEvent.click(claimButton);
    expect(defaultProps.onClaim).toHaveBeenCalledWith(1);
  });

  it("disables claim button when total_estimate_fee_in_token0 is less than or equal to 1", () => {
    const positionWithLowFees = {
      ...mockPosition,
      total_estimate_fee_in_token0: "0.5",
    };
    render(<PositionDetail {...defaultProps} position={positionWithLowFees} />);
    const claimButton = screen.getByText("getFee");
    expect(claimButton).toBeDisabled();
  });

  it("handles close position button click with confirmation", () => {
    render(<PositionDetail {...defaultProps} />);

    // Click the close button - should open confirmation dialog
    const closePositionButton = screen.getByText("close");
    fireEvent.click(closePositionButton);

    // Confirm dialog should appear
    expect(screen.getByTestId("confirm-close-dialog")).toBeInTheDocument();

    // onClosePosition should not be called yet
    expect(defaultProps.onClosePosition).not.toHaveBeenCalled();

    // Click confirm in the dialog
    const confirmButton = screen.getByText("Confirm Close");
    fireEvent.click(confirmButton);

    // Now onClosePosition should be called
    expect(defaultProps.onClosePosition).toHaveBeenCalledWith(mockPosition);
  });

  it("handles close position dialog cancellation", () => {
    render(<PositionDetail {...defaultProps} />);

    // Click the close button - should open confirmation dialog
    const closePositionButton = screen.getByText("close");
    fireEvent.click(closePositionButton);

    // Confirm dialog should appear
    expect(screen.getByTestId("confirm-close-dialog")).toBeInTheDocument();

    // Click cancel in the dialog
    const cancelButton = screen.getByText("Cancel");
    fireEvent.click(cancelButton);

    // onClosePosition should not be called
    expect(defaultProps.onClosePosition).not.toHaveBeenCalled();

    // Dialog should be closed
    expect(
      screen.queryByTestId("confirm-close-dialog"),
    ).not.toBeInTheDocument();
  });

  it("renders different status badges correctly", () => {
    const statuses = ["pending", "closed", "error"] as const;

    statuses.forEach((status) => {
      const { unmount } = render(
        <PositionDetail
          {...defaultProps}
          position={{ ...mockPosition, status }}
        />,
      );
      expect(screen.getByText(`status.${status}`)).toBeInTheDocument();
      unmount();
    });
  });

  it("renders price information correctly", () => {
    render(<PositionDetail {...defaultProps} />);

    // Check min price
    expect(screen.getByText("1000.1 USDT/ETH")).toBeInTheDocument();

    // Check max price
    expect(screen.getByText("2000.2 USDT/ETH")).toBeInTheDocument();

    // Check current price
    expect(screen.getByText("152.87 USDT/ETH")).toBeInTheDocument();
  });

  it("renders APR correctly", () => {
    render(<PositionDetail {...defaultProps} />);
    expect(screen.getByText("3.51%")).toBeInTheDocument();

    // Test with no APR
    const positionWithoutAPR = {
      ...mockPosition,
      apr: undefined,
    };
    const { rerender } = render(
      <PositionDetail {...defaultProps} position={positionWithoutAPR} />,
    );
    expect(screen.getByText("0.00%")).toBeInTheDocument();
  });

  it("renders estimated fees section when data is available", () => {
    const { rerender } = render(<PositionDetail {...defaultProps} />);

    // Check for estimated fees section
    expect(screen.getByText("estimatedFees")).toBeInTheDocument();
    expect(screen.getByText("2.5 ETH")).toBeInTheDocument();
    expect(screen.getByText(/ETH: 0.2 & USDT: 40/)).toBeInTheDocument();

    // Test without estimated fees
    const positionWithoutEstimatedFees = {
      ...mockPosition,
      estimate_fee_token0: undefined,
      estimate_fee_token1: undefined,
      total_estimate_fee_in_token0: undefined,
    };

    // Unmount the previous component and render a new one
    rerender(
      <PositionDetail
        {...defaultProps}
        position={positionWithoutEstimatedFees}
      />,
    );

    // The estimated fees section should not be present
    expect(screen.queryByText("estimatedFees")).toBeNull();
  });

  it("handles dialog close via onOpenChange", () => {
    render(<PositionDetail {...defaultProps} />);
    const dialog = screen.getByRole("dialog");
    fireEvent.keyDown(dialog, { key: "Escape" });
    expect(defaultProps.onClose).toHaveBeenCalled();
  });
});
