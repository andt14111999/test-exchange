// Mock next-intl first to avoid ESM import errors
jest.mock(
  "next-intl",
  () => ({
    useTranslations: () => (key: string) => `translated.${key}`,
  }),
  { virtual: true },
);

import { render } from "@testing-library/react";
import { TwoFactorAuthSetup } from "@/components/two-factor-auth-setup";

// Mock all the required components and hooks to avoid ES module issues
jest.mock("@/navigation", () => ({
  useRouter: () => ({
    push: jest.fn(),
    back: jest.fn(),
  }),
}));

jest.mock("@/lib/api/user", () => ({
  enableTwoFactorAuth: jest.fn().mockResolvedValue({ secret: "test-secret" }),
  verifyTwoFactorAuth: jest.fn().mockResolvedValue({ success: true }),
}));

jest.mock("@/components/ui/use-toast", () => ({
  useToast: () => ({
    toast: jest.fn(),
  }),
}));

jest.mock("react-qr-code", () => ({
  __esModule: true,
  default: () => <div data-testid="qr-code">QR Code</div>,
}));

describe("TwoFactorAuthSetup", () => {
  it("renders without crashing", () => {
    // Since we're mocking so many dependencies, this test just verifies
    // the component doesn't throw when rendered
    expect(() => render(<TwoFactorAuthSetup />)).not.toThrow();
  });
});
