import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { ConfirmationDialog } from "../../../../../../../src/app/[locale]/withdraw/usdt/components/confirmation-dialog";
import { Network } from "../../../../../../../src/app/[locale]/withdraw/usdt/components/types";

// Mock formatNumber function
jest.mock("../../../../../../../src/lib/utils/index", () => ({
  formatNumber: jest.fn((value: number) => value.toFixed(2)),
}));

// Mock UI components
jest.mock("../../../../../../../src/components/ui/button", () => ({
  Button: ({
    children,
    onClick,
    disabled,
    className,
    variant,
  }: {
    children: React.ReactNode;
    onClick?: () => void;
    disabled?: boolean;
    className?: string;
    variant?: string;
  }) => (
    <button
      onClick={onClick}
      disabled={disabled}
      className={className}
      data-variant={variant}
    >
      {children}
    </button>
  ),
}));

jest.mock("../../../../../../../src/components/ui/dialog", () => ({
  Dialog: ({ children, open }: { children: React.ReactNode; open: boolean }) =>
    open ? <div data-testid="dialog">{children}</div> : null,
  DialogContent: ({
    children,
    className,
  }: {
    children: React.ReactNode;
    className?: string;
  }) => (
    <div className={className} data-testid="dialog-content">
      {children}
    </div>
  ),
  DialogHeader: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dialog-header">{children}</div>
  ),
  DialogTitle: ({ children }: { children: React.ReactNode }) => (
    <h2 data-testid="dialog-title">{children}</h2>
  ),
  DialogDescription: ({
    children,
    className,
  }: {
    children: React.ReactNode;
    className?: string;
  }) => (
    <p className={className} data-testid="dialog-description">
      {children}
    </p>
  ),
  DialogFooter: ({
    children,
    className,
  }: {
    children: React.ReactNode;
    className?: string;
  }) => (
    <div className={className} data-testid="dialog-footer">
      {children}
    </div>
  ),
}));

jest.mock("../../../../../../../src/components/ui/separator", () => ({
  Separator: () => <hr data-testid="separator" />,
}));

const mockNetwork: Network = {
  id: "trc20",
  name: "TRC20",
  enabled: true,
  fee: 1.5,
};

const defaultProps = {
  open: true,
  onOpenChange: jest.fn(),
  withdrawalType: "external" as const,
  amount: 100,
  address: "TXyZ123ABC456DEF789GHI",
  selectedNetwork: mockNetwork,
  withdrawalFee: 1.5,
  totalAmount: 101.5,
  isSubmitting: false,
  onConfirm: jest.fn(),
};

