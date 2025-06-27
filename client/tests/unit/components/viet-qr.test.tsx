import { render, screen, fireEvent } from "@testing-library/react";
import { VietQR } from "@/components/viet-qr";
import { useToast } from "@/components/ui/use-toast";
import { useBanks } from "@/lib/api/hooks/use-banks";
import "@testing-library/jest-dom";
import type { ImageProps } from "next/image";
import { Bank } from "@/lib/api/banks";

// Mock the useToast hook
jest.mock("@/components/ui/use-toast", () => ({
  useToast: jest.fn(),
}));

// Mock the useBanks hook
jest.mock("@/lib/api/hooks/use-banks", () => ({
  useBanks: jest.fn(),
}));

// Mock the QRCodeSVG component
jest.mock("qrcode.react", () => ({
  QRCodeSVG: jest.fn(({ value, width, height, level, className }) => (
    <div
      data-testid="qr-code-svg"
      data-value={value}
      data-width={width}
      data-height={height}
      data-level={level}
      className={className}
    />
  )),
}));

// Mock next/image
jest.mock("next/image", () => ({
  __esModule: true,
  default: function MockImage({
    src,
    alt,
    width,
    height,
    className,
  }: Partial<ImageProps>) {
    return (
      <img
        src={src as string}
        alt={alt as string}
        width={width}
        height={height}
        className={className}
        data-testid="next-image"
      />
    );
  },
}));

// Mock next-intl (if needed)
jest.mock("next-intl", () => ({
  useTranslations: () => (key: string) => `translated.${key}`,
}));

// Mock clipboard API
Object.defineProperty(navigator, "clipboard", {
  value: {
    writeText: jest.fn(),
  },
  writable: true,
});

