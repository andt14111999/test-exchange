import { renderHook, act, waitFor } from "@testing-library/react";
import { useUser } from "@/hooks/use-user";
import { fetchUserData } from "@/lib/api/user";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode } from "react";
import { UserData } from "@/types/user";

// Mock localStorage
const mockLocalStorage = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};

Object.defineProperty(window, "localStorage", {
  value: mockLocalStorage,
});

// Mock fetchUserData
jest.mock("@/lib/api/user", () => ({
  fetchUserData: jest.fn(),
}));

const mockFetchUserData = fetchUserData as jest.MockedFunction<
  typeof fetchUserData
>;

// Create a wrapper with QueryClient for testing
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        retryDelay: 1,
        gcTime: 0,
      },
    },
  });
  const TestWrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  TestWrapper.displayName = "TestWrapper";
  return TestWrapper;
};

describe("useUser", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockLocalStorage.clear();
    mockLocalStorage.getItem.mockReturnValue(null);
  });

  it("should not fetch user data when no token exists", async () => {
    mockLocalStorage.getItem.mockReturnValue(null);

    const { result } = renderHook(() => useUser(), {
      wrapper: createWrapper(),
    });

    // Wait for effect to complete
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.isLoading).toBe(false);
    expect(result.current.data).toBeUndefined();
    expect(mockFetchUserData).not.toHaveBeenCalled();
  });

  it("should fetch user data when token exists", async () => {
    const mockUserData: UserData = {
      id: 123,
      email: "test@example.com",
      display_name: "Test User",
      role: "user",
      avatar_url: "https://example.com/avatar.jpg",
      created_at: "2024-03-20T00:00:00Z",
      updated_at: "2024-03-20T00:00:00Z",
    };

    mockLocalStorage.getItem.mockReturnValue("valid-token");
    mockFetchUserData.mockResolvedValueOnce(mockUserData);

    const { result } = renderHook(() => useUser(), {
      wrapper: createWrapper(),
    });

    // Wait for effect and initial query setup
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Wait for query to complete
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(result.current.data).toEqual(mockUserData);
    expect(mockFetchUserData).toHaveBeenCalledTimes(1);
  });

  it("should handle error when fetching user data fails", async () => {
    const error = new Error("Failed to fetch user data");
    mockLocalStorage.getItem.mockReturnValue("valid-token");
    mockFetchUserData.mockRejectedValueOnce(error);

    const { result } = renderHook(() => useUser(), {
      wrapper: createWrapper(),
    });

    // Wait for the query to fail and error state to be set
    await waitFor(
      () => {
        expect(result.current.isError).toBe(true);
      },
      { timeout: 2000 },
    );

    expect(result.current.error).toBeDefined();
    expect(result.current.error?.message).toBe("Failed to fetch user data");
    expect(result.current.data).toBeUndefined();
  });

  it("should update hasToken state when token changes", async () => {
    // Start with no token
    mockLocalStorage.getItem.mockReturnValue(null);

    const { result } = renderHook(() => useUser(), {
      wrapper: createWrapper(),
    });

    // Wait for effect and initial query setup
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(mockFetchUserData).not.toHaveBeenCalled();
    expect(result.current.isLoading).toBe(false);

    // Simulate token being added
    mockLocalStorage.getItem.mockReturnValue("new-token");

    // Mock successful response
    const mockUserData: UserData = {
      id: 123,
      email: "test@example.com",
      display_name: "Test User",
      role: "user",
      avatar_url: "https://example.com/avatar.jpg",
      created_at: "2024-03-20T00:00:00Z",
      updated_at: "2024-03-20T00:00:00Z",
    };
    mockFetchUserData.mockResolvedValueOnce(mockUserData);

    // Simulate storage event
    await act(async () => {
      window.dispatchEvent(
        new StorageEvent("storage", {
          key: "token",
          newValue: "new-token",
          oldValue: null,
        }),
      );
    });

    // Wait for effect to complete
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Wait for query to complete
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Query should be enabled after token is added
    expect(mockFetchUserData).toHaveBeenCalled();
    expect(result.current.data).toEqual(mockUserData);
  });

  it("should respect staleTime configuration", async () => {
    mockLocalStorage.getItem.mockReturnValue("valid-token");
    const mockUserData: UserData = {
      id: 123,
      email: "test@example.com",
      display_name: "Test User",
      role: "user",
      avatar_url: "https://example.com/avatar.jpg",
      created_at: "2024-03-20T00:00:00Z",
      updated_at: "2024-03-20T00:00:00Z",
    };
    mockFetchUserData.mockResolvedValueOnce(mockUserData);

    const { result, rerender } = renderHook(() => useUser(), {
      wrapper: createWrapper(),
    });

    // Wait for effect and initial query setup
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Wait for query to complete
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(mockFetchUserData).toHaveBeenCalledTimes(1);
    expect(result.current.data).toEqual(mockUserData);

    // Rerender immediately
    rerender();

    // Wait for any potential refetch
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Should not trigger another fetch due to staleTime
    expect(mockFetchUserData).toHaveBeenCalledTimes(1);
    expect(result.current.data).toEqual(mockUserData);
  });
});
