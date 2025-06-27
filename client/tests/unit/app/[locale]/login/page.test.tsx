import { render, screen, waitFor } from "@testing-library/react";
import Login from "@/app/[locale]/login/page";
import { useTranslations } from "next-intl";
import { useGoogleAuth } from "@/hooks/use-google-auth";
import { useUserStore } from "@/lib/store/user-store";
import { useRouter } from "@/navigation";

// Mock next-intl
jest.mock("next-intl", () => ({
  useTranslations: jest.fn(),
}));

// Mock useGoogleAuth hook
jest.mock("@/hooks/use-google-auth", () => ({
  useGoogleAuth: jest.fn(),
}));

// Mock useUserStore
jest.mock("@/lib/store/user-store", () => ({
  useUserStore: jest.fn(),
}));

// Mock useRouter
jest.mock("@/navigation", () => ({
  useRouter: jest.fn(),
}));

describe("Login", () => {
  const mockTranslations: Record<string, string> = {
    title: "Sign In",
    description: "Sign in to your account to continue",
    continueWith: "Continue with",
    termsAndPrivacy:
      "By signing in, you agree to our Terms of Service and Privacy Policy",
  };

  const mockRouter = {
    push: jest.fn(),
  };

  beforeEach(() => {
    // Reset all mocks
    jest.clearAllMocks();

    // Setup default mock implementations
    (useTranslations as jest.Mock).mockReturnValue(
      (key: string) => mockTranslations[key],
    );
    (useGoogleAuth as jest.Mock).mockReturnValue({
      error: null,
      isPending: false,
    });
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: null,
    });
    (useRouter as jest.Mock).mockReturnValue(mockRouter);

    // Mock localStorage
    const localStorageMock = {
      getItem: jest.fn(),
      removeItem: jest.fn(),
    };
    Object.defineProperty(window, "localStorage", {
      value: localStorageMock,
    });
  });

  it("renders login form when mounted", async () => {
    render(<Login />);

    await waitFor(() => {
      expect(screen.getByText("Sign In")).toBeInTheDocument();
      expect(
        screen.getByText("Sign in to your account to continue"),
      ).toBeInTheDocument();
      expect(screen.getByText("Continue with")).toBeInTheDocument();
      expect(
        screen.getByText(
          "By signing in, you agree to our Terms of Service and Privacy Policy",
        ),
      ).toBeInTheDocument();
    });
  });

  it("shows error message when there is an auth error", async () => {
    const errorMessage = "Authentication failed";
    (useGoogleAuth as jest.Mock).mockReturnValue({
      error: errorMessage,
      isPending: false,
    });

    render(<Login />);

    await waitFor(() => {
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });
  });

  it("disables Google sign-in button when pending", async () => {
    (useGoogleAuth as jest.Mock).mockReturnValue({
      error: null,
      isPending: true,
    });

    render(<Login />);

    await waitFor(() => {
      const googleButton = screen.getByTestId("googleSignInButton");
      expect(googleButton).toHaveClass("opacity-50");
      expect(googleButton).toHaveClass("pointer-events-none");
    });
  });

  it("redirects to home when user is already logged in", async () => {
    (useUserStore as unknown as jest.Mock).mockReturnValue({
      user: { id: "123", name: "Test User" },
    });

    render(<Login />);

    await waitFor(() => {
      expect(mockRouter.push).toHaveBeenCalledWith("/");
    });
  });

  it("clears token from localStorage when no user is present", async () => {
    const mockToken = "test-token";
    (window.localStorage.getItem as jest.Mock).mockReturnValue(mockToken);

    render(<Login />);

    await waitFor(() => {
      expect(window.localStorage.getItem).toHaveBeenCalledWith("token");
      expect(window.localStorage.removeItem).toHaveBeenCalledWith("token");
    });
  });

  it("does not clear token when no token exists", async () => {
    (window.localStorage.getItem as jest.Mock).mockReturnValue(null);

    render(<Login />);

    await waitFor(() => {
      expect(window.localStorage.getItem).toHaveBeenCalledWith("token");
      expect(window.localStorage.removeItem).not.toHaveBeenCalled();
    });
  });
});