describe("VietQR", () => {
  const mockShowToast = jest.fn();
  const mockUseBanks = useBanks as jest.MockedFunction<typeof useBanks>;

  const mockBanks: Bank[] = [
    {
      name: "Ngân hàng TMCP Ngoại Thương Việt Nam",
      code: "VCB",
      bin: "970436",
      shortName: "Vietcombank",
      logo: "https://api.vietqr.io/img/VCB.png",
      transferSupported: 1,
      lookupSupported: 1,
      short_name: "Vietcombank",
      support: 3,
      isTransfer: 1,
      swift_code: "BFTVVNVX",
    },
    {
      name: "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam",
      code: "BIDV",
      bin: "970418",
      shortName: "BIDV",
      logo: "https://api.vietqr.io/img/BIDV.png",
      transferSupported: 1,
      lookupSupported: 1,
      short_name: "BIDV",
      support: 3,
      isTransfer: 1,
      swift_code: "BIDVVNVX",
    },
    {
      name: "Ngân hàng TMCP Tiên Phong",
      code: "TPB",
      bin: "970423",
      shortName: "TPBank",
      logo: "https://api.vietqr.io/img/TPB.png",
      transferSupported: 1,
      lookupSupported: 1,
      short_name: "TPBank",
      support: 3,
      isTransfer: 1,
      swift_code: "TPBVVNVX",
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    (useToast as jest.Mock).mockReturnValue({
      toast: mockShowToast,
    });

    // Mock useBanks to return React Query structure
    mockUseBanks.mockReturnValue({
      data: { status: "success", data: mockBanks },
      isLoading: false,
      error: null,
      isError: false,
      isSuccess: true,
    } as ReturnType<typeof useBanks>);
  });

  const defaultProps = {
    bankName: "VIETCOMBANK",
    accountName: "NGUYEN VAN A",
    accountNumber: "1234567890",
    amount: "1000000",
    content: "Payment for order #123",
  };

  it("renders correctly with valid Vietnamese bank information", () => {
    render(<VietQR {...defaultProps} />);

    expect(screen.getByText("Scan QR Code")).toBeInTheDocument();
    expect(screen.getByTestId("next-image")).toBeInTheDocument();
    expect(screen.getByTestId("next-image")).toHaveAttribute(
      "src",
      expect.stringContaining("970436-1234567890-compact2.png"),
    );
    expect(screen.getByText("VietQR")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Copy Payment Info/ }),
    ).toBeInTheDocument();
  });

  it("renders with custom text props", () => {
    render(
      <VietQR
        {...defaultProps}
        copyButtonText="Custom Copy Text"
        scanQRText="Custom Scan Text"
      />,
    );

    expect(screen.getByText("Custom Scan Text")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Custom Copy Text/ }),
    ).toBeInTheDocument();
  });

  it("renders with QRCodeSVG when useImageAPI is false", () => {
    render(<VietQR {...defaultProps} useImageAPI={false} />);

    expect(screen.getByTestId("qr-code-svg")).toBeInTheDocument();
    expect(screen.queryByTestId("next-image")).not.toBeInTheDocument();
  });

  it("renders with QRCodeSVG when bank is not Vietnamese", () => {
    render(
      <VietQR
        {...defaultProps}
        bankName=""
        accountNumber="1234567890"
        useImageAPI={true}
      />,
    );

    expect(screen.getByTestId("qr-code-svg")).toBeInTheDocument();
    expect(screen.queryByTestId("next-image")).not.toBeInTheDocument();
    expect(screen.queryByText("VietQR")).not.toBeInTheDocument();
  });

  it("renders with custom QR size", () => {
    render(<VietQR {...defaultProps} qrSize={300} />);

    expect(screen.getByTestId("next-image")).toHaveAttribute("width", "300");
    expect(screen.getByTestId("next-image")).toHaveAttribute("height", "300");
  });

  it("handles copy button click correctly", () => {
    render(<VietQR {...defaultProps} />);

    const copyButton = screen.getByRole("button", {
      name: /Copy Payment Info/,
    });
    fireEvent.click(copyButton);

    expect(navigator.clipboard.writeText).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith({
      title: "Copied to clipboard",
      description: "Payment information copied",
    });
  });

  it("copies QR data when hovering and clicking on QR code", () => {
    render(<VietQR {...defaultProps} />);

    const qrContainer = screen
      .getByTestId("next-image")
      .closest(".relative.group");
    expect(qrContainer).toBeInTheDocument();

    // Find the copy button in the hover overlay and click it
    const overlayButton = screen.getAllByRole("button")[0];
    fireEvent.click(overlayButton);

    expect(navigator.clipboard.writeText).toHaveBeenCalled();
    expect(mockShowToast).toHaveBeenCalledWith({
      title: "Copied to clipboard",
      description: "Payment information copied",
    });
  });

  it("generates correct VietQR data for known banks", () => {
    const { rerender } = render(
      <VietQR {...defaultProps} useImageAPI={false} />,
    );

    let qrCode = screen.getByTestId("qr-code-svg");
    expect(qrCode).toHaveAttribute(
      "data-value",
      expect.stringContaining("970436"),
    ); // Vietcombank BIN

    // Test with different bank
    rerender(<VietQR {...defaultProps} bankName="BIDV" useImageAPI={false} />);

    qrCode = screen.getByTestId("qr-code-svg");
    expect(qrCode).toHaveAttribute(
      "data-value",
      expect.stringContaining("970418"),
    ); // BIDV BIN
  });

  it("renders generic QR for unknown bank names", () => {
    render(
      <VietQR {...defaultProps} bankName="UNKNOWN_BANK" useImageAPI={false} />,
    );

    const qrCode = screen.getByTestId("qr-code-svg");
    // Should render generic QR with JSON data since bank not found
    const qrValue = qrCode.getAttribute("data-value") || "";
    expect(qrValue).toContain("UNKNOWN_BANK");
    expect(
      screen.getByText("Bank not recognized - using generic QR"),
    ).toBeInTheDocument();
  });

  it("handles amount with non-numeric characters correctly", () => {
    render(
      <VietQR
        {...defaultProps}
        amount="1,000,000.50 VND"
        useImageAPI={false}
      />,
    );

    const qrCode = screen.getByTestId("qr-code-svg");
    const qrValue = qrCode.getAttribute("data-value") || "";

    // The QR code format follows EMV QR Code specs
    // Check that it contains the transaction amount field (54) with properly formatted amount
    expect(qrValue).toContain("5409"); // Field 54 with length 09
    expect(qrValue).toContain("1000000.5"); // The actual amount after non-numeric chars are removed
  });

  it("correctly constructs VietQR.io image URL", () => {
    render(
      <VietQR {...defaultProps} amount="1,000,000" content="Test payment" />,
    );

    const imageElement = screen.getByTestId("next-image");
    const src = imageElement.getAttribute("src") || "";

    expect(src).toContain("970436-1234567890-compact2.png");
    expect(src).toContain("amount=1000000");
    expect(src).toContain("addInfo=Test%20payment");
    expect(src).toContain("accountName=NGUYEN%20VAN%20A");
  });

  it("handles error in generateVietQRData gracefully", () => {
    // Force an error by passing amount that can't be parsed
    render(<VietQR {...defaultProps} amount="invalid" useImageAPI={false} />);

    // Still renders a QR code with fallback data
    expect(screen.getByTestId("qr-code-svg")).toBeInTheDocument();
  });

  it("normalizes bank name correctly", () => {
    render(
      <VietQR {...defaultProps} bankName="viet com bank" useImageAPI={false} />,
    );

    const qrCode = screen.getByTestId("qr-code-svg");
    expect(qrCode).toHaveAttribute(
      "data-value",
      expect.stringContaining("970436"),
    ); // Vietcombank BIN
  });

  it("correctly handles undefined accountName in getVietQRImageUrl", () => {
    render(
      <VietQR
        bankName="VIETCOMBANK"
        accountName=""
        accountNumber="1234567890"
        amount="1000000"
        content="Payment for order #123"
      />,
    );

    const imageElement = screen.getByTestId("next-image");
    const src = imageElement.getAttribute("src") || "";

    expect(src).toContain("970436-1234567890-compact2.png");
    expect(src).toContain("accountName=");
  });

  it("shows loading state when banks are being fetched", () => {
    mockUseBanks.mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
      isError: false,
      isSuccess: false,
    } as ReturnType<typeof useBanks>);

    render(<VietQR {...defaultProps} />);

    expect(screen.getByText("Loading banks data...")).toBeInTheDocument();
    // Check for loading skeleton div
    const loadingDiv = document.querySelector(
      ".w-\\[200px\\].h-\\[200px\\].bg-gray-200.animate-pulse.rounded-lg",
    );
    expect(loadingDiv).toBeInTheDocument();
  });

  it("shows error state when banks cannot be loaded", () => {
    mockUseBanks.mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error("Failed to fetch banks"),
      isError: true,
      isSuccess: false,
    } as ReturnType<typeof useBanks>);

    render(<VietQR {...defaultProps} />);

    expect(
      screen.getByText("Unable to load banks data. Please try again later."),
    ).toBeInTheDocument();
  });

  it("uses provided banks prop instead of hook", () => {
    const customBanks: Bank[] = [
      {
        name: "Custom Bank",
        code: "CB",
        bin: "999999",
        shortName: "CustomBank",
        logo: "",
        transferSupported: 1,
        lookupSupported: 1,
        short_name: "CustomBank",
        support: 3,
        isTransfer: 1,
        swift_code: "CUSTCB",
      },
    ];

    render(
      <VietQR
        {...defaultProps}
        bankName="CustomBank"
        banks={customBanks}
        useImageAPI={false}
      />,
    );

    const qrCode = screen.getByTestId("qr-code-svg");
    expect(qrCode).toHaveAttribute(
      "data-value",
      expect.stringContaining("999999"),
    );
  });
});