describe("ConfirmationDialog", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("External Withdrawal", () => {
    it("renders external withdrawal confirmation dialog", () => {
      render(<ConfirmationDialog {...defaultProps} />);

      expect(screen.getByTestId("dialog")).toBeInTheDocument();
      expect(screen.getByTestId("dialog-title")).toHaveTextContent(
        "Confirm Withdrawal",
      );
      expect(screen.getByText("Withdraw Amount")).toBeInTheDocument();
      expect(screen.getByText("100.00 USDT")).toBeInTheDocument();
      expect(screen.getByText("~ $100.00")).toBeInTheDocument();
    });

    it("displays transaction details for external withdrawal", () => {
      render(<ConfirmationDialog {...defaultProps} />);

      expect(screen.getByText("Transaction Details")).toBeInTheDocument();
      expect(screen.getByText("Address")).toBeInTheDocument();
      expect(screen.getByText("TXyZ123ABC456DEF789GHI")).toBeInTheDocument();
      expect(screen.getByText("Network")).toBeInTheDocument();
      expect(screen.getByText("TRC20")).toBeInTheDocument();
      expect(screen.getByText("Fee")).toBeInTheDocument();
      expect(screen.getByText("1.50 USDT")).toBeInTheDocument();
      expect(screen.getByText("Total")).toBeInTheDocument();
      expect(screen.getByText("101.50 USDT")).toBeInTheDocument();
    });

    it("calls onConfirm when confirm button is clicked", () => {
      const onConfirm = jest.fn();
      render(<ConfirmationDialog {...defaultProps} onConfirm={onConfirm} />);

      const confirmButton = screen.getByText("Confirm");
      fireEvent.click(confirmButton);

      expect(onConfirm).toHaveBeenCalledTimes(1);
    });

    it("calls onOpenChange when cancel button is clicked", () => {
      const onOpenChange = jest.fn();
      render(
        <ConfirmationDialog {...defaultProps} onOpenChange={onOpenChange} />,
      );

      const cancelButton = screen.getByText("Cancel");
      fireEvent.click(cancelButton);

      expect(onOpenChange).toHaveBeenCalledWith(false);
    });
  });

  describe("Internal Transfer", () => {
    const internalProps = {
      ...defaultProps,
      withdrawalType: "internal" as const,
      username: "testuser",
      withdrawalFee: 0,
      totalAmount: 100,
    };

    it("renders internal transfer confirmation dialog", () => {
      render(<ConfirmationDialog {...internalProps} />);

      expect(screen.getByTestId("dialog-title")).toHaveTextContent(
        "Confirm Internal Transfer",
      );
      expect(screen.getByText("Transfer Amount")).toBeInTheDocument();
      expect(screen.getAllByText("100.00 USDT")).toHaveLength(2); // Amount and Total
    });

    it("displays transaction details for internal transfer", () => {
      render(<ConfirmationDialog {...internalProps} />);

      expect(screen.getByText("Recipient")).toBeInTheDocument();
      expect(screen.getByText("@testuser")).toBeInTheDocument();
      expect(screen.queryByText("Network")).not.toBeInTheDocument();
      expect(screen.getByText("0.00 USDT")).toBeInTheDocument(); // Fee is 0
      expect(screen.getAllByText("100.00 USDT")).toHaveLength(2); // Amount and Total are same
    });
  });

  describe("Loading States", () => {
    it("shows processing state when submitting", () => {
      render(<ConfirmationDialog {...defaultProps} isSubmitting={true} />);

      expect(screen.getByText("Processing...")).toBeInTheDocument();
      expect(screen.getByText("Processing...")).toBeDisabled();
      expect(screen.getByText("Cancel")).toBeDisabled();
    });

    it("enables buttons when not submitting", () => {
      render(<ConfirmationDialog {...defaultProps} isSubmitting={false} />);

      expect(screen.getByText("Confirm")).not.toBeDisabled();
      expect(screen.getByText("Cancel")).not.toBeDisabled();
    });
  });

  describe("Dialog Visibility", () => {
    it("renders dialog when open is true", () => {
      render(<ConfirmationDialog {...defaultProps} open={true} />);
      expect(screen.getByTestId("dialog")).toBeInTheDocument();
    });

    it("does not render dialog when open is false", () => {
      render(<ConfirmationDialog {...defaultProps} open={false} />);
      expect(screen.queryByTestId("dialog")).not.toBeInTheDocument();
    });
  });

  describe("Address and Username Display", () => {
    it("truncates long addresses properly", () => {
      const longAddress =
        "TXyZ123ABC456DEF789GHI123456789012345678901234567890";
      render(<ConfirmationDialog {...defaultProps} address={longAddress} />);

      const addressElement = screen.getByText(longAddress);
      expect(addressElement).toHaveAttribute("title", longAddress);
    });

    it("formats username with @ symbol", () => {
      const internalProps = {
        ...defaultProps,
        withdrawalType: "internal" as const,
        username: "testuser123",
        withdrawalFee: 0,
        totalAmount: 100,
      };

      render(<ConfirmationDialog {...internalProps} />);
      expect(screen.getByText("@testuser123")).toBeInTheDocument();
    });
  });
});
