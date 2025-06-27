import { renderHook, act } from "@testing-library/react";
import { useGoogleAuth } from "@/hooks/use-google-auth";
import { useRouter } from "@/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api/client";
import { useUserStore } from "@/lib/store/user-store";
import type { Mock } from "jest-mock";

// Mock dependencies
jest.mock("@/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("@tanstack/react-query", () => {
  return {
    useQueryClient: jest.fn(),
    useMutation: jest.fn().mockImplementation((options) => {
      let mutationError: Error | null = null;
      const mutate = jest.fn().mockImplementation(async (credential) => {
        try {
          const result = await options.mutationFn(credential);
          await options.onSuccess?.(result);
          return result;
        } catch (error) {
          mutationError = error as Error;
          await options.onError?.(error);
          // Don't throw the error, just handle it
          return undefined;
        }
      });
      return {
        mutate,
        isPending: false,
        error: mutationError,
      };
    }),
  };
});

jest.mock("@/lib/api/client", () => ({
  apiClient: {
    post: jest.fn(),
    get: jest.fn(),
  },
}));

jest.mock("@/lib/store/user-store");

describe("useGoogleAuth", () => {
  const mockRouter = {
    replace: jest.fn(),
  };
  const mockQueryClient = {
    invalidateQueries: jest.fn(),
  };
  const mockSetUser = jest.fn();
  const originalCreateElement = document.createElement;
  const mockScript = originalCreateElement.call(document, "script");

  // Mock localStorage
  const mockLocalStorage = {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
    length: 0,
    key: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue(mockRouter);
    (useQueryClient as jest.Mock).mockReturnValue(mockQueryClient);
    (useUserStore as unknown as Mock).mockReturnValue({ setUser: mockSetUser });

    // Setup localStorage mock
    Object.defineProperty(window, "localStorage", {
      value: mockLocalStorage,
      writable: true,
      configurable: true,
    });

    // Mock process.env
    process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID = "test-client-id";

    // Mock window.google with proper initialization
    const mockInitialize = jest.fn((options) => {
      window.handleCredentialResponse = options.callback;
    });
    const mockRenderButton = jest.fn();

    Object.defineProperty(window, "google", {
      value: {
        accounts: {
          id: {
            initialize: mockInitialize,
            renderButton: mockRenderButton,
          },
        },
      },
      writable: true,
      configurable: true,
    });

    // Mock document.createElement and appendChild
    jest
      .spyOn(document, "createElement")
      .mockImplementation((tagName: string) => {
        if (tagName === "script") {
          return mockScript;
        }
        return originalCreateElement.call(document, tagName);
      });

    // Mock getElementById
    jest
      .spyOn(document, "getElementById")
      .mockReturnValue(document.createElement("div"));

    jest.spyOn(document.head, "appendChild").mockImplementation(() => {
      setTimeout(() => {
        if (mockScript.onload) {
          mockScript.onload({} as Event);
        }
      }, 0);
      return mockScript;
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  });

  it("should initialize Google Sign-In and render button", async () => {
    renderHook(() => useGoogleAuth());

    // Wait for script onload to be called
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(window.google.accounts.id.initialize).toHaveBeenCalledWith({
      client_id: "test-client-id",
      callback: expect.any(Function),
    });

    expect(window.google.accounts.id.renderButton).toHaveBeenCalledWith(
      expect.any(HTMLElement),
      {
        theme: "outline",
        size: "large",
        text: "sign_in_with",
        shape: "rectangular",
        logo_alignment: "left",
      },
    );
  });

  it("should handle successful login and user data fetch", async () => {
    const mockToken = "mock-token";
    const mockUserData = {
      id: "123",
      email: "test@example.com",
      display_name: "Test User",
      role: "user",
      avatar_url: "avatar.jpg",
      username: null, // User doesn't have a username
    };

    (apiClient.post as jest.Mock).mockResolvedValueOnce({
      data: { token: mockToken },
    });
    (apiClient.get as jest.Mock).mockResolvedValueOnce({ data: mockUserData });

    const { result } = renderHook(() => useGoogleAuth());

    // Wait for script onload
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Simulate Google credential response
    await act(async () => {
      await window.handleCredentialResponse({
        credential: "mock-credential",
        select_by: "",
        g_csrf_token: "",
      });
    });

    // Wait for state updates and setTimeout to execute (at least 100ms)
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 150));
    });

    expect(apiClient.post).toHaveBeenCalledWith(expect.any(String), {
      id_token: "mock-credential",
      account_type: "user",
    });
    expect(mockLocalStorage.setItem).toHaveBeenCalledWith("token", mockToken);
    expect(mockSetUser).toHaveBeenCalledWith({
      id: mockUserData.id,
      email: mockUserData.email,
      name: mockUserData.display_name,
      role: mockUserData.role,
      avatar: mockUserData.avatar_url,
      username: null,
    });
    expect(mockQueryClient.invalidateQueries).toHaveBeenCalledWith({
      queryKey: ["user"],
    });
    expect(mockRouter.replace).toHaveBeenCalledWith("/profile/update");
    expect(result.current.error).toBeNull();
  });

  it("should handle login failure", async () => {
    const { result } = renderHook(() => useGoogleAuth());

    // Wait for script onload
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Mock the API error
    (apiClient.post as jest.Mock).mockRejectedValueOnce(new Error("API Error"));

    // Simulate Google credential response
    await act(async () => {
      try {
        await window.handleCredentialResponse({
          credential: "mock-credential",
          select_by: "",
          g_csrf_token: "",
        });
      } catch {
        // Error is expected to be caught by mutation
      }
    });

    // Wait for error state to be set
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 50));
    });

    expect(result.current.error).toBe("Login failed");
    expect(apiClient.post).toHaveBeenCalledWith(expect.any(String), {
      id_token: "mock-credential",
      account_type: "user",
    });
  });

  it("should handle user data fetch failure", async () => {
    const mockToken = "mock-token";
    (apiClient.post as jest.Mock).mockResolvedValueOnce({
      data: { token: mockToken },
    });
    (apiClient.get as jest.Mock).mockRejectedValueOnce(
      new Error("Failed to fetch user data"),
    );

    const { result } = renderHook(() => useGoogleAuth());

    // Wait for script onload
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    // Simulate Google credential response
    await act(async () => {
      await window.handleCredentialResponse({
        credential: "mock-credential",
        select_by: "",
        g_csrf_token: "",
      });
    });

    // Wait for state updates
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });

    expect(mockLocalStorage.setItem).toHaveBeenCalledWith("token", mockToken);
    // When user data fetch fails, no redirection should happen
    expect(mockRouter.replace).not.toHaveBeenCalled();
    expect(result.current.error).toBeNull();
  });

  it("should cleanup on unmount", () => {
    const { unmount } = renderHook(() => useGoogleAuth());

    jest.spyOn(document, "querySelector").mockReturnValueOnce(mockScript);
    jest
      .spyOn(document.head, "removeChild")
      .mockImplementation(() => mockScript);

    unmount();

    expect(window.handleCredentialResponse).toBeDefined();
    expect(document.querySelector).toHaveBeenCalledWith(
      'script[src="https://accounts.google.com/gsi/client"]',
    );
    expect(document.head.removeChild).toHaveBeenCalled();
  });
});
