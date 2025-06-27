import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import MerchantRegistration from "@/app/[locale]/merchant/register/page";
import { registerAsMerchant } from "@/lib/api/merchant";
import { useRouter } from "next/navigation";
import React from "react";

// Mock browser APIs
class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

global.ResizeObserver = ResizeObserverMock;

// Mock the dependencies
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("next-intl", () => ({
  useTranslations: jest.fn(() => (key: string) => `translated.${key}`),
}));

jest.mock("@/lib/api/merchant", () => ({
  registerAsMerchant: jest.fn(),
}));

jest.mock("@/components/protected-layout", () => ({
  ProtectedLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="protected-layout">{children}</div>
  ),
}));

describe("MerchantRegistration", () => {
  const mockRouter = {
    push: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    // Clear console.error before each test
    jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    // Restore console.error after each test
    jest.restoreAllMocks();
  });

  it("renders the registration form", () => {
    render(<MerchantRegistration />);
    expect(screen.getByText("translated.title")).toBeInTheDocument();
    expect(screen.getByText("translated.terms.title")).toBeInTheDocument();
    expect(screen.getByText("translated.terms.content")).toBeInTheDocument();
    expect(screen.getByText("translated.terms.accept")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "translated.submit" }),
    ).toBeInTheDocument();
  });

  it("toggles the checkbox when clicked", () => {
    render(<MerchantRegistration />);
    const checkbox = screen.getByRole("checkbox");
    expect(checkbox).toHaveAttribute("aria-checked", "false");
    fireEvent.click(checkbox);
    expect(checkbox).toHaveAttribute("aria-checked", "true");
  });

  it("shows validation error when submitting without accepting terms", async () => {
    render(<MerchantRegistration />);
    const submitButton = screen.getByRole("button", {
      name: "translated.submit",
    });
    fireEvent.click(submitButton);
    await waitFor(() => {
      expect(
        screen.getByText("You must accept the terms and conditions"),
      ).toBeInTheDocument();
    });
  });

  it("submits the form when terms are accepted", async () => {
    (registerAsMerchant as jest.Mock).mockResolvedValueOnce({});
    render(<MerchantRegistration />);

    const checkbox = screen.getByRole("checkbox");
    fireEvent.click(checkbox);

    const submitButton = screen.getByRole("button", {
      name: "translated.submit",
    });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(registerAsMerchant).toHaveBeenCalledTimes(1);
      expect(mockRouter.push).toHaveBeenCalledWith("/merchant/mint-fiat");
    });
  });

  it("displays error message from API response", async () => {
    const errorMessage = "Account already registered as merchant";
    (registerAsMerchant as jest.Mock).mockRejectedValueOnce({
      response: {
        data: {
          error: errorMessage,
        },
      },
    });

    render(<MerchantRegistration />);

    const checkbox = screen.getByRole("checkbox");
    fireEvent.click(checkbox);

    const submitButton = screen.getByRole("button", {
      name: "translated.submit",
    });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(registerAsMerchant).toHaveBeenCalledTimes(1);
      expect(screen.getByTestId("error-message")).toBeInTheDocument();
    });
  });

  it("hides validation error when terms are accepted after initial validation", async () => {
    render(<MerchantRegistration />);

    // Submit without accepting terms
    const submitButton = screen.getByRole("button", {
      name: "translated.submit",
    });
    fireEvent.click(submitButton);

    // Verify error is shown
    await waitFor(() => {
      expect(
        screen.getByText("You must accept the terms and conditions"),
      ).toBeInTheDocument();
    });

    // Accept terms
    const checkbox = screen.getByRole("checkbox");
    fireEvent.click(checkbox);

    // Verify error is hidden
    await waitFor(() => {
      expect(
        screen.queryByText("You must accept the terms and conditions"),
      ).not.toBeInTheDocument();
    });
  });

  it("handles merchant registration error", async () => {
    // Mock the API call to reject with an error
    const errorMessage = "Account already registered as merchant";
    (registerAsMerchant as jest.Mock).mockRejectedValueOnce({
      response: {
        data: {
          error: errorMessage,
        },
      },
    });

    render(<MerchantRegistration />);

    // Accept terms first
    const checkbox = screen.getByRole("checkbox");
    fireEvent.click(checkbox);

    // Submit the form
    const submitButton = screen.getByRole("button", {
      name: "translated.submit",
    });
    fireEvent.click(submitButton);

    // Wait for API call to happen and error to be displayed
    await waitFor(() => {
      expect(registerAsMerchant).toHaveBeenCalled();
      // Check for error message element instead of specific text
      expect(screen.getByTestId("error-message")).toBeInTheDocument();
    });
  });
});
