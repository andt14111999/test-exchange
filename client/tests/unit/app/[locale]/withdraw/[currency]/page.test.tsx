import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import userEvent from "@testing-library/user-event";
import WithdrawPage from "@/app/[locale]/withdraw/[currency]/page";
import { useWallet } from "@/hooks/use-wallet";
import { useBankAccounts } from "@/lib/api/hooks/use-bank-accounts";
import { getOffers } from "@/lib/api/merchant";
import { useUserStore } from "@/lib/store/user-store";
import { useParams, useRouter } from "next/navigation";
import { BankAccount } from "@/lib/api/bank-accounts";
import { useOffers } from "@/lib/api/hooks/use-offers";

const user = userEvent.setup();
const push = jest.fn();

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

// Mock all the hooks and modules
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => {
    const t = (key: string) => key;
    t.rich = (key: string) => key;
    return t;
  }),
}));

jest.mock("next-intl/navigation", () => ({
  createNavigation: jest.fn(() => ({
    Link: "mocked-link",
    redirect: jest.fn(),
    usePathname: jest.fn(),
    useRouter: jest.fn(),
  })),
}));

jest.mock("next-intl/server", () => ({
  getFormatter: jest.fn(),
  getLocale: jest.fn(),
  getMessages: jest.fn(),
  getNow: jest.fn(),
  getRequestConfig: jest.fn(),
  getTimeZone: jest.fn(),
  getTranslations: jest.fn(),
  setRequestLocale: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useRouter: jest.fn(() => ({
    push,
  })),
  useParams: jest.fn(),
}));

jest.mock("@/hooks/use-wallet", () => ({
  useWallet: jest.fn(() => ({
    data: {
      fiat_accounts: [
        {
          currency: "vnd",
          balance: 1000000,
        },
      ],
    },
  })),
}));

jest.mock("@/lib/api/hooks/use-bank-accounts", () => ({
  useBankAccounts: jest.fn(() => ({
    isLoading: false,
  })),
}));

jest.mock("@/lib/api/hooks/use-offers", () => ({
  useOffers: jest.fn(() => ({
    data: [],
    isLoading: false,
  })),
}));

jest.mock("@/lib/api/merchant", () => ({
  getOffers: jest.fn(() => [
    {
      id: 1,
      currency: "vnd",
      country_code: "VN",
      offer_type: "buy",
      is_active: true,
      online: true,
      user_id: 2,
      merchant_display_name: "Merchant 1",
      price: 1,
      min_amount: 100000,
      max_amount: 1000000,
      payment_details: {
        bank_name: "Test Bank",
        account_number: "123456789",
        account_holder_name: "Test Account",
      },
    },
  ]),
}));

jest.mock("@/lib/api/trades", () => ({
  createTrade: jest.fn(() => Promise.resolve({ id: "123" })),
}));

jest.mock("@/lib/store/user-store", () => ({
  useUserStore: jest.fn(() => ({
    user: {
      id: 1,
    },
  })),
}));

const mockToast = jest.fn();
jest.mock("@/components/ui/use-toast", () => ({
  useToast: jest.fn(() => ({
    toast: mockToast,
  })),
}));

jest.mock("@/components/bank-account-selector", () => ({
  BankAccountSelector: ({
    onAccountSelect,
  }: {
    onAccountSelect: (account: BankAccount) => void;
  }) => (
    <div data-testid="bank-account-selector">
      <button
        data-testid="select-bank-account-button"
        onClick={() =>
          onAccountSelect({
            id: "123",
            bank_name: "Test Bank",
            account_name: "Test Account",
            account_number: "12345678",
            branch: "Test Branch",
            country_code: "VN",
            is_primary: true,
            verified: true,
            created_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
          })
        }
      >
        Select Bank Account
      </button>
    </div>
  ),
}));

// Mock sonner toast
jest.mock("sonner", () => ({
  toast: {
    error: jest.fn(),
    success: jest.fn(),
  },
}));

