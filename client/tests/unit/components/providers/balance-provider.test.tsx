import { render, act } from "@testing-library/react";
import { BalanceProvider } from "@/components/providers/balance-provider";
import { useBalanceChannel } from "@/hooks/use-balance-channel";
import { useWallet } from "@/hooks/use-wallet";
import type { BalanceData } from "@/lib/api/balance";
import { useBalanceStore } from "@/lib/store/balance-store";
import { useUserStore } from "@/lib/store/user-store";
import { useQueryClient } from "@tanstack/react-query";

// Mock all the hooks
jest.mock("@/hooks/use-balance-channel");
jest.mock("@/hooks/use-wallet");
jest.mock("@/lib/store/balance-store");
jest.mock("@/lib/store/user-store");
jest.mock("@tanstack/react-query");

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

describe("BalanceProvider", () => {
  // Mock implementations
  const mockSetBalanceData = jest.fn();
  const mockSetBalanceUpdated = jest.fn();
  const mockSetQueryData = jest.fn();
  const mockUser = { id: "123" };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();

    // Mock useUserStore with type assertion
    (useUserStore as unknown as jest.Mock).mockImplementation((selector) =>
      selector({ user: mockUser }),
    );

    // Mock useBalanceStore with type assertion
    (useBalanceStore as unknown as jest.Mock).mockImplementation((selector) =>
      selector({
        setBalanceData: mockSetBalanceData,
        setBalanceUpdated: mockSetBalanceUpdated,
      }),
    );

    // Mock useQueryClient
    (useQueryClient as jest.Mock).mockReturnValue({
      setQueryData: mockSetQueryData,
    });

    // Mock useWallet (empty implementation as it's just called)
    (useWallet as jest.Mock).mockImplementation(() => {});

    // Mock console.log to prevent noise in test output
    jest.spyOn(console, "log").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it("should render children correctly", () => {
    const { getByText } = render(
      <BalanceProvider>
        <div>Test Child</div>
      </BalanceProvider>,
    );

    expect(getByText("Test Child")).toBeInTheDocument();
  });

  it("should initialize balance channel with correct userId", () => {
    render(
      <BalanceProvider>
        <div>Test Child</div>
      </BalanceProvider>,
    );

    expect(useBalanceChannel).toHaveBeenCalledWith(
      expect.objectContaining({
        userId: 123,
      }),
    );
  });

  it("should handle balance updates correctly", () => {
    // Mock balance data
    const mockBalanceData: BalanceData = {
      coin_accounts: [
        {
          coin_currency: "btc",
          balance: 1.5,
          frozen_balance: 0.5,
        },
      ],
      fiat_accounts: [
        {
          currency: "usd",
          balance: 1000,
          frozen_balance: 100,
        },
      ],
    };

    // Capture the onBalanceUpdate callback
    let onBalanceUpdate: (data: BalanceData) => void;
    (useBalanceChannel as jest.Mock).mockImplementation(
      ({ onBalanceUpdate: callback }) => {
        onBalanceUpdate = callback;
      },
    );

    render(
      <BalanceProvider>
        <div>Test Child</div>
      </BalanceProvider>,
    );

    // Simulate balance update
    act(() => {
      onBalanceUpdate(mockBalanceData);
    });

    // Verify balance data is updated
    expect(mockSetBalanceData).toHaveBeenCalledWith(mockBalanceData);
    expect(mockSetBalanceUpdated).toHaveBeenCalledWith(true);
    expect(mockSetQueryData).toHaveBeenCalledWith(
      ["wallet", "123"],
      expect.any(Function),
    );

    // Test the query data transformer
    const transformer = mockSetQueryData.mock.calls[0][1];

    // Test with no existing data
    const newDataResult = transformer(undefined);
    expect(newDataResult).toEqual({
      status: "success",
      data: mockBalanceData,
    });

    // Test with existing data
    const existingData = {
      status: "success",
      data: {
        coin_accounts: [
          {
            coin_currency: "eth",
            balance: 10,
            frozen_balance: 2,
          },
        ],
        fiat_accounts: [
          {
            currency: "eur",
            balance: 500,
            frozen_balance: 50,
          },
        ],
      },
    };
    const updatedDataResult = transformer(existingData);
    expect(updatedDataResult).toEqual({
      status: "success",
      data: mockBalanceData,
    });

    // Verify balance updated flag is reset after timeout
    act(() => {
      jest.advanceTimersByTime(1000);
    });
    expect(mockSetBalanceUpdated).toHaveBeenLastCalledWith(false);
  });

  it("should handle user with no ID correctly", () => {
    // Mock user with no ID
    (useUserStore as unknown as jest.Mock).mockImplementation((selector) =>
      selector({ user: null }),
    );

    render(
      <BalanceProvider>
        <div>Test Child</div>
      </BalanceProvider>,
    );

    expect(useBalanceChannel).toHaveBeenCalledWith(
      expect.objectContaining({
        userId: 0,
      }),
    );
  });

  it("should handle console logging of balance updates", () => {
    const mockBalanceData: BalanceData = {
      coin_accounts: [],
      fiat_accounts: [],
    };

    // Spy on console.log
    const consoleSpy = jest.spyOn(console, "log");

    // Capture the onBalanceUpdate callback
    let onBalanceUpdate: (data: BalanceData) => void;
    (useBalanceChannel as jest.Mock).mockImplementation(
      ({ onBalanceUpdate: callback }) => {
        onBalanceUpdate = callback;
      },
    );

    render(
      <BalanceProvider>
        <div>Test Child</div>
      </BalanceProvider>,
    );

    // Simulate balance update
    act(() => {
      onBalanceUpdate(mockBalanceData);
    });

    // Verify console.log was called with the correct message
    expect(consoleSpy).toHaveBeenCalledWith(
      "💰 Balance updated for user 123:",
      mockBalanceData,
    );
  });
});
