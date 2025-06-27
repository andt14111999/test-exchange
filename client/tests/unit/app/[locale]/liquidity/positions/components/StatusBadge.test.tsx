import { render, screen } from "@testing-library/react";
import StatusBadge from "@/app/[locale]/liquidity/positions/components/StatusBadge";
import { useTranslations } from "next-intl";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

describe("StatusBadge", () => {
  beforeEach(() => {
    // Reset all mocks before each test
    jest.clearAllMocks();

    // Mock translations
    (useTranslations as jest.Mock).mockReturnValue((key: string) => {
      const translations: Record<string, string> = {
        statusOpen: "Open",
        statusPending: "Pending",
        statusClosed: "Closed",
        statusError: "Error",
      };
      return translations[key] || key;
    });
  });

  it("renders open status correctly", () => {
    render(<StatusBadge status="open" />);
    const badge = screen.getByText("Open");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-green-500");
  });

  it("renders pending status correctly", () => {
    render(<StatusBadge status="pending" />);
    const badge = screen.getByText("Pending");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-yellow-500");
  });

  it("renders closed status correctly", () => {
    render(<StatusBadge status="closed" />);
    const badge = screen.getByText("Closed");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-gray-500");
  });

  it("renders error status correctly", () => {
    render(<StatusBadge status="error" />);
    const badge = screen.getByText("Error");
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass("bg-red-500");
  });

  it("renders default status correctly", () => {
    render(<StatusBadge status="unknown" />);
    const badge = screen.getByText("unknown");
    expect(badge).toBeInTheDocument();
    expect(badge).not.toHaveClass("bg-green-500");
    expect(badge).not.toHaveClass("bg-yellow-500");
    expect(badge).not.toHaveClass("bg-gray-500");
    expect(badge).not.toHaveClass("bg-red-500");
  });
});
