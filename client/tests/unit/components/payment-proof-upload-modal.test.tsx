import {
  render,
  screen,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import { PaymentProofUploadModal } from "@/components/payment-proof-upload-modal";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => {
    const translations: Record<string, string> = {
      uploadPaymentProof: "Upload Payment Proof",
      paymentReceipt: "Payment Receipt",
      additionalInfo: "Additional Info",
      enterAdditionalInfo: "Enter additional information",
      cancel: "Cancel",
      confirmAndSendMoney: "Confirm and Send Money",
      submitting: "Submitting",
      errorMissingPaymentProof: "Missing Payment Proof",
      pleaseUploadPaymentProof: "Please upload payment proof",
      uploadPaymentProofHint: "Upload your payment proof here",
      errorUploadingProof: "Error uploading proof",
    };
    // Remove the namespace prefix if it exists
    const translationKey = key.split(".").pop() || key;
    return translations[translationKey] || key;
  },
}));

// Mock useToast
const mockToast = jest.fn();
jest.mock("@/components/ui/use-toast", () => ({
  useToast: () => ({ toast: mockToast }),
}));

describe("PaymentProofUploadModal", () => {
  const mockOnClose = jest.fn();
  const mockOnSuccess = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders correctly", () => {
    render(<PaymentProofUploadModal onClose={mockOnClose} />);

    expect(screen.getByText("Upload Payment Proof")).toBeInTheDocument();
    expect(screen.getByLabelText("Payment Receipt")).toBeInTheDocument();
    expect(screen.getByText("Additional Info")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("Enter additional information"),
    ).toBeInTheDocument();
    expect(screen.getByText("Cancel")).toBeInTheDocument();
    expect(screen.getByText("Confirm and Send Money")).toBeInTheDocument();
  });

  it("shows error toast when submitting without file", async () => {
    render(<PaymentProofUploadModal onClose={mockOnClose} />);

    // Get the submit button and enable it by setting a file
    const file = new File(["test"], "test.png", { type: "image/png" });
    const fileInput = screen.getByLabelText("Payment Receipt");
    fireEvent.change(fileInput, { target: { files: [file] } });

    // Clear the file to trigger the error
    fireEvent.change(fileInput, { target: { files: [] } });

    const submitButton = screen.getByText("Confirm and Send Money");
    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: "Missing Payment Proof",
        description: "Please upload payment proof",
        variant: "destructive",
      });
    });
  });

  it("handles file selection", () => {
    render(<PaymentProofUploadModal onClose={mockOnClose} />);

    const file = new File(["test"], "test.png", { type: "image/png" });
    const fileInput = screen.getByLabelText("Payment Receipt");

    fireEvent.change(fileInput, { target: { files: [file] } });

    const submitButton = screen.getByText("Confirm and Send Money");
    expect(submitButton).not.toBeDisabled();
  });

  it("handles description input", () => {
    render(<PaymentProofUploadModal onClose={mockOnClose} />);

    const textarea = screen.getByPlaceholderText(
      "Enter additional information",
    );
    fireEvent.change(textarea, { target: { value: "Test description" } });

    expect(textarea).toHaveValue("Test description");
  });

  it("disables buttons during upload", async () => {
    render(
      <PaymentProofUploadModal
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />,
    );

    // Select a file first
    const file = new File(["test"], "test.png", { type: "image/png" });
    const fileInput = screen.getByLabelText("Payment Receipt");
    fireEvent.change(fileInput, { target: { files: [file] } });

    // Get buttons
    const submitButton = screen.getByText("Confirm and Send Money");
    const cancelButton = screen.getByText("Cancel");

    // Mock onSuccess to be async
    mockOnSuccess.mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100)),
    );

    // Submit the form
    await act(async () => {
      fireEvent.click(submitButton);
    });

    // Wait for state update
    await waitFor(() => {
      expect(submitButton).toHaveTextContent("Submitting");
      expect(submitButton).toBeDisabled();
      expect(cancelButton).toBeDisabled();
    });
  });

  it("calls onSuccess with file and description", async () => {
    render(
      <PaymentProofUploadModal
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />,
    );

    const file = new File(["test"], "test.png", { type: "image/png" });
    const fileInput = screen.getByLabelText("Payment Receipt");
    const textarea = screen.getByPlaceholderText(
      "Enter additional information",
    );
    const submitButton = screen.getByText("Confirm and Send Money");

    fireEvent.change(fileInput, { target: { files: [file] } });
    fireEvent.change(textarea, { target: { value: "Test description" } });

    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(mockOnSuccess).toHaveBeenCalledWith({
        file,
        description: "Test description",
      });
    });
  });

  it("handles upload error", async () => {
    render(
      <PaymentProofUploadModal
        onClose={mockOnClose}
        onSuccess={mockOnSuccess}
      />,
    );

    const file = new File(["test"], "test.png", { type: "image/png" });
    const fileInput = screen.getByLabelText("Payment Receipt");
    const submitButton = screen.getByText("Confirm and Send Money");

    // Mock onSuccess to throw an error
    mockOnSuccess.mockImplementation(() =>
      Promise.reject(new Error("Upload failed")),
    );

    fireEvent.change(fileInput, { target: { files: [file] } });

    await act(async () => {
      fireEvent.click(submitButton);
    });

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith({
        title: "Error uploading proof",
        variant: "destructive",
      });
    });
  });
});
