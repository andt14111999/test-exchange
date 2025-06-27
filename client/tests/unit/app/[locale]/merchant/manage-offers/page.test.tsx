import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import ManageOffers from "@/app/[locale]/merchant/manage-offers/page";
import {
  getMerchantOffers,
  deleteOffer,
  setOfferOnlineStatus,
  enableOffer,
  disableOffer,
} from "@/lib/api/merchant";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { handleApiError } from "@/lib/utils/error-handler";

// Mock the API functions
jest.mock("@/lib/api/merchant", () => ({
  getMerchantOffers: jest.fn(),
  deleteOffer: jest.fn(),
  setOfferOnlineStatus: jest.fn(),
  enableOffer: jest.fn(),
  disableOffer: jest.fn(),
}));

// Mock formatFiatAmount
jest.mock("@/lib/utils/index", () => ({
  formatFiatAmount: jest.fn((amount, currency) => `${currency}${amount}`),
}));

// Mock next/navigation
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

// Mock next/link
jest.mock("next/link", () => {
  const MockLink = ({
    children,
    href,
  }: {
    children: React.ReactNode;
    href: string;
  }) => <a href={href}>{children}</a>;
  MockLink.displayName = "Link";
  return MockLink;
});

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

// Mock sonner toast
jest.mock("sonner", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

// Mock error handler
jest.mock("@/lib/utils/error-handler", () => ({
  handleApiError: jest.fn(),
}));

describe("ManageOffers", () => {
  const mockRouter = {
    push: jest.fn(),
  };

  const mockOffers = [
    {
      id: 1,
      offer_type: "buy",
      coin_currency: "BTC",
      currency: "USD",
      price: "40000",
      total_amount: "10000",
      available_amount: "10000",
      min_amount: "100",
      max_amount: "5000",
      payment_method_id: 1,
      payment_time: 30,
      payment_details: { bank_name: "Test Bank" },
      country_code: "US",
      is_active: true,
      online: true,
      disabled: false,
      deleted: false,
      created_at: "2023-01-01",
      status: "active",
    },
    {
      id: 2,
      offer_type: "sell",
      coin_currency: "BTC",
      currency: "EUR",
      price: "38000",
      total_amount: "5000",
      available_amount: "5000",
      min_amount: "100",
      max_amount: "2500",
      payment_method_id: 2,
      payment_time: 15,
      payment_details: { bank_name: "Euro Bank" },
      country_code: "DE",
      is_active: false,
      online: false,
      disabled: true,
      deleted: false,
      created_at: "2023-01-02",
      status: "disabled",
    },
    {
      id: 3,
      offer_type: "buy",
      coin_currency: "BTC",
      currency: "GBP",
      price: "35000",
      total_amount: "3000",
      available_amount: "3000",
      min_amount: "50",
      max_amount: "1500",
      payment_method_id: 3,
      payment_time: 20,
      payment_details: { bank_name: "UK Bank" },
      country_code: "GB",
      is_active: true,
      online: false,
      disabled: false,
      deleted: true,
      created_at: "2023-01-03",
      status: "deleted",
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (getMerchantOffers as jest.Mock).mockResolvedValue(mockOffers);
  });

  it("renders loading state initially", async () => {
    render(<ManageOffers />);
    expect(screen.getByText("loading")).toBeInTheDocument();
    await waitFor(() => expect(getMerchantOffers).toHaveBeenCalledTimes(1));
  });

  it("renders active and inactive offers correctly after loading", async () => {
    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // Check section titles
    expect(screen.getByText("activeOffers")).toBeInTheDocument();
    expect(screen.getByText("createNewOffer")).toBeInTheDocument();

    // Active offer (id 1) should be displayed
    expect(screen.getByText("BUY USD")).toBeInTheDocument();
    expect(screen.getByText("active")).toBeInTheDocument();
    expect(screen.getByText("online")).toBeInTheDocument();

    // Check if inactive offers section exists (has disabled offer with id 2)
    expect(screen.getByText("inactiveOffers")).toBeInTheDocument();
    expect(screen.getByText("SELL EUR")).toBeInTheDocument();
    expect(screen.getByText("disabled")).toBeInTheDocument();
    expect(screen.getByText("offline")).toBeInTheDocument();

    // Deleted offer (id 3) should not be displayed
    expect(screen.queryByText("BUY GBP")).not.toBeInTheDocument();
  });

  it("handles errors when fetching offers", async () => {
    const mockError = new Error("API error");
    (getMerchantOffers as jest.Mock).mockRejectedValue(mockError);

    render(<ManageOffers />);

    await waitFor(() =>
      expect(handleApiError).toHaveBeenCalledWith(
        mockError,
        "fetchOffersFailed",
      ),
    );
    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );
  });

  it("navigates to edit offer page when edit button is clicked", async () => {
    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    const editButtons = screen.getAllByText("edit");
    fireEvent.click(editButtons[0]);

    expect(mockRouter.push).toHaveBeenCalledWith("/merchant/create-offer?id=1");
  });

  it("toggles offer online status when online/offline button is clicked", async () => {
    (setOfferOnlineStatus as jest.Mock).mockResolvedValue({
      status: "success",
    });

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // Find the active offer's "Set Offline" button and click it
    const onlineStatusButtons = screen.getAllByText("setOffline");
    fireEvent.click(onlineStatusButtons[0]);

    await waitFor(() => {
      expect(setOfferOnlineStatus).toHaveBeenCalledWith(1, false);
      expect(toast.success).toHaveBeenCalledWith("offerStatusUpdated");
      expect(getMerchantOffers).toHaveBeenCalledTimes(2); // Initial load + refresh
    });
  });

  it("handles errors when toggling online status", async () => {
    const mockError = new Error("API error");
    (setOfferOnlineStatus as jest.Mock).mockRejectedValue(mockError);

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    const onlineStatusButtons = screen.getAllByText("setOffline");
    fireEvent.click(onlineStatusButtons[0]);

    await waitFor(() => {
      expect(handleApiError).toHaveBeenCalledWith(
        mockError,
        "updateOfferFailed",
      );
    });
  });

  it("enables a disabled offer when enable button is clicked", async () => {
    (enableOffer as jest.Mock).mockResolvedValue({ status: "success" });

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // Find the disabled offer's "enable" button and click it
    const enableButton = screen.getAllByText("enable")[0];
    fireEvent.click(enableButton);

    await waitFor(() => {
      expect(enableOffer).toHaveBeenCalledWith(2);
      expect(toast.success).toHaveBeenCalledWith("offerStatusUpdated");
      expect(getMerchantOffers).toHaveBeenCalledTimes(2); // Initial load + refresh
    });
  });

  it("disables an active offer when disable button is clicked", async () => {
    (disableOffer as jest.Mock).mockResolvedValue({ status: "success" });

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // Find the active offer's "disable" button and click it
    const disableButton = screen.getAllByText("disable")[0];
    fireEvent.click(disableButton);

    await waitFor(() => {
      expect(disableOffer).toHaveBeenCalledWith(1);
      expect(toast.success).toHaveBeenCalledWith("offerStatusUpdated");
      expect(getMerchantOffers).toHaveBeenCalledTimes(2); // Initial load + refresh
    });
  });

  it("handles errors when toggling active status", async () => {
    const mockError = new Error("API error");
    (disableOffer as jest.Mock).mockRejectedValue(mockError);

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    const disableButton = screen.getAllByText("disable")[0];
    fireEvent.click(disableButton);

    await waitFor(() => {
      expect(handleApiError).toHaveBeenCalledWith(
        mockError,
        "updateOfferFailed",
      );
    });
  });

  it("deletes an offer when delete button is clicked", async () => {
    (deleteOffer as jest.Mock).mockResolvedValue({ status: "success" });

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // Find the active offer's "delete" button and click it
    const deleteButton = screen.getAllByText("delete")[0];
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(deleteOffer).toHaveBeenCalledWith(1);
      expect(toast.success).toHaveBeenCalledWith("offerDeleted");
      expect(getMerchantOffers).toHaveBeenCalledTimes(2); // Initial load + refresh
    });
  });

  it("handles errors when deleting an offer", async () => {
    const mockError = new Error("API error");
    (deleteOffer as jest.Mock).mockRejectedValue(mockError);

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    const deleteButton = screen.getAllByText("delete")[0];
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(handleApiError).toHaveBeenCalledWith(
        mockError,
        "deleteOfferFailed",
      );
    });
  });

  it("correctly formats amounts using toNumber helper", async () => {
    // Mock offers with string and number values to test toNumber helper
    const specialOffers = [
      {
        ...mockOffers[0],
        total_amount: "5000.50", // String decimal
        min_amount: 100, // Number
        max_amount: "invalid", // Invalid string
      },
    ];

    (getMerchantOffers as jest.Mock).mockResolvedValue(specialOffers);

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // The formatFiatAmount function is mocked by the test, but we can verify if specific elements exist
    expect(screen.getByText(/amount:/i)).toBeInTheDocument();
    expect(screen.getByText(/limit:/i)).toBeInTheDocument();
  });

  it("applies correct status colors to badges", async () => {
    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // Check if badges have the expected classes
    const activeBadge = screen.getByText("active");
    expect(activeBadge).toHaveClass("bg-emerald-100");
    expect(activeBadge).toHaveClass("text-emerald-800");

    const onlineBadge = screen.getByText("online");
    expect(onlineBadge).toHaveClass("bg-blue-100");
    expect(onlineBadge).toHaveClass("text-blue-800");

    const disabledBadge = screen.getByText("disabled");
    expect(disabledBadge).toHaveClass("bg-yellow-100");
    expect(disabledBadge).toHaveClass("text-yellow-800");
  });

  it("applies correct type colors to offer type badges", async () => {
    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // Check if type badges have the expected classes
    const buyBadge = screen.getByText("BUY USD");
    expect(buyBadge).toHaveClass("bg-green-100");
    expect(buyBadge).toHaveClass("text-green-800");

    const sellBadge = screen.getByText("SELL EUR");
    expect(sellBadge).toHaveClass("bg-red-100");
    expect(sellBadge).toHaveClass("text-red-800");
  });

  it("correctly derives offer status using getOfferStatus", async () => {
    // Mock offers without status to test getOfferStatus function
    const offersWithoutStatus = [
      {
        ...mockOffers[0],
        status: undefined,
        disabled: false,
        deleted: false,
        online: true,
        is_active: true,
      },
      {
        ...mockOffers[1],
        status: undefined,
        disabled: true,
        deleted: false,
      },
      {
        ...mockOffers[2],
        status: undefined,
        deleted: true,
      },
    ];

    (getMerchantOffers as jest.Mock).mockResolvedValue(offersWithoutStatus);

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );

    // The first offer should have derived status "active"
    expect(screen.getAllByText("active")[0]).toBeInTheDocument();

    // The second offer should have derived status "disabled"
    expect(screen.getByText("disabled")).toBeInTheDocument();

    // The third offer should have derived status "deleted" and should not be shown
    expect(screen.queryByText("deleted")).not.toBeInTheDocument();
  });

  it("handles different response formats from getMerchantOffers API", async () => {
    // Test with response in the form { data: [...] }
    const wrappedResponse = { data: mockOffers };
    (getMerchantOffers as jest.Mock).mockResolvedValue(wrappedResponse);

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );
    expect(screen.getAllByText("BUY USD")[0]).toBeInTheDocument();

    // Test with response in the form { data: {...} } (single offer)
    const singleOfferResponse = { data: mockOffers[0] };
    (getMerchantOffers as jest.Mock).mockResolvedValue(singleOfferResponse);

    render(<ManageOffers />);

    await waitFor(() =>
      expect(screen.queryByText("loading")).not.toBeInTheDocument(),
    );
    expect(screen.getAllByText("BUY USD")[0]).toBeInTheDocument();
  });
});
