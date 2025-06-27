import { render, screen, fireEvent } from "@testing-library/react";
import { useTranslations } from "next-intl";
import ConfirmCloseDialog from "@/app/[locale]/liquidity/positions/components/ConfirmCloseDialog";
import { AmmPosition } from "@/lib/api/positions";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

describe("ConfirmCloseDialog", () => {
  const mockT = (key: string, values?: Record<string, string>) => {
    if (values) {
      return `${key} ${Object.values(values).join(" ")}`;
    }
    return key;
  };

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
    created_at: Date.now(),
    updated_at: Date.now(),
  };

  const defaultProps = {
    isOpen: true,
    position: mockPosition,
    onClose: jest.fn(),
    onConfirm: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useTranslations as jest.Mock).mockReturnValue(mockT);
  });

  it("renders dialog when isOpen is true", () => {
    render(<ConfirmCloseDialog {...defaultProps} />);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("does not render dialog when isOpen is false", () => {
    render(<ConfirmCloseDialog {...defaultProps} isOpen={false} />);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("displays correct title and description", () => {
    render(<ConfirmCloseDialog {...defaultProps} />);
    expect(screen.getByText("confirmClose")).toBeInTheDocument();
    expect(
      screen.getByText("confirmCloseDescription ETH/USDT"),
    ).toBeInTheDocument();
  });

  it("calls onClose when cancel button is clicked", () => {
    render(<ConfirmCloseDialog {...defaultProps} />);
    const cancelButton = screen.getByRole("button", { name: "cancel" });
    fireEvent.click(cancelButton);
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it("calls onConfirm when close button is clicked", () => {
    render(<ConfirmCloseDialog {...defaultProps} />);
    const closeButton = screen.getByRole("button", { name: "close" });
    fireEvent.click(closeButton);
    expect(defaultProps.onConfirm).toHaveBeenCalledTimes(1);
  });

  it("calls onClose when dialog is closed via overlay", () => {
    render(<ConfirmCloseDialog {...defaultProps} />);
    const dialog = screen.getByRole("dialog");
    fireEvent.keyDown(dialog, { key: "Escape" });
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it("handles null position gracefully", () => {
    render(<ConfirmCloseDialog {...defaultProps} position={null} />);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(
      screen.queryByText("confirmCloseDescription"),
    ).not.toBeInTheDocument();
  });

  it("renders with correct button variants", () => {
    render(<ConfirmCloseDialog {...defaultProps} />);
    const cancelButton = screen.getByRole("button", { name: "cancel" });
    const closeButton = screen.getByRole("button", { name: "close" });

    // Check for outline variant classes
    expect(cancelButton).toHaveClass(
      "border",
      "border-input",
      "bg-background",
      "hover:bg-accent",
      "hover:text-accent-foreground",
    );

    // Check for default variant classes
    expect(closeButton).toHaveClass(
      "bg-primary",
      "text-primary-foreground",
      "hover:bg-primary/90",
    );
  });
});
