import { render, screen, fireEvent, waitFor } from "@testing-library/react";
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

describe("CopyableField", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders with text content", () => {
    render(<CopyableField content="Copy me" value="test-value" />);
    expect(screen.getByText("Copy me")).toBeInTheDocument();
  });

  it("renders with React node content", () => {
    render(
      <CopyableField
        content={<CopyIcon data-testid="copy-icon" />}
        value="test-value"
      />,
    );
    expect(screen.getByTestId("copy-icon")).toBeInTheDocument();
  });

  it("copies the value to clipboard when clicked", async () => {
    render(
      <CopyableField
        content={<CopyIcon data-testid="copy-icon" />}
        value="test-value"
        copyMessage="Custom message"
      />,
    );

    fireEvent.click(screen.getByRole("button"));

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith("test-value");
      expect(mockToast).toHaveBeenCalledWith({
        title: "Copied to clipboard",
        description: "Custom message",
      });
    });
  });

  it("handles undefined value", async () => {
    render(
      <CopyableField
        content={<CopyIcon data-testid="copy-icon" />}
        value={undefined}
      />,
    );

    fireEvent.click(screen.getByRole("button"));

    await waitFor(() => {
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith("");
    });
  });

  it("uses default copy message when not provided", async () => {
    render(
      <CopyableField
        content={<CopyIcon data-testid="copy-icon" />}
        value="test-value"
      />,
    );

    fireEvent.click(screen.getByRole("button"));

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: "Copied to clipboard",
        description: "Information copied",
      });
    });
  });
});
