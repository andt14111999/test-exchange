import { render, screen } from "@testing-library/react";
import { QueryProvider } from "@/components/providers/query-provider";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

// Mock the react-query dependencies
jest.mock("@tanstack/react-query", () => ({
  QueryClient: jest.fn(),
  QueryClientProvider: jest.fn(({ children }) => (
    <div data-testid="query-provider">{children}</div>
  )),
}));

jest.mock("@tanstack/react-query-devtools", () => ({
  ReactQueryDevtools: jest.fn(() => <div data-testid="react-query-devtools" />),
}));

describe("QueryProvider", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should render children and ReactQueryDevtools", () => {
    render(
      <QueryProvider>
        <div data-testid="child">Test Child</div>
      </QueryProvider>,
    );

    // Verify that children are rendered
    expect(screen.getByTestId("child")).toBeInTheDocument();
    expect(screen.getByText("Test Child")).toBeInTheDocument();

    // Verify that QueryClientProvider is rendered
    expect(screen.getByTestId("query-provider")).toBeInTheDocument();
    expect(QueryClientProvider).toHaveBeenCalled();

    // Verify that ReactQueryDevtools is rendered
    expect(screen.getByTestId("react-query-devtools")).toBeInTheDocument();
    expect(ReactQueryDevtools).toHaveBeenCalled();
  });

  it("should create QueryClient with correct default options", () => {
    render(
      <QueryProvider>
        <div>Test</div>
      </QueryProvider>,
    );

    // Verify QueryClient was created with correct options
    expect(QueryClient).toHaveBeenCalledWith({
      defaultOptions: {
        queries: {
          staleTime: 60 * 1000,
          refetchOnWindowFocus: false,
        },
      },
    });
  });

  it("should maintain the same QueryClient instance between renders", () => {
    const { rerender } = render(
      <QueryProvider>
        <div>Test</div>
      </QueryProvider>,
    );

    const firstCallCount = (QueryClient as jest.Mock).mock.calls.length;

    // Rerender the component
    rerender(
      <QueryProvider>
        <div>Test Updated</div>
      </QueryProvider>,
    );

    // Verify that QueryClient was not created again
    expect((QueryClient as jest.Mock).mock.calls.length).toBe(firstCallCount);
  });
});
