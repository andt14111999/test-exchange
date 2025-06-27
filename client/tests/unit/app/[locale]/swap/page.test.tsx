import { render, screen } from "@testing-library/react";
import Swap from "@/app/[locale]/swap/page";
import { SwapContainer } from "@/app/[locale]/swap/components/swap-container";

// Mock the SwapContainer component
jest.mock("@/app/[locale]/swap/components/swap-container", () => ({
  SwapContainer: jest.fn(() => (
    <div data-testid="swap-container">Swap Container</div>
  )),
}));

describe("Swap Page", () => {
  it("renders the SwapContainer component", () => {
    render(<Swap />);

    // Check if SwapContainer is rendered
    expect(screen.getByTestId("swap-container")).toBeInTheDocument();

    // Verify SwapContainer was called
    expect(SwapContainer).toHaveBeenCalledTimes(1);
  });
});
