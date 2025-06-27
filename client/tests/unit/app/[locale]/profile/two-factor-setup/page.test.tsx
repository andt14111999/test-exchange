// Mock next-intl first
jest.mock(
  "next-intl",
  () => ({
    useTranslations: () => (key: string) => `translated.${key}`,
  }),
  { virtual: true },
);

// Mock navigation
jest.mock("@/navigation", () => ({
  useRouter: () => ({
    push: jest.fn(),
    back: jest.fn(),
  }),
}));

// Mock API calls
jest.mock("@/lib/api/user", () => ({
  enableTwoFactorAuth: jest.fn(),
  verifyTwoFactorAuth: jest.fn(),
}));

// Mock device trust utils
jest.mock("@/lib/utils/device-trust-preference", () => ({
  setDeviceTrustPreference: jest.fn(),
  getDeviceTrustPreference: jest.fn(() => false),
}));

// Mock react-qr-code
jest.mock("react-qr-code", () => {
  return function MockQRCode({ value }: { value: string }) {
    return (
      <div data-testid="qr-code" data-value={value}>
        QR Code
      </div>
    );
  };
});

// Mock toast
jest.mock("@/components/ui/use-toast", () => ({
  useToast: () => ({
    toast: jest.fn(),
  }),
}));

// Mock the TwoFactorAuthSetup component with realistic behavior
jest.mock("@/components/two-factor-auth-setup", () => ({
  TwoFactorAuthSetup: ({ onComplete }: { onComplete?: () => void }) => (
    <div data-testid="two-factor-auth-setup">
      <h2>Two Factor Auth Setup</h2>
      <p>Setup your 2FA authentication</p>
      <button onClick={onComplete} data-testid="complete-setup-button">
        Complete Setup
      </button>
    </div>
  ),
}));

// Mock ProtectedLayout
jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <main data-testid="protected-layout">
      <div className="container py-8">{children}</div>
    </main>
  ),
}));

import { render, screen, fireEvent } from "@testing-library/react";
import TwoFactorSetupPage from "@/app/[locale]/profile/two-factor-setup/page";

describe("TwoFactorSetupPage", () => {
  it("should render the page with ProtectedLayout", () => {
    render(<TwoFactorSetupPage />);

    // Check if ProtectedLayout is rendered
    expect(screen.getByTestId("protected-layout")).toBeInTheDocument();

    // Check if the container has correct styling
    const container = screen
      .getByTestId("protected-layout")
      .querySelector(".container.py-8");
    expect(container).toBeInTheDocument();
  });

  it("should render TwoFactorAuthSetup component", () => {
    render(<TwoFactorSetupPage />);

    // Check if TwoFactorAuthSetup component is rendered
    expect(screen.getByTestId("two-factor-auth-setup")).toBeInTheDocument();
    expect(screen.getByText("Two Factor Auth Setup")).toBeInTheDocument();
    expect(
      screen.getByText("Setup your 2FA authentication"),
    ).toBeInTheDocument();
  });

  it("should render without crashing", () => {
    expect(() => render(<TwoFactorSetupPage />)).not.toThrow();
  });

  it("should have proper page structure", () => {
    render(<TwoFactorSetupPage />);

    // Should have the protected layout wrapper
    const protectedLayout = screen.getByTestId("protected-layout");
    expect(protectedLayout).toBeInTheDocument();

    // Should have the two-factor setup component inside
    const setupComponent = screen.getByTestId("two-factor-auth-setup");
    expect(setupComponent).toBeInTheDocument();

    // The setup component should be inside the protected layout
    expect(protectedLayout).toContainElement(setupComponent);
  });

  it("should handle component interactions", () => {
    render(<TwoFactorSetupPage />);

    // Should be able to interact with the setup component
    const completeButton = screen.getByTestId("complete-setup-button");
    expect(completeButton).toBeInTheDocument();

    // Should not throw when clicking
    expect(() => fireEvent.click(completeButton)).not.toThrow();
  });

  it("should render component in correct container structure", () => {
    render(<TwoFactorSetupPage />);

    // Check the nested container structure
    const outerContainer = screen
      .getByTestId("protected-layout")
      .querySelector(".container.py-8");
    expect(outerContainer).toBeInTheDocument();

    const innerContainer = outerContainer?.querySelector(".container.py-8");
    expect(innerContainer).toBeInTheDocument();

    // The setup component should be in the inner container
    const setupComponent = screen.getByTestId("two-factor-auth-setup");
    expect(innerContainer).toContainElement(setupComponent);
  });
});