describe("WithdrawPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    // Default mock implementations
    (useParams as jest.Mock).mockReturnValue({ currency: "vnd" });
    (useRouter as jest.Mock).mockReturnValue({ push });
    (useWallet as jest.Mock).mockReturnValue({
      data: {
        fiat_accounts: [
          {
            currency: "vnd",
            balance: 1000000,
          },
        ],
      },
      isLoading: false,
    });
    (useBankAccounts as jest.Mock).mockReturnValue({
      data: [],
      isLoading: false,
    });
    (useUserStore as unknown as jest.Mock).mockReturnValue({ user: { id: 1 } });
    (getOffers as jest.Mock).mockResolvedValue([
      {
        id: 1,
        currency: "vnd",
        country_code: "VN",
        offer_type: "buy",
        is_active: true,
        online: true,
        user_id: 2,
        merchant_display_name: "Merchant 1",
        price: 1,
        min_amount: 100000,
        max_amount: 1000000,
        payment_details: {
          bank_name: "Test Bank",
          account_number: "123456789",
          account_holder_name: "Test Account",
        },
      },
    ]);
    (
      jest.requireMock("@/lib/api/trades").createTrade as jest.Mock
    ).mockResolvedValue({ id: "123" });
  });

  it("renders withdrawal type buttons and form fields", () => {
    render(
      <TestWrapper>
        <WithdrawPage />
      </TestWrapper>,
    );
    // Check for the actual elements that exist in the fiat currency page
    expect(screen.getByText("title")).toBeInTheDocument();
    expect(screen.getByText("balance")).toBeInTheDocument();
    expect(screen.getByText("amountToWithdraw")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("0 VND")).toBeInTheDocument();
    expect(screen.getByText("withdrawalAccount")).toBeInTheDocument();
    expect(screen.getByTestId("bank-account-selector")).toBeInTheDocument();
    expect(screen.getByText("continue")).toBeInTheDocument();
  });

  it("shows error if no bank account is selected", async () => {
    render(
      <TestWrapper>
        <WithdrawPage />
      </TestWrapper>,
    );
    // Enter amount
    const amountInput = screen.getByPlaceholderText("0 VND");
    await user.type(amountInput, "100000");
    // Click continue
    const continueButton = screen.getByText("continue");
    await user.click(continueButton);
    expect(mockToast).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "missingBankAccount",
      }),
    );
  });

  it("shows error if no amount is entered", async () => {
    render(
      <TestWrapper>
        <WithdrawPage />
      </TestWrapper>,
    );
    // Select bank account
    const selectBankButton = screen.getByTestId("select-bank-account-button");
    await user.click(selectBankButton);
    // Click continue
    const continueButton = screen.getByText("continue");
    await user.click(continueButton);
    expect(mockToast).toHaveBeenCalledWith(
      expect.objectContaining({
        title: "missingAmount",
      }),
    );
  });

  it("shows error if amount is below minimum", async () => {
    render(
      <TestWrapper>
        <WithdrawPage />
      </TestWrapper>,
    );
    // Select bank account
    const selectBankButton = screen.getByTestId("select-bank-account-button");
    await user.click(selectBankButton);
    // Enter invalid amount
    const amountInput = screen.getByPlaceholderText("0 VND");
    await user.type(amountInput, "5000"); // Below minimum
    // Click continue
    const continueButton = screen.getByText("continue");
    await user.click(continueButton);
    // Error message should be shown in the UI
    expect(screen.getByText("amountTooLow")).toBeInTheDocument();
  });

  it("shows error if amount is above balance", async () => {
    render(
      <TestWrapper>
        <WithdrawPage />
      </TestWrapper>,
    );
    // Select bank account
    const selectBankButton = screen.getByTestId("select-bank-account-button");
    await user.click(selectBankButton);
    // Enter amount above balance
    const amountInput = screen.getByPlaceholderText("0 VND");
    await user.type(amountInput, "999999999");
    // Click continue
    const continueButton = screen.getByText("continue");
    await user.click(continueButton);
    // Error message should be shown in the UI
    expect(
      screen.getByText("insufficientBalanceDescription"),
    ).toBeInTheDocument();
  });

  it("shows offers section if offers are available after continue", async () => {
    // Mock offers data
    (useOffers as jest.Mock).mockReturnValue({
      data: [
        {
          id: 1,
          merchant_name: "Merchant 1",
          rate: 1.0,
          min_amount: 100,
          max_amount: 1000,
        },
      ],
      isLoading: false,
    });

    render(
      <TestWrapper>
        <WithdrawPage />
      </TestWrapper>,
    );

    // Fill form
    const amountInput = screen.getByPlaceholderText("0 VND");
    await user.type(amountInput, "100000");

    // Select bank account
    const bankAccountButton = screen.getByTestId("select-bank-account-button");
    await user.click(bankAccountButton);

    // Click continue
    const continueButton = screen.getByText("continue");
    await user.click(continueButton);

    // Wait for offers to load
    await waitFor(() => {
      expect(screen.getByText("selectMerchant")).toBeInTheDocument();
    });

    // Check that offers are displayed
    expect(screen.getByText("Merchant 1")).toBeInTheDocument();
  });

  it("shows toast and empty offers section if no offers are available", async () => {
    // Mock empty offers
    (useOffers as jest.Mock).mockReturnValue({
      data: [],
      isLoading: false,
    });

    // Mock getOffers to return empty array
    (getOffers as jest.Mock).mockResolvedValue([]);

    render(
      <TestWrapper>
        <WithdrawPage />
      </TestWrapper>,
    );

    // Fill form
    const amountInput = screen.getByPlaceholderText("0 VND");
    await user.type(amountInput, "100000");

    // Select bank account
    const bankAccountButton = screen.getByTestId("select-bank-account-button");
    await user.click(bankAccountButton);

    // Click continue
    const continueButton = screen.getByText("continue");
    await user.click(continueButton);

    // Wait for toast to be called
    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(
        expect.objectContaining({
          title: "noOffersForCurrency",
        }),
      );
    });

    // Wait for empty offers section to appear
    await waitFor(() => {
      expect(screen.getByText("selectMerchant")).toBeInTheDocument();
      expect(screen.getByText("noOffersAvailable")).toBeInTheDocument();
    });
  });
});
