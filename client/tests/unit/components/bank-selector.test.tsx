import { render, screen, fireEvent } from "@testing-library/react";
import { BankSelector } from "@/components/bank-selector";
import { useBanks } from "@/lib/api/hooks/use-banks";
import { HTMLAttributes } from "react";

// Mock the hooks
jest.mock("@/lib/api/hooks/use-banks");

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

// Mock next/image
jest.mock("next/image", () => ({
  __esModule: true,
  default: ({
    src,
    alt,
    ...props
  }: { src: string; alt: string } & HTMLAttributes<HTMLImageElement>) => (
    <img src={src} alt={alt} {...props} />
  ),
}));

// Mock lucide-react icons
jest.mock("lucide-react", () => ({
  Check: () => <div data-testid="check-icon" />,
  ChevronsUpDown: () => <div data-testid="chevrons-icon" />,
  Loader2: () => <div data-testid="loader-icon" />,
  Search: () => <div data-testid="search-icon" />,
}));

// Mock Popover components
jest.mock("@/components/ui/popover", () => ({
  Popover: ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  ),
  PopoverTrigger: ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  ),
  PopoverContent: ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  ),
}));

// Mock data
const mockBanks = [
  {
    id: 1,
    code: "VCB",
    name: "Vietcombank",
    shortName: "VCB",
    short_name: "VCB",
    logo: "/banks/vcb.png",
  },
  {
    id: 2,
    code: "TCB",
    name: "Techcombank",
    shortName: "TCB",
    short_name: "TCB",
    logo: "/banks/tcb.png",
  },
];

describe("BankSelector", () => {
  const defaultProps = {
    value: "",
    onChange: jest.fn(),
    placeholder: "Select a bank",
    disabled: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should render loading state", () => {
    (useBanks as jest.Mock).mockReturnValue({
      data: null,
      isLoading: true,
      error: null,
    });

    render(<BankSelector {...defaultProps} />);
    expect(screen.getByTestId("loader-icon")).toBeInTheDocument();
    expect(screen.getByText("loading")).toBeInTheDocument();
  });

  it("should render error state", () => {
    (useBanks as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error("Failed to fetch banks"),
    });

    render(<BankSelector {...defaultProps} />);
    expect(screen.getByText("fetchError")).toBeInTheDocument();
  });

  it("should render banks and handle selection", () => {
    (useBanks as jest.Mock).mockReturnValue({
      data: { data: mockBanks },
      isLoading: false,
      error: null,
    });

    const onChange = jest.fn();
    render(<BankSelector {...defaultProps} onChange={onChange} />);

    // Check if banks are rendered
    const bankItems = screen.getAllByText("Vietcombank");
    expect(bankItems.length).toBeGreaterThan(0);
    expect(screen.getAllByText("Techcombank").length).toBeGreaterThan(0);

    // Select a bank
    fireEvent.click(bankItems[0]);
    expect(onChange).toHaveBeenCalledWith("VCB", mockBanks[0]);
  });

  it("should show selected bank", () => {
    (useBanks as jest.Mock).mockReturnValue({
      data: { data: mockBanks },
      isLoading: false,
      error: null,
    });

    render(<BankSelector {...defaultProps} value="VCB" />);
    const bankItems = screen.getAllByText("Vietcombank");
    expect(bankItems.length).toBeGreaterThan(0);
    expect(screen.getAllByAltText("Vietcombank").length).toBeGreaterThan(0);
  });

  it("should handle disabled state", () => {
    (useBanks as jest.Mock).mockReturnValue({
      data: { data: mockBanks },
      isLoading: false,
      error: null,
    });

    render(<BankSelector {...defaultProps} disabled={true} />);
    const combobox = screen.getByRole("combobox");
    expect(combobox).toBeDisabled();
  });

  it("should filter banks by search", () => {
    (useBanks as jest.Mock).mockReturnValue({
      data: { data: mockBanks },
      isLoading: false,
      error: null,
    });

    render(<BankSelector {...defaultProps} />);

    // Search for "tech"
    const searchInput = screen.getByPlaceholderText("searchBank");
    fireEvent.change(searchInput, { target: { value: "tech" } });

    // Should only show Techcombank
    expect(screen.getAllByText("Techcombank").length).toBe(1);
    expect(screen.queryByText("Vietcombank")).not.toBeInTheDocument();
  });

  it("should show no results message when search has no matches", () => {
    (useBanks as jest.Mock).mockReturnValue({
      data: { data: mockBanks },
      isLoading: false,
      error: null,
    });

    render(<BankSelector {...defaultProps} />);

    // Search for non-existent bank
    const searchInput = screen.getByPlaceholderText("searchBank");
    fireEvent.change(searchInput, { target: { value: "nonexistent" } });

    expect(screen.getByText("noResults")).toBeInTheDocument();
  });

  it("should handle banks without logos", () => {
    const banksWithoutLogo = [
      {
        id: 1,
        code: "VCB",
        name: "Vietcombank",
        shortName: "VCB",
        short_name: "VCB",
        logo: null,
      },
    ];

    (useBanks as jest.Mock).mockReturnValue({
      data: { data: banksWithoutLogo },
      isLoading: false,
      error: null,
    });

    render(<BankSelector {...defaultProps} value="VCB" />);
    const bankItems = screen.getAllByText("Vietcombank");
    expect(bankItems.length).toBeGreaterThan(0);
    expect(screen.queryByAltText("Vietcombank")).not.toBeInTheDocument();
  });
});
