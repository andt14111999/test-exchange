// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => `translated.${key}`,
}));

import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import WithdrawUSDTPage from "@/app/[locale]/withdraw/usdt/page";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { createWithdrawal } from "@/lib/api/withdrawals";
import { getWithdrawalFees } from "@/lib/api/settings";
import { useWallet } from "@/hooks/use-wallet";

// Create a test query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

// Wrapper component with QueryClientProvider
const TestWrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

// Mock next/navigation
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

// Mock sonner toast
jest.mock("sonner", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

// Mock API calls
jest.mock("@/lib/api/withdrawals", () => ({
  createWithdrawal: jest.fn(),
  checkReceiver: jest.fn(),
}));

jest.mock("@/lib/api/settings", () => ({
  getWithdrawalFees: jest.fn(),
}));

// Mock useWallet hook
jest.mock("@/hooks/use-wallet", () => ({
  useWallet: jest.fn(),
}));

// Mock useCoinNetworks hook
jest.mock("@/hooks/use-coin-networks", () => ({
  useCoinNetworks: jest.fn(() => ({
    networks: [
      {
        id: "erc20",
        name: "Ethereum (ERC20)",
        enabled: true,
      },
      {
        id: "bep20",
        name: "BNB Smart Chain (BEP20)",
        enabled: true,
      },
      {
        id: "trc20",
        name: "TRON (TRC20)",
        enabled: true,
      },
      {
        id: "solana",
        name: "Solana",
        enabled: true,
      },
    ],
    isLoading: false,
  })),
}));

// Mock user store
jest.mock("@/lib/store/user-store", () => ({
  useUserStore: () => ({
    user: { authenticatorEnabled: true },
  }),
}));

// Mock device trust hook
jest.mock("@/hooks/use-device-trust", () => ({
  useDeviceTrust: () => ({
    isDeviceTrusted: true,
    isCheckingDevice: false,
  }),
}));

describe("WithdrawUSDTPage", () => {
  const mockRouter = {
    push: jest.fn(),
  };

  const mockWithdrawalFees = {
    usdt_bep20: 1,
    usdt_trc20: 2,
    usdt_erc20: 10, // Updated to match backend default
    usdt_solana: 3,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (getWithdrawalFees as jest.Mock).mockResolvedValue(mockWithdrawalFees);
    (useWallet as jest.Mock).mockReturnValue({
      data: {
        coin_accounts: [
          {
            coin_currency: "USDT",
            balance: 10000, // Sufficient balance for testing
          },
        ],
      },
      isLoading: false,
    });
  });

  it("renders the withdrawal form correctly", async () => {
    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Check main elements
    expect(screen.getAllByText("Withdraw USDT").length).toBeGreaterThan(0);
    expect(
      screen.getByText("translated.withdrawalDetails"),
    ).toBeInTheDocument();
    expect(screen.getByText("translated.network")).toBeInTheDocument();
    expect(screen.getByText("translated.amount")).toBeInTheDocument();
    expect(
      screen.getByText("translated.destinationAddress"),
    ).toBeInTheDocument();
  });

  it("selects ERC20 (Ethereum) as default network", async () => {
    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for networks and fees to load
    await waitFor(() => {
      expect(getWithdrawalFees).toHaveBeenCalled();
    });

    // Check that ERC20 is selected by default by looking for the display text
    await waitFor(
      () => {
        expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
      },
      { timeout: 3000 },
    );
  });

  it("loads withdrawal fees and applies them to networks", async () => {
    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for fees to be loaded
    await waitFor(() => {
      expect(getWithdrawalFees).toHaveBeenCalled();
    });

    // Verify fees are applied correctly by checking that ERC20 is selected
    await waitFor(() => {
      // Should show ERC20 selected as default since it has the highest fee
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });
  });

  it("handles fee loading error gracefully", async () => {
    (getWithdrawalFees as jest.Mock).mockRejectedValue(new Error("API Error"));

    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        "Failed to load withdrawal fees. Using default values.",
      );
    });

    // Should show error state (no network selected) when fee loading fails
    await waitFor(() => {
      expect(screen.getByText("translated.selectNetwork")).toBeInTheDocument();
    });
  });

  it("validates ERC20 address correctly", async () => {
    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    const addressInput = screen.getByPlaceholderText("translated.enterAddress");

    // Invalid address
    fireEvent.change(addressInput, {
      target: { value: "invalid-address" },
    });

    await waitFor(() => {
      expect(
        screen.getByText("Invalid Ethereum (ERC20) address format"),
      ).toBeInTheDocument();
    });

    // Valid ERC20 address
    fireEvent.change(addressInput, {
      target: { value: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F" },
    });

    await waitFor(() => {
      expect(
        screen.queryByText("Invalid Ethereum (ERC20) address format"),
      ).not.toBeInTheDocument();
    });
  });

  it("validates minimum withdrawal amount", async () => {
    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    const amountInput = screen.getByPlaceholderText("translated.enterAmount");
    const addressInput = screen.getByPlaceholderText("translated.enterAddress");

    // Amount less than minimum
    fireEvent.change(amountInput, { target: { value: "0.005" } });
    const submitButton = screen.getByRole("button", { name: /Withdraw.*USDT/ });
    expect(submitButton).toBeDisabled();

    // Valid amount and address for ERC20
    fireEvent.change(amountInput, { target: { value: "10" } });
    fireEvent.change(addressInput, {
      target: { value: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F" },
    });

    await waitFor(() => {
      const button = screen.getByRole("button", { name: /Withdraw.*USDT/ });
      expect(button).not.toBeDisabled();
    });
  });

  it("shows confirmation dialog with correct details for ERC20", async () => {
    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    // Fill in form
    fireEvent.change(screen.getByPlaceholderText("translated.enterAmount"), {
      target: { value: "100" },
    });

    // Enter ERC20 address
    fireEvent.change(screen.getByPlaceholderText("translated.enterAddress"), {
      target: { value: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F" },
    });

    // Click withdraw button
    const withdrawButton = screen.getByRole("button", {
      name: /Withdraw.*USDT/,
    });
    fireEvent.click(withdrawButton);

    // Check confirmation dialog
    expect(screen.getByText("Confirm Withdrawal")).toBeInTheDocument();
    expect(screen.getAllByText(/100\.00 USDT$/).length).toBeGreaterThan(0);

    // Check that the total includes ERC20 fee (100 + 10 = 110)
    await waitFor(() => {
      expect(screen.getAllByText(/110\.00 USDT/).length).toBeGreaterThan(0);
    });
  });

  it("handles successful withdrawal with ERC20 as default", async () => {
    const mockWithdrawalResponse = { id: "123" };
    (createWithdrawal as jest.Mock).mockResolvedValue(mockWithdrawalResponse);

    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    // Fill in form
    fireEvent.change(screen.getByPlaceholderText("translated.enterAmount"), {
      target: { value: "100" },
    });

    // Enter ERC20 address
    fireEvent.change(screen.getByPlaceholderText("translated.enterAddress"), {
      target: { value: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F" },
    });

    // Open confirmation dialog
    const withdrawButton = screen.getByRole("button", {
      name: /Withdraw.*USDT/,
    });
    fireEvent.click(withdrawButton);

    // Confirm withdrawal
    const confirmButton = screen.getByRole("button", {
      name: "Confirm",
    });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(createWithdrawal).toHaveBeenCalledWith({
        coin_amount: 100,
        coin_currency: "USDT",
        coin_layer: "erc20",
        coin_address: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F",
      });
      expect(toast.success).toHaveBeenCalledWith(
        "translated.withdrawalRequestSuccess",
      );
      expect(mockRouter.push).toHaveBeenCalledWith("/withdraw/usdt/123");
    });
  });

  it("calculates total amount correctly with ERC20 default", async () => {
    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    // Enter amount
    fireEvent.change(screen.getByPlaceholderText("translated.enterAmount"), {
      target: { value: "100" },
    });

    // With ERC20 selected, fee should be 10 USDT, so total = 110
    await waitFor(() => {
      expect(screen.getAllByText(/110.*USDT/).length).toBeGreaterThan(0);
    });
  });

  it("handles network change correctly", async () => {
    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    // Change network to TRC20
    fireEvent.click(screen.getByRole("combobox"));
    fireEvent.click(screen.getAllByText("TRON (TRC20)")[0]);

    // Verify address validation changes
    const addressInput = screen.getByPlaceholderText("translated.enterAddress");
    fireEvent.change(addressInput, {
      target: { value: "TKQpQkMWRvTJpQgYrGp8wKgJSHV3DqNHJ3" },
    });
    expect(
      screen.queryByText("Invalid TRON (TRC20) address format"),
    ).not.toBeInTheDocument();
  });

  it("validates insufficient balance including withdrawal fees", async () => {
    // Set up a wallet with limited balance
    (useWallet as jest.Mock).mockReturnValue({
      data: {
        coin_accounts: [
          {
            coin_currency: "USDT",
            balance: 105, // Only 105 USDT available
          },
        ],
      },
      isLoading: false,
    });

    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default (10 USDT fee)
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    const amountInput = screen.getByPlaceholderText("translated.enterAmount");
    const addressInput = screen.getByPlaceholderText("translated.enterAddress");

    // Enter valid address first
    fireEvent.change(addressInput, {
      target: { value: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F" },
    });

    // Try to withdraw 100 USDT (total needed: 100 + 10 fee = 110 USDT, but only 105 available)
    fireEvent.change(amountInput, { target: { value: "100" } });

    // Should show insufficient balance error
    await waitFor(() => {
      expect(
        screen.getByText(/Insufficient balance\. Available: 105.*USDT/),
      ).toBeInTheDocument();
    });

    // Submit button should be disabled
    const submitButton = screen.getByRole("button", { name: /Withdraw.*USDT/ });
    expect(submitButton).toBeDisabled();
  });

  it("allows withdrawal when balance covers amount plus fees", async () => {
    // Set up a wallet with sufficient balance
    (useWallet as jest.Mock).mockReturnValue({
      data: {
        coin_accounts: [
          {
            coin_currency: "USDT",
            balance: 120, // 120 USDT available
          },
        ],
      },
      isLoading: false,
    });

    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default (10 USDT fee)
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    const amountInput = screen.getByPlaceholderText("translated.enterAmount");
    const addressInput = screen.getByPlaceholderText("translated.enterAddress");

    // Enter valid address
    fireEvent.change(addressInput, {
      target: { value: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F" },
    });

    // Withdraw 100 USDT (total needed: 100 + 10 fee = 110 USDT, 120 available)
    fireEvent.change(amountInput, { target: { value: "100" } });

    // Should not show insufficient balance error
    await waitFor(() => {
      expect(
        screen.queryByText(/Insufficient balance/),
      ).not.toBeInTheDocument();
    });

    // Submit button should be enabled
    await waitFor(() => {
      const submitButton = screen.getByRole("button", {
        name: /Withdraw.*USDT/,
      });
      expect(submitButton).not.toBeDisabled();
    });
  });

  it("validates balance correctly with different network fees", async () => {
    // Set up a wallet with limited balance
    (useWallet as jest.Mock).mockReturnValue({
      data: {
        coin_accounts: [
          {
            coin_currency: "USDT",
            balance: 103, // 103 USDT available
          },
        ],
      },
      isLoading: false,
    });

    render(
      <TestWrapper>
        <WithdrawUSDTPage />
      </TestWrapper>,
    );

    // Wait for ERC20 to be selected by default (10 USDT fee)
    await waitFor(() => {
      expect(screen.getByText("Ethereum (ERC20)")).toBeInTheDocument();
    });

    const amountInput = screen.getByPlaceholderText("translated.enterAmount");
    const addressInput = screen.getByPlaceholderText("translated.enterAddress");

    // Enter valid address
    fireEvent.change(addressInput, {
      target: { value: "0x71C7656EC7ab88b098defB751B7401B5f6d8976F" },
    });

    // Try to withdraw 100 USDT with ERC20 (total needed: 100 + 10 = 110 USDT, but only 103 available)
    fireEvent.change(amountInput, { target: { value: "100" } });

    // Should show insufficient balance error
    await waitFor(() => {
      expect(
        screen.getByText(/Insufficient balance\. Available: 103.*USDT/),
      ).toBeInTheDocument();
    });

    // Change network to TRC20 (2 USDT fee)
    fireEvent.click(screen.getByRole("combobox"));
    fireEvent.click(screen.getAllByText("TRON (TRC20)")[0]);

    // Wait for network change
    await waitFor(() => {
      expect(screen.getByText("TRON (TRC20)")).toBeInTheDocument();
    });

    // Enter valid TRC20 address
    fireEvent.change(addressInput, {
      target: { value: "TKQpQkMWRvTJpQgYrGp8wKgJSHV3DqNHJ3" },
    });

    // Now withdraw 100 USDT with TRC20 (total needed: 100 + 2 = 102 USDT, 103 available)
    fireEvent.change(amountInput, { target: { value: "100" } });

    // Should not show insufficient balance error since 102 <= 103
    await waitFor(() => {
      expect(
        screen.queryByText(/Insufficient balance/),
      ).not.toBeInTheDocument();
    });

    // Submit button should be enabled
    await waitFor(() => {
      const submitButton = screen.getByRole("button", {
        name: /Withdraw.*USDT/,
      });
      expect(submitButton).not.toBeDisabled();
    });
  });
});
