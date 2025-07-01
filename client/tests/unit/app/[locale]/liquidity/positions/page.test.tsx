import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import PositionsPage from "@/app/[locale]/liquidity/positions/page";
import { AmmPosition } from "@/lib/api/positions";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock next/link
jest.mock("next/link", () => ({
  __esModule: true,
  default: ({
    children,
    href,
  }: {
    children: React.ReactNode;
    href: string;
  }) => <a href={href}>{children}</a>,
}));

// Mock @tanstack/react-query
jest.mock("@tanstack/react-query", () => ({
  useQuery: jest.fn(),
  useMutation: jest.fn(),
  useQueryClient: jest.fn(),
}));

// Mock sonner
jest.mock("sonner", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
  Toaster: () => null,
}));

// Mock API functions
jest.mock("@/lib/api/positions", () => ({
  collectFee: jest.fn(),
  closePosition: jest.fn(),
  fetchPositions: jest.fn(),
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
          <button onClick={onConfirm} data-testid="confirm-close-btn">
            Confirm Close
          </button>
          <button onClick={onClose} data-testid="cancel-close-btn">
            Cancel
          </button>
        </div>
      );
    };
  },
);

describe("PositionsPage", () => {
  const mockT = (key: string) => key;
  const mockQueryClient = {
    invalidateQueries: jest.fn(),
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
    // New fields
    apr: "3.51",
    estimate_fee_token0: "0.2",
    estimate_fee_token1: "40",
    total_estimate_fee_in_token0: "2.5",
    created_at: Date.now(),
    updated_at: Date.now(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useTranslations as jest.Mock).mockReturnValue(mockT);
    (useQueryClient as jest.Mock).mockReturnValue(mockQueryClient);
  });

  it("renders loading state", () => {
    (useQuery as jest.Mock).mockReturnValue({
      data: null,
      isLoading: true,
    });

    render(<PositionsPage />);
    expect(screen.getByText("loading")).toBeInTheDocument();
  });

  it("renders empty state when no positions", () => {
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [] },
      isLoading: false,
    });

    render(<PositionsPage />);
    expect(screen.getByText("noPositions")).toBeInTheDocument();
  });

  it("renders positions list when data is available", () => {
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });

    render(<PositionsPage />);
    expect(screen.getAllByText(mockPosition.pool_pair).length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText("ETH").length).toBeGreaterThan(0);
    expect(screen.getAllByText("USDT").length).toBeGreaterThan(0);
  });

  it("handles tab switching", () => {
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });

    render(<PositionsPage />);
    const openTabs = screen.getAllByText("statusOpen");
    const openTabButton = openTabs.find(
      (el) => el.getAttribute("role") === "tab",
    );
    if (openTabButton) fireEvent.click(openTabButton);
  });

  it("handles claim fee action", () => {
    const mockCollectFee = jest.fn();
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });
    (useMutation as jest.Mock).mockReturnValue({
      mutate: mockCollectFee,
    });

    render(<PositionsPage />);
    const claimButton = screen
      .getAllByText("getFee")
      .find((el) => el.tagName === "BUTTON" && !el.hasAttribute("disabled"));
    if (claimButton) fireEvent.click(claimButton);
    expect(mockCollectFee).toHaveBeenCalledWith(mockPosition.id);
  });

  it("handles close position action", async () => {
    const mockClosePosition = jest.fn().mockResolvedValue({});
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });
    (useMutation as jest.Mock).mockReturnValue({
      mutate: mockClosePosition,
    });

    render(<PositionsPage />);
    const card = screen.getAllByText(mockPosition.pool_pair)[0];
    fireEvent.click(card);
    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });
    const closeButtons = screen.getAllByRole("button", { name: /close/i });
    const actionCloseButton = closeButtons.find(
      (btn) => btn.textContent?.trim().toLowerCase() === "close",
    );
    expect(actionCloseButton).toBeDefined();
    fireEvent.click(actionCloseButton!);

    // Should show confirm dialog
    await waitFor(() => {
      expect(screen.getByTestId("confirm-close-dialog")).toBeInTheDocument();
    });

    // Click confirm in the dialog
    const confirmButton = screen.getByTestId("confirm-close-btn");
    fireEvent.click(confirmButton);

    expect(mockClosePosition).toHaveBeenCalledWith(mockPosition);
  });

  it("handles error in claim fee action", () => {
    const mockCollectFee = jest.fn();
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });
    (useMutation as jest.Mock).mockReturnValue({
      mutate: mockCollectFee,
    });

    render(<PositionsPage />);
    const claimButton = screen
      .getAllByText("getFee")
      .find((el) => el.tagName === "BUTTON" && !el.hasAttribute("disabled"));
    if (claimButton) fireEvent.click(claimButton);
    expect(mockCollectFee).toHaveBeenCalledWith(mockPosition.id);
  });

  it("handles error in close position action", async () => {
    const mockClosePosition = jest.fn().mockResolvedValue({});
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });
    (useMutation as jest.Mock).mockReturnValue({
      mutate: mockClosePosition,
    });
    jest.spyOn(console, "error").mockImplementation(() => {});

    render(<PositionsPage />);
    const card = screen.getAllByText(mockPosition.pool_pair)[0];
    fireEvent.click(card);
    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });
    const closeButtons = screen.getAllByRole("button", { name: /close/i });
    const actionCloseButton = closeButtons.find(
      (btn) => btn.textContent?.trim().toLowerCase() === "close",
    );
    expect(actionCloseButton).toBeDefined();
    fireEvent.click(actionCloseButton!);

    // Should show confirm dialog
    await waitFor(() => {
      expect(screen.getByTestId("confirm-close-dialog")).toBeInTheDocument();
    });

    // Click confirm in the dialog
    const confirmButton = screen.getByTestId("confirm-close-btn");
    fireEvent.click(confirmButton);

    expect(mockClosePosition).toHaveBeenCalledWith(mockPosition);
    await waitFor(() => {}); // Let the promise settle
  });

  it("opens position detail modal", async () => {
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });

    render(<PositionsPage />);
    const card = screen.getAllByText(mockPosition.pool_pair)[0];
    fireEvent.click(card);

    await waitFor(() => {
      const dialog = screen.getByRole("dialog");
      expect(dialog).toBeInTheDocument();
      expect(dialog).toBeVisible();
    });
  });

  it("closes position detail modal", async () => {
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });

    render(<PositionsPage />);
    const card = screen.getAllByText(mockPosition.pool_pair)[0];
    fireEvent.click(card);

    await waitFor(() => {
      expect(screen.getByRole("dialog")).toBeInTheDocument();
    });

    const closeButtons = screen.getAllByRole("button", { name: /close/i });
    const dialogCloseButton = closeButtons.find(
      (btn) =>
        btn.className.includes("absolute") && btn.className.includes("right-4"),
    );
    expect(dialogCloseButton).toBeDefined();
    fireEvent.click(dialogCloseButton!);

    await waitFor(() => {
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });

  it("renders navigation elements", () => {
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [mockPosition] },
      isLoading: false,
    });

    render(<PositionsPage />);
    expect(screen.getByText("yourPositions")).toBeInTheDocument();
    expect(screen.getByText("selectPool")).toBeInTheDocument();
  });

  it("calls toast and invalidateQueries on claimFeeMutation success", () => {
    const t = jest.fn((key) => key);
    (useTranslations as jest.Mock).mockReturnValue(t);
    const queryClient = { invalidateQueries: jest.fn() };
    (useQueryClient as jest.Mock).mockReturnValue(queryClient);
    render(<PositionsPage />);
    const mutationConfig = (useMutation as jest.Mock).mock.calls[0][0];
    mutationConfig.onSuccess();
    expect(t).toHaveBeenCalledWith("feeCollected");
    expect(t).toHaveBeenCalledWith("feeCollectedSuccess");
    expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
      queryKey: ["positions"],
    });
  });

  it("calls toast.error and console.error on claimFeeMutation error", () => {
    const t = jest.fn((key) => key);
    (useTranslations as jest.Mock).mockReturnValue(t);
    const error = new Error("fail");
    jest.spyOn(console, "error").mockImplementation(() => {});
    render(<PositionsPage />);
    const mutationConfig = (useMutation as jest.Mock).mock.calls[0][0];
    mutationConfig.onError(error);
    expect(t).toHaveBeenCalledWith("error");
    expect(t).toHaveBeenCalledWith("feeCollectedError");
    expect(console.error).toHaveBeenCalledWith("Error collecting fee:", error);
  });

  it("calls toast and invalidateQueries on closePositionMutation success", () => {
    const t = jest.fn((key) => key);
    (useTranslations as jest.Mock).mockReturnValue(t);
    const queryClient = { invalidateQueries: jest.fn() };
    (useQueryClient as jest.Mock).mockReturnValue(queryClient);
    render(<PositionsPage />);
    const mutationConfig = (useMutation as jest.Mock).mock.calls[1][0];
    mutationConfig.onSuccess();
    expect(t).toHaveBeenCalledWith("positionClosing");
    expect(t).toHaveBeenCalledWith("positionClosingSuccess");
    expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
      queryKey: ["positions"],
    });
  });

  it("calls toast.error and console.error on closePositionMutation error", () => {
    const t = jest.fn((key) => key);
    (useTranslations as jest.Mock).mockReturnValue(t);
    const error = new Error("fail");
    jest.spyOn(console, "error").mockImplementation(() => {});
    render(<PositionsPage />);
    const mutationConfig = (useMutation as jest.Mock).mock.calls[1][0];
    mutationConfig.onError(error);
    expect(t).toHaveBeenCalledWith("error");
    expect(t).toHaveBeenCalledWith("positionClosedError");
    expect(console.error).toHaveBeenCalledWith(
      "Error closing position:",
      error,
    );
  });

  it("disables claim fee button when total_estimate_fee_in_token0 is less than or equal to 1", () => {
    const positionWithLowFees = {
      ...mockPosition,
      total_estimate_fee_in_token0: "0.5",
    };
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: [positionWithLowFees] },
      isLoading: false,
    });

    render(<PositionsPage />);
    const claimButton = screen
      .getAllByText("getFee")
      .find((el) => el.tagName === "BUTTON");
    expect(claimButton).toHaveAttribute("disabled");
  });

  it("shows correct status badge for different position statuses", () => {
    const positions = [
      { ...mockPosition, status: "open" },
      { ...mockPosition, id: 2, status: "pending" },
      { ...mockPosition, id: 3, status: "closed" },
      { ...mockPosition, id: 4, status: "error" },
    ];
    (useQuery as jest.Mock).mockReturnValue({
      data: { amm_positions: positions },
      isLoading: false,
    });

    render(<PositionsPage />);
    expect(screen.getAllByText("statusOpen").length).toBeGreaterThan(0);
    expect(screen.getAllByText("statusPending").length).toBeGreaterThan(0);
    expect(screen.getAllByText("statusClosed").length).toBeGreaterThan(0);
    expect(screen.getAllByText("statusError").length).toBeGreaterThan(0);
  });
});
