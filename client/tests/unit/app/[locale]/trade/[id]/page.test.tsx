import React from "react";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  act,
  within,
} from "@testing-library/react";
import TradeDetailPage from "@/app/[locale]/trade/[id]/page";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "next/navigation";
import {
  useDisputeTrade,
  useMarkTradePaid,
  useReleaseTrade,
  useTrade,
  useCancelTrade,
} from "@/lib/api/hooks/use-trades";
import { useQueryClient } from "@tanstack/react-query";

interface PaymentProofModalProps {
  onClose: () => void;
  onSuccess: (data: { file: File; description: string }) => void;
}

interface LayoutProps {
  children: React.ReactNode;
}

interface VietQRProps {
  bankName: string;
  accountName: string;
  accountNumber: string;
  amount: string;
  content: string;
  currency: string;
  copyButtonText: string;
  scanQRText: string;
  qrSize: number;
  useImageAPI: boolean;
}

// Mock the dependencies
jest.mock("@/lib/store/user-store");
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(() => ({
    push: jest.fn(),
  })),
  useParams: () => ({ id: "1" }),
}));
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));
jest.mock("@/lib/api/hooks/use-trades");
jest.mock("@tanstack/react-query");

// Mock toast component
const mockToast = jest.fn();
jest.mock("@/components/ui/use-toast", () => ({
  useToast: jest.fn(() => ({
    toast: mockToast,
  })),
}));

jest.mock("@/components/payment-proof-upload-modal", () => ({
  PaymentProofUploadModal: ({ onClose, onSuccess }: PaymentProofModalProps) => (
    <div data-testid="payment-proof-modal">
      <button
        data-testid="proof-submit-button"
        onClick={() =>
          onSuccess({ file: new File([], "test.jpg"), description: "test" })
        }
      >
        Submit
      </button>
      <button onClick={onClose}>Close</button>
    </div>
  ),
}));

jest.mock("@/components/viet-qr", () => ({
  VietQR: (props: VietQRProps) => (
    <div data-testid="viet-qr">
      <div>Bank: {props.bankName}</div>
      <div>Account: {props.accountName}</div>
      <div>Number: {props.accountNumber}</div>
      <div>Amount: {props.amount}</div>
      <button onClick={() => {}}>{props.copyButtonText}</button>
      <button>{props.scanQRText}</button>
    </div>
  ),
}));

jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: LayoutProps) => (
    <div data-testid="protected-layout">{children}</div>
  ),
}));

jest.mock("@/components/fiat-transaction-layout", () => ({
  FiatTransactionLayout: ({
    children,
    title,
  }: LayoutProps & { title: string }) => (
    <div data-testid="fiat-transaction-layout">
      <h1>{title}</h1>
      {children}
    </div>
  ),
}));

jest.mock("next/image", () => ({
  __esModule: true,
  default: ({ src, alt }: { src: string; alt: string }) => (
    <img src={src} alt={alt} />
  ),
}));

// Mock implementation for Dialog from shadcn ui
jest.mock("@/components/ui/dialog", () => ({
  Dialog: ({
    children,
    open,
  }: {
    children: React.ReactNode;
    open: boolean;
  }) => (
    <div data-testid="dialog" style={{ display: open ? "block" : "none" }}>
      {children}
    </div>
  ),
  DialogContent: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dialog-content">{children}</div>
  ),
  DialogHeader: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dialog-header">{children}</div>
  ),
  DialogTitle: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dialog-title">{children}</div>
  ),
  DialogFooter: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dialog-footer">{children}</div>
  ),
  DialogClose: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dialog-close">{children}</div>
  ),
}));

// Mock implementation for Textarea
jest.mock("@/components/ui/textarea", () => ({
  Textarea: (props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) => (
    <textarea
      data-testid="textarea"
      placeholder={props.placeholder}
      value={props.value}
      onChange={props.onChange}
    />
  ),
}));

