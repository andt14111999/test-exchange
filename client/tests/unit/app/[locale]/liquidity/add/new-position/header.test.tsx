import { render, screen, fireEvent } from "@testing-library/react";
import { Header } from "../../../../../../../src/app/[locale]/liquidity/add/new-position/header";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";

// Mock the dependencies
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

describe("Header", () => {
  const mockRouter = {
    back: jest.fn(),
  };
  const mockTranslations = jest.fn((key) => key);

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (useTranslations as jest.Mock).mockReturnValue(mockTranslations);
  });

  it("renders the header with correct title", () => {
    render(<Header />);

    // Check if the title is rendered with correct translation key
    expect(screen.getByText("liquidity.addLiquidity")).toBeInTheDocument();
  });

  it("calls router.back when back button is clicked", () => {
    render(<Header />);

    // Find and click the back button
    const backButton = screen.getByRole("button");
    fireEvent.click(backButton);

    // Check if router.back was called
    expect(mockRouter.back).toHaveBeenCalledTimes(1);
  });

  it("renders with correct styling classes", () => {
    render(<Header />);

    // Check if the container has correct classes
    const container = screen.getByRole("button").parentElement;
    expect(container).toHaveClass(
      "flex",
      "items-center",
      "space-x-2",
      "p-4",
      "border-b",
    );

    // Check if the back button has correct classes
    const backButton = screen.getByRole("button");
    expect(backButton).toHaveClass(
      "text-muted-foreground",
      "hover:text-foreground",
    );

    // Check if the title has correct classes
    const title = screen.getByText("liquidity.addLiquidity");
    expect(title).toHaveClass("text-xl", "font-semibold");
  });

  it("translates the title correctly", () => {
    // Mock a specific translation
    mockTranslations.mockImplementation((key) => {
      if (key === "liquidity.addLiquidity") return "Add Liquidity";
      return key;
    });

    render(<Header />);

    // Check if the translated title is rendered
    expect(screen.getByText("Add Liquidity")).toBeInTheDocument();
  });
});
