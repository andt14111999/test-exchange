import { render, screen, fireEvent } from "@testing-library/react";
import { TokenSelect } from "@/app/[locale]/swap/components/token-select";

// Mock the shadcn/ui components
jest.mock("@/components/ui/select", () => {
  // Simulate select item click
  const onItemClick = jest.fn();

  return {
    onItemClick, // Export for testing
    Select: jest.fn(({ children, value, onValueChange, disabled }) => (
      <div
        data-testid="select"
        data-value={value}
        data-disabled={disabled}
        // Add dummy handler for test instrumentation
        onClick={() => onValueChange && onValueChange("TEST_VALUE")}
      >
        {children}
      </div>
    )),
    SelectTrigger: jest.fn(({ children, className }) => (
      <div data-testid="select-trigger" className={className}>
        {children}
      </div>
    )),
    SelectValue: jest.fn(({ placeholder }) => (
      <div data-testid="select-value" data-placeholder={placeholder}></div>
    )),
    SelectContent: jest.fn(({ children }) => (
      <div data-testid="select-content">{children}</div>
    )),
    SelectItem: jest.fn(({ children, value }) => (
      <div
        data-testid="select-item"
        data-value={value}
        onClick={() => onItemClick(value)}
      >
        {children}
      </div>
    )),
  };
});

// Get the exported onItemClick function
const { onItemClick } = jest.requireMock("@/components/ui/select");

describe("TokenSelect", () => {
  const defaultProps = {
    value: "",
    onValueChange: jest.fn(),
    tokens: ["ETH", "BTC", "USDT"],
    placeholder: "Select token",
    disabled: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders with default props", () => {
    render(<TokenSelect {...defaultProps} />);

    // Check if Select component is rendered with correct props
    const selectComponent = screen.getByTestId("select");
    expect(selectComponent).toBeInTheDocument();
    expect(selectComponent).toHaveAttribute("data-value", "");
    expect(selectComponent).toHaveAttribute("data-disabled", "false");

    // Check if SelectTrigger is rendered with correct className
    const triggerComponent = screen.getByTestId("select-trigger");
    expect(triggerComponent).toBeInTheDocument();
    expect(triggerComponent).toHaveClass("w-full");

    // Check if SelectValue is rendered with correct placeholder
    const valueComponent = screen.getByTestId("select-value");
    expect(valueComponent).toBeInTheDocument();
    expect(valueComponent).toHaveAttribute("data-placeholder", "Select token");

    // Check if SelectContent is rendered
    const contentComponent = screen.getByTestId("select-content");
    expect(contentComponent).toBeInTheDocument();

    // Check if all tokens are rendered as SelectItems
    const items = screen.getAllByTestId("select-item");
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveAttribute("data-value", "ETH");
    expect(items[1]).toHaveAttribute("data-value", "BTC");
    expect(items[2]).toHaveAttribute("data-value", "USDT");
  });

  it("renders with selected value", () => {
    render(<TokenSelect {...defaultProps} value="ETH" />);

    const selectComponent = screen.getByTestId("select");
    expect(selectComponent).toHaveAttribute("data-value", "ETH");
  });

  it("handles value change", () => {
    render(<TokenSelect {...defaultProps} />);

    // Simulate clicking on the select component to trigger onValueChange
    const selectComponent = screen.getByTestId("select");
    fireEvent.click(selectComponent);

    // Verify onValueChange was called
    expect(defaultProps.onValueChange).toHaveBeenCalledWith("TEST_VALUE");
  });

  it("renders with custom placeholder", () => {
    const customPlaceholder = "Choose token";
    render(<TokenSelect {...defaultProps} placeholder={customPlaceholder} />);

    const valueComponent = screen.getByTestId("select-value");
    expect(valueComponent).toHaveAttribute(
      "data-placeholder",
      customPlaceholder,
    );
  });

  it("renders in disabled state", () => {
    render(<TokenSelect {...defaultProps} disabled={true} />);

    const selectComponent = screen.getByTestId("select");
    expect(selectComponent).toHaveAttribute("data-disabled", "true");
  });

  it("renders with empty tokens array", () => {
    render(<TokenSelect {...defaultProps} tokens={[]} />);

    const items = screen.queryAllByTestId("select-item");
    expect(items).toHaveLength(0);
  });

  it("simulates token selection", () => {
    render(<TokenSelect {...defaultProps} />);

    // Get the first token item
    const firstItem = screen.getAllByTestId("select-item")[0];

    // Simulate clicking on the item
    fireEvent.click(firstItem);

    // Verify the onItemClick was called with the right value
    expect(onItemClick).toHaveBeenCalledWith("ETH");
  });
});
