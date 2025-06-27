import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import MerchantMintFiat from "@/app/[locale]/merchant/mint-fiat/page";
import { toast } from "sonner";
import {
  getFiatMints,
  createFiatMint,
  cancelFiatMint,
} from "@/lib/api/merchant";
import { getExchangeRates } from "@/lib/api/settings";
import { useWallet } from "@/hooks/use-wallet";
import { getTokenBalance, fetchCoins } from "@/lib/api/coins";
import { useQuery } from "@tanstack/react-query";

// Mock data
const mockFiatMints = [
  {
    id: 1,
    usdt_amount: "100",
    fiat_amount: "2300000",
    fiat_currency: "VND",
    status: "active",
    created_at: "2023-01-01T00:00:00Z",
    updated_at: "2023-01-01T00:00:00Z",
  },
  {
    id: 2,
    usdt_amount: "50",
    fiat_amount: "1150000",
    fiat_currency: "VND",
    status: "cancelled",
    created_at: "2023-01-02T00:00:00Z",
    updated_at: "2023-01-02T00:00:00Z",
  },
];

const mockExchangeRates = {
  usdt_to_vnd: 23000,
  usdt_to_php: 50,
  usdt_to_ngn: 410,
};

const mockWalletData = {
  usdt: { balance: "1000" },
};

const mockCoinsData = [{ symbol: "usdt", name: "Tether", balance: "1000" }];

// Mocking dependencies
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  ),
}));

jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));

jest.mock("@/lib/api/merchant", () => ({
  getFiatMints: jest.fn(),
  createFiatMint: jest.fn(),
  cancelFiatMint: jest.fn(),
}));

jest.mock("@/lib/api/settings", () => ({
  getExchangeRates: jest.fn(),
}));

jest.mock("@/hooks/use-wallet", () => ({
  useWallet: jest.fn(),
}));

jest.mock("@/lib/api/coins", () => ({
  fetchCoins: jest.fn(),
  getTokenBalance: jest.fn(),
}));

jest.mock("@tanstack/react-query", () => ({
  useQuery: jest.fn(),
}));

jest.mock("@/hooks/use-fiat-mint-channel", () => ({
  useFiatMintChannel: jest.fn(),
}));

jest.mock("sonner", () => ({
  toast: {
    error: jest.fn(),
    success: jest.fn(),
  },
}));

