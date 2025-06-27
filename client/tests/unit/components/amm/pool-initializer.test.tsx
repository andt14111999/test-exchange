import { render, act } from "@testing-library/react";
import { PoolInitializer } from "@/components/amm/pool-initializer";
import { useAMMStore } from "@/lib/amm/store";

// Mock the store
jest.mock("@/lib/amm/store", () => ({
  useAMMStore: jest.fn(),
}));

const mockUseAMMStore = useAMMStore as unknown as jest.Mock;

describe("PoolInitializer", () => {
  // Mock for initializePool function
  const mockInitializePool = jest.fn();

  beforeEach(() => {
    // Enable fake timers
    jest.useFakeTimers();

    // Reset all mocks before each test
    jest.clearAllMocks();

    // Setup the store mock
    mockUseAMMStore.mockImplementation((selector) => {
      return selector({
        initializePool: mockInitializePool,
      });
    });
  });

  afterEach(() => {
    // Clean up fake timers
    jest.useRealTimers();
  });

  it("should initialize pools after component mounts", () => {
    render(<PoolInitializer />);

    // Should call initializePool for each pool in INITIAL_POOLS
    expect(mockInitializePool).toHaveBeenCalledTimes(3);

    // Verify calls for each pool
    expect(mockInitializePool).toHaveBeenNthCalledWith(
      1,
      "usdt-vnd",
      "USDT",
      "VND",
      "23500",
      "1000000",
    );

    expect(mockInitializePool).toHaveBeenNthCalledWith(
      2,
      "usdt-php",
      "USDT",
      "PHP",
      "56",
      "1000000",
    );

    expect(mockInitializePool).toHaveBeenNthCalledWith(
      3,
      "usdt-ngn",
      "USDT",
      "NGN",
      "1550",
      "1000000",
    );

    // Verify that calling initializePool again doesn't trigger more calls
    act(() => {
      jest.advanceTimersByTime(1000);
    });
    expect(mockInitializePool).toHaveBeenCalledTimes(3);
  });

  it("should return null (render nothing)", () => {
    const { container } = render(<PoolInitializer />);
    expect(container.firstChild).toBeNull();
  });
});
