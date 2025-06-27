import { render, screen } from "@testing-library/react";
import SwapHistoryPage from "@/app/[locale]/swap/history/page";
import { SwapHistory } from "@/app/[locale]/swap/components/swap-history";

// Mock the SwapHistory component
jest.mock("@/app/[locale]/swap/components/swap-history", () => ({
  SwapHistory: jest.fn(() => (
    <div data-testid="swap-history">Mocked SwapHistory</div>
  )),
}));

describe("SwapHistoryPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders SwapHistory component within a container", () => {
    render(<SwapHistoryPage />);

    // Check if the container has the correct classes
    const container = screen.getByTestId("swap-history").parentElement;
    expect(container).toHaveClass("container", "max-w-5xl", "mx-auto", "py-8");

    // Check if SwapHistory component is rendered
    expect(screen.getByTestId("swap-history")).toBeInTheDocument();
    expect(screen.getByText("Mocked SwapHistory")).toBeInTheDocument();

    // Verify SwapHistory was called
    expect(SwapHistory).toHaveBeenCalled();
  });
});