describe("MerchantMintFiat Page", () => {
  const user = userEvent.setup();

  beforeEach(() => {
    jest.clearAllMocks();
    (getFiatMints as jest.Mock).mockResolvedValue(mockFiatMints);
    (getExchangeRates as jest.Mock).mockResolvedValue(mockExchangeRates);
    (useWallet as jest.Mock).mockReturnValue({ data: mockWalletData });
    (useQuery as jest.Mock).mockReturnValue({ data: mockCoinsData });
    (getTokenBalance as jest.Mock).mockReturnValue("1000");
  });

  it("renders the main title", () => {
    render(<MerchantMintFiat />);

    const title = screen.getByRole("heading", {
      name: /createTitle/i,
    });
    expect(title).toBeInTheDocument();
  });

  it("renders the create form with all inputs", () => {
    render(<MerchantMintFiat />);

    expect(
      screen.getByPlaceholderText(/usdtAmountPlaceholder/i),
    ).toBeInTheDocument();
    expect(screen.getByRole("combobox")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /create/i })).toBeInTheDocument();
  });

  it("displays loading state initially", () => {
    render(<MerchantMintFiat />);
    expect(screen.getByText("loading")).toBeInTheDocument();
  });

  it("displays list title", () => {
    render(<MerchantMintFiat />);
    expect(screen.getByText("listTitle")).toBeInTheDocument();
  });

  it("displays current USDT balance", () => {
    render(<MerchantMintFiat />);
    expect(screen.getByText(/currentBalance.*1000 USDT/)).toBeInTheDocument();
  });

  it("shows validation error for empty form submission", async () => {
    render(<MerchantMintFiat />);

    const submitButton = screen.getByRole("button", { name: /create/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("validationError");
    });
  });

  it("handles form input changes", async () => {
    render(<MerchantMintFiat />);

    const amountInput = screen.getByPlaceholderText(/usdtAmountPlaceholder/i);
    await user.type(amountInput, "100");

    expect(amountInput).toHaveValue("100");
  });

  it("displays estimated fiat amount when amount is entered", async () => {
    render(<MerchantMintFiat />);

    const amountInput = screen.getByPlaceholderText(/usdtAmountPlaceholder/i);
    await user.type(amountInput, "100");

    await waitFor(() => {
      expect(screen.getByText(/estimatedFiatAmount/)).toBeInTheDocument();
    });
  });

  it("shows insufficient balance error when amount exceeds balance", async () => {
    (getTokenBalance as jest.Mock).mockReturnValue("50");
    render(<MerchantMintFiat />);

    const amountInput = screen.getByPlaceholderText(/usdtAmountPlaceholder/i);
    await user.type(amountInput, "100");

    await waitFor(() => {
      expect(screen.getByText("insufficientBalance")).toBeInTheDocument();
    });

    const submitButton = screen.getByRole("button", { name: /create/i });
    expect(submitButton).toBeDisabled();
  });

  it("handles successful form submission", async () => {
    (createFiatMint as jest.Mock).mockResolvedValue({
      data: {
        id: 3,
        usdt_amount: "200",
        fiat_amount: "4600000",
        fiat_currency: "VND",
        status: "active",
        created_at: "2023-01-03T00:00:00Z",
        updated_at: "2023-01-03T00:00:00Z",
      },
    });

    render(<MerchantMintFiat />);

    const amountInput = screen.getByPlaceholderText(/usdtAmountPlaceholder/i);
    const submitButton = screen.getByRole("button", { name: /create/i });

    await user.type(amountInput, "200");
    await user.click(submitButton);

    await waitFor(() => {
      expect(createFiatMint).toHaveBeenCalledWith("200", "VND");
      expect(toast.success).toHaveBeenCalledWith("creationSuccess");
    });
  });

  it("handles form submission error", async () => {
    (createFiatMint as jest.Mock).mockRejectedValue(new Error("Network error"));

    render(<MerchantMintFiat />);

    const amountInput = screen.getByPlaceholderText(/usdtAmountPlaceholder/i);
    const submitButton = screen.getByRole("button", { name: /create/i });

    await user.type(amountInput, "100");
    await user.click(submitButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("creationError Network error");
    });
  });

  it("handles form submission with message error", async () => {
    (createFiatMint as jest.Mock).mockResolvedValue({
      message: "Validation failed",
    });

    render(<MerchantMintFiat />);

    const amountInput = screen.getByPlaceholderText(/usdtAmountPlaceholder/i);
    const submitButton = screen.getByRole("button", { name: /create/i });

    await user.type(amountInput, "100");
    await user.click(submitButton);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        "creationError Validation failed",
      );
    });
  });

  it("shows creating state during form submission", async () => {
    (createFiatMint as jest.Mock).mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100)),
    );

    render(<MerchantMintFiat />);

    const amountInput = screen.getByPlaceholderText(/usdtAmountPlaceholder/i);
    const submitButton = screen.getByRole("button", { name: /create/i });

    await user.type(amountInput, "100");
    await user.click(submitButton);

    expect(
      screen.getByRole("button", { name: /creating/i }),
    ).toBeInTheDocument();
  });

  it("displays default currency selection", () => {
    render(<MerchantMintFiat />);

    const currencySelect = screen.getByRole("combobox");
    expect(currencySelect).toHaveTextContent("VND");
  });

  it("renders page title", () => {
    render(<MerchantMintFiat />);
    expect(screen.getByText("title")).toBeInTheDocument();
  });

  it("renders form labels", () => {
    render(<MerchantMintFiat />);
    expect(screen.getByText("usdtAmount")).toBeInTheDocument();
    expect(screen.getByText("fiatCurrency")).toBeInTheDocument();
  });

  it("calls getFiatMints on component mount", () => {
    render(<MerchantMintFiat />);
    expect(getFiatMints).toHaveBeenCalled();
  });

  it("calls getExchangeRates on component mount", () => {
    render(<MerchantMintFiat />);
    expect(getExchangeRates).toHaveBeenCalled();
  });

  it("handles fetch fiat mints error", async () => {
    (getFiatMints as jest.Mock).mockRejectedValue(new Error("Fetch error"));
    render(<MerchantMintFiat />);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        "fetchError: Error: Fetch error",
      );
    });
  });
});
