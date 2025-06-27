import { render, screen, fireEvent } from "@testing-library/react";
import { CopyableField } from "@/components/copyable-field";
import { Copy as CopyIcon } from "lucide-react";
import React from "react";

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: jest.fn(),
  },
});

// Mock useToast
const mockToast = jest.fn();
jest.mock("@/components/ui/use-toast", () => ({
  useToast: () => ({ toast: mockToast }),
}));

describe("CopyableField Integration", () => {
  const formatFiatAmount = (amount: number, _currency: string) => {
    return `₫${amount.toLocaleString()}`;
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("can be used with text content", () => {
    render(
      <div className="flex items-center">
        <div className="font-medium">Sample Text</div>
        <CopyableField
          content={<CopyIcon data-testid="copy-icon" />}
          value="Sample Text"
          copyMessage="Text copied"
        />
      </div>,
    );

    fireEvent.click(screen.getByRole("button"));

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("Sample Text");
    expect(mockToast).toHaveBeenCalledWith({
      title: "Copied to clipboard",
      description: "Text copied",
    });
  });

  it("can be used with formatted amounts but copy only numbers", () => {
    const amount = 500000;
    const currency = "VND";
    const formattedAmount = formatFiatAmount(amount, currency);

    render(
      <div className="flex items-center justify-between">
        <div className="text-gray-500 text-sm">Amount</div>
        <div className="flex items-center">
          <div className="font-medium">{formattedAmount}</div>
          <CopyableField
            content={<CopyIcon data-testid="copy-icon" />}
            value={amount.toString()}
            copyMessage="Amount copied"
          />
        </div>
      </div>,
    );

    fireEvent.click(screen.getByRole("button"));

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("500000");
    expect(mockToast).toHaveBeenCalledWith({
      title: "Copied to clipboard",
      description: "Amount copied",
    });

    // Verify formatted amount is displayed
    expect(screen.getByText("₫500,000")).toBeInTheDocument();
  });

  it("can be used with bank account information", () => {
    render(
      <div className="space-y-4">
        <div className="bg-slate-50 p-4 rounded">
          <div className="flex items-center justify-between">
            <div className="text-gray-500 text-sm">Bank Name</div>
            <div className="flex items-center">
              <div className="font-medium">Example Bank</div>
              <CopyableField
                content={<CopyIcon data-testid="copy-icon-bank" />}
                value="Example Bank"
                copyMessage="Bank name copied"
              />
            </div>
          </div>
        </div>

        <div className="bg-slate-50 p-4 rounded">
          <div className="flex items-center justify-between">
            <div className="text-gray-500 text-sm">Account Number</div>
            <div className="flex items-center">
              <div className="font-medium">1234567890</div>
              <CopyableField
                content={<CopyIcon data-testid="copy-icon-account" />}
                value="1234567890"
                copyMessage="Account number copied"
              />
            </div>
          </div>
        </div>
      </div>,
    );

    // Test bank name copy
    fireEvent.click(screen.getByTestId("copy-icon-bank").closest("button")!);
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("Example Bank");

    // Test account number copy
    fireEvent.click(screen.getByTestId("copy-icon-account").closest("button")!);
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("1234567890");
  });
});
