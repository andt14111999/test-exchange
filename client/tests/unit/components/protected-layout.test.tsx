import { render, screen } from "@testing-library/react";
import { ProtectedLayout } from "@/components/protected-layout";
import { useProtectedRoute } from "@/hooks/use-protected-route";
import { act } from "react-dom/test-utils";

// Mock the useProtectedRoute hook
jest.mock("@/hooks/use-protected-route", () => ({
  useProtectedRoute: jest.fn(),
}));

describe("ProtectedLayout", () => {
  const mockUseProtectedRoute = useProtectedRoute as jest.Mock;
  const testChild = <div data-testid="test-child">Test Child</div>;
  const testLoadingFallback = (
    <div data-testid="custom-loading">Custom Loading</div>
  );

  beforeEach(() => {
    jest.clearAllMocks();
    // Reset all mocks before each test
    mockUseProtectedRoute.mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      user: null,
    });
  });

  it("shows default loading skeleton when checking authentication", () => {
    mockUseProtectedRoute.mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      user: null,
    });

    render(<ProtectedLayout>{testChild}</ProtectedLayout>);

    // Should show skeleton loading UI
    expect(screen.getByTestId("skeleton-container")).toBeInTheDocument();
    expect(screen.queryByTestId("test-child")).not.toBeInTheDocument();
  });

  it("shows custom loading fallback when provided", () => {
    mockUseProtectedRoute.mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      user: null,
    });

    render(
      <ProtectedLayout loadingFallback={testLoadingFallback}>
        {testChild}
      </ProtectedLayout>,
    );

    // Should show custom loading fallback
    expect(screen.getByTestId("custom-loading")).toBeInTheDocument();
    expect(screen.queryByTestId("test-child")).not.toBeInTheDocument();
    expect(screen.queryByTestId("skeleton-container")).not.toBeInTheDocument();
  });

  it("shows loading state when not mounted", () => {
    // Mock the initial state before mounting
    mockUseProtectedRoute.mockReturnValue({
      isAuthenticated: true,
      isLoading: true, // This should be true because !mounted is true
      user: { id: "1" },
    });

    render(<ProtectedLayout>{testChild}</ProtectedLayout>);

    // Should show skeleton loading UI initially since mounted is false
    expect(screen.getByTestId("skeleton-container")).toBeInTheDocument();
    expect(screen.queryByTestId("test-child")).not.toBeInTheDocument();
  });

  it("renders children when authenticated and mounted", async () => {
    mockUseProtectedRoute.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      user: { id: "1" },
    });

    render(<ProtectedLayout>{testChild}</ProtectedLayout>);

    // Wait for the useEffect to run and set mounted to true
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Should render children
    expect(screen.getByTestId("test-child")).toBeInTheDocument();
    expect(screen.queryByTestId("skeleton-container")).not.toBeInTheDocument();
  });

  it("updates when authentication state changes", async () => {
    // Initial render with loading state
    mockUseProtectedRoute.mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      user: null,
    });

    const { rerender } = render(<ProtectedLayout>{testChild}</ProtectedLayout>);

    // Should show loading state
    expect(screen.getByTestId("skeleton-container")).toBeInTheDocument();

    // Update to authenticated state
    mockUseProtectedRoute.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      user: { id: "1" },
    });

    // Wait for the useEffect to run and set mounted to true
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    rerender(<ProtectedLayout>{testChild}</ProtectedLayout>);

    // Should now show children
    expect(screen.getByTestId("test-child")).toBeInTheDocument();
    expect(screen.queryByTestId("skeleton-container")).not.toBeInTheDocument();
  });
});
