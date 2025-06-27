import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import WithdrawalDetailPage from "@/app/[locale]/withdraw/usdt/[id]/page";
import { useParams } from "next/navigation";
import { toast } from "sonner";
import { getWithdrawalById } from "@/lib/api/withdrawals";

// Mock next/navigation
jest.mock("next/navigation", () => ({
  useParams: jest.fn(),
}));

// Mock sonner toast
jest.mock("sonner", () => ({
  toast: {
    success: jest.fn(),
  },
}));

// Mock API calls
jest.mock("@/lib/api/withdrawals", () => ({
  getWithdrawalById: jest.fn(),
}));

describe("WithdrawalDetailPage", () => {
  const mockWithdrawalId = "123";
  const mockWithdrawalData = {
    id: mockWithdrawalId,
    coin_amount: 100,
    coin_fee: 1,
    coin_currency: "USDT",
    coin_layer: "bep20",
    network_name: "BNB Smart Chain",
    coin_address: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F",
    tx_hash: "0x123...abc",
    status: "completed",
    created_at: "2024-03-20T17:30:00.000Z",
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useParams as jest.Mock).mockReturnValue({ id: mockWithdrawalId });
    (getWithdrawalById as jest.Mock).mockResolvedValue(mockWithdrawalData);
  });

  it("renders withdrawal details successfully", async () => {
    render(<WithdrawalDetailPage />);

    await waitFor(() => {
      // Check main elements
      expect(screen.getByText("Withdrawal Details")).toBeInTheDocument();
      expect(screen.getByText(/^Withdraw USDT$/)).toBeInTheDocument();
      expect(screen.getByText(/100\.00 USDT$/)).toBeInTheDocument();

      // Check status
      expect(screen.getByText("completed")).toBeInTheDocument();

      // Check network
      expect(screen.getByText("BNB Smart Chain")).toBeInTheDocument();

      // Check address
      expect(screen.getByText("0x71C7...d8976F")).toBeInTheDocument();

      // Check transaction hash
      expect(screen.getByText("0x123...abc")).toBeInTheDocument();

      // Check time - use a function matcher to handle timezone differences
      expect(
        screen.getByText((content) => {
          return content.includes("/03/2024") && content.includes(":");
        }),
      ).toBeInTheDocument();
    });
  });

  it("handles copy to clipboard", async () => {
    const mockClipboard = {
      writeText: jest.fn(),
    };
    Object.assign(navigator, {
      clipboard: mockClipboard,
    });

    render(<WithdrawalDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("0x71C7...d8976F")).toBeInTheDocument();
    });

    const copyButton = screen.getAllByRole("button")[1]; // First copy button (address)
    fireEvent.click(copyButton);

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
      mockWithdrawalData.coin_address,
    );
    expect(toast.success).toHaveBeenCalledWith("Address copied to clipboard");
  });

  it("renders different status colors correctly", async () => {
    const statuses = [
      { status: "pending", className: "bg-yellow-100 text-yellow-800" },
      { status: "processing", className: "bg-yellow-50 text-yellow-700" },
      { status: "completed", className: "bg-green-100 text-green-800" },
      { status: "failed", className: "bg-red-100 text-red-800" },
    ];

    for (const { status, className } of statuses) {
      (getWithdrawalById as jest.Mock).mockResolvedValue({
        ...mockWithdrawalData,
        status,
      });

      const { unmount } = render(<WithdrawalDetailPage />);

      await waitFor(() => {
        const statusBadge = screen.getByText(status);
        expect(statusBadge).toBeInTheDocument();
        const badgeElement = statusBadge.closest("div");
        expect(badgeElement).toHaveClass(className.split(" ")[0]);
        expect(badgeElement).toHaveClass(className.split(" ")[1]);
      });

      // Cleanup after each iteration
      unmount();
    }
  });

  it("formats date correctly", async () => {
    render(<WithdrawalDetailPage />);

    await waitFor(() => {
      const dateElement = screen.getByText((content) => {
        return content.includes("/03/2024") && content.includes(":");
      });
      expect(dateElement).toBeInTheDocument();
    });
  });

  it("handles invalid date gracefully", async () => {
    (getWithdrawalById as jest.Mock).mockResolvedValue({
      ...mockWithdrawalData,
      created_at: "invalid-date",
    });

    render(<WithdrawalDetailPage />);

    await waitFor(() => {
      const dateElement = screen.getByText((content) => {
        return content.includes("Invalid Date");
      });
      expect(dateElement).toBeInTheDocument();
    });
  });

  it("renders correct explorer link based on network", async () => {
    const networks = [
      {
        coin_layer: "bep20",
        tx_hash: "0x123",
        expected_url: "https://bscscan.com/tx/0x123",
      },
      {
        coin_layer: "erc20",
        tx_hash: "0x456",
        expected_url: "https://etherscan.io/tx/0x456",
      },
      {
        coin_layer: "trc20",
        tx_hash: "0x789",
        expected_url: "https://tronscan.org/#/transaction/0x789",
      },
    ];

    for (const network of networks) {
      (getWithdrawalById as jest.Mock).mockResolvedValue({
        ...mockWithdrawalData,
        coin_layer: network.coin_layer,
        tx_hash: network.tx_hash,
      });

      render(<WithdrawalDetailPage />);

      await waitFor(() => {
        const txHash = screen.getByTitle(network.tx_hash);
        const viewButton = txHash.parentElement?.querySelector(
          '[aria-label="View transaction"]',
        );
        expect(viewButton?.closest("a")).toHaveAttribute(
          "href",
          network.expected_url,
        );
      });
    }
  });

  it("uses fallback network name when network_name is not provided", async () => {
    (getWithdrawalById as jest.Mock).mockResolvedValue({
      ...mockWithdrawalData,
      network_name: undefined,
    });

    render(<WithdrawalDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("BEP20 Network")).toBeInTheDocument();
    });
  });

  it("handles navigation back to wallet", async () => {
    render(<WithdrawalDetailPage />);

    await waitFor(() => {
      const backButton = screen.getByRole("button", { name: "Back to Wallet" });
      expect(backButton).toBeInTheDocument();
      expect(backButton.closest("a")).toHaveAttribute("href", "/wallet");
    });
  });
});
