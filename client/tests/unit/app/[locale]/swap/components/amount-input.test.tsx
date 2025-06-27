import { render, screen, fireEvent } from "@testing-library/react";
import { AmountInput } from "@/app/[locale]/swap/components/amount-input";

// Mock the next-intl translations
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => key),
}));

// Mock the TokenSelect component
jest.mock("@/app/[locale]/swap/components/token-select", () => ({
  TokenSelect: jest.fn(
    ({ value, onValueChange, tokens, placeholder, disabled }) => (
      <select
        data-testid="token-select"
        value={value}
        onChange={(e) => onValueChange(e.target.value)}
        disabled={disabled}
      >
        <option value="">{placeholder}</option>
        {tokens.map((token: string) => (
          <option key={token} value={token}>
            {token}
          </option>
        ))}
      </select>
    ),
  ),
}));

describe("AmountInput", () => {
  const defaultProps = {
    label: "Amount",
    value: "",
    onChange: jest.fn(),
    token: "",
    onTokenChange: jest.fn(),
    tokens: ["ETH", "BTC", "USDT"],
    placeholder: "0.0",
    tokenPlaceholder: "Select token",
    disabled: false,
    error: undefined,
    tokenBalance: "0",
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders with default props", () => {
    render(<AmountInput {...defaultProps} />);

    // Check if label is rendered
    expect(screen.getByText("Amount")).toBeInTheDocument();

    // Check if input is rendered with correct props
    const input = screen.getByPlaceholderText("0.0");
    expect(input).toBeInTheDocument();
    expect(input).not.toBeDisabled();

    // Check if token select is rendered
    const tokenSelect = screen.getByTestId("token-select");
    expect(tokenSelect).toBeInTheDocument();
    expect(tokenSelect).not.toBeDisabled();
  });

  it("renders with balance when token is selected", () => {
    render(<AmountInput {...defaultProps} token="ETH" tokenBalance="1.5" />);

    // Check if balance is rendered
    const balanceLabel = screen.getByText(/balance|Số dư/i);
    const balanceValue = screen.getByText((content) =>
      /1\.5|1,500\.00|1\.50/.test(content),
    );
    expect(balanceLabel).toBeInTheDocument();
    expect(balanceValue).toBeInTheDocument();
  });

  it("does not render balance when no token is selected", () => {
    render(<AmountInput {...defaultProps} />);

    // Check that balance is not rendered
    expect(screen.queryByText(/balance:/)).not.toBeInTheDocument();
  });

  it("handles input changes", () => {
    render(<AmountInput {...defaultProps} />);

    const input = screen.getByPlaceholderText("0.0");
    fireEvent.change(input, { target: { value: "1.5" } });

    expect(defaultProps.onChange).toHaveBeenCalledWith("1.5");
  });

  it("handles token changes", () => {
    render(<AmountInput {...defaultProps} />);

    const tokenSelect = screen.getByTestId("token-select");
    fireEvent.change(tokenSelect, { target: { value: "ETH" } });

    expect(defaultProps.onTokenChange).toHaveBeenCalledWith("ETH");
  });

  it("renders error message when provided", () => {
    const errorMessage = "Invalid amount";
    render(<AmountInput {...defaultProps} error={errorMessage} />);

    expect(screen.getByText(errorMessage)).toBeInTheDocument();
  });

  it("renders in disabled state", () => {
    render(<AmountInput {...defaultProps} disabled={true} />);

    const input = screen.getByPlaceholderText("0.0");
    const tokenSelect = screen.getByTestId("token-select");

    expect(input).toBeDisabled();
    expect(tokenSelect).toBeDisabled();
  });

  it("renders with custom placeholder", () => {
    const customPlaceholder = "Enter amount";
    render(<AmountInput {...defaultProps} placeholder={customPlaceholder} />);

    expect(screen.getByPlaceholderText(customPlaceholder)).toBeInTheDocument();
  });

  it("renders with custom token placeholder", () => {
    const customTokenPlaceholder = "Choose token";
    render(
      <AmountInput
        {...defaultProps}
        tokenPlaceholder={customTokenPlaceholder}
      />,
    );

    const tokenSelect = screen.getByTestId("token-select");
    expect(tokenSelect).toHaveTextContent(customTokenPlaceholder);
  });

  it("renders with empty token balance", () => {
    render(<AmountInput {...defaultProps} token="ETH" tokenBalance="" />);

    // Check if balance is rendered with default value
    const balanceLabel2 = screen.getByText(/balance|Số dư/i);
    const balanceValue2 = screen.getByText((content) =>
      /0(\.00)?/.test(content),
    );
    expect(balanceLabel2).toBeInTheDocument();
    expect(balanceValue2).toBeInTheDocument();
  });

  it("renders with undefined token balance", () => {
    render(
      <AmountInput {...defaultProps} token="ETH" tokenBalance={undefined} />,
    );

    // Check if balance is rendered with default value
    const balanceLabel3 = screen.getByText(/balance|Số dư/i);
    const balanceValue3 = screen.getByText((content) =>
      /0(\.00)?/.test(content),
    );
    expect(balanceLabel3).toBeInTheDocument();
    expect(balanceValue3).toBeInTheDocument();
  });
});
