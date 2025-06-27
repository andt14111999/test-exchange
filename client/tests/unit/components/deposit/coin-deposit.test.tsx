import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { CoinDeposit } from "@/components/deposit/coin-deposit";
import { useCoinAddress } from "@/hooks/use-coin-address";
import { mockNetworksByToken } from "@/lib/constants/mock-data";
import "@testing-library/jest-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// Mock the hooks
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string, params?: Record<string, unknown>) => {
    switch (key) {
      case "depositAddress":
        return "Deposit Address";
      case "networkSelection.title":
        return "Select Network";
      case "networkSelection.description":
        return "Choose the network for your deposit";
      case "generateAddress.button":
        return "Generate Address";
      case "generateAddress.generating":
        return "Generating...";
      case "generateAddress.message":
        return "Click to generate a deposit address.";
      case "error.message":
        return "An error occurred";
      case "error.retry":
        return "Retry";
      case "important.title":
        return "Important Notes";
      case "important.network":
        return params ? `Network: ${params.network}` : "Network";
      case "important.minimum":
        return params ? `Minimum: 0.0001 ${params.coin}` : "Minimum Amount";
      case "important.confirmation":
        return "Confirmation required";
      case "maintenance":
        return "Maintenance";
      default:
        return key;
    }
  },
}));

jest.mock("@/hooks/use-coin-address", () => ({
  useCoinAddress: jest.fn().mockReturnValue({
    data: null,
    isLoading: false,
    error: null,
    generateAddress: jest.fn(),
    isGenerating: false,
    generateError: null,
  }),
}));

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: jest.fn(),
  },
});

// Mock window.location.reload
const mockReload = jest.fn();
Object.defineProperty(window, "location", {
  value: { reload: mockReload },
  writable: true,
});

// Mock fetchCoinSettings to avoid network error and loading forever
jest.mock("@/lib/api/coins", () => ({
  fetchCoinSettings: jest.fn(() =>
    Promise.resolve([
      {
        id: 1,
        currency: "BTC",
        name: "Bitcoin",
        is_active: true,
        deposit_enabled: true,
        layers: [{ layer: "btc", deposit_enabled: true }],
      },
      {
        id: 2,
        currency: "ETH",
        name: "Ethereum",
        is_active: true,
        deposit_enabled: true,
        layers: [{ layer: "eth", deposit_enabled: true }],
      },
    ]),
  ),
}));

// Helper to wrap component in QueryClientProvider
function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient();
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
  );
}

describe("CoinDeposit", () => {
  const defaultProps = {
    coin: "btc",
    networks: mockNetworksByToken.btc,
    title: "Bitcoin Deposit",
    description: "Deposit Bitcoin to your account",
  };

  const mockAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh";

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders initial state correctly", async () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: null,
      generateAddress: jest.fn(),
      isGenerating: false,
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);

    // Check title and description
    await waitFor(() => {
      expect(screen.getByText(defaultProps.title)).toBeInTheDocument();
      expect(screen.getByText(defaultProps.description)).toBeInTheDocument();
    });

    // Check network selection
    expect(screen.getByText("Select Network")).toBeInTheDocument();
    expect(
      screen.getByText("Choose the network for your deposit"),
    ).toBeInTheDocument();

    // Check that the first network (Bitcoin) is selected by default
    expect(screen.getByText("Bitcoin (BTC)")).toBeInTheDocument();

    // Check generate address button
    expect(
      screen.getByRole("button", { name: "Generate Address" }),
    ).toBeInTheDocument();

    // Check important notes section
    expect(screen.getByText("Important Notes")).toBeInTheDocument();
    expect(screen.getByText("Network: Bitcoin (BTC)")).toBeInTheDocument();
    expect(screen.getByText("Minimum: 0.0001 btc")).toBeInTheDocument();
    expect(screen.getByText("Confirmation required")).toBeInTheDocument();
  });

  it("displays loading skeleton when loading", () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      isLoading: true,
      error: null,
      data: null,
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);
    expect(screen.getAllByRole("status")).toHaveLength(2);
  });

  it("displays error state when there is an error", async () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      isLoading: false,
      error: new Error("Failed to fetch"),
      data: null,
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText("An error occurred")).toBeInTheDocument();
      expect(screen.getByText("Retry")).toBeInTheDocument();
    });
  });

  it("displays address and QR code when address is available", async () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      isLoading: false,
      error: null,
      data: { data: { address: mockAddress } },
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText(mockAddress)).toBeInTheDocument();
    });
    const qrCode = screen.getByRole("img", {
      name: "QR code for deposit address",
    });
    expect(qrCode).toBeInTheDocument();
  });

  it("handles address copy functionality", async () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      isLoading: false,
      error: null,
      data: { data: { address: mockAddress } },
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText(mockAddress)).toBeInTheDocument();
    });
    const copyButton = screen.getByRole("button", { name: "Copy address" });
    fireEvent.click(copyButton);
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(mockAddress);
  });

  it("handles network selection", async () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      isLoading: false,
      error: null,
      data: { data: { address: mockAddress } },
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText("Bitcoin (BTC)")).toBeInTheDocument();
    });
  });

  it("displays generate address button when no address is available", async () => {
    const generateAddress = jest.fn();
    (useCoinAddress as jest.Mock).mockReturnValue({
      isLoading: false,
      error: null,
      data: null,
      generateAddress,
      isGenerating: false,
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText("Generate Address")).toBeInTheDocument();
    });
    const generateButton = screen.getByText("Generate Address");
    fireEvent.click(generateButton);
    expect(generateAddress).toHaveBeenCalled();
  });

  it("displays generating state when address is being generated", async () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      isLoading: false,
      error: null,
      data: null,
      generateAddress: jest.fn(),
      isGenerating: true,
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText("Generating...")).toBeInTheDocument();
    });
  });

  it("handles retry functionality", async () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      isLoading: false,
      error: new Error("Failed to fetch"),
      data: null,
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);
    await waitFor(() => {
      expect(screen.getByText("Retry")).toBeInTheDocument();
    });
    const retryButton = screen.getByText("Retry");
    fireEvent.click(retryButton);
    expect(mockReload).toHaveBeenCalled();
  });

  it("displays important notes with correct network and coin", async () => {
    (useCoinAddress as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: null,
      generateAddress: jest.fn(),
      isGenerating: false,
    });

    renderWithClient(<CoinDeposit {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText("Important Notes")).toBeInTheDocument();
      expect(screen.getByText("Network: Bitcoin (BTC)")).toBeInTheDocument();
      expect(screen.getByText("Minimum: 0.0001 btc")).toBeInTheDocument();
      expect(screen.getByText("Confirmation required")).toBeInTheDocument();
    });
  });
});