describe("TradeDetailPage", () => {
  const mockQueryClient = {
    invalidateQueries: jest.fn(),
  };

  const defaultMockTrade = {
    id: "1",
    ref: "TRADE123",
    status: "unpaid",
    created_at: "2024-03-20T10:00:00Z",
    fiat_amount: "1000",
    fiat_currency: "USD",
    coin_amount: "0.05",
    coin_currency: "BTC",
    price: "20000",
    payment_method: "bank_transfer",
    seller: { id: "2" },
    buyer: { id: "1" },
    payment_details: {
      bank_name: "Test Bank",
      bank_account_name: "Test Account",
      bank_account_number: "123456789",
    },
    payment_receipt_details: null,
    taker_side: "buy",
    countdown_status: null,
    countdown_seconds: null,
    unpaid_timeout_at: null,
    paid_timeout_at: null,
  };

  const mockUser = {
    id: "1",
    role: "user",
  };

  const mockMarkTradePaid = jest.fn();
  const mockCancelTrade = jest.fn();
  const mockReleaseTrade = jest.fn();
  const mockDisputeTrade = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();

    // Setup query client mock
    (useQueryClient as jest.Mock).mockReturnValue(mockQueryClient);

    // Setup user store mock
    (useUserStore as unknown as jest.Mock).mockReturnValue({ user: mockUser });

    // Setup trade data mock
    (useTrade as jest.Mock).mockReturnValue({
      data: defaultMockTrade,
      isLoading: false,
      error: null,
    });

    // Setup action hooks mocks
    (useMarkTradePaid as jest.Mock).mockReturnValue({
      mutate: mockMarkTradePaid,
      isPending: false,
    });

    (useCancelTrade as jest.Mock).mockReturnValue({
      mutate: mockCancelTrade,
      isPending: false,
    });

    (useReleaseTrade as jest.Mock).mockReturnValue({
      mutate: mockReleaseTrade,
      isPending: false,
    });

    (useDisputeTrade as jest.Mock).mockReturnValue({
      mutate: mockDisputeTrade,
      isPending: false,
    });
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("renders loading state correctly", () => {
    (useTrade as jest.Mock).mockReturnValue({
      data: null,
      isLoading: true,
      error: null,
    });

    render(<TradeDetailPage />);
    expect(screen.getByTestId("protected-layout")).toBeInTheDocument();
  });

  it("renders error state correctly", () => {
    (useTrade as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error("Failed to load"),
    });

    render(<TradeDetailPage />);
    expect(screen.getByText("failedToLoad")).toBeInTheDocument();
  });

  it("renders trade details correctly for unpaid status", () => {
    render(<TradeDetailPage />);

    expect(screen.getByText("TRADE123")).toBeInTheDocument();
    expect(screen.getByText("Test Bank")).toBeInTheDocument();
  });

  describe("TradeCountdown component", () => {
    it("renders unpaid countdown correctly", () => {
      const mockTradeWithCountdown = {
        ...defaultMockTrade,
        countdown_status: "unpaid_countdown",
        unpaid_timeout_at: new Date(Date.now() + 300000).toISOString(), // 5 minutes in the future
      };

      (useTrade as jest.Mock).mockReturnValue({
        data: mockTradeWithCountdown,
        isLoading: false,
        error: null,
      });

      render(<TradeDetailPage />);

      expect(screen.getByText("autoCancel")).toBeInTheDocument();
      expect(screen.getByText("autoCancelDescription")).toBeInTheDocument();

      // Check for time format (5:00 or similar)
      const timeElement = screen.getByText(/\d+:\d+/);
      expect(timeElement).toBeInTheDocument();
    });

    it("renders paid countdown correctly", () => {
      const mockTradeWithCountdown = {
        ...defaultMockTrade,
        status: "paid",
        countdown_status: "paid_countdown",
        paid_timeout_at: new Date(Date.now() + 300000).toISOString(), // 5 minutes in the future
      };

      (useTrade as jest.Mock).mockReturnValue({
        data: mockTradeWithCountdown,
        isLoading: false,
        error: null,
      });

      render(<TradeDetailPage />);

      expect(screen.getByText("autoDispute")).toBeInTheDocument();
      expect(screen.getByText("autoDisputeDescription")).toBeInTheDocument();

      // Check for time format (5:00 or similar)
      const timeElement = screen.getByText(/\d+:\d+/);
      expect(timeElement).toBeInTheDocument();
    });

    it("updates countdown correctly", async () => {
      const mockTradeWithCountdown = {
        ...defaultMockTrade,
        countdown_status: "unpaid_countdown",
        unpaid_timeout_at: new Date(Date.now() + 65000).toISOString(), // 1:05 minutes in the future
      };

      (useTrade as jest.Mock).mockReturnValue({
        data: mockTradeWithCountdown,
        isLoading: false,
        error: null,
      });

      render(<TradeDetailPage />);

      // Initially should be around 1:05
      const initialTime = screen.getByText(/1:\d+/);
      expect(initialTime).toBeInTheDocument();

      // Advance timer by 10 seconds
      act(() => {
        jest.advanceTimersByTime(10000);
      });

      // Time should be updated to around 0:55
      const updatedTime = screen.getByText(/0:5\d/);
      expect(updatedTime).toBeInTheDocument();

      // Advance timer beyond countdown end
      act(() => {
        jest.advanceTimersByTime(60000);
      });

      // Should call invalidateQueries
      await waitFor(() => {
        expect(mockQueryClient.invalidateQueries).toHaveBeenCalled();
      });
    });
  });

  it("handles mark paid action correctly", () => {
    render(<TradeDetailPage />);

    fireEvent.click(screen.getByText("uploadPaymentProof"));

    const submitButton = screen.getByTestId("proof-submit-button");
    fireEvent.click(submitButton);

    expect(mockMarkTradePaid).toHaveBeenCalled();
  });

  it("validates required reason for cancel trade", () => {
    render(<TradeDetailPage />);

    // Open cancel modal
    fireEvent.click(screen.getByText("cancelTrade"));

    // Try to confirm without reason
    fireEvent.click(screen.getByText("confirmCancel"));

    // Should not submit with empty reason
    expect(mockCancelTrade).not.toHaveBeenCalled();
  });

  it("handles cancel trade action correctly", () => {
    render(<TradeDetailPage />);

    // Open cancel modal
    fireEvent.click(screen.getByText("cancelTrade"));

    // Enter reason
    const reasonInput = screen.getByTestId("textarea");
    fireEvent.change(reasonInput, { target: { value: "Test reason" } });

    // Submit
    fireEvent.click(screen.getByText("confirmCancel"));

    expect(mockCancelTrade).toHaveBeenCalled();
  });

  it("handles release trade action correctly", () => {
    // Set up paid trade
    const paidTrade = { ...defaultMockTrade, status: "paid" };
    (useTrade as jest.Mock).mockReturnValue({
      data: paidTrade,
      isLoading: false,
      error: null,
    });

    // Make user the seller
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { ...mockUser, id: "2" },
    });

    render(<TradeDetailPage />);

    // Click on release funds button to open confirmation dialog
    fireEvent.click(screen.getByText("releaseFunds"));

    // The confirmation dialog should be shown
    expect(screen.getByText("confirmReleaseFunds")).toBeInTheDocument();
    expect(screen.getByText("releaseConfirmationWarning")).toBeInTheDocument();

    // Use within to limit the search to the dialog content
    const dialog = screen.getByTestId("dialog-content");
    expect(within(dialog).getByText("recipient")).toBeInTheDocument();
    expect(within(dialog).getByText("tradeAmount")).toBeInTheDocument();
    expect(within(dialog).getByText("tradeReference")).toBeInTheDocument();

    // Click on confirm release button in the dialog
    fireEvent.click(screen.getByText("confirmRelease"));

    expect(mockReleaseTrade).toHaveBeenCalled();
  });

  it("validates required reason for dispute trade", () => {
    // Set up paid trade
    const paidTrade = { ...defaultMockTrade, status: "paid" };
    (useTrade as jest.Mock).mockReturnValue({
      data: paidTrade,
      isLoading: false,
      error: null,
    });

    // Make user the seller
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { ...mockUser, id: "2" },
    });

    render(<TradeDetailPage />);

    // Open dispute modal
    fireEvent.click(screen.getByText("disputeTrade"));

    // Try to submit without reason
    fireEvent.click(screen.getByText("submitDispute"));

    // Should not submit with empty reason
    expect(mockDisputeTrade).not.toHaveBeenCalled();
  });

  it("handles dispute trade action correctly", () => {
    // Set up paid trade
    const paidTrade = { ...defaultMockTrade, status: "paid" };
    (useTrade as jest.Mock).mockReturnValue({
      data: paidTrade,
      isLoading: false,
      error: null,
    });

    // Make user the seller
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { ...mockUser, id: "2" },
    });

    render(<TradeDetailPage />);

    // Open dispute modal
    fireEvent.click(screen.getByText("disputeTrade"));

    // Enter reason
    const reasonInput = screen.getByTestId("textarea");
    fireEvent.change(reasonInput, { target: { value: "Test dispute reason" } });

    // Submit
    fireEvent.click(screen.getByText("submitDispute"));

    expect(mockDisputeTrade).toHaveBeenCalled();
  });

  it("renders completed trade status correctly", () => {
    const mockCompletedTrade = {
      ...defaultMockTrade,
      status: "completed",
    };
    (useTrade as jest.Mock).mockReturnValue({
      data: mockCompletedTrade,
      isLoading: false,
      error: null,
    });

    render(<TradeDetailPage />);
    expect(screen.getByText("tradeCompleted")).toBeInTheDocument();
    expect(screen.getByText("tradeCompletedInfo")).toBeInTheDocument();
  });

  it("renders cancelled trade status correctly", () => {
    const mockCancelledTrade = {
      ...defaultMockTrade,
      status: "cancelled",
    };
    (useTrade as jest.Mock).mockReturnValue({
      data: mockCancelledTrade,
      isLoading: false,
      error: null,
    });

    render(<TradeDetailPage />);
    expect(screen.getByText("tradeCancelled")).toBeInTheDocument();
    expect(screen.getByText("tradeCancelledInfo")).toBeInTheDocument();
  });

  it("renders disputed trade status correctly", () => {
    const mockDisputedTrade = {
      ...defaultMockTrade,
      status: "disputed",
      dispute_reason: "Test dispute reason",
      dispute_resolution: "Test resolution",
    };
    (useTrade as jest.Mock).mockReturnValue({
      data: mockDisputedTrade,
      isLoading: false,
      error: null,
    });

    render(<TradeDetailPage />);
    expect(screen.getByText("tradeDisputed")).toBeInTheDocument();
    expect(screen.getByText("Test dispute reason")).toBeInTheDocument();
    expect(screen.getByText("Test resolution")).toBeInTheDocument();
  });

  it("renders disputed trade without resolution correctly", () => {
    const mockDisputedTrade = {
      ...defaultMockTrade,
      status: "disputed",
      dispute_reason: "Test dispute reason",
      dispute_resolution: null,
    };
    (useTrade as jest.Mock).mockReturnValue({
      data: mockDisputedTrade,
      isLoading: false,
      error: null,
    });

    render(<TradeDetailPage />);
    expect(screen.getByText("tradeDisputed")).toBeInTheDocument();
    expect(screen.getByText("Test dispute reason")).toBeInTheDocument();
    expect(screen.queryByText("disputeResolution")).not.toBeInTheDocument();
  });

  it("renders VietQR component correctly", () => {
    render(<TradeDetailPage />);
    expect(screen.getByTestId("viet-qr")).toBeInTheDocument();
    expect(screen.getByText("Bank: Test Bank")).toBeInTheDocument();
    expect(screen.getByText("Account: Test Account")).toBeInTheDocument();
    expect(screen.getByText("Number: 123456789")).toBeInTheDocument();
  });

  it("renders payment receipt details correctly when available", () => {
    const mockTradeWithReceipt = {
      ...defaultMockTrade,
      status: "paid",
      payment_receipt_details: {
        file_url: "https://example.com/receipt.jpg",
        description: "Payment receipt description",
        uploaded_at: "2024-03-20T10:00:00Z",
      },
    };
    (useTrade as jest.Mock).mockReturnValue({
      data: mockTradeWithReceipt,
      isLoading: false,
      error: null,
    });

    render(<TradeDetailPage />);

    expect(screen.getByText("paymentProof")).toBeInTheDocument();
    expect(screen.getByText("Payment receipt description")).toBeInTheDocument();
    // The ImageViewer component shows a loading skeleton initially, so we just check for the basic elements
    const paymentProofSection = screen.getByText("paymentProof").closest("div");
    expect(paymentProofSection).toBeInTheDocument();
  });

  it("renders payment receipt details with file but no URL correctly", () => {
    const mockTradeWithReceipt = {
      ...defaultMockTrade,
      status: "paid",
      payment_receipt_details: {
        file: new File([], "receipt.jpg"),
        description: "Payment receipt description",
        uploaded_at: "2024-03-20T10:00:00Z",
      },
    };
    (useTrade as jest.Mock).mockReturnValue({
      data: mockTradeWithReceipt,
      isLoading: false,
      error: null,
    });

    render(<TradeDetailPage />);

    expect(screen.getByText("paymentProof")).toBeInTheDocument();
    expect(screen.getByText("Payment receipt description")).toBeInTheDocument();
    expect(screen.getByText("paymentProofUploading")).toBeInTheDocument();
  });

  it("renders different UI for buyer vs seller", () => {
    // Test as buyer
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { ...mockUser, id: "1" }, // User is buyer
    });

    const { rerender } = render(<TradeDetailPage />);
    expect(screen.getByText("uploadPaymentProof")).toBeInTheDocument();
    expect(screen.getByText("cancelTrade")).toBeInTheDocument();

    // Test as seller
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { ...mockUser, id: "2" }, // User is seller
    });

    rerender(<TradeDetailPage />);
    expect(screen.queryByText("uploadPaymentProof")).not.toBeInTheDocument();
    expect(screen.queryByText("cancelTrade")).not.toBeInTheDocument();
  });

  it("handles navigation back to transactions", () => {
    const mockRouter = {
      push: jest.fn(),
    };
    (useRouter as jest.Mock).mockReturnValue(mockRouter);

    render(<TradeDetailPage />);

    fireEvent.click(screen.getByText("backToTransactions"));

    expect(mockRouter.push).toHaveBeenCalledWith("/transactions");
  });

  it("handles release from disputed trade correctly", () => {
    // Set up disputed trade
    const disputedTrade = {
      ...defaultMockTrade,
      status: "disputed",
      dispute_reason: "Test dispute reason",
    };
    (useTrade as jest.Mock).mockReturnValue({
      data: disputedTrade,
      isLoading: false,
      error: null,
    });

    // Make user the seller
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { ...mockUser, id: "2" },
    });

    render(<TradeDetailPage />);

    // Click on release funds button to open confirmation dialog
    fireEvent.click(screen.getByText("releaseFunds"));

    // The confirmation dialog should be shown
    expect(screen.getByText("confirmReleaseFunds")).toBeInTheDocument();
    expect(screen.getByText("releaseConfirmationWarning")).toBeInTheDocument();

    // Use within to limit the search to the dialog content
    const dialog = screen.getByTestId("dialog-content");
    expect(within(dialog).getByText("recipient")).toBeInTheDocument();
    expect(within(dialog).getByText("tradeAmount")).toBeInTheDocument();
    expect(within(dialog).getByText("tradeReference")).toBeInTheDocument();

    // Click on confirm release button in the dialog
    fireEvent.click(screen.getByText("confirmRelease"));

    expect(mockReleaseTrade).toHaveBeenCalled();
  });

  it("can cancel release trade confirmation", () => {
    // Set up paid trade
    const paidTrade = { ...defaultMockTrade, status: "paid" };
    (useTrade as jest.Mock).mockReturnValue({
      data: paidTrade,
      isLoading: false,
      error: null,
    });

    // Make user the seller
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { ...mockUser, id: "2" },
    });

    render(<TradeDetailPage />);

    // Click on release funds button to open confirmation dialog
    fireEvent.click(screen.getByText("releaseFunds"));

    // The confirmation dialog should be shown
    expect(screen.getByText("confirmReleaseFunds")).toBeInTheDocument();

    // Click on cancel button in the dialog
    fireEvent.click(screen.getByText("cancel"));

    // The dialog should be closed and release function not called
    expect(mockReleaseTrade).not.toHaveBeenCalled();
  });
});
