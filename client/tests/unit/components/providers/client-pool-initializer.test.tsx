import { render } from "@testing-library/react";
import { ClientPoolInitializer } from "@/components/providers/client-pool-initializer";
import { useAMMStore } from "@/lib/store/amm-store";

// Mock the store
jest.mock("@/lib/store/amm-store", () => ({
  useAMMStore: jest.fn(),
}));

const mockUseAMMStore = useAMMStore as unknown as jest.Mock;

describe("ClientPoolInitializer", () => {
  const mockInitializePools = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseAMMStore.mockImplementation((selector) => {
      return selector({
        initializePools: mockInitializePools,
      });
    });
  });

  it("should call initializePools on mount", () => {
    render(<ClientPoolInitializer />);
    expect(mockInitializePools).toHaveBeenCalledTimes(1);
  });

  it("should not call initializePools again on rerender", () => {
    const { rerender } = render(<ClientPoolInitializer />);
    expect(mockInitializePools).toHaveBeenCalledTimes(1);

    rerender(<ClientPoolInitializer />);
    expect(mockInitializePools).toHaveBeenCalledTimes(1);
  });

  it("should return null", () => {
    const { container } = render(<ClientPoolInitializer />);
    expect(container.firstChild).toBeNull();
  });
});
